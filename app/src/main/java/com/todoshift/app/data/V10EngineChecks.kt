package com.todoshift.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object V10EngineChecks {

    fun compileCheck(
        data: AppData,
        selection: PainterSelection
    ) {

        val today =
            LocalDate.now()

        val state =
            SmartToolsState(
                startDate = today,
                endDate =
                    today.plusDays(30),
                selectedWeekdays =
                    setOf(
                        DayOfWeek.SATURDAY,
                        DayOfWeek.TUESDAY
                    )
            )

        SmartToolsController.applyRange(
            data,
            state,
            selection
        )

        SmartToolsController.applyWeekdays(
            data,
            state,
            selection
        )

        SmartToolsController.copyDay(
            data,
            today,
            today.plusDays(1),
            true
        )

        SmartToolsController.copyWeek(
            data,
            today,
            today.plusWeeks(1),
            true
        )

        SmartToolsController.copyMonth(
            data,
            YearMonth.now(),
            YearMonth.now().plusMonths(1),
            true
        )
    }
}
