package com.todoshift.app.v11.ui

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NeoNavigationTest {
    @Test
    fun rootSelectionAlwaysClosesChildrenAndBackReturnsToCalendar() {
        for (from in 0..4) for (to in 0..4) {
            val nav = NeoNavigator(from)
            nav.openFeature("APPEARANCE")
            nav.selectTab(to)
            assertEquals(to, nav.tab)
            assertTrue(nav.isRoot)
            assertFalse(nav.featureOpen)
            nav.back()
            assertEquals(0, nav.tab)
        }
    }

    @Test
    fun childHistoryKeepsOriginAndWorkplacesCannotOverlapFeature() {
        val n = NeoNavigator(3)
        n.openFeature("BACKUP")
        n.openFeature("LEGAL")
        n.back()
        assertEquals("BACKUP", n.feature)
        n.openWorkplaces()
        assertTrue(n.workplacesOpen)
        assertFalse(n.featureOpen)
        n.back()
        assertEquals("BACKUP", n.feature)
        n.back()
        assertTrue(n.isRoot)
        assertEquals(3, n.tab)
    }

    @Test
    fun deferredNavigationExecutesOnlyAfterGuardAndNestedRemovalRestoresParent() {
        val gate = NeoLeaveGate()
        var deferred: (() -> Unit)? = null
        var destination = 0
        val parent = Any()
        val child = Any()
        gate.register(parent) { deferred = it }
        gate.register(child) {}
        gate.request { destination = 1 }
        assertEquals(0, destination)
        gate.remove(child)
        gate.request { destination = 2 }
        assertEquals(0, destination)
        deferred!!()
        assertEquals(2, destination)
        gate.remove(parent)
        gate.request { destination = 3 }
        assertEquals(3, destination)
    }

    @Test
    fun entryParsingRejectsUnknownActionsAndInvalidDatesWithoutInventingDestination() {
        val e =
            NeoEntry.parse(
                Intent()
                    .putExtra("neo4_open_tab", 99)
                    .putExtra("todo_shift_task_action", "delete")
                    .putExtra("todo_shift_open_date", "2026-02-30"),
                1,
            )
        assertEquals(0, e.tab)
        assertNull(e.date)
        assertNull(e.taskAction)
        assertEquals(4, NeoEntry.parse(Intent().putExtra("todo_shift_task_action", "open"), 2).tab)
        assertEquals(2, NeoEntry.parse(Intent("com.todoshift.app.OPEN_FINANCE"), 3).tab)
    }

    @Test
    fun calendarManagerReturnsToItsSettingsOrigin() {
        val nav = NeoNavigator(3)
        nav.openCalendarManager()
        assertEquals(0, nav.tab)
        assertTrue(nav.hasParent)
        nav.back()
        assertEquals(3, nav.tab)
        assertTrue(nav.isRoot)
    }

    @Test
    fun ordinaryLauncherResumeIsNotADeferredNavigationRequest() {
        assertNull(NeoEntry.routed(Intent(Intent.ACTION_MAIN), 1))
        assertNotNull(NeoEntry.routed(Intent().putExtra("neo4_open_tab", 0), 2))
    }
}
