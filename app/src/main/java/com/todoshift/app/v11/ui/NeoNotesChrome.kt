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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Chrome only: v56 keeps the v55 note document, draft, history, and attachment formats. */
@Composable
internal fun NeoNoteQuickBar(
    state: NeoRichEditorState,
    onChange: (NeoRichEditorState) -> Unit,
    onFormat: () -> Unit
) {
    var inkOpen by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier=Modifier.fillMaxWidth(),
            tonalElevation=2.dp,
            color=MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal=8.dp,vertical=4.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ) {
                NeoChromeIcon(Icons.Rounded.FormatBold,neoLabel("عريض","Bold"),
                    state.active("bold")){onChange(state.toggle("bold"))}
                NeoChromeIcon(Icons.Rounded.FormatItalic,neoLabel("مائل","Italic"),
                    state.active("italic")){onChange(state.toggle("italic"))}
                NeoChromeIcon(Icons.Rounded.FormatUnderlined,neoLabel("تسطير النص","Underline text"),
                    state.active("underline")){onChange(state.toggle("underline"))}
                Box {
                    NeoChromeIcon(Icons.Rounded.FormatColorText,neoLabel("لون النص","Text color")) {
                        inkOpen=true
                    }
                    DropdownMenu(expanded=inkOpen,onDismissRequest={inkOpen=false}) {
                        neoInkChoices().forEach {(name,hex) ->
                            DropdownMenuItem(text={Text(name)},onClick={
                                inkOpen=false;onChange(state.ink(hex))
                            })
                        }
                    }
                }
                VerticalDivider(Modifier.height(28.dp),thickness=1.dp)
                NeoChromeIcon(Icons.Rounded.FormatListBulleted,neoLabel("قائمة نقطية","Bullet list"),
                    state.listActive("bullet")){onChange(state.list("bullet"))}
                NeoChromeIcon(Icons.Rounded.CheckBox,neoLabel("قائمة اختيار","Checklist"),
                    state.listActive("check")){onChange(state.list("check"))}
                FilledTonalButton(onClick=onFormat,contentPadding=PaddingValues(horizontal=12.dp)) {
                    Icon(Icons.Rounded.FormatSize,null,Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(neoLabel("تنسيق","Format"))
                }
            }
        }
    }
}

@Composable
private fun NeoChromeIcon(
    icon:ImageVector,description:String,selected:Boolean=false,onClick:()->Unit
) {
    Surface(
        onClick=onClick,modifier=Modifier.size(42.dp),
        shape=RoundedCornerShape(12.dp),
        color=if(selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        border=if(selected) BorderStroke(1.dp,MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment=Alignment.Center) {
            Icon(icon,contentDescription=description,modifier=Modifier.size(21.dp))
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
    return state.input(TextFieldValue(updated,
        TextRange((from+replacement.length).coerceIn(0,updated.length))))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NeoNoteFormatSheet(
    state:NeoRichEditorState,
    onChange:(NeoRichEditorState)->Unit,
    direction:String,
    onDirectionChange:(String)->Unit,
    ruledPaper:Boolean,
    onRulingChange:(Boolean)->Unit,
    onDefaultRuling:()->Unit,
    onNoteAppearance:()->Unit,
    onPageColor:()->Unit,
    onDismiss:()->Unit
) {
    var tab by remember {mutableStateOf(0)}
    var fontMenu by remember {mutableStateOf(false)}
    val currentSize=state.effective().size ?: 16
    val sizes=listOf(14,16,18,20,24,28)
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
                    Text(neoLabel("نمط النص","Text style"),
                        style=MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(
                            0 to neoLabel("عادي","Normal"),
                            1 to "H1",2 to "H2",3 to "H3"
                        ).forEach {(level,label) ->
                            AssistChip(onClick={onChange(neoNoteHeading(state,level))},
                                label={Text(label)})
                        }
                    }
                    Text(neoLabel("الخط للنص المحدد","Selected text font"),
                        style=MaterialTheme.typography.titleSmall)
                    Box {
                        OutlinedButton(onClick={fontMenu=true}) {
                            Text("Aa  "+(state.effective().face ?: neoLabel("الافتراضي","Default")))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.ArrowDropDown,null)
                        }
                        DropdownMenu(expanded=fontMenu,onDismissRequest={fontMenu=false}) {
                            (listOf(null to neoLabel("الافتراضي","Default"))+
                                listOf(
                                    "amiri" to "Amiri","tajawal" to "Tajawal",
                                    "reem" to "Reem Kufi","montserrat" to "Montserrat",
                                    "lora" to "Lora","oswald" to "Oswald"
                                )).forEach {(key,label) ->
                                DropdownMenuItem(text={Text(label)},onClick={
                                    fontMenu=false;onChange(state.face(key))
                                })
                            }
                        }
                    }
                    Row(verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(neoLabel("حجم الخط","Font size"),modifier=Modifier.weight(1f))
                        IconButton(onClick={
                            val next=sizes.lastOrNull{it<currentSize} ?: sizes.first()
                            onChange(state.fontSize(next))
                        }) {Icon(Icons.Rounded.Remove,neoLabel("تصغير","Decrease"))}
                        Text(currentSize.toString())
                        IconButton(onClick={
                            val next=sizes.firstOrNull{it>currentSize} ?: sizes.last()
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
                    Text(neoLabel("قوائم","Lists"),style=MaterialTheme.typography.titleSmall)
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "bullet" to neoLabel("نقاط","Bullets"),
                            "circle" to neoLabel("دوائر","Circles"),
                            "square" to neoLabel("مربعات","Squares"),
                            "check" to neoLabel("قائمة اختيار","Checklist"),
                            "number" to neoLabel("أرقام","Numbers"),
                            "numberParen" to neoLabel("أرقام بأقواس","Numbered parentheses"),
                            "letter" to neoLabel("حروف لاتينية","Latin letters"),
                            "letterParen" to neoLabel("حروف بأقواس","Letter parentheses"),
                            "arabic" to neoLabel("حروف عربية","Arabic letters")
                        ).chunked(3).forEach {row ->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                row.forEach {(key,label) ->
                                    FilterChip(
                                        selected=state.listActive(key),
                                        onClick={onChange(state.list(key))},
                                        label={Text(label,maxLines=1)},
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
