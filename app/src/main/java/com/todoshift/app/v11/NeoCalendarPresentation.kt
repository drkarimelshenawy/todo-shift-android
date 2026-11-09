package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure presentation projection. Never mutates records, durations or financial rules. */
data class NeoDayPresentation(val entries: List<DayEntryV11>, val tasks: Int, val events: Int) {
    val segments: List<ShiftSegmentV11> get() = entries.flatMap { it.segments }
}
fun neoVisibleDays(db: V11Database, visible: Set<String>): Map<String, NeoDayPresentation> {
    val days = db.days.filter { it.calendarId in visible }.groupBy { it.date }
    val tasks = db.tasks.filter { it.status != TaskStatusV11.COMPLETED && (it.calendarId == null || it.calendarId in visible) }.groupingBy { it.date }.eachCount()
    val events = db.dayItems.filter { !it.completed && (it.calendarId == null || it.calendarId in visible) }.groupingBy { it.date }.eachCount()
    return (days.keys + tasks.keys + events.keys).associateWith { NeoDayPresentation(days[it].orEmpty(), tasks[it] ?: 0, events[it] ?: 0) }
}
fun neoMoveMonth(month: YearMonth, selected: LocalDate, step: Long): LocalDate =
    month.plusMonths(step).let { it.atDay(minOf(selected.dayOfMonth, it.lengthOfMonth())) }
fun neoMonthTitle(month: YearMonth, english: Boolean): String = month.atDay(1).format(
    DateTimeFormatter.ofPattern("MMMM yyyy", if (english) Locale.ENGLISH else Locale.forLanguageTag("ar")))
fun neoSwipeStep(distance: Float, threshold: Float, rtl: Boolean): Long? =
    if (kotlin.math.abs(distance) < threshold) null else if ((distance > 0) == rtl) 1L else -1L
