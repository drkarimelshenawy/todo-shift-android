package com.exmore.app.v11.ui

/**
 * Safe, offline, text-only document exports. Attachments remain private unless
 * separately shared; no automatic disclosure of linked tasks or file URIs.
 */
internal object NeoNoteExporter {
    private fun escape(s:String)=buildString {
        for(ch in s) append(when(ch) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            else -> ch.toString()
        })
    }

    private val permitted=Regex(
        """ ?<!--task:[A-Za-z0-9_-]+-->|</?(?:b|i|u|s)>|<font color="#[0-9A-Fa-f]{6}">|</font>|<size sp="(?:14|16|18|20|24|28)">|</size>|<face key="(?:amiri|tajawal|reem|montserrat|lora|oswald|sans|serif|mono)">|</face>|<!--neo-p:(?:auto|left|center|right|justify):[0-4]-->|<mark color="#[0-9A-Fa-f]{6}">|</mark>|<sup>|</sup>|<sub>|</sub>"""
    )

    private fun safeInline(source:String):String {
        val out=StringBuilder()
        var cursor=0
        for(match in permitted.findAll(source)) {
            out.append(escape(source.substring(cursor,match.range.first)))
            val token=match.value
            when {
                token.contains("<!--task:") || token.startsWith("<!--neo-p:") -> Unit
                token.startsWith("<mark color=") -> {
                    val hex=token.substringAfter("color=\"").substringBefore('"')
                    out.append("<span style=\"background-color:").append(hex).append("\">")
                }
                token=="</mark>" -> out.append("</span>")
                token.startsWith("<size sp=") -> {
                    val size=Regex("""\d+""").find(token)?.value ?: "16"
                    out.append("<span style=\"font-size:").append(size).append("pt\">")
                }
                token=="</size>" -> out.append("</span>")
                token.startsWith("<face key=") -> {
                    val font=token.substringAfter("key=\"").substringBefore('"')
                    val family=when(font) {
                        "amiri"->"Amiri, serif"
                        "tajawal"->"Tajawal, sans-serif"
                        "reem"->"'Reem Kufi', sans-serif"
                        "montserrat"->"Montserrat, sans-serif"
                        "lora"->"Lora, serif"
                        "oswald"->"Oswald, sans-serif"
                        "serif"->"serif"
                        "mono"->"monospace"
                        else->"sans-serif"
                    }
                    out.append("<span style=\"font-family:").append(family).append("\">")
                }
                token=="</face>" -> out.append("</span>")
                else -> out.append(token)
            }
            cursor=match.range.last+1
        }
        out.append(escape(source.substring(cursor)))
        return out.toString()
    }

    fun plain(source:String):String = NeoNoteDocumentTools.visible(
        NeoDocumentBlocks.safePreview(source))

    fun html(title:String,source:String):String = buildString {
        append("<!doctype html><html lang=\"ar\"><head><meta charset=\"utf-8\">")
        append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        append("<title>").append(escape(title)).append("</title>")
        append("<style>body{font-family:system-ui,Arial,sans-serif;max-width:840px;")
        append("margin:2rem auto;padding:1rem;line-height:1.65;overflow-wrap:anywhere;}")
        append("p{margin:.4em 0;white-space:pre-wrap}h1,h2,h3{margin:1em 0 .3em;}")
        append("table{border-collapse:collapse;width:100%;margin:.8em 0;}") 
        append("th,td{border:1px solid #aaa;padding:.45em;text-align:start;}")
        append("@media print{body{max-width:none;margin:0;padding:0;}}</style></head><body dir=\"auto\">")
        append("<h1>").append(escape(title)).append("</h1>")
        NeoDocumentBlocks.parse(source).forEach {block ->
            when(block) {
                is NeoDocumentBlocks.Block.Paragraph -> {
                    block.source.split('\n').forEach {raw ->
                        val line=raw.replace(
                            Regex(" ?<!--task:[A-Za-z0-9_-]+-->"),"")
                        val heading=Regex("""^(#{1,3}) (.*)$""").matchEntire(line)
                        val paragraph=NeoRichParagraphStyle.fromSavedLine(raw)
                        val style=buildString {
                            if(paragraph.align!="auto") {
                                append("text-align:").append(paragraph.align).append(';')
                            }
                            if(paragraph.indent>0) {
                                append("padding-inline-start:").append(paragraph.indent*24)
                                    .append("px;")
                            }
                        }
                        if(heading!=null) {
                            val level=heading.groupValues[1].length
                            append("<h").append(level).append(" dir=\"auto\" style=\"")
                                .append(style).append("\">")
                            append(safeInline(heading.groupValues[2]))
                            append("</h").append(level).append(">")
                        } else {
                            append("<p dir=\"auto\" style=\"").append(style).append("\">")
                            if(line.isBlank())append("&nbsp;")
                            else append(safeInline(line))
                            append("</p>")
                        }
                    }
                }
                is NeoDocumentBlocks.Block.Table -> {
                    append("<table><thead><tr>")
                    block.cells.first().forEach{cell ->
                        append("<th>").append(escape(cell)).append("</th>")
                    }
                    append("</tr></thead><tbody>")
                    block.cells.drop(1).forEach{row ->
                        append("<tr>")
                        row.forEach{cell ->
                            append("<td>").append(escape(cell)).append("</td>")
                        }
                        append("</tr>")
                    }
                    append("</tbody></table>")
                }
                is NeoDocumentBlocks.Block.Image -> {
                    // Private attachment paths must never leak through a
                    // standalone HTML export. PDF includes authorized media.
                    append("<p>[Image — use PDF export to include media]</p>")
                }
                is NeoDocumentBlocks.Block.PageBreak ->
                    append("<div style=\"break-before:page\"></div>")
            }
        }
        append("</body></html>")
    }
}
