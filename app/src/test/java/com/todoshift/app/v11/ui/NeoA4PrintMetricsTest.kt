package com.todoshift.app.v11.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The displayed page must have the same proportional type geometry as the
 * exported 595pt PDF sheet, including different phone widths / A4 zoom levels.
 */
class NeoA4PrintMetricsTest {
    @Test fun textProportionsMatchPdfAtPhoneTabletAndZoomWidths() {
        for(width in listOf(240f,320f,360f,510f,595f,800f))
            for(hostFontScale in listOf(1f,1.3f,2f)) {
                val size=NeoA4PrintMetrics.measuredSizeSp(
                    NeoA4PrintMetrics.BODY_PT,width,hostFontScale)
                val actualRatio=size*hostFontScale/width
                val pdfRatio=NeoA4PrintMetrics.BODY_PT/
                    NeoA4PrintMetrics.PAGE_WIDTH_PT
                assertEquals("A4 preview text size changed relative to the paper",
                    pdfRatio,actualRatio,0.00001f)
            }
    }

    @Test fun pageZoomChangesVisualSizeButNeverPrintedGeometry() {
        val normal=NeoA4PrintMetrics.sheetScale(320f)
        val zoomed=NeoA4PrintMetrics.sheetScale(320f*1.6f)
        assertEquals(1.6f,zoomed/normal,0.00001f)
        assertEquals(595,NeoA4PrintMetrics.PAGE_WIDTH_PT)
        assertEquals(842,NeoA4PrintMetrics.PAGE_HEIGHT_PT)
        assertEquals(42f,NeoA4PrintMetrics.MARGIN_PT,0.00001f)
        assertEquals(12f,NeoA4PrintMetrics.BODY_PT,0.00001f)
    }

    @Test fun invalidTextMetricsFailClosedInsteadOfProducingNaNOrInfinity() {
        val invalid=listOf(Float.NaN,Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,0f,-1f)
        for(value in invalid) {
            var rejectedPrint=false
            try { NeoA4PrintMetrics.measuredSizeSp(value,360f,1f) }
            catch(_:IllegalArgumentException) { rejectedPrint=true }
            assertTrue("Invalid print size must be rejected",rejectedPrint)
            var rejectedScale=false
            try { NeoA4PrintMetrics.measuredSizeSp(12f,360f,value) }
            catch(_:IllegalArgumentException) { rejectedScale=true }
            assertTrue("Invalid font scale must be rejected",rejectedScale)
        }
        var rejectedOverflow=false
        try { NeoA4PrintMetrics.measuredSizeSp(Float.MAX_VALUE,Float.MAX_VALUE,1f) }
        catch(_:IllegalArgumentException) { rejectedOverflow=true }
        assertTrue("Overflow must not produce infinite A4 text size",rejectedOverflow)
    }

    @Test fun subnormalWidthsFailClosedInsteadOfCollapsingA4Preview() {
        var rejected=false
        try { NeoA4PrintMetrics.sheetScale(Float.MIN_VALUE) }
        catch(_:IllegalArgumentException) { rejected=true }
        assertTrue("Subnormal sheet width must not collapse A4 scale to zero",rejected)
    }

    @Test fun invalidWidthsAndFontScaleCannotCreateBrokenPageGeometry() {
        var rejected=0
        listOf(-1f,0f,Float.NaN,Float.POSITIVE_INFINITY).forEach { width ->
            try {NeoA4PrintMetrics.sheetScale(width)}
            catch(_:IllegalArgumentException) {rejected++}
        }
        assertEquals(4,rejected)
        assertTrue(NeoA4PrintMetrics.measuredSizeSp(12f,360f,1f)>0)
    }
}
