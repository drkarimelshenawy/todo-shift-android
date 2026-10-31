package com.todoshift.app.data

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyFinance(
    val shiftCount: Int,
    val expected: Double,
    val received: Double,
    val balance: Double
)

object FinanceEngine {

    fun shiftAmount(
        shift: ShiftEntry,
        workplaces: List<Workplace>
    ): Double {

        shift.customAmount?.let { return it }

        fun workplace(id: String?): Workplace? =
            workplaces.firstOrNull { it.id == id }

        return when (shift.type) {

            ShiftType.FULL_DAY ->
                workplace(shift.workplaceIds.getOrNull(0))
                    ?.fullDayRate ?: 0.0

            ShiftType.LONG_NIGHT -> {
                val long =
                    workplace(shift.workplaceIds.getOrNull(0))
                        ?.longRate ?: 0.0

                val night =
                    workplace(shift.workplaceIds.getOrNull(1))
                        ?.nightRate ?: 0.0

                long + night
            }

            ShiftType.THREE_SHIFTS -> {
                val morning =
                    workplace(shift.workplaceIds.getOrNull(0))
                        ?.morningRate ?: 0.0

                val evening =
                    workplace(shift.workplaceIds.getOrNull(1))
                        ?.eveningRate ?: 0.0

                val night =
                    workplace(shift.workplaceIds.getOrNull(2))
                        ?.nightRate ?: 0.0

                morning + evening + night
            }

            ShiftType.OFF -> 0.0
        }
    }

    fun month(
        month: YearMonth,
        data: AppData
    ): MonthlyFinance {

        val monthShifts =
            data.shifts.filter {
                YearMonth.from(LocalDate.parse(it.date)) == month &&
                    it.type != ShiftType.OFF
            }

        val expected =
            monthShifts.sumOf {
                shiftAmount(it, data.workplaces)
            }

        val received =
            data.payments
                .filter {
                    YearMonth.from(LocalDate.parse(it.date)) == month
                }
                .sumOf { it.amount }

        return MonthlyFinance(
            shiftCount = monthShifts.size,
            expected = expected,
            received = received,
            balance = expected - received
        )
    }
}
