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

    fun calendarCell(segments: List<ShiftSegmentV11>, colors: Map<String,Int>, appearance: NeoAppearance,
        date: java.time.LocalDate, month: java.time.YearMonth, rtl: Boolean, accent: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val muted = java.time.YearMonth.from(date) != month
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = if(muted) 35 else 255 }
        canvas.drawBitmap(render(segments,colors,appearance),null,Rect(0,0,128,128),paint)
        paint.alpha = 255; paint.shader = null; paint.style = Paint.Style.STROKE
        paint.color = (accent and 0x00ffffff) or (35 shl 24); paint.strokeWidth=1f
        canvas.drawRect(.5f,.5f,127.5f,127.5f,paint)
        val e = NeoMonthGrid.edges(date,month,appearance.firstWeekday)
        paint.color=accent; paint.strokeWidth=3f
        if(e.top) canvas.drawLine(0f,1.5f,128f,1.5f,paint)
        if(e.bottom) canvas.drawLine(0f,126.5f,128f,126.5f,paint)
        if(if(rtl)e.end else e.start) canvas.drawLine(1.5f,0f,1.5f,128f,paint)
        if(if(rtl)e.start else e.end) canvas.drawLine(126.5f,0f,126.5f,128f,paint)
        if(date==java.time.LocalDate.now()) {
            paint.color=accent; paint.strokeWidth=3f
            canvas.drawRoundRect(5f,5f,123f,123f,8f,8f,paint)
        }
        return bitmap
    }

    fun monthGrid(db: V11Database, colors: Map<String,Int>, p: NeoAppearance, month: java.time.YearMonth,
        rtl: Boolean, accent: Int, width: Int = 560, height: Int = 600): Bitmap {
        val dates = NeoMonthGrid.dates(month,p.firstWeekday)
        val columns = 7; val rows = dates.size / 7
        val w = width.toFloat()/columns; val h = height.toFloat()/rows
        val image = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(image); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val indexed = NeoWidgetContentModel.indexedSegments(db)
        dates.forEachIndexed { index,date ->
            val logical = index%7; val col = if(rtl) 6-logical else logical
            val x = col*w; val y = (index/7)*h
            paint.alpha = if(java.time.YearMonth.from(date)==month) 255 else 35
            canvas.drawBitmap(render(indexed[date.toString()].orEmpty(),colors,p),null,RectF(x,y,x+w,y+h),paint)
        }
        paint.alpha=255;paint.style=Paint.Style.STROKE
        paint.color=(accent and 0xffffff) or (35 shl 24);paint.strokeWidth=1f
        for(col in 0..7) canvas.drawLine(col*w,0f,col*w,height.toFloat(),paint)
        for(row in 0..rows) canvas.drawLine(0f,row*h,width.toFloat(),row*h,paint)
        paint.color=accent;paint.strokeWidth=2f
        dates.forEachIndexed { index,date ->
            val col=if(rtl) 6-index%7 else index%7;val x=col*w;val y=(index/7)*h
            val edge=NeoMonthGrid.edges(date,month,p.firstWeekday)
            val left=x.coerceAtLeast(1f);val right=(x+w).coerceAtMost(width-1f)
            val top=y.coerceAtLeast(1f);val bottom=(y+h).coerceAtMost(height-1f)
            if(edge.top) canvas.drawLine(left,top,right,top,paint)
            if(edge.bottom) canvas.drawLine(left,bottom,right,bottom,paint)
            if(if(rtl) edge.end else edge.start) canvas.drawLine(left,top,left,bottom,paint)
            if(if(rtl) edge.start else edge.end) canvas.drawLine(right,top,right,bottom,paint)
            if(date==java.time.LocalDate.now()) canvas.drawRoundRect(x+4,y+4,x+w-4,y+h-4,5f,5f,paint)
        }
        return image
    }

    fun panel(background: Int, outline: Int, opacity: Float = .85f, width: Int = 256, height: Int = 256): Bitmap {
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color=if(background==Color.TRANSPARENT) Color.TRANSPARENT else (background and 0x00ffffff) or ((opacity.coerceIn(0f,1f)*255).toInt() shl 24)
        canvas.drawRoundRect(2f,2f,width-2f,height-2f,24f,24f,paint)
        paint.color=(outline and 0x00ffffff) or (130 shl 24);paint.style=Paint.Style.STROKE;paint.strokeWidth=2f
        canvas.drawRoundRect(2f,2f,width-2f,height-2f,24f,24f,paint)
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

