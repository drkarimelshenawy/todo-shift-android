package com.todoshift.app.widget

import android.graphics.*
import com.todoshift.app.v11.*

object Neo4WidgetPainter {
    fun render(
        segments: List<ShiftSegmentV11>,
        workplaceColors: Map<String, Int>,
        appearance: NeoAppearance,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val shaders =
            segments.map { segment ->
                val style =
                    (if (appearance.showShiftPaint) appearance.shiftPaints[segment.kind] else null)
                        ?: NeoShiftPaint()
                val first =
                    if (style.mode == NeoPaintMode.INHERIT)
                        workplaceColors[segment.workplaceId] ?: Color.rgb(139, 186, 203)
                    else color(style.first)
                val factor =
                    when (style.strength) {
                        NeoGradientStrength.SOFT -> 0.35f
                        NeoGradientStrength.MEDIUM -> 0.7f
                        NeoGradientStrength.STRONG -> 1f
                    }
                val second =
                    if (style.mode == NeoPaintMode.GRADIENT)
                        blend(first, color(style.second), factor)
                    else first
                val end =
                    when (style.direction) {
                        NeoGradientDirection.VERTICAL -> 0f to 64f
                        NeoGradientDirection.HORIZONTAL -> 64f to 0f
                        NeoGradientDirection.DIAGONAL -> 64f to 64f
                    }
                LinearGradient(0f, 0f, end.first, end.second, first, second, Shader.TileMode.CLAMP)
            }
        neo4PaintBands(segments.map { it.kind }).forEach { band ->
            val top = band.top * 64f
            val bottom = band.bottom * 64f
            if (band.owners.size == 1) {
                paint.shader = shaders[band.owners[0]]
                canvas.drawRect(0f, top, 64f, bottom, paint)
            } else {
                val stripe = minOf(4f, (bottom - top) / band.owners.size).coerceAtLeast(0.001f)
                var y = top
                var i = 0
                while (y < bottom) {
                    paint.shader = shaders[band.owners[i % band.owners.size]]
                    canvas.drawRect(0f, y, 64f, minOf(y + stripe, bottom), paint)
                    y += stripe
                    i++
                }
            }
        }
        if (segments.isNotEmpty() && segments.all { it.kind == ShiftKind.OFF }) {
            paint.shader = null
            paint.color = Color.rgb(116, 129, 149)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawCircle(32f, 32f, 9f, paint)
            canvas.drawLine(25f, 39f, 39f, 25f, paint)
        }
        return bitmap
    }

    private fun color(hex: String) = runCatching { Color.parseColor(hex) }.getOrDefault(Color.GRAY)

    private fun blend(a: Int, b: Int, f: Float): Int =
        Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt(),
        )
}
