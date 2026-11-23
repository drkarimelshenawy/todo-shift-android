package com.todoshift.app.v11.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp

/**
 * Styled text model for the app's Compose 2024.12 BOM. The IME and selection
 * operate on visible characters, never on serialized note formatting tags.
 * Existing backup, draft and storage schemas remain unchanged.
 */
internal data class NeoRichCharStyle(
    val bold: Boolean = false, val italic: Boolean = false,
    val underline: Boolean = false, val strike: Boolean = false,
    val size: Int? = null, val color: String? = null,
    val face:String?=null,
    val highlight:String?=null,
    val script:String?=null
) {
    fun span(): SpanStyle = SpanStyle(
        fontWeight=if(bold) FontWeight.Bold else null,
        fontStyle=if(italic) FontStyle.Italic else null,
        textDecoration=when {
            underline && strike -> TextDecoration.combine(listOf(TextDecoration.Underline,TextDecoration.LineThrough))
            underline -> TextDecoration.Underline
            strike -> TextDecoration.LineThrough
            else -> null
        },
        fontFamily=face?.let(NeoNoteTypefaces::family),
        fontSize=if (script != null) ((size ?: 16) * 0.75f).sp
            else size?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
        color=color?.let {runCatching {Color(android.graphics.Color.parseColor(it))}.getOrNull()} ?: Color.Unspecified,
        background=highlight?.let {runCatching {Color(android.graphics.Color.parseColor(it))}.getOrNull()} ?: Color.Unspecified,
        baselineShift=when(script) {
            "sup" -> BaselineShift.Superscript
            "sub" -> BaselineShift.Subscript
            else -> null
        }
    )
}

internal data class NeoRichHiddenToken(val position:Int,val text:String)

