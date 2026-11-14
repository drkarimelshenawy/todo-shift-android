package com.todoshift.app.v11

import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

/** Money and historical-rate invariants; no device or UI fixtures required. */
class FixedSalaryIntegrityTest {
    private val month = YearMonth.of(2026, 11)
    private val source = IncomeSourceV11("salary", "Hospital", IncomeSourceTypeV11.FIXED_SALARY,
        workplaceId = "hospital", calendarId = "work", fixedMonthlyAmount = 3000.0)
    private fun database(count: Int = 0) = V11Database(
        incomeSources = listOf(source),
        days = (1..count).map { day -> DayEntryV11("day-$day", "2026-11-${day.toString().padStart(2, '0')}", "work",
            listOf(ShiftSegmentV11("shift-$day", ShiftKind.FULL_DAY, "hospital"))) },
        rates = listOf(RateHistoryEntryV11("rate", "hospital", ShiftKind.FULL_DAY, "2020-01-01", 500.0)),
    )

    @Test fun monthlySalaryIsEarnedOnceWithZeroOrManyShifts() {
        for (count in listOf(0, 1, 20)) {
            val result = V11FinanceEngine.month(database(count), month)
            assertEquals(3000.0, result.expected, 0.0)
            assertEquals(count, result.shiftCount)
            assertEquals(result.expected, result.byWorkplace.sumOf { it.amount }, 0.0)
        }
    }

    @Test fun fixedSalaryAndUnrelatedShiftEarningsAreIndependent() {
        val data = database(5).let { it.copy(
            days = it.days + DayEntryV11("other-day", "2026-11-10", "other", listOf(ShiftSegmentV11("other-shift", ShiftKind.NIGHT, "other"))),
            rates = it.rates + RateHistoryEntryV11("other-rate", "other", ShiftKind.NIGHT, "2020-01-01", 100.0),
        ) }
        assertEquals(3100.0, V11FinanceEngine.month(data, month).expected, 0.0)
        assertEquals(3000.0, V11FinanceEngine.month(data, month, setOf("work")).expected, 0.0)
        assertEquals(100.0, V11FinanceEngine.month(data, month, setOf("other")).expected, 0.0)
    }

    @Test fun rateChangePreservesEarlierEarningMonthsAndDoesNotInventOlderSalary() {
        val updated = V11AccountsController.updateIncomeSource(database(), source.copy(fixedMonthlyAmount = 4500.0), "2026-12")
        assertEquals(3000.0, V11FinanceEngine.month(updated, month).expected, 0.0)
        assertEquals(4500.0, V11FinanceEngine.month(updated, month.plusMonths(1)).expected, 0.0)
        val added = V11AccountsController.addIncomeSource(V11Database(), "New", IncomeSourceTypeV11.FIXED_SALARY, 5000.0)
        assertEquals(0.0, V11FinanceEngine.month(added, YearMonth.now().minusMonths(1)).expected, 0.0)
    }

    @Test fun closedHistoricalMonthRejectsCompensationRewrite() {
        val data = V11SettlementEngine.closeMonth(database(), "2026-11", 3000.0, 0.0, 0.0, 3000.0)
        assertThrows(IllegalArgumentException::class.java) {
            V11AccountsController.updateIncomeSource(data, source.copy(fixedMonthlyAmount = 9000.0), "2026-10")
        }
    }

    @Test fun deactivationPreservesExistingPeriodsAndReceiptSourceRelationship() {
        val data = database().copy(payments = listOf(PaymentV11("receipt", 100.0, "2026-11-10", "2026-11", sourceId = source.id)))
        val archived = V11AccountsController.deleteIncomeSource(data, source.id)
        assertEquals(source.id, archived.incomeSources.single().id)
        assertFalse(archived.incomeSources.single().active)
        assertEquals(data.payments, archived.payments)
        assertEquals(3000.0, V11FinanceEngine.month(archived, YearMonth.now()).expected, 0.0)
        assertEquals(0.0, V11FinanceEngine.month(archived, YearMonth.now().plusMonths(1)).expected, 0.0)
    }

    @Test fun switchingBetweenShiftAndMonthlyPayKeepsHistoricalEarningsAndReceipts() {
        val place=com.todoshift.app.data.Workplace("hospital","Hospital","#112233",fullDayRate=500.0)
        val old=database(1).copy(incomeSources=listOf(source.copy(type=IncomeSourceTypeV11.SHIFT_BASED,fixedMonthlyAmount=null)),payments=listOf(PaymentV11("p",100.0,"2026-11-20","2026-11",sourceId="salary")))
        val monthly=NeoWorkplaceEdit.apply(old,place,place,3000.0,"2026-12-01")
        assertEquals(500.0,V11FinanceEngine.month(monthly,month).expected,0.0)
        assertEquals(3000.0,V11FinanceEngine.month(monthly,month.plusMonths(1)).expected,0.0)
        val future=monthly.copy(days=monthly.days+DayEntryV11("jan","2027-01-04","work",listOf(ShiftSegmentV11("jan-shift",ShiftKind.FULL_DAY,"hospital"))))
        val perShift=NeoWorkplaceEdit.apply(future,place,place,null,"2027-01-01")
        assertEquals(3000.0,V11FinanceEngine.month(perShift,month.plusMonths(1)).expected,0.0)
        assertEquals(500.0,V11FinanceEngine.month(perShift,month.plusMonths(2)).expected,0.0)
        assertEquals(old.payments,perShift.payments)
    }

}
