package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Composable
fun NeoPaymentForm(
    db: V11Database,
    month: YearMonth,
    existing: PaymentV11?,
    onDismiss: () -> Unit,
    onSave: (PaymentV11) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var cashAccount by rememberSaveable(existing?.id) { mutableStateOf(existing?.cashBookAccountId) }
    var cashAccounts by remember { mutableStateOf(emptyList<com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge.EligibleAccount>()) }
    var cashAccountsFailed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        try {
            cashAccounts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge(context).accounts()
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { cashAccountsFailed = true }
    }
    val linked = existing?.cashBookAccountId != null
    val recordId = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    val allocationId = rememberSaveable(existing?.id) { UUID.randomUUID().toString() }
    var amount by
        rememberSaveable(existing?.id) { mutableStateOf(existing?.amount?.toString().orEmpty()) }
    var date by
        rememberSaveable(existing?.id) {
            mutableStateOf(existing?.receivedDate ?: LocalDate.now().toString())
        }
    var source by rememberSaveable { mutableStateOf(existing?.sourceId) }
    var method by rememberSaveable { mutableStateOf(existing?.paymentMethodId) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var submitted by remember { mutableStateOf(false) }
    val write = LocalNeoEditorWriteState.current
    LaunchedEffect(write.error) { if (write.error != null) submitted = false }
    val amountInput = MoneyInput.parse(amount)
    val keyboard = LocalSoftwareKeyboardController.current
    val leave = neoDiscardGuard(listOf(amount, date, source, method, note, cashAccount), onDismiss)
    AlertDialog(
        onDismissRequest = leave,
        title = { Text(neoLabel("دفعة مستلمة", "Received payment")) },
        text = {
            Column(
                Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeoMoneyInput(amount, { if (!linked) amount = it })
                NeoDateInput(neoLabel("تاريخ الاستلام", "Received date"), date) { if (!linked) date = it }
                if (linked) Text(neoLabel("الدفعة مرتبطة بكاش بوك؛ المبلغ والتاريخ والحساب محفوظون", "Linked to Cash Book; amount, date and account are locked"))
                else {
                    NeoChoice(
                        neoLabel("حساب كاش بوك", "Cash Book account"),
                        listOf<String?>(null) + cashAccounts.map { it.id },
                        cashAccount,
                        { id -> cashAccounts.firstOrNull { it.id == id }?.name ?: neoLabel("بدون ترحيل", "Do not post") },
                        { cashAccount = it },
                    )
                    if (cashAccountsFailed) Text(neoLabel("تعذر تحميل حسابات كاش بوك", "Could not load Cash Book accounts"))
                    else if (cashAccounts.isEmpty()) Text(neoLabel("أنشئ حسابًا داخل كاش بوك لتفعيل الربط", "Create a Cash Book account to link receipts"))
                }
                NeoChoice(
                    neoLabel("مكان العمل أو مصدر الدخل", "Workplace or income source"),
                    listOf<String?>(null) +
                        db.incomeSources.filter { it.active || it.id == source }.map { it.id },
                    source,
                    { id ->
                        db.incomeSources.firstOrNull { it.id == id }?.name
                            ?: neoLabel("عام", "General")
                    },
                    { source = it },
                )
                NeoChoice(
                    neoLabel("طريقة الدفع", "Payment method"),
                    listOf<String?>(null) +
                        db.paymentMethods.filter { it.active || it.id == method }.map { it.id },
                    method,
                    { id ->
                        db.paymentMethods.firstOrNull { it.id == id }?.name
                            ?: neoLabel("بدون", "None")
                    },
                    { method = it },
                )
                OutlinedTextField(
                    note,
                    { note = it.take(1000) },
                    label = { Text(neoLabel("ملاحظة اختيارية", "Optional note")) },
                )
                if ((existing?.allocations?.size ?: 0) > 1)
                    Text(
                        neoLabel(
                            "يُحفظ توزيع الأشهر؛ عدّله من أدوات التنظيم",
                            "Monthly allocation is preserved; change it in Organize and review",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        },
        confirmButton = {
            leave.SaveButton(
                enabled =
                    !submitted &&
                        !write.pending &&
                        amountInput.positive &&
                        runCatching { LocalDate.parse(date) }.isSuccess,
                onClick =
                    neoWriteAction {
                        if (submitted) return@neoWriteAction
                        submitted = true
                        keyboard?.hide()
                        val value = amountInput.amount!!
                        val base = existing ?: PaymentV11(recordId, value, date, month.toString())
                        val allocations =
                            if (existing == null)
                                listOf(
                                    PaymentAllocationV11(
                                        allocationId,
                                        month.toString(),
                                        source,
                                        value,
                                    )
                                )
                            else
                                ModernOperations.resizedAllocations(base.allocations, value).map {
                                    if (it.sourceId == base.sourceId) it.copy(sourceId = source)
                                    else it
                                }
                        onSave(
                            base.copy(
                                amount = value,
                                receivedDate = date,
                                sourceId = source,
                                paymentMethodId = method,
                                note = note,
                                allocations = allocations,
                                cashBookAccountId = cashAccount,
                            )
                        )
                    },
            ) {
                Text(neoLabel("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = leave) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}

@Composable
fun NeoObligationForm(
    db: V11Database,
    onDismiss: () -> Unit,
    onSave: (V11Database) -> Unit,
    existing: ObligationV11? = null,
) {
    val recordId = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var type by rememberSaveable {
        mutableStateOf(
            if (existing?.note?.startsWith("[SAVINGS]") == true) "SAVINGS"
            else if (existing?.note?.startsWith("[SUBSCRIPTION]") == true) "SUBSCRIPTION"
            else existing?.type?.name ?: "INSTALLMENT"
        )
    }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var total by rememberSaveable { mutableStateOf(existing?.totalAmount?.toString().orEmpty()) }
    var installment by rememberSaveable {
        mutableStateOf(existing?.installmentAmount?.toString().orEmpty())
    }
    var count by rememberSaveable {
        mutableStateOf(existing?.installmentCount?.toString().orEmpty())
    }
    var date by rememberSaveable {
        mutableStateOf(existing?.startDate ?: LocalDate.now().toString())
    }
    var dueDay by rememberSaveable {
        mutableIntStateOf(existing?.dueDay ?: LocalDate.now().dayOfMonth)
    }
    var note by rememberSaveable {
        mutableStateOf(
            existing?.note?.removePrefix("[SAVINGS] ")?.removePrefix("[SUBSCRIPTION] ").orEmpty()
        )
    }
    var submitted by remember { mutableStateOf(false) }
    val write = LocalNeoEditorWriteState.current
    LaunchedEffect(write.error) { if (write.error != null) submitted = false }
    val totalInput = MoneyInput.parse(total)
    val installmentInput = MoneyInput.parse(installment)
    val recurring = type in listOf("SUBSCRIPTION", "RECURRING_BILL")
    val valid =
        name.isNotBlank() &&
            installmentInput.positive &&
            (recurring || totalInput.positive) &&
            (count.isBlank() || (count.toLocalizedIntOrNull() ?: 0) > 0)
    val leave =
        neoDiscardGuard(
            listOf(type, name, total, installment, count, date, dueDay, note),
            onDismiss,
        )
    AlertDialog(
        onDismissRequest = leave,
        title = { Text(neoLabel("التزام أو هدف ادخار", "Obligation or savings goal")) },
        text = {
            Column(
                Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeoChoice(
                    neoLabel("النوع", "Type"),
                    listOf(
                        "INSTALLMENT",
                        "DEBT",
                        "LOAN",
                        "SUBSCRIPTION",
                        "SAVINGS",
                        "CREDIT_CARD",
                        "ASSOCIATION",
                        "RECURRING_BILL",
                        "OTHER",
                    ),
                    type,
                    { neoObligationType(it) },
                    { type = it },
                )
                OutlinedTextField(
                    name,
                    { name = it.take(200) },
                    label = { Text(neoLabel("الاسم", "Name")) },
                )
                if (!recurring) {
                    Text(neoLabel("الإجمالي أو الهدف", "Total or goal"))
                    NeoMoneyInput(total, { total = it })
                }
                Text(neoLabel("المبلغ الشهري", "Monthly amount"))
                NeoMoneyInput(installment, { installment = it })
                if (type in listOf("INSTALLMENT", "LOAN", "ASSOCIATION"))
                    OutlinedTextField(
                        count,
                        { count = it },
                        label = {
                            Text(neoLabel("عدد الدفعات (اختياري)", "Payment count (optional)"))
                        },
                        isError = count.isNotBlank() && (count.toLocalizedIntOrNull() ?: 0) <= 0,
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                    )
                NeoDateInput(neoLabel("بداية", "Start"), date) { date = it }
                NeoChoice(
                    neoLabel("يوم الاستحقاق", "Due day"),
                    (1..31).toList(),
                    dueDay,
                    { it.toString() },
                    { dueDay = it },
                )
                OutlinedTextField(
                    note,
                    { note = it.take(1000) },
                    label = { Text(neoLabel("ملاحظة", "Note")) },
                )
                if (type in listOf("SAVINGS", "SUBSCRIPTION"))
                    Text(
                        neoLabel(
                            "يُحفظ كنوع متوافق (أخرى) مع تصنيف واضح؛ لا يُعاد تفسير التزاماتك السابقة",
                            "Stored as compatible Other with a clear subtype; existing obligations are unchanged",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        },
        confirmButton = {
            leave.SaveButton(
                enabled = valid && !submitted && !write.pending,
                onClick =
                    neoWriteAction {
                        if (submitted) return@neoWriteAction
                        submitted = true
                        val base =
                            runCatching { ObligationTypeV11.valueOf(type) }
                                .getOrDefault(ObligationTypeV11.OTHER)
                        if (existing != null)
                            onSave(
                                db.copy(
                                    obligations =
                                        db.obligations.map {
                                            if (it.id == existing.id)
                                                it.copy(
                                                    name = name,
                                                    type = base,
                                                    totalAmount =
                                                        if (recurring) null else totalInput.amount,
                                                    installmentAmount = installmentInput.amount,
                                                    startDate = date,
                                                    dueDay = dueDay,
                                                    installmentCount =
                                                        if (
                                                            type in
                                                                listOf(
                                                                    "INSTALLMENT",
                                                                    "LOAN",
                                                                    "ASSOCIATION",
                                                                )
                                                        )
                                                            count.toLocalizedIntOrNull()
                                                        else null,
                                                    note =
                                                        (if (
                                                            type in
                                                                listOf("SAVINGS", "SUBSCRIPTION")
                                                        )
                                                            "[$type] "
                                                        else "") + note,
                                                )
                                            else it
                                        }
                                )
                            )
                        else
                            onSave(
                                V11AccountsController.addObligation(
                                    db,
                                    name,
                                    base,
                                    if (recurring) null else totalInput.amount,
                                    installmentInput.amount,
                                    date,
                                    dueDay,
                                    if (type in listOf("INSTALLMENT", "LOAN", "ASSOCIATION"))
                                        count.toLocalizedIntOrNull()
                                    else null,
                                    3,
                                    (if (type in listOf("SAVINGS", "SUBSCRIPTION")) "[$type] "
                                    else "") + note,
                                    recordId = recordId,
                                )
                            )
                    },
            ) {
                Text(neoLabel("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = leave) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}

@Composable
fun neoObligationType(s: String): String =
    when (s) {
        "INSTALLMENT" -> neoLabel("قسط", "Installment")
        "DEBT" -> neoLabel("دين", "Debt")
        "LOAN" -> neoLabel("قرض", "Loan")
        "SUBSCRIPTION" -> neoLabel("اشتراك", "Subscription")
        "SAVINGS" -> neoLabel("ادخار", "Savings")
        "CREDIT_CARD" -> neoLabel("بطاقة ائتمان", "Credit card")
        "ASSOCIATION" -> neoLabel("جمعية", "Association")
        "RECURRING_BILL" -> neoLabel("فاتورة دورية", "Recurring bill")
        else -> neoLabel("أخرى", "Other")
    }

/** Keeps edits in place unless the user explicitly discards them. */
@Composable
private fun neoDiscardGuard(values: List<Any?>, onDismiss: () -> Unit): NeoEditorExit =
    rememberNeoEditorExit(values, onDismiss)
