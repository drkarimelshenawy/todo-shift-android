package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun V11FinanceScreen(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    moneyVisible: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleMoney: () -> Unit,
    securitySettings: V11SecuritySettings,
    securityStore: V11SecurityStore,
    onDatabaseChange: (V11Database) -> Unit
) {
    val result = remember(db, month) {
        V11FinanceEngine.month(
            db = db,
            month = month
        )
    }

    var paymentOpen by remember {
        mutableStateOf(false)
    }

    var obligationOpen by remember {
        mutableStateOf(false)
    }

    var sourceManagerOpen by remember {
        mutableStateOf(false)
    }

    var methodManagerOpen by remember {
        mutableStateOf(false)
    }

    var payingObligationId by remember {
        mutableStateOf<String?>(null)
    }

    var workplaceDetailsId by remember {
        mutableStateOf<String?>(null)
    }

    var paymentDetails by remember {
        mutableStateOf<PaymentV11?>(null)
    }

    var editingPayment by remember {
        mutableStateOf<PaymentV11?>(null)
    }

    var editingObligation by remember {
        mutableStateOf<ObligationV11?>(null)
    }

    var pendingProtectedAction by remember {
        mutableStateOf<(() -> Unit)?>(null)
    }

    val obligationDue =
        V11AccountsController
            .monthObligationDue(
                db,
                month
            )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(14.dp)
    ) {
        if (LocalV11NeoEnabled.current) {
            V11NeoSectionHero("الملخص المالي", "الحسابات والمدفوعات • ${month}")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            FilledTonalButton(
                onClick = onPrevious
            ) {
                Text("‹", fontSize = 25.sp)
            }

            Text(
                month.toString(),
                fontWeight = FontWeight.Bold
            )

            FilledTonalButton(
                onClick = onNext
            ) {
                Text("›", fontSize = 25.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(7.dp)
        ) {
            AccountCard(
                "المتبقي",
                result.difference,
                moneyVisible,
                Modifier.weight(1f)
            )

            AccountCard(
                "المستلم",
                result.received,
                moneyVisible,
                Modifier.weight(1f)
            )

            AccountCard(
                "المستحق",
                result.expected,
                moneyVisible,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(10.dp))

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {
                Text(
                    "ملخص الشهر",
                    fontWeight =
                        FontWeight.Black
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                AccountLine(
                    "عدد الشيفتات",
                    result.shiftCount.toString()
                )

                AccountLine(
                    "حالة المستحقات",
                    when {
                        result.difference > 0 ->
                            "متبقي"
                        result.difference < 0 ->
                            "دفع زائد"
                        result.expected > 0 ->
                            "مسدد"
                        else ->
                            "لا توجد مستحقات"
                    }
                )

                AccountLine(
                    "التزامات الشهر",
                    money(
                        obligationDue,
                        moneyVisible
                    )
                )

                AccountLine(
                    "الصافي بعد الالتزامات",
                    money(
                        result.expected -
                            obligationDue,
                        moneyVisible
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                paymentOpen = true
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Add,
                null
            )
            Spacer(Modifier.width(5.dp))
            Text("تسجيل مبلغ مستلم")
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    sourceManagerOpen = true
                },
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("مصادر الدخل")
            }

            FilledTonalButton(
                onClick = {
                    methodManagerOpen = true
                },
                modifier =
                    Modifier.weight(1f)
            ) {
                Text("طرق الدفع")
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "المستحق حسب مكان العمل",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp
        )

        result.byWorkplace
            .forEach { summary ->
                val name =
                    workplaces
                        .firstOrNull {
                            it.id ==
                                summary
                                    .workplaceId
                        }
                        ?.name
                        ?: "غير محدد"

                Surface(
                    onClick = {
                        workplaceDetailsId =
                            summary.workplaceId
                    },
                    shape =
                        MaterialTheme.shapes.medium,
                    color =
                        MaterialTheme.colorScheme
                            .surfaceVariant
                            .copy(alpha = 0.45f),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 8.dp
                                ),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            money(
                                summary.amount,
                                moneyVisible
                            ),
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

        Spacer(Modifier.height(20.dp))

        Text(
            "المبالغ المستلمة",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp
        )

        val monthPayments =
            db.payments.filter { payment ->
                payment.accrualMonth ==
                    month.toString() ||
                    payment.allocations.any {
                        it.accrualMonth ==
                            month.toString()
                    }
            }

        if (monthPayments.isEmpty()) {
            Text(
                "لا توجد دفعات مسجلة",
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        vertical = 10.dp
                    )
            )
        }

        monthPayments
            .sortedByDescending {
                it.receivedDate
            }
            .forEach { payment ->
                val source =
                    db.incomeSources
                        .firstOrNull {
                            it.id ==
                                payment.sourceId
                        }

                val method =
                    db.paymentMethods
                        .firstOrNull {
                            it.id ==
                                payment
                                    .paymentMethodId
                        }

                Card(
                    onClick = {
                        paymentDetails = payment
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier =
                            Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement
                                    .SpaceBetween
                        ) {
                            Text(
                                money(
                                    payment.amount,
                                    moneyVisible
                                ),
                                fontWeight =
                                    FontWeight.Black
                            )

                            Text(
                                payment.receivedDate,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        Text(
                            listOfNotNull(
                                source?.name,
                                method?.name
                            ).joinToString(" • "),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )

                        if (
                            payment.note
                                .isNotBlank()
                        ) {
                            Text(
                                payment.note,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }
            }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = {
                    obligationOpen = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    null
                )
                Text("إضافة")
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    "الالتزامات",
                    fontSize = 19.sp,
                    fontWeight =
                        FontWeight.Black
                )

                Text(
                    "أقساط • ديون • مدفوعات دورية",
                    style =
                        MaterialTheme.typography
                            .labelSmall
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (db.obligations.isEmpty()) {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "لا توجد التزامات مسجلة",
                    modifier =
                        Modifier.padding(14.dp)
                )
            }
        }

        db.obligations.forEach {
            obligation ->

            val paid =
                V11AccountsController
                    .obligationPaid(
                        db,
                        obligation.id
                    )

            val totalRemaining =
                V11AccountsController
                    .obligationRemaining(
                        db,
                        obligation
                    )

            val monthInfo =
                V11ObligationEngine
                    .monthInfo(
                        db,
                        obligation,
                        month
                    )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {
                            Text(
                                obligation.name,
                                fontWeight =
                                    FontWeight.Black
                            )

                            Text(
                                obligationLabel(
                                    obligation.type
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                            )
                        }

                        IconButton(
                            onClick = {
                                editingObligation =
                                    obligation
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription =
                                    "تعديل الالتزام"
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    AccountLine(
                        "حالة هذا الشهر",
                        V11ObligationEngine
                            .stateLabel(
                                monthInfo.state
                            )
                    )

                    monthInfo.dueDate
                        ?.let {
                            AccountLine(
                                "تاريخ الاستحقاق",
                                it.toString()
                            )
                        }

                    if (
                        monthInfo.expected >
                        0.0
                    ) {
                        AccountLine(
                            "مستحق الشهر",
                            money(
                                monthInfo.expected,
                                moneyVisible
                            )
                        )

                        AccountLine(
                            "المسدد للشهر",
                            money(
                                monthInfo.paid,
                                moneyVisible
                            )
                        )

                        AccountLine(
                            "متبقي الشهر",
                            money(
                                monthInfo.remaining,
                                moneyVisible
                            )
                        )
                    }

                    AccountLine(
                        "إجمالي المدفوع",
                        money(
                            paid,
                            moneyVisible
                        )
                    )

                    totalRemaining?.let {
                        AccountLine(
                            "إجمالي المتبقي",
                            money(
                                it,
                                moneyVisible
                            )
                        )
                    }

                    Button(
                        onClick = {
                            payingObligationId =
                                obligation.id
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        enabled =
                            obligation.status ==
                                ObligationStatusV11
                                    .ACTIVE &&
                            monthInfo.remaining >
                                0.0
                    ) {
                        Text(
                            if (
                                monthInfo.paid >
                                0.0
                            ) {
                                "استكمال / سداد جزئي"
                            } else {
                                "تسجيل سداد"
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    workplaceDetailsId?.let { workplaceId ->
        V11WorkplaceDueDetailsDialog(
            workplaceId = workplaceId,
            month = month,
            db = db,
            workplaces = workplaces,
            moneyVisible = moneyVisible,
            onDismiss = {
                workplaceDetailsId = null
            }
        )
    }

    paymentDetails?.let { payment ->
        V11PaymentDetailsDialog(
            payment = payment,
            db = db,
            workplaceName = payment.sourceId
                ?.let { sourceId ->
                    db.incomeSources
                        .firstOrNull { it.id == sourceId }
                        ?.workplaceId
                }
                ?.let { workplaceId ->
                    workplaces
                        .firstOrNull { it.id == workplaceId }
                        ?.name
                },
            moneyVisible = moneyVisible,
            onDismiss = {
                paymentDetails = null
            },
            onEdit = {
                paymentDetails = null
                editingPayment = payment
            }
        )
    }

    editingPayment?.let { payment ->
        V11EditPaymentDialog(
            payment = payment,
            db = db,
            onDismiss = {
                editingPayment = null
            },
            onSave = { updated ->
                val saveAction = {
                    onDatabaseChange(
                        db.copy(
                            payments =
                                db.payments.map {
                                    if (it.id == updated.id)
                                        updated
                                    else
                                        it
                                }
                        )
                    )
                    editingPayment = null
                }

                val securityEnabled =
                    securitySettings.mode !=
                        V11LockMode.NONE ||
                        securitySettings.biometricEnabled

                if (securityEnabled) {
                    pendingProtectedAction =
                        saveAction
                } else {
                    saveAction()
                }
            }
        )
    }

    editingObligation?.let { obligation ->
        V11EditObligationDialog(
            obligation = obligation,
            onDismiss = {
                editingObligation = null
            },
            onSave = { updated ->
                val saveAction = {
                    onDatabaseChange(
                        db.copy(
                            obligations =
                                db.obligations.map {
                                    if (it.id == updated.id)
                                        updated
                                    else
                                        it
                                }
                        )
                    )
                    editingObligation = null
                }

                val securityEnabled =
                    securitySettings.mode !=
                        V11LockMode.NONE ||
                        securitySettings.biometricEnabled

                if (securityEnabled) {
                    pendingProtectedAction =
                        saveAction
                } else {
                    saveAction()
                }
            }
        )
    }

    pendingProtectedAction?.let { action ->
        V11FinancialVerificationDialog(
            settings = securitySettings,
            store = securityStore,
            onDismiss = {
                pendingProtectedAction = null
            },
            onVerified = {
                pendingProtectedAction = null
                action()
            }
        )
    }

    if (paymentOpen) {
        V11PaymentDialog(
            month = month,
            db = db,
            onDismiss = {
                paymentOpen = false
            },
            onSave = {
                onDatabaseChange(it)
                paymentOpen = false
            }
        )
    }

    if (obligationOpen) {
        V11ObligationDialog(
            db = db,
            onDismiss = {
                obligationOpen = false
            },
            onSave = {
                onDatabaseChange(it)
                obligationOpen = false
            }
        )
    }

    if (sourceManagerOpen) {
        V11IncomeSourcesDialog(
            db = db,
            onDismiss = {
                sourceManagerOpen = false
            },
            onDatabaseChange =
                onDatabaseChange
        )
    }

    if (methodManagerOpen) {
        V11PaymentMethodsDialog(
            db = db,
            onDismiss = {
                methodManagerOpen = false
            },
            onDatabaseChange =
                onDatabaseChange
        )
    }

    payingObligationId
        ?.let { obligationId ->

            val obligation =
                db.obligations
                    .firstOrNull {
                        it.id ==
                            obligationId
                    }

            if (obligation != null) {
                val info =
                    V11ObligationEngine
                        .monthInfo(
                            db,
                            obligation,
                            month
                        )

                V11ObligationPaymentDialog(
                    obligation = obligation,
                    month = month,
                    remaining =
                        info.remaining,
                    db = db,
                    onDismiss = {
                        payingObligationId =
                            null
                    },
                    onSave = {
                        amount,
                        methodId ->

                        onDatabaseChange(
                            V11AccountsController
                                .payObligation(
                                    db = db,
                                    obligationId =
                                        obligation.id,
                                    month =
                                        month.toString(),
                                    amount =
                                        amount,
                                    methodId =
                                        methodId
                                )
                        )

                        payingObligationId =
                            null
                    }
                )
            }
        }
}


@Composable
private fun V11WorkplaceDueDetailsDialog(
    workplaceId: String?,
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    moneyVisible: Boolean,
    onDismiss: () -> Unit
) {
    val workplaceName =
        workplaces.firstOrNull {
            it.id == workplaceId
        }?.name ?: "غير محدد"

    val rows =
        db.days
            .filter {
                it.date.startsWith(
                    month.toString()
                )
            }
            .flatMap { day ->
                day.segments.map {
                    day to it
                }
            }
            .filter {
                it.second.kind != ShiftKind.OFF &&
                    it.second.workplaceId ==
                        workplaceId
            }

    val groups =
        rows.groupBy {
            it.second.kind
        }

    val total =
        rows.sumOf { (day, segment) ->
            V11RateEngine.segmentAmount(
                segment = segment,
                date = day.date,
                rates = db.rates
            )
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(workplaceName)
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                AccountLine(
                    "عدد الشفتات",
                    rows.size.toString()
                )

                HorizontalDivider()

                ShiftKind.entries
                    .filter {
                        it != ShiftKind.OFF
                    }
                    .forEach { kind ->
                        val kindRows =
                            groups[kind]
                                .orEmpty()

                        if (kindRows.isNotEmpty()) {
                            val subtotal =
                                kindRows.sumOf {
                                    (day, segment) ->
                                    V11RateEngine
                                        .segmentAmount(
                                            segment,
                                            day.date,
                                            db.rates
                                        )
                                }

                            val unitRates =
                                kindRows.map {
                                    (day, segment) ->
                                    V11RateEngine
                                        .segmentAmount(
                                            segment,
                                            day.date,
                                            db.rates
                                        )
                                }.distinct()

                            Text(
                                shiftKindLabelV11(
                                    kind
                                ),
                                fontWeight =
                                    FontWeight.Black,
                                modifier =
                                    Modifier.padding(
                                        top = 10.dp
                                    )
                            )

                            AccountLine(
                                "العدد",
                                kindRows.size
                                    .toString()
                            )

                            AccountLine(
                                "قيمة الوحدة",
                                if (
                                    unitRates.size == 1
                                ) {
                                    money(
                                        unitRates.first(),
                                        moneyVisible
                                    )
                                } else {
                                    "متغيرة"
                                }
                            )

                            AccountLine(
                                "الإجمالي",
                                money(
                                    subtotal,
                                    moneyVisible
                                )
                            )
                        }
                    }

                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            vertical = 8.dp
                        )
                )

                AccountLine(
                    "الإجمالي الكلي",
                    money(
                        total,
                        moneyVisible
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun V11PaymentDetailsDialog(
    payment: PaymentV11,
    db: V11Database,
    workplaceName: String?,
    moneyVisible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val source =
        db.incomeSources
            .firstOrNull {
                it.id == payment.sourceId
            }

    val method =
        db.paymentMethods
            .firstOrNull {
                it.id ==
                    payment.paymentMethodId
            }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تفاصيل المبلغ المستلم")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                AccountLine(
                    "المبلغ",
                    money(
                        payment.amount,
                        moneyVisible
                    )
                )

                AccountLine(
                    "تاريخ الاستلام",
                    payment.receivedDate
                )

                AccountLine(
                    "شهر الاستحقاق",
                    payment.accrualMonth
                )

                source?.let {
                    AccountLine(
                        "مصدر الدخل",
                        it.name
                    )
                }

                workplaceName?.let { name ->
                    AccountLine(
                        "مكان العمل",
                        name
                    )
                }

                method?.let {
                    AccountLine(
                        "طريقة الدفع",
                        it.name
                    )
                }

                if (
                    payment.note.isNotBlank()
                ) {
                    AccountLine(
                        "ملاحظة",
                        payment.note
                    )
                }

                if (
                    payment.allocations
                        .isNotEmpty()
                ) {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        "توزيع الدفعة",
                        fontWeight =
                            FontWeight.Black
                    )

                    payment.allocations
                        .forEach {
                            AccountLine(
                                it.accrualMonth,
                                money(
                                    it.amount,
                                    moneyVisible
                                )
                            )
                        }
                }
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    null
                )
                Spacer(Modifier.width(5.dp))
                Text("تعديل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}

@Composable
private fun V11EditPaymentDialog(
    payment: PaymentV11,
    db: V11Database,
    onDismiss: () -> Unit,
    onSave: (PaymentV11) -> Unit
) {
    var amount by remember(payment.id) {
        mutableStateOf(
            payment.amount.toString()
        )
    }

    var date by remember(payment.id) {
        mutableStateOf(
            payment.receivedDate
        )
    }

    var note by remember(payment.id) {
        mutableStateOf(payment.note)
    }

    var sourceId by remember(payment.id) {
        mutableStateOf(payment.sourceId)
    }

    var methodId by remember(payment.id) {
        mutableStateOf(
            payment.paymentMethodId
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل المبلغ المستلم")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text("المبلغ")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                    },
                    label = {
                        Text("تاريخ الاستلام")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "مصدر الدخل",
                    fontWeight =
                        FontWeight.Bold
                )

                db.incomeSources
                    .filter {
                        it.active ||
                            it.id == sourceId
                    }
                    .sortedBy { it.order }
                    .forEach { source ->
                        Row(
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    sourceId ==
                                        source.id,
                                onClick = {
                                    sourceId =
                                        source.id
                                }
                            )
                            Text(source.name)
                        }
                    }

                Text(
                    "طريقة الدفع",
                    fontWeight =
                        FontWeight.Bold
                )

                db.paymentMethods
                    .filter {
                        it.active ||
                            it.id == methodId
                    }
                    .sortedBy { it.order }
                    .forEach { method ->
                        Row(
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    methodId ==
                                        method.id,
                                onClick = {
                                    methodId =
                                        method.id
                                }
                            )
                            Text(method.name)
                        }
                    }

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("ملاحظة")
                    }
                )

                if (
                    payment.allocations
                        .isNotEmpty()
                ) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "سيتم الحفاظ على توزيع الدفعة الحالي.",
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newAmount =
                        amount.toDoubleOrNull()
                            ?: return@Button

                    val oldAmount =
                        payment.amount

                    val newAllocations =
                        if (
                            payment.allocations
                                .isEmpty() ||
                            oldAmount <= 0.0
                        ) {
                            payment.allocations
                        } else {
                            val ratio =
                                newAmount /
                                    oldAmount

                            payment.allocations
                                .map {
                                    it.copy(
                                        amount =
                                            it.amount *
                                                ratio
                                    )
                                }
                        }

                    onSave(
                        payment.copy(
                            amount = newAmount,
                            receivedDate = date,
                            sourceId = sourceId,
                            paymentMethodId =
                                methodId,
                            note = note,
                            allocations =
                                newAllocations
                        )
                    )
                },
                enabled =
                    amount.toDoubleOrNull() !=
                        null
            ) {
                Text("حفظ التعديل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun V11EditObligationDialog(
    obligation: ObligationV11,
    onDismiss: () -> Unit,
    onSave: (ObligationV11) -> Unit
) {
    var name by remember(obligation.id) {
        mutableStateOf(obligation.name)
    }

    var total by remember(obligation.id) {
        mutableStateOf(
            obligation.totalAmount
                ?.toString()
                .orEmpty()
        )
    }

    var installment by remember(
        obligation.id
    ) {
        mutableStateOf(
            obligation.installmentAmount
                ?.toString()
                .orEmpty()
        )
    }

    var dueDay by remember(obligation.id) {
        mutableStateOf(
            obligation.dueDay
                ?.toString()
                .orEmpty()
        )
    }

    var count by remember(obligation.id) {
        mutableStateOf(
            obligation.installmentCount
                ?.toString()
                .orEmpty()
        )
    }

    var note by remember(obligation.id) {
        mutableStateOf(obligation.note)
    }

    var type by remember(obligation.id) {
        mutableStateOf(obligation.type)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل الالتزام")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("اسم الالتزام")
                    }
                )

                OutlinedTextField(
                    value = total,
                    onValueChange = {
                        total =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text("إجمالي المبلغ")
                    }
                )

                OutlinedTextField(
                    value = installment,
                    onValueChange = {
                        installment =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text("قيمة القسط")
                    }
                )

                OutlinedTextField(
                    value = dueDay,
                    onValueChange = {
                        dueDay =
                            it.filter(
                                Char::isDigit
                            )
                    },
                    label = {
                        Text("يوم الاستحقاق")
                    }
                )

                OutlinedTextField(
                    value = count,
                    onValueChange = {
                        count =
                            it.filter(
                                Char::isDigit
                            )
                    },
                    label = {
                        Text("عدد الأقساط")
                    }
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("ملاحظة")
                    }
                )

                ObligationTypeV11.entries
                    .forEach { item ->
                        Row(
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    type == item,
                                onClick = {
                                    type = item
                                }
                            )
                            Text(
                                obligationLabel(
                                    item
                                )
                            )
                        }
                    }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        obligation.copy(
                            name = name,
                            type = type,
                            totalAmount =
                                total.toDoubleOrNull(),
                            installmentAmount =
                                installment
                                    .toDoubleOrNull(),
                            dueDay =
                                dueDay.toIntOrNull()
                                    ?.coerceIn(
                                        1,
                                        31
                                    ),
                            installmentCount =
                                count.toIntOrNull(),
                            note = note
                        )
                    )
                },
                enabled =
                    name.isNotBlank()
            ) {
                Text("حفظ التعديل")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

private fun shiftKindLabelV11(
    kind: ShiftKind
): String =
    when (kind) {
        ShiftKind.FULL_DAY -> "يوم كامل"
        ShiftKind.LONG -> "طويل"
        ShiftKind.NIGHT -> "ليلي"
        ShiftKind.MORNING -> "صباحي"
        ShiftKind.EVENING -> "مسائي"
        ShiftKind.OFF -> "إجازة"
    }

@Composable
private fun AccountCard(
    title: String,
    value: Double,
    visible: Boolean,
    modifier: Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 11.sp
            )

            Text(
                money(
                    value,
                    visible
                ),
                fontWeight =
                    FontWeight.Black,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun AccountLine(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(value)

        Text(
            title,
            fontWeight =
                FontWeight.Bold
        )
    }

    HorizontalDivider()
}

private fun money(
    value: Double,
    visible: Boolean
): String =
    if (visible)
        "${"%.2f".format(value)} ج"
    else
        "••••••"

@Composable
private fun V11PaymentDialog(
    month: YearMonth,
    db: V11Database,
    onDismiss: () -> Unit,
    onSave: (V11Database) -> Unit
) {
    var amount by remember {
        mutableStateOf("")
    }

    var date by remember {
        mutableStateOf(
            LocalDate.now()
                .toString()
        )
    }

    var note by remember {
        mutableStateOf("")
    }

    var sourceId by remember {
        mutableStateOf(
            db.incomeSources
                .firstOrNull {
                    it.active
                }?.id
        )
    }

    var methodId by remember {
        mutableStateOf(
            db.paymentMethods
                .firstOrNull {
                    it.active
                }?.id
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تسجيل مبلغ مستلم")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text("المبلغ")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                    },
                    label = {
                        Text("تاريخ الاستلام")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "مصدر الدخل",
                    fontWeight =
                        FontWeight.Bold
                )

                db.incomeSources
                    .filter { it.active }
                    .sortedBy { it.order }
                    .forEach { source ->
                        Row(
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    sourceId ==
                                        source.id,
                                onClick = {
                                    sourceId =
                                        source.id
                                }
                            )

                            Text(source.name)
                        }
                    }

                Text(
                    "طريقة الدفع",
                    fontWeight =
                        FontWeight.Bold
                )

                db.paymentMethods
                    .filter { it.active }
                    .sortedBy { it.order }
                    .forEach { method ->
                        Row(
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    methodId ==
                                        method.id,
                                onClick = {
                                    methodId =
                                        method.id
                                }
                            )

                            Text(method.name)
                        }
                    }

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("ملاحظة")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value =
                        amount.toDoubleOrNull()
                            ?: return@Button

                    onSave(
                        V11AccountsController
                            .addPayment(
                                db = db,
                                amount = value,
                                receivedDate =
                                    date,
                                accrualMonth =
                                    month.toString(),
                                sourceId =
                                    sourceId,
                                paymentMethodId =
                                    methodId,
                                note = note
                            )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun V11IncomeSourcesDialog(
    db: V11Database,
    onDismiss: () -> Unit,
    onDatabaseChange: (V11Database) -> Unit
) {
    var newName by remember {
        mutableStateOf("")
    }

    var fixedAmount by remember {
        mutableStateOf("")
    }

    var editing by remember {
        mutableStateOf<IncomeSourceV11?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("مصادر الدخل")
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {
                val ordered =
                    db.incomeSources
                        .sortedBy { it.order }

                ordered.forEachIndexed {
                    index,
                    source ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 3.dp
                                )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(7.dp)
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        onDatabaseChange(
                                            V11AccountsController
                                                .deleteIncomeSource(
                                                    db,
                                                    source.id
                                                )
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "حذف"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        editing =
                                            source
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        "تعديل"
                                    )
                                }

                                Spacer(
                                    Modifier.weight(1f)
                                )

                                Column(
                                    horizontalAlignment =
                                        Alignment.End
                                ) {
                                    Text(
                                        source.name,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    source
                                        .fixedMonthlyAmount
                                        ?.let {
                                            Text(
                                                "ثابت شهريا: %.2f ج"
                                                    .format(it),
                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .labelSmall
                                            )
                                        }
                                }
                            }

                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        onDatabaseChange(
                                            V11AccountsController
                                                .moveIncomeSource(
                                                    db,
                                                    source.id,
                                                    -1
                                                )
                                        )
                                    },
                                    enabled =
                                        index > 0
                                ) {
                                    Icon(
                                        Icons.Default
                                            .KeyboardArrowUp,
                                        "لأعلى"
                                    )
                                }

                                Text(
                                    "ترتيب ${index + 1}",
                                    modifier =
                                        Modifier.align(
                                            Alignment
                                                .CenterVertically
                                        ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall
                                )

                                IconButton(
                                    onClick = {
                                        onDatabaseChange(
                                            V11AccountsController
                                                .moveIncomeSource(
                                                    db,
                                                    source.id,
                                                    1
                                                )
                                        )
                                    },
                                    enabled =
                                        index <
                                        ordered.lastIndex
                                ) {
                                    Icon(
                                        Icons.Default
                                            .KeyboardArrowDown,
                                        "لأسفل"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    "إضافة مصدر دخل",
                    fontWeight =
                        FontWeight.Black
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                    },
                    label = {
                        Text("اسم المصدر")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fixedAmount,
                    onValueChange = {
                        fixedAmount = it
                    },
                    label = {
                        Text(
                            "راتب/دخل شهري ثابت - اختياري"
                        )
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (
                            newName.isNotBlank()
                        ) {
                            onDatabaseChange(
                                V11AccountsController
                                    .addIncomeSource(
                                        db = db,
                                        name =
                                            newName.trim(),
                                        type =
                                            IncomeSourceTypeV11
                                                .OTHER,
                                        fixedAmount =
                                            fixedAmount
                                                .replace(
                                                    ",",
                                                    "."
                                                )
                                                .toDoubleOrNull()
                                    )
                            )

                            newName = ""
                            fixedAmount = ""
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("إضافة المصدر")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("تم")
            }
        }
    )

    editing?.let { source ->
        V11EditIncomeSourceDialog(
            source = source,
            onDismiss = {
                editing = null
            },
            onSave = {
                onDatabaseChange(
                    V11AccountsController
                        .updateIncomeSource(
                            db,
                            it
                        )
                )
                editing = null
            }
        )
    }
}

@Composable
private fun V11EditIncomeSourceDialog(
    source: IncomeSourceV11,
    onDismiss: () -> Unit,
    onSave: (IncomeSourceV11) -> Unit
) {
    var name by remember(source.id) {
        mutableStateOf(source.name)
    }

    var amount by remember(source.id) {
        mutableStateOf(
            source.fixedMonthlyAmount
                ?.toString()
                ?: ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تعديل مصدر الدخل")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("الاسم")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    label = {
                        Text(
                            "الدخل الشهري الثابت"
                        )
                    },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        source.copy(
                            name =
                                name.trim(),
                            fixedMonthlyAmount =
                                amount
                                    .replace(
                                        ",",
                                        "."
                                    )
                                    .toDoubleOrNull()
                        )
                    )
                },
                enabled =
                    name.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun V11PaymentMethodsDialog(
    db: V11Database,
    onDismiss: () -> Unit,
    onDatabaseChange: (V11Database) -> Unit
) {
    var newName by remember {
        mutableStateOf("")
    }

    var editing by remember {
        mutableStateOf<PaymentMethodV11?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("طرق الدفع")
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {
                val ordered =
                    db.paymentMethods
                        .sortedBy { it.order }

                ordered.forEachIndexed {
                    index,
                    method ->

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 3.dp
                                )
                    ) {
                        Row(
                            modifier =
                                Modifier.padding(5.dp),
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11AccountsController
                                            .deletePaymentMethod(
                                                db,
                                                method.id
                                            )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "حذف"
                                )
                            }

                            IconButton(
                                onClick = {
                                    editing =
                                        method
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "تعديل"
                                )
                            }

                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11AccountsController
                                            .movePaymentMethod(
                                                db,
                                                method.id,
                                                -1
                                            )
                                    )
                                },
                                enabled =
                                    index > 0
                            ) {
                                Icon(
                                    Icons.Default
                                        .KeyboardArrowUp,
                                    "لأعلى"
                                )
                            }

                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11AccountsController
                                            .movePaymentMethod(
                                                db,
                                                method.id,
                                                1
                                            )
                                    )
                                },
                                enabled =
                                    index <
                                    ordered.lastIndex
                            ) {
                                Icon(
                                    Icons.Default
                                        .KeyboardArrowDown,
                                    "لأسفل"
                                )
                            }

                            Spacer(
                                Modifier.weight(1f)
                            )

                            Text(
                                method.name,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                    },
                    label = {
                        Text("طريقة دفع جديدة")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (
                            newName.isNotBlank()
                        ) {
                            onDatabaseChange(
                                V11AccountsController
                                    .addPaymentMethod(
                                        db,
                                        newName.trim()
                                    )
                            )
                            newName = ""
                        }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("إضافة طريقة الدفع")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("تم")
            }
        }
    )

    editing?.let { method ->
        var name by remember(method.id) {
            mutableStateOf(method.name)
        }

        AlertDialog(
            onDismissRequest = {
                editing = null
            },
            title = {
                Text("تعديل طريقة الدفع")
            },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("الاسم")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (
                            name.isNotBlank()
                        ) {
                            onDatabaseChange(
                                V11AccountsController
                                    .updatePaymentMethod(
                                        db,
                                        method.copy(
                                            name =
                                                name.trim()
                                        )
                                    )
                            )
                            editing = null
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editing = null
                    }
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun V11ObligationDialog(
    db: V11Database,
    onDismiss: () -> Unit,
    onSave: (V11Database) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var total by remember {
        mutableStateOf("")
    }

    var installment by remember {
        mutableStateOf("")
    }

    var dueDay by remember {
        mutableStateOf("")
    }

    var count by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf(
            ObligationTypeV11.INSTALLMENT
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إضافة التزام")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("اسم الالتزام")
                    }
                )

                OutlinedTextField(
                    value = total,
                    onValueChange = {
                        total =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text("إجمالي المبلغ")
                    }
                )

                OutlinedTextField(
                    value = installment,
                    onValueChange = {
                        installment =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text("قيمة القسط")
                    }
                )

                OutlinedTextField(
                    value = dueDay,
                    onValueChange = {
                        dueDay =
                            it.filter(
                                Char::isDigit
                            )
                    },
                    label = {
                        Text(
                            "يوم الاستحقاق"
                        )
                    }
                )

                OutlinedTextField(
                    value = count,
                    onValueChange = {
                        count =
                            it.filter(
                                Char::isDigit
                            )
                    },
                    label = {
                        Text("عدد الأقساط")
                    }
                )

                Spacer(Modifier.height(8.dp))

                ObligationTypeV11.entries
                    .forEach {
                        Row(
                            verticalAlignment =
                                Alignment
                                    .CenterVertically
                        ) {
                            RadioButton(
                                selected =
                                    type == it,
                                onClick = {
                                    type = it
                                }
                            )

                            Text(
                                obligationLabel(
                                    it
                                )
                            )
                        }
                    }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        V11AccountsController
                            .addObligation(
                                db = db,
                                name = name,
                                type = type,
                                totalAmount =
                                    total
                                        .toDoubleOrNull(),
                                installmentAmount =
                                    installment
                                        .toDoubleOrNull(),
                                startDate =
                                    LocalDate.now()
                                        .toString(),
                                dueDay =
                                    dueDay
                                        .toIntOrNull()
                                        ?.coerceIn(
                                            1,
                                            31
                                        ),
                                installmentCount =
                                    count
                                        .toIntOrNull(),
                                reminderDaysBefore =
                                    3,
                                note = ""
                            )
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

private fun obligationLabel(
    type: ObligationTypeV11
): String =
    when (type) {
        ObligationTypeV11.INSTALLMENT ->
            "قسط"
        ObligationTypeV11.DEBT ->
            "دين"
        ObligationTypeV11.LOAN ->
            "قرض"
        ObligationTypeV11.CREDIT_CARD ->
            "بطاقة ائتمان"
        ObligationTypeV11.ASSOCIATION ->
            "جمعية"
        ObligationTypeV11.RECURRING_BILL ->
            "مدفوع دوري"
        ObligationTypeV11.OTHER ->
            "أخرى"
    }


@Composable
private fun V11ObligationPaymentDialog(
    obligation: ObligationV11,
    month: YearMonth,
    remaining: Double,
    db: V11Database,
    onDismiss: () -> Unit,
    onSave: (
        Double,
        String?
    ) -> Unit
) {
    var amount by remember(
        obligation.id,
        month
    ) {
        mutableStateOf(
            if (remaining > 0.0)
                "%.2f".format(remaining)
            else
                ""
        )
    }

    var methodId by remember {
        mutableStateOf(
            db.paymentMethods
                .firstOrNull {
                    it.active
                }
                ?.id
        )
    }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                "سداد ${obligation.name}"
            )
        },
        text = {
            Column {
                Text(
                    "المتبقي لهذا الشهر: ${
                        "%.2f".format(
                            remaining
                        )
                    } ج"
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    label = {
                        Text("المبلغ")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (
                    db.paymentMethods
                        .isNotEmpty()
                ) {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        "طريقة الدفع",
                        fontWeight =
                            FontWeight.Bold
                    )

                    db.paymentMethods
                        .filter {
                            it.active
                        }
                        .forEach {
                            method ->

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(),
                                verticalAlignment =
                                    Alignment
                                        .CenterVertically
                            ) {
                                RadioButton(
                                    selected =
                                        methodId ==
                                            method.id,
                                    onClick = {
                                        methodId =
                                            method.id
                                    }
                                )

                                Text(
                                    method.name
                                )
                            }
                        }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed =
                        amount
                            .replace(
                                ",",
                                "."
                            )
                            .toDoubleOrNull()
                            ?: 0.0

                    onSave(
                        parsed,
                        methodId
                    )
                },
                enabled =
                    (
                        amount
                            .replace(
                                ",",
                                "."
                            )
                            .toDoubleOrNull()
                            ?: 0.0
                    ) > 0.0
            ) {
                Text("حفظ السداد")
            }
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}
