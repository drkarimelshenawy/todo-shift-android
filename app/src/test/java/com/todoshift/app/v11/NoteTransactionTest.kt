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

    @Test fun nativeRichFormattingUsesSelectionAndPreservesNestedStylesAndPlainText() {
        val original=androidx.compose.ui.text.input.TextFieldValue("عربي English",androidx.compose.ui.text.TextRange(0,4))
        val under=com.todoshift.app.v11.ui.noteFormat(original,"<u>","</u>")
        val bold=com.todoshift.app.v11.ui.noteFormat(under,"**","**")
        val document=com.todoshift.app.v11.ui.NeoRichTextCodec.parse(bold.text)
        assertEquals("عربي English",document.text.toString())
        assertEquals(0,document.visible(bold.selection.start));assertEquals(4,document.visible(bold.selection.end))
        assertEquals(1,document.text.getSpans(0,4,android.text.style.UnderlineSpan::class.java).size)
        assertTrue(document.text.getSpans(0,4,android.text.style.StyleSpan::class.java).any {it.style==android.graphics.Typeface.BOLD})
        assertTrue(document.text.getSpans(5,12,android.text.style.UnderlineSpan::class.java).isEmpty())
    }

    @Test fun editingVisibleTextCannotDeleteHalfOfAFormattingTag() {
        val doc=com.todoshift.app.v11.ui.NeoRichTextCodec.parse("<u><s>abc</s></u>")
        doc.text.delete(2,3)
        val saved=com.todoshift.app.v11.ui.NeoRichTextCodec.encode(doc.text)
        val reloaded=com.todoshift.app.v11.ui.NeoRichTextCodec.parse(saved)
        assertEquals("ab",reloaded.text.toString())
        assertEquals(1,reloaded.text.getSpans(0,2,android.text.style.UnderlineSpan::class.java).size)
        assertEquals(1,reloaded.text.getSpans(0,2,android.text.style.StrikethroughSpan::class.java).size)
        assertFalse(reloaded.text.contains("<u>"))
    }

    @Test fun fontColorsSplitAtTheExactSelectionInsteadOfOverwritingTheWholeParagraph() {
        val codec=com.todoshift.app.v11.ui.NeoRichTextCodec
        val red="<font color=\"#C62828\">abc</font>"
        val parsed=codec.parse(red)
        val selected=androidx.compose.ui.text.input.TextFieldValue(red,androidx.compose.ui.text.TextRange(parsed.source(1),parsed.source(2)))
        val blue=com.todoshift.app.v11.ui.noteFormat(selected,"<font color=\"#1565C0\">","</font>")
        val doc=codec.parse(blue.text).text
        fun color(at:Int)=doc.getSpans(at,at+1,android.text.style.ForegroundColorSpan::class.java).first {doc.getSpanStart(it)<=at&&doc.getSpanEnd(it)>at}.foregroundColor
        assertEquals(android.graphics.Color.parseColor("#C62828"),color(0));assertEquals(android.graphics.Color.parseColor("#1565C0"),color(1));assertEquals(android.graphics.Color.parseColor("#C62828"),color(2))
    }

    @Test fun checkboxCompletionAndStableTaskTokenSurviveFormattingAndEditing() {
        val codec=com.todoshift.app.v11.ui.NeoRichTextCodec
        val doc=codec.parse("- [ ] <u>Work</u> <!--task:stable-123-->")
        val box=doc.text.getSpans(0,1,com.todoshift.app.v11.ui.NoteList::class.java).single();box.checked=true
        doc.text.append(" today")
        val saved=codec.encode(doc.text)
        assertTrue(saved.startsWith("- [x] "));assertEquals(1,Regex("<!--task:stable-123-->").findAll(saved).count())
        assertTrue(codec.parse(saved).text.toString().contains("Work today"))
    }

    @Test fun legacyNestedTagsLinksAndEscapedLiteralTextRoundTripWithoutLeaks() {
        val codec=com.todoshift.app.v11.ui.NeoRichTextCodec
        val raw="### <u>**Title**</u>\n<font color=\"#C62828\">red <font color=\"#1565C0\">blue</font> red</font>\n[Details](https://example.org)\n2 * 3 <literal> & text"
        val first=codec.parse(raw);val second=codec.parse(codec.encode(first.text))
        assertEquals(first.text.toString(),second.text.toString())
        assertFalse(second.text.toString().contains("<u>"))
        assertEquals("https://example.org",second.text.getSpans(0,second.text.length,android.text.style.URLSpan::class.java).single().url)
        assertEquals(3,second.text.getSpans(0,second.text.length,com.todoshift.app.v11.ui.NoteHeading::class.java).single().level)
        for(offset in 0..raw.length) assertTrue(first.visible(offset) in 0..first.text.length)
    }

    @Test fun emptyHeadingAndNestedFontColorArePreservedByTheNativeModel() {
        val codec=com.todoshift.app.v11.ui.NeoRichTextCodec
        val value=com.todoshift.app.v11.ui.noteFormat(androidx.compose.ui.text.input.TextFieldValue(""),"# ")
        val heading=codec.parse(value.text)
        heading.text.append("Title")
        assertTrue(codec.encode(heading.text).startsWith("# "))
        val nested=codec.parse("<font color=\"#C62828\">a<font color=\"#1565C0\">b</font>c</font>").text
        assertEquals(android.graphics.Color.parseColor("#1565C0"),nested.getSpans(1,2,android.text.style.ForegroundColorSpan::class.java).single {nested.getSpanStart(it)==1}.foregroundColor)
    }
}
