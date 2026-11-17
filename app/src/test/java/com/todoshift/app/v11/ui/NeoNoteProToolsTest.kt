package com.todoshift.app.v11.ui

import android.graphics.Bitmap
import com.todoshift.app.v11.NeoImageOptimizer
import com.todoshift.app.v11.NeoAttachmentStore
import com.todoshift.app.v11.NeoNoteHistoryStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoNoteProToolsTest {
    @Test fun unicodeWordsAndSearchWorkAcrossArabicEnglishCase() {
        val doc="الشيفت الصباحي\nWord WORD\n2026"
        val stats=NeoNoteDocumentTools.stats(doc)
        assertEquals(5,stats.words)
        assertEquals(doc.length,stats.characters)
        assertEquals(3,stats.lines)
        assertEquals(2,NeoNoteDocumentTools.find(doc,"word").size)
        assertEquals(1,NeoNoteDocumentTools.find(doc,"الشيفت").size)
        assertTrue(NeoNoteDocumentTools.find(doc,"  ").isEmpty())
    }

    @Test fun headingsMapToVisibleCursorDespiteHiddenMarkdownMarkers() {
        val src="# Overview\nParagraph\n## الشيفت\nMore\n### Final"
        val headings=NeoNoteDocumentTools.headings(src)
        assertEquals(3,headings.size)
        assertEquals(listOf(1,2,3),headings.map{it.level})
        assertEquals(listOf("Overview","الشيفت","Final"),headings.map{it.title})
        val displayed=NeoNoteDocumentTools.visible(src)
        headings.forEach{h ->assertTrue(h.start in displayed.indices)}
        assertTrue(displayed.substring(headings[1].start).startsWith("الشيفت"))
    }

    @Test fun exportedHtmlEscapesUserMarkupAndStripsTaskIds() {
        val source="# Hello\n<b>bold & text</b>\n☐ Item <!--task:task-123-->\n<script>alert(1)</script>"
        val html=NeoNoteExporter.html("<Secret>",source)
        assertTrue(html.contains("&lt;Secret&gt;"))
        assertTrue(html.contains("<h1"))
        assertTrue(html.contains("<b>bold &amp; text</b>"))
        assertTrue(html.contains("☐ Item"))
        assertFalse(html.contains("task-123"))
        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertFalse(NeoNoteExporter.plain(source).contains("task-123"))
    }

    @Test fun encryptedRevisionStoreKeepsMeaningfulCheckpointsAndRestoresFullText() {
        val context=RuntimeEnvironment.getApplication()
        val store=NeoNoteHistoryStore(context)
        val key="history-test-"+System.nanoTime()
        store.savePrior(key,"Title","<b>مسودة</b>",1000L)
        store.savePrior(key,"Title2","change",2000L) // Automatic save is throttled.
        assertEquals(1,store.revisions(key).size)
        store.savePrior(key,"Title2","change",2001L,force=true)
        assertEquals(2,store.revisions(key).size)
        assertEquals("change",NeoNoteHistoryStore(context).revisions(key).first().details)
        store.savePrior(key,"Title3","end",310_000L)
        assertEquals("end",store.revisions(key).first().details)
    }

    @Test fun largePngIsBoundedlyDownscaledInsteadOfImportFailure() {
        val bitmap=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888)
        val output=ByteArrayOutputStream()
        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output))
        bitmap.recycle()
        val small=output.toByteArray()
        val expanded=small+ByteArray(NeoAttachmentStore.MAX_FILE+64)
        val optimized=NeoImageOptimizer.optimize(expanded,"image/png")
        assertTrue(optimized.size<=NeoAttachmentStore.MAX_FILE)
        assertTrue(optimized.size<expanded.size)
    }
}
