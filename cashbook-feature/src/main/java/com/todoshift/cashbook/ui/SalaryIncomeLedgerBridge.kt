package com.todoshift.cashbook.ui

import android.content.Context
import com.todoshift.cashbook.core.Ledger
import java.io.IOException
import java.time.LocalDate

/**
 * Durable Cash Book boundary for a Salary receipt.
 *
 * The host must persist the Salary receipt before calling post(), then retry
 * pending receipts after process restart. Never call this from the UI thread.
 * The ledger owns the idempotency marker and atomic entry write.
 */
class SalaryIncomeLedgerBridge(context: Context) {
    private val ledger = Ledger(AndroidLedgerStore(context.applicationContext))

    data class EligibleAccount(val id: String, val name: String, val currency: String)

    @Throws(IOException::class)
    fun accounts(): List<EligibleAccount> = synchronized(Ledger::class.java) {
        ledger.load().accounts
            .filter { !it.archived && it.currency == "EGP" }
            .map { EligibleAccount(it.id, it.name, it.currency) }
    }

    @Throws(IOException::class)
    fun post(
        paymentId: String,
        accountId: String,
        amountMinor: Long,
        date: LocalDate,
    ): String {
        require(amountMinor > 0L) { "Receipt amount must be positive" }
        return ledger.postSalaryIncomeOnce(paymentId, accountId, amountMinor, date.toString())
    }

    @Throws(IOException::class)
    fun find(paymentId: String): String? = synchronized(Ledger::class.java) {
        require(paymentId.matches(Regex("[A-Za-z0-9_-]{1,120}")))
        val marker = "[salary-receipt:$paymentId]"
        ledger.load().entries.singleOrNull { it.note == marker && !it.deleted }?.id
    }
}
