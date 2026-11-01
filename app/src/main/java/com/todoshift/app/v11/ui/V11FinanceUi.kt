package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.YearMonth
import java.util.UUID

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
        V11FinanceEngine.calculateMonth(
            db = db,
            month = month.toString()
        )
    }

    var paymentOpen by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onToggleMoney) {
                Text(
                    if (moneyVisible)
                        "إخفاء المبالغ"
                    else
                        "إظهار المبالغ"
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "المالية",
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Finance Center V2",
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
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
                Arrangement.spacedBy(8.dp)
        ) {
            FinanceCard(
                title = "المتبقي",
                value = result.difference,
                visible = moneyVisible,
                modifier = Modifier.weight(1f)
            )
            FinanceCard(
                title = "المستلم",
                value = result.received,
                visible = moneyVisible,
                modifier = Modifier.weight(1f)
            )
            FinanceCard(
                title = "المتوقع",
                value = result.expected,
                visible = moneyVisible,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    "الحالة",
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        result.received <= 0.0 &&
                            result.expected > 0.0 ->
                            "غير مدفوع"

                        result.received > 0.0 &&
                            result.received <
                            result.expected ->
                            "مدفوع جزئيا"

                        result.received >
                            result.expected ->
                            "دفع زائد"

                        result.expected > 0.0 &&
                            result.received >=
                            result.expected ->
                            "مدفوع بالكامل"

                        else -> "لا توجد مستحقات"
                    }
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "عدد الشيفتات: ${result.shiftCount}"
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                paymentOpen = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("إضافة دفعة")
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "حسب نوع الشيفت",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        result.byKind.forEach { (kind, amount) ->
            FinanceLine(
                title = V11Labels.shift(kind),
                value = amount,
                visible = moneyVisible
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "حسب مكان العمل",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        result.byWorkplace.forEach {
                (workplaceId, amount) ->

            FinanceLine(
                title = workplaces.firstOrNull {
                    it.id == workplaceId
                }?.name ?: "غير محدد",
                value = amount,
                visible = moneyVisible
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "الدفعات",
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        db.payments
            .filter {
                it.accrualMonth ==
                    month.toString()
            }
            .sortedByDescending {
                it.receivedDate
            }
            .forEach { payment ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (moneyVisible)
                                "${payment.amount} ج"
                            else
                                "••••••"
                        )

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {
                            Text(
                                payment.receivedDate,
                                fontWeight =
                                    FontWeight.Bold
                            )
                            if (
                                payment.note
                                    .isNotBlank()
                            ) {
                                Text(
                                    payment.note,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

        Spacer(Modifier.height(30.dp))
    }

    if (paymentOpen) {
        PaymentDialog(
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
}

@Composable
private fun FinanceCard(
    title: String,
    value: Double,
    visible: Boolean,
    modifier: Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (visible)
                    "${"%.2f".format(value)} ج"
                else
                    "••••••",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FinanceLine(
    title: String,
    value: Double,
    visible: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        Text(
            if (visible)
                "${"%.2f".format(value)} ج"
            else
                "••••••"
        )
        Text(
            title,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider()
}

@Composable
private fun PaymentDialog(
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
            java.time.LocalDate.now()
                .toString()
        )
    }
    var note by remember {
        mutableStateOf("")
    }
    var sourceId by remember {
        mutableStateOf(
            db.incomeSources
                .firstOrNull()?.id
        )
    }
    var methodId by remember {
        mutableStateOf(
            db.paymentMethods
                .firstOrNull()?.id
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("إضافة دفعة")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { c ->
                            c.isDigit() ||
                                c == '.'
                        }
                    },
                    label = {
                        Text("المبلغ")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

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

                Spacer(Modifier.height(10.dp))

                Text(
                    "مصدر الدخل",
                    fontWeight = FontWeight.Bold
                )

                db.incomeSources.forEach {
                    source ->
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
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

                Spacer(Modifier.height(8.dp))

                Text(
                    "طريقة الدفع",
                    fontWeight = FontWeight.Bold
                )

                db.paymentMethods.forEach {
                    method ->
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
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

                    val payment = PaymentV11(
                        id = UUID.randomUUID()
                            .toString(),
                        amount = value,
                        receivedDate = date,
                        accrualMonth =
                            month.toString(),
                        sourceId = sourceId,
                        paymentMethodId =
                            methodId,
                        note = note
                    )

                    onSave(
                        db.copy(
                            payments =
                                db.payments +
                                    payment,
                            auditLog =
                                db.auditLog +
                                    AuditEntryV11(
                                        id = UUID
                                            .randomUUID()
                                            .toString(),
                                        action =
                                            "PAYMENT_ADD",
                                        entityType =
                                            "payment",
                                        entityId =
                                            payment.id,
                                        description =
                                            "إضافة دفعة ${payment.amount}",
                                        createdAt =
                                            System
                                                .currentTimeMillis()
                                    )
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
