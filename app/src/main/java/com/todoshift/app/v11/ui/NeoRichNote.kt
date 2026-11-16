package com.todoshift.app.v11.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun noteFormat(value: TextFieldValue, prefix: String, suffix: String = ""): TextFieldValue {
    val start = value.selection.min; val end = value.selection.max
    if(suffix.isEmpty() && prefix in listOf("# ","## ","### ","\n- ","\n1. ","\n- [ ] ")) {
        val lineStart=value.text.lastIndexOf('\n',start-1)+1
        val lineEnd=value.text.indexOf('\n',end).takeIf {it>=0} ?: value.text.length
        val marker=prefix.removePrefix("\n")
        val replacement=value.text.substring(lineStart,lineEnd).lines().mapIndexed {index,line ->
            val clean=line.replaceFirst(Regex("^(#{1,3} |[0-9]+\\. |- (?:\\[[ xX]\\] )?)"),"")
            (if(marker=="1. ") "${index+1}. " else marker)+clean
        }.joinToString("\n")
        return TextFieldValue(value.text.replaceRange(lineStart,lineEnd,replacement),TextRange(lineStart+marker.length,lineStart+replacement.length))
    }
    val selected = value.text.substring(start, end)
    val replacement = prefix + selected + suffix
    return TextFieldValue(value.text.replaceRange(start, end, replacement), TextRange(start + prefix.length, start + prefix.length + selected.length))
}

/** Portable Markdown is persisted in the original details field; no lossy HTML migration. */
@Composable
internal fun NeoNoteFormatting(
    value: TextFieldValue,
    change: (TextFieldValue) -> Unit,
    undo: () -> Unit,
    redo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean
) {
    var colorMenu by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val textTools = listOf(
        "B" to ("**" to "**"),
        "I" to ("*" to "*"),
        "U" to ("<u>" to "</u>"),
        "S̶" to ("<s>" to "</s>"),
        "H1" to ("# " to ""),
        "H2" to ("## " to ""),
        "H3" to ("### " to "")
    )
    val listTools = listOf(
        "•" to ("\n- " to ""),
        "1." to ("\n1. " to ""),
        "☐" to ("\n- [ ] " to ""),
        "🔗" to ("[" to "](https://)")
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            textTools.forEach { (label, marks) ->
                TextButton(
                    onClick = { change(noteFormat(value, marks.first, marks.second)) },
                    modifier = Modifier.widthIn(min = 40.dp).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                ) {
                    Text(label, fontWeight = if (label == "B") FontWeight.Bold else null, maxLines = 1)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listTools.forEach { (label, marks) ->
                TextButton(
                    onClick = { change(noteFormat(value, marks.first, marks.second)) },
                    modifier = Modifier.widthIn(min = 40.dp).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)
                ) { Text(label, maxLines = 1) }
            }
            Box {
                TextButton(
                    onClick = { colorMenu = true },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text(neoLabel("لون ●", "Ink ●"), maxLines = 1) }
                DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                    listOf(
                        "أحمر" to "#C62828", "أزرق" to "#1565C0",
                        "أخضر" to "#2E7D32", "ذهبي" to "#9B6A12"
                    ).forEach { (label, hex) ->
                        DropdownMenuItem(
                            text = { Text(label, color = Color(android.graphics.Color.parseColor(hex))) },
                            onClick = {
                                colorMenu = false
                                change(noteFormat(value, "<font color=\"$hex\">", "</font>"))
                            }
                        )
                    }
                }
            }
            TextButton(onClick = undo, enabled = canUndo, modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("↶") }
            TextButton(onClick = redo, enabled = canRedo, modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("↷") }
        }
    }
}

/**
 * Rendering must use the same nested-style parser as the editing transformation.
 * The old single-regex preview exposed <font>, <u>, and <s> markup when
 * formatting ranges overlapped (especially in mixed Arabic/English notes).
 * No stored note text is changed by this read-only transformation.
 */
internal fun noteInline(text: String): AnnotatedString =
    NeoNoteVisualTransformation().filter(AnnotatedString(text)).text

@Composable
internal fun NeoNotePreview(text: String, checklistsOnly:Boolean=false, onChecklist: ((Int) -> Unit)? = null, onCreateTask: ((Int, String) -> Unit)? = null) {
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        text.lines().forEachIndexed { index, rawLine ->
            val line=noteDisplayText(rawLine)
            val checked = Regex("^\\s*- \\[([ xX])\\] (.*)$").matchEntire(line)
            if (checked != null) Row {
                Checkbox(checked.groupValues[1] != " ", onCheckedChange=onChecklist?.let { callback -> { _: Boolean -> callback(index) } })
                Text(noteInline(checked.groupValues[2]), Modifier.weight(1f).padding(top=12.dp))
                if(onCreateTask!=null) TextButton(onClick={onCreateTask(index,checked.groupValues[2])}) {Text(if(rawLine.contains("<!--task:")) neoLabel("فتح المهمة","Open task") else neoLabel("إلى مهمة","To task"))}
            } else if(!checklistsOnly) {
                val heading = line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ")
                Text(noteInline(if(heading) line.substringAfter(' ') else if(line.startsWith("- ")) "• " + line.drop(2) else line), style=when {line.startsWith("# ") -> MaterialTheme.typography.titleLarge; line.startsWith("## ") -> MaterialTheme.typography.titleMedium; line.startsWith("### ") -> MaterialTheme.typography.titleSmall; else -> MaterialTheme.typography.bodyMedium})
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
        // Keep the portable source unchanged while hiding paired HTML tags in the editor.
        data class OpenMark(val kind:String,val start:Int,val end:Int,val style:SpanStyle)
        val opened=mutableListOf<OpenMark>()
        Regex("""<font color="(#[0-9A-Fa-f]{6})">|</font>|<u>|</u>|<s>|</s>""").findAll(source).forEach { token ->
            val raw=token.value
            val kind=when {raw.startsWith("<font ")||raw=="</font>" -> "font";raw=="<u>"||raw=="</u>" -> "u";else -> "s"}
            if(!raw.startsWith("</")) {
                val style=when(kind) {
                    "font" -> SpanStyle(color=Color(android.graphics.Color.parseColor(token.groupValues[1])))
                    "u" -> SpanStyle(textDecoration=androidx.compose.ui.text.style.TextDecoration.Underline)
                    else -> SpanStyle(textDecoration=androidx.compose.ui.text.style.TextDecoration.LineThrough)
                }
                opened.add(OpenMark(kind,token.range.first,token.range.last+1,style))
            } else {
                val index=opened.indexOfLast {it.kind==kind}
                if(index>=0) {
                    val open=opened.removeAt(index)
                    hide(open.start,open.end);hide(token.range.first,token.range.last+1)
                    spans+=Triple(open.end,token.range.first,open.style)
                }
            }
        }
        Regex("(?m)^#{1,3} ").findAll(source).forEach {
            hide(it.range.first,it.range.last+1)
            val end=source.indexOf('\n',it.range.last+1).takeIf {p->p>=0} ?: source.length
            spans+=Triple(it.range.last+1,end,SpanStyle(fontWeight=FontWeight.Bold,fontSize=when(it.value.count { char -> char == '#' }) {1 -> 22.sp; 2 -> 18.sp; else -> 16.sp}))
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
