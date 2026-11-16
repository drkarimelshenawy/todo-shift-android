package com.todoshift.app.v11.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.focus.focusRequester
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
    if(suffix.isEmpty() && prefix in listOf("# ","## ","### ","\n- ","\n1. ","\nA. ","\n- [ ] ")) {
        val lineStart=value.text.lastIndexOf('\n',start-1)+1
        val lineEnd=value.text.indexOf('\n',end).takeIf {it>=0} ?: value.text.length
        val marker=prefix.removePrefix("\n")
        val replacement=value.text.substring(lineStart,lineEnd).lines().mapIndexed {index,line ->
            val clean=line.replaceFirst(Regex("^(#{1,3} |[0-9]+\\. |[A-Za-z]\\. |- (?:\\[[ xX]\\] )?)"),"")
            (when(marker) { "1. " -> "${index+1}. "; "A. " -> "${(65+index%26).toChar()}. "; else -> marker })+clean
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
    canRedo: Boolean,
    direction: String = "auto",
    onDirectionChange: (String) -> Unit = {}
) {
    var sizeMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var listMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var directionMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var inkMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var inkHex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("#9B6A12") }
    // Fixed toolbar order in both Arabic and English. Two rows, no header or close button.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement=Arrangement.spacedBy(0.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                listOf(
                    "B" to ("**" to "**"),
                    "I" to ("*" to "*"),
                    "U" to ("<u>" to "</u>"),
                    "S̶" to ("<s>" to "</s>")
                ).forEach { (label,marks) ->
                    TextButton(
                        onClick={change(noteFormat(value,marks.first,marks.second))},
                        modifier=Modifier.weight(1f).height(44.dp),
                        contentPadding=PaddingValues(0.dp)
                    ) {
                        Text(
                            label,maxLines=1,
                            fontWeight=if(label=="B") FontWeight.Bold else null
                        )
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(
                        onClick={sizeMenu=true},
                        modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)
                    ) {Text("Aa ▾",maxLines=1)}
                    DropdownMenu(expanded=sizeMenu,onDismissRequest={sizeMenu=false}) {
                        listOf(14,16,18,20,24,28).forEach {fontSize ->
                            DropdownMenuItem(
                                text={Text("$fontSize sp",fontSize=fontSize.sp)},
                                onClick={
                                    sizeMenu=false
                                    change(noteFormat(value,"<size sp=\"$fontSize\">","</size>"))
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.35f))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                Box(Modifier.weight(1f)) {
                    TextButton(
                        onClick={listMenu=true},
                        modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)
                    ) {Text("☷ ▾",maxLines=1)}
                    DropdownMenu(expanded=listMenu,onDismissRequest={listMenu=false}) {
                        listOf(
                            "•  "+neoLabel("نقط","Bullets") to "\n- ",
                            "☐  "+neoLabel("مربعات","Checklist") to "\n- [ ] ",
                            "1.  "+neoLabel("أرقام","Numbers") to "\n1. ",
                            "A.  "+neoLabel("حروف","Letters") to "\nA. "
                        ).forEach { (label,mark) ->
                            DropdownMenuItem(text={Text(label)},onClick={
                                listMenu=false;change(noteFormat(value,mark))
                            })
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(
                        onClick={directionMenu=true},
                        modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)
                    ) {Text("⇄",maxLines=1)}
                    DropdownMenu(expanded=directionMenu,onDismissRequest={directionMenu=false}) {
                        listOf("auto" to "Auto","rtl" to "RTL","ltr" to "LTR").forEach { (key,label) ->
                            DropdownMenuItem(
                                text={Text(label+(if(direction==key) " ✓" else ""))},
                                onClick={directionMenu=false;onDirectionChange(key)}
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(
                        onClick={inkMenu=true},
                        modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)
                    ) {Text("●",color=Color(android.graphics.Color.parseColor(inkHex)),fontSize=25.sp)}
                    DropdownMenu(expanded=inkMenu,onDismissRequest={inkMenu=false}) {
                        listOf(
                            "أحمر" to "#C62828","أزرق" to "#1565C0",
                            "أخضر" to "#2E7D32","ذهبي" to "#9B6A12",
                            "بنفسجي" to "#7B1FA2","أسود" to "#222222"
                        ).forEach { (label,hex) ->
                            DropdownMenuItem(
                                text={Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    Text("●",fontSize=23.sp,color=Color(android.graphics.Color.parseColor(hex)))
                                    Text(label)
                                }},
                                onClick={
                                    inkMenu=false;inkHex=hex
                                    change(noteFormat(value,"<font color=\"$hex\">","</font>"))
                                }
                            )
                        }
                    }
                }
                TextButton(
                    onClick=undo,enabled=canUndo,
                    modifier=Modifier.weight(1f).height(44.dp),
                    contentPadding=PaddingValues(0.dp)
                ) {Text("↶",fontSize=24.sp)}
                TextButton(
                    onClick=redo,enabled=canRedo,
                    modifier=Modifier.weight(1f).height(44.dp),
                    contentPadding=PaddingValues(0.dp)
                ) {Text("↷",fontSize=24.sp)}
            }
        }
    }
}
@Composable
internal fun NeoRuledNoteEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textDirection: androidx.compose.ui.text.style.TextDirection,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    modifier: Modifier = Modifier
) {
    val lineColor=MaterialTheme.colorScheme.outline.copy(alpha=0.33f)
    val cursorColor=MaterialTheme.colorScheme.primary
    val density=androidx.compose.ui.platform.LocalDensity.current
    val spacing=with(density) { 34.sp.toPx() }
    var bottoms by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Float>>(emptyList()) }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val pageHeight=maxHeight
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal=16.dp)
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value=value,
                onValueChange=onValueChange,
                modifier=Modifier.fillMaxWidth()
                    .heightIn(min=pageHeight)
                    .then(Modifier.focusRequester(focusRequester))
                    .drawBehind {
                        val first=bottoms.ifEmpty { listOf(spacing) }
                        first.forEach { y ->
                            drawLine(
                                lineColor,
                                androidx.compose.ui.geometry.Offset(0f,y),
                                androidx.compose.ui.geometry.Offset(size.width,y),
                                strokeWidth=0.7.dp.toPx()
                            )
                        }
                        var y=first.last()+spacing
                        while(y<size.height) {
                            drawLine(
                                lineColor,
                                androidx.compose.ui.geometry.Offset(0f,y),
                                androidx.compose.ui.geometry.Offset(size.width,y),
                                strokeWidth=0.7.dp.toPx()
                            )
                            y+=spacing
                        }
                    },
                textStyle=MaterialTheme.typography.bodyLarge.copy(
                    color=MaterialTheme.colorScheme.onSurface,
                    lineHeight=34.sp,
                    textDirection=textDirection
                ),
                visualTransformation=androidx.compose.runtime.remember {NeoNoteVisualTransformation()},
                cursorBrush=androidx.compose.ui.graphics.SolidColor(cursorColor),
                onTextLayout={result ->
                    val next=(0 until result.lineCount).map {result.getLineBottom(it)}
                    if(bottoms!=next) bottoms=next
                },
                decorationBox={inner ->
                    Box {
                        if(value.text.isEmpty()) {
                            Text(
                                neoLabel("اكتب ملاحظتك هنا…","Write your note here…"),
                                color=MaterialTheme.colorScheme.onSurfaceVariant,
                                style=MaterialTheme.typography.bodyLarge.copy(lineHeight=34.sp,textDirection=textDirection)
                            )
                        }
                        inner()
                    }
                }
            )
            // Room to scroll the last line above the keyboard/navigation bar.
            Spacer(Modifier.height(196.dp))
        }
    }
}

