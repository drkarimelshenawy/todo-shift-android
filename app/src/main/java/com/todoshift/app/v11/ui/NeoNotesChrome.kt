package com.todoshift.app.v11.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Chrome only: v56 keeps the v55 note document, draft, history, and attachment formats. */
@Composable
internal fun NeoNoteQuickBar(
    state: NeoRichEditorState,
    onChange: (NeoRichEditorState) -> Unit,
    onFormat: () -> Unit,
    enabled:Boolean=true
) {
    var inkOpen by remember {mutableStateOf(false)}
    var listsOpen by remember {mutableStateOf(false)}
    var selectedList by rememberSaveable {mutableStateOf("bullet")}
    val kinds=listOf(
        Triple("bullet",Icons.Rounded.FormatListBulleted,neoLabel("نقاط","Bullets")),
        Triple("number",Icons.Rounded.FormatListNumbered,neoLabel("أرقام","Numbers")),
        Triple("letter",Icons.Rounded.SortByAlpha,neoLabel("حروف","Letters")),
        Triple("check",Icons.Rounded.CheckBox,neoLabel("قائمة مهام","Checklist"))
    )
    val active=kinds.firstOrNull{state.listActive(it.first)}?.first
    val shown=active ?: selectedList
    val shownIcon=kinds.first{it.first==shown}.second
    val currentSize=(state.effective().size ?: 16).coerceIn(10,72)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier=Modifier.fillMaxWidth().testTag("notes_quick_bar"),
            tonalElevation=2.dp,
            color=MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal=4.dp,vertical=5.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(2.dp)
            ) {
                NeoChromeIcon(Icons.Rounded.FormatBold,neoLabel("عريض","Bold"),
                    state.active("bold"),enabled=enabled){onChange(state.toggle("bold"))}
                NeoChromeIcon(Icons.Rounded.FormatItalic,neoLabel("مائل","Italic"),
                    state.active("italic"),enabled=enabled){onChange(state.toggle("italic"))}
                NeoChromeIcon(Icons.Rounded.FormatUnderlined,neoLabel("تسطير النص","Underline text"),
                    state.active("underline"),enabled=enabled){onChange(state.toggle("underline"))}
                Box {
                    NeoChromeIcon(Icons.Rounded.FormatColorText,
                        neoLabel("اختيار لون الخط","Text color"),enabled=enabled) {
                        inkOpen=true
                    }
                    NeoLocalizedMenu(expanded=inkOpen,onDismissRequest={inkOpen=false}) {
                        Column(Modifier.padding(10.dp),
                            verticalArrangement=Arrangement.spacedBy(10.dp)) {
                            neoInkChoices().chunked(3).forEach {colors ->
                                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                    colors.forEach {(name,hex) ->
                                        val selected=state.effective().color==hex
                                        Surface(
                                            onClick={inkOpen=false;onChange(state.ink(hex))},
                                            enabled=enabled,
                                            shape=androidx.compose.foundation.shape.CircleShape,
                                            modifier=Modifier.size(34.dp).semantics {
                                                contentDescription=name
                                            },
                                            color=Color(android.graphics.Color.parseColor(hex)),
                                            border=BorderStroke(if(selected) 3.dp else 1.dp,
                                                if(selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline)
                                        ) {
                                            if(selected) Box(contentAlignment=Alignment.Center) {
                                                Icon(Icons.Rounded.Check,null,Modifier.size(17.dp),
                                                    tint=if(hex=="#222222") Color.White else Color.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                VerticalDivider(Modifier.height(25.dp),thickness=1.dp)
                Box {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        NeoChromeIcon(shownIcon,
                            neoLabel("تفعيل نوع القائمة","Toggle selected list"),
                            selected=active==shown,enabled=enabled) {
                            onChange(state.list(shown))
                        }
                        IconButton(
                            onClick={listsOpen=true},enabled=enabled,
                            modifier=Modifier.size(23.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowDropDown,
                                neoLabel("اختيار نوع القائمة","Choose list type"),
                                modifier=Modifier.size(19.dp))
                        }
                    }
                    NeoLocalizedMenu(expanded=listsOpen,onDismissRequest={listsOpen=false}) {
                        kinds.forEach {(kind,icon,label) ->
                            DropdownMenuItem(
                                text={Text(label)},leadingIcon={Icon(icon,null)},
                                onClick={
                                    listsOpen=false
                                    selectedList=kind
                                    if(active!=null && active!=kind)
                                        onChange(state.list(kind))
                                }
                            )
                        }
                    }
                }
                VerticalDivider(Modifier.height(25.dp),thickness=1.dp)
                IconButton(
                    onClick={onChange(state.fontSize((currentSize-1).coerceAtLeast(10)))},
                    enabled=enabled && currentSize>10,
                    modifier=Modifier.size(28.dp).testTag("notes_font_smaller")
                ) {
                    Icon(Icons.Rounded.Remove,
                        neoLabel("تصغير الخط","Smaller text"),Modifier.size(19.dp))
                }
                Text(currentSize.toString(),
                    modifier=Modifier.widthIn(min=20.dp),
                    textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                    style=MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick={onChange(state.fontSize((currentSize+1).coerceAtMost(72)))},
                    enabled=enabled && currentSize<72,
                    modifier=Modifier.size(28.dp).testTag("notes_font_larger")
                ) {
                    Icon(Icons.Rounded.Add,
                        neoLabel("تكبير الخط","Larger text"),Modifier.size(19.dp))
                }
                NeoChromeIcon(Icons.Rounded.FormatSize,
                    neoLabel("لوحة التنسيق","Format panel")) {onFormat()}
            }
        }
    }
}

@Composable
private fun NeoChromeIcon(
    icon:ImageVector,description:String,selected:Boolean=false,enabled:Boolean=true,onClick:()->Unit
) {
    Surface(
        onClick=onClick,enabled=enabled,modifier=Modifier.size(35.dp),
        shape=RoundedCornerShape(10.dp),
        color=if(selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        border=if(selected) BorderStroke(1.dp,MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment=Alignment.Center) {
            Icon(icon,contentDescription=description,modifier=Modifier.size(20.dp))
        }
    }
}

@Composable
private fun neoInkChoices()=listOf(
    neoLabel("أسود","Black") to "#222222",
    neoLabel("أحمر","Red") to "#C62828",
    neoLabel("أزرق","Blue") to "#1565C0",
    neoLabel("أخضر","Green") to "#2E7D32",
    neoLabel("ذهبي","Gold") to "#9B6A12",
    neoLabel("بنفسجي","Purple") to "#7B1FA2"
)

/** Heading edit on the live visible buffer; does not rewrite attachment tokens on disk. */
internal fun neoNoteHeading(state:NeoRichEditorState,level:Int):NeoRichEditorState {
    require(level in 0..3)
    val text=state.value.text
    val selection=state.value.selection
    val from=text.lastIndexOf('\n',selection.min-1)+1
    val to=text.indexOf('\n',selection.max).takeIf{it>=0} ?: text.length
    val selected=text.substring(from,to)
    val replacement=selected.split('\n').joinToString("\n") {line ->
        val clean=line.replaceFirst(Regex("^#{1,3} "),"")
        if(level==0) clean else "#".repeat(level)+" "+clean
    }
    val updated=text.replaceRange(from,to,replacement)
    // An existing heading prefix is stored as a hidden structural token.
    // If the user changes its level, remove the old prefix before applying
    // the visible edit so headings are never doubled or moved after the line.
    val clean=state.copy(hidden=state.hidden.filterNot { token ->
        token.text.matches(Regex("^#{1,3} ")) && token.position in from..to
    })
    return clean.input(TextFieldValue(updated,
        TextRange((from+replacement.length).coerceIn(0,updated.length))))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NeoNoteFormatSheet(
    state:NeoRichEditorState,
    initialTab:Int=0,
    onChange:(NeoRichEditorState)->Unit,
    direction:String,
    onDirectionChange:(String)->Unit,
    ruledPaper:Boolean,
    onRulingChange:(Boolean)->Unit,
    onDefaultRuling:()->Unit,
    onNoteAppearance:()->Unit,
    onPageColor:()->Unit,
    pageBorderStyle:String=NeoPageBorders.NONE,
    onPageBorderChange:(String)->Unit={},
    onDismiss:()->Unit
) {
    var tab by remember(initialTab) {mutableIntStateOf(initialTab)}
    var fontMenu by remember {mutableStateOf(false)}
    val currentSize=(state.effective().size ?: 16).coerceIn(10,72)
    val sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheetState) {
        Column(
            Modifier.fillMaxWidth().heightIn(max=610.dp).verticalScroll(rememberScrollState())
                .padding(horizontal=18.dp,vertical=8.dp),
            verticalArrangement=Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(neoLabel("تنسيق","Format"),style=MaterialTheme.typography.titleLarge,
                    modifier=Modifier.weight(1f))
                IconButton(onClick=onDismiss) {
                    Icon(Icons.Rounded.Close,neoLabel("إغلاق","Close"))
                }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(
                    neoLabel("النص","Text"),
                    neoLabel("الفقرة","Paragraph"),
                    neoLabel("الصفحة","Page")
                ).forEachIndexed {index,label ->
                    FilterChip(selected=tab==index,onClick={tab=index},label={Text(label)})
                }
            }
            HorizontalDivider()
            when(tab) {
                0 -> {
                    Text(neoLabel("الخط للنص المحدد","Selected text font"),
                        style=MaterialTheme.typography.titleSmall)
                    Box {
                        OutlinedButton(onClick={fontMenu=true}) {
                            Text("Aa  "+when(state.effective().face) {
                                null -> neoLabel("الافتراضي","Default")
                                "sans" -> "Sans Serif"
                                "serif" -> "Serif"
                                "mono" -> "Monospace"
                                "amiri" -> "Amiri"
                                "tajawal" -> "Tajawal"
                                "reem" -> "Reem Kufi"
                                "montserrat" -> "Montserrat"
                                "lora" -> "Lora"
                                "oswald" -> "Oswald"
                                else -> neoLabel("الافتراضي","Default")
                            })
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.ArrowDropDown,null)
                        }
                        NeoLocalizedMenu(expanded=fontMenu,onDismissRequest={fontMenu=false}) {
                            (listOf(null to neoLabel("الافتراضي","Default"))+
                                listOf(
                                    "amiri" to "Amiri","tajawal" to "Tajawal",
                                    "reem" to "Reem Kufi","montserrat" to "Montserrat",
                                    "lora" to "Lora","oswald" to "Oswald",
                                    "sans" to neoLabel("بدون زوائد · Sans Serif","Sans Serif"),
                                    "serif" to neoLabel("بزوائد · Serif","Serif"),
                                    "mono" to neoLabel("أحادي العرض · Monospace","Monospace")
                                )).forEach {(key,label) ->
                                DropdownMenuItem(
                                    text={Text(label,style=MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily=key?.let(NeoNoteTypefaces::family)
                                    ))},
                                    onClick={fontMenu=false;onChange(state.face(key))}
                                )
                            }
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(neoLabel("حجم الخط","Font size"),modifier=Modifier.weight(1f))
                        IconButton(onClick={
                            val next=(currentSize-1).coerceAtLeast(10)
                            onChange(state.fontSize(next))
                        }) {Icon(Icons.Rounded.Remove,neoLabel("تصغير","Decrease"))}
                        Text(currentSize.toString())
                        IconButton(onClick={
                            val next=(currentSize+1).coerceAtMost(72)
                            onChange(state.fontSize(next))
                        }) {Icon(Icons.Rounded.Add,neoLabel("تكبير","Increase"))}
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple(Icons.Rounded.FormatBold,"bold",neoLabel("عريض","Bold")),
                            Triple(Icons.Rounded.FormatItalic,"italic",neoLabel("مائل","Italic")),
                            Triple(Icons.Rounded.FormatUnderlined,"underline",neoLabel("تسطير","Underline")),
                            Triple(Icons.Rounded.StrikethroughS,"strike",neoLabel("شطب","Strike"))
                        ).forEach {(icon,mark,label) ->
                            NeoChromeIcon(icon,label,state.active(mark)) {
                                onChange(state.toggle(mark))
                            }
                        }
                    }
                    Text(neoLabel("الكتابة الفوقية والتحتية","Superscript / Subscript"),
                        style=MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected=state.effective().script=="sup",
                            onClick={onChange(state.script("sup"))},
                            label={Text("X²")},
                            leadingIcon={Icon(Icons.Rounded.VerticalAlignTop,null)}
                        )
                        FilterChip(
                            selected=state.effective().script=="sub",
                            onClick={onChange(state.script("sub"))},
                            label={Text("X₂")},
                            leadingIcon={Icon(Icons.Rounded.VerticalAlignBottom,null)}
                        )
                    }
                    Text(neoLabel("تظليل النص المحدد","Text highlight"),
                        style=MaterialTheme.typography.titleSmall)
                    Text(
                        neoLabel("التظليل خلف الكلمات، وليس لون الورقة.",
                            "Highlight applies behind selected characters, not the page."),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        listOf("#FFF59D","#C8E6C9","#BBDEFB","#F8BBD0","#E1BEE7")
                            .forEach { hex ->
                                val selected=state.effective().highlight==hex
                                val description=neoLabel("تظليل", "Highlight")+" "+hex
                                Surface(
                                    onClick={onChange(state.highlight(hex))},
                                    modifier=Modifier.size(42.dp).semantics {
                                        contentDescription=description
                                    },
                                    color=Color(android.graphics.Color.parseColor(hex)),
                                    shape=RoundedCornerShape(12.dp),
                                    border=BorderStroke(if(selected) 3.dp else 1.dp,
                                        if(selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant)
                                ) {}
                            }
                        OutlinedButton(onClick={onChange(state.highlight(null))}) {
                            Text(neoLabel("بلا تظليل","Clear"))
                        }
                    }
                    Text(neoLabel("لون النص","Text color"),
                        style=MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        neoInkChoices().forEach {(label,hex) ->
                            val color=Color(android.graphics.Color.parseColor(hex))
                            Surface(
                                onClick={onChange(state.ink(hex))},
                                shape=RoundedCornerShape(50),
                                modifier=Modifier.size(38.dp),
                                color=color,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)
                            ) {}
                        }
                    }
                }
                1 -> {
                    Text(neoLabel("محاذاة الفقرة","Paragraph alignment"),
                        style=MaterialTheme.typography.titleSmall)
                    Text(
                        neoLabel("المحاذاة مستقلة عن اتجاه الكتابة.",
                            "Alignment is independent of writing direction."),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("left",Icons.Rounded.FormatAlignLeft,neoLabel("يسار","Left")),
                            Triple("center",Icons.Rounded.FormatAlignCenter,neoLabel("وسط","Center")),
                            Triple("right",Icons.Rounded.FormatAlignRight,neoLabel("يمين","Right")),
                            Triple("justify",Icons.Rounded.FormatAlignJustify,neoLabel("ضبط","Justify"))
                        ).forEach { (key,icon,label) ->
                            Surface(onClick={onChange(state.paragraphAlign(key))},
                                shape=RoundedCornerShape(10.dp),
                                color=if(state.currentParagraph().align==key)
                                    MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainerLow,
                                border=BorderStroke(1.dp,if(state.currentParagraph().align==key)
                                    MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant)) {
                                Row(Modifier.padding(horizontal=10.dp,vertical=9.dp),
                                    verticalAlignment=Alignment.CenterVertically,
                                    horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                                    Icon(icon,label,modifier=Modifier.size(19.dp))
                                    Text(label,style=MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(neoLabel("المسافة البادئة","Indent"),
                            modifier=Modifier.weight(1f),
                            style=MaterialTheme.typography.titleSmall)
                        NeoChromeIcon(
                            Icons.Rounded.FormatIndentDecrease,
                            neoLabel("تقليل المسافة البادئة","Decrease indent"),
                            enabled=state.currentParagraph().indent>0
                        ) {onChange(state.paragraphIndent(-1))}
                        Text(state.currentParagraph().indent.toString())
                        NeoChromeIcon(
                            Icons.Rounded.FormatIndentIncrease,
                            neoLabel("زيادة المسافة البادئة","Increase indent"),
                            enabled=state.currentParagraph().indent<4
                        ) {onChange(state.paragraphIndent(1))}
                    }
                    HorizontalDivider()
                    Text(neoLabel("قوائم","Lists"),style=MaterialTheme.typography.titleSmall)
                    Text(
                        neoLabel("اختر الشكل من معاينة القائمة التي ستظهر في الفقرة.",
                            "Choose a visual preview of the list to apply to this paragraph."),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        neoNoteListKinds.map { kind -> kind to when(kind) {
                            "bullet" -> neoLabel("نقاط","Bullets")
                            "circle" -> neoLabel("دوائر","Circles")
                            "square" -> neoLabel("مربعات","Squares")
                            "check" -> neoLabel("قائمة اختيار","Checklist")
                            "number" -> neoLabel("أرقام","Numbers")
                            "numberParen" -> neoLabel("أرقام بأقواس","Number parentheses")
                            "letter" -> neoLabel("حروف لاتينية","Latin letters")
                            "letterParen" -> neoLabel("حروف بأقواس","Letter parentheses")
                            else -> neoLabel("حروف عربية","Arabic letters")
                        } }.chunked(3).forEach { row ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                row.forEach { (kind,label) ->
                                    NeoNoteListStyleTile(
                                        kind=kind,label=label,
                                        selected=state.listActive(kind),
                                        onClick={onChange(state.list(kind))},
                                        modifier=Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Text(neoLabel("اتجاه الكتابة","Text direction"),
                        style=MaterialTheme.typography.titleSmall)
                    listOf(
                        "auto" to neoLabel("تلقائي","Automatic"),
                        "rtl" to neoLabel("يمين ← يسار","Right to left"),
                        "ltr" to neoLabel("يسار ← يمين","Left to right")
                    ).forEach {(value,label) ->
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                            RadioButton(selected=direction==value,
                                onClick={onDirectionChange(value)})
                            Text(label)
                        }
                    }
                }
                else -> {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(neoLabel("الورق المسطّر","Ruled paper"),
                            modifier=Modifier.weight(1f))
                        Switch(checked=ruledPaper,onCheckedChange=onRulingChange)
                    }
                    TextButton(onClick=onDefaultRuling) {
                        Text(neoLabel("استخدام إعداد التسطير الافتراضي","Use default ruling"))
                    }
                    Text(neoLabel("حدود الصفحة • تخطيط الصفحة", "Page borders • Page Layout"),
                        style=MaterialTheme.typography.titleSmall)
                    Text(neoLabel("إطارات خارج هوامش الكتابة والعنوان، وتظهر في A4 وPDF وWord.",
                        "Borders stay outside the title and text margins, in A4, PDF and Word."),
                        style=MaterialTheme.typography.bodySmall)
                    Text(
                        neoLabel("معاينة مصغّرة لحدود الصفحة قبل تطبيقها على المستند.",
                            "See the border's appearance on a page before applying it."),
                        style=MaterialTheme.typography.bodySmall,
                        color=MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    neoNotePageBorderChoices.map { style -> style to when(style) {
                        NeoPageBorders.NONE -> neoLabel("بدون إطار","No border")
                        NeoPageBorders.SINGLE -> neoLabel("خط مفرد","Single")
                        NeoPageBorders.DOUBLE -> neoLabel("خط مزدوج","Double")
                        NeoPageBorders.DASHED -> neoLabel("خط متقطع","Dashed")
                        else -> neoLabel("خط منقّط","Dotted")
                    } }.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            row.forEach { (style,label) ->
                                NeoNotePageBorderTile(
                                    style=style,label=label,
                                    selected=pageBorderStyle==style,
                                    onClick={onPageBorderChange(style)},
                                    modifier=Modifier.weight(1f)
                                )
                            }
                            repeat(3-row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    OutlinedButton(onClick=onPageColor,modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Palette,null)
                        Spacer(Modifier.width(8.dp))
                        Text(neoLabel("لون الصفحة","Page color"))
                    }
                    OutlinedButton(onClick=onNoteAppearance,modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.TextFields,null)
                        Spacer(Modifier.width(8.dp))
                        Text(neoLabel("خطوط الملاحظة ومظهرها","Note appearance and fonts"))
                    }
                    Text(neoLabel("إعدادات الصفحة لا تغيّر محتوى الملاحظة.",
                        "Page appearance does not change note content."),
                        style=MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}


/**
 * Secondary settings live in a grouped, scrollable sheet rather than in an
 * oversized nested popup. No note feature is removed by this presentation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NeoNoteSettingsSheet(
    pinned:Boolean,
    locked:Boolean,
    previewHidden:Boolean,
    a4Preview:Boolean,
    onOutline:()->Unit,
    onA4:()->Unit,
    onAttachments:()->Unit,
    onRulingFonts:()->Unit,
    onPageColor:()->Unit,
    onPreviewHidden:()->Unit,
    onPinned:()->Unit,
    onLocked:()->Unit,
    onLinks:()->Unit,
    onDismiss:()->Unit
) {
    ModalBottomSheet(
        onDismissRequest=onDismiss,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max=590.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal=16.dp,vertical=6.dp),
            verticalArrangement=Arrangement.spacedBy(3.dp)
        ) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(neoLabel("إعدادات الملاحظة","Note settings"),
                    style=MaterialTheme.typography.titleLarge,
                    modifier=Modifier.weight(1f))
                IconButton(onClick=onDismiss) {
                    Icon(Icons.Rounded.Close,neoLabel("إغلاق","Close"))
                }
            }
            Text(neoLabel("المستند","Document"),
                style=MaterialTheme.typography.titleSmall,
                color=MaterialTheme.colorScheme.primary,
                modifier=Modifier.padding(top=8.dp,bottom=2.dp))
            NeoSettingsAction(Icons.Rounded.FormatListBulleted,
                neoLabel("فهرس العناوين","Document outline"),onOutline)
            NeoSettingsAction(Icons.Rounded.Description,
                if(a4Preview) neoLabel("العودة للكتابة","Back to writing")
                else neoLabel("معاينة A4","A4 reading preview"),onA4)
            NeoSettingsAction(Icons.Rounded.AttachFile,
                neoLabel("الصور والمرفقات","Images and attachments"),onAttachments)
            HorizontalDivider(Modifier.padding(vertical=6.dp))
            Text(neoLabel("مظهر الصفحة","Page appearance"),
                style=MaterialTheme.typography.titleSmall,
                color=MaterialTheme.colorScheme.primary)
            NeoSettingsAction(Icons.Rounded.ViewHeadline,
                neoLabel("الورق المسطّر والخطوط","Paper ruling and fonts"),onRulingFonts)
            NeoSettingsAction(Icons.Rounded.Palette,
                neoLabel("لون الصفحة","Page color"),onPageColor)
            NeoSettingsAction(Icons.Rounded.Visibility,
                if(previewHidden) neoLabel("إظهار معاينة بطاقة الملاحظة","Show note card preview")
                else neoLabel("إخفاء معاينة بطاقة الملاحظة","Hide note card preview"),
                onPreviewHidden)
            HorizontalDivider(Modifier.padding(vertical=6.dp))
            Text(neoLabel("تنظيم وحماية","Organization and protection"),
                style=MaterialTheme.typography.titleSmall,
                color=MaterialTheme.colorScheme.primary)
            NeoSettingsAction(Icons.Rounded.PushPin,
                if(pinned) neoLabel("إلغاء التثبيت","Unpin")
                else neoLabel("تثبيت الملاحظة","Pin note"),onPinned)
            NeoSettingsAction(Icons.Rounded.Lock,
                if(locked) neoLabel("إلغاء قفل الملاحظة","Unlock note")
                else neoLabel("قفل الملاحظة","Lock note"),onLocked)
            NeoSettingsAction(Icons.Rounded.Link,
                neoLabel("الربط بالتقويم والمهام","Calendar and task links"),onLinks)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NeoSettingsAction(
    icon:ImageVector,
    label:String,
    onClick:()->Unit
) {
    Surface(
        onClick=onClick,
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(10.dp),
        color=Color.Transparent
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min=44.dp)
                .padding(horizontal=9.dp,vertical=6.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon,contentDescription=null,
                modifier=Modifier.size(19.dp),
                tint=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label,style=MaterialTheme.typography.bodyMedium)
        }
    }
}
