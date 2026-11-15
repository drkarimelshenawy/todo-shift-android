package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocalNeoTaskCompact = staticCompositionLocalOf { true }
val LocalNeoTaskMove = staticCompositionLocalOf<((String,Int)->Unit)?> { null }

@Composable
fun NeoPremiumTasksScreen(db: V11Database, workplaces: List<Workplace>, onChange: (V11Database)->Unit,
    initialAction: String?, initialTaskId: String?, onHandled: ()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val permission=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}
    val store=remember { NeoSmartStore(context.applicationContext) }
    val commit=rememberNeoCommit(onChange);val write=LocalNeoEditorWriteState.current
    val undo=LocalNeoUndo.current;val nav=LocalNeoFeatures.current
    var options by remember { mutableStateOf(NeoTaskListOptions()) }
    var optionsReady by remember { mutableStateOf(false) };var settingsBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { options=withContext(Dispatchers.IO) { NeoTaskListOptions.read(store.snapshot()) };optionsReady=true }
        catch(failure: Exception) { error=failure.message ?: "Unable to load task preferences" }
    }
    fun saveOptions(next: NeoTaskListOptions) {
        if(!optionsReady || settingsBusy) return
        settingsBusy=true
        scope.launch {
            try { withContext(Dispatchers.IO) { store.update { it.put("taskListOptions",next.json()) } };options=next }
            catch(failure: Exception) { error=failure.message }
            finally { settingsBusy=false }
        }
    }
    var notesTab by rememberSaveable { mutableStateOf(NeoLaunchPreferences(context).openNotes) }
    LaunchedEffect(Unit) {if(initialAction==null) notesTab=NeoLaunchPreferences(context).openNotes}
    LaunchedEffect(initialAction,initialTaskId) {
        if(initialAction=="tasks" || (initialAction=="open" && db.tasks.any {it.id==initialTaskId})) notesTab=false
        if(initialAction=="notes" || (initialAction=="open" && db.dayItems.any {it.id==initialTaskId && it.type==DayItemTypeV11.NOTE})) notesTab=true
    }
    val noteSecurity=remember {V11SecurityStore(context.applicationContext)}
    val noteSecuritySettings=noteSecurity.load()
    var taskSectionUnlocked by remember {mutableStateOf(false)}
    var noteSectionUnlocked by remember {mutableStateOf(false)}
    val securityLifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var noteBackgroundAt by remember {mutableLongStateOf(0L)}
    DisposableEffect(securityLifecycle,noteSecuritySettings) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {noteBackgroundAt=System.currentTimeMillis();if(noteSecuritySettings.autoLockMinutes==0) {noteSectionUnlocked=false;taskSectionUnlocked=false}}
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START && noteBackgroundAt>0 && System.currentTimeMillis()-noteBackgroundAt>=noteSecuritySettings.autoLockMinutes*60000L) {noteSectionUnlocked=false;taskSectionUnlocked=false}
        }
        securityLifecycle.addObserver(observer);onDispose {securityLifecycle.removeObserver(observer)}
    }
    if(!notesTab&&noteSecuritySettings.protectTasksOnly&&!taskSectionUnlocked) {
        Column {
            TextButton(onClick={notesTab=true}) {Text(neoLabel("الملاحظات","Notes"))}
            V11AccountsUnlockScreen(noteSecuritySettings,noteSecurity) {taskSectionUnlocked=true}
        }
        return
    }
    if(notesTab&&noteSecuritySettings.protectNotes&&!noteSectionUnlocked) {
        Column {
            TextButton(onClick={notesTab=false}) {Text(neoLabel("المهام","Tasks"))}
            V11AccountsUnlockScreen(noteSecuritySettings,noteSecurity) {noteSectionUnlocked=true}
        }
        return
    }
    var preferencesOpen by remember { mutableStateOf(false) }
    var noteEditor by rememberSaveable { mutableStateOf(false) }
    var noteId by rememberSaveable { mutableStateOf<String?>(null) }
    var noteSearch by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var noteControlsExpanded by rememberSaveable { mutableStateOf(false) }
    var noteFolder by rememberSaveable { mutableStateOf("active") }
    var noteTag by rememberSaveable {mutableStateOf<String?>(null)}
    var noteSelectionMode by remember {mutableStateOf(false)}
    var selectedNotes by remember {mutableStateOf(setOf<String>())}
    var confirmTrash by remember {mutableStateOf(false)}
    var noteSort by rememberSaveable { mutableStateOf("updated") }
    var hiddenAuthorized by remember { mutableStateOf(false) }
    var hiddenUnlock by remember { mutableStateOf(false) }
    val noteLifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(noteLifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP){hiddenAuthorized=false;if(noteFolder=="hidden")noteFolder="active"}}
        noteLifecycle.addObserver(observer)
        onDispose {noteLifecycle.removeObserver(observer)}
    }
    var period by rememberSaveable { mutableStateOf(NeoTaskPeriod.ALL_DATES) }
    var status by rememberSaveable { mutableStateOf(NeoTaskStatus.ALL) }
    var search by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf<TaskPriorityV11?>(null) }
    var calendar by rememberSaveable { mutableStateOf<String?>(null) }
    var filters by rememberSaveable { mutableStateOf(false) };var menu by remember { mutableStateOf(false) }
    var taskControlsExpanded by rememberSaveable { mutableStateOf(false) }
    var editor by rememberSaveable { mutableStateOf(false) };var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) };var missing by remember { mutableStateOf(false) }
    var undoCandidate by remember { mutableStateOf<V11Database?>(null) };var notice by remember { mutableStateOf<String?>(null) }
    val today=neoNow().toLocalDate();val date=today.toString()
    val visible=remember(db.tasks,db.calendars) {
        val ids=db.calendars.filter { it.visible }.map { it.id }.toSet()
        db.tasks.filter { it.calendarId==null || it.calendarId in ids }
    }
    val tasks=remember(visible,db.dayItems,period,status,search,priority,calendar,options,date) {
        NeoTaskQueries.select(visible.filter {search.isBlank() || (!it.linkedNotePrivateV11(db) && !(noteSecuritySettings.protectNotes&&it.linkedNoteId!=null))},date,period,status,search,priority,calendar,options)
    }
    var quickTitle by rememberSaveable {mutableStateOf("")}
    var quickId by rememberSaveable {mutableStateOf(java.util.UUID.randomUUID().toString())}
    var quickSubmitted by remember {mutableStateOf(false)}
    LaunchedEffect(write.error) {if(write.error!=null)quickSubmitted=false}
    LaunchedEffect(db.tasks,quickId) {if(db.tasks.any {it.id==quickId}) {quickTitle="";quickId=java.util.UUID.randomUUID().toString();quickSubmitted=false}}
    fun quickAdd() {
        if(quickTitle.isBlank()||quickSubmitted||write.pending)return
        quickSubmitted=true
        val task=TaskV11(quickId,quickTitle.trim(),date=date,calendarId=calendar)
        if(db.tasks.any {it.id==quickId}) {quickSubmitted=false;return}
        commit(db.copy(tasks=db.tasks+task)) {quickTitle="";quickId=java.util.UUID.randomUUID().toString();quickSubmitted=false}
    }
    fun open(id: String?=null) { editingId=id;editor=true }
    LaunchedEffect(initialAction,initialTaskId) {
        when(initialAction) { "tasks" -> notesTab=false; "notes" -> notesTab=true; "add" -> open(); "open" -> when {db.tasks.any { it.id==initialTaskId }->open(initialTaskId);db.dayItems.any {it.id==initialTaskId&&it.type==DayItemTypeV11.NOTE}-> {notesTab=true;noteId=initialTaskId;noteEditor=true};else->missing=true} }
        if(initialAction!=null) onHandled()
    }
    fun changed(next: V11Database, message: String) {
        if(write.pending) return
        commit(next) { undoCandidate=next;notice=message }
    }
    if(confirmTrash) AlertDialog(onDismissRequest={confirmTrash=false},title={Text(neoLabel("نقل المحدد إلى المهملات؟","Move selected notes to trash?"))},text={Text(neoLabel("يمكن استعادتها من المهملات","You can restore them from Trash"))},confirmButton={TextButton(onClick={changed(db.copy(dayItems=db.dayItems.map {if(it.id in selectedNotes&&!it.noteLocked) it.copy(noteTrashed=true,updatedAt=System.currentTimeMillis()) else it}),neoLiteral("نُقلت إلى المهملات","Moved to Trash"));selectedNotes=emptySet();confirmTrash=false}) {Text(neoLabel("نقل","Move"))}},dismissButton={TextButton(onClick={confirmTrash=false}) {Text(neoLabel("إلغاء","Cancel"))}})
    if(preferencesOpen) AlertDialog(onDismissRequest={preferencesOpen=false},title={Text(neoLabel("المهام والملاحظات","Tasks & Notes"))},text={Column {NeoLaunchPreferencesUi()}},confirmButton={TextButton(onClick={preferencesOpen=false}) {Text(neoLabel("تم","Done"))}})
    if(missing) AlertDialog(onDismissRequest={missing=false},title={Text(neoLiteral("المهمة غير متاحة","Task unavailable"))},
        confirmButton={TextButton(onClick={missing=false}) {Text(neoLiteral("إغلاق","Close"))}})
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNeoTaskCompact provides options.compact,
            LocalNeoTaskMove provides if(options.sort==NeoTaskSort.MANUAL && optionsReady && !settingsBusy) { { id,delta ->
                saveOptions(options.copy(order=NeoTaskQueries.move(db.tasks,options.order,tasks,id,delta)))
            } } else null) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start=12.dp,end=12.dp,top=8.dp,bottom=96.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item(key="task-header") {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(if(notesTab) neoLabel("الملاحظات", "Notes") else neoLabel("المهام", "Tasks"), Modifier.weight(1f), style=MaterialTheme.typography.titleLarge)
                        Box {
                            var sharedMenu by remember {mutableStateOf(false)}
                            IconButton(onClick={sharedMenu=true}) {Icon(Icons.Rounded.MoreVert,neoLabel("خيارات","Options"))}
                            DropdownMenu(sharedMenu,{sharedMenu=false}) {
                                if(!notesTab) {
                                    DropdownMenuItem(text={Text(neoLabel("قوالب المهام","Task templates"))},onClick={sharedMenu=false;nav.open("TASK_TEMPLATES")})
                                    DropdownMenuItem(text={Text(neoLabel("مكتبة المرفقات","Attachments"))},onClick={sharedMenu=false;nav.open("ATTACHMENTS")})
                                    DropdownMenuItem(text={Text(neoLabel("تراجع عن آخر تعديل","Undo last change"))},enabled=undo.enabled&&!write.pending,onClick={sharedMenu=false;undo.perform()})
                                }
                                if(notesTab) DropdownMenuItem(text={Text(neoLabel("تحديد الملاحظات","Select notes"))},onClick={sharedMenu=false;noteSelectionMode=!noteSelectionMode;selectedNotes=emptySet()})
                                DropdownMenuItem(text={Text(neoLabel("القسم الافتراضي وألوان الملاحظات","Default section and note colors"))},onClick={sharedMenu=false;preferencesOpen=true})
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { NeoPremiumTabs(listOf(neoLabel("المهام", "Tasks"),neoLabel("الملاحظات", "Notes")),if(notesTab) 1 else 0,{notesTab=it==1}) }
                        IconButton(onClick={searchExpanded=!searchExpanded}) {Icon(if(searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,neoLabel("بحث","Search"))}
                        IconButton(onClick={if(notesTab) noteControlsExpanded=!noteControlsExpanded else taskControlsExpanded=!taskControlsExpanded}) {Icon(Icons.Rounded.Tune,neoLabel("فلاتر وفرز","Filters and sort"))}
                    }
                    if(searchExpanded) OutlinedTextField(if(notesTab) noteSearch else search,{if(notesTab) noteSearch=it else search=it},label={Text(neoLabel("بحث","Search"))},singleLine=true,trailingIcon={if((if(notesTab)noteSearch else search).isNotBlank()) IconButton(onClick={if(notesTab)noteSearch="" else search=""}) {Icon(Icons.Rounded.Clear,neoLabel("مسح البحث","Clear search"))}},modifier=Modifier.fillMaxWidth())
                }
                if (!notesTab) {
                    item(key="task-filters") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            listOf("all" to neoLabel("الكل","All"),"active" to neoLabel("نشطة","Active"),"done" to neoLabel("مكتملة","Completed"),"overdue" to neoLabel("متأخرة","Overdue"),"today" to neoLabel("اليوم","Today")).forEach {(key,label)->
                                val selected=when(key){"today"->period==NeoTaskPeriod.TODAY;"overdue"->period==NeoTaskPeriod.OVERDUE;"done"->period==NeoTaskPeriod.ALL_DATES&&status==NeoTaskStatus.COMPLETED;"active"->period==NeoTaskPeriod.ALL_DATES&&status==NeoTaskStatus.ACTIVE;else->period==NeoTaskPeriod.ALL_DATES&&status==NeoTaskStatus.ALL}
                                FilterChip(selected,onClick={period=when(key){"today"->NeoTaskPeriod.TODAY;"overdue"->NeoTaskPeriod.OVERDUE;else->NeoTaskPeriod.ALL_DATES};status=when(key){"active","overdue"->NeoTaskStatus.ACTIVE;"done"->NeoTaskStatus.COMPLETED;else->NeoTaskStatus.ALL}},label={Text(label)})
                            }
                        }
                        OutlinedTextField(quickTitle,{quickTitle=it.take(300)},enabled=!quickSubmitted&&!write.pending,label={Text(neoLabel("إضافة مهمة سريعة…","Quick task…"))},singleLine=true,modifier=Modifier.fillMaxWidth(),trailingIcon={IconButton(enabled=quickTitle.isNotBlank()&&!quickSubmitted&&!write.pending,onClick={quickAdd()}) {Icon(Icons.Rounded.Add,neoLabel("إضافة","Add"))}})
                        if(taskControlsExpanded) {
                        TextButton(onClick={filters=true}) {Text(neoLabel("الفرز والتصفية المتقدمة","Advanced sort and filters"))}
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            AssistChip(onClick={filters=true},label={Text(periodLabel(period))})
                            FilterChip(selected=calendar==null,onClick={calendar=null},label={Text(neoLabel("كل التقاويم","All calendars"))})
                            db.calendars.filter {it.visible}.forEach { category ->
                                FilterChip(selected=calendar==category.id,onClick={calendar=category.id},label={Text(category.name)})
                            }
                        }
                        }
                        val due=visible.filter {it.date==date};val done=due.count {it.status==TaskStatusV11.COMPLETED}
                        if(due.isNotEmpty()) {
                            Text(neoLabel("$done من ${due.size} مكتملة اليوم","$done of ${due.size} completed today"),style=MaterialTheme.typography.labelSmall)
                            LinearProgressIndicator(progress={done.toFloat()/due.size},modifier=Modifier.fillMaxWidth().padding(vertical=6.dp))
                        }
                        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                    }
                    if(tasks.isEmpty()) item {NeoEmptyState(if(search.isBlank() && priority==null && calendar==null && period==NeoTaskPeriod.TODAY && status==NeoTaskStatus.ALL) neoLabel("لا توجد مهام لليوم","No tasks for today") else neoLabel("لا توجد مهام مطابقة","No matching tasks"),neoLabel("غيّر التاريخ أو الفلاتر، أو أضف مهمة","Change the date or filters, or add a task"))}
                    (if(options.sort==NeoTaskSort.MANUAL) linkedMapOf("" to tasks) else tasks.groupBy {it.date}).forEach { (day,group) ->
                        item(key="heading-$day") {Text((if(day.isEmpty()) neoLabel("ترتيب يدوي","Manual order") else if(day==date) neoLabel("مهام اليوم","Today") else day)+" · "+group.size,Modifier.padding(top=8.dp),style=MaterialTheme.typography.titleMedium)}
                        items(group,key={it.id}) {task ->
                            TaskCardV11(task,privateSource=task.linkedNotePrivateV11(db)||(noteSecuritySettings.protectNotes&&task.linkedNoteId!=null),onToggleComplete={
                                val complete=task.status!=TaskStatusV11.COMPLETED;val now=System.currentTimeMillis()
                                changed(db.copy(tasks=db.tasks.map {if(it.id==task.id) it.withCompletionV11(complete,now) else it}),neoLiteral("تم تحديث المهمة","Task updated"))
                            },onEdit={open(task.id)},onDelete={deleteId=task.id},categoryLabel=db.calendars.firstOrNull {it.id==task.calendarId}?.name)
                        }
                    }
                } else {
                    item(key="notes-controls") {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                            listOf("active" to neoLabel("الكل","All"),"pinned" to neoLabel("مثبتة","Pinned"),"protected" to neoLabel("محمية","Protected"),"archive" to neoLabel("أرشيف","Archive")).forEach {(folder,label)->FilterChip(noteFolder==folder,onClick={noteFolder=folder},label={Text(label)})}
                        }
                        if(noteControlsExpanded) {
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton(onClick={noteFolder="trash"}) {Text(neoLabel("سلة المهملات","Trash"))}
                                TextButton(onClick={hiddenAuthorized=true;noteFolder="hidden"}) {Text(neoLabel("المخفية","Hidden"))}
                                TextButton(onClick={noteTag=null;noteSearch="";noteSort="updated";noteFolder="active"}) {Text(neoLabel("مسح الفلاتر","Reset filters"))}
                            }
                            val noteTags=db.dayItems.filter {it.type==DayItemTypeV11.NOTE&&!it.noteLocked&&!it.noteHidden}.flatMap {it.noteTags.split(",")}.map {it.trim()}.filter {it.isNotBlank()}.distinct().sorted()
                            if(noteTags.isNotEmpty()) NeoChoice(neoLabel("التصنيف أو الوسم","Category or tag"),listOf<String?>(null)+noteTags,noteTag,{it ?: neoLabel("الكل","All")},{noteTag=it})
                            NeoChoice(neoLabel("ترتيب الملاحظات","Sort notes"),listOf("updated","title","date"),noteSort,{when(it){"title"->neoLabel("العنوان","Title");"date"->neoLabel("تاريخ الإنشاء","Created");else->neoLabel("آخر تعديل","Updated")}},{noteSort=it})
                        }
                        if(noteFolder in listOf("trash","hidden")) Text(if(noteFolder=="trash") neoLabel("سلة المهملات","Trash") else neoLabel("الملاحظات المخفية","Hidden notes"),style=MaterialTheme.typography.titleSmall)
                        if(selectedNotes.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState())) {
                            TextButton(enabled=!write.pending,onClick={changed(db.copy(dayItems=db.dayItems.map {if(it.id in selectedNotes&&!it.noteLocked) it.copy(notePinned=true) else it}),neoLiteral("تم التثبيت","Pinned"));selectedNotes=emptySet()}) {Text(neoLabel("تثبيت","Pin"))}
                            TextButton(enabled=!write.pending,onClick={changed(db.copy(dayItems=db.dayItems.map {if(it.id in selectedNotes&&!it.noteLocked) it.copy(noteArchived=noteFolder !in setOf("archive","trash"),noteTrashed=false) else it}),neoLiteral("تم التحديث","Updated"));selectedNotes=emptySet()}) {Text(if(noteFolder=="archive"||noteFolder=="trash") neoLabel("استعادة","Restore") else neoLabel("أرشفة","Archive"))}
                            TextButton(enabled=!write.pending,onClick={confirmTrash=true}) {Text(neoLabel("مهملات","Trash"))}
                            TextButton(onClick={selectedNotes=emptySet();noteSelectionMode=false}) {Text(neoLabel("إلغاء التحديد","Clear selection"))}
                        }
                    }
                    val notes=db.dayItems.filter { note ->
                        note.type==DayItemTypeV11.NOTE &&
                        when(noteFolder) {"protected"->note.noteLocked&&!note.noteHidden&&!note.noteArchived&&!note.noteTrashed;"archive"->note.noteArchived&&!note.noteTrashed&&!note.noteHidden;"trash"->note.noteTrashed&&!note.noteHidden;"hidden"->hiddenAuthorized&&note.noteHidden;"pinned"->note.notePinned&&!note.noteHidden&&!note.noteArchived&&!note.noteTrashed;else->!note.noteArchived&&!note.noteTrashed&&!note.noteHidden} &&
                        (noteTag==null || (!note.noteLocked && note.noteTags.split(",").map {it.trim()}.contains(noteTag))) &&
                        (noteSearch.isBlank() || (!note.noteLocked && listOf(note.title,notePlainText(note.details),note.noteTags).any {it.contains(noteSearch,true)}))
                    }.let { notes -> when(noteSort){"title"->notes.sortedBy {if(it.noteLocked) "" else it.title.ifBlank {notePlainText(it.details).lineSequence().firstOrNull().orEmpty()}};"date"->notes.sortedByDescending {it.createdAt};else->notes.sortedByDescending {it.updatedAt}} }.sortedByDescending {it.notePinned}
                    if(notes.isEmpty()) item {NeoEmptyState(neoLabel("لا توجد ملاحظات مطابقة","No matching notes"),neoLabel("غيّر البحث أو المجلد، أو أضف ملاحظة","Change search or folder, or add a note"))}
                    items(notes,key={it.id}) {note ->
                        val customColor=if(note.notePrivate) null else (note.noteBackground ?: NeoLaunchPreferences(context).defaultNoteBackground.takeIf {it.isNotBlank()})?.let {runCatching {android.graphics.Color.parseColor(it)}.getOrNull()}
                        val contentColor=customColor?.let {if(androidx.core.graphics.ColorUtils.calculateLuminance(it)>0.45) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White} ?: MaterialTheme.colorScheme.onSurface
                        Card(onClick={noteId=note.id;noteEditor=true},colors=CardDefaults.cardColors(containerColor=customColor?.let {androidx.compose.ui.graphics.Color(it)} ?: MaterialTheme.colorScheme.surface,contentColor=contentColor)) {
                            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                                if(noteSelectionMode&&!note.noteLocked) Checkbox(checked=note.id in selectedNotes,onCheckedChange={checked->selectedNotes=if(checked) selectedNotes+note.id else selectedNotes-note.id})
                                if(note.noteLocked) Text(neoLabel("🔒 ملاحظة محمية","🔒 Protected note"),style=MaterialTheme.typography.titleMedium)
                                else {
                                    Text((if(note.notePinned) "📌 " else "")+note.title.ifBlank {notePlainText(note.details).lineSequence().firstOrNull {it.isNotBlank()}?.take(100) ?: neoLabel("بدون عنوان","Untitled")},style=MaterialTheme.typography.titleMedium)
                                    Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT,if(LocalNeoEnglish.current)Locale.ENGLISH else Locale("ar")).format(java.util.Date(note.updatedAt)),style=MaterialTheme.typography.labelSmall,color=contentColor.copy(alpha=0.75f))
                                    if(note.details.isNotBlank()) Text(noteInline(noteDisplayText(note.details).lineSequence().take(3).joinToString("\n")),maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodyMedium)
                                    if(note.noteTags.isNotBlank()) Text(note.noteTags,style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                if(notice!=null) item {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(notice!!,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
                        if(undoCandidate==db && undo.enabled) TextButton(enabled=!write.pending,onClick={undo.perform();notice=null}) {Text(neoLabel("تراجع","Undo"))}
                        IconButton(onClick={notice=null}) {Icon(Icons.Rounded.Close,neoLabel("إغلاق","Close"))}
                    }
                }
            }
        }
        FloatingActionButton(onClick={if(notesTab) {noteId=null;noteEditor=true} else open()},containerColor=NeoPremium.Navy,contentColor=NeoPremium.Gold,modifier=Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Rounded.Add,if(notesTab) neoLabel("إضافة ملاحظة","Add note") else neoLabel("إضافة مهمة","Add task"))
        }
    }
    if(hiddenUnlock) androidx.compose.ui.window.Dialog(onDismissRequest={hiddenUnlock=false},properties=androidx.compose.ui.window.DialogProperties(securePolicy=androidx.compose.ui.window.SecureFlagPolicy.SecureOn)) {
        val security=remember {V11SecurityStore(context.applicationContext)}
        val settings=security.load()
        if(settings.mode==V11LockMode.NONE&&!settings.biometricEnabled) Surface {Text(neoLabel("أعد إعداد المصادقة من الإعدادات لفتح الملاحظات المحمية","Configure authentication in Settings to open protected notes"),Modifier.padding(20.dp))}
        else V11AccountsUnlockScreen(settings,security) {hiddenAuthorized=true;hiddenUnlock=false;noteFolder="hidden"}
    }
    if (noteEditor) {
        val existing = db.dayItems.firstOrNull { it.id == noteId }
        CompositionLocalProvider(LocalNeoOpenNoteTask provides {taskId -> noteEditor=false;noteId=null;open(taskId)}) {
        NeoDayNoteDialog(existing?.date?.let { java.time.LocalDate.parse(it) } ?: today, existing,
            expectedDatabase = db, calendarId = existing?.calendarId ?: db.calendars.firstOrNull { it.visible }?.id,
            onDismiss = { noteEditor = false; noteId = null }) { noteEditor = false; noteId = null }
        }
    }
    if(filters) AlertDialog(onDismissRequest={filters=false},title={Text(neoLiteral("فلاتر وعرض المهام","Task filters and view"))},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            NeoChoice(neoLiteral("الفترة","Date range"),NeoTaskPeriod.entries,period,{periodLabel(it)},{period=it})
            NeoChoice(neoLiteral("الأولوية","Priority"),listOf<TaskPriorityV11?>(null)+TaskPriorityV11.entries,priority,{it?.let { neoPriorityLabel(it) } ?: neoLiteral("أي أولوية","Any priority")},{priority=it})
            NeoChoice(neoLiteral("التقويم","Calendar"),listOf<String?>(null)+db.calendars.map {it.id},calendar,{id-> db.calendars.firstOrNull {it.id==id}?.name ?: neoLiteral("كل التقاويم","All calendars")},{calendar=it})
            NeoChoice(neoLiteral("الترتيب","Sort"),NeoTaskSort.entries,options.sort,{when(it){NeoTaskSort.DATE->neoLiteral("التاريخ","Date");NeoTaskSort.PRIORITY->neoLiteral("الأولوية","Priority");NeoTaskSort.MANUAL->neoLiteral("يدوي","Manual")}},{saveOptions(options.copy(sort=it))})
            NeoChoice(neoLabel("كثافة البطاقات","Card density"),listOf(true,false),options.compact,
                { if(it) neoLabel("مضغوط","Compact") else neoLabel("مريح","Comfortable") },
                {saveOptions(options.copy(compact=it))})
            NeoSwitch(neoLiteral("إخفاء المكتمل تلقائيًا","Hide completed automatically"),options.hideCompleted) {saveOptions(options.copy(hideCompleted=it))}
            TextButton(onClick={period=NeoTaskPeriod.ALL_DATES;status=NeoTaskStatus.ALL;priority=null;calendar=null;search=""}) {Text(neoLiteral("إعادة ضبط الفلاتر","Reset filters"))}
        }
    },confirmButton={TextButton(onClick={filters=false}) {Text(neoLiteral("تم","Done"))}})
    deleteId?.let { id -> db.tasks.firstOrNull {it.id==id}?.let { task ->
        AlertDialog(onDismissRequest={deleteId=null},title={Text(neoLiteral("حذف المهمة؟","Delete task?"))},text={Text(task.title)},
            confirmButton={TextButton(enabled=!write.pending,onClick={commit(db.copy(tasks=db.tasks.filterNot {it.id==id})) {deleteId=null;undoCandidate=db.copy(tasks=db.tasks.filterNot {it.id==id});notice=neoLiteral("حُذفت المهمة","Task deleted")}}) {Text(neoLiteral("حذف","Delete"))}},
            dismissButton={TextButton(onClick={deleteId=null}) {Text(neoLiteral("إلغاء","Cancel"))}})
    } }
    if(editor) {
        val task=db.tasks.firstOrNull {it.id==editingId};val unchanged=rememberNeoDraftUnchanged(editingId ?: "new-task",task);val reject=LocalNeoRejectWrite.current
        TaskEditorDialogV11(task,db,workplaces,{editor=false;editingId=null}) { next ->
            if(!unchanged || (editingId==null && db.tasks.any {it.id==next.id})) reject("Task changed or already saved; draft retained")
            else commit(db.copy(tasks=db.tasks.filterNot {it.id==next.id}+next)) {
                if(next.reminderAt!=null && android.os.Build.VERSION.SDK_INT>=33 && androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                editor=false;editingId=null
            }
        }
    }
}

@Composable
private fun periodLabel(period: NeoTaskPeriod): String = when(period) {
    NeoTaskPeriod.TODAY -> neoLiteral("اليوم","Today");NeoTaskPeriod.UPCOMING -> neoLiteral("القادمة","Upcoming")
    NeoTaskPeriod.OVERDUE -> neoLiteral("المتأخرة","Overdue");NeoTaskPeriod.ALL_DATES -> neoLiteral("كل التواريخ","All dates")
}
