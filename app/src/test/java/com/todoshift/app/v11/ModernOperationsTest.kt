package com.todoshift.app.v11

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class ModernOperationsTest {
    @Test
    fun undoCannotMutatePrivateFinancialRecordsWhileLocked() {
        val empty = V11Database()
        val paid = empty.copy(payments = listOf(PaymentV11("p", 10.0, "2026-11-10", "2026-11")))
        assertFalse(ModernOperations.canUndo(paid, empty, false))
        assertTrue(ModernOperations.canUndo(paid, empty, true))
        val task = paid.copy(tasks = listOf(TaskV11("t", "Synthetic", date = "2026-11-10")))
        assertTrue(ModernOperations.canUndo(task, paid, false))
    }

    @Test
    fun localizedMoneyAcceptsEveryApprovedExampleWithoutChangingValue() {
        for (s in listOf("9000", "٩٠٠٠", "۹۰۰۰", "9,000", "٩٬٠٠٠", "9٬٠۰۰")) assertEquals(
            BigDecimal("9000.00"),
            LocalizedNumbers.money(s),
        )
        assertEquals(BigDecimal("9000.50"), LocalizedNumbers.money("٩٠٠٠٫٥٠"))
        assertEquals(BigDecimal("9000.50"), LocalizedNumbers.money("۹٬۰۰۰٫۵۰"))
    }

    @Test
    fun invalidOrAmbiguousMoneyNeverSilentlyChangesMeaning() {
        for (s in
            listOf(
                "9,00",
                "1.2.3",
                "NaN",
                "Infinity",
                "-1",
                "1e3",
                "",
                "٩٫",
                "1,234,56",
            )) assertNull(s, LocalizedNumbers.money(s))
        assertEquals(BigDecimal("1.01"), LocalizedNumbers.money("1.005"))
    }

    @Test
    fun explicitPaymentAllocationsOverrideLegacyMonthWithoutDoubleCounting() {
        val p = PaymentV11("p", 100.01, "2026-11-10", "2026-11")
        val db =
            ModernOperations.distributePayment(
                V11Database(payments = listOf(p)),
                "p",
                listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12)),
            )
        assertEquals(50.01, V11FinanceEngine.month(db, YearMonth.of(2026, 11)).received, 0.0)
        assertEquals(50.0, V11FinanceEngine.month(db, YearMonth.of(2026, 12)).received, 0.0)
        assertEquals(
            10001,
            db.payments.single().allocations.sumOf { LocalizedNumbers.cents(it.amount) }.toInt(),
        )
        assertEquals(100.01, db.payments.single().amount, 0.0)
    }

    @Test
    fun legacyPaymentStillBelongsToExactlyOneMonth() {
        val db = V11Database(payments = listOf(PaymentV11("p", 23.5, "2026-11-10", "2026-11")))
        assertEquals(23.5, V11FinanceEngine.month(db, YearMonth.of(2026, 11)).received, 0.0)
        assertEquals(0.0, V11FinanceEngine.month(db, YearMonth.of(2026, 12)).received, 0.0)
    }

    @Test
    fun quarterBandsDoNotChangeActualStoredShiftDurations() {
        assertEquals(.25f, neo4Coverage(ShiftKind.MORNING)!!.bottom, 0f)
        assertEquals(.25f, neo4Coverage(ShiftKind.EVENING)!!.top, 0f)
        assertEquals(.5f, neo4Coverage(ShiftKind.EVENING)!!.bottom, 0f)
        assertEquals(480, ShiftKind.MORNING.defaultTimeRangeV11()!!.durationMinutes)
        assertNull(neo4Coverage(ShiftKind.OFF))
    }

    @Test
    fun paintingUsesExplicitNewClockTimesAndNeverRewritesHistoricalDates() {
        val db = V11Database(calendars = listOf(AppCalendarV11("c", "C", "#123456")))
        val review =
            NeoPaintOperations.preview(
                db,
                setOf(LocalDate.parse("2026-11-10")),
                "c",
                ShiftKind.FULL_DAY,
                "w",
                false,
            )
        assertTrue(review.conflicts.isEmpty())
        assertEquals(540, review.candidate.days.single().segments.single().startMinute)
        assertEquals(1980, review.candidate.days.single().segments.single().endMinute)
        assertTrue(db.days.isEmpty())
    }

    @Test
    fun eraserPreservesIndependentNotesAndTheDayNote() {
        val db =
            V11Database(
                calendars = listOf(AppCalendarV11("c", "C", "#123456")),
                days =
                    listOf(
                        DayEntryV11(
                            "d",
                            "2026-11-10",
                            "c",
                            listOf(ShiftSegmentV11("s", ShiftKind.FULL_DAY, "w")),
                            "Keep",
                        )
                    ),
                dayItems =
                    listOf(
                        DayItemV11(
                            "n",
                            "2026-11-10",
                            type = DayItemTypeV11.NOTE,
                            title = "Independent",
                        )
                    ),
            )
        val next =
            NeoPaintOperations.preview(
                    db,
                    setOf(LocalDate.parse("2026-11-10")),
                    "c",
                    ShiftKind.FULL_DAY,
                    "w",
                    true,
                )
                .candidate
        assertTrue(next.days.single().segments.isEmpty())
        assertEquals("Keep", next.days.single().note)
        assertEquals(db.dayItems, next.dayItems)
    }

    @Test
    fun copiesPreviewConflictsWithoutDeletingAnything() {
        val entry =
            DayEntryV11(
                "d",
                "2026-11-10",
                "c",
                listOf(ShiftSegmentV11("s", ShiftKind.FULL_DAY, "w")),
            )
        val target =
            entry.copy(
                id = "target",
                date = "2026-11-11",
                segments = listOf(ShiftSegmentV11("other", ShiftKind.FULL_DAY, "w")),
            )
        val db = V11Database(days = listOf(entry, target))
        val p =
            ModernOperations.previewCopy(
                db,
                LocalDate.parse(entry.date),
                LocalDate.parse(entry.date),
                LocalDate.parse(target.date),
            )
        assertEquals(1, p.conflicts.size)
        assertEquals(db, p.candidate)
    }

    @Test
    fun statisticsCountDistinctWorkedDaysSeparatelyFromSegmentsAndOff() {
        val db =
            V11Database(
                days =
                    listOf(
                        DayEntryV11(
                            "d",
                            "2026-11-10",
                            "c",
                            listOf(
                                ShiftSegmentV11("a", ShiftKind.MORNING, "w"),
                                ShiftSegmentV11("b", ShiftKind.NIGHT, "w"),
                            ),
                        ),
                        DayEntryV11(
                            "off",
                            "2026-11-11",
                            "c",
                            listOf(ShiftSegmentV11("o", ShiftKind.OFF)),
                        ),
                    )
            )
        val s = ModernOperations.stats(db, YearMonth.of(2026, 11))
        assertEquals(2, s.shifts)
        assertEquals(1, s.workedDays)
        assertEquals(1, s.off)
    }

    @Test
    fun weekStartsHonorAllThreeOptions() {
        val d = LocalDate.parse("2026-11-10")
        assertEquals("2026-11-07", neoWeekStart(d, 6).toString())
        assertEquals("2026-11-08", neoWeekStart(d, 7).toString())
        assertEquals("2026-11-09", neoWeekStart(d, 1).toString())
    }
}
