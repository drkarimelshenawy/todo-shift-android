package com.todoshift.app.v11

import java.time.LocalDate
import java.util.UUID

data class NeoPaintReview(
    val before: V11Database,
    val candidate: V11Database,
    val conflicts: List<String>,
)

object NeoPaintOperations {
    fun preview(
        db: V11Database,
        dates: Set<LocalDate>,
        calendar: String,
        kind: ShiftKind,
        workplace: String?,
        erase: Boolean,
    ): NeoPaintReview {
        require(db.calendars.any { it.id == calendar })
        require(erase || kind == ShiftKind.OFF || !workplace.isNullOrBlank())
        var next = db
        val conflicts = mutableListOf<String>()
        dates.sorted().forEach { date ->
            val key = date.toString()
            require(db.monthSnapshots.none { it.month == key.take(7) && it.reopenedAt == null }) {
                "Month is closed; reopen it before painting"
            }
            val old = next.days.firstOrNull { it.date == key && it.calendarId == calendar }
            if (erase) {
                if (old != null)
                    next =
                        next.copy(
                            days =
                                next.days.map {
                                    if (it.id == old.id) it.copy(segments = emptyList()) else it
                                }
                        )
            } else {
                val base = kind.defaultTimeRangeV11()
                val incoming =
                    ShiftSegmentV11(
                        UUID.randomUUID().toString(),
                        kind,
                        if (kind == ShiftKind.OFF) null else workplace,
                        startMinute = base?.startMinute?.plus(540),
                        endMinute = base?.endMinute?.plus(540),
                    )
                val existing = old?.segments.orEmpty()
                val analysis = V11ShiftConflictEngine.analyse(existing, incoming)
                val segments =
                    if (analysis.canAddDirectly) {
                        if (existing.any { it.kind == ShiftKind.OFF })
                            conflicts += "$key • OFF → $kind"
                        existing.filterNot { it.kind == ShiftKind.OFF } + incoming
                    } else {
                        fun describe(segment: ShiftSegmentV11): String {
                            val range = segment.timeRange() ?: return segment.kind.name
                            fun clock(minute: Int): String =
                                "%02d:%02d"
                                    .format(
                                        java.util.Locale.ROOT,
                                        (minute / 60) % 24,
                                        minute % 60,
                                    ) + if (minute >= 1440) " (+${minute / 1440}d)" else ""
                            return "${segment.kind} ${clock(range.startMinute)}–${clock(range.endMinute)}"
                        }
                        val replaced =
                            if (kind == ShiftKind.OFF) existing
                            else
                                existing.filter { oldSegment ->
                                    analysis.conflicts.any { it.existing.id == oldSegment.id }
                                }
                        conflicts +=
                            "$key • ${replaced.joinToString { describe(it) }} → ${describe(incoming)}"
                        // Explicit whole-segment replacement: never silently split a paid shift
                        // into inferred shift kinds, which could change its price.
                        existing.filterNot { oldSegment ->
                            replaced.any { it.id == oldSegment.id } ||
                                oldSegment.kind == ShiftKind.OFF
                        } + incoming
                    }
                val day =
                    (old ?: DayEntryV11(UUID.randomUUID().toString(), key, calendar)).copy(
                        segments = segments
                    )
                next =
                    next.copy(
                        days =
                            next.days.filterNot { it.date == key && it.calendarId == calendar } +
                                day
                    )
            }
        }
        return NeoPaintReview(db, next, conflicts)
    }
}
