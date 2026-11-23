package com.todoshift.app.v11.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.text.style.StrikethroughSpan
import android.text.style.ForegroundColorSpan
import android.text.style.BackgroundColorSpan
import android.text.style.SuperscriptSpan
import android.text.style.SubscriptSpan
import android.text.style.RelativeSizeSpan
import android.text.style.AbsoluteSizeSpan
import com.todoshift.app.v11.NeoAttachmentStore
import com.todoshift.app.v11.NeoSmartStore
import java.io.File
import java.io.OutputStream

/** Native Android PDF/A4 renderer. Never uploads note contents or media. */
internal object NeoDocumentPdf {
    private const val PAGE_WIDTH=NeoA4PrintMetrics.PAGE_WIDTH_PT
    private const val PAGE_HEIGHT=NeoA4PrintMetrics.PAGE_HEIGHT_PT
    private const val MARGIN=NeoA4PrintMetrics.MARGIN_PT
    private const val CONTENT_WIDTH=PAGE_WIDTH-2*MARGIN

    /** Native Android glyph spans: preserve visible styling in the offline PDF export. */
    internal fun formattedLine(source:String):CharSequence {
        val rich=NeoRichEditorState.open(source)
        val text=SpannableStringBuilder(rich.value.text)
        val styles=rich.characters
        var index=0
        while(index<styles.size) {
            val style=styles[index]
            var end=index+1
            while(end<styles.size && styles[end]==style)end++
            fun mark(span:Any) {text.setSpan(span,index,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            val typeface=(if(style.bold) android.graphics.Typeface.BOLD else 0) or
                (if(style.italic) android.graphics.Typeface.ITALIC else 0)
            if(typeface!=0)mark(StyleSpan(typeface))
            if(style.underline)mark(UnderlineSpan())
            if(style.strike)mark(StrikethroughSpan())
            style.color?.let { hex -> runCatching {
                mark(ForegroundColorSpan(android.graphics.Color.parseColor(hex)))
            } }
            style.highlight?.let { hex -> runCatching {
                mark(BackgroundColorSpan(android.graphics.Color.parseColor(hex)))
            } }
            style.size?.let { mark(AbsoluteSizeSpan(it,true)) }
            when(style.script) {
                "sup" -> {mark(SuperscriptSpan());mark(RelativeSizeSpan(.75f))}
                "sub" -> {mark(SubscriptSpan());mark(RelativeSizeSpan(.75f))}
            }
            index=end
        }
        return text
    }

    /** Returns actual PDF page count after a fully written export. */
    fun write(
        context:Context,
        noteId:String,
        title:String,
        source:String,
        output:OutputStream,
        requestedPageBorderStyle:String?=null
    ):Int {
        require(source.length<=300_000) {"Document is too long for PDF export"}
        val pageBorderStyle=requestedPageBorderStyle?.let(NeoPageBorders::normalize)
            ?: NeoPageBorders.load(NeoSmartStore(context.applicationContext),noteId)
        val pdf=PdfDocument()
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color=android.graphics.Color.BLACK;textSize=NeoA4PrintMetrics.BODY_PT
            typeface=android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL)
        }
        val borderPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color=android.graphics.Color.LTGRAY
            strokeWidth=.7f;style=Paint.Style.STROKE
        }
        var page:PdfDocument.Page?=null
        var y=MARGIN
        var pages=0

