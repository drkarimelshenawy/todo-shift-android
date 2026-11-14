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
        note: String,
        recordId: String = UUID.randomUUID().toString(),
    ): V11Database {
        check(db.payments.none { it.id == recordId }) {
            "This request was already saved. Review the stored record before opening a new entry."
        }
        if (amount <= 0.0) return db

        val allocation =
            PaymentAllocationV11(
                id = UUID.randomUUID().toString(),
                accrualMonth = accrualMonth,
                sourceId = sourceId,
                amount = amount,
            )

        val payment =
            PaymentV11(
                id = recordId,
                amount = amount,
                receivedDate = receivedDate,
                accrualMonth = accrualMonth,
                sourceId = sourceId,
                paymentMethodId = paymentMethodId,
                note = note,
                allocations = listOf(allocation),
            )

        return db.copy(
            payments = db.payments + payment,
            auditLog =
                db.auditLog +
                    AuditEntryV11(
                        id = UUID.randomUUID().toString(),
                        action = "PAYMENT_ADD",
                        entityType = "payment",
                        entityId = payment.id,
                        description = "إضافة دفعة $amount",
                        createdAt = System.currentTimeMillis(),
                    ),
        )
    }

    fun addIncomeSource(
        db: V11Database,
        name: String,
        type: IncomeSourceTypeV11,
        fixedAmount: Double?,
        recordId: String = UUID.randomUUID().toString(),
        workplaceId: String? = null,
        calendarId: String? = null,
    ): V11Database {
        check(db.incomeSources.none { it.id == recordId }) {
            "This request was already saved. Review the stored record before opening a new entry."
        }
        if (name.isBlank()) return db
        require(fixedAmount == null || (fixedAmount.isFinite() && fixedAmount >= 0.0))

        return db.copy(
            incomeSources =
                db.incomeSources +
                    IncomeSourceV11(
                        id = recordId,
                        name = name.trim(),
                        type = if (fixedAmount != null) IncomeSourceTypeV11.FIXED_SALARY else type,
                        workplaceId = workplaceId,
                        calendarId = calendarId,
                        fixedMonthlyAmount = fixedAmount,
                        order = db.incomeSources.size,
                        compensationHistory = fixedAmount?.let { listOf(FixedSalaryPeriodV11(YearMonth.now().toString(), it)) } ?: emptyList(),
                    )
        )
    }

    fun updateIncomeSource(
        db: V11Database,
        source: IncomeSourceV11,
        effectiveMonth: String = YearMonth.now().toString(),
    ): V11Database {
        val original = db.incomeSources.firstOrNull { it.id == source.id } ?: return db
        val updated = if (original.fixedMonthlyAmount == source.fixedMonthlyAmount) {
            // History is owned by the controller, never by a stale editor copy.
            source.copy(compensationHistory = original.compensationHistory)
        } else {
            YearMonth.parse(effectiveMonth)
            require(db.monthSnapshots.none { it.reopenedAt == null && it.month >= effectiveMonth }) {
                "Historical closed month is protected"
            }
            val history = original.compensationHistory.ifEmpty {
                original.fixedMonthlyAmount?.let { listOf(FixedSalaryPeriodV11("0001-01", it)) } ?: emptyList()
            }
            require(history.none { it.effectiveMonth > effectiveMonth }) {
                "A later compensation period exists; review it before changing this month"
            }
            source.copy(compensationHistory = history.filterNot { it.effectiveMonth == effectiveMonth } +
                FixedSalaryPeriodV11(effectiveMonth, source.fixedMonthlyAmount ?: 0.0))
        }
        require(updated.fixedMonthlyAmount == null || (updated.fixedMonthlyAmount.isFinite() && updated.fixedMonthlyAmount >= 0.0))
        return db.copy(
            incomeSources = db.incomeSources.map { if (it.id == source.id) updated else it },
            auditLog = db.auditLog + AuditEntryV11(UUID.randomUUID().toString(), "INCOME_SOURCE_UPDATED", "INCOME_SOURCE", source.id,
                "Compensation effective $effectiveMonth; previous periods retained"),
        )
    }

    fun moveIncomeSource(db: V11Database, id: String, direction: Int): V11Database {
        val items = db.incomeSources.sortedBy { it.order }.toMutableList()

        val index = items.indexOfFirst { it.id == id }

        if (index < 0) return db

        val target = index + direction

        if (target !in items.indices) return db

        val tmp = items[index]
        items[index] = items[target]
        items[target] = tmp

        return db.copy(incomeSources = items.mapIndexed { order, item -> item.copy(order = order) })
    }

    fun deleteIncomeSource(db: V11Database, id: String): V11Database {
        val referenced = db.payments.any { it.sourceId == id || it.allocations.any { a -> a.sourceId == id } } ||
            db.settlements.any { it.sourceId == id } ||
            db.incomeSources.any { it.id == id && (it.fixedMonthlyAmount != null || it.compensationHistory.isNotEmpty()) }
        if (!referenced) return db.copy(incomeSources = db.incomeSources.filterNot { it.id == id })
        val source = db.incomeSources.first { it.id == id }
        // End future accrual from next month without altering this or any closed earning period.
        val stopMonth = YearMonth.now().plusMonths(1).toString()
        val history = source.compensationHistory.ifEmpty {
            source.fixedMonthlyAmount?.let { listOf(FixedSalaryPeriodV11("0001-01", it)) } ?: emptyList()
        }
        return db.copy(incomeSources = db.incomeSources.map {
            if (it.id == id) it.copy(active = false, compensationHistory =
                if (history.isEmpty()) history else history.filter { period -> period.effectiveMonth < stopMonth } + FixedSalaryPeriodV11(stopMonth, 0.0))
            else it
        })
    }

    fun addPaymentMethod(
        db: V11Database,
        name: String,
        recordId: String = UUID.randomUUID().toString(),
    ): V11Database {
        check(db.paymentMethods.none { it.id == recordId }) {
            "This request was already saved. Review the stored record before opening a new entry."
        }
        if (name.isBlank()) return db

        return db.copy(
            paymentMethods =
                db.paymentMethods +
                    PaymentMethodV11(
                        id = recordId,
                        name = name.trim(),
                        order = db.paymentMethods.size,
                    )
        )
    }

    fun updatePaymentMethod(db: V11Database, method: PaymentMethodV11): V11Database =
        db.copy(paymentMethods = db.paymentMethods.map { if (it.id == method.id) method else it })

    fun movePaymentMethod(db: V11Database, id: String, direction: Int): V11Database {
        val items = db.paymentMethods.sortedBy { it.order }.toMutableList()

        val index = items.indexOfFirst { it.id == id }

        if (index < 0) return db

        val target = index + direction

        if (target !in items.indices) return db

        val tmp = items[index]
        items[index] = items[target]
        items[target] = tmp

        return db.copy(
            paymentMethods = items.mapIndexed { order, item -> item.copy(order = order) }
        )
    }

    fun deletePaymentMethod(db: V11Database, id: String): V11Database =
        db.copy(paymentMethods = db.paymentMethods.filterNot { it.id == id })

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
        note: String,
        recordId: String = UUID.randomUUID().toString(),
    ): V11Database {
        check(db.obligations.none { it.id == recordId }) {
            "This request was already saved. Review the stored record before opening a new entry."
        }
        if (name.isBlank()) return db

        val item =
            ObligationV11(
                id = recordId,
                name = name.trim(),
                type = type,
                totalAmount = totalAmount,
                installmentAmount = installmentAmount,
                startDate = startDate,
                dueDay = dueDay,
                installmentCount = installmentCount,
                reminderDaysBefore = reminderDaysBefore,
                note = note,
            )

        return db.copy(
            obligations = db.obligations + item,
            auditLog =
                db.auditLog +
                    AuditEntryV11(
                        id = UUID.randomUUID().toString(),
                        action = "OBLIGATION_ADD",
                        entityType = "obligation",
                        entityId = item.id,
                        description = "إضافة التزام ${item.name}",
                    ),
        )
    }

    fun payObligation(
        db: V11Database,
        obligationId: String,
        month: String,
        amount: Double,
        methodId: String?,
        recordId: String = UUID.randomUUID().toString(),
    ): V11Database {
        check(db.obligationPayments.none { it.id == recordId }) {
            "This request was already saved. Review the stored record before opening a new entry."
        }
        if (amount <= 0.0) return db

        val obligation = db.obligations.firstOrNull { it.id == obligationId } ?: return db

        val ym = runCatching { YearMonth.parse(month) }.getOrNull() ?: return db

        val info = V11ObligationEngine.monthInfo(db, obligation, ym)

        if (info.expected <= 0.0 || info.remaining <= 0.0) {
            return db
        }

        val safeAmount = minOf(amount, info.remaining)

        val payment =
            ObligationPaymentV11(
                id = recordId,
                obligationId = obligationId,
                dueMonth = month,
                amount = safeAmount,
                paidDate = LocalDate.now().toString(),
                paymentMethodId = methodId,
            )

        var updated =
            db.copy(
                obligationPayments = db.obligationPayments + payment,
                auditLog =
                    db.auditLog +
                        AuditEntryV11(
                            id = UUID.randomUUID().toString(),
                            action = "OBLIGATION_PAYMENT",
                            entityType = "obligation",
                            entityId = obligationId,
                            description = "سداد ${safeAmount} للالتزام ${obligation.name}",
                        ),
            )

        val totalRemaining = obligationRemaining(updated, obligation)

        if (totalRemaining != null && totalRemaining <= 0.0001) {
            updated =
                updated.copy(
                    obligations =
                        updated.obligations.map {
                            if (it.id == obligationId) {
                                it.copy(status = ObligationStatusV11.PAID)
                            } else {
                                it
                            }
                        }
                )
        }

        return updated
    }

    fun obligationPaid(db: V11Database, id: String): Double =
        db.obligationPayments.filter { it.obligationId == id }.sumOf { it.amount }

    fun obligationRemaining(db: V11Database, obligation: ObligationV11): Double? {
        val total = obligation.totalAmount ?: return null

        return (total - obligationPaid(db, obligation.id)).coerceAtLeast(0.0)
    }

    fun monthObligationDue(db: V11Database, month: YearMonth): Double =
        V11ObligationEngine.totalDue(db, month)
}
