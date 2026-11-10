package com.todoshift.app.v11

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class NeoSchedulePresentationTest {
    @Test
    fun offFilterIncludesVisibleDatedRecordsWithoutInventingTimeOrLosingOwnership() {
        val visible =
            DayEntryV11(
                "off",
                "2026-11-10",
                "a",
                listOf(ShiftSegmentV11("off-segment", ShiftKind.OFF)),
            )
        val hidden = visible.copy(id = "hidden", calendarId = "b")
        val db = V11Database(days = listOf(visible, hidden))
        val rows =
            neoScheduleOffEntries(
                db,
                setOf("a"),
                "2026-11-01",
                "2026-11-30",
                NeoTimelineFilter.ALL,
                LocalDate.parse("2026-11-10"),
            )
        assertEquals(1, rows.size)
        assertEquals(visible, rows.single().first)
        assertNull(rows.single().second.timeRange())
        assertTrue(
            neoScheduleOffEntries(
                    db,
                    setOf("a"),
                    "2026-11-01",
                    "2026-11-30",
                    NeoTimelineFilter.UPCOMING,
                    LocalDate.parse("2026-11-10"),
                )
                .isEmpty()
        )
        assertEquals(listOf(visible, hidden), db.days)
    }

    @Test
    fun unknownLegacyClockCannotBeClaimedAsInProgress() {
        val day =
            DayEntryV11(
                "legacy",
                "2026-11-10",
                "a",
                listOf(ShiftSegmentV11("legacy", ShiftKind.FULL_DAY)),
            )
        val row = neoShiftOccurrences(V11Database(days = listOf(day)), setOf("a")).single()
        val now = java.time.LocalDateTime.parse("2026-11-10T10:00:00")
        assertFalse(neoScheduleMatchesPeriod(row, NeoTimelineFilter.CURRENT, now))
        assertTrue(neoScheduleMatchesPeriod(row, NeoTimelineFilter.ALL, now))
    }
}
