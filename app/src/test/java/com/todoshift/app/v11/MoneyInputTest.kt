package com.todoshift.app.v11

import org.junit.Assert.*
import org.junit.Test

class MoneyInputTest {
    @Test
    fun localizedValuesPreserveCents() {
        listOf("9000", "٩٠٠٠", "۹۰۰۰", "9,000", "٩٬٠٠٠", "9٬٠۰۰").forEach {
            assertEquals(it, 900000L, MoneyInput.parse(it).cents)
            assertEquals(9000.0, MoneyInput.parse(it).amount!!, 0.0)
        }
        assertEquals(900050L, MoneyInput.parse("٩٠٠٠٫٥٠").cents)
        assertEquals(101L, MoneyInput.parse("1.005").cents)
    }

    @Test
    fun optionalBlankIsNotInvalidOrZero() {
        assertTrue(MoneyInput.parse("  ").optionalValid)
        assertNull(MoneyInput.parse("").amount)
        assertTrue(MoneyInput.parse("0").optionalValid)
        assertFalse(MoneyInput.parse("0").positive)
        listOf("9,00", "9.000,50", "oops", "-1", "9000000000001").forEach {
            assertFalse(it, MoneyInput.parse(it).optionalValid)
            assertNull(MoneyInput.parse(it).amount)
        }
    }
}
