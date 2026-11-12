package com.todoshift.app.v11

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class PremiumUxQueryTest {
    private val tasks=listOf(
        TaskV11("late","Late",date="2026-11-11",priority=TaskPriorityV11.URGENT),
        TaskV11("done","Done",date="2026-11-11",status=TaskStatusV11.COMPLETED),
        TaskV11("now","Today",date="2026-11-12"),TaskV11("next","Later",date="2026-11-13"))
    @Test fun statusAndDateFiltersAreIndependentIncludingCompletedOverdueTasks() {
        assertEquals(listOf("late"),NeoTaskQueries.select(tasks,"2026-11-12",NeoTaskPeriod.OVERDUE,NeoTaskStatus.ACTIVE).map {it.id})
        assertEquals(listOf("done"),NeoTaskQueries.select(tasks,"2026-11-12",NeoTaskPeriod.OVERDUE,NeoTaskStatus.COMPLETED).map {it.id})
        assertEquals(listOf("next"),NeoTaskQueries.select(tasks,"2026-11-12",NeoTaskPeriod.UPCOMING,NeoTaskStatus.ALL).map {it.id})
    }
    @Test fun explicitDoneViewOverridesAutomaticCompletedHiding() {
        val options=NeoTaskListOptions(hideCompleted=true)
        assertFalse(NeoTaskQueries.select(tasks,"2026-11-12",NeoTaskPeriod.ALL_DATES,NeoTaskStatus.ALL,options=options).any {it.id=="done"})
        assertEquals("done",NeoTaskQueries.select(tasks,"2026-11-12",NeoTaskPeriod.ALL_DATES,NeoTaskStatus.COMPLETED,options=options).single().id)
    }
    @Test fun manualOrderPersistsAcrossStoreInstancesWithoutChangingTaskRecords() {
        val c=RuntimeEnvironment.getApplication();val store=NeoSmartStore(c)
        val moved=NeoTaskQueries.move(tasks,emptyList(),tasks,"next",-1)
        val value=NeoTaskListOptions(compact=false,sort=NeoTaskSort.MANUAL,order=moved)
        store.update {it.put("taskListOptions",value.json())}
        assertEquals(value,NeoTaskListOptions.read(NeoSmartStore(c).snapshot()))
        val result=NeoTaskQueries.select(tasks,"2026-11-12",NeoTaskPeriod.ALL_DATES,NeoTaskStatus.ALL,options=value)
        assertEquals(listOf("late","done","next","now"),result.map {it.id})
        assertEquals(tasks.associateBy {it.id},result.associateBy {it.id})
    }
    @Test fun prioritySearchAndCalendarAreCombinedWithoutMutatingSource() {
        val task=tasks[0].copy(calendarId="work",details="Clinic visit")
        assertEquals(listOf(task.copy(id="other",calendarId="hidden")),NeoTaskQueries.select(tasks+task.copy(id="other",calendarId="hidden"),"2026-11-12",NeoTaskPeriod.ALL_DATES,NeoTaskStatus.ALL,search="Clinic",calendar="hidden"))
    }
    @Test fun februaryCanHaveFourRowsWhileOtherMonthsUseFiveOrSix() {
        assertEquals(28,NeoMonthGrid.dates(YearMonth.of(2021,2),1).size)
        assertEquals(35,NeoMonthGrid.dates(YearMonth.of(2026,4),1).size)
        assertEquals(42,NeoMonthGrid.dates(YearMonth.of(2026,8),1).size)
    }
}
