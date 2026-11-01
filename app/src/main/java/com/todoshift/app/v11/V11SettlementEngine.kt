package com.todoshift.app.v11

import java.util.UUID

object V11SettlementEngine {

    fun addPayment(
        db: V11Database,
        amount: Double,
        receivedDate: String,
        accrualMonth: String,
        sourceId: String?,
        paymentMethodId: String?,
        note: String = "",
        linkedShiftIds: List<String> = emptyList()
    ): V11Database {

        require(amount >= 0.0)

        val payment = PaymentV11(
            id = UUID.randomUUID().toString(),
            amount = amount,
            receivedDate = receivedDate,
            accrualMonth = accrualMonth,
            sourceId = sourceId,
            paymentMethodId = paymentMethodId,
            note = note,
            linkedShiftIds = linkedShiftIds
        )

        return db.copy(
            payments = db.payments + payment,
            auditLog = db.auditLog + AuditEntryV11(
                id = UUID.randomUUID().toString(),
                action = "PAYMENT_CREATED",
                entityType = "PAYMENT",
                entityId = payment.id,
                description =
                    "Payment recorded for $accrualMonth"
            )
        )
    }

    fun settleDifference(
        db: V11Database,
        month: String,
        amount: Double,
        reason: String,
        sourceId: String? = null,
        workplaceId: String? = null,
        calendarId: String? = null
    ): V11Database {

        require(amount >= 0.0)

        val settlement = SettlementV11(
            id = UUID.randomUUID().toString(),
            accrualMonth = month,
            sourceId = sourceId,
            workplaceId = workplaceId,
            calendarId = calendarId,
            action = SettlementActionV11.WRITE_OFF,
            amount = amount,
            reason = reason
        )

        return db.copy(
            settlements = db.settlements + settlement,
            auditLog = db.auditLog + AuditEntryV11(
                id = UUID.randomUUID().toString(),
                action = "DIFFERENCE_SETTLED",
                entityType = "SETTLEMENT",
                entityId = settlement.id,
                description =
                    "Difference settled for $month: $reason"
            )
        )
    }

    fun closeMonth(
        db: V11Database,
        month: String,
        expected: Double,
        received: Double,
        settlementAmount: Double,
        difference: Double
    ): V11Database {

        val snapshot = MonthSnapshotV11(
            id = UUID.randomUUID().toString(),
            month = month,
            expectedAmount = expected,
            receivedAmount = received,
            settlementAmount = settlementAmount,
            finalDifference = difference,
            closedAt = System.currentTimeMillis()
        )

        return db.copy(
            monthSnapshots =
                db.monthSnapshots.filterNot {
                    it.month == month
                } + snapshot,

            auditLog =
                db.auditLog + AuditEntryV11(
                    id = UUID.randomUUID().toString(),
                    action = "MONTH_CLOSED",
                    entityType = "MONTH",
                    entityId = month,
                    description =
                        "Financial month $month closed"
                )
        )
    }
}
