package com.todoshift.app.v11

import java.math.BigDecimal
import java.math.RoundingMode

/** Strict, locale-independent parsing; presentation separators never reach stored calculations. */
object LocalizedNumbers {
    fun normalize(input: String): String? {
        val s = buildString {
            for (ch in input.trim()) when {
                ch in '0'..'9' -> append(ch)
                ch in '\u0660'..'\u0669' -> append('0' + (ch - '\u0660'))
                ch in '\u06f0'..'\u06f9' -> append('0' + (ch - '\u06f0'))
                ch == '\u066b' -> append('.')
                ch == '\u066c' || ch == ',' || ch == '\u00a0' || ch == '\u202f' || ch == ' ' ->
                    append(',')
                ch == '.' -> append('.')
                ch in listOf('\u200e', '\u200f', '\u2066', '\u2067', '\u2068', '\u2069') -> Unit
                else -> return null
            }
        }
        if (s.isEmpty()) return null
        // A comma is grouping only; reject ambiguous decimal commas and malformed groups.
        val parts = s.split('.')
        if (parts.size > 2 || (parts.size == 2 && parts[1].isEmpty())) return null
        val whole = parts[0]
        if (',' in whole && !whole.matches(Regex("[0-9]{1,3}(,[0-9]{3})+"))) return null
        if (!whole.replace(",", "").matches(Regex("[0-9]+"))) return null
        if (parts.size == 2 && !parts[1].matches(Regex("[0-9]+"))) return null
        return whole.replace(",", "") + if (parts.size == 2) ".${parts[1]}" else ""
    }

    fun money(input: String): BigDecimal? =
        normalize(input)
            ?.toBigDecimalOrNull()
            ?.takeIf {
                it.precision() <= 16 && it >= BigDecimal.ZERO && it <= BigDecimal("9000000000000")
            }
            ?.setScale(2, RoundingMode.HALF_UP)

    fun integer(input: String): Int? = normalize(input)?.takeIf { '.' !in it }?.toIntOrNull()

    fun cents(value: Double): Long =
        BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()

    fun amount(cents: Long): Double = BigDecimal.valueOf(cents, 2).toDouble()
}

fun String.toMoneyOrNull(): Double? = LocalizedNumbers.money(this)?.toDouble()

fun String.toLocalizedIntOrNull(): Int? = LocalizedNumbers.integer(this)
