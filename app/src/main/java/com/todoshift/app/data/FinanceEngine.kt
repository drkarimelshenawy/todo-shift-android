package com.todoshift.app.data

import java.time.LocalDate
import java.time.YearMonth

data class MonthFinance(
    val shiftCount: Int,
    val expected: Double,
    val received: Double,
    val balance: Double
)

data class WorkplaceFinance(
    val workplaceId: String,
    val workplaceName: String,
    val shiftParts: Int,
    val amount: Double
)

data class ShiftTypeFinance(
    val label: String,
    val count: Int,
    val amount: Double
)

object FinanceEngine {

    fun shiftAmount(
        shift: ShiftEntry,
        workplaces: List<Workplace>
    ): Double {

        shift.customAmount
            ?.let {
                return it
            }

        return shiftParts(
            shift,
            workplaces
        ).sumOf {
            it.second
        }
    }

    fun shiftParts(
        shift: ShiftEntry,
        workplaces: List<Workplace>
    ): List<Pair<String, Double>> {

        fun place(
            index: Int
        ): Workplace? {

            val id =
                shift.workplaceIds
                    .getOrNull(index)

            return workplaces
                .firstOrNull {
                    it.id == id
                }
        }

        if (
            shift.customAmount != null
        ) {
            val ids =
                shift.workplaceIds
                    .filterNotNull()

            if (
                ids.isEmpty()
            ) {
                return emptyList()
            }

            val amountEach =
                shift.customAmount /
                    ids.size

            return ids.map {
                it to amountEach
            }
        }

        return when (
            shift.type
        ) {

            ShiftType.FULL_DAY -> {

                val p =
                    place(0)
                        ?: return emptyList()

                listOf(
                    p.id to
                        p.fullDayRate
                )
            }

            ShiftType.LONG_NIGHT -> {

                buildList {

                    place(0)?.let {
                        add(
                            it.id to
                                it.longRate
                        )
                    }

                    place(1)?.let {
                        add(
                            it.id to
                                it.nightRate
                        )
                    }
                }
            }

            ShiftType.THREE_SHIFTS -> {

                buildList {

                    place(0)?.let {
                        add(
                            it.id to
                                it.morningRate
                        )
                    }

                    place(1)?.let {
                        add(
                            it.id to
                                it.eveningRate
                        )
                    }

                    place(2)?.let {
                        add(
                            it.id to
                                it.nightRate
                        )
                    }
                }
            }

            ShiftType.OFF ->
                emptyList()
        }
    }

    fun month(
        data: AppData,
        month: YearMonth
    ): MonthFinance {

        val monthShifts =
            data.shifts.filter {
                YearMonth.from(
                    LocalDate.parse(
                        it.date
                    )
                ) == month
            }

        val workShifts =
            monthShifts.filter {
                it.type !=
                    ShiftType.OFF
            }

        val expected =
            workShifts.sumOf {
                shiftAmount(
                    it,
                    data.workplaces
                )
            }

        val received =
            data.payments
                .filter {
                    YearMonth.from(
                        LocalDate.parse(
                            it.date
                        )
                    ) == month
                }
                .sumOf {
                    it.amount
                }

        return MonthFinance(
            shiftCount =
                workShifts.size,
            expected =
                expected,
            received =
                received,
            balance =
                expected -
                    received
        )
    }

    fun byWorkplace(
        data: AppData,
        month: YearMonth
    ): List<WorkplaceFinance> {

        val monthShifts =
            data.shifts.filter {
                YearMonth.from(
                    LocalDate.parse(
                        it.date
                    )
                ) == month &&
                it.type !=
                    ShiftType.OFF
            }

        return data.workplaces
            .mapNotNull {
                workplace ->

                var parts = 0
                var amount = 0.0

                monthShifts.forEach {
                    shift ->

                    shiftParts(
                        shift,
                        data.workplaces
                    )
                        .filter {
                            it.first ==
                                workplace.id
                        }
                        .forEach {
                            part ->

                            parts++
                            amount +=
                                part.second
                        }
                }

                if (
                    parts == 0
                ) {
                    null
                } else {
                    WorkplaceFinance(
                        workplaceId =
                            workplace.id,
                        workplaceName =
                            workplace.name,
                        shiftParts =
                            parts,
                        amount =
                            amount
                    )
                }
            }
    }

    fun byShiftType(
        data: AppData,
        month: YearMonth
    ): List<ShiftTypeFinance> {

        val shifts =
            data.shifts.filter {
                YearMonth.from(
                    LocalDate.parse(
                        it.date
                    )
                ) == month
            }

        fun result(
            type: ShiftType,
            label: String
        ): ShiftTypeFinance {

            val list =
                shifts.filter {
                    it.type == type
                }

            return ShiftTypeFinance(
                label = label,
                count = list.size,
                amount =
                    list.sumOf {
                        shiftAmount(
                            it,
                            data.workplaces
                        )
                    }
            )
        }

        return listOf(
            result(
                ShiftType.FULL_DAY,
                "24 ساعة"
            ),
            result(
                ShiftType.LONG_NIGHT,
                "Long / Night"
            ),
            result(
                ShiftType.THREE_SHIFTS,
                "Morning / Evening / Night"
            )
        ).filter {
            it.count > 0
        }
    }
}