@Composable
private fun NeoRuledParagraph(
    content: AnnotatedString,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    ruled: Boolean
) {
    if(!ruled) {
        Text(content,modifier=modifier,style=style)
        return
    }
    var bounds by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Float>>(emptyList()) }
    val line=MaterialTheme.colorScheme.outline.copy(alpha=0.33f)
    Text(
        content,
        modifier=modifier.fillMaxWidth().drawBehind {
            bounds.forEach {bottom ->
                val y=(bottom-1.dp.toPx()).coerceAtLeast(0f)
                drawLine(
                    line,
                    androidx.compose.ui.geometry.Offset(0f,y),
                    androidx.compose.ui.geometry.Offset(size.width,y),
                    strokeWidth=0.7.dp.toPx()
                )
            }
        },
        style=style,
        onTextLayout={result ->
            val next=(0 until result.lineCount).map {result.getLineBottom(it)}
            if(bounds!=next) bounds=next
        }
    )
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
internal fun NeoNotePreview(
    text: String,
    checklistsOnly: Boolean = false,
    onChecklist: ((Int) -> Unit)? = null,
    onCreateTask: ((Int, String) -> Unit)? = null,
    textDirection: androidx.compose.ui.text.style.TextDirection =
        androidx.compose.ui.text.style.TextDirection.ContentOrRtl,
    ruled: Boolean = false
) {
    val lineStyle=MaterialTheme.typography.bodyLarge.copy(lineHeight=34.sp,textDirection=textDirection)
    Column(verticalArrangement=Arrangement.spacedBy(0.dp)) {
        text.lines().forEachIndexed { index, rawLine ->
            val line=noteDisplayText(rawLine)
            val checked=Regex("^\\s*- \\[([ xX])\\] (.*)$").matchEntire(line)
            if(checked!=null) {
                val checkedText=checked.groupValues[2]
                if(onChecklist==null && onCreateTask==null) {
                    NeoRuledParagraph(
                        noteInline((if(checked.groupValues[1]==" ") "☐ " else "☑ ")+checkedText),
                        lineStyle,ruled=ruled
                    )
                } else {
                    Row {
                        Checkbox(checked.groupValues[1]!=" ",onCheckedChange=onChecklist?.let {callback->{ _:Boolean->callback(index) }})
                        NeoRuledParagraph(noteInline(checkedText),lineStyle,Modifier.weight(1f).padding(top=6.dp),ruled)
                        if(onCreateTask!=null) TextButton(onClick={onCreateTask(index,checkedText)}) {
                            Text(if(rawLine.contains("<!--task:")) neoLabel("فتح المهمة","Open task") else neoLabel("إلى مهمة","To task"))
                        }
                    }
                }
            } else if(!checklistsOnly) {
                val heading=line.startsWith("# ")||line.startsWith("## ")||line.startsWith("### ")
                val visible=if(heading) line.substringAfter(' ')
                    else if(line.startsWith("- ")) "• "+line.drop(2)
                    else line
                val type=when {
                    line.startsWith("# ") -> MaterialTheme.typography.titleLarge
                    line.startsWith("## ") -> MaterialTheme.typography.titleMedium
                    line.startsWith("### ") -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.bodyLarge
                }
                NeoRuledParagraph(
                    noteInline(visible.ifEmpty {" "}),
                    type.copy(lineHeight=34.sp,textDirection=textDirection),
                    ruled=ruled
                )
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
        Regex("""<font color="(#[0-9A-Fa-f]{6})">|</font>|<size sp="(14|16|18|20|24|28)">|</size>|<u>|</u>|<s>|</s>""").findAll(source).forEach { token ->
            val raw=token.value
            val kind=when {raw.startsWith("<font ")||raw=="</font>" -> "font";raw.startsWith("<size ")||raw=="</size>" -> "size";raw=="<u>"||raw=="</u>" -> "u";else -> "s"}
            if(!raw.startsWith("</")) {
                val style=when(kind) {
                    "font" -> SpanStyle(color=Color(android.graphics.Color.parseColor(token.groupValues[1])))
                    "size" -> SpanStyle(fontSize=token.groupValues[2].toInt().sp)
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
