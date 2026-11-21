package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun remainingWorkEntitlement(db:V11Database,workplace:String,month:YearMonth):Long {
    val sourceIds=db.incomeSources.filter {it.workplaceId==workplace}.map {it.id}.toSet()
    val earned=V11FinanceEngine.month(db,month).byWorkplace.filter {it.workplaceId==workplace}.sumOf {LocalizedNumbers.cents(it.amount)}
    val received=db.payments.filter {it.reversedAt==null}.sumOf {p ->
        if(p.allocations.isEmpty()) if(p.sourceId in sourceIds && p.accrualMonth==month.toString()) LocalizedNumbers.cents(p.amount) else 0L
        else p.allocations.filter {it.sourceId in sourceIds && it.accrualMonth==month.toString()}.sumOf {LocalizedNumbers.cents(it.amount)}
    }
    val settled=db.settlements.filter {(it.sourceId in sourceIds || it.workplaceId==workplace) && it.accrualMonth==month.toString() && it.action in setOf(SettlementActionV11.WRITE_OFF,SettlementActionV11.CANCEL_ENTITLEMENT,SettlementActionV11.ADJUSTMENT)}.sumOf {LocalizedNumbers.cents(it.amount)}
    return (earned-received-settled).coerceAtLeast(0)
}

@Composable
internal fun NeoReceiveEntitlementsDialog(db:V11Database,workplaces:List<Workplace>,initialWorkplace:String?,initialMonths:List<YearMonth>,onDismiss:()->Unit,onSave:(V11Database)->Unit) {
    val context=LocalContext.current
    val requestId=rememberSaveable {UUID.randomUUID().toString()}
    var workplace by rememberSaveable {mutableStateOf(initialWorkplace ?: workplaces.firstOrNull()?.id)}
    // Resolve outstanding periods from real earnings rather than parsing month text.
    // The initial statement months are preserved even when outside the lookback.
    val availableMonths=remember(db, workplace, initialMonths) {
        (initialMonths + (0..35).map {YearMonth.now().minusMonths(it.toLong())})
            .distinct().sortedDescending()
            .filter {month -> workplace?.let {remainingWorkEntitlement(db,it,month)>0L} == true}
            .take(36)
    }
    var selectedMonthKeys by rememberSaveable {
        mutableStateOf(initialMonths.map {it.toString()}.toSet())
    }
    LaunchedEffect(workplace,availableMonths) {
        if(selectedMonthKeys.none {key -> availableMonths.any {it.toString()==key}}) {
            selectedMonthKeys=availableMonths.take(3).map {it.toString()}.toSet()
        }
    }
    val months=availableMonths.filter {it.toString() in selectedMonthKeys}.sorted()
    var amounts by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    LaunchedEffect(workplace,months) {
        amounts=months.associate {month ->
            month.toString() to LocalizedNumbers.amount(
                workplace?.let {remainingWorkEntitlement(db,it,month)} ?: 0L
            ).toString()
        }
    }
    var receivedDate by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var account by rememberSaveable {mutableStateOf<String?>(null)}
    var method by rememberSaveable {mutableStateOf<String?>(null)}
    var accounts by remember {mutableStateOf(emptyList<com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge.EligibleAccount>())}
    var error by remember {mutableStateOf<String?>(null)}
    var review by remember {mutableStateOf<V11Database?>(null)}
    var submitted by remember {mutableStateOf(false)}
    val write=LocalNeoEditorWriteState.current
    LaunchedEffect(write.error) {if(write.error!=null) submitted=false}
    LaunchedEffect(Unit) {try {accounts=withContext(Dispatchers.IO) {com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge(context).accounts()}} catch(_:Exception) {error="تعذر تحميل الحسابات / Could not load accounts"}}
    if(review!=null) {
        val receipt=review!!.payments.single {it.id==requestId}
        AlertDialog(onDismissRequest={if(!submitted) review=null},title={Text(neoLabel("مراجعة الاستلام","Review receipt"))},text={Column {
            Text(workplaces.firstOrNull {it.id==workplace}?.name.orEmpty())
            Text(accounts.firstOrNull {it.id==account}?.name.orEmpty()+" • "+receipt.receivedDate)
            receipt.allocations.forEach {Text(it.accrualMonth+" • "+it.amount)}
            Text(neoLabel("الإجمالي: ","Total: ")+receipt.amount)
            error?.let {Text(it)}
        }},confirmButton={TextButton(enabled=!submitted&&!write.pending,onClick={if(!submitted) {submitted=true;onSave(review!!)}}) {Text(neoLabel("تأكيد الاستلام","Confirm receipt"))}},dismissButton={TextButton(enabled=!submitted,onClick={review=null}) {Text(neoLabel("تعديل","Edit"))}})
        return
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text(neoLabel("استلام المستحقات","Receive entitlements"))},text={Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        NeoChoice(neoLabel("مكان العمل","Workplace"),workplaces.map {it.id},workplace,{id->workplaces.firstOrNull {it.id==id}?.name.orEmpty()},{workplace=it})
        Text(neoLabel("اختر أشهر المستحقات غير المستلمة", "Select unpaid earning months"))
        if(availableMonths.isEmpty()) Text(neoLabel("لا توجد مستحقات متبقية لمكان العمل هذا", "No outstanding entitlements for this workplace"))
        availableMonths.forEach {month ->
            val key=month.toString()
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked=key in selectedMonthKeys,onCheckedChange={checked ->
                    selectedMonthKeys=if(checked) selectedMonthKeys+key else selectedMonthKeys-key
                })
                Text(key+" • "+LocalizedNumbers.amount(workplace?.let {remainingWorkEntitlement(db,it,month)} ?: 0L))
            }
        }
        months.forEach {month ->
            OutlinedTextField(
                amounts[month.toString()].orEmpty(),
                {amounts=amounts+(month.toString() to it)},
                label={Text(neoLabel("مبلغ الاستلام: ","Received amount: ")+month)},
                supportingText={Text(neoLabel("المتبقي: ","Remaining: ")+LocalizedNumbers.amount(workplace?.let {remainingWorkEntitlement(db,it,month)} ?: 0L))}
            )
        }
        NeoDateInput(neoLabel("تاريخ الاستلام الفعلي","Actual receipt date"),receivedDate){receivedDate=it}
        NeoChoice(neoLabel("الحساب المستلم","Receiving account"),listOf<String?>(null)+accounts.map {it.id},account,{id->accounts.firstOrNull {it.id==id}?.name ?: neoLabel("اختر حسابًا","Select account")},{account=it})
        NeoChoice(neoLabel("طريقة الدفع","Payment method"),listOf<String?>(null)+db.paymentMethods.filter {it.active}.map {it.id},method,{id->db.paymentMethods.firstOrNull {it.id==id}?.name ?: neoLabel("بدون","None")},{method=it})
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=account!=null&&workplace!=null&&months.isNotEmpty()&&!write.pending,onClick={
        try {
            check(db.payments.none {it.id==requestId})
            LocalDate.parse(receivedDate)
            val place=requireNotNull(workplace);val selected=months
            val existingSource=db.incomeSources.firstOrNull {it.workplaceId==place}
            val source=existingSource ?: IncomeSourceV11(UUID.nameUUIDFromBytes(("receipt-source:"+place).toByteArray()).toString(),workplaces.first {it.id==place}.name,IncomeSourceTypeV11.SHIFT_BASED,workplaceId=place)
            val allocations=selected.mapNotNull {month ->
                val parsed=MoneyInput.parse(amounts[month.toString()].orEmpty())
                val value=requireNotNull(parsed.amount)
                val minor=LocalizedNumbers.cents(value)
                require(minor>=0 && minor<=remainingWorkEntitlement(db,place,month)) {"Receipt exceeds remaining entitlement"}
                if(minor==0L) null else PaymentAllocationV11(UUID.nameUUIDFromBytes((requestId+month).toByteArray()).toString(),month.toString(),source.id,value)
            }
            val minor=allocations.fold(0L) {total,item->Math.addExact(total,LocalizedNumbers.cents(item.amount))}
            require(minor>0)
            val receipt=PaymentV11(requestId,LocalizedNumbers.amount(minor),receivedDate,selected.first().toString(),sourceId=source.id,paymentMethodId=method,allocations=allocations,cashBookAccountId=requireNotNull(account))
            review=db.copy(incomeSources=if(existingSource==null) db.incomeSources+source else db.incomeSources,payments=db.payments+receipt,auditLog=db.auditLog+AuditEntryV11(UUID.randomUUID().toString(),"PAYMENT_ADD","payment",requestId,"Entitlement receipt reviewed and confirmed"))
            error=null
        } catch(_:Exception) {error="راجع الأشهر والمبالغ والتاريخ؛ لا يمكن قبض أكثر من المتبقي / Check periods, amounts and date; receipt cannot exceed the remaining entitlement"}
    }) {Text(neoLabel("مراجعة","Review"))}},dismissButton={TextButton(onClick=onDismiss) {Text(neoLabel("إلغاء","Cancel"))}})
}
