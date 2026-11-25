package com.exmore.app.v11.ui

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import com.exmore.app.v11.NeoSmartStore

/**
 * Decoration only, never inserted into the note body or mixed with rich text spans.
 * A4 content starts 42pt from the paper edge; these frames stay at 20pt,
 * leaving at least 22pt of breathing room around title and body.
 */
internal object NeoPageBorders {
    const val NONE = "none"
    const val SINGLE = "single"
    const val DOUBLE = "double"
    const val DASHED = "dashed"
    const val DOTTED = "dotted"
    val styles = listOf(NONE, SINGLE, DOUBLE, DASHED, DOTTED)
    private const val INSET_PT = 20f

    fun valid(value: String): Boolean = value in styles
    fun normalize(value: String?): String = value?.takeIf(::valid) ?: NONE

    fun load(store: NeoSmartStore, noteId: String): String =
        normalize(store.snapshot().optJSONObject("notePageBorders")?.optString(noteId))

    fun draw(canvas: Canvas, width: Float, height: Float, style: String) {
        if (style == NONE || !valid(style) || width <= 0 || height <= 0) return
        val scale = width / NeoA4PrintMetrics.PAGE_WIDTH_PT
        val inset = INSET_PT * scale
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(96, 99, 103)
            this.style = Paint.Style.STROKE
            strokeWidth = 1.1f * scale
            if (style == DASHED) pathEffect = DashPathEffect(floatArrayOf(7f * scale, 4f * scale), 0f)
            if (style == DOTTED) pathEffect = DashPathEffect(floatArrayOf(1f * scale, 3f * scale), 0f)
        }
        val rect = RectF(inset, inset, width - inset, height - inset)
        if (rect.width() <= 0 || rect.height() <= 0) return
        canvas.drawRect(rect, paint)
        if (style == DOUBLE) {
            val gap = 4f * scale
            paint.strokeWidth = .75f * scale
            canvas.drawRect(RectF(rect.left + gap, rect.top + gap,
                rect.right - gap, rect.bottom - gap), paint)
        }
    }

    /** Word page-border settings use the same printable inset and a safe gap. */
    fun wordBorderXml(style: String): String {
        val type = when(normalize(style)) {
            SINGLE -> "single"
            DOUBLE -> "double"
            DASHED -> "dashed"
            DOTTED -> "dotted"
            else -> return ""
        }
        return buildString {
            append("<w:pgBorders w:offsetFrom=\"page\">")
            for (side in listOf("top","left","bottom","right")) {
                append("<w:").append(side)
                    .append(" w:val=\"").append(type)
                    .append("\" w:sz=\"8\" w:color=\"606367\" w:space=\"20\"/>")
            }
            append("</w:pgBorders>")
        }
    }
}
