package com.todoshift.app.v11.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

/**
 * One financial data source and one selected earning month for every salary tab.
 * Presentation never posts a receipt, mutates a settlement or duplicates Cash Book data.
 */
internal object NeoSalaryDashboardLogic {
    fun relevantPayments(db: V11Database, month: YearMonth): List<PaymentV11> {
        val key = month.toString()
        return db.payments.filter { p ->
            if (p.allocations.isEmpty()) p.accrualMonth == key
            else p.allocations.any { it.accrualMonth == key }
        }.sortedWith(compareByDescending<PaymentV11> { it.receivedDate }.thenByDescending { it.id })
    }

    fun portionCents(payment: PaymentV11, month: YearMonth): Long {
        if (payment.reversedAt != null) return 0L
        return historicalPortionCents(payment, month)
    }

    /** Original allocation for audit cards, even after a reversible receipt reversal. */
    fun historicalPortionCents(payment: PaymentV11, month: YearMonth): Long {
        val key = month.toString()
        return if (payment.allocations.isEmpty()) {
            if (payment.accrualMonth == key) LocalizedNumbers.cents(payment.amount) else 0L
        } else payment.allocations.filter { it.accrualMonth == key }
            .sumOf { LocalizedNumbers.cents(it.amount) }
    }

    fun workplaceReceivedCents(db: V11Database, workplaceId: String, month: YearMonth): Long {
        val sources = db.incomeSources.filter { it.workplaceId == workplaceId }
            .map { it.id }.toSet()
        return db.payments.filter { it.reversedAt == null }.sumOf { payment ->
            if (payment.allocations.isEmpty()) {
                if (payment.sourceId in sources && payment.accrualMonth == month.toString())
                    LocalizedNumbers.cents(payment.amount) else 0L
            } else payment.allocations.filter { allocation ->
                allocation.sourceId in sources && allocation.accrualMonth == month.toString()
            }.sumOf { LocalizedNumbers.cents(it.amount) }
        }
    }

    fun collectionProgress(summary: MonthFinanceV11): Float =
        if (summary.expected <= 0.0) 0f
        else (summary.received / summary.expected).toFloat().coerceIn(0f, 1f)
}

@Composable
private fun neoSalaryMoney(amount: Double, visible: Boolean): String {
    if (!visible) return "••••"
    val formatter = NumberFormat.getNumberInstance(if (LocalNeoEnglish.current) Locale.US else Locale("en", "EG"))
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 2
    return formatter.format(amount) + " " + neoLabel("ج.م", "EGP")
}

