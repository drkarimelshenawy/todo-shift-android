package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class NeoCalendarPresentationTest {
    private val date = "2026-11-09"
    private fun entry(id: String, calendar: String, kind: ShiftKind) = DayEntryV11(id, date, calendar,
        listOf(ShiftSegmentV11("segment-$id", kind)))
    @Test fun singleShiftSurvivesProjectionWithItsIdentity() {
        val entry = entry("a", "visible", ShiftKind.MORNING)
        val day = neoVisibleDays(V11Database(days = listOf(entry)), setOf("visible")).getValue(date)
        assertEquals(listOf(entry), day.entries)
        assertEquals("segment-a", day.segments.single().id)
    }
    @Test fun offShiftRemainsAnIndicatorCandidateWithoutPaintCoverage() {
        val day = neoVisibleDays(V11Database(days = listOf(entry("off", "a", ShiftKind.OFF))), setOf("a")).getValue(date)
        assertEquals(1, day.segments.size)
        assertEquals(ShiftKind.OFF, day.segments.single().kind)
        assertTrue(neo4PaintBands(day.segments.map { it.kind }).isEmpty())
    }
    @Test fun allVisibleCalendarsContributeAndHiddenCalendarsDoNot() {
        val db = V11Database(days = listOf(entry("a", "a", ShiftKind.LONG), entry("b", "b", ShiftKind.NIGHT), entry("c", "c", ShiftKind.FULL_DAY)))
        assertEquals(listOf("a", "b"), neoVisibleDays(db, setOf("a", "b")).getValue(date).entries.map { it.id })
        assertEquals(listOf("c"), neoVisibleDays(db, setOf("c")).getValue(date).entries.map { it.id })
        assertTrue(neoVisibleDays(db, emptySet()).isEmpty())
        assertEquals(3, db.days.size)
    }
    @Test fun globalAndVisibleTasksAndEventsUseTheSamePolicy() {
        val db = V11Database(tasks = listOf(TaskV11("global", "Global", date = date), TaskV11("a", "A", date = date, calendarId = "a"), TaskV11("hidden", "Hidden", date = date, calendarId = "b"), TaskV11("done", "Done", date = date, status = TaskStatusV11.COMPLETED)),
            dayItems = listOf(DayItemV11("global", date, type = DayItemTypeV11.NOTE, title = "Global"), DayItemV11("hidden", date, "b", DayItemTypeV11.NOTE, "Hidden")))
        val day = neoVisibleDays(db, setOf("a")).getValue(date)
        assertEquals(2, day.tasks); assertEquals(1, day.events)
    }
    @Test fun everyNonOffShiftProducesPaint() {
        ShiftKind.entries.filter { it != ShiftKind.OFF }.forEach { assertTrue(neo4PaintBands(listOf(it)).isNotEmpty()) }
    }
    @Test fun overlappingShiftsRetainEveryOwner() {
        val bands = neo4PaintBands(listOf(ShiftKind.FULL_DAY, ShiftKind.MORNING, ShiftKind.NIGHT))
        assertEquals(setOf(0, 1, 2), bands.flatMap { it.owners }.toSet())
        assertTrue(bands.all { it.bottom > it.top })
    }
    @Test fun leapYearAndYearBoundaryClampWithoutChangingStoredDates() {
        assertEquals(LocalDate.parse("2024-02-29"), neoMoveMonth(YearMonth.of(2024, 1), LocalDate.parse("2024-01-31"), 1))
        assertEquals(LocalDate.parse("2025-02-28"), neoMoveMonth(YearMonth.of(2025, 1), LocalDate.parse("2025-01-31"), 1))
        assertEquals(LocalDate.parse("2027-01-31"), neoMoveMonth(YearMonth.of(2026, 12), LocalDate.parse("2026-12-31"), 1))
    }
    @Test fun localizedMonthNamesRemainTextual() {
        assertEquals("November 2026", neoMonthTitle(YearMonth.of(2026, 11), true))
        assertTrue(neoMonthTitle(YearMonth.of(2026, 11), false).contains("نوفمبر"))
    }
    @Test fun swipeThresholdAndDirectionAreSymmetric() {
        assertNull(neoSwipeStep(40f, 64f, false))
        assertEquals(-1L, neoSwipeStep(100f, 64f, false))
        assertEquals(1L, neoSwipeStep(-100f, 64f, false))
        assertEquals(1L, neoSwipeStep(100f, 64f, true))
        assertEquals(-1L, neoSwipeStep(-100f, 64f, true))
    }
    @Test fun primarySelectionIsNotAVisibilityMutation() {
        val db = V11Database(calendars = listOf(AppCalendarV11("a", "A", "#123456"), AppCalendarV11("b", "B", "#123456")))
        val updated = V11CalendarController.toggleVisibility(db, "b")
        assertTrue(updated.calendars.first().visible)
        assertFalse(updated.calendars.last().visible)
        assertEquals(listOf("a", "b"), updated.calendars.map { it.id })
    }
    @Test fun referencedCalendarCannotBeDeleted() {
        val db = V11Database(calendars = listOf(AppCalendarV11("a", "A", "#123456"), AppCalendarV11("b", "B", "#123456")), days = listOf(entry("a", "a", ShiftKind.OFF)))
        assertEquals(db, V11CalendarController.deleteCalendar(db, "a"))
    }
}
