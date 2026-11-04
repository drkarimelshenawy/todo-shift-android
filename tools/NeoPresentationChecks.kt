package com.todoshift.app.v11

import java.time.LocalDateTime
import java.time.YearMonth

/** JVM regression fixtures; no Android runtime or test-library dependency.
 * NOT executed in the delivery environment: kotlinc is unavailable.
 */
fun main() {
    val night = ShiftSegmentV11("night", ShiftKind.NIGHT, workplaceId = "work", startMinute = 1200, endMinute = 1920)
    val fullDay = ShiftSegmentV11("full", ShiftKind.FULL_DAY, workplaceId = "work")
    val db = V11Database(
        calendars = listOf(AppCalendarV11("cal", "Work", "#43DDE7")),
        days = listOf(DayEntryV11("d1", "2026-11-04", "cal", listOf(night)),
            DayEntryV11("d2", "2026-11-06", "cal", listOf(fullDay))),
        rates = listOf(RateHistoryEntryV11("r1", "work", ShiftKind.NIGHT, "2026-01-01", 250.0),
            RateHistoryEntryV11("r2", "work", ShiftKind.FULL_DAY, "2026-01-01", 500.0)),
        payments = listOf(PaymentV11("pay", 100.0, "2026-11-04", "2026-11")),
        settlements = listOf(SettlementV11("settle", "2026-11", action = SettlementActionV11.WRITE_OFF, amount = 50.0))
    )
    val original = db.copy()
    val projected = neoShiftOccurrences(db)
    check(projected.size == 2) { "A shift must not be duplicated on its ending day" }
    val n = projected.single { it.segment.id == "night" }
    check(n.start == LocalDateTime.parse("2026-11-04T20:00:00"))
    check(n.end == LocalDateTime.parse("2026-11-05T08:00:00"))
    check(n.entry.date == "2026-11-04")
    check(projected.none { it.entry.date == "2026-11-05" })
    val f = projected.single { it.segment.id == "full" }
    check(java.time.Duration.between(f.start, f.end).toHours() == 24L)
    check(f.entry.date == "2026-11-06")
    check(neoShiftOccurrences(db, emptySet()).isEmpty())
    check(db == original) { "Presentation must not mutate stored records" }
    val overlapping = night.copy(id = "overlap", startMinute = 1260, endMinute = 1380)
    val sameDay = db.copy(days = db.days.map { if (it.id == "d1") it.copy(segments = listOf(night, overlapping)) else it })
    val projectedConflict = neoShiftOccurrences(sameDay)
    check(neoHasConflict(projectedConflict.first(), projectedConflict))
    check(!neoHasConflict(f, projectedConflict))
    val resolved = V11ShiftConflictEngine.analyse(listOf(night), overlapping)
    check(!resolved.canAddDirectly && resolved.conflicts.isNotEmpty())
    val finance = V11FinanceEngine.month(db, YearMonth.of(2026, 11))
    check(finance.expected == 750.0 && finance.received == 100.0 && finance.settlements == 50.0 && finance.difference == 600.0)
    check(NeoPalette.entries.size == 10)
    check(NeoAppearance(reduceMotion = true, motion = 2).motionMillis == 0)
    println("PASS: overnight, 24h, visibility, immutability, overlap, finance, palettes and reduced motion")
}
