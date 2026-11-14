package com.todoshift.app.integration

import android.content.Context
import com.todoshift.app.v11.V11Database
import com.todoshift.app.v11.LocalizedNumbers
import com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge
import java.time.LocalDate

/** The persisted receipt is the outbox; replay always uses its immutable ID. */
internal object SalaryCashSync {
    fun validateTransition(previous: V11Database, next: V11Database) {
        previous.payments.filter { it.cashBookAccountId != null }.forEach { old ->
            val updated = next.payments.singleOrNull { it.id == old.id }
            require(updated != null && updated.cashBookAccountId == old.cashBookAccountId &&
                LocalizedNumbers.cents(updated.amount) == LocalizedNumbers.cents(old.amount) &&
                updated.receivedDate == old.receivedDate && updated.cashBookRevision == old.cashBookRevision &&
                updated.reversedAt == old.reversedAt && updated.allocations == old.allocations &&
                updated.accrualMonth == old.accrualMonth && updated.sourceId == old.sourceId) {
                "Restore cannot remove or change a Cash Book linked receipt or revision"
            }
        }
        next.payments.filter { it.cashBookAccountId != null }.forEach {
            require(it.id.matches(Regex("[A-Za-z0-9_-]{1,120}")) &&
                !it.cashBookAccountId.isNullOrBlank() && LocalizedNumbers.cents(it.amount) > 0)
            LocalDate.parse(it.receivedDate)
        }
    }
    /** Normal writes may advance a linked receipt but cannot erase its outbox/tombstone. */
    fun validateWrite(previous: V11Database, next: V11Database) {
        previous.payments.filter { it.cashBookAccountId != null }.forEach { old ->
            val updated = requireNotNull(next.payments.singleOrNull { it.id == old.id }) { "Reverse linked receipts instead of deleting them" }
            require(old.reversedAt == null || updated == old) { "Reversed receipt is immutable" }
            require(updated.cashBookAccountId != null) { "A linked receipt cannot be unlinked" }
            val changed = updated.cashBookAccountId != old.cashBookAccountId ||
                LocalizedNumbers.cents(updated.amount) != LocalizedNumbers.cents(old.amount) ||
                updated.receivedDate != old.receivedDate || updated.reversedAt != old.reversedAt
            require(if (changed) old.reversedAt == null && updated.cashBookRevision == Math.addExact(old.cashBookRevision, 1)
                else updated.cashBookRevision == old.cashBookRevision) { "Invalid receipt revision" }
        }
    }
    fun reconcile(context: Context, db: V11Database) {
        val bridge = SalaryIncomeLedgerBridge(context)
        db.payments.filter { it.cashBookAccountId != null }.forEach {
            bridge.post(it.id, requireNotNull(it.cashBookAccountId),
                LocalizedNumbers.cents(it.amount), LocalDate.parse(it.receivedDate), it.cashBookRevision, it.reversedAt != null)
        }
    }
}
