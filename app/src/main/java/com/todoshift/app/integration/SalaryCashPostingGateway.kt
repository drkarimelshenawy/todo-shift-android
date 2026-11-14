package com.todoshift.app.integration

/**
 * One receipt in Salary corresponds to exactly one income in Cash Book.
 * IDs are stable across retries and process restarts.
 */
data class SalaryCashLink(
    val paymentId: String,
    val cashTransactionId: String,
    val cashAccountId: String,
    val amountMinor: Long,
    val currencyCode: String,
) {
    init {
        require(paymentId.isNotBlank())
        require(cashTransactionId.isNotBlank())
        require(cashAccountId.isNotBlank())
        require(amountMinor > 0L)
        require(currencyCode.matches(Regex("[A-Z]{3}")))
    }
}

/**
 * Implementations must durably persist both the receipt and its corresponding
 * Cash Book entry, or retain enough recovery state to finish an interrupted write.
 */
interface SalaryCashPostingGateway {
    fun findByPaymentId(paymentId: String): SalaryCashLink?
    fun postReceipt(
        paymentId: String,
        cashAccountId: String,
        amountMinor: Long,
        currencyCode: String,
    ): SalaryCashLink
}

/**
 * Integration seam for Cash Book's Ledger. The adapter MUST return an existing
 * entry for a repeated externalKey rather than calling Ledger.post twice.
 * This is not yet a production adapter or a durable two-store transaction.
 */
interface CashIncomePort {
    fun findByExternalKey(externalKey: String): CashIncomeRecord?
    fun createIncome(
        externalKey: String,
        accountId: String,
        amountMinor: Long,
        currencyCode: String,
    ): CashIncomeRecord
}

data class CashIncomeRecord(
    val id: String,
    val externalKey: String,
    val accountId: String,
    val amountMinor: Long,
    val currencyCode: String,
)

/** Store link by payment ID with uniqueness enforcement and crash-safe writes. */
interface SalaryCashLinkStore {
    fun find(paymentId: String): SalaryCashLink?
    fun saveIfAbsent(link: SalaryCashLink): SalaryCashLink
}

/**
 * Recoverable posting logic: if Cash Book succeeded but link storage failed,
 * a retry finds the income by its stable external key instead of duplicating it.
 * Production requires a durable CashIncomePort, unique external keys and
 * serialized calls for the same paymentId.
 */
class SalaryCashPostingCoordinator(
    private val incomes: CashIncomePort,
    private val links: SalaryCashLinkStore,
) : SalaryCashPostingGateway {
    override fun findByPaymentId(paymentId: String): SalaryCashLink? = links.find(paymentId)

    @Synchronized
    override fun postReceipt(
        paymentId: String,
        cashAccountId: String,
        amountMinor: Long,
        currencyCode: String,
    ): SalaryCashLink {
        require(paymentId.isNotBlank() && cashAccountId.isNotBlank())
        require(amountMinor > 0L && currencyCode.matches(Regex("[A-Z]{3}")))
        links.find(paymentId)?.let { existing ->
            require(existing.cashAccountId == cashAccountId &&
                existing.amountMinor == amountMinor &&
                existing.currencyCode == currencyCode) {
                "Receipt already posted with different financial details"
            }
            return existing
        }
        val key = "salary-receipt:$paymentId"
        val entry = incomes.findByExternalKey(key)
            ?: incomes.createIncome(key, cashAccountId, amountMinor, currencyCode)
        require(entry.externalKey == key &&
            entry.accountId == cashAccountId &&
            entry.amountMinor == amountMinor &&
            entry.currencyCode == currencyCode) {
            "Existing Cash Book entry conflicts with receipt"
        }
        return links.saveIfAbsent(
            SalaryCashLink(paymentId, entry.id, cashAccountId, amountMinor, currencyCode)
        ).also { stored ->
            require(stored.paymentId == paymentId &&
                stored.cashTransactionId == entry.id &&
                stored.cashAccountId == cashAccountId &&
                stored.amountMinor == amountMinor &&
                stored.currencyCode == currencyCode) {
                "Receipt link conflicts with existing posting"
            }
        }
    }
}
