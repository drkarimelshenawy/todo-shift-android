package com.todoshift.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class RotationStep(
    val type: ShiftType,
    val workplaceIds: List<String?>,
    val customAmount: Double? = null
)

data class ScheduleOperationResult(
    val data: AppData,
    val affectedDates: Set<String>
)

object ScheduleEngine {

    fun applySelection(
        data: AppData,
        dates: Collection<LocalDate>,
        selection: PainterSelection,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        if (dates.isEmpty()) {
            return ScheduleOperationResult(
                data = data,
                affectedDates = emptySet()
            )
        }

        val targets =
            dates
                .map { it.toString() }
                .toSet()

        val existingByDate =
            data.shifts.associateBy {
                it.date
            }

        val allowedTargets =
            if (overwrite) {
                targets
            } else {
                targets.filter {
                    it !in existingByDate
                }.toSet()
            }

        val kept =
            data.shifts.filterNot {
                it.date in allowedTargets
            }

        val created =
            allowedTargets
                .sorted()
                .map { date ->
                    ShiftEntry(
                        date = date,
                        type = selection.type,
                        workplaceIds =
                            selection.workplaceIds,
                        customAmount =
                            selection.customAmount
                    )
                }

        return ScheduleOperationResult(
            data = data.copy(
                shifts = kept + created
            ),
            affectedDates =
                allowedTargets
        )
    }

    fun applyDateRange(
        data: AppData,
        start: LocalDate,
        end: LocalDate,
        selection: PainterSelection,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        val first =
            minOf(start, end)

        val last =
            maxOf(start, end)

        val dates =
            generateSequence(first) {
                current ->
                current
                    .plusDays(1)
                    .takeIf {
                        !it.isAfter(last)
                    }
            }.toList()

        return applySelection(
            data = data,
            dates = dates,
            selection = selection,
            overwrite = overwrite
        )
    }

    fun applyWeekdays(
        data: AppData,
        start: LocalDate,
        end: LocalDate,
        weekdays: Set<DayOfWeek>,
        selection: PainterSelection,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        if (weekdays.isEmpty()) {
            return ScheduleOperationResult(
                data,
                emptySet()
            )
        }

        val first =
            minOf(start, end)

        val last =
            maxOf(start, end)

        val dates =
            generateSequence(first) {
                current ->
                current
                    .plusDays(1)
                    .takeIf {
                        !it.isAfter(last)
                    }
            }
                .filter {
                    it.dayOfWeek in weekdays
                }
                .toList()

        return applySelection(
            data = data,
            dates = dates,
            selection = selection,
            overwrite = overwrite
        )
    }

    fun applyRotation(
        data: AppData,
        start: LocalDate,
        end: LocalDate,
        steps: List<RotationStep>,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        if (steps.isEmpty()) {
            return ScheduleOperationResult(
                data,
                emptySet()
            )
        }

        val first =
            minOf(start, end)

        val last =
            maxOf(start, end)

        val targetDates =
            generateSequence(first) {
                current ->
                current
                    .plusDays(1)
                    .takeIf {
                        !it.isAfter(last)
                    }
            }.toList()

        val targets =
            targetDates
                .map { it.toString() }
                .toSet()

        val existing =
            data.shifts.associateBy {
                it.date
            }

        val affected =
            mutableSetOf<String>()

        val generated =
            targetDates.mapIndexedNotNull {
                    index,
                    date ->

                val step =
                    steps[
                        index %
                            steps.size
                    ]

                val dateString =
                    date.toString()

                if (
                    !overwrite &&
                    existing.containsKey(
                        dateString
                    )
                ) {
                    null
                } else {
                    affected += dateString

                    ShiftEntry(
                        date = dateString,
                        type = step.type,
                        workplaceIds =
                            step.workplaceIds,
                        customAmount =
                            step.customAmount
                    )
                }
            }

        val kept =
            if (overwrite) {
                data.shifts.filterNot {
                    it.date in targets
                }
            } else {
                data.shifts
            }

        return ScheduleOperationResult(
            data = data.copy(
                shifts = kept + generated
            ),
            affectedDates = affected
        )
    }

    fun copyDay(
        data: AppData,
        source: LocalDate,
        targets: Collection<LocalDate>,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        val sourceEntry =
            data.shifts.firstOrNull {
                it.date ==
                    source.toString()
            }
                ?: return ScheduleOperationResult(
                    data,
                    emptySet()
                )

        return applySelection(
            data = data,
            dates = targets,
            selection =
                PainterSelection(
                    type =
                        sourceEntry.type,
                    workplaceIds =
                        sourceEntry.workplaceIds,
                    customAmount =
                        sourceEntry.customAmount
                ),
            overwrite = overwrite
        )
    }

    fun copyWeek(
        data: AppData,
        sourceWeekStart: LocalDate,
        targetWeekStart: LocalDate,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        var result = data

        val affected =
            mutableSetOf<String>()

        for (offset in 0L..6L) {

            val source =
                sourceWeekStart
                    .plusDays(offset)

            val target =
                targetWeekStart
                    .plusDays(offset)

            val sourceEntry =
                data.shifts
                    .firstOrNull {
                        it.date ==
                            source.toString()
                    }
                    ?: continue

            val operation =
                applySelection(
                    data = result,
                    dates =
                        listOf(target),
                    selection =
                        PainterSelection(
                            type =
                                sourceEntry.type,
                            workplaceIds =
                                sourceEntry
                                    .workplaceIds,
                            customAmount =
                                sourceEntry
                                    .customAmount
                        ),
                    overwrite =
                        overwrite
                )

            result =
                operation.data

            affected +=
                operation.affectedDates
        }

        return ScheduleOperationResult(
            data = result,
            affectedDates = affected
        )
    }

    fun copyMonth(
        data: AppData,
        sourceMonth: YearMonth,
        targetMonth: YearMonth,
        overwrite: Boolean = true
    ): ScheduleOperationResult {

        var result =
            data

        val affected =
            mutableSetOf<String>()

        val sourceEntries =
            data.shifts.filter {
                YearMonth.from(
                    LocalDate.parse(
                        it.date
                    )
                ) == sourceMonth
            }

        sourceEntries.forEach {
            entry ->

            val sourceDate =
                LocalDate.parse(
                    entry.date
                )

            val day =
                sourceDate
                    .dayOfMonth
                    .coerceAtMost(
                        targetMonth
                            .lengthOfMonth()
                    )

            val targetDate =
                targetMonth
                    .atDay(day)

            val operation =
                applySelection(
                    data = result,
                    dates =
                        listOf(
                            targetDate
                        ),
                    selection =
                        PainterSelection(
                            type =
                                entry.type,
                            workplaceIds =
                                entry.workplaceIds,
                            customAmount =
                                entry.customAmount
                        ),
                    overwrite =
                        overwrite
                )

            result =
                operation.data

            affected +=
                operation.affectedDates
        }

        return ScheduleOperationResult(
            data = result,
            affectedDates = affected
        )
    }

    fun clearDates(
        data: AppData,
        dates: Collection<LocalDate>
    ): ScheduleOperationResult {

        val targets =
            dates
                .map {
                    it.toString()
                }
                .toSet()

        return ScheduleOperationResult(
            data = data.copy(
                shifts =
                    data.shifts
                        .filterNot {
                            it.date in targets
                        }
            ),
            affectedDates =
                targets
        )
    }
}
