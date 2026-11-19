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
    @Test fun trailingBreaksDoNotConsumeOrAlterEarlierContent() {
        val original="عنوان عربي\nEnglish body\n"+NeoDocumentBlocks.PAGE_BREAK+
            "\n"+NeoDocumentBlocks.PAGE_BREAK
        val blocks=NeoDocumentBlocks.parse(original)
        assertEquals(2,blocks.filterIsInstance<NeoDocumentBlocks.Block.PageBreak>().size)
        assertEquals("عنوان عربي\nEnglish body",
            (blocks.first() as NeoDocumentBlocks.Block.Paragraph).source)
        assertEquals(original,blocks.joinToString("\n") { block ->
            when(block) {
                is NeoDocumentBlocks.Block.Paragraph -> block.source
                is NeoDocumentBlocks.Block.PageBreak -> NeoDocumentBlocks.PAGE_BREAK
                else -> error("Unexpected block")
            }
        })
    }

    @Test fun longMultilingualTableCellsRoundTripWithoutTruncation() {
        val longCell=(1..16).joinToString(" ") { "عربي English" }
        assertTrue(longCell.length in 150..250)
        val source=NeoDocumentBlocks.serializeTable(listOf(
            listOf("الوصف", "Description"),
            listOf(longCell, "Value"),
            listOf("آخر صف", "Final row")
        ))
        val table=NeoDocumentBlocks.parse(source).single()
            as NeoDocumentBlocks.Block.Table
        assertEquals(longCell,table.cells[1][0])
        assertEquals(source,NeoDocumentBlocks.serializeTable(table.cells))
    }

    @Test fun insertingAtDocumentEndDoesNotCreateUnwantedBlankParagraph() {
        val original="عنوان\nBody"
        val updated=NeoDocumentBlocks.insert(original,original.length,
            NeoDocumentBlocks.PAGE_BREAK)
        assertEquals("عنوان\nBody\n"+NeoDocumentBlocks.PAGE_BREAK,updated)
        assertEquals(1,NeoDocumentBlocks.parse(updated)
            .filterIsInstance<NeoDocumentBlocks.Block.PageBreak>().size)
    }

    @Test fun insertingBetweenLinesKeepsExistingTextAndNewline() {
        val original="First\nSecond"
        val updated=NeoDocumentBlocks.insert(original,2,NeoDocumentBlocks.PAGE_BREAK)
        assertEquals("First\n"+NeoDocumentBlocks.PAGE_BREAK+"\nSecond",updated)
    }

}
