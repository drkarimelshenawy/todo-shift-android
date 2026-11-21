package com.todoshift.app.v11.ui

import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class NeoSalaryDashboardLogicTest {
    private val month = YearMonth.of(2026,11)
    private val source = IncomeSourceV11(
        "salary", "Hospital", IncomeSourceTypeV11.FIXED_SALARY,
        workplaceId="hospital", calendarId="work", fixedMonthlyAmount=3000.0
    )
    private val split = PaymentV11(
        "split", 1000.0, "2026-11-14", "2026-11", sourceId=source.id,
        allocations=listOf(
            PaymentAllocationV11("nov", "2026-11", source.id, 600.0),
            PaymentAllocationV11("dec", "2026-12", source.id, 400.0)
        )
    )
    private val general = PaymentV11("general",500.0,"2026-11-14","2026-11")
    private val reversed = PaymentV11(
        "reversed",300.0,"2026-11-14","2026-11",
        sourceId=source.id,reversedAt=1L
    )
    private val db = V11Database(
        incomeSources=listOf(source),
        payments=listOf(split,general,reversed),
        days=(1..11).map { d -> DayEntryV11(
            "day-$d","2026-11-"+d.toString().padStart(2,'0'),"work",
            listOf(ShiftSegmentV11("shift-$d",ShiftKind.FULL_DAY,"hospital"))
        ) },
        rates=listOf(RateHistoryEntryV11(
            "rate","hospital",ShiftKind.FULL_DAY,"2020-01-01",500.0))
    )

    @Test fun dashboardCountsEachAllocationOnceByEarningMonth() {
        val nov=NeoSalaryDashboardLogic.relevantPayments(db,month)
        assertEquals(3,nov.size)
        assertEquals(60000L,NeoSalaryDashboardLogic.portionCents(split,month))
        assertEquals(40000L,NeoSalaryDashboardLogic.portionCents(split,month.plusMonths(1)))
        assertEquals(0L,NeoSalaryDashboardLogic.portionCents(reversed,month))
        assertEquals(60000L,NeoSalaryDashboardLogic.workplaceReceivedCents(db,"hospital",month))
        assertEquals(240000L,remainingWorkEntitlement(db,"hospital",month))
        assertEquals(1100.0,V11FinanceEngine.month(db,month).received,0.0)
        assertEquals(3000.0,V11FinanceEngine.month(db,month).expected,0.0)
        assertEquals(0L,NeoSalaryDashboardLogic.portionCents(general,month.plusMonths(1)))
    }

    @Test fun summaryProgressHandlesEmptyAndOverpaidMonths() {
        val empty=V11FinanceEngine.month(V11Database(),month)
        assertEquals(0f,NeoSalaryDashboardLogic.collectionProgress(empty),0f)
        val normal=V11FinanceEngine.month(db,month)
        assertEquals((1100f/3000f),NeoSalaryDashboardLogic.collectionProgress(normal),0.001f)
        assertEquals(1f,NeoSalaryDashboardLogic.collectionProgress(normal.copy(received=7000.0)),0f)
    }

    @Test fun csvUsesOneFixedSalaryAccrualWithoutCountingElevenShiftsAsEarnings() {
        val before=db
        val csv=ModernOperations.statementCsv(db,"hospital",
            LocalDate.parse("2026-11-01"),LocalDate.parse("2026-11-30"))
        assertEquals(1,csv.lineSequence().count { it.startsWith("\"SALARY\"") })
        assertEquals(11,csv.lineSequence().count { it.startsWith("\"SHIFT\"") })
        assertTrue(csv.contains("\"FIXED_MONTHLY\""))
        assertTrue(csv.contains("\"3000.00\""))
        assertFalse(csv.contains("\"500.00\""))
        assertEquals(before,db)
    }

    @Test fun exportIncludesFixedPayEvenWhenNoShiftWasRecorded() {
        val withoutShifts=db.copy(days=emptyList(),payments=emptyList())
        val csv=ModernOperations.statementCsv(withoutShifts,"hospital",
            LocalDate.parse("2026-11-01"),LocalDate.parse("2026-11-30"))
        assertEquals(1,csv.lineSequence().count { it.startsWith("\"SALARY\"") })
        assertEquals(0,csv.lineSequence().count { it.startsWith("\"SHIFT\"") })
        assertEquals(3000.0,V11FinanceEngine.month(withoutShifts,month).expected,0.0)
    }

    @Test fun salaryDataAndLedgerAreNeverMutatedByDashboardQueriesOrExport() {
        val before=db.copy()
        NeoSalaryDashboardLogic.relevantPayments(db,month)
        NeoSalaryDashboardLogic.workplaceReceivedCents(db,"hospital",month)
        ModernOperations.statementCsv(db,"hospital",month.atDay(1),month.atEndOfMonth())
        assertEquals(before,db)
        assertEquals(3,db.payments.size)
    }
}
