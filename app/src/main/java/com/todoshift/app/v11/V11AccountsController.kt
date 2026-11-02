package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object V11AccountsController {

    fun addPayment(
        db: V11Database,
        amount: Double,
        receivedDate: String,
        accrualMonth: String,
        sourceId: String?,
        paymentMethodId: String?,
        note: String
    ): V11Database {
        if (amount <= 0.0) return db

        val allocation = PaymentAllocationV11(
            id = UUID.randomUUID().toString(),
            accrualMonth = accrualMonth,
            sourceId = sourceId,
            amount = amount
        )

        val payment = PaymentV11(
            id = UUID.randomUUID().toString(),
            amount = amount,
            receivedDate = receivedDate,
            accrualMonth = accrualMonth,
            sourceId = sourceId,
            paymentMethodId = paymentMethodId,
            note = note,
            allocations = listOf(allocation)
        )

        return db.copy(
            payments = db.payments + payment,
            auditLog = db.auditLog + AuditEntryV11(
                id = UUID.randomUUID().toString(),
                action = "PAYMENT_ADD",
                entityType = "payment",
                entityId = payment.id,
                description = "إضافة دفعة $amount",
                createdAt = System.currentTimeMillis()
            )
        )
    }

    fun addIncomeSource(
        db: V11Database,
        name: String,
        type: IncomeSourceTypeV11,
        fixedAmount: Double?
    ): V11Database {
        if (name.isBlank()) return db

        return db.copy(
            incomeSources = db.incomeSources +
                IncomeSourceV11(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    type = type,
                    fixedMonthlyAmount = fixedAmount,
                    order = db.incomeSources.size
                )
        )
    }

    fun deleteIncomeSource(
        db: V11Database,
        id: String
    ): V11Database =
        db.copy(
            incomeSources =
                db.incomeSources.filterNot {
                    it.id == id
                }
        )

    fun addPaymentMethod(
        db: V11Database,
        name: String
    ): V11Database {
        if (name.isBlank()) return db

        return db.copy(
            paymentMethods =
                db.paymentMethods +
                    PaymentMethodV11(
                        id = UUID.randomUUID()
                            .toString(),
                        name = name.trim(),
                        order =
                            db.paymentMethods.size
                    )
        )
    }

    fun deletePaymentMethod(
        db: V11Database,
        id: String
    ): V11Database =
        db.copy(
            paymentMethods =
                db.paymentMethods.filterNot {
                    it.id == id
                }
        )

    fun addObligation(
        db: V11Database,
        name: String,
        type: ObligationTypeV11,
        totalAmount: Double?,
        installmentAmount: Double?,
        startDate: String,
        dueDay: Int?,
        installmentCount: Int?,
        reminderDaysBefore: Int,
        note: String
    ): V11Database {
        if (name.isBlank()) return db

        val item = ObligationV11(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            type = type,
            totalAmount = totalAmount,
            installmentAmount = installmentAmount,
            startDate = startDate,
            dueDay = dueDay,
            installmentCount = installmentCount,
            reminderDaysBefore =
                reminderDaysBefore,
            note = note
        )

        return db.copy(
            obligations =
                db.obligations + item,
            auditLog =
                db.auditLog +
                    AuditEntryV11(
                        id = UUID.randomUUID()
                            .toString(),
                        action =
                            "OBLIGATION_ADD",
                        entityType =
                            "obligation",
                        entityId = item.id,
                        description =
                            "إضافة التزام ${item.name}"
                    )
        )
    }

    fun payObligation(
        db: V11Database,
        obligationId: String,
        month: String,
        amount: Double,
        methodId: String?
    ): V11Database {
        if (amount <= 0.0) return db

        val obligation =
            db.obligations
                .firstOrNull {
                    it.id == obligationId
                }
                ?: return db

        val ym =
            runCatching {
                YearMonth.parse(month)
            }.getOrNull()
                ?: return db

        val info =
            V11ObligationEngine
                .monthInfo(
                    db,
                    obligation,
                    ym
                )

        if (
            info.expected <= 0.0 ||
            info.remaining <= 0.0
        ) {
            return db
        }

        val safeAmount =
            minOf(
                amount,
                info.remaining
            )

        val payment =
            ObligationPaymentV11(
                id = UUID.randomUUID()
                    .toString(),
                obligationId =
                    obligationId,
                dueMonth =
                    month,
                amount =
                    safeAmount,
                paidDate =
                    LocalDate.now()
                        .toString(),
                paymentMethodId =
                    methodId
            )

        var updated =
            db.copy(
                obligationPayments =
                    db.obligationPayments +
                        payment,
                auditLog =
                    db.auditLog +
                        AuditEntryV11(
                            id = UUID.randomUUID()
                                .toString(),
                            action =
                                "OBLIGATION_PAYMENT",
                            entityType =
                                "obligation",
                            entityId =
                                obligationId,
                            description =
                                "سداد ${safeAmount} للالتزام ${obligation.name}"
                        )
            )

        val totalRemaining =
            obligationRemaining(
                updated,
                obligation
            )

        if (
            totalRemaining != null &&
            totalRemaining <= 0.0001
        ) {
            updated =
                updated.copy(
                    obligations =
                        updated.obligations
                            .map {
                                if (
                                    it.id ==
                                    obligationId
                                ) {
                                    it.copy(
                                        status =
                                            ObligationStatusV11
                                                .PAID
                                    )
                                } else {
                                    it
                                }
                            }
                )
        }

        return updated
    }

    fun obligationPaid(
        db: V11Database,
        id: String
    ): Double =
        db.obligationPayments
            .filter {
                it.obligationId == id
            }
            .sumOf { it.amount }

    fun obligationRemaining(
        db: V11Database,
        obligation: ObligationV11
    ): Double? {
        val total =
            obligation.totalAmount
                ?: return null

        return (
            total -
                obligationPaid(
                    db,
                    obligation.id
                )
            ).coerceAtLeast(0.0)
    }

    fun monthObligationDue(
        db: V11Database,
        month: YearMonth
    ): Double =
        V11ObligationEngine
            .totalDue(
                db,
                month
            )
}
