package com.todoshift.app.v11.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.NeoNoteHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Compact document navigator: search, heading outline and encrypted revisions.
 * Does not rewrite the active document while searching or counting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NeoNoteProToolbar(
    noteId:String,
    document:String,
    source:String,
    title:String,
    onJump:(Int,Int)->Unit,
    onRestore:(NeoNoteHistoryStore.Revision)->Unit,
    onError:(String)->Unit,
    command:String?=null,
    onCommandHandled:()->Unit={},
    showToolbar:Boolean=true
) {
    val context=LocalContext.current
    val store=remember(context) {NeoNoteHistoryStore(context.applicationContext)}
    val scope=rememberCoroutineScope()
    var findOpen by rememberSaveable {mutableStateOf(false)}
    var query by rememberSaveable {mutableStateOf("")}
    var matchIndex by rememberSaveable {mutableIntStateOf(-1)}
    var outlineOpen by remember {mutableStateOf(false)}
    var historyOpen by remember {mutableStateOf(false)}
    var chosen by remember {mutableStateOf<NeoNoteHistoryStore.Revision?>(null)}
    var exportOpen by remember {mutableStateOf(false)}
    val htmlLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/html")) {uri ->
        if(uri!=null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {output ->
                        output.write(NeoNoteExporter.html(title,source).toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output document")
                }
            } catch(e:Exception) {onError(e.message ?: "Could not export HTML")}
        }
    }
    val textLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")) {uri ->
        if(uri!=null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {output ->
                        output.write(NeoNoteExporter.plain(source).toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output document")
                }
            } catch(e:Exception) {onError(e.message ?: "Could not export text")}
        }
    }
    val pdfLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")) {uri ->
        if(uri!=null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {output ->
                        NeoDocumentPdf.write(context,noteId,title,source,output)
                    } ?: error("Cannot open PDF output document")
                }
            } catch(e:Exception) {onError(e.message ?: "Could not export PDF")}
        }
    }
    val docxLauncher=rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    ) {uri ->
        if(uri!=null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {output ->
                        NeoDocumentDocx.write(context,noteId,title,source,output)
                    } ?: error("Cannot open DOCX output document")
                }
            } catch(e:Exception) {onError(e.message ?: "Could not export DOCX")}
        }
    }
    val exportName=title.ifBlank {"Note"}
        .replace(Regex("""[/\\:*?"<>|]"""),"_").take(55)
    var revisions by remember {mutableStateOf<List<NeoNoteHistoryStore.Revision>>(emptyList())}
    val stats=remember(document) {NeoNoteDocumentTools.stats(document)}
    val results=remember(document,query) {NeoNoteDocumentTools.find(document,query)}
    val headings=remember(source) {NeoNoteDocumentTools.headings(source)}
    // Commands open the requested utility directly, without an additional
    // toolbar-only mode or a nested bottom sheet of unexplained icons.
    LaunchedEffect(command) {
        when(command) {
            "find" -> findOpen=true
            "outline" -> outlineOpen=true
            "export" -> exportOpen=true
            "history" -> try {
                revisions=withContext(Dispatchers.IO) {store.revisions(noteId)}
                historyOpen=true
            } catch(e:Exception) {onError(e.message ?: "Could not open history")}
        }
        if(command!=null) onCommandHandled()
    }
    fun jump(direction:Int) {
        if(results.isEmpty()) return
        matchIndex=(matchIndex+direction+results.size)%results.size
        val current=results[matchIndex]
        onJump(current.start,current.end)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=1.dp)) {
        if(showToolbar) Row(verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            if(showToolbar) Text(neoLabel(stats.words.toString()+" كلمة • "+
                    stats.characters+" حرف",stats.words.toString()+" words • "+
                    stats.characters+" chars"),
                modifier=Modifier.weight(1f),
                color=MaterialTheme.colorScheme.onSurfaceVariant,
                style=MaterialTheme.typography.labelSmall)
            Box {
                if(showToolbar) IconButton(modifier=Modifier.size(36.dp),onClick={exportOpen=true}) {
                    Icon(Icons.Rounded.FileDownload,
                        neoLabel("تصدير المستند","Export document"),
                        Modifier.size(20.dp))
                }
                NeoLocalizedMenu(expanded=exportOpen,onDismissRequest={exportOpen=false}) {
                    DropdownMenuItem(
                        leadingIcon={Icon(Icons.Rounded.Description,null)},text={Text(neoLabel("Word DOCX قابل للتحرير","Editable Word DOCX"))},
                        onClick={
                            exportOpen=false
                            docxLauncher.launch(exportName+".docx")
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon={Icon(Icons.Rounded.PictureAsPdf,null)},text={Text(neoLabel("PDF بحجم A4","PDF (A4)"))},
                        onClick={
                            exportOpen=false
                            pdfLauncher.launch(exportName+".pdf")
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon={Icon(Icons.Rounded.Print,null)},text={Text(neoLabel("طباعة المستند","Print document"))},
                        onClick={
                            exportOpen=false
                            runCatching {
                                NeoDocumentPrinter.print(context,noteId,title,source)
                            }.onFailure {onError(it.message ?: "Print unavailable")}
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon={Icon(Icons.Rounded.Code,null)},text={Text(neoLabel("HTML منسّق","Styled HTML"))},
                        onClick={
                            exportOpen=false
                            htmlLauncher.launch(exportName+".html")
                        }
                    )
                    DropdownMenuItem(
                        leadingIcon={Icon(Icons.Rounded.TextSnippet,null)},text={Text(neoLabel("نص TXT","Plain text TXT"))},
                        onClick={
                            exportOpen=false
                            textLauncher.launch(exportName+".txt")
                        }
                    )
                }
            }
            if(showToolbar) {
            IconButton(modifier=Modifier.size(36.dp),onClick={findOpen=!findOpen}) {
                Icon(Icons.Rounded.Search,neoLabel("بحث داخل المستند","Find in note"),
                    modifier=Modifier.size(20.dp))
            }
            IconButton(modifier=Modifier.size(36.dp),onClick={outlineOpen=true}) {
                Icon(Icons.Rounded.FormatListBulleted,
                    neoLabel("فهرس العناوين","Document outline"),Modifier.size(20.dp))
            }
            IconButton(modifier=Modifier.size(36.dp),onClick={
                scope.launch {
                    try {
                        revisions=withContext(Dispatchers.IO) {store.revisions(noteId)}
                        historyOpen=true
                    } catch(e:Exception) {onError(e.message ?: "Could not open history")}
                }
            }) {
                Icon(Icons.Rounded.History,neoLabel("سجل الإصدارات","Version history"),
                    Modifier.size(20.dp))
            }
            }
        }
        if(findOpen) {
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                OutlinedTextField(
                    value=query,onValueChange={
                        query=it.take(128);matchIndex=-1
                    },
                    modifier=Modifier.weight(1f),
                    singleLine=true,
                    label={Text(neoLabel("بحث داخل الملاحظة","Find in note"))}
                )
                Text(
                    if(query.isBlank()) "—" else
                        (if(results.isEmpty()) "0" else
                            (matchIndex.coerceIn(0,results.lastIndex)+1).toString())+
                            "/"+results.size,
                    style=MaterialTheme.typography.labelSmall
                )
                IconButton(enabled=results.isNotEmpty(),onClick={jump(-1)}) {
                    Icon(Icons.Rounded.KeyboardArrowUp,
                        neoLabel("السابق","Previous result"))
                }
                IconButton(enabled=results.isNotEmpty(),onClick={jump(1)}) {
                    Icon(Icons.Rounded.KeyboardArrowDown,
                        neoLabel("التالي","Next result"))
                }
                IconButton(onClick={findOpen=false;query=""}) {
                    Icon(Icons.Rounded.Close,neoLabel("إغلاق البحث","Close search"))
                }
            }
        }
    }

    // The no-toolbar mode uses a labeled export sheet. A DropdownMenu anchored
    // to a hidden zero-width icon is both confusing and hard to reach.
    if(exportOpen && !showToolbar) {
        ModalBottomSheet(onDismissRequest={exportOpen=false}) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=8.dp),
                verticalArrangement=Arrangement.spacedBy(5.dp)
            ) {
                Text(neoLabel("مشاركة وتصدير","Share & export"),
                    style=MaterialTheme.typography.titleLarge)
                NeoNoteExportAction(neoLabel("Word DOCX — قابل للتحرير","Editable Word DOCX")) {
                    exportOpen=false;docxLauncher.launch(exportName+".docx")
                }
                NeoNoteExportAction(neoLabel("PDF — صفحات A4","PDF — A4 pages")) {
                    exportOpen=false;pdfLauncher.launch(exportName+".pdf")
                }
                NeoNoteExportAction(neoLabel("طباعة","Print")) {
                    exportOpen=false
                    runCatching {NeoDocumentPrinter.print(context,noteId,title,source)}
                        .onFailure {onError(it.message ?: "Print unavailable")}
                }
                NeoNoteExportAction(neoLabel("HTML منسّق","Styled HTML")) {
                    exportOpen=false;htmlLauncher.launch(exportName+".html")
                }
                NeoNoteExportAction(neoLabel("نص عادي TXT","Plain TXT")) {
                    exportOpen=false;textLauncher.launch(exportName+".txt")
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if(outlineOpen) AlertDialog(
        onDismissRequest={outlineOpen=false},
        title={Text(neoLabel("فهرس العناوين","Document outline"))},
        text={
            Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState())) {
                if(headings.isEmpty()) Text(neoLabel(
                    "استخدم عناوين H1 وH2 وH3 من قائمة التنسيق.",
                    "Use H1, H2 and H3 headings from formatting tools."))
                headings.forEach {heading ->
                    TextButton(onClick={
                        onJump(heading.start,heading.start)
                        outlineOpen=false
                    }) {Text("   ".repeat(heading.level-1)+heading.title,maxLines=2)}
                }
            }
        },
        confirmButton={TextButton(onClick={outlineOpen=false}) {
            Text(neoLabel("إغلاق","Close"))
        }}
    )

    if(historyOpen) AlertDialog(
        onDismissRequest={historyOpen=false},
        title={Text(neoLabel("سجل الإصدارات","Version history"))},
        text={
            Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(neoLabel(
                    "سجل مشفّر حتى 12 إصدارًا. يُدرج في النسخة الاحتياطية المحمية بكلمة مرور فقط.",
                    "Up to 12 encrypted revisions. Included only in password-protected backups."),
                    style=MaterialTheme.typography.bodySmall)
                if(revisions.isEmpty()) Text(neoLabel("لا توجد نسخ سابقة",
                    "No previous versions yet"))
                revisions.forEach {revision ->
                    TextButton(onClick={chosen=revision;historyOpen=false}) {
                        Column {
                            Text(java.text.DateFormat.getDateTimeInstance(
                                java.text.DateFormat.MEDIUM,java.text.DateFormat.SHORT
                            ).format(java.util.Date(revision.savedAt)))
                            Text(revision.title.ifBlank {
                                neoLabel("بدون عنوان","Untitled")
                            },maxLines=1)
                            Text(NeoNoteDocumentTools.visible(revision.details).take(90),
                                maxLines=2,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={historyOpen=false}) {
            Text(neoLabel("إغلاق","Close"))
        }}
    )

    chosen?.let {revision ->
        AlertDialog(
            onDismissRequest={chosen=null},
            title={Text(neoLabel("استعادة النسخة؟","Restore version?"))},
            text={Text(neoLabel(
                "ستحل هذه النسخة محل العنوان والنص بعد تأمين النسخة الحالية. المرفقات والروابط لن تتغير.",
                "The saved title and text will replace the current content after it is checkpointed. Attachments and links remain unchanged."))},
            confirmButton={TextButton(onClick={
                chosen=null;onRestore(revision)
            }) {Text(neoLabel("استعادة","Restore"))}},
            dismissButton={TextButton(onClick={chosen=null}) {
                Text(neoLabel("إلغاء","Cancel"))
            }}
        )
    }
}


@Composable
private fun NeoNoteExportAction(label:String,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),
        shape=MaterialTheme.shapes.small) {
        Text(label,modifier=Modifier.fillMaxWidth()
            .padding(horizontal=14.dp,vertical=12.dp),
            style=MaterialTheme.typography.bodyLarge)
    }
}
