package com.todoshift.app.v11

/** Shared financial invariant checks for backup validation and durable database writes.
 * Reject invalid snapshots without normalizing or rewriting historical values.
 */
internal object FinancialDataValidation {
    fun validate(database: V11Database) {
        val amounts =
            database.rates.map { it.amount } +
                database.payments.map { it.amount } +
                database.settlements.map { it.amount } +
                database.obligationPayments.map { it.amount } +
                database.obligations.flatMap {
                    listOfNotNull(it.totalAmount, it.installmentAmount)
                } +
                database.days.flatMap { it.segments }.mapNotNull { it.exceptionalAmount } +
                database.payments.flatMap { it.allocations }.map { it.amount } +
                database.incomeSources.mapNotNull { it.fixedMonthlyAmount } +
                database.monthSnapshots.flatMap {
                    listOf(
                        it.expectedAmount,
                        it.receivedAmount,
                        it.settlementAmount,
                        it.finalDifference,
                    )
                }
        validateMoneyRange(amounts)
        database.payments.forEach { payment ->
            require(payment.amount >= 0.0) { "Negative payment: ${payment.id}" }
            val ids = mutableSetOf<String>()
            var allocated = 0L
            payment.allocations.forEach { allocation ->
                require(allocation.id.isNotBlank() && ids.add(allocation.id)) {
                    "Invalid or duplicate allocation ID: ${payment.id}"
                }
                java.time.YearMonth.parse(allocation.accrualMonth)
                require(allocation.amount >= 0.0) { "Negative allocation: ${payment.id}" }
                allocated = Math.addExact(allocated, LocalizedNumbers.cents(allocation.amount))
            }
            require(payment.allocations.isEmpty() || allocated == LocalizedNumbers.cents(payment.amount)) {
                "Allocation total differs from payment: ${payment.id}"
            }
        }
    }

    /** Apply the input field's magnitude limit before any exact Long conversion.
     * Signed historical adjustments and snapshot differences remain supported.
     * A conservative absolute-total budget also protects later subset sums.
     */
    fun validateMoneyRange(amounts: List<Double>) {
        var total = java.math.BigInteger.ZERO
        amounts.forEach { amount ->
            require(amount.isFinite() && kotlin.math.abs(amount) <= 9_000_000_000_000.0) {
                "Financial amount is outside the supported range"
            }
            total = total.add(java.math.BigInteger.valueOf(LocalizedNumbers.cents(amount)).abs())
            require(total <= java.math.BigInteger.valueOf(Long.MAX_VALUE)) {
                "Financial totals exceed the supported range"
            }
        }
    }

}
