package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4LocalBackup
import org.junit.Assert.*
import androidx.compose.ui.unit.sp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NoteTransactionTest {
    @Test fun lateSaveCleanupCannotDeleteANewerEncryptedDraft() {
        val context=RuntimeEnvironment.getApplication();val first=NeoNoteDraftStore(context);val reopened=NeoNoteDraftStore(context)
        first.save("shared-note",org.json.JSONObject().put("editVersion","older").put("details","Old edit"))
        reopened.save("shared-note",org.json.JSONObject().put("editVersion","newer").put("details","New text retained"))
        first.removeIfVersion("shared-note","older")
        assertEquals("New text retained",reopened.read("shared-note")!!.getString("details"))
        reopened.removeIfVersion("shared-note","newer")
        assertNull(first.read("shared-note"))
    }

    @Test fun unlinkingNoteFromCalendarPreservesItsTextIdentityAndReminder() {
        val note=DayItemV11("optional-date","2026-11-14",type=DayItemTypeV11.NOTE,title="Keep",details="Body",reminderAt=1234L)
        val unlinked=note.copy(noteCalendarLinked=false)
        val db=V11Database(dayItems=listOf(unlinked))
        assertFalse(neoVisibleDays(db,emptySet()).values.any {it.notes.any {n->n.id==note.id}})
        assertEquals(note.id,unlinked.id);assertEquals(note.details,unlinked.details);assertEquals(note.reminderAt,unlinked.reminderAt)
        assertEquals(note,unlinked.copy(noteCalendarLinked=true))
        val context=RuntimeEnvironment.getApplication();val base=V11Runtime(context).start()
        Neo4LocalBackup(context).saveNote(base,NeoSmartStore(context).snapshot().toString(),unlinked,"#00A4B8",false)
        assertEquals(unlinked,V11Runtime(context).start().dayItems.single {it.id==note.id})
    }

    @Test fun blockFormattingAppliesToTheCurrentParagraphAndPreservesOtherParagraphs() {
        val value=androidx.compose.ui.text.input.TextFieldValue("first\nعربي text\nlast",androidx.compose.ui.text.TextRange(9))
        val heading=com.todoshift.app.v11.ui.noteFormat(value,"## ")
        assertEquals("first\n## عربي text\nlast",heading.text)
        val list=com.todoshift.app.v11.ui.noteFormat(androidx.compose.ui.text.input.TextFieldValue("one\ntwo",androidx.compose.ui.text.TextRange(0,7)),"\n1. ")
        assertEquals("1. one\n2. two",list.text)
    }

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
    @Test fun readingModeDoesNotExposeNestedFormattingMarkup() {
        // This crosses closing-tag boundaries just like notes formatted on the phone.
        val stored = """نص <u>*<font color="#C62828">الملاحظة</font> <font color="#2E7D32">الجديدة</u>*</font> ختام"""
        val preview=com.todoshift.app.v11.ui.noteInline(stored)
        assertEquals("نص الملاحظة الجديدة ختام",preview.text)
        assertFalse(preview.text.contains("<font"))
        assertFalse(preview.text.contains("<u>"))
        assertFalse(preview.text.contains("*"))
        assertTrue(preview.spanStyles.any {it.item.textDecoration==androidx.compose.ui.text.style.TextDecoration.Underline})
        assertTrue(preview.spanStyles.any {it.item.color==androidx.compose.ui.graphics.Color(0xFFC62828)})
        assertTrue(preview.spanStyles.any {it.item.color==androidx.compose.ui.graphics.Color(0xFF2E7D32)})
        assertTrue(stored.contains("<font color=")) // read-only rendering must not mutate storage
    }

    @Test fun richNoteSelectionOffsetsStayWithinVisibleTextAfterFormatting() {
        val source = "## عنوان\n<u>عربي <font color=\"#C62828\">لون</font></u>\n**Bold**"
        val transformed = com.todoshift.app.v11.ui.NeoNoteVisualTransformation()
            .filter(androidx.compose.ui.text.AnnotatedString(source))
        val visible = transformed.text.text
        assertEquals("عنوان\nعربي لون\nBold",visible)
        val mapping = transformed.offsetMapping
        var previous = -1
        for(offset in 0..source.length) {
            val visibleOffset = mapping.originalToTransformed(offset)
            assertTrue(visibleOffset in 0..visible.length)
            assertTrue("Original-to-visual offsets must be monotonic",visibleOffset>=previous)
            previous=visibleOffset
        }
        for(offset in 0..visible.length) {
            val originalOffset=mapping.transformedToOriginal(offset)
            assertTrue(originalOffset in 0..source.length)
            assertEquals("Cursor must round-trip through hidden formatting marks",
                offset,mapping.originalToTransformed(originalOffset))
        }
        assertTrue(source.contains("<font"))
        assertTrue(transformed.text.spanStyles.any {it.item.color==androidx.compose.ui.graphics.Color(0xFFC62828)})
    }

    @Test fun previewPrivacyIsIndependentFromPasswordLockAndPersistsAcrossBackups() {
        val context=RuntimeEnvironment.getApplication()
        val storage=V11Storage(context)
        val note=DayItemV11(
            id="preview-only",date="2026-11-16",type=DayItemTypeV11.NOTE,
            title="Visible title",details="Private excerpt",
            notePreviewHidden=true,noteLocked=false,noteHidden=false
        )
        assertTrue(note.noteVisible)
        assertFalse(note.notePrivate)
        val payload=storage.exportJson(V11Database(dayItems=listOf(note)))
        val restored=storage.importJson(payload).dayItems.single()
        assertEquals(note,restored)
        assertTrue(restored.notePreviewHidden)
        assertTrue(restored.noteVisible)
        assertFalse(restored.noteLocked)
        val legacy=org.json.JSONObject(payload)
        legacy.getJSONArray("dayItems").getJSONObject(0).remove("notePreviewHidden")
        val old=storage.importJson(legacy.toString()).dayItems.single()
        assertFalse("Older records default to visible excerpts",old.notePreviewHidden)
        assertEquals("Private excerpt",old.details)
        val passwordLocked=note.copy(notePreviewHidden=false,noteLocked=true)
        assertTrue("Password-protected notes remain in the note list",passwordLocked.noteVisible)
        assertTrue(passwordLocked.notePrivate)
    }

    @Test fun noteFontSizeIsVisualNotRawMarkupAndLetteredListsWork() {
        val source=androidx.compose.ui.text.input.TextFieldValue(
            "عربي English",androidx.compose.ui.text.TextRange(0,12)
        )
        val formatted=com.todoshift.app.v11.ui.noteFormat(
            source,"<size sp=\"20\">","</size>"
        )
        val rendered=com.todoshift.app.v11.ui.NeoNoteVisualTransformation()
            .filter(androidx.compose.ui.text.AnnotatedString(formatted.text)).text
        assertEquals("عربي English",rendered.text)
        assertTrue(rendered.spanStyles.any {it.item.fontSize==20.sp})
        assertTrue(formatted.text.contains("<size sp=\"20\">"))
        val letters=com.todoshift.app.v11.ui.noteFormat(
            androidx.compose.ui.text.input.TextFieldValue(
                "one\ntwo\nthree",androidx.compose.ui.text.TextRange(0,13)
            ),
            "\nA. "
        )
        assertEquals("A. one\nB. two\nC. three",letters.text)
    }

}
