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
    @Test fun protectedLegacyRecordWithoutOptionalMetadataMigratesWithoutChangingItsFields() {
        val context=RuntimeEnvironment.getApplication();val base=V11Runtime(context).start()
        val storage=V11Storage(context)
        val record=DayItemV11("legacy-protected","2026-11-14",type=DayItemTypeV11.NOTE,title="Legacy secret",details="Retain me",noteLocked=true)
        val raw=org.json.JSONObject(storage.exportJson(base.copy(dayItems=listOf(record))))
        raw.getJSONArray("dayItems").getJSONObject(0).remove("noteTags")
        context.getSharedPreferences("todo_shift_v11",android.content.Context.MODE_PRIVATE).edit().putString("database",raw.toString()).commit()
        assertEquals(record,V11Runtime(context).start().dayItems.single())
        val persisted=context.getSharedPreferences("todo_shift_v11",android.content.Context.MODE_PRIVATE).getString("database",null)!!
        assertFalse(persisted.contains("Legacy secret"))
        assertFalse(persisted.contains("Retain me"))
    }

    @Test fun linkingAndUnlinkingExistingTaskRetainsBothRecords() {
        val context=RuntimeEnvironment.getApplication()
        val base=V11Runtime(context).start()
        val note=DayItemV11("link-note","2026-11-14",type=DayItemTypeV11.NOTE,title="Link",details="Preserved")
        val task=TaskV11("existing-task","Existing",date=note.date)
        val withTask=base.copy(tasks=base.tasks+task)
        V11Runtime(context).save(withTask)
        val backup=Neo4LocalBackup(context)
        val linked=backup.saveNote(withTask,NeoSmartStore(context).snapshot().toString(),note,"#00A4B8",false,linkedTasks=listOf(task.copy(linkedNoteId=note.id)))
        assertEquals(1,linked.tasks.count {it.id==task.id})
        assertEquals(note.id,linked.tasks.single {it.id==task.id}.linkedNoteId)
        val detached=backup.saveNote(linked,NeoSmartStore(context).snapshot().toString(),note,"#00A4B8",false,detachedTaskIds=setOf(task.id))
        assertEquals(task,detached.tasks.single {it.id==task.id})
        assertEquals(note,detached.dayItems.single {it.id==note.id})
    }

    @Test
    fun linkedChecklistTasksInheritOnlyTheirSourceNotesPrivacy() {
        val note=DayItemV11("private-source", "2026-11-14", type=DayItemTypeV11.NOTE,title="Secret",noteLocked=true)
        val linked=TaskV11("linked", "Secret checklist",date=note.date,linkedNoteId=note.id)
        val unrelated=TaskV11("ordinary", "Ordinary",date=note.date)
        val db=V11Database(dayItems=listOf(note),tasks=listOf(linked,unrelated))
        assertTrue(linked.linkedNotePrivateV11(db))
        assertFalse(unrelated.linkedNotePrivateV11(db))
        assertTrue(ModernOperations.search(db,"Secret",false).isEmpty())
        assertFalse(linked.linkedNotePrivateV11(db.copy(dayItems=listOf(note.copy(noteLocked=false)))))
        assertTrue(linked.linkedNotePrivateV11(db.copy(dayItems=emptyList())))
    }

    @Test
    fun richNoteMetadataAndChecklistTaskCommitTogetherWithoutRetryDuplication() {
        val c = RuntimeEnvironment.getApplication()
        val before = V11Runtime(c).start()
        val note = DayItemV11("rich-note", "2026-11-14", type=DayItemTypeV11.NOTE, title="", details="**عربي** and *English*\n- [ ] Checklist", notePinned=true, noteLocked=true, noteHidden=true, noteTags="work,عمل", noteBackground="#AABBCC")
        val task = TaskV11("linked-checklist-task", "Checklist", date=note.date, linkedNoteId=note.id)
        val store=Neo4LocalBackup(c)
        val first=store.saveNote(before, NeoSmartStore(c).snapshot().toString(), note, "#00A4B8", false, linkedTasks=listOf(task))
        assertEquals(note, V11Runtime(c).start().dayItems.first {it.id==note.id})
        assertEquals(task, first.tasks.first {it.id==task.id})
        val persisted=c.getSharedPreferences("todo_shift_v11",android.content.Context.MODE_PRIVATE).getString("database",null)!!
        assertTrue(persisted.contains("sealedContentV45"))
        assertFalse(persisted.contains("**عربي**"))
        assertFalse(persisted.contains("Checklist"))
        val second=store.saveNote(first, NeoSmartStore(c).snapshot().toString(), note, "#00A4B8", false, linkedTasks=listOf(task))
        assertEquals(1, second.tasks.count {it.id==task.id})
        assertEquals(first, second)
    }

    @Test
    fun taskHistoryPreservesOriginalCompletionWhenReopenedAndCompletedAgain() {
        val original=TaskV11("history", "Task", date="2026-11-14", status=TaskStatusV11.COMPLETED,completedAt=100L)
        val reopened=original.withCompletionV11(false,200L)
        val again=reopened.withCompletionV11(true,300L)
        assertEquals(listOf(TaskCompletionV11(100L,true),TaskCompletionV11(200L,false),TaskCompletionV11(300L,true)),again.completionHistory)
        assertEquals(300L,again.completedAt)
        assertEquals(again,again.withCompletionV11(true,400L))
    }

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
        for (boundary in listOf("journal", "effects", "text", "style")) {
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
