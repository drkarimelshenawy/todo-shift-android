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
    state:NeoRichEditorState,
    change:(NeoRichEditorState)->Unit,
    undo:()->Unit,redo:()->Unit,canUndo:Boolean,canRedo:Boolean,
    direction:String="auto",onDirectionChange:(String)->Unit={}
) {
    var sizes by androidx.compose.runtime.remember{androidx.compose.runtime.mutableStateOf(false)}
    var lists by androidx.compose.runtime.remember{androidx.compose.runtime.mutableStateOf(false)}
    var directions by androidx.compose.runtime.remember{androidx.compose.runtime.mutableStateOf(false)}
    var colors by androidx.compose.runtime.remember{androidx.compose.runtime.mutableStateOf(false)}
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                listOf("B" to "bold","I" to "italic","U" to "underline","S̶" to "strike")
                    .forEach{(label,mark)->
                        FilterChip(
                            selected=state.active(mark),onClick={change(state.toggle(mark))},
                            modifier=Modifier.weight(1f).height(42.dp),
                            label={Text(label,modifier=Modifier.fillMaxWidth(),
                                textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight=if(mark=="bold") FontWeight.Bold else null)}
                        )
                    }
                Box(Modifier.weight(1.2f)) {
                    FilterChip(
                        selected=state.effective().size!=null,
                        onClick={sizes=true},modifier=Modifier.fillMaxWidth().height(42.dp),
                        label={Text("${state.effective().size ?: "Aa"} ▾",
                            modifier=Modifier.fillMaxWidth(),
                            textAlign=androidx.compose.ui.text.style.TextAlign.Center,maxLines=1)}
                    )
                    DropdownMenu(expanded=sizes,onDismissRequest={sizes=false}) {
                        listOf(14,16,18,20,24,28).forEach{points->
                            DropdownMenuItem(text={Text("$points sp",fontSize=points.sp)},
                                onClick={sizes=false;change(state.fontSize(points))})
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                Box(Modifier.weight(1f)) {
                    TextButton(onClick={lists=true},modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)){Text("☷ ▾",maxLines=1)}
                    DropdownMenu(expanded=lists,onDismissRequest={lists=false}){
                        listOf("• " to "- ","☐ " to "- [ ] ","1. " to "1. ","A. " to "A. ")
                            .forEach{(label,marker)->
                                DropdownMenuItem(text={Text(label)},onClick={lists=false;change(state.list(marker))})
                            }
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(onClick={directions=true},modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)){Text("⇄")}
                    DropdownMenu(expanded=directions,onDismissRequest={directions=false}){
                        listOf("auto" to "Auto","rtl" to "RTL","ltr" to "LTR")
                            .forEach{(key,label)->
                                DropdownMenuItem(text={Text(label+(if(direction==key)" ✓" else ""))},
                                    onClick={directions=false;onDirectionChange(key)})
                            }
                    }
                }
                Box(Modifier.weight(1f)) {
                    TextButton(onClick={colors=true},modifier=Modifier.fillMaxWidth().height(44.dp),
                        contentPadding=PaddingValues(0.dp)) {
                        Text("●",fontSize=24.sp,color=state.effective().color?.let{
                            runCatching{Color(android.graphics.Color.parseColor(it))}.getOrNull()
                        } ?: MaterialTheme.colorScheme.primary)
                    }
                    DropdownMenu(expanded=colors,onDismissRequest={colors=false}) {
                        listOf("أحمر" to "#C62828","أزرق" to "#1565C0","أخضر" to "#2E7D32",
                            "ذهبي" to "#9B6A12","بنفسجي" to "#7B1FA2","أسود" to "#222222")
                            .forEach{(label,hex)->
                                DropdownMenuItem(text={
                                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                        Text("●",color=Color(android.graphics.Color.parseColor(hex)))
                                        Text(label)
                                    }
                                },onClick={colors=false;change(state.ink(hex))})
                            }
                    }
                }
                TextButton(onClick=undo,enabled=canUndo,modifier=Modifier.weight(1f).height(44.dp),
                    contentPadding=PaddingValues(0.dp)){Text("↶",fontSize=24.sp)}
                TextButton(onClick=redo,enabled=canRedo,modifier=Modifier.weight(1f).height(44.dp),
                    contentPadding=PaddingValues(0.dp)){Text("↷",fontSize=24.sp)}
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
        val density=androidx.compose.ui.platform.LocalDensity.current
        val visibleHeight=with(density){pageHeight.toPx()}
        val viewportScroll=rememberScrollState()
        var cursorTop by androidx.compose.runtime.remember {androidx.compose.runtime.mutableFloatStateOf(0f)}
        var cursorBottom by androidx.compose.runtime.remember {androidx.compose.runtime.mutableFloatStateOf(0f)}
        val bottomMargin=with(density){84.dp.toPx()}
        // The cursor, not merely the bottom of a tall text field, must move above IME.
        androidx.compose.runtime.LaunchedEffect(value.selection,visibleHeight,cursorBottom,cursorTop,viewportScroll.maxValue) {
            val current=viewportScroll.value.toFloat()
            val desired=when {
                cursorBottom-current > visibleHeight-bottomMargin -> cursorBottom-visibleHeight+bottomMargin
                cursorTop-current < bottomMargin*0.32f -> cursorTop-bottomMargin*0.32f
                else -> current
            }.coerceIn(0f,viewportScroll.maxValue.toFloat())
            if(kotlin.math.abs(desired-current)>1f) viewportScroll.scrollTo(desired.toInt())
        }
        Column(Modifier.fillMaxSize().verticalScroll(viewportScroll).padding(horizontal=16.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value=value,
                onValueChange=onValueChange,
                modifier=Modifier.fillMaxWidth().heightIn(min=pageHeight)
                    .focusRequester(focusRequester).drawBehind {
                        val first=bottoms.ifEmpty {listOf(spacing)}
                        first.forEach{y->
                            drawLine(lineColor,androidx.compose.ui.geometry.Offset(0f,y),
                                androidx.compose.ui.geometry.Offset(size.width,y),strokeWidth=0.7.dp.toPx())
                        }
                        var y=first.last()+spacing
                        while(y<size.height) {
                            drawLine(lineColor,androidx.compose.ui.geometry.Offset(0f,y),
                                androidx.compose.ui.geometry.Offset(size.width,y),strokeWidth=0.7.dp.toPx())
                            y+=spacing
                        }
                    },
                textStyle=MaterialTheme.typography.bodyLarge.copy(
                    color=MaterialTheme.colorScheme.onSurface,
                    lineHeight=34.sp,textDirection=textDirection,
                    textAlign=if(textDirection==androidx.compose.ui.text.style.TextDirection.Ltr)
                        androidx.compose.ui.text.style.TextAlign.Left
                    else androidx.compose.ui.text.style.TextAlign.Right
                ),
                cursorBrush=androidx.compose.ui.graphics.SolidColor(cursorColor),
                onTextLayout={result->
                    val next=(0 until result.lineCount).map{result.getLineBottom(it)}
                    if(bottoms!=next) bottoms=next
                    val rect=result.getCursorRect(value.selection.start.coerceIn(0,value.text.length))
                    if(cursorTop!=rect.top) cursorTop=rect.top
                    if(cursorBottom!=rect.bottom) cursorBottom=rect.bottom
                },
                decorationBox={inner->
                    Box {
                        if(value.text.isEmpty()) Text(
                            neoLabel("اكتب ملاحظتك هنا…","Write your note here…"),
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                            style=MaterialTheme.typography.bodyLarge.copy(lineHeight=34.sp,
                                textDirection=textDirection,
                                textAlign=if(textDirection==androidx.compose.ui.text.style.TextDirection.Ltr)
                                    androidx.compose.ui.text.style.TextAlign.Left
                                else androidx.compose.ui.text.style.TextAlign.Right)
                        )
                        inner()
                    }
                }
            )
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
            val raw=noteDisplayText(rawLine)
            val rendered=noteInline(raw)
            // Parse the visible, formatted content: stored <b>/<i>/font wrappers
            // must never break checklists or expose their markup in reading mode.
            val checked=Regex("^\\s*- \\[([ xX])\\] (.*)$").matchEntire(rendered.text)
            if(checked!=null) {
                val label=checked.groupValues[2]
                val start=checked.groups[2]?.range?.first ?: rendered.length
                val styled=rendered.subSequence(start,rendered.length)
                if(onChecklist==null && onCreateTask==null) {
                    NeoRuledParagraph(
                        buildAnnotatedString {
                            append(if(checked.groupValues[1]==" ") "☐ " else "☑ ")
                            append(styled)
                        },lineStyle,ruled=ruled
                    )
                } else {
                    Row {
                        Checkbox(
                            checked.groupValues[1]!=" ",
                            onCheckedChange=onChecklist?.let{callback->{_:Boolean->callback(index)}}
                        )
                        NeoRuledParagraph(styled,lineStyle,Modifier.weight(1f).padding(top=6.dp),ruled)
                        if(onCreateTask!=null) TextButton(onClick={onCreateTask(index,label)}) {
                            Text(if(rawLine.contains("<!--task:")) neoLabel("فتح المهمة","Open task")
                                else neoLabel("إلى مهمة","To task"))
                        }
                    }
                }
            } else if(!checklistsOnly) {
                val heading=raw.startsWith("# ")||raw.startsWith("## ")||raw.startsWith("### ")
                val styled=if(rendered.text.startsWith("- ")) {
                    buildAnnotatedString {append("• ");append(rendered.subSequence(2,rendered.length))}
                } else rendered
                val type=when {
                    raw.startsWith("# ") -> MaterialTheme.typography.titleLarge
                    raw.startsWith("## ") -> MaterialTheme.typography.titleMedium
                    raw.startsWith("### ") -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.bodyLarge
                }
                NeoRuledParagraph(
                    if(styled.isEmpty()) AnnotatedString(" ") else styled,
                    type.copy(lineHeight=34.sp,textDirection=textDirection),ruled=ruled
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
        Regex("""<font color="(#[0-9A-Fa-f]{6})">|</font>|<size sp="(14|16|18|20|24|28)">|</size>|<b>|</b>|<i>|</i>|<u>|</u>|<s>|</s>""").findAll(source).forEach { token ->
            val raw=token.value
            val kind=when {raw.startsWith("<font ")||raw=="</font>" -> "font";raw.startsWith("<size ")||raw=="</size>" -> "size";raw=="<b>"||raw=="</b>" -> "b";raw=="<i>"||raw=="</i>" -> "i";raw=="<u>"||raw=="</u>" -> "u";else -> "s"}
            if(!raw.startsWith("</")) {
                val style=when(kind) {
                    "font" -> SpanStyle(color=Color(android.graphics.Color.parseColor(token.groupValues[1])))
                    "size" -> SpanStyle(fontSize=token.groupValues[2].toInt().sp)
                    "b" -> SpanStyle(fontWeight=FontWeight.Bold)
                    "i" -> SpanStyle(fontStyle=FontStyle.Italic)
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
