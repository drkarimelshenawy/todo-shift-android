package com.todoshift.app.integration

/**
 * Stable cross-module identity for a salary receipt and its Cash Book income entry.
 *
 * This contract does not perform writes. Implementations must validate and persist
 * both sides safely before enabling automatic posting in the user interface.
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
 * A receipt's immutable payment ID is the idempotency key. Retrying the same
 * posting must return the existing link, never create a second income entry.
 */
interface SalaryCashPostingGateway {
    fun findByPaymentId(paymentId: String): SalaryCashLink?

    /**
     * Must be recoverable after a crash at any intermediate persistence step.
     * An implementation must not report success until the link and transaction
     * are durably committed.
     */
    fun postReceipt(
        paymentId: String,
        cashAccountId: String,
        amountMinor: Long,
        currencyCode: String,
    ): SalaryCashLink
}
