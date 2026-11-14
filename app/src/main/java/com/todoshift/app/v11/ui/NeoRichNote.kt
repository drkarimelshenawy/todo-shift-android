package com.todoshift.app.v11.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

internal fun noteFormat(value: TextFieldValue, prefix: String, suffix: String = ""): TextFieldValue {
    val start = value.selection.min; val end = value.selection.max
    val selected = value.text.substring(start, end)
    val replacement = prefix + selected + suffix
    return TextFieldValue(value.text.replaceRange(start, end, replacement), TextRange(start + prefix.length, start + prefix.length + selected.length))
}

/** Portable Markdown is persisted in the original details field; no lossy HTML migration. */
@Composable
internal fun NeoNoteFormatting(value: TextFieldValue, change: (TextFieldValue) -> Unit, undo: () -> Unit, redo: () -> Unit, canUndo: Boolean, canRedo: Boolean) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf("B" to Pair("**", "**"), "I" to Pair("*", "*"), "H" to Pair("# ", ""), "•" to Pair("\n- ", ""), "1." to Pair("\n1. ", ""), "☐" to Pair("\n- [ ] ", "")).forEach { (label, marks) ->
            TextButton(onClick = { change(noteFormat(value, marks.first, marks.second)) }, contentPadding = PaddingValues(4.dp), modifier = Modifier.widthIn(min=48.dp)) { Text(label, fontWeight = if(label=="B") FontWeight.Bold else null) }
        }
        TextButton(onClick=undo, enabled=canUndo) { Text(neoLabel("تراجع", "Undo")) }
        TextButton(onClick=redo, enabled=canRedo) { Text(neoLabel("إعادة", "Redo")) }
    }
}

private fun noteInline(text: String): AnnotatedString = buildAnnotatedString {
    val matches = Regex("\\*\\*(.+?)\\*\\*|\\*([^*]+)\\*").findAll(text)
    var end = 0
    for (match in matches) {
        append(text.substring(end, match.range.first))
        val bold = match.groupValues[1].isNotEmpty()
        withStyle(SpanStyle(fontWeight=if(bold) FontWeight.Bold else null, fontStyle=if(bold) null else FontStyle.Italic)) { append(if(bold) match.groupValues[1] else match.groupValues[2]) }
        end = match.range.last + 1
    }
    append(text.substring(end))
}

@Composable
internal fun NeoNotePreview(text: String, onChecklist: ((Int) -> Unit)? = null, onCreateTask: ((Int, String) -> Unit)? = null) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        text.lines().forEachIndexed { index, rawLine ->
            val line=noteDisplayText(rawLine)
            val checked = Regex("^\\s*- \\[([ xX])\\] (.*)$").matchEntire(line)
            if (checked != null) Row {
                Checkbox(checked.groupValues[1] != " ", onCheckedChange=onChecklist?.let { callback -> { _: Boolean -> callback(index) } })
                Text(noteInline(checked.groupValues[2]), Modifier.weight(1f).padding(top=12.dp))
                if(onCreateTask!=null) TextButton(onClick={onCreateTask(index,checked.groupValues[2])}) {Text(neoLabel("إلى مهمة","To task"))}
            } else {
                val heading = line.startsWith("# ") || line.startsWith("## ")
                Text(noteInline(if(heading) line.substringAfter(' ') else if(line.startsWith("- ")) "• " + line.drop(2) else line), style=if(heading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A stable token travels with its checklist line, but is never shown or shared. */
internal fun noteDisplayText(text:String):String = text.replace(Regex(" ?<!--task:[A-Za-z0-9_-]+-->"),"")

/** Selection offsets map to the portable source while editing displays formatted text. */
internal class NeoNoteVisualTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text:AnnotatedString):androidx.compose.ui.text.input.TransformedText {
        val source=text.text
        val hidden=BooleanArray(source.length)
        val spans=mutableListOf<Triple<Int,Int,SpanStyle>>()
        fun hide(start:Int,end:Int) {for(i in start until end) hidden[i]=true}
        Regex(" ?<!--task:[A-Za-z0-9_-]+-->").findAll(source).forEach {hide(it.range.first,it.range.last+1)}
        Regex("\\*\\*([^\\n]+?)\\*\\*|(?<!\\*)\\*([^*\\n]+)\\*(?!\\*)").findAll(source).forEach {
            val bold=it.groupValues[1].isNotEmpty();val count=if(bold) 2 else 1
            hide(it.range.first,it.range.first+count);hide(it.range.last-count+1,it.range.last+1)
            spans+=Triple(it.range.first+count,it.range.last-count+1,SpanStyle(fontWeight=if(bold) FontWeight.Bold else null,fontStyle=if(bold) null else FontStyle.Italic))
        }
        Regex("(?m)^#{1,3} ").findAll(source).forEach {
            hide(it.range.first,it.range.last+1)
            val end=source.indexOf('\n',it.range.last+1).takeIf {p->p>=0} ?: source.length
            spans+=Triple(it.range.last+1,end,SpanStyle(fontWeight=FontWeight.Bold))
        }
        val original=IntArray(source.length+1)
        val visible=StringBuilder()
        for(i in source.indices) {original[i]=visible.length;if(!hidden[i]) visible.append(source[i])}
        original[source.length]=visible.length
        val reverse=IntArray(visible.length+1)
        for(i in original.indices) reverse[original[i]]=i
        val result=buildAnnotatedString {
            append(visible.toString())
            spans.forEach {(start,end,style)->if(original[start]<original[end])addStyle(style,original[start],original[end])}
        }
        return androidx.compose.ui.text.input.TransformedText(result,object:androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset:Int)=original[offset.coerceIn(original.indices)]
            override fun transformedToOriginal(offset:Int)=reverse[offset.coerceIn(reverse.indices)]
        })
    }
}
