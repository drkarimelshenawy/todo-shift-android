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
import com.todoshift.app.v11.NeoAttachmentStore
import java.io.File
import java.io.OutputStream

/** Native Android PDF/A4 renderer. Never uploads note contents or media. */
internal object NeoDocumentPdf {
    private const val PAGE_WIDTH=595
    private const val PAGE_HEIGHT=842
    private const val MARGIN=42f
    private const val CONTENT_WIDTH=PAGE_WIDTH-2*MARGIN

    /** Returns actual PDF page count after a fully written export. */
    fun write(
        context:Context,
        noteId:String,
        title:String,
        source:String,
        output:OutputStream
    ):Int {
        require(source.length<=300_000) {"Document is too long for PDF export"}
        val pdf=PdfDocument()
        val paint=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color=android.graphics.Color.BLACK;textSize=12f
            typeface=android.graphics.Typeface.create("sans-serif",0)
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
                    PAGE_HEIGHT-MARGIN/2f,footer)
                pdf.finishPage(existing)
            }
            pages++
            page=pdf.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH,PAGE_HEIGHT,pages).create())
            y=MARGIN
        }
        fun ensure(height:Float) {
            if(page==null)newPage()
            if(y+height>PAGE_HEIGHT-MARGIN && y>MARGIN)newPage()
        }
        fun layout(text:String,width:Int=CONTENT_WIDTH.toInt()):StaticLayout {
            val value=text.ifEmpty{" "}
            return StaticLayout.Builder.obtain(value,0,value.length,paint,width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .setIncludePad(false).build()
        }
        fun printText(sourceText:String,size:Float=12f,space:Float=4f) {
            paint.textSize=size
            // Split at actual shaped layout line boundaries, so long text
            // can paginate without clipping a whole multi-line paragraph.
            val text=sourceText.ifBlank{" "}
            val wrapped=layout(text)
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
                canvas.translate(MARGIN,y)
                canvas.clipRect(0f,0f,CONTENT_WIDTH,height)
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
                printText(title,18f,14f)
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
                                else ->12f
                            }
                            printText(NeoNoteDocumentTools.visible(line),size,
                                if(line.isBlank()) 5f else 3f)
                        }
                    }
                    // A terminal page-break marker should not produce an empty
                    // trailing sheet in the exported PDF.
                    is NeoDocumentBlocks.Block.PageBreak ->
                        if(index<documentBlocks.lastIndex) newPage()
                    is NeoDocumentBlocks.Block.Table -> {
                        val columns=block.cells.first().size
                        val width=CONTENT_WIDTH/columns
                        val textWidth=(width-12f).toInt().coerceAtLeast(14)
                        fun drawTableRow(row:List<String>,header:Boolean) {
                            paint.textSize=if(header) 11f else 10f
                            val layouts=row.map{layout(it,textWidth)}
                            val height=(layouts.maxOf{it.height}+12f).coerceAtLeast(25f)
                            require(height<=PAGE_HEIGHT-2*MARGIN) {
                                "Table cell is too tall for A4; split the cell"
                            }
                            ensure(height)
                            row.forEachIndexed {column,_ ->
                                val x=MARGIN+column*width
                                val canvas=page!!.canvas
                                canvas.drawRect(x,y,x+width,y+height,borderPaint)
                                canvas.save()
                                canvas.translate(x+6f,y+6f)
                                layouts[column].draw(canvas)
                                canvas.restore()
                            }
                            y+=height
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
                    PAGE_HEIGHT-MARGIN/2f,footer)
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
