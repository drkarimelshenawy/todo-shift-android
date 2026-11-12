package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import com.todoshift.app.backup.Neo4DataGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun NeoTaskTemplatesScreen(db: V11Database, workplaces: List<Workplace>, onChange: (V11Database)->Unit, onBack: ()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val store=remember { NeoSmartStore(context.applicationContext) }
    val request=LocalNeoCommitRequest.current;val write=LocalNeoEditorWriteState.current
    val leaveGate=LocalNeoLeaveGate.current
    var templates by remember { mutableStateOf(emptyList<NeoSmartStore.TaskTemplate>()) }
    var ready by remember { mutableStateOf(false) };var busy by remember { mutableStateOf(false) };var error by remember { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf(false) };var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") };var titles by rememberSaveable { mutableStateOf("") }
    var workplace by rememberSaveable { mutableStateOf<String?>(null) };var calendar by rememberSaveable { mutableStateOf<String?>(null) };var kind by rememberSaveable { mutableStateOf<ShiftKind?>(null) }
    var usingId by rememberSaveable { mutableStateOf<String?>(null) };var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var deleting by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { templates=withContext(Dispatchers.IO) { store.templates() };ready=true }
        catch(failure: Exception) { error=failure.message }
    }
    fun mutate(action: ()->Unit, after: ()->Unit={}) {
        if(!ready||busy) return
        val expected=templates;busy=true
        scope.launch {
            try {
                templates=withContext(Dispatchers.IO) { synchronized(Neo4DataGate.monitor) {
                    check(store.templates()==expected) { "Templates changed elsewhere; reopen before editing" }
                    action();store.templates()
                } };after()
            } catch(failure: Exception) { error=failure.message }
            finally { busy=false }
        }
    }
    fun start(template: NeoSmartStore.TaskTemplate?=null) {
        editingId=template?.id;name=template?.name.orEmpty();titles=template?.titles?.joinToString("\n").orEmpty()
        workplace=template?.workplaceId;calendar=template?.calendarId;kind=template?.shiftKind;editing=true
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        NeoPageHeader(neoLabel("قوالب المهام","Task templates"),onBack)
        error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
        Button(enabled=ready&&!busy,onClick={start()}) {Text(neoLabel("قالب جديد","New template"))}
        if(ready&&templates.isEmpty()) Text(neoLabel("لا توجد قوالب محفوظة","No saved templates"))
        templates.forEach { template -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(10.dp)) {
                Text(template.name,style=MaterialTheme.typography.titleMedium)
                Text(neoLabel("${template.titles.size} مهمة","${template.titles.size} tasks"),style=MaterialTheme.typography.labelSmall)
                Row {
                    TextButton(enabled=!busy,onClick={start(template)}) {Text(neoLabel("تعديل","Edit"))}
                    TextButton(enabled=!busy,onClick={mutate({store.saveTemplate(template.name+" • "+neoLiteral("نسخة","Copy"),template.titles,template.workplaceId,template.calendarId,template.shiftKind)})}) {Text(neoLabel("نسخ","Duplicate"))}
                    TextButton(enabled=!busy,onClick={deleting=template.id}) {Text(neoLabel("حذف","Delete"))}
                }
                OutlinedButton(enabled=!busy&&!write.pending,onClick={usingId=template.id;workplace=template.workplaceId;calendar=template.calendarId;kind=template.shiftKind}) {Text(neoLabel("استخدام القالب","Use template"))}
            }
        } }
    }
    if(editing) {
        val leave=rememberNeoEditorExit(listOf(name,titles,workplace,calendar,kind),{editing=false},busy)
        AlertDialog(onDismissRequest={leave()},title={Text(neoLabel("تحرير القالب","Template editor"))},text={
            Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()).imePadding(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name,{name=it.take(100)},label={Text(neoLabel("اسم القائمة","List name"))})
                OutlinedTextField(titles,{titles=it},label={Text(neoLabel("مهمة في كل سطر","One task per line"))},minLines=2,maxLines=5)
                TemplateLinks(db,workplaces,workplace,{workplace=it},calendar,{calendar=it},kind,{kind=it})
            }
        },confirmButton={leave.SaveButton(enabled=ready&&!busy&&name.isNotBlank()&&titles.lines().any {it.isNotBlank()},onClick={
            val exit=leaveGate?.takeSavedExit()
            mutate({store.saveTemplate(name,titles.lines().map {it.trim()}.filter {it.isNotEmpty()},workplace,calendar,kind,editingId)}) {editing=false;exit?.invoke()}
        }) {Text(neoLabel("حفظ القالب","Save template"))}},dismissButton={TextButton(enabled=!busy,onClick={leave()}) {Text(neoLabel("إلغاء","Cancel"))}})
    }
    usingId?.let { id -> templates.firstOrNull {it.id==id}?.let { template ->
        AlertDialog(onDismissRequest={if(!busy) usingId=null},title={Text(neoLabel("إنشاء مهام من القالب","Create tasks from template"))},text={
            Column(Modifier.heightIn(max=430.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(template.name);NeoDateInput(neoLabel("إنشاء في","Create on"),date) {date=it}
                TemplateLinks(db,workplaces,workplace,{workplace=it},calendar,{calendar=it},kind,{kind=it})
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            }
        },confirmButton={Button(enabled=!busy&&!write.pending&&runCatching {LocalDate.parse(date)}.isSuccess,onClick={
            try {
                val next=store.instantiate(template.copy(workplaceId=workplace,calendarId=calendar,shiftKind=kind),LocalDate.parse(date),db)
                if(next==db) error=neoLiteral("تم إنشاء القالب لهذا التاريخ بالفعل","Already created for this date")
                else {
                    busy=true
                    val done: (Boolean)->Unit={saved->busy=false;if(saved) usingId=null}
                    if(request!=null) request(next,done) else {onChange(next);done(true)}
                }
            } catch(failure: Exception) {error=failure.message;busy=false}
        }) {Text(neoLabel("إنشاء المهام","Create tasks"))}},dismissButton={TextButton(enabled=!busy,onClick={usingId=null}) {Text(neoLabel("إلغاء","Cancel"))}})
    } }
    deleting?.let { id -> AlertDialog(onDismissRequest={deleting=null},title={Text(neoLabel("حذف القالب فقط؟","Delete template only?"))},text={Text(neoLabel("المهام المنشأة مسبقًا لن تتغير","Previously created tasks are kept"))},
        confirmButton={TextButton(enabled=!busy,onClick={mutate({store.deleteTemplate(id)}) {deleting=null}}) {Text(neoLabel("حذف","Delete"))}},
        dismissButton={TextButton(onClick={deleting=null}) {Text(neoLabel("إلغاء","Cancel"))}}) }
}

@Composable
private fun TemplateLinks(db: V11Database, places: List<Workplace>, workplace: String?, onPlace: (String?)->Unit,
    calendar: String?, onCalendar: (String?)->Unit, kind: ShiftKind?, onKind: (ShiftKind?)->Unit) {
    NeoChoice(neoLabel("مكان عمل اختياري","Optional workplace"),listOf<String?>(null)+places.map {it.id},workplace,{id->places.firstOrNull {it.id==id}?.name ?: neoLabel("بدون","None")},onPlace)
    NeoChoice(neoLabel("تقويم اختياري","Optional calendar"),listOf<String?>(null)+db.calendars.map {it.id},calendar,{id->db.calendars.firstOrNull {it.id==id}?.name ?: neoLabel("بدون","None")},onCalendar)
    NeoChoice(neoLabel("نوع الشيفت","Shift type"),listOf<ShiftKind?>(null)+ShiftKind.entries,kind,{it?.let {neoShiftLabel(it)} ?: neoLabel("بدون","None")},onKind)
}
