package com.todoshift.app.v11

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class V70MissingRateDiagnosticsTest {
    private val month = YearMonth.parse("2026-02")
    private val shift = ShiftSegmentV11("shift", ShiftKind.NIGHT, "work")
    private val day = DayEntryV11("day", "2026-02-07", "cal", listOf(shift))

    @Test fun missingRateIsFlaggedRatherThanSilentlyTreatedAsRealZero() {
        val db = V11Database(
            days = listOf(day),
            rates = listOf(RateHistoryEntryV11("later", "work", ShiftKind.NIGHT,
                "2026-03-01", 225.0))
        )
        assertEquals(1, V11FinanceEngine.missingRateCount(db, month))
        assertEquals(0.0, V11FinanceEngine.month(db, month).expected, 0.0)
    }

    @Test fun historicalRateSuppressesWarningAndKeepsHistoricalAmount() {
        val db = V11Database(
            days = listOf(day),
            rates = listOf(
                RateHistoryEntryV11("earlier", "work", ShiftKind.NIGHT, "2026-01-01", 200.0),
                RateHistoryEntryV11("later", "work", ShiftKind.NIGHT, "2026-03-01", 225.0)
            )
        )
        assertEquals(0, V11FinanceEngine.missingRateCount(db, month))
        assertEquals(200.0, V11FinanceEngine.month(db, month).expected, 0.0)
    }

    @Test fun fixedSalaryAndManualOverrideAreNotMistakenForMissingRates() {
        val salary = IncomeSourceV11("pay", "Salary", IncomeSourceTypeV11.FIXED_SALARY,
            workplaceId = "work", fixedMonthlyAmount = 4000.0)
        val db = V11Database(days = listOf(day), incomeSources = listOf(salary))
        assertEquals(0, V11FinanceEngine.missingRateCount(db, month))
        assertEquals(4000.0, V11FinanceEngine.month(db, month).expected, 0.0)

        val exception = db.copy(incomeSources = emptyList(),
            days = listOf(day.copy(segments = listOf(shift.copy(exceptionalAmount = 250.0)))))
        assertEquals(0, V11FinanceEngine.missingRateCount(exception, month))
        assertEquals(250.0, V11FinanceEngine.month(exception, month).expected, 0.0)
    }
}