@Composable
private fun SalaryMetric(label: String, number: Double, visible: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f),
        ),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(neoSalaryMoney(number, visible), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SalaryActionRow(
    primaryEnabled: Boolean,
    onReceive: () -> Unit,
    onRecord: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Button(onClick = onReceive, enabled = primaryEnabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Payments, contentDescription = null,
                modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(neoLabel("استلام المستحقات", "Receive entitlements"))
        }
        TextButton(onClick = onRecord, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(neoLabel("تسجيل مبلغ مستلم يدويًا", "Record a manual receipt"),
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun NeoSalaryDashboard(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    moneyVisible: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReceive: (String?) -> Unit,
    onRecordPayment: () -> Unit,
    onOpenWorkplace: (String?) -> Unit,
    onPaymentDetails: (String) -> Unit,
    onIncomeSources: () -> Unit,
    onPaymentMethods: () -> Unit,
    onRates: () -> Unit,
    onStatement: (String?) -> Unit,
    onReview: () -> Unit,
) {
    val summary = remember(db, month) { V11FinanceEngine.month(db, month) }
    val missingRateDetails = remember(db, month) { V11FinanceEngine.missingRateDetails(db, month) }
    val missingRateCount = missingRateDetails.size
    val payments = remember(db, month) { NeoSalaryDashboardLogic.relevantPayments(db, month) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Independent searches: filtering one tab must not hide records in another.
    var workplaceSearch by rememberSaveable { mutableStateOf("") }
    var receiptSearch by rememberSaveable { mutableStateOf("") }
    var selectedWorkplace by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(workplaces) {
        if (selectedWorkplace == null || workplaces.none { it.id == selectedWorkplace })
            selectedWorkplace = workplaces.firstOrNull()?.id
    }
    val filteredPlaces = summary.byWorkplace.filter { row ->
        workplaceSearch.isBlank() ||
            (workplaces.firstOrNull { it.id == row.workplaceId }?.name
                ?.contains(workplaceSearch, ignoreCase = true) == true)
    }
    val expected = summary.expected
    val pending = summary.difference
    val progress = NeoSalaryDashboardLogic.collectionProgress(summary)
    val currentSnapshot = db.monthSnapshots.firstOrNull {
        it.month == month.toString() && it.reopenedAt == null
    }
    val titleColor = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(neoLabel("مستحقات العمل", "Work entitlements"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = titleColor)
                Text(neoLabel("الاستحقاق والتحصيل والمراجعة", "Earnings, receipts and review"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalIconButton(onClick = onRates) {
                Icon(Icons.Rounded.Tune, neoLabel("أسعار الورديات", "Shift rates"))
            }
        }

        // Every tab shares this exact accrual month, not the receipt's transaction date.
        Surface(shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(if (LocalNeoEnglish.current) Icons.Rounded.ChevronLeft
                        else Icons.Rounded.ChevronRight,
                        neoLabel("الشهر السابق", "Previous month"))
                }
                Text(neoMonthTitle(month, LocalNeoEnglish.current),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = onNext) {
                    Icon(if (LocalNeoEnglish.current) Icons.Rounded.ChevronRight
                        else Icons.Rounded.ChevronLeft,
                        neoLabel("الشهر التالي", "Next month"))
                }
            }
        }

        val tabs = listOf(
            neoLabel("الملخص", "Summary"),
            neoLabel("الدفعات", "Payments"),
            neoLabel("كشف الحساب", "Statement"),
            neoLabel("المراجعة", "Review"),
        )
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, text ->
                FilterChip(
                    selected = index == tab,
                    onClick = { tab = index },
                    label = { Text(text, maxLines = 1,
                        style = MaterialTheme.typography.labelMedium) },
                )
            }
        }

        when (tab) {
            0 -> {
                if (missingRateCount > 0) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                neoLabel(
                                    "هناك $missingRateCount وردية بلا سعر سارٍ بتاريخها؛ إجمالياتها قد تظهر صفرًا. راجع تاريخ بدء السعر.",
                                    "$missingRateCount shifts have no rate effective on their dates; their amounts may show zero. Review rate start dates."
                                ),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            val groups = missingRateDetails.groupBy { it.workplaceId to it.kind }
                                .entries.sortedBy { item -> item.value.minOf { it.date } }
                            groups.take(5).forEach { group ->
                                val place = workplaces.firstOrNull { it.id == group.key.first }?.name
                                    ?: neoLabel("مكان عمل غير محدد", "Unassigned workplace")
                                val first = group.value.minOf { it.date }
                                Text(
                                    "$place • " + neoShiftLabel(group.key.second) +
                                        " • " + group.value.size.toString() + " • " + first,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            if (groups.size > 5) Text(
                                neoLabel("مجموعات أخرى: ", "Other groups: ") + (groups.size - 5),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (missingRateDetails.any { it.workplaceId == null }) Text(
                                neoLabel("بعض الورديات بلا مكان عمل. اربطها بمكان عمل قبل تحديد السعر.",
                                    "Some shifts have no workplace. Assign one before setting rates."),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                neoLabel("سجّل السعر بتاريخ لا يتأخر عن أول وردية ناقصة من نفس النوع والمكان.",
                                    "Enter a rate effective no later than the first missing shift for each type and workplace."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            TextButton(onClick = onRates) {
                                Text(neoLabel("مراجعة أسعار الورديات", "Review shift rates"))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.weight(1f)) {
                        SalaryMetric(neoLabel("المستحق", "Expected"), expected, moneyVisible)
                    }
                    Box(Modifier.weight(1f)) {
                        SalaryMetric(neoLabel("المستلم", "Received"), summary.received, moneyVisible)
                    }
                    Box(Modifier.weight(1f)) {
                        SalaryMetric(neoLabel("المتبقي", "Remaining"), pending, moneyVisible)
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(neoLabel("تقدم التحصيل", "Collection progress"),
                                fontWeight = FontWeight.SemiBold)
                            Text(((progress * 100f).toInt()).toString() + "%",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp))
                        if (summary.settlements != 0.0) Text(
                            neoLabel("تسويات الشهر: ", "Settlements: ") +
                                neoSalaryMoney(summary.settlements, moneyVisible),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                SalaryActionRow(pending > 0.009, { onReceive(null) }, onRecordPayment)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(neoLabel("أماكن العمل", "Workplaces"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(neoLabel("الورديات: ", "Shifts: ") + summary.shiftCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(workplaceSearch, { workplaceSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(neoLabel("ابحث عن مكان عمل", "Search workplaces")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = if (workplaceSearch.isBlank()) null else {
                        { IconButton(onClick = { workplaceSearch = "" }) {
                            Icon(Icons.Rounded.Close, neoLabel("مسح البحث", "Clear search"))
                        } }
                    }, singleLine = true)
                if (filteredPlaces.isEmpty()) {
                    SalaryEmpty(
                        neoLabel("لا توجد مستحقات مطابقة", "No matching entitlements"),
                        neoLabel("تحقق من الشهر أو مكان العمل", "Check the selected month or workplace"),
                    )
                }
                filteredPlaces.forEach { row ->
                    val label = workplaces.firstOrNull { it.id == row.workplaceId }?.name
                        ?: neoLabel("غير محدد", "Unassigned")
                    val remaining = row.workplaceId?.let {
                        LocalizedNumbers.amount(remainingWorkEntitlement(db, it, month))
                    } ?: 0.0
                    Surface(
                        onClick = { if (row.workplaceId != null) onOpenWorkplace(row.workplaceId) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(label, modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Rounded.ChevronLeft, null,
                                    modifier = Modifier.size(19.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(neoLabel("عدد الورديات: ", "Shifts: ") + row.count,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(neoLabel("المستحق: ", "Expected: ") +
                                neoSalaryMoney(row.amount, moneyVisible),
                                style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (remaining <= 0.009 && row.amount > 0.0)
                                    neoLabel("تم استلام المستحقات بالكامل", "Fully received")
                                else neoLabel("المتبقي: ", "Remaining: ") +
                                    neoSalaryMoney(remaining, moneyVisible),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            1 -> {
                Text(neoLabel("سجل الدفعات", "Receipt history"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Text(neoLabel("المبالغ حسب شهر الاستحقاق، حتى لو تم الاستلام في تاريخ آخر",
                    "Receipts by earning month, regardless of received date"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                SalaryActionRow(pending > .009, { onReceive(null) }, onRecordPayment)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = onPaymentMethods, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CreditCard, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(neoLabel("طرق الدفع", "Methods"), maxLines = 1)
                    }
                    OutlinedButton(onClick = onIncomeSources, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.AccountBalance, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(neoLabel("مصادر الدخل", "Sources"), maxLines = 1)
                    }
                }
                OutlinedTextField(receiptSearch, { receiptSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(neoLabel("بحث في سجل الدفعات", "Find receipt")) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true)
                val visiblePayments = payments.filter { payment ->
                    if (receiptSearch.isBlank()) true
                    else {
                        val source = db.incomeSources.firstOrNull { it.id == payment.sourceId }
                        val allNames = payment.allocations.mapNotNull { allocation ->
                            db.incomeSources.firstOrNull { it.id == allocation.sourceId }?.name
                        }
                        (listOf(payment.receivedDate, payment.note, source?.name.orEmpty()) +
                            allNames).any { it.contains(receiptSearch, ignoreCase = true) }
                    }
                }
                if (visiblePayments.isEmpty())
                    SalaryEmpty(neoLabel("لا توجد دفعات مسجلة", "No receipts"),
                        neoLabel("ستظهر هنا الدفعات بعد تسجيلها", "Recorded receipts appear here"))
                visiblePayments.forEach { payment ->
                    val source = db.incomeSources.firstOrNull { it.id == payment.sourceId }
                    val method = db.paymentMethods.firstOrNull { it.id == payment.paymentMethodId }
                    val portions = NeoSalaryDashboardLogic.historicalPortionCents(payment, month)
                    val monthAmount = LocalizedNumbers.amount(portions)
                    val placeNames = (
                        listOfNotNull(source?.workplaceId) +
                        payment.allocations.mapNotNull { it.sourceId?.let { id ->
                            db.incomeSources.firstOrNull { src -> src.id == id }?.workplaceId
                        } }
                    ).distinct().mapNotNull { id -> workplaces.firstOrNull { it.id == id }?.name }
                    Surface(
                        onClick = { onPaymentDetails(payment.id) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(neoSalaryMoney(monthAmount, moneyVisible),
                                    modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                if (payment.reversedAt != null) Text(
                                    neoLabel("معكوسة • مبلغ أصلي", "Reversed • original"),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                            Text(neoLabel("تاريخ الاستلام: ", "Received on: ") + payment.receivedDate,
                                style = MaterialTheme.typography.bodySmall)
                            val description = (placeNames + listOfNotNull(method?.name)).joinToString(" • ")
                            if (description.isNotBlank()) Text(description,
                                style = MaterialTheme.typography.bodySmall)
                            if (payment.allocations.isNotEmpty()) {
                                Text(neoLabel("نصيب الشهر من الدفعة", "Allocated to this month"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (payment.note.isNotBlank()) Text(payment.note,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            2 -> {
                Text(neoLabel("كشف حساب مكان العمل", "Workplace statement"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(neoLabel("عرض المستحق والمستلم والتسويات لشهر الاستحقاق المختار",
                    "Earned, collected and adjusted for the selected accrual month"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (workplaces.isNotEmpty()) {
                    NeoChoice(
                        neoLabel("مكان العمل", "Workplace"), workplaces.map { it.id },
                        selectedWorkplace, { id -> workplaces.firstOrNull { it.id == id }?.name.orEmpty() },
                        { selectedWorkplace = it }
                    )
                    val key = selectedWorkplace
                    if (key != null) {
                        val due = summary.byWorkplace.firstOrNull { it.workplaceId == key }?.amount ?: 0.0
                        val paid = LocalizedNumbers.amount(
                            NeoSalaryDashboardLogic.workplaceReceivedCents(db, key, month))
                        val remaining = LocalizedNumbers.amount(
                            remainingWorkEntitlement(db, key, month))
                        Surface(color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SalaryStatementRow(neoLabel("المستحق", "Earned"), due, moneyVisible)
                                HorizontalDivider()
                                SalaryStatementRow(neoLabel("المستلم", "Collected"), paid, moneyVisible)
                                HorizontalDivider()
                                SalaryStatementRow(neoLabel("المتبقي", "Remaining"), remaining, moneyVisible)
                                if (remaining <= .009 && due > 0.0)
                                    Text(neoLabel("تم استلام المستحقات بالكامل", "Fully received"),
                                        color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Button(onClick = { onReceive(key) }, enabled = remaining > .009,
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Payments, null)
                            Spacer(Modifier.width(7.dp))
                            Text(neoLabel("استلام مستحقات الكشف", "Receive statement entitlements"))
                        }
                        OutlinedButton(onClick = { onStatement(key) },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.FileDownload, null)
                            Spacer(Modifier.width(7.dp))
                            Text(neoLabel("كشف تفصيلي وتصدير CSV", "Detailed statement & CSV"))
                        }
                    }
                } else {
                    SalaryEmpty(neoLabel("لا توجد أماكن عمل", "No workplaces"),
                        neoLabel("أضف مكان عمل أولًا", "Add a workplace first"))
                }
                Text(neoLabel("الدفعات العامة غير المنسوبة لمكان عمل لا تُوزّع تلقائيًا.",
                    "Unassigned general receipts are never distributed automatically."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                Text(neoLabel("مراجعة وإقفال الشهر", "Month review & close"),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (currentSnapshot != null) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            Icon(Icons.Rounded.Lock, null)
                            Text(neoLabel("الشهر مُقفل رسميًا", "Month officially closed"))
                        }
                    }
                }
                Surface(color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SalaryStatementRow(neoLabel("المستحق", "Earned"), expected, moneyVisible)
                        SalaryStatementRow(neoLabel("المستلم", "Collected"), summary.received, moneyVisible)
                        SalaryStatementRow(neoLabel("التسويات", "Adjustments"), summary.settlements, moneyVisible)
                        HorizontalDivider()
                        SalaryStatementRow(neoLabel("المتبقي", "Outstanding"), pending, moneyVisible)
                        Text(neoLabel("إجمالي الورديات: ", "Shift count: ") + summary.shiftCount,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                val health = remember(db) { ModernOperations.health(db) }
                if (health.isNotEmpty()) Text(
                    neoLabel("توجد ملاحظات لسلامة البيانات يجب مراجعتها قبل الإقفال.",
                        "Data-health issues require review before closing."),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                Text(neoLabel("الإقفال والتصحيح لهما تأكيد مستقل وسجل تدقيق؛ لا يتم تغيير الدفعات تلقائيًا.",
                    "Closing and reopening require confirmation and an audit trail; receipts are never changed automatically."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.FactCheck, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (currentSnapshot != null)
                        neoLabel("مراجعة الإقفال وإعادة الفتح", "Review or reopen")
                    else neoLabel("مراجعة الإقفال", "Review monthly closing"))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SalaryStatementRow(title: String, value: Double, visible: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(neoSalaryMoney(value, visible), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SalaryEmpty(title: String, description: String) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
