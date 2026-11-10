package com.todoshift.app.v11

import java.time.LocalDate

/** OFF has a date and ownership, never a fabricated clock window. */
fun neoScheduleOffEntries(
    db: V11Database,
    visible: Set<String>,
    from: String,
    to: String,
    filter: NeoTimelineFilter,
    today: LocalDate,
): List<Pair<DayEntryV11, ShiftSegmentV11>> =
    db.days
        .filter { day ->
            day.calendarId in visible &&
                day.date in from..to &&
                when (filter) {
                    NeoTimelineFilter.ALL -> true
                    NeoTimelineFilter.CURRENT -> day.date == today.toString()
                    NeoTimelineFilter.UPCOMING -> day.date > today.toString()
                    NeoTimelineFilter.PAST -> day.date < today.toString()
                }
        }
        .sortedBy { it.date }
        .flatMap { day -> day.segments.filter { it.kind == ShiftKind.OFF }.map { day to it } }

fun neoScheduleMatchesPeriod(
    item: NeoShiftOccurrence,
    filter: NeoTimelineFilter,
    now: java.time.LocalDateTime,
): Boolean {
    if (filter == NeoTimelineFilter.ALL) return true
    val recorded = item.segment.startMinute != null && item.segment.endMinute != null
    return when (filter) {
        NeoTimelineFilter.ALL -> true
        NeoTimelineFilter.CURRENT -> recorded && now >= item.start && now < item.end
        NeoTimelineFilter.UPCOMING ->
            if (recorded) item.start > now else item.entry.date > now.toLocalDate().toString()
        NeoTimelineFilter.PAST ->
            if (recorded) item.end <= now else item.entry.date < now.toLocalDate().toString()
    }
}
