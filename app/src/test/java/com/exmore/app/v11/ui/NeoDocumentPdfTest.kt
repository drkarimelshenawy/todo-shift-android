package com.exmore.app.v11.ui

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

    @Test fun editingParagraphPreservesAdjacentTableAndManualPageBreak() {
        val table=NeoDocumentBlocks.table(3,2)
        val original="Before\n"+table+"\n"+NeoDocumentBlocks.PAGE_BREAK+"\nAfter"
        val updated=NeoDocumentBlocks.replaceParagraphAt(original,0,"Edited")
        assertEquals("Edited\n"+table+"\n"+NeoDocumentBlocks.PAGE_BREAK+"\nAfter",updated)
        assertEquals(1,NeoDocumentBlocks.parse(updated)
            .filterIsInstance<NeoDocumentBlocks.Block.Table>().size)
    }

    @Test fun replacingParagraphRejectsStaleDocumentVersion() {
        val original="First\n"+NeoDocumentBlocks.PAGE_BREAK+"\nLast"
        val stale=NeoDocumentBlocks.parse(original).first()
        val changed="Changed\n"+NeoDocumentBlocks.PAGE_BREAK+"\nLast"
        try {
            NeoDocumentBlocks.replace(changed,stale,"Accidental overwrite")
            fail("Stale document edits must not overwrite newer changes")
        } catch(expected:IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("Document changed"))
        }
    }

    @Test fun insertingIntoTrailingNewlineDoesNotAddExtraBlankLine() {
        val original="Body\n"
        val updated=NeoDocumentBlocks.insert(original,original.length,
            NeoDocumentBlocks.PAGE_BREAK)
        assertEquals("Body\n"+NeoDocumentBlocks.PAGE_BREAK,updated)
    }

    @Test fun malformedTableSyntaxStaysInParagraphWithoutLosingLines() {
        val source="Intro\n| A | B |\n| --- | --- |\n| only one |\nOutro"
        val blocks=NeoDocumentBlocks.parse(source)
        assertEquals(1,blocks.size)
        assertEquals(source,(blocks.single() as NeoDocumentBlocks.Block.Paragraph).source)
    }

    @Test fun validTableFollowingProseRemainsEditableSeparateBlock() {
        val table=NeoDocumentBlocks.serializeTable(listOf(
            listOf("الاسم","Name"),listOf("محمد","Mohamed")
        ))
        val blocks=NeoDocumentBlocks.parse("Intro\n"+table+"\nOutro")
        assertEquals(3,blocks.size)
        assertTrue(blocks[1] is NeoDocumentBlocks.Block.Table)
        assertEquals("Outro",(blocks[2] as NeoDocumentBlocks.Block.Paragraph).source)
    }

    @Test fun previewMasksOnlyValidImageBlocksNotMalformedOrInlineReferences() {
        val valid=NeoDocumentBlocks.image(
            "neo-attachment-01234567-89ab-cdef-0123-456789abcdef.png")
        val invalidWidth=valid.replace(":80:",":999:")
        val prose="Text "+valid
        val preview=NeoDocumentBlocks.safePreview(
            listOf(valid,invalidWidth,prose).joinToString("\n"))
        assertEquals(listOf("[Image]",invalidWidth,prose).joinToString("\n"),preview)
        assertEquals(1,NeoDocumentBlocks.parse(valid+"\n"+invalidWidth)
            .filterIsInstance<NeoDocumentBlocks.Block.Image>().size)
    }

    @Test fun malformedImageMarkerStaysInSameParagraphWithoutPhantomBlock() {
        val image=NeoDocumentBlocks.image(
            "neo-attachment-01234567-89ab-cdef-0123-456789abcdef.png")
        val invalid=image.replace(":80:",":999:")
        val source="قبل\n"+invalid+"\nبعد"
        val blocks=NeoDocumentBlocks.parse(source)
        assertEquals(1,blocks.size)
        assertEquals(source,(blocks.single() as NeoDocumentBlocks.Block.Paragraph).source)
        assertEquals(source,NeoDocumentBlocks.safePreview(source))
    }

    @Test fun editingMultilineArabicParagraphPreservesFollowingMediaAndPageBreak() {
        val image=NeoDocumentBlocks.image(
            "neo-attachment-01234567-89ab-cdef-0123-456789abcdef.png")
        val source="أول سطر\nثاني سطر\n"+image+"\n"+
            NeoDocumentBlocks.PAGE_BREAK+"\nالخاتمة"
        val changed=NeoDocumentBlocks.replaceParagraphAt(
            source,0,"أول سطر محرر\nسطر إضافي\nثاني سطر")
        assertEquals("أول سطر محرر\nسطر إضافي\nثاني سطر\n"+image+"\n"+
            NeoDocumentBlocks.PAGE_BREAK+"\nالخاتمة",changed)
        val blocks=NeoDocumentBlocks.parse(changed)
        assertEquals(1,blocks.filterIsInstance<NeoDocumentBlocks.Block.Image>().size)
        assertEquals(1,blocks.filterIsInstance<NeoDocumentBlocks.Block.PageBreak>().size)
        assertEquals("الخاتمة",(blocks.last() as NeoDocumentBlocks.Block.Paragraph).source)
    }

    @Test fun emptyParagraphEditDoesNotRemoveAdjacentPageBreak() {
        val source="مقدمة\n"+NeoDocumentBlocks.PAGE_BREAK+"\nنهاية"
        val changed=NeoDocumentBlocks.replaceParagraphAt(source,0,"")
        assertEquals("\n"+NeoDocumentBlocks.PAGE_BREAK+"\nنهاية",changed)
        assertEquals(1,NeoDocumentBlocks.parse(changed)
            .filterIsInstance<NeoDocumentBlocks.Block.PageBreak>().size)
    }

}
