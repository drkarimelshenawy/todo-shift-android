package com.todoshift.app.v11.ui

import android.content.Context
import android.graphics.BitmapFactory
import com.todoshift.app.v11.NeoAttachmentStore
import com.todoshift.app.v11.NeoSmartStore
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Self-contained offline Office Open XML (.docx) export. Document remains
 * owned by user; no cloud converter or networking is involved.
 * Editable text, tables, page breaks and authorized inline images are emitted.
 */
internal object NeoDocumentDocx {
    private const val WORD="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    private const val REL="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val DRAW="http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val PIC="http://schemas.openxmlformats.org/drawingml/2006/picture"
    private const val WP="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    private const val PKG_REL="http://schemas.openxmlformats.org/package/2006/relationships"
    private const val PKG_CT="http://schemas.openxmlformats.org/package/2006/content-types"

    private fun escape(value:String):String=buildString {
        value.forEach {c->
            append(when(c) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> if(c.code>=0x20 || c=='\t' || c=='\n') c.toString() else " "
            })
        }
    }

    private fun paragraph(text:String,heading:Int=0):String {
        val rich=NeoRichEditorState.open(text)
        val chars=rich.value.text
        val styles=rich.characters
        val firstStrong=chars.firstOrNull {it.isLetter()}
        val rtl=firstStrong!=null &&
            Character.UnicodeScript.of(firstStrong.code)==Character.UnicodeScript.ARABIC
        val xml=StringBuilder("<w:p><w:pPr>")
        if(rtl)xml.append("<w:bidi w:val=\"1\"/>")
        val paragraph=NeoRichParagraphStyle.fromSavedLine(text)
        when(paragraph.align) {
            "left" -> xml.append("<w:jc w:val=\"left\"/>")
            "center" -> xml.append("<w:jc w:val=\"center\"/>")
            "right" -> xml.append("<w:jc w:val=\"right\"/>")
            "justify" -> xml.append("<w:jc w:val=\"both\"/>")
        }
        if(paragraph.indent>0) {
            xml.append(if(rtl) "<w:ind w:right=\"" else "<w:ind w:left=\"")
            xml.append(paragraph.indent*480).append("\"/>")
        }
        if(heading>0)xml.append("<w:spacing w:before=\"200\" w:after=\"100\"/>")
        xml.append("</w:pPr>")
        var index=0
        while(index<chars.length) {
            val style=styles[index]
            var end=index+1
            while(end<chars.length && styles[end]==style)end++
            xml.append("<w:r><w:rPr>")
            if(style.bold || heading>0)xml.append("<w:b/>")
            if(style.italic)xml.append("<w:i/>")
            if(style.underline)xml.append("<w:u w:val=\"single\"/>")
            if(style.strike)xml.append("<w:strike/>")
            val size=style.size ?: when(heading){1->38;2->32;3->27;else->24}
            // Word half-points: 24 means 12pt, 38 means 19pt
            xml.append("<w:sz w:val=\"").append(
                if(style.size!=null) size*2 else size
            ).append("\"/>")
            style.color?.takeIf{it.matches(Regex("#[0-9A-Fa-f]{6}"))}?.let{
                xml.append("<w:color w:val=\"").append(it.drop(1)).append("\"/>")
            }
            style.highlight?.takeIf{it.matches(Regex("#[0-9A-Fa-f]{6}"))}?.let{
                xml.append("<w:shd w:val=\"clear\" w:fill=\"")
                    .append(it.drop(1)).append("\"/>")
            }
            when(style.script) {
                "sup" -> xml.append("<w:vertAlign w:val=\"superscript\"/>")
                "sub" -> xml.append("<w:vertAlign w:val=\"subscript\"/>")
            }
            style.face?.let {font ->
                val face=when(font) {
                    "amiri"->"Amiri";"tajawal"->"Tajawal"
                    "reem"->"Reem Kufi";"montserrat"->"Montserrat"
                    "lora"->"Lora";"oswald"->"Oswald"
                    else->""
                }
                if(face.isNotBlank())xml.append("<w:rFonts w:ascii=\"")
                    .append(face).append("\" w:hAnsi=\"").append(face)
                    .append("\" w:cs=\"").append(face).append("\"/>")
            }
            xml.append("</w:rPr><w:t xml:space=\"preserve\">")
                .append(escape(chars.substring(index,end)))
                .append("</w:t></w:r>")
            index=end
        }
        if(chars.isEmpty())xml.append("<w:r><w:t> </w:t></w:r>")
        xml.append("</w:p>")
        return xml.toString()
    }

    private fun tableXml(rows:List<List<String>>):String=buildString {
        append("<w:tbl><w:tblPr><w:tblW w:w=\"0\" w:type=\"auto\"/>")
        append("<w:tblBorders>")
        listOf("top","bottom","left","right","insideH","insideV").forEach{edge ->
            append("<w:").append(edge)
                .append(" w:val=\"single\" w:sz=\"6\" w:color=\"B4B4B4\"/>")
        }
        append("</w:tblBorders></w:tblPr><w:tblGrid>")
        val cellWidth=(9638/rows.first().size).coerceAtLeast(1200)
        repeat(rows.first().size) {
            append("<w:gridCol w:w=\"").append(cellWidth).append("\"/>")
        }
        append("</w:tblGrid>")
        rows.forEachIndexed{rowIndex,row ->
            append("<w:tr>")
            row.forEach{cell ->
                append("<w:tc><w:tcPr><w:tcW w:w=\"")
                    .append(cellWidth).append("\" w:type=\"dxa\"/></w:tcPr>")
                append(if(rowIndex==0) paragraph(cell,heading=3) else paragraph(cell))
                append("</w:tc>")
            }
            append("</w:tr>")
        }
        append("</w:tbl>")
    }

    private data class ImagePart(
        val name:String,val mime:String,val bytes:ByteArray,
        val width:Long,val height:Long,val relId:Int
    )
    private fun drawing(image:ImagePart,align:String):String=buildString {
        append("<w:p><w:pPr><w:jc w:val=\"")
        append(when(align){"left"->"left";"right"->"right";else->"center"})
        append("\"/></w:pPr><w:r><w:drawing>")
        append("<wp:inline xmlns:wp=\"").append(WP).append("\" distT=\"0\" distB=\"0\">")
        append("<wp:extent cx=\"").append(image.width).append("\" cy=\"")
            .append(image.height).append("\"/>")
        append("<wp:docPr id=\"").append(image.relId).append("\" name=\"Image")
            .append(image.relId).append("\"/>")
        append("<a:graphic xmlns:a=\"").append(DRAW).append("\"><a:graphicData uri=\"")
            .append(PIC).append("\">")
        append("<pic:pic xmlns:pic=\"").append(PIC).append("\">")
        append("<pic:nvPicPr><pic:cNvPr id=\"").append(image.relId)
            .append("\" name=\"Picture\"/><pic:cNvPicPr/></pic:nvPicPr>")
        append("<pic:blipFill><a:blip xmlns:r=\"").append(REL)
            .append("\" r:embed=\"rId").append(image.relId).append("\"/>")
        append("<a:stretch><a:fillRect/></a:stretch></pic:blipFill>")
        append("<pic:spPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"")
            .append(image.width).append("\" cy=\"").append(image.height)
            .append("\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/>")
        append("</a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>")
        append("</wp:inline></w:drawing></w:r></w:p>")
    }

    fun write(
        context:Context,noteId:String,title:String,source:String,output:OutputStream,
        requestedPageBorderStyle:String?=null
    ) {
        require(source.length<=300_000)
        val pageBorderStyle=requestedPageBorderStyle?.let(NeoPageBorders::normalize)
            ?: NeoPageBorders.load(NeoSmartStore(context.applicationContext),noteId)
        val permitted=NeoAttachmentStore(context.applicationContext).list()
            .filter{it.owner==noteId && it.mime in setOf("image/jpeg","image/png")}
            .associateBy{it.name}
        val images=mutableListOf<ImagePart>()
        val document=StringBuilder()
        document.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        document.append("<w:document xmlns:w=\"").append(WORD)
            .append("\" xmlns:r=\"").append(REL).append("\"><w:body>")
        if(title.isNotBlank())document.append(paragraph(title,1))
        NeoDocumentBlocks.parse(source).forEach {block ->
            when(block) {
                is NeoDocumentBlocks.Block.Paragraph -> {
                    block.source.split('\n').forEach {line ->
                        val heading=when {
                            line.startsWith("### ")->3
                            line.startsWith("## ")->2
                            line.startsWith("# ")->1
                            else->0
                        }
                        document.append(paragraph(
                            if(heading>0)line.substring(heading+1) else line,heading))
                    }
                }
                is NeoDocumentBlocks.Block.Table -> document.append(tableXml(block.cells))
                is NeoDocumentBlocks.Block.PageBreak ->
                    document.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                is NeoDocumentBlocks.Block.Image -> {
                    val file=permitted[block.name]
                    if(file==null || !NeoAttachmentStore.safeName(block.name)) {
                        document.append(paragraph("[Image unavailable]"))
                    } else {
                        val raw=File(context.filesDir,"attachments/"+file.name)
                        if(!raw.isFile || raw.length()<=0 ||
                            raw.length()>NeoAttachmentStore.MAX_FILE) {
                            document.append(paragraph("[Image unavailable]"))
                        } else {
                            val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                            BitmapFactory.decodeFile(raw.absolutePath,bounds)
                            if(bounds.outWidth<=0 || bounds.outHeight<=0) {
                                document.append(paragraph("[Image unavailable]"))
                            } else {
                                val width=6_120_000L*block.width/100
                                val height=(width*bounds.outHeight/bounds.outWidth)
                                    .coerceAtMost(8_000_000L)
                                val image=ImagePart(
                                    "image"+(images.size+1)+
                                        if(file.mime=="image/png")".png" else ".jpg",
                                    file.mime,raw.readBytes(),width,height,images.size+2)
                                images+=image
                                document.append(drawing(image,block.align))
                            }
                        }
                    }
                }
            }
        }
        document.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/>")
        document.append("<w:pgMar w:top=\"1134\" w:right=\"1134\"")
        document.append(" w:bottom=\"1134\" w:left=\"1134\"/>")
        document.append(NeoPageBorders.wordBorderXml(pageBorderStyle))
        document.append("</w:sectPr>")
        document.append("</w:body></w:document>")

        val ct=StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        ct.append("<Types xmlns=\"").append(PKG_CT).append("\">")
        ct.append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        ct.append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        if(images.any{it.mime=="image/png"})
            ct.append("<Default Extension=\"png\" ContentType=\"image/png\"/>")
        if(images.any{it.mime=="image/jpeg"})
            ct.append("<Default Extension=\"jpg\" ContentType=\"image/jpeg\"/>")
        ct.append("<Override PartName=\"/word/document.xml\" ContentType=\"")
        ct.append("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>")
        ct.append("</Types>")

        val rootRels="<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
            "<Relationships xmlns=\""+PKG_REL+"\">"+
            "<Relationship Id=\"rId1\" Type=\""+
            REL+"/officeDocument\" Target=\"word/document.xml\"/>"+
            "</Relationships>"
        val docRels=StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<Relationships xmlns=\"").append(PKG_REL).append("\">")
        images.forEach{image ->
            docRels.append("<Relationship Id=\"rId").append(image.relId)
                .append("\" Type=\"").append(REL)
                .append("/image\" Target=\"media/").append(image.name)
                .append("\"/>")
        }
        docRels.append("</Relationships>")

        ZipOutputStream(output).use{zip ->
            fun put(name:String,bytes:ByteArray) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes);zip.closeEntry()
            }
            fun textEntry(name:String,xml:String) =
                put(name,xml.toByteArray(Charsets.UTF_8))
            textEntry("[Content_Types].xml",ct.toString())
            textEntry("_rels/.rels",rootRels)
            textEntry("word/document.xml",document.toString())
            textEntry("word/_rels/document.xml.rels",docRels.toString())
            images.forEach{image ->put("word/media/"+image.name,image.bytes)}
            zip.finish()
        }
    }
}
