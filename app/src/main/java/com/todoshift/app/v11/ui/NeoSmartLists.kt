package com.todoshift.app.v11.ui

import androidx.compose.ui.text.TextRange

/**
 * Structural list editing on visible text, independent of IME composition.
 *
 * All styles are stored as plain, portable lines. Existing Markdown checklists
 * (- [ ] and - [x]) remain readable and editable; new lists use a visible
 * checkbox glyph instead of exposing [ ] tokens to the author.
 */
internal object NeoSmartLists {
    internal data class Prefix(
        val kind:String,
        val length:Int,
        val ordinal:Int=1,
        val checked:Boolean=false
    )
    internal data class Edit(val text:String,val caret:Int)

    private val digits=Regex("""^(\d+)([.)]) """)
    private val latin=Regex("""^([A-Z]+)([.)]) """)
    private val arabic=Regex("""^([أابتثجحخدذرزسشصضطظعغفقكلمنهوي])([.)]) """)
    private val legacyBox=Regex("""^- \[([ xX])\] """)
    private val arabicAlphabet="أبتثجحخدذرزسشصضطظعغفقكلمنهوي"

    fun parse(line:String):Prefix? {
        legacyBox.find(line)?.let {return Prefix("check",it.value.length,checked=it.groupValues[1]!=" ")}
        if(line.startsWith("☐ ") || line.startsWith("☑ "))
            return Prefix("check",2,checked=line.startsWith("☑"))
        if(line.startsWith("• ")||line.startsWith("- ")) return Prefix("bullet",2)
        if(line.startsWith("◦ ")) return Prefix("circle",2)
        if(line.startsWith("▪ ")) return Prefix("square",2)
        digits.find(line)?.let {
            val number=it.groupValues[1].toIntOrNull() ?: return null
            return Prefix(if(it.groupValues[2]==")") "numberParen" else "number",it.value.length,number)
        }
        latin.find(line)?.let {
            val value=it.groupValues[1].fold(0) {n,c->n*26+(c-'A'+1)}
            return Prefix(if(it.groupValues[2]==")") "letterParen" else "letter",it.value.length,value)
        }
        arabic.find(line)?.let {
            val char=it.groupValues[1].first()
            val n=arabicAlphabet.indexOf(char).coerceAtLeast(0)+1
            return Prefix("arabic",it.value.length,n)
        }
        return null
    }

    private fun letters(n:Int):String {
        var current=n.coerceAtLeast(1)
        val b=StringBuilder()
        while(current>0) {
            current--
            b.append(('A'.code+current%26).toChar())
            current/=26
        }
        return b.reverse().toString()
    }

    private fun marker(kind:String, ordinal:Int=1, checked:Boolean=false):String=when(kind) {
        "number" -> ordinal.toString()+". "
        "numberParen" -> ordinal.toString()+") "
        "letter" -> letters(ordinal)+". "
        "letterParen" -> letters(ordinal)+") "
        "arabic" -> arabicAlphabet[(ordinal-1).coerceAtLeast(0)%arabicAlphabet.length]+". "
        "circle" -> "◦ "
        "square" -> "▪ "
        "check" -> if(checked) "☑ " else "☐ "
        else -> "• "
    }

    private fun numbered(kind:String):Boolean =
        kind in setOf("number","numberParen","letter","letterParen","arabic")

    private fun lineIndex(text:String,position:Int):Int=
        text.substring(0,position.coerceIn(0,text.length)).count {it=='\n'}

    fun active(text:String,caret:Int,kind:String):Boolean {
        val lines=text.split('\n')
        return parse(lines.getOrElse(lineIndex(text,caret)){""})?.kind==kind
    }

