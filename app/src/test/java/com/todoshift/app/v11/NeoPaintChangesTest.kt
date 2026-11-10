package com.todoshift.app.v11

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class NeoPaintChangesTest {
    @Test
    fun eraserReviewNamesAllRemovedSegmentsAndPreservesOtherCalendarAndNotes() {
        val a =
            ShiftSegmentV11("am", ShiftKind.MORNING, "clinic", startMinute = 540, endMinute = 900)
        val b =
            ShiftSegmentV11(
                "pm",
                ShiftKind.EVENING,
                "hospital",
                startMinute = 900,
                endMinute = 1260,
            )
        val before =
            V11Database(
                calendars =
                    listOf(
                        AppCalendarV11("visible", "Visible", "#00A4B8"),
                        AppCalendarV11("hidden", "Hidden", "#6C3A9B", visible = false),
                    ),
                days =
                    listOf(
                        DayEntryV11("a", "2026-11-10", "visible", listOf(a, b)),
                        DayEntryV11("b", "2026-11-10", "hidden", listOf(a.copy(id = "unrelated"))),
                    ),
                dayItems =
                    listOf(
                        DayItemV11(
                            "note",
                            "2026-11-10",
                            type = DayItemTypeV11.NOTE,
                            title = "Keep fixture",
                        )
                    ),
            )
        val review =
            NeoPaintOperations.preview(
                before,
                setOf(LocalDate.parse("2026-11-10")),
                "visible",
                ShiftKind.FULL_DAY,
                null,
                true,
            )
        val change = neoPaintChanges(review).single()
        assertEquals(listOf("am", "pm"), change.before.map { it.id })
        assertTrue(change.after.isEmpty())
        assertEquals(before.dayItems, review.candidate.dayItems)
        assertEquals(before.days[1], review.candidate.days.first { it.calendarId == "hidden" })
    }

    @Test
    fun clockTextUsesStoredOvernightTimesAndOffNeverGetsInventedDuration() {
        assertEquals(
            "09:00–09:00 (+1d)",
            neoSegmentClock(
                ShiftSegmentV11("24", ShiftKind.FULL_DAY, startMinute = 540, endMinute = 1980),
                true,
            ),
        )
        assertEquals(
            "Off — no duration",
            neoSegmentClock(ShiftSegmentV11("off", ShiftKind.OFF), true),
        )
    }

    @Test
    fun legacyShiftWithoutClockTimesDoesNotInventMidnight() {
        assertEquals(
            "Clock times not recorded",
            neoSegmentClock(ShiftSegmentV11("legacy", ShiftKind.FULL_DAY), true),
        )
    }
}
