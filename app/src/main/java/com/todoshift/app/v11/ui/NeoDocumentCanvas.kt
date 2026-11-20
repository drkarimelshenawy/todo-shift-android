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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
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
    // Keep a single owner for IME composition in edit mode. This read-only
    // A4 presentation path never inserts artificial breaks into stored text.
    val textMeasurer=rememberTextMeasurer()
    val editingSessions=remember(noteId){mutableMapOf<Int,NeoA4EditingSession>()}
    fun blockKey(block:NeoDocumentBlocks.Block):String =
        "${block.startLine}:${block.endLine}:${(block as? NeoDocumentBlocks.Block.Paragraph)?.pageFragment ?: 0}"
    // Heights are measured from the real Compose content, not guessed from
    // character counts. Page breaks are presentation-only; source is unchanged.
    var zoomPercent by remember(noteId,a4) { mutableIntStateOf(100) }
    // Width and font scale affect wrapping; never paginate with stale
    // measurements from a different zoom level.
    var viewportWidthPx by remember(noteId) { mutableIntStateOf(0) }
    val measuredBlockHeights=remember(
        noteId,source,a4,zoomPercent,inlineEditing,editable,viewportWidthPx
    ) {
        mutableStateMapOf<String,Int>()
    }
    var titleHeightPx by remember(noteId,documentTitle,a4,zoomPercent,viewportWidthPx) {
        mutableIntStateOf(0)
    }
    val background=if(a4) Color.White else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor=if(a4) Color(0xFF202126) else MaterialTheme.colorScheme.onSurface
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
        BoxWithConstraints(Modifier.fillMaxWidth().onSizeChanged { size ->
            if(viewportWidthPx!=size.width) viewportWidthPx=size.width
        }) {
        val viewportWidth=maxWidth
        val sheetWidth=if(a4) viewportWidth*zoomPercent/100f else viewportWidth
        val availableHeightPx=with(LocalDensity.current) {
            (sheetWidth*(297f/210f-84f/595f)).roundToPx()
        }
        val previewStyle=MaterialTheme.typography.bodyLarge
        // A single rich-text/IME state owns the original source paragraph.
        // Only completed compositions are used for live page measurements.
        val editingSnapshots=if(a4 && editable && inlineEditing) {
            blocks.filterIsInstance<NeoDocumentBlocks.Block.Paragraph>().map { block ->
                editingSessions.getOrPut(block.startLine) {
                    NeoA4EditingSession(block.source)
                }.committed
            }
        } else emptyList()
        val displayBlocks=remember(blocks,a4,inlineEditing,editable,viewportWidthPx,zoomPercent,
            arabicFont,englishFont,textDirection,availableHeightPx,textMeasurer,previewStyle,
            editingSnapshots) {
            if(!a4 || viewportWidthPx==0) blocks
            else if(inlineEditing && editable) blocks.flatMap { block ->
                if(block !is NeoDocumentBlocks.Block.Paragraph || block.source.length>40_000)
                    listOf(block)
                else runCatching {
                    val rich=editingSessions.getOrPut(block.startLine) {
                        NeoA4EditingSession(block.source)
                    }.committed
                    val widthPx=(viewportWidthPx*zoomPercent/100f*
                        (1f-84f/595f)).toInt().coerceAtLeast(1)
                    val layout=textMeasurer.measure(
                        text=NeoNoteTypefaces.styled(rich.value.annotatedString,
                            arabicFont,englishFont),
                        style=previewStyle.copy(fontSize=(12f*zoomPercent/100f).sp,
                            lineHeight=(18f*zoomPercent/100f).sp,
                            textDirection=textDirection,
                            textAlign=if(textDirection==TextDirection.Ltr)
                                TextAlign.Left else TextAlign.Right),
                        constraints=Constraints(maxWidth=widthPx)
                    )
                    val slices=NeoA4SoftWrap.slices(rich.value.text.length,
                        IntArray(layout.lineCount){layout.getLineStart(it)},
                        IntArray(layout.lineCount){layout.getLineTop(it).toInt()},
                        FloatArray(layout.lineCount){layout.getLineBottom(it)},
                        layout.size.height,(availableHeightPx*0.82f).toInt().coerceAtLeast(1))
                    // Multiple full-buffer textfields are expensive on very
                    // large paragraphs. Fall back safely rather than allocating
                    // unbounded O(pages x characters) editing layouts.
                    if(slices.size>24 || slices.size.toLong()*
                        rich.value.text.length>180_000L) listOf(block)
                    else slices.mapIndexed { index,slice ->
                        block.copy(pageFragment=index,sliceTopPx=slice.topPx,
                            sliceBottomPx=slice.bottomPx,wholeHeightPx=layout.size.height,
                            sliceStartOffset=slice.start,sliceEndOffset=slice.end)
                    }
                }.getOrElse { listOf(block) }
            }
            else blocks.flatMap { block ->
                if(block !is NeoDocumentBlocks.Block.Paragraph) listOf(block)
                else block.source.split('\n').flatMapIndexed { lineIndex,line ->
                    val startLine=block.startLine+lineIndex
                    val eligible=line.isNotBlank() && line.length<30_000 &&
                        !line.startsWith("# ") && !line.startsWith("## ") &&
                        !line.startsWith("### ") && !line.startsWith("- [") &&
                        !line.startsWith("☐ ") && !line.startsWith("☑ ") &&
                        !line.contains("<!--task:")
                    val fragments=if(!eligible) listOf(line) else runCatching {
                        val rich=NeoRichEditorState.open(line)
                        val widthPx=(viewportWidthPx*zoomPercent/100f*
                            (1f-84f/595f)).toInt().coerceAtLeast(1)
                        val layout=textMeasurer.measure(
                            text=NeoNoteTypefaces.styled(rich.value.annotatedString,
                                arabicFont,englishFont),
                            style=previewStyle.copy(fontSize=(12f*zoomPercent/100f).sp,
                                lineHeight=(18f*zoomPercent/100f).sp,
                                textDirection=textDirection),
                            constraints=Constraints(maxWidth=widthPx)
                        )
                        val ranges=NeoA4SoftWrap.ranges(rich.value.text.length,
                            IntArray(layout.lineCount){layout.getLineStart(it)},
                            FloatArray(layout.lineCount){layout.getLineBottom(it)},
                            availableHeightPx*0.72f)
                        ranges.map { range ->
                            if(ranges.size==1) line else {
                                val tokens=rich.hidden.filter {
                                    it.position>=range.start && (it.position<range.end ||
                                        (range.end==rich.value.text.length &&
                                            it.position==range.end))
                                }.map{it.copy(position=it.position-range.start)}
                                NeoRichEditorState(
                                    TextFieldValue(rich.value.annotatedString.subSequence(
                                        range.start,range.end),TextRange(0)),
                                    rich.characters.subList(range.start,range.end),
                                    tokens
                                ).stored()
                            }
                        }
                    }.getOrElse { listOf(line) }
                    fragments.mapIndexed { fragment,part ->
                        NeoDocumentBlocks.Block.Paragraph(
                            part,startLine,startLine+1,block.sourceHash,fragment)
                    }
                }
            }
        }
        val pages=remember(displayBlocks,a4,availableHeightPx,titleHeightPx,documentTitle,measuredBlockHeights.toMap()) {
            val result=mutableListOf<MutableList<NeoDocumentBlocks.Block>>(mutableListOf())
            var used=if(a4 && documentTitle.isNotBlank()) titleHeightPx else 0
            // Precompute whether real content follows each block. Scanning the
            // remainder of the document for every manual break was quadratic
            // for long notes containing many page breaks.
            val contentAfter=BooleanArray(displayBlocks.size)
            var seenContent=false
            for(index in displayBlocks.indices.reversed()) {
                contentAfter[index]=seenContent
                val block=displayBlocks[index]
                if(when(block) {
                    is NeoDocumentBlocks.Block.PageBreak -> false
                    is NeoDocumentBlocks.Block.Paragraph -> block.source.isNotBlank()
                    else -> true
                }) seenContent=true
            }
            displayBlocks.forEachIndexed { index,block ->
                if(block is NeoDocumentBlocks.Block.PageBreak) {
                    // Preserve terminal breaks in source without showing an
                    // extra blank A4 sheet.
                    val hasFollowingContent=contentAfter[index]
                    if(hasFollowingContent || !a4) {
                        // Consecutive manual breaks (or a break immediately
                        // after an automatic page transition) must not create
                        // an empty A4 sheet. The document title on page one
                        // counts as visible content, even without body blocks.
                        val hasVisiblePageContent=result.last().any { previous ->
                            when(previous) {
                                is NeoDocumentBlocks.Block.Paragraph ->
                                    previous.source.isNotBlank()
                                is NeoDocumentBlocks.Block.PageBreak -> false
                                else -> true
                            }
                        } || (result.size==1 && a4 && documentTitle.isNotBlank())
                        if(!a4 || hasVisiblePageContent) {
                            result.add(mutableListOf())
                            used=0
                        }
                    }
                } else {
                    val height=measuredBlockHeights[blockKey(block)] ?: 0
                    if(a4 && height>0 && used+height>availableHeightPx &&
                        (result.last().isNotEmpty() ||
                            (result.size==1 && used>0))) {
                        // Even an oversized first paragraph must start below
                        // the title on a fresh sheet. Keeping it beside the
                        // title would clip more of the editable content.
                        // Splitting a single soft-wrapped line remains a
                        // separate text-layout pagination requirement.
                        result.add(mutableListOf())
                        used=0
                    }
                    result.last().add(block)
                    used+=height
                }
            }
            // Match PDF export: a trailing explicit page break must not
            // create an extra blank printed sheet.
            while(result.size>1 && result.last().isEmpty()) result.removeAt(result.lastIndex)
            result
        }
        Column(Modifier.fillMaxWidth().then(if(a4) Modifier.horizontalScroll(rememberScrollState())
            else Modifier),verticalArrangement=Arrangement.spacedBy(if(a4) 12.dp else 2.dp)) {
        pages.forEachIndexed {pageIndex,pageBlocks ->
            // At 100% fit the page to the available width; zooming in increases
            // the measured sheet width and enables horizontal scrolling.
            val pageWidth=if(a4) viewportWidth*zoomPercent/100f else viewportWidth
            Column(Modifier.width(pageWidth)) {
            val a4Height=pageWidth*297f/210f
            var contentHeightPx by remember(noteId,pageIndex,pageBlocks.map { blockKey(it) },source,zoomPercent,inlineEditing,editable,viewportWidthPx) {
                mutableIntStateOf(0)
            }
            val maxContentHeightPx=with(LocalDensity.current) {
                (a4Height-pageWidth*(84f/595f)).roundToPx()
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
                            modifier=Modifier.fillMaxWidth().padding(bottom=3.dp)
                                .onSizeChanged { size ->
                                    if(titleHeightPx!=size.height) titleHeightPx=size.height
                                },
                            // PDF prints the title at 18pt. Use matching A4
                            // typography instead of theme-dependent titleLarge.
                            style=MaterialTheme.typography.titleLarge.copy(
                                fontSize=18.sp,
                                lineHeight=24.sp,
                                color=Color(0xFF202126),
                                textAlign=TextAlign.Center))
                    }
                    pageBlocks.forEach {block ->
                        // Keep each editor's identity stable as measured blocks
                        // move between sheets; otherwise Compose can reuse a
                        // different paragraph's IME state at the same position.
                        androidx.compose.runtime.key(blockKey(block)) {
                        Box(Modifier.fillMaxWidth().onSizeChanged { size ->
                            if(measuredBlockHeights[blockKey(block)]!=size.height)
                                measuredBlockHeights[blockKey(block)]=size.height
                        }) {
                        when(block) {
                            is NeoDocumentBlocks.Block.Paragraph -> {
                                if(inlineEditing && editable && onParagraphEdited!=null &&
                                     a4 && block.wholeHeightPx>0) {
                                     NeoA4SharedEditableSlice(
                                         block=block,
                                         session=editingSessions.getOrPut(block.startLine) {
                                             NeoA4EditingSession(block.source)
                                         },
                                         direction=textDirection,ruled=ruled,
                                         arabicFont=arabicFont,englishFont=englishFont,
                                         onEdited=onParagraphEdited,
                                         onActivated=onActiveParagraph,onError=onError
                                     )
                                 } else if(inlineEditing && editable && onParagraphEdited!=null) {
                                    NeoEditableDocumentParagraph(
                                        noteId=noteId,block=block,
                                        direction=textDirection,ruled=ruled,a4=a4,
                                        arabicFont=arabicFont,englishFont=englishFont,
                                        onEdited=onParagraphEdited,
                                        onActivated=onActiveParagraph,
                                        onError=onError
                                    )
                                } else SelectionContainer {
                                    NeoNotePreview(block.source,
                                        textDirection=textDirection,
                                        ruled=ruled,arabicFont=arabicFont,
                                        englishFont=englishFont,a4=a4,
                                        onChecklist=onChecklist?.let {callback->
                                            {line:Int ->callback(block.startLine+line)}
                                        }
                                    )
                                }
                            }
                            is NeoDocumentBlocks.Block.Table -> {
                                NeoDocumentTable(block,editable,a4,{rows ->
                                    runCatching {
                                        onSourceChange(NeoDocumentBlocks.replace(source,block,
                                            NeoDocumentBlocks.serializeTable(rows)))
                                        true
                                    }.getOrElse {
                                        onError(it.message ?: "Table could not be saved")
                                        false
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
                    }
                    }
                }
                }
            }
            if(a4) {
                Text(
                    neoLabel("صفحة ${pageIndex+1} من ${pages.size}",
                        "Page ${pageIndex+1} of ${pages.size}"),
                    modifier=Modifier.fillMaxWidth().padding(top=6.dp),
                    style=MaterialTheme.typography.labelSmall,
                    color=MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign=TextAlign.Center
                )
            }
            if(overflowsA4) {
                // The warning belongs outside the printable sheet: it must
                // never alter page geometry or appear as document content.
                Text(
                    neoLabel(
                        "تنبيه: بعض المحتوى يتجاوز حدود ورقة A4. راجع ملف PDF قبل مشاركته.",
                        "Some content exceeds A4 bounds. Review the exported PDF before sharing."
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
    a4:Boolean,
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
    // IME composition is deliberately not serialized mid-word. Flush the
    // completed local draft when focus leaves this paragraph so switching
    // pages/fields cannot silently discard the last composed characters.
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
    // React only to an actual source change, never to focus loss alone.
    // Focus loss first flushes the local draft; reloading the older parent
    // value at that instant would silently discard the last typed characters.
    // While focused, preserve the IME composition and cursor until the
    // user leaves the field; a subsequent source change can then refresh it.
    LaunchedEffect(block.source) {
        if(!focused && state.value.composition==null &&
            state.stored()!=block.source)
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
            modifier=Modifier.fillMaxWidth().heightIn(min=if(a4) 18.dp else 46.dp)
                .onFocusChanged {
                    val wasFocused=focused
                    focused=it.isFocused
                    if(wasFocused && !it.isFocused && state.stored()!=block.source) {
                        runCatching { edited(block.startLine,state.stored()) }
                            .onFailure { errors(it.message ?: "Could not save paragraph") }
                    }
                }
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
                color=if(a4) Color(0xFF202126) else MaterialTheme.colorScheme.onSurface,
                // PDF paragraphs use 12pt text with approximately 18pt leading.
                // Keep the on-sheet editor close to that scale in A4 mode.
                fontSize=if(a4)12.sp else MaterialTheme.typography.bodyLarge.fontSize,
                lineHeight=if(a4)18.sp else if(ruled)42.sp else 32.sp,
                textDirection=direction,
                textAlign=if(rtl)TextAlign.Right else TextAlign.Left
            ),
            visualTransformation=NeoNoteTypefaces.richVisual(state.characters,arabicFont,englishFont),
            cursorBrush=androidx.compose.ui.graphics.SolidColor(
                if(a4) Color(0xFF174EA6) else MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun NeoDocumentTable(
    table:NeoDocumentBlocks.Block.Table,
    editable:Boolean,
    a4:Boolean,
    onSave:(List<List<String>>)->Boolean
) {
    var editor by remember(table.startLine,table.sourceHash) {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(4.dp)) {
        // In A4, controls must not consume printable page height:
        // tap the table itself to edit, while normal notes retain the toolbar.
        if(editable && !a4) Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text(neoLabel("جدول","Table"),style=MaterialTheme.typography.labelSmall,
                modifier=Modifier.weight(1f))
            TextButton(onClick={editor=true}) {
                Icon(Icons.Rounded.Edit,null,Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(neoLabel("تعديل الجدول","Edit table"))
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().then(
            if(a4 && editable) Modifier.clickable { editor=true } else Modifier
        )) {
            // A4 tables use the printable page width, not a fixed 110dp
            // per column that could silently clip exported columns.
            val cellWidth=if(a4) maxWidth/table.cells.first().size else 110.dp
            Column(Modifier.fillMaxWidth().then(
                if(a4) Modifier else Modifier.horizontalScroll(rememberScrollState())
            )) {
            table.cells.forEachIndexed {rowIndex,row ->
                Row(Modifier.height(IntrinsicSize.Min)) {
                    row.forEach {value ->
                        Surface(
                            modifier=Modifier.width(cellWidth)
                                .then(if(a4) Modifier.fillMaxHeight().heightIn(min=25.dp)
                                    else Modifier.heightIn(min=36.dp)),
                            color=if(a4) Color.White else if(rowIndex==0)
                                MaterialTheme.colorScheme.surfaceContainerHigh
                                else MaterialTheme.colorScheme.surface,
                            border=androidx.compose.foundation.BorderStroke(
                                0.5.dp,MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(value.ifEmpty{" "},Modifier.padding(6.dp),
                                style=if(a4) MaterialTheme.typography.bodySmall.copy(
                                    fontSize=if(rowIndex==0) 11.sp else 10.sp,
                                    lineHeight=if(rowIndex==0) 14.sp else 13.sp,
                                    color=Color(0xFF202126)
                                ) else MaterialTheme.typography.bodySmall,
                                maxLines=if(a4) Int.MAX_VALUE else 4)
                        }
                    }
                }
            }
            }
        }
    }
    if(editor) {
        var cells by remember(table.startLine,table.sourceHash) {mutableStateOf(table.cells)}
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
            confirmButton={TextButton(onClick={if(onSave(cells)) editor=false}) {
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