    /** Pressing the active list button turns it off; selecting lines applies as a block. */
    fun toggle(text:String,selection:TextRange,kind:String):Edit {
        val lines=text.split('\n').toMutableList()
        val start=lineIndex(text,selection.min).coerceIn(lines.indices)
        val end=lineIndex(text,(if(selection.collapsed) selection.max else selection.max-1).coerceAtLeast(selection.min))
            .coerceIn(lines.indices)
        val wasActive=(start..end).all {parse(lines[it])?.kind==kind}
        for(i in start..end) {
            val old=lines[i]
            val parsed=parse(old)
            val clean=if(parsed==null) old else old.drop(parsed.length)
            lines[i]=if(wasActive) clean else marker(kind,i-start+1)+clean
        }
        val result=resequence(lines.joinToString("\n"))
        val finalLines=result.split('\n')
        val offset=finalLines.take(start).sumOf{it.length+1}
        val caret=if(selection.collapsed) {
            val oldCol=selection.start-(text.split('\n').take(start).sumOf{it.length+1})
            val oldPrefix=parse(text.split('\n')[start])?.length ?: 0
            val newPrefix=parse(finalLines[start])?.length ?: 0
            (offset+newPrefix+(oldCol-oldPrefix).coerceAtLeast(0)).coerceAtMost(offset+finalLines[start].length)
        } else finalLines.take(end+1).sumOf{it.length+1}-1
        return Edit(result,caret.coerceIn(0,result.length))
    }

    /** Renumber only adjacent ordered rows; unrelated paragraphs stay untouched. */
    fun resequence(text:String):String {
        var previous:String?=null
        var index=0
        return text.split('\n').map {line ->
            val prefix=parse(line)
            if(prefix==null || !numbered(prefix.kind)) {
                previous=null
                line
            } else {
                index=if(prefix.kind==previous) index+1 else prefix.ordinal
                previous=prefix.kind
                marker(prefix.kind,index)+line.drop(prefix.length)
            }
        }.joinToString("\n")
    }

    /**
     * Only interpret an actual, committed Enter, never Arabic composing edits
     * or pasted multiline text. A blank list item exits the list.
     */
    fun onKeyboardEdit(before:String,after:String,caret:Int):Edit? {
        val oldBreaks=before.count{it=='\n'}
        val newBreaks=after.count{it=='\n'}
        if(oldBreaks==newBreaks) return null
        val insertion=(caret-1).coerceAtLeast(0)
        // Check the keyboard's new cursor, not a common-prefix diff: inserting
        // another newline beside an existing one otherwise selects the wrong row.
        val committedEnter=caret in 1..after.length &&
            after[insertion]=='\n' &&
            after.removeRange(insertion,insertion+1)==before
        if(!committedEnter) return Edit(resequence(after),caret).takeIf{it.text!=after}
        val lineStart=before.lastIndexOf('\n',insertion-1)+1
        val oldLineEnd=before.indexOf('\n',insertion).takeIf{it>=0} ?: before.length
        val originalLine=before.substring(lineStart,oldLineEnd)
        val prefix=parse(originalLine) ?: return Edit(resequence(after),caret).takeIf{it.text!=after}
        val preceding=before.substring(lineStart,insertion)
        val next=if(preceding.drop(prefix.length.coerceAtMost(preceding.length)).isBlank()) {
            // Enter on an empty list row removes that marker and exits.
            val out=after.removeRange(lineStart,caret)
            Edit(out,lineStart)
        } else {
            val appended=marker(prefix.kind,prefix.ordinal+1)
            Edit(after.substring(0,caret)+appended+after.substring(caret),caret+appended.length)
        }
        val numberedText=resequence(next.text)
        val line=lineIndex(next.text,next.caret)
        val beforeCol=next.caret-next.text.split('\n').take(line).sumOf{it.length+1}
        val newLines=numberedText.split('\n')
        val finalCaret=(newLines.take(line).sumOf{it.length+1}+beforeCol)
            .coerceIn(0,numberedText.length)
        return Edit(numberedText,finalCaret)
    }

    fun toggleChecked(line:String):String {
        val prefix=parse(line) ?: return line
        if(prefix.kind!="check") return line
        val leading=line.take(prefix.length)
        val rest=line.drop(prefix.length)
        val switched=if(leading.startsWith("- [")) {
            if(prefix.checked) "- [ ] " else "- [x] "
        } else marker("check",checked=!prefix.checked)
        return switched+rest
    }
}
