package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoDocumentPdfTest {
    @Test fun exportCreatesValidA4PdfWithArabicTableAndManualPageBreak() {
        val source="## خطة العمل\nمراجعة المهام\n"+
            NeoDocumentBlocks.table(3,2)+"\n"+
            NeoDocumentBlocks.PAGE_BREAK+"\nOther page"
        val output=ByteArrayOutputStream()
        val pages=NeoDocumentPdf.write(RuntimeEnvironment.getApplication(),
            "pdf-note","مستند تجريبي",source,output)
        val bytes=output.toByteArray()
        assertTrue(pages>=2)
        assertTrue(bytes.size>800)
        assertTrue(String(bytes.copyOfRange(0,4),Charsets.US_ASCII)=="%PDF")
    }

    @Test fun standaloneHtmlKeepsEditableTableWithoutPrivateImageNames() {
        val name="neo-attachment-01234567-89ab-cdef-0123-456789abcdef.png"
        val src=NeoDocumentBlocks.table(2,2)+"\n"+
            NeoDocumentBlocks.image(name)+"\n"+NeoDocumentBlocks.PAGE_BREAK
        val html=NeoNoteExporter.html("Title",src)
        assertTrue(html.contains("<table>"))
        assertTrue(html.contains("<th>"))
        assertTrue(html.contains("break-before:page"))
        assertFalse(html.contains(name))
        assertTrue(NeoNoteExporter.plain(src).contains("[Image]"))
    }

    @Test fun longParagraphDoesNotCrashPaging() {
        val paragraph=(1..400).joinToString(" "){"word كلمة"}
        val out=ByteArrayOutputStream()
        val pages=NeoDocumentPdf.write(RuntimeEnvironment.getApplication(),
            "test-note","Long document",paragraph,out)
        assertTrue(pages>=1)
        assertTrue(out.size()>600)
    }
}
