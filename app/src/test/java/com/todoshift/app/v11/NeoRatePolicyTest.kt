package com.todoshift.app.v11

import com.todoshift.app.data.Workplace
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class NeoRatePolicyTest {
    private val place = Workplace("w", "Work", "#08233D", fullDayRate = 100.0)

    private fun initial() =
        V11Database(
            rates = listOf(RateHistoryEntryV11("old", "w", ShiftKind.FULL_DAY, "2020-01-01", 100.0))
        )

    @Test
    fun datedRateSurvivesLegacySyncAndPreservesEarlierAmounts() {
        val date = LocalDate.now()
        val added = NeoRatePolicy.add(initial(), "w", ShiftKind.FULL_DAY, date, 150.0)
        val synced = NeoRatePolicy.reconcile(added, listOf(place))
        assertEquals(
            100.0,
            V11RateEngine.applicableRate(
                synced.rates,
                "w",
                ShiftKind.FULL_DAY,
                date.minusDays(1).toString(),
            ),
            0.0,
        )
        assertEquals(
            150.0,
            V11RateEngine.applicableRate(synced.rates, "w", ShiftKind.FULL_DAY, date.toString()),
            0.0,
        )
        assertEquals(synced, NeoRatePolicy.reconcile(synced, listOf(place)))
        assertEquals(150.0, NeoRatePolicy.display(synced, listOf(place)).single().fullDayRate, 0.0)
    }

    @Test
    fun futureRateDoesNotChangeCurrentOrHistoricalRate() {
        val date = LocalDate.now().plusMonths(2)
        val synced =
            NeoRatePolicy.reconcile(
                NeoRatePolicy.add(initial(), "w", ShiftKind.FULL_DAY, date, 200.0),
                listOf(place),
            )
        assertEquals(100.0, NeoRatePolicy.display(synced, listOf(place)).single().fullDayRate, 0.0)
        assertEquals(
            200.0,
            V11RateEngine.applicableRate(synced.rates, "w", ShiftKind.FULL_DAY, date.toString()),
            0.0,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateEffectiveDateIsRejectedRatherThanIgnoredByRateEngine() {
        NeoRatePolicy.add(initial(), "w", ShiftKind.FULL_DAY, LocalDate.parse("2020-01-01"), 900.0)
    }

    @Test
    fun conflictReplacementPreservesUnrelatedShiftAndNeverInventsPartialRates() {
        val morning =
            ShiftSegmentV11("old", ShiftKind.FULL_DAY, "w", startMinute = 540, endMinute = 1980)
        val unrelated =
            ShiftSegmentV11("keep", ShiftKind.MORNING, "other", startMinute = 0, endMinute = 480)
        val db =
            V11Database(
                calendars = listOf(AppCalendarV11("c", "Calendar", "#08233D")),
                days = listOf(DayEntryV11("d", "2026-11-10", "c", listOf(morning, unrelated))),
            )
        val review =
            NeoPaintOperations.preview(
                db,
                setOf(LocalDate.parse("2026-11-10")),
                "c",
                ShiftKind.LONG,
                "new",
                false,
            )
        assertEquals(1, review.conflicts.size)
        assertTrue(review.conflicts.single().contains("09:00"))
        val segments = review.candidate.days.single().segments
        assertEquals(2, segments.size)
        assertEquals(unrelated, segments.first { it.id == "keep" })
        assertFalse(segments.any { it.id == "old" })
        assertEquals(db.days.single().segments.size, 2)
    }

    @Test
    fun replacingOffRequiresReviewAndOffNeverRemainsAlongsideWork() {
        val db =
            V11Database(
                calendars = listOf(AppCalendarV11("c", "Calendar", "#08233D")),
                days =
                    listOf(
                        DayEntryV11(
                            "d",
                            "2026-11-10",
                            "c",
                            listOf(ShiftSegmentV11("off", ShiftKind.OFF)),
                        )
                    ),
            )
        val review =
            NeoPaintOperations.preview(
                db,
                setOf(LocalDate.parse("2026-11-10")),
                "c",
                ShiftKind.LONG,
                "w",
                false,
            )
        assertEquals(1, review.conflicts.size)
        assertEquals(ShiftKind.LONG, review.candidate.days.single().segments.single().kind)
    }
}
