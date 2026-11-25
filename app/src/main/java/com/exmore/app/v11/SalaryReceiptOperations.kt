package com.exmore.app.v11

import java.util.UUID

/** Salary is the durable outbox: financial edits advance a version before Cash Book reconciliation. */
internal object SalaryReceiptOperations {
    fun update(db: V11Database, candidate: PaymentV11): V11Database {
        val old = db.payments.single { it.id == candidate.id }
        require(old.reversedAt == null) { "A reversed receipt is retained for audit and cannot be edited" }
        require(candidate.cashBookAccountId != null || old.cashBookAccountId == null) { "Choose a receiving account" }
        val financialChange = old.cashBookAccountId != candidate.cashBookAccountId ||
            LocalizedNumbers.cents(old.amount) != LocalizedNumbers.cents(candidate.amount) ||
            old.receivedDate != candidate.receivedDate || old.reversedAt != candidate.reversedAt
        if (financialChange || old.allocations != candidate.allocations) {
            val months = (old.allocations.map { it.accrualMonth } + old.accrualMonth +
                candidate.allocations.map { it.accrualMonth } + candidate.accrualMonth).toSet()
            require(db.monthSnapshots.none { it.month in months && it.reopenedAt == null }) {
                "Reopen the earning period before correcting or reversing this receipt"
            }
        }
        val updated = candidate.copy(cashBookRevision =
            if (financialChange && old.cashBookAccountId != null) Math.addExact(old.cashBookRevision, 1)
            else old.cashBookRevision)
        val action = if (updated.reversedAt != null) "PAYMENT_REVERSE" else "PAYMENT_CORRECT"
        return db.copy(
            payments = db.payments.map { if (it.id == updated.id) updated else it },
            auditLog = db.auditLog + AuditEntryV11(
                UUID.randomUUID().toString(), action, "payment", updated.id,
                "Revision ${updated.cashBookRevision}; before ${old.amount} / ${old.receivedDate} / ${old.cashBookAccountId}; after ${updated.amount} / ${updated.receivedDate} / ${updated.cashBookAccountId}; reversed=${updated.reversedAt != null}",
            ),
        )
    }

    fun reverse(db: V11Database, id: String): V11Database {
        val payment = db.payments.single { it.id == id }
        if (payment.reversedAt != null) return db
        return update(db, payment.copy(reversedAt = System.currentTimeMillis()))
    }
}
