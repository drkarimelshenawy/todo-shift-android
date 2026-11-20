package com.todoshift.app.v11.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeoA4SoftWrapTest {
    @Test fun softWrappedArabicEnglishParagraphPreservesAllCharacters() {
        val source="عربي مختلط English 👨‍👩‍👧‍👦 عربي ".repeat(35)
        val starts=intArrayOf(0,32,64,96,128,160,192,224,256,288,320)
        val bottoms=FloatArray(starts.size){(it+1)*18f}
        val ranges=NeoA4SoftWrap.ranges(source.length,starts,bottoms,54f)
        assertTrue(ranges.size>1)
        assertEquals(0,ranges.first().start)
        assertEquals(source.length,ranges.last().end)
        assertEquals(source,ranges.joinToString(""){source.substring(it.start,it.end)})
        assertTrue(ranges.zipWithNext().all{it.first.end==it.second.start})
    }

    @Test fun shortAndEmptyTextNeverGainExtraPages() {
        assertEquals(listOf(NeoA4SoftWrap.Range(0,3)),
            NeoA4SoftWrap.ranges(3,intArrayOf(0),floatArrayOf(20f),50f))
        assertEquals(listOf(NeoA4SoftWrap.Range(0,0)),
            NeoA4SoftWrap.ranges(0,intArrayOf(),floatArrayOf(),50f))
    }

    @Test fun oversizedFirstLineAndRepeatedStartsDoNotCreateEmptyFragments() {
        val ranges=NeoA4SoftWrap.ranges(30,intArrayOf(0,0,10,20),
            floatArrayOf(10f,20f,30f,40f),15f)
        assertTrue(ranges.all{it.end>it.start})
        assertEquals(30,ranges.sumOf{it.end-it.start})
    }

    @Test fun viewportSlicesAreContiguousInSourceAndPhysicalParagraph() {
        val source="عربية English test!".repeat(6)
        val starts=intArrayOf(0,20,40,60,80)
        val tops=intArrayOf(0,18,36,54,72)
        val bottoms=floatArrayOf(18f,36f,54f,72f,90f)
        val slices=NeoA4SoftWrap.slices(source.length,starts,tops,bottoms,90,38)
        assertTrue(slices.size>1)
        assertEquals(0,slices.first().start)
        assertEquals(0,slices.first().topPx)
        assertEquals(source.length,slices.last().end)
        assertEquals(90,slices.last().bottomPx)
        assertEquals(source,slices.joinToString(""){source.substring(it.start,it.end)})
        assertTrue(slices.zipWithNext().all {
            it.first.end==it.second.start && it.first.bottomPx==it.second.topPx
        })
        assertTrue(slices.all{it.heightPx>0})
    }

    @Test fun pageSlicesNeverBecomePersistedParagraphText() {
        val source="<b>مرحبا بالعالم</b> and English without inserted breaks"
        val parsed=NeoDocumentBlocks.parse(source)
            .filterIsInstance<NeoDocumentBlocks.Block.Paragraph>().single()
        val page=parsed.copy(pageFragment=2,sliceTopPx=60,sliceBottomPx=80,
            wholeHeightPx=120,sliceStartOffset=10,sliceEndOffset=20)
        assertEquals(source,parsed.source)
        assertEquals(source,page.source)
        assertEquals(parsed.sourceHash,page.sourceHash)
        assertEquals(source,NeoDocumentBlocks.parse(source)
            .filterIsInstance<NeoDocumentBlocks.Block.Paragraph>().single().source)
    }
}