internal data class NeoRichEditorState(
    val value:TextFieldValue,
    val characters:List<NeoRichCharStyle>,
    val hidden:List<NeoRichHiddenToken> = emptyList(),
    val pending:NeoRichCharStyle? = null,
    val paragraphs:List<NeoRichParagraphStyle> =
        List(value.text.count { it=='\n' } + 1) { NeoRichParagraphStyle() }
) {
    init {
        require(value.text.length==characters.size)
        require(paragraphs.size==value.text.count { it=='\n' }+1)
    }

    // Paint the existing character styles over an IME buffer without dropping its
    // composition range. Earlier formatted lines must stay formatted while typing.
    private fun styledBuffer(
        buffer:AnnotatedString,styles:List<NeoRichCharStyle>,
        paragraphs:List<NeoRichParagraphStyle>
    ):AnnotatedString=buildAnnotatedString {
        append(buffer)
        var index=0
        while(index<styles.size) {
            var until=index+1
            while(until<styles.size && styles[until]==styles[index]) until++
            if(styles[index]!=NeoRichCharStyle()) addStyle(styles[index].span(),index,until)
            index=until
        }
        var offset=0
        buffer.text.split('\n').forEachIndexed { line, value ->
            val format=paragraphs.getOrElse(line) { NeoRichParagraphStyle() }
            if (format!=NeoRichParagraphStyle() && value.isNotEmpty())
                addStyle(format.asCompose(),offset,offset+value.length)
            offset+=value.length+1
        }
    }

    private fun withValue(
        text:String,styles:List<NeoRichCharStyle>,selection:TextRange,
        tokens:List<NeoRichHiddenToken> = hidden,
        typing:NeoRichCharStyle? = pending,
        composition:TextRange? = null,
        paragraphFormats:List<NeoRichParagraphStyle> = paragraphs,
    ):NeoRichEditorState =
        NeoRichEditorState(TextFieldValue(styledBuffer(AnnotatedString(text),styles,paragraphFormats),
            selection,composition),styles,tokens,typing,paragraphFormats)


    /** Diff an IME edit and move its style ranges and linked-checklist anchors. */
    fun input(next:TextFieldValue):NeoRichEditorState {
        if(next.text==value.text) {
            val typing=if(value.composition==null && next.composition==null &&
                next.selection!=value.selection) null else pending
            // When an Arabic IME commits its composing word without a text
            // change, apply the chosen styles only after composition finishes.
            return if(value.composition!=null && next.composition==null)
                withValue(value.text,characters,next.selection,typing=typing)
            else copy(
                value=if(next.composition!=null) next
                    else TextFieldValue(value.annotatedString,next.selection,next.composition),
                pending=typing
            )
        }
        val before=value.text;val after=next.text
        var start=0
        while(start<before.length && start<after.length && before[start]==after[start]) start++
        var oldEnd=before.length;var newEnd=after.length
        while(oldEnd>start && newEnd>start && before[oldEnd-1]==after[newEnd-1]) {oldEnd--;newEnd--}
        val added=newEnd-start
        val style=pending ?: characters.getOrNull(start-1) ?: characters.getOrNull(start) ?: NeoRichCharStyle()
        val styles=characters.take(start)+List(added){style}+characters.drop(oldEnd)
        val delta=added-(oldEnd-start)
        // Track paragraph formats through IME changes and newline insertion/deletion.
        // Every newly inserted line inherits the format of the edit's original line.
        fun oldParagraph(offset:Int):NeoRichParagraphStyle {
            val line=before.substring(0,offset.coerceIn(0,before.length)).count{it=='\n'}
            return paragraphs.getOrElse(line){ NeoRichParagraphStyle() }
        }
        val nextParagraphs=buildList {
            var newLineStart=0
            after.split('\n').forEach { line ->
                val previous=when {
                    newLineStart < start -> newLineStart
                    newLineStart >= newEnd -> newLineStart-delta
                    else -> start
                }
                add(oldParagraph(previous))
                newLineStart+=line.length+1
            }
        }
        val tokens=hidden.map {token ->token.copy(
            position=(when {
                token.text.matches(Regex("^#{1,3} ")) && token.position==start -> start
                token.position<start -> token.position
                token.position>=oldEnd -> token.position+delta
                else -> newEnd
            }).coerceIn(0,after.length)
        )}
        // Preserve the exact IME-supplied editing buffer while composing.
        // Replacing it with a newly serialized AnnotatedString on every Arabic
        // keystroke can invalidate composing spans and move the cursor.
        if(next.composition!=null) {
            val composing=requireNotNull(next.composition)
            // Leave the IME-owned composing region byte-for-byte untouched.
            // Paint only previously formatted characters outside that range:
            // this prevents an earlier bold paragraph flashing as plain text
            // without changing the keyboard's in-progress Arabic word.
            val previousStyles=styles.mapIndexed {index,mark ->
                if(index>=composing.min && index<composing.max)
                    NeoRichCharStyle() else mark
            }
            val hasEarlierFormat=previousStyles.any {it!=NeoRichCharStyle()}
            return copy(
                value=if(!hasEarlierFormat) next else TextFieldValue(
                    styledBuffer(next.annotatedString,previousStyles,nextParagraphs),
                    next.selection,next.composition),
                characters=styles,hidden=tokens,pending=pending,
                paragraphs=nextParagraphs
            )
        }
        val edited=withValue(after,styles,next.selection,tokens,pending,next.composition,
            nextParagraphs)
        val smart=NeoSmartLists.onKeyboardEdit(before,after,next.selection.start)
        return if(smart!=null && smart.text!=after)
            edited.input(TextFieldValue(smart.text,TextRange(smart.caret)))
        else edited
    }

    /** A selection or a word under the caret; at a boundary toggle future typing. */
    private fun editRange():IntRange? {
        val sel=value.selection
        if(!sel.collapsed) return sel.min until sel.max
        val txt=value.text;val caret=sel.start.coerceIn(0,txt.length)
        if(caret>=txt.length || !isWordUnit(txt[caret])) return null
        var start=caret;var end=caret
        while(start>0 && isWordUnit(txt[start-1])) start--
        while(end<txt.length && isWordUnit(txt[end])) end++
        return if(start<end) start until end else null
    }

    fun effective():NeoRichCharStyle {
        val range=editRange()
        if(range!=null && !value.selection.collapsed) return characters.getOrNull(range.first) ?: NeoRichCharStyle()
        return pending ?: characters.getOrNull(value.selection.start)
            ?: characters.getOrNull(value.selection.start-1) ?: NeoRichCharStyle()
    }
    fun active(mark:String):Boolean {
        val range=editRange()
        val chars=if(range!=null) range.mapNotNull {characters.getOrNull(it)} else listOf(effective())
        return chars.isNotEmpty() && chars.all {
            when(mark) {"bold"->it.bold;"italic"->it.italic;"underline"->it.underline;"strike"->it.strike;else->false}
        }
    }
    fun toggle(mark:String):NeoRichEditorState {
        val enabled=!active(mark)
        return style { when(mark) {
            "bold"->it.copy(bold=enabled)
            "italic"->it.copy(italic=enabled)
            "underline"->it.copy(underline=enabled)
            "strike"->it.copy(strike=enabled)
            else->it
        } }
    }
    fun fontSize(points:Int):NeoRichEditorState=style {it.copy(size=points)}
    fun ink(hex:String):NeoRichEditorState=style {it.copy(color=hex)}
    fun highlight(hex:String?):NeoRichEditorState {
        require(hex==null || Regex("#[0-9A-Fa-f]{6}").matches(hex))
        return style { it.copy(highlight=hex) }
    }
    /** Mutually exclusive baseline positions; tapping an active script resets it. */
    fun script(kind:String):NeoRichEditorState {
        require(kind in setOf("sup","sub"))
        val next=if(effective().script==kind) null else kind
        return style {it.copy(script=next)}
    }
    fun paragraphAlign(alignment:String):NeoRichEditorState {
        require(alignment in NeoRichParagraphStyle.ALIGNMENTS)
        val selected=selectedParagraphs()
        val updated=paragraphs.mapIndexed { index, format ->
            if(index in selected) format.copy(align=alignment) else format
        }
        return withValue(value.text,characters,value.selection,paragraphFormats=updated)
    }
    fun paragraphIndent(delta:Int):NeoRichEditorState {
        require(delta in -1..1)
        val selected=selectedParagraphs()
        val updated=paragraphs.mapIndexed { index, format ->
            if(index in selected) format.copy(indent=(format.indent+delta).coerceIn(0,4))
            else format
        }
        return withValue(value.text,characters,value.selection,paragraphFormats=updated)
    }
    fun currentParagraph():NeoRichParagraphStyle =
        paragraphs.getOrElse(value.text.take(value.selection.min).count { it=='\n' }) {
            NeoRichParagraphStyle()
        }
    private fun selectedParagraphs():IntRange {
        val start=value.text.take(value.selection.min).count { it=='\n' }
        val endOffset=if(value.selection.collapsed)value.selection.max
            else (value.selection.max-1).coerceAtLeast(value.selection.min)
        val end=value.text.take(endOffset).count { it=='\n' }
        return start..end
    }
    /** Applies selected font to highlighted text/word or future typing. */
    fun face(name:String?):NeoRichEditorState {
        require(name==null || name in com.todoshift.app.v11.NeoNotePresentation.FONT_KEYS)
        return style {it.copy(face=name)}
    }
    private fun style(change:(NeoRichCharStyle)->NeoRichCharStyle):NeoRichEditorState {
        val range=editRange() ?: return copy(pending=change(effective()))
        val styled=characters.toMutableList()
        range.forEach {if(it in styled.indices) styled[it]=change(styled[it])}
        return withValue(value.text,styled,value.selection,typing=null)
    }

    fun listActive(kind:String):Boolean =
        NeoSmartLists.active(value.text,value.selection.start,kind)

    fun list(kind:String):NeoRichEditorState {
        val edit=NeoSmartLists.toggle(value.text,value.selection,kind)
        return input(TextFieldValue(edit.text,TextRange(edit.caret)))
    }

    fun toggleChecklist():NeoRichEditorState =
        toggleChecklistAtLine(value.text.substring(0,value.selection.start.coerceIn(0,value.text.length)).count {it=='\n'})

    /** One-tap checkbox from the visual editor; offsets and task anchors survive. */
    fun toggleChecklistAtLine(index:Int):NeoRichEditorState {
        val lines=value.text.split('\n').toMutableList()
        if(index !in lines.indices) return this
        val updated=NeoSmartLists.toggleChecked(lines[index])
        if(updated==lines[index]) return this
        lines[index]=updated
        val offset=lines.take(index).sumOf{it.length+1}
        val newCaret=(offset+(value.selection.start-offset).coerceAtMost(updated.length))
            .coerceIn(0,lines.joinToString("\n").length)
        return input(TextFieldValue(lines.joinToString("\n"),TextRange(newCaret)))
    }

    /** Encode WYSIWYG ranges into inline tags understood by the existing reader. */
    fun stored():String {
        val text=value.text;val tokens=hidden.groupBy {it.position.coerceIn(0,text.length)}
        val out=StringBuilder()
        // List prefixes are structure, not user-formatted words. Keeping their
        // serialization outside rich spans preserves checklist recognition.
        val listMarker=BooleanArray(text.length)
        var lineOffset=0
        text.split('\n').forEach {line ->
            val prefix=NeoSmartLists.parse(line)
            if(prefix!=null) for(j in 0 until prefix.length)
                if(lineOffset+j in listMarker.indices) listMarker[lineOffset+j]=true
            lineOffset+=line.length+1
        }
        var open=emptyList<Pair<String,String>>()
        fun close() {open.asReversed().forEach{out.append(it.second)};open=emptyList()}
        var lineIndex=0
        fun saveParagraph() {
            paragraphs.getOrNull(lineIndex)?.portableTag()?.let(out::append)
            lineIndex++
        }
        for(i in 0..text.length) {
            if(tokens[i].orEmpty().isNotEmpty()) {
                close();tokens[i].orEmpty().forEach{out.append(it.text)}
            }
            if(i==text.length) break
            if(text[i]=='\n') {close();saveParagraph();out.append('\n');continue}
            val style=if(listMarker[i]) NeoRichCharStyle() else characters[i]
            val tags=buildList {
                if(style.bold)add("<b>" to "</b>")
                if(style.italic)add("<i>" to "</i>")
                if(style.underline)add("<u>" to "</u>")
                if(style.strike)add("<s>" to "</s>")
                style.size?.let{add("<size sp=\"$it\">" to "</size>")}
                style.color?.let{add("<font color=\"$it\">" to "</font>")}
                style.face?.let{add("<face key=\"$it\">" to "</face>")}
                style.highlight?.let { add("<mark color=\"$it\">" to "</mark>") }
                style.script?.let { add("<$it>" to "</$it>") }
            }
            if(tags!=open) {close();tags.forEach{out.append(it.first)};open=tags}
            out.append(text[i])
        }
        close()
        saveParagraph()
        return out.toString()
    }

    companion object {
        private fun isWordUnit(c:Char):Boolean =
            c.isLetterOrDigit() ||
                Character.getType(c)==Character.NON_SPACING_MARK.toInt() ||
                Character.getType(c)==Character.COMBINING_SPACING_MARK.toInt()

        fun open(source:String):NeoRichEditorState {
            val transformed=NeoNoteVisualTransformation().filter(AnnotatedString(source))
            val annotated=transformed.text
            val paragraphFormats=source.split('\n').map(NeoRichParagraphStyle::fromSavedLine)
            val internalTaskTokens=Regex(" ?<!--task:[A-Za-z0-9_-]+-->")
                .findAll(source).map { token ->
                    NeoRichHiddenToken(
                        transformed.offsetMapping.originalToTransformed(token.range.first),
                        token.value)
                }
            val headingTokens=Regex("(?m)^#{1,3} ").findAll(source).map { token ->
                NeoRichHiddenToken(
                    transformed.offsetMapping.originalToTransformed(token.range.first),
                    token.value)
            }
            val tokens=(internalTaskTokens+headingTokens).sortedBy { it.position }.toList()
            val styles=MutableList(annotated.length){NeoRichCharStyle()}
            annotated.spanStyles.forEach{range ->
                for(i in range.start.coerceAtLeast(0) until range.end.coerceAtMost(styles.size)){
                    val item=range.item;val prev=styles[i]
                    val size=item.fontSize.takeIf{it.type==TextUnitType.Sp}?.value?.toInt()
                    val color=item.color.takeIf{it!=Color.Unspecified}?.toArgb()
                        ?.let{String.format("#%06X",it and 0xFFFFFF)}
                    val baseline=when(item.baselineShift) {
                        BaselineShift.Superscript -> "sup"
                        BaselineShift.Subscript -> "sub"
                        else -> null
                    }
                    val highlighted=item.background.takeIf { it!=Color.Unspecified }?.toArgb()
                        ?.let { String.format("#%06X", it and 0xFFFFFF) }
                    styles[i]=prev.copy(
                        bold=prev.bold||(item.fontWeight?.weight ?: 0)>=FontWeight.SemiBold.weight,
                        italic=prev.italic||item.fontStyle==FontStyle.Italic,
                        underline=prev.underline||item.textDecoration?.contains(TextDecoration.Underline)==true,
                        strike=prev.strike||item.textDecoration?.contains(TextDecoration.LineThrough)==true,
                        size=if(baseline!=null) prev.size else size?:prev.size,
                        color=color?:prev.color,
                        face=NeoNoteTypefaces.keyOf(item.fontFamily) ?: prev.face,
                        highlight=highlighted?:prev.highlight,script=baseline?:prev.script
                    )
                }
            }
            return NeoRichEditorState(TextFieldValue(annotated,TextRange(annotated.length)),
                styles,tokens,paragraphs=paragraphFormats)
        }
    }
}
