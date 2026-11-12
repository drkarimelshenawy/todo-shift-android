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
    var period by rememberSaveable { mutableStateOf(NeoTaskPeriod.TODAY) }
    var status by rememberSaveable { mutableStateOf(NeoTaskStatus.ALL) }
    var search by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf<TaskPriorityV11?>(null) }
    var calendar by rememberSaveable { mutableStateOf<String?>(null) }
    var filters by rememberSaveable { mutableStateOf(false) };var menu by remember { mutableStateOf(false) }
    var editor by rememberSaveable { mutableStateOf(false) };var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) };var missing by remember { mutableStateOf(false) }
    var undoCandidate by remember { mutableStateOf<V11Database?>(null) };var notice by remember { mutableStateOf<String?>(null) }
    val today=neoNow().toLocalDate();val date=today.toString()
    val visible=remember(db.tasks,db.calendars) {
        val ids=db.calendars.filter { it.visible }.map { it.id }.toSet()
        db.tasks.filter { it.calendarId==null || it.calendarId in ids }
    }
    val tasks=remember(visible,period,status,search,priority,calendar,options,date) {
        NeoTaskQueries.select(visible,date,period,status,search,priority,calendar,options)
    }
    fun open(id: String?=null) { editingId=id;editor=true }
    LaunchedEffect(initialAction,initialTaskId) {
        when(initialAction) { "add" -> open(); "open" -> if(db.tasks.any { it.id==initialTaskId }) open(initialTaskId) else missing=true }
        if(initialAction!=null) onHandled()
    }
    fun changed(next: V11Database, message: String) {
        if(write.pending) return
        commit(next) { undoCandidate=next;notice=message }
    }
    if(missing) AlertDialog(onDismissRequest={missing=false},title={Text(neoLiteral("المهمة غير متاحة","Task unavailable"))},
        confirmButton={TextButton(onClick={missing=false}) {Text(neoLiteral("إغلاق","Close"))}})
    val direction=LocalLayoutDirection.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical=6.dp)) {
                        Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM",if(LocalNeoEnglish.current) Locale.ENGLISH else Locale.forLanguageTag("ar"))),style=MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick={menu=true}) { Icon(Icons.Rounded.MoreVert,neoLiteral("خيارات المهام","Task options")) }
                    DropdownMenu(menu,{menu=false}) {
                        DropdownMenuItem(text={Text(neoLiteral("قوالب المهام","Task templates"))},onClick={menu=false;nav.open("TASK_TEMPLATES")})
                        DropdownMenuItem(text={Text(neoLiteral("مكتبة المرفقات","Attachments"))},onClick={menu=false;nav.open("ATTACHMENTS")})
                        DropdownMenuItem(text={Text(neoLiteral("تراجع عن آخر تعديل","Undo last change"))},enabled=undo.enabled&&!write.pending,onClick={menu=false;undo.perform()})
                    }
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    OutlinedTextField(search,{search=it},label={Text(neoLiteral("بحث في المهام","Search tasks"))},singleLine=true,modifier=Modifier.weight(1f))
                    IconButton(onClick={filters=true}) { Icon(Icons.Rounded.Tune,neoLiteral("بحث وفلاتر","Filters")) }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    NeoTaskStatus.entries.forEach { state -> FilterChip(selected=status==state,onClick={status=state},label={Text(when(state) { NeoTaskStatus.ALL -> neoLiteral("الكل","All");NeoTaskStatus.ACTIVE -> neoLiteral("نشطة","Active");NeoTaskStatus.COMPLETED -> neoLiteral("مكتملة","Done") })}) }
                    TextButton(onClick={filters=true}) {Text(periodLabel(period))}
                }
            }
            val due=visible.filter { it.date==date };val done=due.count { it.status==TaskStatusV11.COMPLETED }
            if(due.isNotEmpty()) {
                Text(neoLiteral("$done من ${due.size} مكتملة اليوم","$done of ${due.size} completed today"),style=MaterialTheme.typography.labelSmall)
                LinearProgressIndicator(progress={done.toFloat()/due.size},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp))
            }
            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
            CompositionLocalProvider(LocalNeoTaskCompact provides options.compact,
                LocalNeoTaskMove provides if(options.sort==NeoTaskSort.MANUAL && optionsReady && !settingsBusy) { { id,delta ->
                    saveOptions(options.copy(order=NeoTaskQueries.move(db.tasks,options.order,tasks,id,delta)))
                } } else null) {
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(top=6.dp,bottom=88.dp),verticalArrangement=Arrangement.spacedBy(if(options.compact) 5.dp else 10.dp)) {
                    if(tasks.isEmpty()) item { NeoEmptyState(if(search.isBlank() && priority==null && calendar==null && period==NeoTaskPeriod.TODAY && status==NeoTaskStatus.ALL) neoLiteral("لا توجد مهام لليوم","No tasks for today") else neoLiteral("لا توجد مهام مطابقة","No matching tasks"),neoLiteral("غيّر التاريخ أو الفلاتر، أو أضف مهمة","Change the date or filters, or add a task")) }
                    items(tasks,key={it.id}) { task ->
                        TaskCardV11(task,onToggleComplete={
                            val complete=task.status!=TaskStatusV11.COMPLETED;val now=System.currentTimeMillis()
                            changed(db.copy(tasks=db.tasks.map { if(it.id==task.id) it.copy(status=if(complete) TaskStatusV11.COMPLETED else TaskStatusV11.TODO,completedAt=if(complete) now else null,updatedAt=now) else it }),neoLiteral("تم تحديث المهمة","Task updated"))
                        },onEdit={open(task.id)},onDelete={deleteId=task.id})
                    }
                }
            }
            if(notice!=null) Surface(tonalElevation=3.dp,shape=MaterialTheme.shapes.medium) {
                Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(notice!!,Modifier.weight(1f),style=MaterialTheme.typography.labelMedium)
                    if(undoCandidate==db && undo.enabled) TextButton(enabled=!write.pending,onClick={undo.perform();notice=null}) {Text(neoLiteral("تراجع","Undo"))}
                    IconButton(onClick={notice=null}) {Icon(Icons.Rounded.Close,neoLiteral("إغلاق","Close"))}
                }
            }
        }
        FloatingActionButton(onClick={open()},modifier=Modifier.align(Alignment.BottomEnd).padding(16.dp)) {Icon(Icons.Rounded.Add,neoLiteral("إضافة مهمة","Add task"))}
    }
    if(filters) AlertDialog(onDismissRequest={filters=false},title={Text(neoLiteral("فلاتر وعرض المهام","Task filters and view"))},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            NeoChoice(neoLiteral("الفترة","Date range"),NeoTaskPeriod.entries,period,{periodLabel(it)},{period=it})
            NeoChoice(neoLiteral("الأولوية","Priority"),listOf<TaskPriorityV11?>(null)+TaskPriorityV11.entries,priority,{it?.let { neoPriorityLabel(it) } ?: neoLiteral("أي أولوية","Any priority")},{priority=it})
            NeoChoice(neoLiteral("التقويم","Calendar"),listOf<String?>(null)+db.calendars.map {it.id},calendar,{id-> db.calendars.firstOrNull {it.id==id}?.name ?: neoLiteral("كل التقاويم","All calendars")},{calendar=it})
            NeoChoice(neoLiteral("الترتيب","Sort"),NeoTaskSort.entries,options.sort,{when(it){NeoTaskSort.DATE->neoLiteral("التاريخ","Date");NeoTaskSort.PRIORITY->neoLiteral("الأولوية","Priority");NeoTaskSort.MANUAL->neoLiteral("يدوي","Manual")}},{saveOptions(options.copy(sort=it))})
            NeoSwitch(neoLiteral("بطاقات مضغوطة","Compact cards"),options.compact) {saveOptions(options.copy(compact=it))}
            NeoSwitch(neoLiteral("إخفاء المكتمل تلقائيًا","Hide completed automatically"),options.hideCompleted) {saveOptions(options.copy(hideCompleted=it))}
            TextButton(onClick={period=NeoTaskPeriod.TODAY;status=NeoTaskStatus.ALL;priority=null;calendar=null;search=""}) {Text(neoLiteral("إعادة ضبط الفلاتر","Reset filters"))}
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
