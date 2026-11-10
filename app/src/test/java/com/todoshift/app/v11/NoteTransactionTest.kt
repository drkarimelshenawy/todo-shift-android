package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4LocalBackup
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NoteTransactionTest {
    @Test
    fun textAndStyleReopenTogether() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val meta = NeoSmartStore(c).snapshot().toString()
        val note =
            DayItemV11(
                "fixture-note",
                "2026-11-10",
                type = DayItemTypeV11.NOTE,
                title = "Pinned fixture",
            )
        val next = Neo4LocalBackup(c).saveNote(before, meta, note, "#00A4B8", true)
        assertEquals(next, V11Runtime(c).start())
        assertEquals("#00A4B8", NeoSmartStore(c).noteColor(note.id))
        assertTrue(NeoSmartStore(c).snapshot().getJSONObject("noteBorders").getBoolean(note.id))
    }

    @Test
    fun processInterruptionAtEveryWriteBoundaryRecoversBothStores() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val note =
            DayItemV11(
                "fixture-note",
                "2026-11-10",
                type = DayItemTypeV11.NOTE,
                title = "New fixture",
            )
        for (boundary in listOf("journal", "text", "style")) {
            val meta = NeoSmartStore(c).snapshot().toString()
            try {
                Neo4LocalBackup(c).saveNote(before, meta, note, "#00A4B8", true) {
                    if (it == boundary) throw AssertionError("Simulated process interruption")
                }
                fail("Must interrupt")
            } catch (expected: AssertionError) {}
            assertEquals(before, V11Runtime(c).start())
            assertEquals(meta, NeoSmartStore(c).snapshot().toString())
        }
    }

    @Test
    fun failedWriteRollsBackAndConcurrentMetadataIsNeverOverwritten() {
        val c = RuntimeEnvironment.getApplication()
        val before = V11Runtime(c).start()
        val meta = NeoSmartStore(c).snapshot().toString()
        val note =
            DayItemV11(
                "fixture-note",
                "2026-11-10",
                type = DayItemTypeV11.NOTE,
                title = "New fixture",
            )
        try {
            Neo4LocalBackup(c).saveNote(before, meta, note, "#00A4B8", true) {
                if (it == "text") error("Injected I/O")
            }
            fail("Must fail")
        } catch (expected: IllegalStateException) {}
        assertEquals(before, V11Runtime(c).start())
        assertEquals(meta, NeoSmartStore(c).snapshot().toString())
        NeoSmartStore(c).setNoteColor("unrelated", "#6C3A9B")
        try {
            Neo4LocalBackup(c).saveNote(before, meta, note, "#00A4B8", true)
            fail("Stale metadata must conflict")
        } catch (expected: IllegalStateException) {}
        assertEquals(before, V11Runtime(c).start())
        assertEquals("#6C3A9B", NeoSmartStore(c).noteColor("unrelated"))
    }
}
