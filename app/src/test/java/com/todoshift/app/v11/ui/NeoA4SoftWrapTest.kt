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
}
