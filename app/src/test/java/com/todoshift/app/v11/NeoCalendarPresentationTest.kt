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
        assertEquals(2, day.tasks); assertEquals(0, day.events); assertEquals(1, day.notes.size)
    }
    @Test fun privateArchivedAndTrashedNotesNeverLeakIntoCalendarProjection() {
        val note = DayItemV11("public", date, type = DayItemTypeV11.NOTE, title = "Visible")
        val db = V11Database(dayItems = listOf(note,
            note.copy(id = "locked", title = "Secret", noteLocked = true),
            note.copy(id = "hidden", title = "Secret", noteHidden = true),
            note.copy(id = "archived", noteArchived = true),
            note.copy(id = "trashed", noteTrashed = true)))
        assertEquals(listOf("public"), neoVisibleDays(db, emptySet()).getValue(date).notes.map { it.id })
    }
    @Test fun sectionProtectedTaskContentDoesNotLeakThroughCalendarCountsOrTitles() {
        val data = V11Database(
            tasks = listOf(TaskV11("task", "Private task", date = date)),
            dayItems = listOf(
                DayItemV11("note", date, type = DayItemTypeV11.NOTE, title = "Private note"),
                DayItemV11("legacy-task", date, type = DayItemTypeV11.TASK, title = "Private legacy task"),
                DayItemV11("event", date, type = DayItemTypeV11.ERRAND, title = "Public event"),
            ),
        )
        val day = neoVisibleDays(data, emptySet(), includeTaskContent = false).getValue(date)
        assertEquals(0, day.tasks)
        assertTrue(day.notes.isEmpty())
        assertEquals(1, day.events)
        assertEquals(3, data.dayItems.size)
        assertEquals(1, data.tasks.size)
    }
    @Test fun approvedShiftCoverageUsesNamedKindsWithoutChangingActualDurations() {
        val expected = mapOf(
            ShiftKind.FULL_DAY to Neo4Coverage(0f, 1f),
            ShiftKind.LONG to Neo4Coverage(0f, .5f),
            ShiftKind.NIGHT to Neo4Coverage(.5f, 1f),
            ShiftKind.MORNING to Neo4Coverage(0f, .25f),
            ShiftKind.EVENING to Neo4Coverage(.25f, .5f),
        )
        expected.forEach { (kind, coverage) -> assertEquals(coverage, neo4Coverage(kind)) }
        assertNull(neo4Coverage(ShiftKind.OFF))
        val recorded = ShiftSegmentV11("custom", ShiftKind.MORNING, startMinute = 480, endMinute = 960)
        assertEquals(Neo4Coverage(0f, .25f), neo4Coverage(recorded.kind))
        assertEquals(480, recorded.timeRange()!!.durationMinutes)
    }
    @Test fun dateNumbersAndNotesDoNotShrinkApprovedFullCellPaintBands() {
        // 23/24/25/26/27 November: FULL_DAY/Long/Morning/Evening/Night.
        // The visual bands use the entire cell even when a date or note is overlaid.
        val expected = listOf(
            ShiftKind.FULL_DAY to Neo4PaintBand(0f, 1f, listOf(0)),
            ShiftKind.LONG to Neo4PaintBand(0f, .5f, listOf(0)),
            ShiftKind.MORNING to Neo4PaintBand(0f, .25f, listOf(0)),
            ShiftKind.EVENING to Neo4PaintBand(.25f, .5f, listOf(0)),
            ShiftKind.NIGHT to Neo4PaintBand(.5f, 1f, listOf(0)),
        )
        expected.forEach { (kind, band) ->
            assertEquals("Full-cell paint coverage for $kind", listOf(band), neo4PaintBands(listOf(kind)))
        }
        // Painting remains presentational: original custom start/end times are preserved.
        val custom = ShiftSegmentV11("actual", ShiftKind.LONG, startMinute = 420, endMinute = 900)
        assertEquals(Neo4Coverage(0f, .5f), neo4Coverage(custom.kind))
        assertEquals(480, custom.timeRange()!!.durationMinutes)
    }

    @Test fun lastCalendarIsRetainedEvenWhenItIsHiddenAndEmpty() {
        val data = V11Database(calendars = listOf(AppCalendarV11("only", "Only", "#123456", visible = false)))
        assertEquals(data, V11CalendarController.deleteCalendar(data, "only"))
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

    @Test fun calendarTransferRetainsRecordIdsAndRejectsAmbiguousTargetDates() {
        val original=entry("a","from",ShiftKind.MORNING)
        val db=V11Database(calendars=listOf(AppCalendarV11("from","From","#112233"),AppCalendarV11("to","To","#334455")),days=listOf(original),tasks=listOf(TaskV11("t","Task",date=date,calendarId="from")))
        val moved=V11CalendarController.transferAndDelete(db,"from","to")
        assertEquals(original.copy(calendarId="to"),moved.days.single())
        assertEquals("t",moved.tasks.single().id);assertEquals("to",moved.tasks.single().calendarId)
        assertEquals(listOf("to"),moved.calendars.map {it.id})
        assertThrows(IllegalArgumentException::class.java) {V11CalendarController.transferAndDelete(db.copy(days=db.days+entry("b","to",ShiftKind.NIGHT)),"from","to")}
    }

}
