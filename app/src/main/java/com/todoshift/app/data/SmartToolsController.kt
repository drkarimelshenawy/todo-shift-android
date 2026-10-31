package com.todoshift.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class SmartToolMode {
    RANGE,
    WEEKDAYS,
    ROTATION,
    COPY_DAY,
    COPY_WEEK,
    COPY_MONTH
}

data class SmartToolsState(
    val mode: SmartToolMode = SmartToolMode.RANGE,
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val selectedWeekdays: Set<DayOfWeek> = emptySet(),
    val overwrite: Boolean = true
)

object SmartToolsController {

    fun applyRange(
        data: AppData,
        state: SmartToolsState,
        selection: PainterSelection
    ): ScheduleOperationResult =
        ScheduleEngine.applyDateRange(
            data = data,
            start = state.startDate,
            end = state.endDate,
            selection = selection,
            overwrite = state.overwrite
        )

    fun applyWeekdays(
        data: AppData,
        state: SmartToolsState,
        selection: PainterSelection
    ): ScheduleOperationResult =
        ScheduleEngine.applyWeekdays(
            data = data,
            start = state.startDate,
            end = state.endDate,
            weekdays = state.selectedWeekdays,
            selection = selection,
            overwrite = state.overwrite
        )

    fun applyRotation(
        data: AppData,
        state: SmartToolsState,
        steps: List<RotationStep>
    ): ScheduleOperationResult =
        ScheduleEngine.applyRotation(
            data = data,
            start = state.startDate,
            end = state.endDate,
            steps = steps,
            overwrite = state.overwrite
        )

    fun copyDay(
        data: AppData,
        source: LocalDate,
        target: LocalDate,
        overwrite: Boolean
    ): ScheduleOperationResult =
        ScheduleEngine.copyDay(
            data = data,
            source = source,
            targets = listOf(target),
            overwrite = overwrite
        )

    fun copyWeek(
        data: AppData,
        source: LocalDate,
        target: LocalDate,
        overwrite: Boolean
    ): ScheduleOperationResult =
        ScheduleEngine.copyWeek(
            data = data,
            sourceWeekStart = source,
            targetWeekStart = target,
            overwrite = overwrite
        )

    fun copyMonth(
        data: AppData,
        source: YearMonth,
        target: YearMonth,
        overwrite: Boolean
    ): ScheduleOperationResult =
        ScheduleEngine.copyMonth(
            data = data,
            sourceMonth = source,
            targetMonth = target,
            overwrite = overwrite
        )
}
