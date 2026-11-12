package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoPaintEditTest {
    private val date=LocalDate.parse("2026-11-12")
    private val initial=V11Database(calendars=listOf(AppCalendarV11("a","Work","#00A4B8")),
        days=listOf(DayEntryV11("day",date.toString(),"a",note="Keep this note")))
    @Test fun secondTapRemovesSameShiftAndKeepsDayNotesAndUnrelatedData() {
        val painted=NeoPaintEdit.tap(initial,date,"a",ShiftKind.FULL_DAY,"w",false).apply(initial)
        val cleared=NeoPaintEdit.tap(painted,date,"a",ShiftKind.FULL_DAY,"w",false).apply(painted)
        assertTrue(cleared.days.single().segments.isEmpty())
        assertEquals("Keep this note",cleared.days.single().note)
        assertEquals(initial.tasks,cleared.tasks)
    }
    @Test fun changingWorkplaceReplacesConflictingShiftDirectlyAndEraseKeepsNotes() {
        val first=NeoPaintEdit.tap(initial,date,"a",ShiftKind.FULL_DAY,"w",false).apply(initial)
        val next=NeoPaintEdit.tap(first,date,"a",ShiftKind.FULL_DAY,"other",false).apply(first)
        assertEquals("other",next.days.single().segments.single().workplaceId)
        val erased=NeoPaintEdit.tap(next,date,"a",ShiftKind.FULL_DAY,"other",true).apply(next)
        assertTrue(erased.days.single().segments.isEmpty())
        assertEquals("Keep this note",erased.days.single().note)
    }
    @Test fun restoredEditIsIdempotentAfterCommitAndCannotOverwriteExternalEdits() {
        val edit=NeoPaintEdit.tap(initial,date,"a",ShiftKind.MORNING,"w",false)
        val restored=NeoPaintEdit.decode(edit.encode())
        val saved=restored.apply(initial)
        assertEquals(saved,restored.apply(saved))
        val external=NeoPaintEdit.tap(saved,date,"a",ShiftKind.FULL_DAY,"other",false).apply(saved)
        try { restored.apply(external); fail("Must reject stale edits") } catch(_:IllegalStateException) {}
    }
    @Test fun rapidPendingStrokesReplayInOrderAcrossDifferentCells() {
        val first=NeoPaintEdit.tap(initial,date,"a",ShiftKind.FULL_DAY,"w",false)
        val one=first.apply(initial)
        val second=NeoPaintEdit.tap(one,date.plusDays(1),"a",ShiftKind.NIGHT,"w",false)
        val two=second.apply(one)
        val third=NeoPaintEdit.tap(two,date,"a",ShiftKind.FULL_DAY,"w",false)
        val replay=listOf(first,second,third).map { NeoPaintEdit.decode(it.encode()) }
            .fold(initial) { db,edit -> edit.apply(db) }
        assertTrue(replay.days.first { it.date==date.toString() }.segments.isEmpty())
        assertEquals(ShiftKind.NIGHT,replay.days.first { it.date!=date.toString() }.segments.single().kind)
    }
    @Test fun monthOutlineIncludesBothStepsAndKeepsOutsideShiftDates() {
        val month=YearMonth.of(2026,10)
        val dates=NeoMonthGrid.dates(month,6)
        assertEquals(6,dates.first().dayOfWeek.value)
        assertEquals(0,dates.size%7)
        assertTrue(dates.any { YearMonth.from(it)!=month })
        assertTrue(NeoMonthGrid.edges(month.atDay(1),month,6).start)
        assertTrue(NeoMonthGrid.edges(month.atEndOfMonth(),month,6).end)
        assertFalse(NeoMonthGrid.edges(month.atDay(15),month,6).top)
        assertEquals(month.lengthOfMonth(),dates.count { YearMonth.from(it)==month })
    }
    @Test fun allWeekStartsAndLeapYearsHaveCompleteContiguousGrids() {
        for(year in 2024..2027) for(m in 1..12) for(start in 1..7) {
            val month=YearMonth.of(year,m);val dates=NeoMonthGrid.dates(month,start)
            assertEquals(start,dates.first().dayOfWeek.value)
            assertEquals(month.lengthOfMonth(),dates.count { YearMonth.from(it)==month })
            dates.zipWithNext().forEach { (a,b)-> assertEquals(a.plusDays(1),b) }
        }
    }
}
