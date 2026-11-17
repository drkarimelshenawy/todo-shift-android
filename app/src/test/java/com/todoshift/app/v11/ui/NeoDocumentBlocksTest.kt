package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

class NeoDocumentBlocksTest {
    @Test fun tableRoundTripSupportsArabicTextAndCellUpdates() {
        val table=NeoDocumentBlocks.table(3,3)
        val source="ملاحظة قديمة\n"+table+"\nمراجعة"
        val blocks=NeoDocumentBlocks.parse(source)
        val data=blocks.filterIsInstance<NeoDocumentBlocks.Block.Table>().single()
        assertEquals(3,data.cells.size)
        assertEquals(3,data.cells[0].size)
        val edited=data.cells.mapIndexed {row,cells ->
            if(row==2) cells.mapIndexed {column,text ->
                if(column==1) "المستحقات" else text
            } else cells
        }
        val next=NeoDocumentBlocks.replace(source,data,NeoDocumentBlocks.serializeTable(edited))
        assertTrue(next.startsWith("ملاحظة قديمة\n"))
        assertTrue(next.endsWith("\nمراجعة"))
        assertEquals("المستحقات",NeoDocumentBlocks.parse(next)
            .filterIsInstance<NeoDocumentBlocks.Block.Table>().single().cells[2][1])
    }

    @Test fun imageAndPageBreakKeepNoteOrderAndCannotUsePathTraversal() {
        val name="neo-attachment-01234567-89ab-cdef-0123-456789abcdef.jpg"
        val code=NeoDocumentBlocks.image(name,60,"right")
        val source="Before\n"+code+"\n"+NeoDocumentBlocks.PAGE_BREAK+"\nAfter"
        val blocks=NeoDocumentBlocks.parse(source)
        assertEquals(4,blocks.size)
        val image=blocks[1] as NeoDocumentBlocks.Block.Image
        assertEquals(60,image.width)
        assertEquals("right",image.align)
        assertTrue(blocks[2] is NeoDocumentBlocks.Block.PageBreak)
        try {NeoDocumentBlocks.image("../secret.png");fail("unsafe path")} catch(_:IllegalArgumentException) {}
        assertFalse(NeoDocumentBlocks.safePreview(source).contains(name))
    }

    @Test fun insertCreatesNewBlockWithoutDestroyingExistingParagraph() {
        val original="# Heading\nHello world\nLast line"
        val updated=NeoDocumentBlocks.insert(original,13,NeoDocumentBlocks.table(2,2))
        assertTrue(updated.startsWith("# Heading\nHello world\n|"))
        assertTrue(updated.endsWith("\nLast line"))
        assertTrue(NeoDocumentBlocks.parse(updated).any{it is NeoDocumentBlocks.Block.Table})
    }

    @Test fun corruptTablesRemainPlainTextAndOversizedCellsAreRejected() {
        val malformed="| a | b |\n| -- | -- |\n| c | d |"
        assertTrue(NeoDocumentBlocks.parse(malformed)
            .none{it is NeoDocumentBlocks.Block.Table})
        val large=List(2){listOf("x".repeat(251),"okay")}
        try {NeoDocumentBlocks.serializeTable(large);fail("Unbounded cell")} catch(_:IllegalArgumentException) {}
    }

    @Test fun staleBlockReplacementMustFailRatherThanOverwriteOtherText() {
        val original="Hello\n"+NeoDocumentBlocks.table(2,2)
        val block=NeoDocumentBlocks.parse(original)
            .filterIsInstance<NeoDocumentBlocks.Block.Table>().single()
        try {
            NeoDocumentBlocks.replace("Something else\n"+NeoDocumentBlocks.table(2,2),block,"bad")
            fail("Stale edit accepted")
        } catch(_:IllegalArgumentException) {}
    }
}
