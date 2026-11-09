package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure presentation projection. Never mutates records, durations or financial rules. */
data class NeoDayPresentation(val entries: List<DayEntryV11>, val tasks: Int, val events: Int, val notes: List<DayItemV11> = emptyList()) {
    val segments: List<ShiftSegmentV11> get() = entries.flatMap { it.segments }
}
fun neoVisibleDays(db: V11Database, visible: Set<String>): Map<String, NeoDayPresentation> {
    val days = db.days.filter { it.calendarId in visible }.groupBy { it.date }
    val tasks = db.tasks.filter { it.status != TaskStatusV11.COMPLETED && (it.calendarId == null || it.calendarId in visible) }.groupingBy { it.date }.eachCount()
    val events = db.dayItems.filter { it.type != DayItemTypeV11.NOTE && !it.completed && (it.calendarId == null || it.calendarId in visible) }.groupingBy { it.date }.eachCount()
    val notes = db.dayItems.filter { it.type == DayItemTypeV11.NOTE && !it.completed && (it.calendarId == null || it.calendarId in visible) }.groupBy { it.date }
    return (days.keys + tasks.keys + events.keys + notes.keys).associateWith { NeoDayPresentation(days[it].orEmpty(), tasks[it] ?: 0, events[it] ?: 0, notes[it].orEmpty().sortedByDescending { n -> n.title.startsWith("📌") }) }
}
fun neoMoveMonth(month: YearMonth, selected: LocalDate, step: Long): LocalDate =
    month.plusMonths(step).let { it.atDay(minOf(selected.dayOfMonth, it.lengthOfMonth())) }
fun neoMonthTitle(month: YearMonth, english: Boolean): String = month.atDay(1).format(
    DateTimeFormatter.ofPattern("MMMM yyyy", if (english) Locale.ENGLISH else Locale.forLanguageTag("ar")))
fun neoSwipeStep(distance: Float, threshold: Float, rtl: Boolean): Long? =
    if (kotlin.math.abs(distance) < threshold) null else if ((distance > 0) == rtl) 1L else -1L

fun neoWeekStart(date: LocalDate, firstWeekday: Int): LocalDate = date.minusDays(((date.dayOfWeek.value - firstWeekday + 7) % 7).toLong())
