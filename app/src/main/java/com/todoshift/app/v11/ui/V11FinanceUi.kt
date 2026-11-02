package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onToggleMoney
            ) {
                Text(
                    if (moneyVisible)
                        "إخفاء المبالغ"
                    else
                        "إظهار المبالغ"
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    "الحسابات",
                    fontSize = 25.sp,
                    fontWeight =
                        FontWeight.Black
                )

                Text(
                    "المستحقات والمدفوعات والالتزامات",
                    style =
                        MaterialTheme.typography
                            .labelSmall
                )
            }
        }

        Spacer(Modifier.height(10.dp))

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

                AccountLine(
                    name,
                    money(
                        summary.amount,
                        moneyVisible
                    )
                )
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
    onDatabaseChange:
        (V11Database) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    var fixed by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("مصادر الدخل")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                db.incomeSources
                    .sortedBy { it.order }
                    .forEach { source ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
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

                            Spacer(
                                Modifier.weight(1f)
                            )

                            Text(
                                source.name,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                HorizontalDivider()

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("مصدر جديد")
                    }
                )

                OutlinedTextField(
                    value = fixed,
                    onValueChange = {
                        fixed =
                            it.filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text(
                            "مرتب ثابت شهري - اختياري"
                        )
                    }
                )

                Button(
                    onClick = {
                        onDatabaseChange(
                            V11AccountsController
                                .addIncomeSource(
                                    db,
                                    name,
                                    if (
                                        fixed
                                            .toDoubleOrNull() !=
                                        null
                                    )
                                        IncomeSourceTypeV11
                                            .FIXED_SALARY
                                    else
                                        IncomeSourceTypeV11
                                            .OTHER,
                                    fixed
                                        .toDoubleOrNull()
                                )
                        )

                        name = ""
                        fixed = ""
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
}

@Composable
private fun V11PaymentMethodsDialog(
    db: V11Database,
    onDismiss: () -> Unit,
    onDatabaseChange:
        (V11Database) -> Unit
) {
    var name by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("طرق الدفع")
        },
        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                db.paymentMethods
                    .sortedBy { it.order }
                    .forEach { method ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
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

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("طريقة دفع جديدة")
                    }
                )

                Button(
                    onClick = {
                        onDatabaseChange(
                            V11AccountsController
                                .addPaymentMethod(
                                    db,
                                    name
                                )
                        )

                        name = ""
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text("إضافة")
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
