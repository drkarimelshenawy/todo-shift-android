package com.todoshift.app.v11

import java.time.LocalDate
import java.time.LocalDateTime

/** Read-only projections: entries stay on their stored start day, including 24h/overnight shifts. */
data class NeoShiftOccurrence(val entry: DayEntryV11, val segment: ShiftSegmentV11, val start: LocalDateTime, val end: LocalDateTime)

fun neoShiftOccurrences(db: V11Database, calendarIds: Set<String>? = null): List<NeoShiftOccurrence> =
    db.days.asSequence().filter { calendarIds == null || it.calendarId in calendarIds }.flatMap { entry ->
        val date = runCatching { LocalDate.parse(entry.date) }.getOrNull()
        entry.segments.asSequence().mapNotNull { segment ->
            val range = segment.timeRange()
            if (date == null || range == null) null else NeoShiftOccurrence(entry, segment,
                date.atStartOfDay().plusMinutes(range.startMinute.toLong()),
                date.atStartOfDay().plusMinutes(range.endMinute.toLong()))
        }
    }.sortedBy { it.start }.toList()

fun neoHasConflict(item: NeoShiftOccurrence, all: List<NeoShiftOccurrence>): Boolean =
    all.any { other ->
        // The existing resolver's comparison scope is one stored day + calendar.
        other.entry.date == item.entry.date && other.entry.calendarId == item.entry.calendarId &&
            other.segment.id != item.segment.id && other.segment.timeRange()?.let { range ->
                item.segment.timeRange()?.overlaps(range)
            } == true
    }
