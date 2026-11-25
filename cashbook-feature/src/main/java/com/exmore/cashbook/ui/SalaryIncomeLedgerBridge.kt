package com.exmore.cashbook.ui

import android.content.Context
import com.exmore.cashbook.core.Ledger
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

    data class EligibleAccount(
        val id: String,
        val name: String,
        val currency: String,
        /** Balance in minor EGP units, read from the same snapshot as the account list. */
        val balanceMinor: Long = 0L,
    )

    @Throws(IOException::class)
    fun totalMoneyBalance():Long = synchronized(Ledger::class.java) {
        val snapshot=ledger.load()
        val debtIds=snapshot.debts.map {it.accountId}.toSet()
        snapshot.accounts.filter {it.id !in debtIds && it.currency=="EGP"}.fold(0L) {total,account->Math.addExact(total,snapshot.balance(account.id))}
    }

    @Throws(IOException::class)
    fun accounts(): List<EligibleAccount> = synchronized(Ledger::class.java) {
        val snapshot = ledger.load()
        val debtIds = snapshot.debts.map { it.accountId }.toSet()
        snapshot.accounts
            .filter { !it.archived && it.currency == "EGP" && it.id !in debtIds }
            .map { EligibleAccount(it.id, it.name, it.currency, snapshot.balance(it.id)) }
    }

    /**
     * Explicit creation from the receipt dialog; always a regular EGP account,
     * with zero opening balance. The account is saved immediately and remains
     * if the user later cancels the receipt; explain this before confirmation.
     */
    @Throws(IOException::class)
    fun createReceivingAccount(rawName: String): EligibleAccount = synchronized(Ledger::class.java) {
        val name = rawName.trim()
        require(name.isNotBlank() && name.length <= 80) { "Enter an account name (up to 80 characters)" }
        val before = ledger.load()
        require(before.accounts.none { it.name.equals(name, ignoreCase = true) }) {
            "An account with that name already exists"
        }
        val id = ledger.addAccount(name, "EGP", 0L)
        val snapshot = ledger.load()
        val created = snapshot.accounts.single { it.id == id }
        EligibleAccount(created.id, created.name, created.currency, snapshot.balance(id))
    }

    @Throws(IOException::class)
    fun post(
        paymentId: String,
        accountId: String,
        amountMinor: Long,
        date: LocalDate,
        revision: Int = 0,
        reversed: Boolean = false,
    ): String {
        require(amountMinor > 0L) { "Receipt amount must be positive" }
        return ledger.reconcileSalaryIncome(paymentId, accountId, amountMinor, date.toString(), revision, reversed)
    }

    @Throws(IOException::class)
    fun find(paymentId: String): String? = synchronized(Ledger::class.java) {
        require(paymentId.matches(Regex("[A-Za-z0-9_-]{1,120}")))
        val marker = "[salary-receipt:$paymentId]"
        ledger.load().entries.singleOrNull { (it.note == marker || it.note.startsWith("$marker [revision:")) && !it.deleted }?.id
    }
}
