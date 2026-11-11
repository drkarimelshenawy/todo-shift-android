package com.todoshift.app.v11

import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class FinanceCalendarFilterTest {
    private val month = YearMonth.of(2026, 11)
    private fun db() = V11Database(incomeSources = listOf(
        IncomeSourceV11("a-source", "A", IncomeSourceTypeV11.OTHER, calendarId = "a"),
        IncomeSourceV11("b-source", "B", IncomeSourceTypeV11.OTHER, calendarId = "b")))

    @Test fun legacyPaymentsFollowTheirSourceCalendar() {
        val data = db().copy(payments = listOf(
            PaymentV11("a-pay", 10.01, "2026-11-11", "2026-11", sourceId = "a-source"),
            PaymentV11("b-pay", 20.02, "2026-11-11", "2026-11", sourceId = "b-source")))
        assertEquals(10.01, V11FinanceEngine.month(data, month, setOf("a")).received, 0.0)
        assertEquals(20.02, V11FinanceEngine.month(data, month, setOf("b")).received, 0.0)
        assertEquals(30.03, V11FinanceEngine.month(data, month).received, 0.0)
    }

    @Test fun allocationsUseTheirOwnSourceAndMonth() {
        val data = db().copy(payments = listOf(PaymentV11("pay", 30.03, "2026-11-11", "2026-11",
            sourceId = "b-source", allocations = listOf(
                PaymentAllocationV11("a", "2026-11", "a-source", 10.01),
                PaymentAllocationV11("b", "2026-12", "b-source", 20.02)))))
        assertEquals(10.01, V11FinanceEngine.month(data, month, setOf("a")).received, 0.0)
        assertEquals(0.0, V11FinanceEngine.month(data, month, setOf("b")).received, 0.0)
    }

    @Test fun settlementUsesExplicitCalendarBeforeSourceCalendar() {
        val data = db().copy(settlements = listOf(
            SettlementV11("explicit", "2026-11", sourceId = "b-source", calendarId = "a",
                action = SettlementActionV11.WRITE_OFF, amount = 3.01),
            SettlementV11("source", "2026-11", sourceId = "b-source",
                action = SettlementActionV11.WRITE_OFF, amount = 7.02)))
        assertEquals(3.01, V11FinanceEngine.month(data, month, setOf("a")).settlements, 0.0)
        assertEquals(7.02, V11FinanceEngine.month(data, month, setOf("b")).settlements, 0.0)
        assertEquals(10.03, V11FinanceEngine.month(data, month).settlements, 0.0)
    }

    @Test fun unattributedMoneyBelongsOnlyToOverallTotals() {
        val data = db().copy(payments = listOf(PaymentV11("pay", 12.34, "2026-11-11", "2026-11")),
            settlements = listOf(SettlementV11("s", "2026-11", action = SettlementActionV11.WRITE_OFF, amount = 1.01)))
        assertEquals(12.34, V11FinanceEngine.month(data, month).received, 0.0)
        assertEquals(1.01, V11FinanceEngine.month(data, month).settlements, 0.0)
        for (filter in listOf(emptySet(), setOf("a"), setOf("a", "b"))) {
            val result = V11FinanceEngine.month(data, month, filter)
            assertEquals(0.0, result.received, 0.0)
            assertEquals(0.0, result.settlements, 0.0)
        }
    }
}
