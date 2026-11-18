package com.todoshift.app.v11.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.NeoAttachmentStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Document blocks are rendered in their original paragraph order, not in a
 * separate attachments shelf. Editing a table or inline image changes the
 * portable source of the same note and participates in draft/autosave/history.
 */
@Composable
internal fun NeoDocumentCanvas(
    noteId:String,
    source:String,
    editable:Boolean,
    documentTitle:String="",
    onSourceChange:(String)->Unit,
    onError:(String)->Unit,
    onChecklist:((Int)->Unit)?=null,
    textDirection:TextDirection=TextDirection.ContentOrRtl,
    ruled:Boolean=false,
    arabicFont:String="system",
    englishFont:String="system",
    a4:Boolean=false,
    inlineEditing:Boolean=false,
    onParagraphEdited:((Int,String)->Unit)?=null,
    onActiveParagraph:((NeoRichEditorState,(NeoRichEditorState)->Unit)->Unit)?=null
) {
    val blocks=remember(source){NeoDocumentBlocks.parse(source)}
    val pages=remember(blocks) {
        val result=mutableListOf<MutableList<NeoDocumentBlocks.Block>>(mutableListOf())
        blocks.forEach {block ->
            if(block is NeoDocumentBlocks.Block.PageBreak) result.add(mutableListOf())
            else result.last().add(block)
        }
        // Match PDF export: a terminal page-break does not create a blank
        // final page, but intentional blank pages in the middle remain.
        if(result.size>1 && result.last().isEmpty()) result.removeAt(result.lastIndex)
        result
    }
    val background=if(a4) Color.White else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor=if(a4) Color(0xFF202126) else MaterialTheme.colorScheme.onSurface
    var zoomPercent by remember(noteId,a4) { mutableIntStateOf(100) }
    Column(verticalArrangement=Arrangement.spacedBy(if(a4) 12.dp else 2.dp)) {
        if(a4) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,
                verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick={zoomPercent=(zoomPercent-10).coerceAtLeast(70)},
                    enabled=zoomPercent>70) {
                    Icon(Icons.Rounded.ZoomOut,contentDescription=neoLabel("تصغير الصفحة","Zoom out"))
                }
                Text("$zoomPercent%",style=MaterialTheme.typography.labelLarge)
                IconButton(onClick={zoomPercent=(zoomPercent+10).coerceAtMost(160)},
                    enabled=zoomPercent<160) {
                    Icon(Icons.Rounded.ZoomIn,contentDescription=neoLabel("تكبير الصفحة","Zoom in"))
                }
                TextButton(onClick={zoomPercent=100}) {Text(neoLabel("إعادة الضبط","Reset"))}
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
        val viewportWidth=maxWidth
        Column(Modifier.fillMaxWidth().then(if(a4) Modifier.horizontalScroll(rememberScrollState())
            else Modifier),verticalArrangement=Arrangement.spacedBy(if(a4) 12.dp else 2.dp)) {
        pages.forEachIndexed {pageIndex,pageBlocks ->
            // At 100% fit the page to the available width; zooming in increases
            // the measured sheet width and enables horizontal scrolling.
            val pageWidth=if(a4) viewportWidth*zoomPercent/100f else viewportWidth
            Box(Modifier.width(pageWidth)) {
            val a4Height=pageWidth*297f/210f
            var contentHeightPx by remember(noteId,pageIndex,source,zoomPercent) {
                mutableIntStateOf(0)
            }
            val maxContentHeightPx=with(LocalDensity.current) {
                (a4Height-2f*pageWidth*42f/595f).roundToPx()
            }
            val overflowsA4=a4 && contentHeightPx>maxContentHeightPx+2
            Surface(
                modifier=if(a4) Modifier.fillMaxWidth()
                    .heightIn(min=a4Height)
                    else Modifier.fillMaxWidth(),
                color=background,contentColor=contentColor,
                shadowElevation=if(a4) 3.dp else 1.dp,
                shape=if(a4) androidx.compose.foundation.shape.RoundedCornerShape(3.dp)
                    else androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                // A4 zoom must scale the printed text as well as the paper.
                // Keep dp-based page geometry unchanged; only scale sp text
                // inside the sheet to avoid applying zoom twice to margins.
                val baseDensity=LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(
                    density=baseDensity.density,
                    fontScale=baseDensity.fontScale*(if(a4) zoomPercent/100f else 1f)
                )) {
                Column(Modifier.fillMaxWidth().padding(
                    horizontal=if(a4) pageWidth*42f/595f else 16.dp,
                    vertical=if(a4) pageWidth*42f/595f else 16.dp),
                    verticalArrangement=Arrangement.spacedBy(if(a4) 0.dp else 6.dp)) {
                    Column(Modifier.fillMaxWidth().onSizeChanged {
                        if(contentHeightPx!=it.height) contentHeightPx=it.height
                    },verticalArrangement=Arrangement.spacedBy(0.dp)) {
                    // PDF export prints the document title at the start of page 1.
                    // Show it on the actual A4 sheet instead of above the sheet.
                    if(a4 && pageIndex==0 && documentTitle.isNotBlank()) {
                        Text(documentTitle,
                            modifier=Modifier.fillMaxWidth().padding(bottom=12.dp),
                            style=MaterialTheme.typography.titleLarge.copy(
                                color=Color(0xFF202126),
                                textAlign=TextAlign.Center))
                    }
                    pageBlocks.forEach {block ->
                        when(block) {
                            is NeoDocumentBlocks.Block.Paragraph -> {
                                if(inlineEditing && editable && onParagraphEdited!=null) {
                                    NeoEditableDocumentParagraph(
                                        noteId=noteId,block=block,
                                        direction=textDirection,ruled=ruled,
                                        arabicFont=arabicFont,englishFont=englishFont,
                                        onEdited=onParagraphEdited,
                                        onActivated=onActiveParagraph,
                                        onError=onError
                                    )
                                } else SelectionContainer {
                                    NeoNotePreview(block.source,
                                        textDirection=textDirection,
                                        ruled=ruled,arabicFont=arabicFont,
                                        englishFont=englishFont,
                                        onChecklist=onChecklist?.let {callback->
                                            {line:Int ->callback(block.startLine+line)}
                                        }
                                    )
                                }
                            }
                            is NeoDocumentBlocks.Block.Table -> {
                                NeoDocumentTable(block,editable,{rows ->
                                    runCatching {
                                        onSourceChange(NeoDocumentBlocks.replace(source,block,
                                            NeoDocumentBlocks.serializeTable(rows)))
                                    }.onFailure {
                                        onError(it.message ?: "Table could not be saved")
                                    }
                                })
                            }
                            is NeoDocumentBlocks.Block.Image -> {
                                NeoDocumentImage(
                                    noteId,block,editable,
                                    onModify={width,align ->
                                        runCatching {
                                            onSourceChange(NeoDocumentBlocks.replace(
                                                source,block,NeoDocumentBlocks.image(
                                                    block.name,width,align)))
                                        }.onFailure {onError(it.message ?: "Image layout failed")}
                                    },onError=onError
                                )
                            }
                            is NeoDocumentBlocks.Block.PageBreak -> Unit
                        }
                    }
                    }
                    if(overflowsA4) {
                        Text(
                            neoLabel(
                                "تنبيه: محتوى الصفحة أطول من A4؛ سيُقسَّم عند تصدير PDF.",
                                "Page exceeds A4; PDF export will paginate the content."
                            ),
                            modifier=Modifier.fillMaxWidth().padding(top=8.dp),
                            color=Color(0xFF9A3412),
                            style=MaterialTheme.typography.labelSmall
                        )
                    }
                }
                }
            }
            }
        }
        }
        }
    }
}

