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
                updated.receivedDate == old.receivedDate) {
                "A Cash Book linked receipt cannot be removed or change its amount, date or account"
            }
        }
        next.payments.filter { it.cashBookAccountId != null }.forEach {
            require(it.id.matches(Regex("[A-Za-z0-9_-]{1,120}")) &&
                !it.cashBookAccountId.isNullOrBlank() && LocalizedNumbers.cents(it.amount) > 0)
            LocalDate.parse(it.receivedDate)
        }
    }
    fun reconcile(context: Context, db: V11Database) {
        val bridge = SalaryIncomeLedgerBridge(context)
        db.payments.filter { it.cashBookAccountId != null }.forEach {
            bridge.post(it.id, requireNotNull(it.cashBookAccountId),
                LocalizedNumbers.cents(it.amount), LocalDate.parse(it.receivedDate))
        }
    }
}
