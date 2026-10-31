package com.todoshift.app.system

import com.todoshift.app.data.AppData
import com.todoshift.app.data.FinanceEngine
import com.todoshift.app.data.ShiftType
import java.time.LocalDate

data class TodaySnapshot(
    val date: LocalDate,
    val hasShift: Boolean,
    val title: String,
    val workplaces: String,
    val expectedAmount: Double
)

object TodaySnapshotEngine {

    fun build(
        data: AppData,
        date: LocalDate =
            LocalDate.now()
    ): TodaySnapshot {

        val shift =
            data.shifts
                .firstOrNull {
                    it.date ==
                        date.toString()
                }

        if (shift == null) {
            return TodaySnapshot(
                date = date,
                hasShift = false,
                title = "لا يوجد شيفت اليوم",
                workplaces = "",
                expectedAmount = 0.0
            )
        }

        val names =
            shift.workplaceIds
                .filterNotNull()
                .mapNotNull { id ->
                    data.workplaces
                        .firstOrNull {
                            it.id == id
                        }
                        ?.name
                }
                .joinToString(" / ")

        val title =
            when (shift.type) {

                ShiftType.FULL_DAY ->
                    "شيفت 24 ساعة"

                ShiftType.LONG_NIGHT ->
                    "Long / Night"

                ShiftType.THREE_SHIFTS ->
                    "3 شيفتات"

                ShiftType.OFF ->
                    "إجازة"
            }

        return TodaySnapshot(
            date = date,
            hasShift =
                shift.type !=
                    ShiftType.OFF,
            title = title,
            workplaces = names,
            expectedAmount =
                FinanceEngine
                    .shiftAmount(
                        shift,
                        data.workplaces
                    )
        )
    }
}
