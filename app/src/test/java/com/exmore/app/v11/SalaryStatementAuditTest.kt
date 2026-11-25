package com.exmore.app.v11

import com.exmore.app.v11.ui.NeoSalaryDashboardLogic
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class SalaryStatementAuditTest {
    private val november = YearMonth.of(2026, 11)
    private val source = IncomeSourceV11(
        "hospital-salary", "Hospital", IncomeSourceTypeV11.FIXED_SALARY,
        workplaceId = "hospital", calendarId = "work", fixedMonthlyAmount = 3000.0
    )
    private val allocated = PaymentV11(
        "receipt", 1000.0, "2026-12-16", "2026-11", sourceId = source.id,
        allocations = listOf(
            PaymentAllocationV11("nov", "2026-11", source.id, 600.0),
            PaymentAllocationV11("dec", "2026-12", source.id, 400.0)
        )
    )
    private val db = V11Database(
        incomeSources = listOf(source),
        payments = listOf(
            allocated, PaymentV11("general", 200.0, "2026-11-12", "2026-11")
        ),
        settlements = listOf(
            SettlementV11("settle", "2026-11", sourceId = source.id,
                workplaceId = "hospital", action = SettlementActionV11.WRITE_OFF,
                amount = 100.0, reason = "Adjustment")
        ),
        days = listOf(DayEntryV11("day", "2026-11-10", "work",
            listOf(ShiftSegmentV11("shift", ShiftKind.FULL_DAY, "hospital")))),
        rates = listOf(RateHistoryEntryV11(
            "rate", "hospital", ShiftKind.FULL_DAY, "2020-01-01", 500.0))
    )

    @Test fun receiptAfterEarningMonthIsStillIncludedInAccrualStatement() {
        val csv = ModernOperations.statementCsv(db, "hospital",
            november.atDay(1), november.atEndOfMonth())
        assertTrue(csv.contains("accrual_month,received_date"))
        assertTrue(csv.contains("\"600.00\""))
        assertTrue(csv.contains("\"2026-12-16\""))
        assertFalse(csv.contains("\"400.00\""))
        assertFalse(csv.contains("\"200.00\""))
        assertEquals(1, csv.lineSequence().count { it.startsWith("\"RECEIPT\"") })
        assertEquals(1, csv.lineSequence().count { it.startsWith("\"SALARY\"") })
        assertEquals(1, csv.lineSequence().count { it.startsWith("\"SETTLEMENT\"") })
        assertTrue(csv.contains("\"100.00\""))
        assertEquals(0L, NeoSalaryDashboardLogic.portionCents(
            allocated.copy(reversedAt = 123L), november))
        assertEquals(60000L, NeoSalaryDashboardLogic.historicalPortionCents(
            allocated.copy(reversedAt = 123L), november))
    }

    @Test fun exportedAmountsMatchWorkplaceAccrualMathWithoutDoubleCounting() {
        val summary = V11FinanceEngine.month(db, november)
        assertEquals(3000.0, summary.expected, 0.0)
        assertEquals(800.0, summary.received, 0.0) // 600 workplace + 200 unassigned
        assertEquals(100.0, summary.settlements, 0.0)
        assertEquals(2100.0, summary.difference, 0.0)
        val csv = ModernOperations.statementCsv(db, "hospital",
            november.atDay(1), november.atEndOfMonth())
        assertTrue(csv.contains("\"3000.00\""))
        assertTrue(csv.contains("\"0.00\""))
        assertEquals(db, db.copy())
    }

    @Test fun partialRangeDoesNotImportFullMonthlySalaryFromOutsideStartDate() {
        val csv = ModernOperations.statementCsv(db, "hospital",
            LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-21"))
        assertEquals(0, csv.lineSequence().count { it.startsWith("\"SALARY\"") })
        assertEquals(1, csv.lineSequence().count { it.startsWith("\"SHIFT\"") })
        assertEquals(0, csv.lineSequence().count { it.startsWith("\"RECEIPT\"") })
    }

    @Test fun exportedSalaryStillExistsInMonthWithoutShifts() {
        val noShifts = db.copy(days = emptyList(), payments = emptyList())
        val csv = ModernOperations.statementCsv(noShifts, "hospital",
            november.atDay(1), november.atEndOfMonth())
        assertEquals(1, csv.lineSequence().count { it.startsWith("\"SALARY\"") })
        assertEquals(0, csv.lineSequence().count { it.startsWith("\"SHIFT\"") })
    }
}
