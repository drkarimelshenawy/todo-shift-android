package com.todoshift.app.v11.ui

import com.todoshift.app.v11.NeoAttachmentStore

/**
 * Portable v1 document blocks. Blocks are embedded in existing note text:
 * old notes, drafts, history, and scoped backups continue to work.
 * Tables are Markdown-compatible, editable in document mode.
 * Image handles point only to this note's private attachment store.
 */
internal object NeoDocumentBlocks {
    sealed interface Block {
        val startLine:Int
        val endLine:Int
        /** Exact source version used to guard against overwriting concurrent edits. */
        val sourceHash:String
        data class Paragraph(
            val source:String,override val startLine:Int,override val endLine:Int,
            override val sourceHash:String
        ):Block
        data class Table(
            val cells:List<List<String>>,
            override val startLine:Int,override val endLine:Int,
            override val sourceHash:String
        ):Block
        data class Image(
            val name:String,val width:Int,val align:String,
            override val startLine:Int,override val endLine:Int,
            override val sourceHash:String
        ):Block
        data class PageBreak(
            override val startLine:Int,override val endLine:Int,
            override val sourceHash:String
        ):Block
    }

    private val imagePattern=Regex("""^\[\[neo-image:([A-Za-z0-9_.-]{1,110}):([0-9]{1,3}):(left|center|right)\]\]$""")
    const val PAGE_BREAK="[[neo-page-break]]"
    private val separator=Regex("""^\|\s*:?-{3,}:?\s*\|(?:\s*:?-{3,}:?\s*\|)*$""")
    private const val MAX_COLUMNS=8
    private const val MAX_ROWS=80

    fun image(name:String,width:Int=80,align:String="center"):String {
        require(NeoAttachmentStore.safeName(name))
        require(width in 25..100 && align in setOf("left","center","right"))
        return "[[neo-image:$name:$width:$align]]"
    }

    fun table(rows:Int=3,columns:Int=3):String {
        require(rows in 2..MAX_ROWS && columns in 1..MAX_COLUMNS)
        val values=List(rows) {row ->
            List(columns) {col -> if(row==0) "Column " +(col+1) else "" }
        }
        return serializeTable(values)
    }

    fun serializeTable(rows:List<List<String>>):String {
        require(rows.size in 2..MAX_ROWS && rows.first().size in 1..MAX_COLUMNS)
        val columns=rows.first().size
        require(rows.all{it.size==columns && it.all {cell ->
            cell.length<=250 && '\n' !in cell && '\r' !in cell && '|' !in cell
        }})
        return buildString {
            rows.forEachIndexed {index,row ->
                if(index==1) append("| ").append(List(columns){"---"}.joinToString(" | "))
                    .append(" |\n")
                append("| ").append(row.joinToString(" | ")).append(" |")
                if(index<rows.lastIndex)append('\n')
            }
        }
    }

    private fun parseCells(line:String):List<String>? {
        if(!line.startsWith("|")||!line.endsWith("|"))return null
        val cells=line.substring(1,line.length-1).split('|').map{it.trim()}
        return cells.takeIf{it.size in 1..MAX_COLUMNS && it.all{v->v.length<=250}}
    }

    /** A table starts only when its header, separator and first data row agree. */
    private fun isTableStart(lines:List<String>,index:Int):Boolean {
        if(index+2>=lines.size)return false
        val header=parseCells(lines[index]) ?: return false
        val firstRow=parseCells(lines[index+2]) ?: return false
        return firstRow.size==header.size && separator.matches(lines[index+1]) &&
            lines[index+1].count{it=='|'}==header.size+1
    }

    private fun digest(source:String):String =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest(source.toByteArray(Charsets.UTF_8))
            .joinToString("") {byte ->"%02x".format(byte)}

    fun parse(source:String):List<Block> {
        val documentHash=digest(source)
        val lines=source.split('\n')
        val blocks=mutableListOf<Block>()
        var cursor=0
        while(cursor<lines.size) {
            val img=imagePattern.matchEntire(lines[cursor])
            if(img!=null && NeoAttachmentStore.safeName(img.groupValues[1])) {
                val width=img.groupValues[2].toIntOrNull()
                if(width!=null && width in 25..100) {
                    blocks+=Block.Image(img.groupValues[1],width,img.groupValues[3],
                        cursor,cursor+1,documentHash)
                    cursor++;continue
                }
            }
            if(lines[cursor]==PAGE_BREAK) {
                blocks+=Block.PageBreak(cursor,cursor+1,documentHash)
                cursor++;continue
            }
            val header=parseCells(lines[cursor])
            if(header!=null && isTableStart(lines,cursor)) {
                val data=mutableListOf(header)
                var next=cursor+2
                while(next<lines.size && data.size<MAX_ROWS) {
                    val row=parseCells(lines[next]) ?: break
                    if(row.size!=header.size)break
                    data+=row;next++
                }
                if(data.size>=2) {
                    blocks+=Block.Table(data,cursor,next,documentHash)
                    cursor=next;continue
                }
            }
            val start=cursor
            cursor++
            while(cursor<lines.size) {
                val line=lines[cursor]
                if(line==PAGE_BREAK || imagePattern.matches(line))break
                if(isTableStart(lines,cursor))break
                cursor++
            }
            blocks+=Block.Paragraph(lines.subList(start,cursor).joinToString("\n"),
                start,cursor,documentHash)
        }
        return blocks
    }

    fun replace(source:String,block:Block,newSource:String):String {
        val rows=source.split('\n')
        require(block.startLine>=0 && block.endLine in 1..rows.size &&
            block.startLine<block.endLine)
        require(block.sourceHash==digest(source)) {
            "Document changed while editing the block"
        }
        require(parse(source).contains(block)) {"Document block was replaced"}
        return (rows.take(block.startLine)+newSource.split('\n')+
            rows.drop(block.endLine)).joinToString("\n")
    }

    /** Re-resolve the paragraph against the latest note source before saving.
     * This avoids stale editor callbacks overwriting an intervening media edit.
     */
    fun replaceParagraphAt(source:String,startLine:Int,paragraph:String):String {
        require(paragraph.length<=300_000)
        val current=parse(source).firstOrNull {
            it is Block.Paragraph && it.startLine==startLine
        } as? Block.Paragraph
            ?: throw IllegalArgumentException("Paragraph moved; reopen the editor")
        return replace(source,current,paragraph)
    }

    fun insert(source:String,caret:Int,block:String):String {
        require(caret in 0..source.length && block.length<=16_000)
        val lineEnd=source.indexOf('\n',caret).takeIf{it>=0} ?: source.length
        val before=source.substring(0,lineEnd)
        val after=source.substring(lineEnd)
        return before+(if(before.isNotEmpty()&&!before.endsWith('\n')) "\n" else "")+
            block+(if(after.isEmpty() || after.startsWith("\n")) "" else "\n")+after
    }

    fun safePreview(source:String):String=source.lines().joinToString("\n") {line->
        when {
            imagePattern.matches(line) -> "[Image]"
            line==PAGE_BREAK -> ""
            else -> line
        }
    }
}