        fun newPage() {
            page?.let {existing ->
                val footer=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color=android.graphics.Color.DKGRAY;textSize=9f
                }
                existing.canvas.drawText(pages.toString(),PAGE_WIDTH/2f,
                    if(pageBorderStyle==NeoPageBorders.NONE) PAGE_HEIGHT-MARGIN/2f else PAGE_HEIGHT-MARGIN+8f,footer)
                pdf.finishPage(existing)
            }
            pages++
            page=pdf.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,pages).create())
            NeoPageBorders.draw(page!!.canvas,PAGE_WIDTH.toFloat(),PAGE_HEIGHT.toFloat(),pageBorderStyle)
            y=MARGIN
        }
        fun ensure(height:Float) {
            if(page==null)newPage()
            if(y+height>PAGE_HEIGHT-MARGIN && y>MARGIN)newPage()
        }
        fun layout(text:CharSequence,width:Int=CONTENT_WIDTH.toInt(),
            alignment:String="auto",center:Boolean=false):StaticLayout {
            val value=if(text.isEmpty()) " " else text
            val resolved=if(center) "center" else alignment
            val builder=StaticLayout.Builder.obtain(value,0,value.length,paint,width)
                .setAlignment(when(resolved) {
                    "center" -> Layout.Alignment.ALIGN_CENTER
                    "right" -> Layout.Alignment.ALIGN_NORMAL
                    "left" -> Layout.Alignment.ALIGN_NORMAL
                    else -> Layout.Alignment.ALIGN_NORMAL
                })
                .setTextDirection(when(resolved) {
                    "left" -> TextDirectionHeuristics.LTR
                    "right" -> TextDirectionHeuristics.RTL
                    else -> TextDirectionHeuristics.FIRSTSTRONG_RTL
                })
                .setIncludePad(false)
            if(resolved=="justify")
                builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
            return builder.build()
        }
        fun printText(sourceText:CharSequence,size:Float=NeoA4PrintMetrics.BODY_PT,
            space:Float=4f,center:Boolean=false,
            paragraph:NeoRichParagraphStyle=NeoRichParagraphStyle()) {
            paint.textSize=size
            // Use Android's shaped StaticLayout with real foreground/background,
            // baseline, and size spans across all page breaks.
            val inset=paragraph.indent*20f
            val width=(CONTENT_WIDTH-inset).coerceAtLeast(100f).toInt()
            val wrapped=layout(sourceText,width,paragraph.align,center)
            for(index in 0 until wrapped.lineCount) {
                // Render each line from the ORIGINAL shaped layout. Rebuilding a
                // new layout from a substring can change Arabic shaping, bidi
                // direction and word wrapping at the PDF page boundary.
                val lineTop=wrapped.getLineTop(index)
                val lineBottom=wrapped.getLineBottom(index)
                val height=(lineBottom-lineTop).toFloat()+1f
                ensure(height)
                val canvas=page!!.canvas
                canvas.save()
                canvas.translate(MARGIN+if(paragraph.align=="right") 0f else inset,y)
                canvas.clipRect(0f,0f,width.toFloat(),height)
                canvas.translate(0f,-lineTop.toFloat())
                wrapped.draw(canvas)
                canvas.restore()
                y+=height
            }
            // Paragraph spacing must not push the cursor beyond the
            // printable page boundary. The next text block will start on
            // a fresh sheet rather than inheriting an overflowing baseline.
            y=(y+space).coerceAtMost(PAGE_HEIGHT-MARGIN)
        }
        try {
            newPage()
            if(title.isNotBlank()) {
                // Keep the printed title aligned with the centered A4 canvas title.
                printText(title,NeoA4PrintMetrics.TITLE_PT,3f,center=true)
            }
            val attachments=NeoAttachmentStore(context).list()
                .filter{it.owner==noteId && it.mime.startsWith("image/")}
                .map{it.name}.toSet()
            val documentBlocks=NeoDocumentBlocks.parse(source)
            documentBlocks.forEachIndexed {index,block ->
                when(block) {
                    is NeoDocumentBlocks.Block.Paragraph -> {
                        block.source.split('\n').forEach {line ->
                            val size=when {
                                line.startsWith("# ")->19f
                                line.startsWith("## ")->16f
                                line.startsWith("### ")->14f
                                else ->NeoA4PrintMetrics.BODY_PT
                            }
                            printText(formattedLine(line),size,
                                if(line.isBlank()) 5f else 3f,
                                paragraph=NeoRichParagraphStyle.fromSavedLine(line))
                        }
                    }
                    // A terminal page-break marker should not produce an empty
                    // trailing sheet in the exported PDF.
                    is NeoDocumentBlocks.Block.PageBreak ->
                        if(documentBlocks.drop(index+1).any { next ->
                            when(next) {
                                is NeoDocumentBlocks.Block.PageBreak -> false
                                is NeoDocumentBlocks.Block.Paragraph -> next.source.isNotBlank()
                                else -> true
                            }
                        }) {
                            // Avoid a blank sheet if a manual break follows
                            // an automatic page transition or another break.
                            // Keep the source marker intact for later editing.
                            if(y>MARGIN) newPage()
                        }
                    is NeoDocumentBlocks.Block.Table -> {
                        val columns=block.cells.first().size
                        val width=CONTENT_WIDTH/columns
                        val textWidth=(width-12f).toInt().coerceAtLeast(14)
                        fun drawTableRow(row:List<String>,header:Boolean) {
                            paint.textSize=if(header) 11f else 10f
                            val layouts=row.map{layout(it,textWidth)}
                            val height=(layouts.maxOf{it.height}+12f).coerceAtLeast(25f)
                            // A long table cell must not abort PDF export.
                            // Render the same shaped layouts in successive A4
                            // slices, preserving every line and the cell borders.
                            var offset=0f
                            while(offset<height) {
                                // Never cut a shaped text line halfway across
                                // pages: choose the last common safe baseline
                                // boundary that fits the remaining page.
                                if(PAGE_HEIGHT-MARGIN-y<25f) newPage()
                                val available=PAGE_HEIGHT-MARGIN-y
                                val remaining=height-offset
                                val slice=if(remaining<=available) remaining else {
                                    // Every cell must stop at a complete
                                    // shaped line. Taking the largest boundary
                                    // across all cells can slice a line in a
                                    // different column with taller glyphs.
                                    val boundary=layouts.mapNotNull { shaped ->
                                        val bottoms=(0 until shaped.lineCount).map { line ->
                                            shaped.getLineBottom(line).toFloat()+6f
                                        }
                                        if(bottoms.lastOrNull()?.let { it<=offset+1f }==true)
                                            null // This cell is already complete.
                                        else bottoms.filter {
                                            it>offset+1f && it-offset<=available-1f
                                        }.maxOrNull()
                                    }.minOrNull()
                                    // If no full shaped line fits, move to a
                                    // fresh sheet rather than clipping a glyph.
                                    if(boundary==null && y>MARGIN) {
                                        newPage()
                                        (height-offset).coerceAtMost(PAGE_HEIGHT-MARGIN-y)
                                    } else (boundary?.minus(offset) ?: available)
                                        .coerceAtMost(available)
                                }
                                row.forEachIndexed {column,_ ->
                                    val x=MARGIN+column*width
                                    val canvas=page!!.canvas
                                    canvas.drawRect(x,y,x+width,y+slice,borderPaint)
                                    canvas.save()
                                    canvas.clipRect(x+1f,y+1f,x+width-1f,y+slice-1f)
                                    canvas.translate(x+6f,y+6f-offset)
                                    layouts[column].draw(canvas)
                                    canvas.restore()
                                }
                                y+=slice
                                offset+=slice
                                if(offset<height) newPage()
                            }
                        }
                        // Calculate the header height before deciding whether
                        // a continued table can fit a header AND its next row.
                        paint.textSize=11f
                        val headerHeight=(block.cells.first().maxOf {
                            layout(it,textWidth).height
                        }+12f).coerceAtLeast(25f)
                        block.cells.forEachIndexed {rowIndex,row ->
                            if(rowIndex>0 && y>MARGIN) {
                                paint.textSize=10f
                                val rowHeight=(row.maxOf{layout(it,textWidth).height}+12f)
                                    .coerceAtLeast(25f)
                                if(y+rowHeight>PAGE_HEIGHT-MARGIN) {
                                    newPage()
                                    // Avoid an orphaned repeated header when a
                                    // very tall row cannot share the same sheet.
                                    if(MARGIN+headerHeight+rowHeight+4f<=PAGE_HEIGHT-MARGIN)
                                        drawTableRow(block.cells.first(),header=true)
                                }
                            }
                            drawTableRow(row,header=rowIndex==0)
                        }
                        y+=8f
                    }
                    is NeoDocumentBlocks.Block.Image -> {
                        if(block.name !in attachments ||
                            !NeoAttachmentStore.safeName(block.name)) {
                            printText("[Image unavailable]",10f)
                        } else {
                            val file=File(context.filesDir,"attachments/"+block.name)
                            val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                            BitmapFactory.decodeFile(file.absolutePath,bounds)
                            if(bounds.outWidth<=0||bounds.outHeight<=0) {
                                printText("[Image unavailable]",10f)
                            } else {
                                var sample=1
                                while(bounds.outWidth/sample>1400||
                                    bounds.outHeight/sample>1400)sample*=2
                                val image=BitmapFactory.decodeFile(file.absolutePath,
                                    BitmapFactory.Options().apply {inSampleSize=sample
                                        inPreferredConfig=Bitmap.Config.RGB_565})
                                if(image!=null) {
                                    try {
                                        // Preserve image aspect ratio when a tall attachment
                                        // must be scaled down to fit the printable A4 area.
                                        val requestedWidth=CONTENT_WIDTH*(block.width/100f)
                                        val naturalHeight=requestedWidth*image.height/image.width
                                        val maxHeight=(PAGE_HEIGHT-2*MARGIN)*.72f
                                        val scale=if(naturalHeight>maxHeight) maxHeight/naturalHeight else 1f
                                        val width=requestedWidth*scale
                                        val height=naturalHeight*scale
                                        ensure(height+8f)
                                        val x=when(block.align) {
                                            "left"->MARGIN
                                            "right"->PAGE_WIDTH-MARGIN-width
                                            else->(PAGE_WIDTH-width)/2f
                                        }
                                        page!!.canvas.drawBitmap(image,null,
                                            RectF(x,y,x+width,y+height),
                                            Paint(Paint.FILTER_BITMAP_FLAG))
                                        y+=height+9f
                                    } finally {image.recycle()}
                                } else printText("[Image unavailable]",10f)
                            }
                        }
                    }
                }
            }
            page?.let {p ->
                val footer=Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color=android.graphics.Color.DKGRAY;textSize=9f
                }
                p.canvas.drawText(pages.toString(),PAGE_WIDTH/2f,
                    if(pageBorderStyle==NeoPageBorders.NONE) PAGE_HEIGHT-MARGIN/2f else PAGE_HEIGHT-MARGIN+8f,footer)
                pdf.finishPage(p)
                page=null
            }
            pdf.writeTo(output)
            return pages
        } finally {
            pdf.close()
        }
    }
}
