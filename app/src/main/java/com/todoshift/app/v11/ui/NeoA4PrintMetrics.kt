package com.todoshift.app.v11.ui

/**
 * A4 geometry is expressed in the same print-point units used by PdfDocument.
 * A mobile A4 sheet is a scaled preview of 595 x 842 points: resizing the
 * sheet must scale type as well as paper, without changing source or PDF bytes.
 */
internal object NeoA4PrintMetrics {
    const val PAGE_WIDTH_PT=595
    const val PAGE_HEIGHT_PT=842
    const val MARGIN_PT=42f
    const val BODY_PT=12f
    const val TITLE_PT=18f
    const val BODY_LINE_PT=16f

    fun sheetScale(sheetWidthDp:Float):Float {
        require(sheetWidthDp>0f && sheetWidthDp.isFinite())
        return sheetWidthDp/PAGE_WIDTH_PT
    }

    /** TextMeasurer runs outside the preview's overridden LocalDensity. */
    fun measuredSizeSp(printPt:Float,sheetWidthDp:Float,hostFontScale:Float):Float {
        require(printPt>0f && printPt.isFinite() && hostFontScale>0f && hostFontScale.isFinite())
        val result=printPt*sheetScale(sheetWidthDp)/hostFontScale
        require(result>0f && result.isFinite())
        return result
    }
}