/**
 * Each editable paragraph is a rich-text field in the same document flow as
 * images and tables. The serialized [[neo-image:...]] handle is never an
 * editable text row, but remains in the original source for backup/history.
 */
@Composable
private fun NeoEditableDocumentParagraph(
    noteId:String,
    block:NeoDocumentBlocks.Block.Paragraph,
    direction:TextDirection,
    ruled:Boolean,
    arabicFont:String,
    englishFont:String,
    onEdited:(Int,String)->Unit,
    onActivated:((NeoRichEditorState,(NeoRichEditorState)->Unit)->Unit)?,
    onError:(String)->Unit
) {
    var state by remember(noteId,block.startLine) {
        mutableStateOf(NeoRichEditorState.open(block.source))
    }
    var focused by remember(noteId,block.startLine) {mutableStateOf(false)}
    val edited by rememberUpdatedState(onEdited)
    val activated by rememberUpdatedState(onActivated)
    val errors by rememberUpdatedState(onError)
    fun apply(next:NeoRichEditorState) {
        state=next
        // Do not serialize a half-composed Arabic word while IME is active.
        if(next.value.composition==null) runCatching {
            edited(block.startLine,next.stored())
        }.onFailure {errors(it.message ?: "Could not save paragraph")}
    }
    LaunchedEffect(block.source) {
        if(state.value.composition==null && state.stored()!=block.source)
            state=NeoRichEditorState.open(block.source)
    }
    LaunchedEffect(focused,state) {
        if(focused) activated?.invoke(state) {updated ->apply(updated)}
    }
    val rtl=direction!=TextDirection.Ltr
    var measuredLines by remember { mutableStateOf<List<Float>>(emptyList()) }
    CompositionLocalProvider(LocalLayoutDirection provides
        if(rtl) androidx.compose.ui.unit.LayoutDirection.Rtl
        else androidx.compose.ui.unit.LayoutDirection.Ltr) {
        BasicTextField(
            value=state.value,
            onValueChange={apply(state.input(it))},
            modifier=Modifier.fillMaxWidth().heightIn(min=46.dp)
                .onFocusChanged {focused=it.isFocused}
                .drawBehind {
                    if(ruled) {
                        val ink=Color.Gray.copy(alpha=0.27f)
                        measuredLines.forEach { bottom ->
                            if(bottom<=size.height) drawLine(ink,
                                androidx.compose.ui.geometry.Offset(0f,bottom),
                                androidx.compose.ui.geometry.Offset(size.width,bottom),
                                strokeWidth=0.7.dp.toPx())
                        }
                    }
                },
            onTextLayout={layout ->
                val next=(0 until layout.lineCount).map { layout.getLineBottom(it) }
                if(measuredLines!=next) measuredLines=next
            },
            textStyle=MaterialTheme.typography.bodyLarge.copy(
                color=MaterialTheme.colorScheme.onSurface,
                lineHeight=if(ruled)42.sp else 32.sp,
                textDirection=direction,
                textAlign=if(rtl)TextAlign.Right else TextAlign.Left
            ),
            visualTransformation=NeoNoteTypefaces.richVisual(state.characters,arabicFont,englishFont),
            cursorBrush=androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun NeoDocumentTable(
    table:NeoDocumentBlocks.Block.Table,
    editable:Boolean,
    onSave:(List<List<String>>)->Unit
) {
    var editor by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(neoLabel("جدول","Table"),style=MaterialTheme.typography.labelSmall,
                modifier=Modifier.weight(1f))
            if(editable) TextButton(onClick={editor=true}) {
                Icon(Icons.Rounded.Edit,null,Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(neoLabel("تعديل الجدول","Edit table"))
            }
        }
        Column(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            table.cells.forEachIndexed {rowIndex,row ->
                Row {
                    row.forEach {value ->
                        Surface(
                            modifier=Modifier.width(110.dp).heightIn(min=36.dp),
                            color=if(rowIndex==0)
                                MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surface,
                            border=androidx.compose.foundation.BorderStroke(
                                0.5.dp,MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(value.ifEmpty{" "},Modifier.padding(6.dp),
                                style=MaterialTheme.typography.bodySmall,
                                maxLines=4)
                        }
                    }
                }
            }
        }
    }
    if(editor) {
        var cells by remember(table.startLine) {mutableStateOf(table.cells)}
        AlertDialog(
            onDismissRequest={editor=false},
            title={Text(neoLabel("تحرير الجدول","Edit table"))},
            text={
                Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(neoLabel("يمكن تعديل الخلايا وإضافة أو حذف الصفوف والأعمدة.",
                        "Edit cells and add or remove rows and columns."),
                        style=MaterialTheme.typography.labelSmall)
                    cells.forEachIndexed {rowIndex,row ->
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            row.forEachIndexed {colIndex,value ->
                                OutlinedTextField(
                                    value=value,
                                    onValueChange={next ->
                                        val safe=next.replace("|","").replace("\n"," ")
                                            .replace("\r"," ").take(250)
                                        cells=cells.mapIndexed {r,old ->
                                            if(r==rowIndex) old.mapIndexed {c,cell ->
                                                if(c==colIndex) safe else cell
                                            } else old
                                        }
                                    },modifier=Modifier.width(115.dp),
                                    label={Text((rowIndex+1).toString()+","+(colIndex+1))},
                                    singleLine=true,textStyle=MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        OutlinedButton(enabled=cells.size<80,onClick={
                            cells=cells+listOf(List(cells[0].size){""})
                        }) {Text(neoLabel("+ صف","+ Row"))}
                        OutlinedButton(enabled=cells.size>2,onClick={
                            cells=cells.dropLast(1)
                        }) {Text(neoLabel("− صف","− Row"))}
                        OutlinedButton(enabled=cells.first().size<8,onClick={
                            cells=cells.map{it+""}
                        }) {Text(neoLabel("+ عمود","+ Column"))}
                        OutlinedButton(enabled=cells.first().size>1,onClick={
                            cells=cells.map{it.dropLast(1)}
                        }) {Text(neoLabel("− عمود","− Column"))}
                    }
                }
            },
            confirmButton={TextButton(onClick={onSave(cells);editor=false}) {
                Text(neoLabel("حفظ الجدول","Save table"))
            }},
            dismissButton={TextButton(onClick={editor=false}) {
                Text(neoLabel("إلغاء","Cancel"))
            }}
        )
    }
}

@Composable
private fun NeoDocumentImage(
    noteId:String,
    image:NeoDocumentBlocks.Block.Image,
    editable:Boolean,
    onModify:(Int,String)->Unit,
    onError:(String)->Unit
) {
    val context=LocalContext.current
    var bitmap by remember(noteId,image.name) {mutableStateOf<Bitmap?>(null)}
    var unavailable by remember(noteId,image.name) {mutableStateOf(false)}
    var imageSelected by remember(noteId,image.name) {mutableStateOf(false)}
    LaunchedEffect(noteId,image.name) {
        bitmap=withContext(Dispatchers.IO) {
            runCatching {
                require(NeoAttachmentStore.safeName(image.name))
                val store=NeoAttachmentStore(context.applicationContext)
                require(store.list().any{it.owner==noteId && it.name==image.name})
                val file=File(context.filesDir,"attachments/"+image.name)
                val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
                BitmapFactory.decodeFile(file.absolutePath,bounds)
                require(bounds.outWidth>0 && bounds.outHeight>0)
                var sample=1
                while(bounds.outWidth/sample>1024||bounds.outHeight/sample>1024)
                    sample*=2
                BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply {
                    inSampleSize=sample;inPreferredConfig=Bitmap.Config.RGB_565
                })
            }.getOrNull()
        }
        unavailable=bitmap==null
    }
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        if(bitmap!=null) {
            Box(Modifier.fillMaxWidth(),contentAlignment=when(image.align) {
                "left"->Alignment.CenterStart
                "right"->Alignment.CenterEnd
                else ->Alignment.Center
            }) {
                Image(
                    bitmap!!.asImageBitmap(),contentDescription=neoLabel(
                        "صورة داخل المستند","Inline document image"),
                    modifier=Modifier.fillMaxWidth(image.width/100f).heightIn(max=420.dp)
                        .clickable(enabled=editable){imageSelected=!imageSelected},
                    contentScale=ContentScale.Fit
                )
            }
        } else if(unavailable) {
            Text(neoLabel("الصورة غير متاحة في هذا المستند",
                "Image not available for this note"),
                color=MaterialTheme.colorScheme.error,
                style=MaterialTheme.typography.bodySmall)
        }
        if(editable && imageSelected) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(neoLabel("حجم الصورة","Image width")+": "+image.width+"%",
                style=MaterialTheme.typography.labelSmall)
            IconButton(onClick={onModify((image.width-10).coerceAtLeast(25),image.align)}) {
                Icon(Icons.Rounded.Remove,neoLabel("تصغير الصورة","Smaller"))
            }
            IconButton(onClick={onModify((image.width+10).coerceAtMost(100),image.align)}) {
                Icon(Icons.Rounded.Add,neoLabel("تكبير الصورة","Larger"))
            }
            listOf("left","center","right").forEach {alignment ->
                TextButton(onClick={onModify(image.width,alignment)}) {
                    Text(when(alignment){"left"->"⇤";"right"->"⇥";else->"↔"})
                }
            }
        }
    }
}
