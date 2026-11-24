package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

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

/**
 * Read-only receipt preview. No allocation may exceed its real outstanding
 * entitlement. One transfer may cover several months but must be positive.
 */
internal fun receiptDraftMinor(
    db: V11Database,
    workplaceId: String?,
    months: List<YearMonth>,
    amounts: Map<String, String>,
): Long? {
    val place = workplaceId ?: return null
    if (months.isEmpty()) return null
    var total = 0L
    for (month in months) {
        val minor = runCatching {
            LocalizedNumbers.cents(
                requireNotNull(MoneyInput.parse(amounts[month.toString()].orEmpty()).amount)
            )
        }.getOrNull() ?: return null
        if (minor < 0 || minor > remainingWorkEntitlement(db, place, month)) return null
        total = runCatching { Math.addExact(total, minor) }.getOrNull() ?: return null
    }
    return total.takeIf { it > 0L }
}

@Composable
internal fun NeoReceiveEntitlementsDialog(db:V11Database,workplaces:List<Workplace>,initialWorkplace:String?,initialMonths:List<YearMonth>,onDismiss:()->Unit,onSave:(V11Database)->Unit) {
    val context=LocalContext.current
    val requestId=rememberSaveable {UUID.randomUUID().toString()}
    var workplace by rememberSaveable {mutableStateOf(initialWorkplace ?: workplaces.firstOrNull()?.id)}
    // Resolve outstanding periods from real earnings rather than parsing month text.
    // The initial statement months are preserved even when outside the lookback.
    val availableMonths=remember(db, workplace, initialMonths) {
        val recordedMonths = db.days.mapNotNull { day ->
            runCatching { YearMonth.from(LocalDate.parse(day.date)) }.getOrNull()
        }
        (initialMonths + recordedMonths + (0..35).map {YearMonth.now().minusMonths(it.toLong())})
            .distinct().sortedDescending()
            .filter {month -> workplace?.let {remainingWorkEntitlement(db,it,month)>0L} == true}
    }
    var selectedMonthKeys by rememberSaveable {
        mutableStateOf(initialMonths.map {it.toString()})
    }
    LaunchedEffect(workplace,availableMonths) {
        if(selectedMonthKeys.none {key -> availableMonths.any {it.toString()==key}}) {
            selectedMonthKeys=availableMonths.take(3).map {it.toString()}
        }
    }
    val months=availableMonths.filter {it.toString() in selectedMonthKeys}.sorted()
    var amounts by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    var amountWorkplace by remember { mutableStateOf(workplace) }
    LaunchedEffect(workplace,months) {
        // Toggling another month must not overwrite an already-entered partial
        // receipt. Switching workplaces, however, resets incompatible amounts.
        val keepAmounts = workplace == amountWorkplace
        amounts=months.associate {month ->
            val key = month.toString()
            key to ((if (keepAmounts) amounts[key] else null)
                ?: LocalizedNumbers.amount(
                    workplace?.let {remainingWorkEntitlement(db,it,month)} ?: 0L
                ).toString())
        }
        amountWorkplace = workplace
    }
    var receivedDate by rememberSaveable {mutableStateOf(LocalDate.now().toString())}
    var account by rememberSaveable {mutableStateOf<String?>(null)}
    var method by rememberSaveable {mutableStateOf<String?>(null)}
    var accounts by remember {
        mutableStateOf(emptyList<com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge.EligibleAccount>())
    }
    var loadingAccounts by remember { mutableStateOf(true) }
    var creatingAccount by remember { mutableStateOf(false) }
    var createAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val draftMinor = remember(db, workplace, months, amounts) {
        receiptDraftMinor(db, workplace, months, amounts)
    }
    val selectedAccount = accounts.firstOrNull { it.id == account }
    var error by remember {mutableStateOf<String?>(null)}
    var review by remember {mutableStateOf<V11Database?>(null)}
    var submitted by remember {mutableStateOf(false)}
    val write=LocalNeoEditorWriteState.current
    LaunchedEffect(write.error) {if(write.error!=null) submitted=false}
    LaunchedEffect(Unit) {
        try {
            accounts = withContext(Dispatchers.IO) {
                com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge(context).accounts()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = "تعذر تحميل حسابات Cash Book؛ حاول تحديث القائمة / Could not load Cash Book accounts"
        } finally {
            loadingAccounts = false
        }
    }
    if (createAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!creatingAccount) createAccountDialog = false },
            title = { Text(neoLabel("إنشاء حساب للاستلام", "Create receiving account")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        neoLabel(
                            "سيتم إنشاء حساب عادي في Cash Book بالجنيه ورصيد افتتاحي صفر. الحساب يُحفظ فورًا حتى لو ألغيت الاستلام بعد ذلك.",
                            "Creates a regular EGP account with zero opening balance, saved immediately even if this receipt is cancelled.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it.take(80) },
                        label = { Text(neoLabel("اسم الحساب", "Account name")) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !creatingAccount && newAccountName.trim().isNotEmpty(),
                    onClick = {
                        if (!creatingAccount) {
                            creatingAccount = true
                            error = null
                            scope.launch {
                                try {
                                    val (newAccount, freshAccounts) = withContext(Dispatchers.IO) {
                                        val bridge = com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge(context)
                                        val created = bridge.createReceivingAccount(newAccountName)
                                        created to bridge.accounts()
                                    }
                                    accounts = freshAccounts
                                    account = newAccount.id
                                    newAccountName = ""
                                    createAccountDialog = false
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (failure: Exception) {
                                    error = failure.message ?: "تعذر إنشاء الحساب / Could not create account"
                                } finally {
                                    creatingAccount = false
                                }
                            }
                        }
                    }
                ) { Text(neoLabel("إنشاء الحساب", "Create account")) }
            },
            dismissButton = {
                TextButton(enabled = !creatingAccount, onClick = {
                    createAccountDialog = false
                    error = null
                }) { Text(neoLabel("إلغاء", "Cancel")) }
            },
        )
        return
    }
    if(review!=null) {
        val receipt=review!!.payments.single {it.id==requestId}
        AlertDialog(onDismissRequest={if(!submitted) review=null},title={Text(neoLabel("مراجعة الاستلام","Review receipt"))},text={Column {
            Text(workplaces.firstOrNull {it.id==workplace}?.name.orEmpty(),
                style = MaterialTheme.typography.titleMedium)
            Text(selectedAccount?.name.orEmpty()+" • "+receipt.receivedDate)
            receipt.allocations.forEach { Text(it.accrualMonth+" • "+it.amount) }
            HorizontalDivider()
            Text(neoLabel("المبلغ المستلم: ", "Receipt amount: ")+receipt.amount+" EGP",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(neoLabel("الحساب المستلم: ", "Receiving account: ")+selectedAccount?.name.orEmpty())
            Text(neoLabel("طريقة الاستلام: ", "Payment method: ")+
                (db.paymentMethods.firstOrNull { it.id == method }?.name ?: neoLabel("غير محددة", "Not specified")))
            selectedAccount?.let { receiving ->
                Text(neoLabel("الرصيد قبل: ", "Before: ")+
                    LocalizedNumbers.amount(receiving.balanceMinor)+" EGP")
                val newBalance = runCatching {
                    Math.addExact(receiving.balanceMinor, LocalizedNumbers.cents(receipt.amount))
                }.getOrNull()
                if (newBalance != null) Text(
                    neoLabel("الرصيد المتوقع بعد: ", "Estimated after: ")+
                        LocalizedNumbers.amount(newBalance)+" EGP")
            }
            Text(
                neoLabel(
                    "تأكيد الاستلام يسجّل دفعة واحدة موزّعة على الأشهر ويُرحّل دخلًا واحدًا إلى Cash Book. عند تعذر الترحيل أعد المزامنة دون تكرار القبض.",
                    "Confirm saves one receipt allocated across months and posts one Cash Book income. If posting fails, retry syncing without creating a second receipt.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }},confirmButton={TextButton(enabled=!submitted&&!write.pending,onClick={if(!submitted) {submitted=true;onSave(review!!)}}) {Text(neoLabel("تأكيد الاستلام","Confirm receipt"))}},dismissButton={TextButton(enabled=!submitted,onClick={review=null}) {Text(neoLabel("تعديل","Edit"))}})
        return
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text(neoLabel("استلام المستحقات","Receive entitlements"))},text={Column(Modifier.heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        NeoChoice(neoLabel("مكان العمل","Workplace"),workplaces.map {it.id},workplace,{id->workplaces.firstOrNull {it.id==id}?.name.orEmpty()},{workplace=it})
        Text(neoLabel("اختر أشهر المستحقات غير المستلمة", "Select unpaid earning months"))
        if(availableMonths.isEmpty()) Text(neoLabel("لا توجد مستحقات متبقية لمكان العمل هذا", "No outstanding entitlements for this workplace"))
        availableMonths.forEach {month ->
            val key=month.toString()
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked=key in selectedMonthKeys,onCheckedChange={checked ->
                    selectedMonthKeys=if(checked) (selectedMonthKeys+key).distinct() else selectedMonthKeys-key
                })
                Text(key+" • "+LocalizedNumbers.amount(workplace?.let {remainingWorkEntitlement(db,it,month)} ?: 0L))
            }
        }
        months.forEach {month ->
            OutlinedTextField(
                amounts[month.toString()].orEmpty(),
                {amounts=amounts+(month.toString() to it)},
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
                singleLine=true,
                label={Text(neoLabel("مبلغ الاستلام: ","Received amount: ")+month)},
                supportingText={Text(neoLabel("المتبقي: ","Remaining: ")+LocalizedNumbers.amount(workplace?.let {remainingWorkEntitlement(db,it,month)} ?: 0L))}
            )
        }
        NeoDateInput(neoLabel("تاريخ الاستلام الفعلي","Actual receipt date"),receivedDate){receivedDate=it}
        HorizontalDivider()
        Text(neoLabel("الفلوس هتتحط فين؟", "Where will the money go?"),
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            neoLabel("اختار حساب Cash Book المستلم؛ حسابات الديون غير متاحة هنا.",
                "Choose an eligible Cash Book account. Debt accounts cannot receive salary."),
            style = MaterialTheme.typography.bodySmall,
        )
        NeoChoice(
            neoLabel("الحساب المستلم", "Receiving account"),
            listOf<String?>(null)+accounts.map {it.id}, account,
            { id -> accounts.firstOrNull { it.id == id }?.let { it.name + " • EGP" }
                ?: neoLabel("اختر حسابًا", "Select account") },
            { account = it },
        )
        if (!loadingAccounts && accounts.isEmpty()) Text(
            neoLabel("لا توجد حسابات مؤهلة. أنشئ حسابًا أولًا.", "No eligible accounts. Create one first."),
            color = MaterialTheme.colorScheme.error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                error = null
                newAccountName = ""
                createAccountDialog = true
            }) { Text(neoLabel("+ حساب جديد", "+ New account")) }
            TextButton(
                enabled = !loadingAccounts,
                onClick = {
                    loadingAccounts = true
                    scope.launch {
                        try {
                            val fresh = withContext(Dispatchers.IO) {
                                com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge(context).accounts()
                            }
                            accounts = fresh
                            if (account != null && fresh.none { it.id == account }) account = null
                            error = null
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            error = "تعذر تحديث الحسابات / Could not refresh accounts"
                        } finally {
                            loadingAccounts = false
                        }
                    }
                },
            ) { Text(neoLabel("تحديث", "Refresh")) }
        }
        NeoChoice(
            neoLabel("طريقة الاستلام (اختيارية)", "Payment method (optional)"),
            listOf<String?>(null)+db.paymentMethods.filter {it.active}.map {it.id},
            method,
            {id->db.paymentMethods.firstOrNull {it.id==id}?.name ?: neoLabel("بدون","None")},
            {method=it},
        )
        Text(
            neoLabel("طريقة الاستلام وصفية، أما الرصيد فيزيد في الحساب المختار فقط.",
                "Method describes the payment. The chosen account alone receives the balance."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selectedAccount != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(selectedAccount.name, style = MaterialTheme.typography.titleSmall)
                    Text(neoLabel("الرصيد قبل الاستلام: ", "Balance before: ")+
                        LocalizedNumbers.amount(selectedAccount.balanceMinor)+" EGP")
                    Text(neoLabel("المبلغ المستلم: ", "This receipt: ")+
                        (draftMinor?.let { LocalizedNumbers.amount(it).toString() } ?: "—")+" EGP")
                    val after = draftMinor?.let {
                        runCatching { Math.addExact(selectedAccount.balanceMinor, it) }.getOrNull()
                    }
                    if (after != null) Text(
                        neoLabel("الرصيد المتوقع بعد الاستلام: ", "Estimated balance after: ")+
                            LocalizedNumbers.amount(after)+" EGP",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        neoLabel("هذه معاينة بناءً على آخر رصيد مقروء؛ الرصيد النهائي يحسبه Cash Book عند الترحيل.",
                            "Preview uses the last loaded balance; Cash Book calculates the final posted balance."),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (draftMinor == null && months.isNotEmpty()) Text(
            neoLabel("راجع المبالغ: الإجمالي لازم يكون أكبر من صفر ولا يتجاوز المتبقي من كل شهر.",
                "Enter a positive receipt, no more than what is owed for each selected month."),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(enabled=selectedAccount!=null&&workplace!=null&&draftMinor!=null&&!write.pending&&!loadingAccounts,onClick={
        try {
            check(db.payments.none {it.id==requestId})
            check(selectedAccount != null && selectedAccount.id == account) { "Select a valid receiving account" }
            check(draftMinor != null && draftMinor > 0L) { "Enter a valid receipt amount" }
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
        } catch(failure:Exception) {error=failure.message ?: "راجع الأشهر والمبالغ والتاريخ / Check months, amounts and date"}
    }) {Text(neoLabel("مراجعة","Review"))}},dismissButton={TextButton(onClick=onDismiss) {Text(neoLabel("إلغاء","Cancel"))}})
}
