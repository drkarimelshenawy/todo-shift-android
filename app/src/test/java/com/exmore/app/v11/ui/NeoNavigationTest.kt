package com.exmore.app.v11.ui

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
        assertEquals(1, NeoEntry.parse(Intent("com.exmore.app.OPEN_FINANCE"), 3).tab)
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

    @Test
    fun textShareRoutesOnlyToNewNoteWithoutSavingDataOrExposingAttachmentUris() {
        val share=Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT,"https://example.org/test")
        val entry=NeoEntry.routed(share,123L)
        assertNotNull(entry)
        assertEquals(2,entry!!.tab)
        assertEquals("new_note",entry.taskAction)
        assertEquals("https://example.org/test",entry.sharedNoteText)
        assertNull(entry.taskId)
        assertNull(NeoEntry.routed(Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_TEXT,"private image"),2L))
        assertNull(NeoEntry.routed(Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT,"   "),3L))
    }

    @Test
    fun directNewNoteShortcutAndShareTextLimitAreRecognized() {
        val shortcut=NeoEntry.routed(Intent(Intent.ACTION_VIEW)
            .putExtra("todo_shift_task_action","new_note"),4L)
        assertEquals("new_note",shortcut!!.taskAction)
        assertNull(shortcut.sharedNoteText)
        val huge="ن".repeat(18_000)
        val shared=NeoEntry.routed(Intent(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT,huge),5L)
        assertEquals(16_000,shared!!.sharedNoteText!!.length)
    }
}
