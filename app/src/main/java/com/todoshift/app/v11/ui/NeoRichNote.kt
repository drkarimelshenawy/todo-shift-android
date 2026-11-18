package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    direction:String="auto",onDirectionChange:(String)->Unit={},
    expanded:Boolean=false,onExpandedChange:(Boolean)->Unit={}
) {
    var lists by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    var faceMenu by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    var more by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    var morePage by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf("root")}
    var sizeOpen by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    var inkOpen by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    var directionOpen by androidx.compose.runtime.remember {androidx.compose.runtime.mutableStateOf(false)}
    val effective=state.effective()
    val allLists=listOf("bullet","circle","square","check",
        "number","numberParen","letter","letterParen","arabic")

    // Fixed icon order in both locales; text direction is not altered here.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal=6.dp,vertical=4.dp),
            horizontalArrangement=Arrangement.spacedBy(4.dp),
            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
        ) {
            NeoFormatAction(Icons.Rounded.FormatBold,neoLabel("عريض","Bold"),
                selected=state.active("bold"),action={change(state.toggle("bold"))})
            NeoFormatAction(Icons.Rounded.FormatItalic,neoLabel("مائل","Italic"),
                selected=state.active("italic"),action={change(state.toggle("italic"))})
            NeoFormatAction(Icons.Rounded.Tune,neoLabel("أدوات متقدمة","Advanced tools"),
                selected=expanded,action={onExpandedChange(!expanded)})
            Box {
                TextButton(onClick={faceMenu=true},modifier=Modifier.size(44.dp),
                    contentPadding=PaddingValues(0.dp)) {
                    Text("Aa",style=MaterialTheme.typography.titleMedium)
                }
                DropdownMenu(expanded=faceMenu,onDismissRequest={faceMenu=false}) {
                    DropdownMenuItem(
                        text={Text(neoLabel("الخط الأساسي للنص المحدد","Default selected-text font"))},
                        onClick={faceMenu=false;change(state.face(null))}
                    )
                    HorizontalDivider()
                    listOf(
                        "amiri" to "Amiri · نسخي",
                        "tajawal" to "Tajawal · حديث",
                        "reem" to "Reem Kufi · كوفي"
                    ).forEach {(key,label) ->
                        DropdownMenuItem(
                            text={Text(label,style=MaterialTheme.typography.bodyLarge.copy(
                                fontFamily=NeoNoteTypefaces.family(key)))},
                            onClick={faceMenu=false;change(state.face(key))}
                        )
                    }
                    HorizontalDivider()
                    listOf(
                        "montserrat" to "Montserrat",
                        "lora" to "Lora",
                        "oswald" to "Oswald"
                    ).forEach {(key,label) ->
                        DropdownMenuItem(
                            text={Text(label,style=MaterialTheme.typography.bodyLarge.copy(
                                fontFamily=NeoNoteTypefaces.family(key)))},
                            onClick={faceMenu=false;change(state.face(key))}
                        )
                    }
                }
            }

            Box {
                NeoFormatAction(Icons.Rounded.FormatListBulleted,
                    neoLabel("قوائم ذكية","Smart lists"),
                    selected=allLists.any {state.listActive(it)},
                    action={lists=true})
                DropdownMenu(expanded=lists,onDismissRequest={lists=false}) {
                    listOf(
                        Triple(neoLabel("نقاط","Bullets"),"•","bullet"),
                        Triple(neoLabel("دوائر","Circles"),"◦","circle"),
                        Triple(neoLabel("مربعات","Squares"),"▪","square"),
                        Triple(neoLabel("قائمة اختيار","Checklist"),"☐","check"),
                        Triple(neoLabel("أرقام","Numbers"),"1.","number"),
                        Triple(neoLabel("أرقام بأقواس","Numbers with parentheses"),"1)","numberParen"),
                        Triple(neoLabel("حروف لاتينية","Latin letters"),"A.","letter"),
                        Triple(neoLabel("حروف بأقواس","Letter parentheses"),"A)","letterParen"),
                        Triple(neoLabel("حروف عربية","Arabic letters"),"أ.","arabic")
                    ).forEach {(label,icon,kind) ->
                        DropdownMenuItem(
                            text={Text("$icon   $label"+if(state.listActive(kind)) " ✓" else "")},
                            onClick={lists=false;change(state.list(kind))}
                        )
                    }
                }
            }
            NeoFormatAction(Icons.Rounded.Undo,neoLabel("تراجع","Undo"),
                enabled=canUndo,action=undo)
            Box {
                NeoFormatAction(Icons.Rounded.MoreHoriz,
                    neoLabel("مزيد من التنسيق","More formatting"),
                    action={morePage="root";more=true})
                DropdownMenu(expanded=more,onDismissRequest={more=false}) {
                    if(morePage=="root") {
                        DropdownMenuItem(
                            text={Text(neoLabel("تسطير","Underline")+
                                if(state.active("underline")) " ✓" else "")},
                            onClick={more=false;change(state.toggle("underline"))}
                        )
                        DropdownMenuItem(
                            text={Text(neoLabel("شطب","Strikethrough")+
                                if(state.active("strike")) " ✓" else "")},
                            onClick={more=false;change(state.toggle("strike"))}
                        )
                        if(state.listActive("check")) DropdownMenuItem(
                            text={Text(neoLabel("تحديد بند Checklist","Toggle checklist"))},
                            onClick={more=false;change(state.toggleChecklist())}
                        )
                        DropdownMenuItem(
                            text={Text(neoLabel("حجم الخط","Font size"))},
                            onClick={morePage="size"}
                        )
                        DropdownMenuItem(
                            text={Text(neoLabel("لون النص","Text color"))},
                            onClick={morePage="color"}
                        )
                        DropdownMenuItem(
                            text={Text(neoLabel("اتجاه الكتابة","Text direction"))},
                            onClick={morePage="direction"}
                        )
                        DropdownMenuItem(
                            text={Text(neoLabel("إعادة التعديل","Redo"))},
                            enabled=canRedo,
                            onClick={more=false;redo()}
                        )
                    } else {
                        DropdownMenuItem(
                            text={Text(neoLabel("رجوع","Back"))},
                            onClick={morePage="root"}
                        )
                        when(morePage) {
                            "size" -> listOf(14,16,18,20,24,28).forEach {points ->
                                DropdownMenuItem(
                                    text={Text("$points sp")},
                                    onClick={more=false;change(state.fontSize(points))}
                                )
                            }
                            "color" -> listOf(
                                neoLabel("أحمر","Red") to "#C62828",
                                neoLabel("أزرق","Blue") to "#1565C0",
                                neoLabel("أخضر","Green") to "#2E7D32",
                                neoLabel("ذهبي","Gold") to "#9B6A12",
                                neoLabel("بنفسجي","Purple") to "#7B1FA2",
                                neoLabel("أسود","Black") to "#222222"
                            ).forEach {(name,hex) ->
                                DropdownMenuItem(
                                    text={Text(name)},
                                    onClick={more=false;change(state.ink(hex))}
                                )
                            }
                            "direction" -> listOf(
                                "auto" to neoLabel("تلقائي","Automatic"),
                                "rtl" to neoLabel("من اليمين إلى اليسار","Right to left"),
                                "ltr" to neoLabel("من اليسار إلى اليمين","Left to right")
                            ).forEach {(value,label) ->
                                DropdownMenuItem(
                                    text={Text(label+if(direction==value) " ✓" else "")},
                                    onClick={more=false;onDirectionChange(value)}
                                )
                            }
                        }
                    }
                }
            }
        }

        if(expanded) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal=6.dp,vertical=2.dp),
                horizontalArrangement=Arrangement.spacedBy(4.dp),
                verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
            ) {
                NeoFormatAction(Icons.Rounded.FormatUnderlined,neoLabel("تسطير","Underline"),
                    selected=state.active("underline"),
                    action={change(state.toggle("underline"))})
                NeoFormatAction(Icons.Rounded.StrikethroughS,neoLabel("شطب","Strikethrough"),
                    selected=state.active("strike"),
                    action={change(state.toggle("strike"))})
                NeoFormatAction(Icons.Rounded.Redo,neoLabel("إعادة","Redo"),
                    enabled=canRedo,action=redo)
                Box {
                    NeoFormatAction(Icons.Rounded.FormatSize,neoLabel("حجم الخط","Font size"),
                        action={sizeOpen=true})
                    DropdownMenu(expanded=sizeOpen,onDismissRequest={sizeOpen=false}) {
                        listOf(14,16,18,20,24,28).forEach {points ->
                            DropdownMenuItem(text={Text("$points sp")},onClick={
                                sizeOpen=false
                                change(state.fontSize(points))
                            })
                        }
                    }
                }
                Box {
                    NeoFormatAction(Icons.Rounded.Palette,neoLabel("لون النص","Text color"),
                        action={inkOpen=true})
                    DropdownMenu(expanded=inkOpen,onDismissRequest={inkOpen=false}) {
                        listOf(
                            neoLabel("أحمر","Red") to "#C62828",
                            neoLabel("أزرق","Blue") to "#1565C0",
                            neoLabel("أخضر","Green") to "#2E7D32",
                            neoLabel("ذهبي","Gold") to "#9B6A12",
                            neoLabel("بنفسجي","Purple") to "#7B1FA2",
                            neoLabel("أسود","Black") to "#222222"
                        ).forEach {(name,hex) ->
                            DropdownMenuItem(text={Text(name)},onClick={
                                inkOpen=false
                                change(state.ink(hex))
                            })
                        }
                    }
                }
                Box {
                    NeoFormatAction(Icons.Rounded.SwapHoriz,neoLabel("اتجاه الكتابة","Text direction"),
                        selected=direction!="auto",action={directionOpen=true})
                    DropdownMenu(expanded=directionOpen,onDismissRequest={directionOpen=false}) {
                        listOf(
                            "auto" to neoLabel("تلقائي","Automatic"),
                            "rtl" to neoLabel("من اليمين إلى اليسار","Right to left"),
                            "ltr" to neoLabel("من اليسار إلى اليمين","Left to right")
                        ).forEach {(value,label) ->
                            DropdownMenuItem(
                                text={Text(label+if(direction==value) " ✓" else "")},
                                onClick={directionOpen=false;onDirectionChange(value)}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeoFormatAction(
    icon:androidx.compose.ui.graphics.vector.ImageVector,
    description:String,
    modifier:Modifier=Modifier.size(44.dp),
    selected:Boolean=false,
    enabled:Boolean=true,
    badge:String?=null,
    iconTint:Color?=null,
    action:()->Unit
) {
    val selectionDescription=if(selected) neoLabel("مفعّل","On") else neoLabel("غير مفعّل","Off")
    Surface(
        onClick=action,
        enabled=enabled,
        modifier=modifier.height(44.dp).semantics {
            contentDescription=description
            stateDescription=selectionDescription
        },
        shape=androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color=if(selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.55f),
        border=if(selected) androidx.compose.foundation.BorderStroke(
            1.dp,MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal=5.dp),
            horizontalArrangement=Arrangement.Center,
            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector=icon,
                contentDescription=null,
                modifier=Modifier.size(22.dp),
                tint=iconTint ?: if(selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=if(enabled) 1f else 0.35f)
            )
            if(badge!=null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    badge,fontSize=12.sp,
                    color=if(selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Rounded.ArrowDropDown,
                    contentDescription=null,
                    modifier=Modifier.size(14.dp)
                )
            }
        }
    }
}

private data class NeoEditorCheckboxHit(val index:Int,val x:Int,val y:Int,val checked:Boolean)

@Composable
internal fun NeoRuledNoteEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textDirection: androidx.compose.ui.text.style.TextDirection,
    focusRequester: androidx.compose.ui.focus.FocusRequester,
    modifier: Modifier = Modifier,
    styles:List<NeoRichCharStyle> = emptyList(),
    onChecklistToggle: ((Int)->Unit)?=null,
    ruled:Boolean=true,
    arabicFont:String="system",
    englishFont:String="system"
) {
    val lineColor=MaterialTheme.colorScheme.outline.copy(alpha=0.33f)
    val cursorColor=MaterialTheme.colorScheme.primary
    val density=androidx.compose.ui.platform.LocalDensity.current
    val rowHeight=if(ruled) 42.sp else 32.sp
    val spacing=with(density) {rowHeight.toPx()}
    // Use the measured text-line bottom directly; a fixed positive offset
    // makes the rule appear below the baseline and drifts across font scales.
    val guideGap=with(density) {0.dp.toPx()}
    var bottoms by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<List<Float>>(emptyList()) }
    var checkboxHits by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<List<NeoEditorCheckboxHit>>(emptyList())
    }
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val pageHeight=maxHeight
        val density=androidx.compose.ui.platform.LocalDensity.current
        val visibleHeight=with(density){pageHeight.toPx()}
        val viewportScroll=rememberScrollState()
        var cursorTop by androidx.compose.runtime.remember {androidx.compose.runtime.mutableFloatStateOf(0f)}
        var cursorBottom by androidx.compose.runtime.remember {androidx.compose.runtime.mutableFloatStateOf(0f)}
        val bottomMargin=with(density){120.dp.toPx()}
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
        // BasicTextField uses its parent's layout direction for the editable
        // text layout. An RTL TextStyle by itself does not anchor its internal
        // text box to the right edge of the full-width ruled paper.
        val writingFromRight=textDirection!=androidx.compose.ui.text.style.TextDirection.Ltr
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalLayoutDirection provides
                if(writingFromRight) androidx.compose.ui.unit.LayoutDirection.Rtl
                else androidx.compose.ui.unit.LayoutDirection.Ltr
        ) {
        Column(Modifier.fillMaxSize().verticalScroll(viewportScroll).padding(horizontal=16.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value=value,
                onValueChange=onValueChange,
                modifier=Modifier.fillMaxWidth().heightIn(min=pageHeight)
                    .testTag("notes_body_input").focusRequester(focusRequester).drawBehind {
                        if(ruled) {
                        val first=bottoms.ifEmpty {listOf(spacing)}
                        first.forEach{y->
                            drawLine(lineColor,androidx.compose.ui.geometry.Offset(0f,y+guideGap),
                                androidx.compose.ui.geometry.Offset(size.width,y+guideGap),strokeWidth=0.7.dp.toPx())
                        }
                        var y=first.last()+spacing
                        while(y<size.height) {
                            drawLine(lineColor,androidx.compose.ui.geometry.Offset(0f,y+guideGap),
                                androidx.compose.ui.geometry.Offset(size.width,y+guideGap),strokeWidth=0.7.dp.toPx())
                            y+=spacing
                        }
                        }
                    },
                visualTransformation=NeoNoteTypefaces.richVisual(styles,arabicFont,englishFont),
                textStyle=MaterialTheme.typography.bodyLarge.copy(
                    color=MaterialTheme.colorScheme.onSurface,
                    lineHeight=rowHeight,textDirection=textDirection,
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
                    if(onChecklistToggle!=null && value.composition==null) {
                        val positions=mutableListOf<NeoEditorCheckboxHit>()
                        var textOffset=0
                        value.text.split('\n').forEachIndexed {index,line ->
                            val prefix=NeoSmartLists.parse(line)
                            if(prefix?.kind=="check" && textOffset<value.text.length) {
                                val markerRect=result.getBoundingBox(textOffset)
                                positions+=NeoEditorCheckboxHit(
                                    index,markerRect.left.toInt(),markerRect.top.toInt(),prefix.checked
                                )
                            }
                            textOffset+=line.length+1
                        }
                        if(checkboxHits!=positions) checkboxHits=positions
                    } else if(checkboxHits.isNotEmpty()) checkboxHits=emptyList()
                },
                decorationBox={inner->
                    // The inner text box must occupy the entire page width.
                    // Otherwise short Arabic lines appear left/centered and jump
                    // as the line grows, despite TextAlign.Right in TextStyle.
                    Box(
                        modifier=Modifier.fillMaxWidth(),
                        contentAlignment=if(writingFromRight)
                            androidx.compose.ui.Alignment.TopEnd
                        else androidx.compose.ui.Alignment.TopStart
                    ) {
                        if(value.text.isEmpty()) Text(
                            neoLabel("اكتب ملاحظتك هنا…","Write your note here…"),
                            modifier=Modifier.fillMaxWidth(),
                            color=MaterialTheme.colorScheme.onSurfaceVariant,
                            style=MaterialTheme.typography.bodyLarge.copy(lineHeight=rowHeight,
                                textDirection=textDirection,
                                textAlign=if(textDirection==androidx.compose.ui.text.style.TextDirection.Ltr)
                                    androidx.compose.ui.text.style.TextAlign.Left
                                else androidx.compose.ui.text.style.TextAlign.Right)
                        )
                        // Ensure the actual editable text takes the page width, not its
                        // intrinsic glyph width. Short RTL lines must not slide as they grow.
                        androidx.compose.ui.layout.Layout(
                            modifier=Modifier.fillMaxWidth(),
                            content={inner()}
                        ) { children, limits ->
                            val width=limits.maxWidth
                            val child=children.single().measure(
                                limits.copy(minWidth=width,maxWidth=width)
                            )
                            layout(width,child.height) {child.place(0,0)}
                        }
                        // The clickable Material checkbox is painted at the
                        // text layout's actual glyph coordinates. No alternate
                        // TextField is created: Arabic IME and caret stay intact.
                        if(onChecklistToggle!=null) checkboxHits.forEach {hit ->
                            Box(
                                Modifier.offset {
                                    androidx.compose.ui.unit.IntOffset(hit.x,hit.y)
                                }.size(23.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface,
                                        androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                                    )
                                    .clickable {onChecklistToggle.invoke(hit.index)},
                                contentAlignment=androidx.compose.ui.Alignment.Center
                            ) {
                                Icon(
                                    if(hit.checked) Icons.Rounded.CheckBox
                                    else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription=if(hit.checked)
                                        neoLabel("إلغاء تحديد البند","Uncheck item")
                                    else neoLabel("تحديد البند","Check item"),
                                    modifier=Modifier.size(21.dp),
                                    tint=MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(196.dp))
        }
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
                // Preview text has no extra space after its last line; keep the
                // guide inside the paragraph bounds to prevent clipping.
                val y=(bottom-2.dp.toPx()).coerceAtLeast(0f)
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
    ruled: Boolean = false,
    arabicFont:String="system",
    englishFont:String="system"
) {
    val paraLineHeight=if(ruled) 42.sp else 32.sp
    val lineStyle=MaterialTheme.typography.bodyLarge.copy(
        lineHeight=paraLineHeight,textDirection=textDirection,
        textAlign=if(textDirection==androidx.compose.ui.text.style.TextDirection.Ltr)
            androidx.compose.ui.text.style.TextAlign.Left
        else androidx.compose.ui.text.style.TextAlign.Right)
    Column(verticalArrangement=Arrangement.spacedBy(0.dp)) {
        text.lines().forEachIndexed { index, rawLine ->
            val raw=noteDisplayText(rawLine)
            val rendered=NeoNoteTypefaces.styled(noteInline(raw),arabicFont,englishFont)
            // Parse the visible, formatted content: stored <b>/<i>/font wrappers
            // must never break checklists or expose their markup in reading mode.
            // Both legacy Markdown and the new in-editor checkbox glyph
            // render as interactive Material checkboxes in reading mode.
            val checked=Regex("""^\s*(?:- \[([ xX])\] |([☐☑]) )(.*)$""").matchEntire(rendered.text)
            if(checked!=null) {
                val done=if(checked.groupValues[1].isNotEmpty())
                    checked.groupValues[1]!=" " else checked.groupValues[2]=="☑"
                val label=checked.groupValues[3]
                val start=checked.groups[3]?.range?.first ?: rendered.length
                val styled=rendered.subSequence(start,rendered.length)
                val checklistLabel=if(done) buildAnnotatedString {
                    withStyle(SpanStyle(
                        textDecoration=androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )) {append(styled)}
                } else styled
                if(onChecklist==null && onCreateTask==null) {
                    NeoRuledParagraph(
                        buildAnnotatedString {
                            append(if(done) "☑ " else "☐ ")
                            append(checklistLabel)
                        },lineStyle,ruled=ruled
                    )
                } else {
                    androidx.compose.runtime.CompositionLocalProvider(
                        androidx.compose.ui.platform.LocalLayoutDirection provides
                            if(textDirection==androidx.compose.ui.text.style.TextDirection.Ltr)
                                androidx.compose.ui.unit.LayoutDirection.Ltr
                            else androidx.compose.ui.unit.LayoutDirection.Rtl
                    ) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.Top) {
                        Checkbox(
                            checked=done,
                            onCheckedChange=onChecklist?.let{callback->{_:Boolean->callback(index)}},
                            enabled=onChecklist!=null
                        )
                        NeoRuledParagraph(checklistLabel,lineStyle,Modifier.weight(1f).padding(top=6.dp),ruled)
                        if(onCreateTask!=null) TextButton(onClick={onCreateTask(index,label)}) {
                            Text(if(rawLine.contains("<!--task:")) neoLabel("فتح المهمة","Open task")
                                else neoLabel("إلى مهمة","To task"))
                        }
                    }
                    }
                }
            } else if(!checklistsOnly) {
                val heading=raw.startsWith("# ")||raw.startsWith("## ")||raw.startsWith("### ")
                val styled=when {
                    rendered.text.startsWith("- ") ->
                        buildAnnotatedString {append("• ");append(rendered.subSequence(2,rendered.length))}
                    rendered.text.matches(Regex("""^[0-9]+[.)] .*""")) ||
                        rendered.text.matches(Regex("""^[A-Z]+[.)] .*""")) ||
                        rendered.text.matches(Regex("""^[أابتثجحخدذرزسشصضطظعغفقكلمنهوي][.)] .*""")) ->
                        rendered
                    else -> rendered
                }
                val type=when {
                    raw.startsWith("# ") -> MaterialTheme.typography.titleLarge
                    raw.startsWith("## ") -> MaterialTheme.typography.titleMedium
                    raw.startsWith("### ") -> MaterialTheme.typography.titleSmall
                    else -> MaterialTheme.typography.bodyLarge
                }
                NeoRuledParagraph(
                    if(styled.isEmpty()) AnnotatedString(" ") else styled,
                    type.copy(lineHeight=paraLineHeight,textDirection=textDirection),ruled=ruled
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
        Regex("""<font color="(#[0-9A-Fa-f]{6})">|</font>|<size sp="([0-9]{1,3})">|</size>|<face key="(amiri|tajawal|reem|montserrat|lora|oswald|sans|serif|mono)">|</face>|<b>|</b>|<i>|</i>|<u>|</u>|<s>|</s>""").findAll(source).forEach { token ->
            val raw=token.value
            val kind=when {raw.startsWith("<font ")||raw=="</font>" -> "font";raw.startsWith("<size ")||raw=="</size>" -> "size";raw.startsWith("<face ")||raw=="</face>" -> "face";raw=="<b>"||raw=="</b>" -> "b";raw=="<i>"||raw=="</i>" -> "i";raw=="<u>"||raw=="</u>" -> "u";else -> "s"}
            if(!raw.startsWith("</")) {
                val style=when(kind) {
                    "font" -> SpanStyle(color=Color(android.graphics.Color.parseColor(token.groupValues[1])))
                    "size" -> SpanStyle(fontSize=(token.groupValues[2].toIntOrNull() ?: 16).coerceIn(8,72).sp)
                    "face" -> SpanStyle(fontFamily=NeoNoteTypefaces.family(token.groupValues[3]))
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
