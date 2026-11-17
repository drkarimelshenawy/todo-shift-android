package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

/**
 * Robolectric does not provide Android PdfDocument's native
 * nativeCreateDocument() implementation. Verify source and export semantics
 * here; PDF byte generation must be exercised by instrumentation on Android.
 */
class NeoDocumentPdfTest {
    @Test fun a4DocumentPlanRetainsArabicTablesAndManualPageBreaks() {
        val source="## خطة العمل\nمراجعة المهام\n"+
            NeoDocumentBlocks.table(3,2)+"\n"+
            NeoDocumentBlocks.PAGE_BREAK+"\nOther page"
        val blocks=NeoDocumentBlocks.parse(source)
        assertTrue(blocks.any{it is NeoDocumentBlocks.Block.Table})
        assertEquals(1,blocks.filterIsInstance<
            NeoDocumentBlocks.Block.PageBreak>().size)
        assertEquals("خطة العمل",NeoNoteDocumentTools.headings(source).first().title)
        assertTrue(NeoNoteExporter.html("A4",source).contains("break-before:page"))
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

    @Test fun longParagraphIsParsedWithoutLosingDocumentContent() {
        val paragraph=(1..400).joinToString(" "){"word كلمة"}
        val blocks=NeoDocumentBlocks.parse(paragraph)
        assertEquals(1,blocks.size)
        val text=(blocks.single() as NeoDocumentBlocks.Block.Paragraph).source
        assertEquals(paragraph,text)
        assertEquals(800,NeoNoteDocumentTools.stats(text).words)
    }
}
