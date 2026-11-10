package com.todoshift.app.v11

/** A blank optional field is different from a nonblank invalid amount. No input rewriting. */
class MoneyInput private constructor(val blank: Boolean, val cents: Long?) {
    val optionalValid: Boolean
        get() = blank || cents != null

    val positive: Boolean
        get() = cents != null && cents > 0

    val amount: Double?
        get() = cents?.let(LocalizedNumbers::amount)

    companion object {
        fun parse(raw: String): MoneyInput =
            MoneyInput(
                raw.isBlank(),
                LocalizedNumbers.money(raw)?.movePointRight(2)?.longValueExact(),
            )
    }
}
