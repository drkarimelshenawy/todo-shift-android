package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.*
import org.json.JSONObject

val LocalNeoOpenNoteTask = staticCompositionLocalOf<((String) -> Unit)?> {null}

val LocalNeoNoteCommitted = staticCompositionLocalOf<(V11Database) -> Unit> { {} }

@Composable
fun NeoDayNoteDialog(date: LocalDate, existing: DayItemV11?, expectedDatabase: V11Database, calendarId: String?, onDismiss: () -> Unit, onSave: (DayItemV11) -> Unit) {
    val context = LocalContext.current
    val security = remember { V11SecurityStore(context.applicationContext) }
    val editorState=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val sectionProtection=security.load().let {it.protectTasks||it.protectNotes||it.protectApplication}
    var editorLocked by remember(existing?.id) {mutableStateOf(existing?.noteLocked==true)}
    var authorized by remember(existing?.id) { mutableStateOf(existing?.noteLocked != true&&!sectionProtection) }
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var backgroundAt by remember {mutableLongStateOf(0L)}
    DisposableEffect(lifecycle,existing?.id,editorLocked,sectionProtection) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                backgroundAt=System.currentTimeMillis()
                if((editorLocked||sectionProtection)&&security.load().autoLockMinutes==0) authorized=false
            }
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START && backgroundAt>0 && (editorLocked||sectionProtection) && System.currentTimeMillis()-backgroundAt>=security.load().autoLockMinutes*60000L) authorized=false
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    if (!authorized) {
        Dialog(onDismissRequest=onDismiss, properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn)) {
            val settings=security.load()
            if(settings.mode==V11LockMode.NONE&&!settings.biometricEnabled) Surface { Text(neoLabel("الملاحظة محمية. أعد إعداد المصادقة من الإعدادات.","This note remains protected. Configure authentication in Settings."),Modifier.padding(20.dp)) }
            else V11AccountsUnlockScreen(settings, security) { authorized=true }
        }
        return
    }
    editorState.SaveableStateProvider(existing?.id ?: "new-note") {NeoRichNoteDialog(date, existing, expectedDatabase, calendarId, onDismiss, onSave) {editorLocked=it}}
}

@Composable
private fun NeoRichNoteDialog(date: LocalDate, existing: DayItemV11?, expectedDatabase: V11Database, calendarId: String?, onDismiss: () -> Unit, onSave: (DayItemV11) -> Unit, onProtectionChanged:(Boolean)->Unit) {
    val context=LocalContext.current
    val styleStore=remember { NeoSmartStore(context.applicationContext) }
    val drafts=remember { NeoNoteDraftStore(context.applicationContext) }
    var expectedMetadata by remember { mutableStateOf(styleStore.snapshot().toString()) }
    var expected by remember { mutableStateOf(expectedDatabase) }
    val committed=LocalNeoNoteCommitted.current
    val scope=rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    val rootWritePending=LocalNeoEditorWriteState.current.pending
    var id by rememberSaveable(existing?.id) { mutableStateOf(existing?.id ?: UUID.randomUUID().toString()) }
    val draftKey=remember { existing?.id ?: "new-$date-${calendarId?.hashCode() ?: 0}" }


    var color by rememberSaveable { mutableStateOf(styleStore.noteColor(id)) }
    var border by rememberSaveable { mutableStateOf(styleStore.snapshot().optJSONObject("noteBorders")?.optBoolean(id,false) ?: false) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var body by rememberSaveable(stateSaver=TextFieldValue.Saver) { mutableStateOf(TextFieldValue(existing?.details.orEmpty())) }
    var tags by rememberSaveable { mutableStateOf(existing?.noteTags.orEmpty()) }
    var pinned by rememberSaveable { mutableStateOf(existing?.notePinned ?: false) }
    var archived by rememberSaveable { mutableStateOf(existing?.noteArchived ?: false) }
    var confirmTrashNote by remember {mutableStateOf(false)}
    var trashed by rememberSaveable { mutableStateOf(existing?.noteTrashed ?: false) }
    var locked by rememberSaveable { mutableStateOf(existing?.noteLocked ?: false) }
    var hidden by rememberSaveable { mutableStateOf(existing?.noteHidden ?: false) }
    var background by rememberSaveable { mutableStateOf(existing?.noteBackground.orEmpty()) }
    var detachedTasks by rememberSaveable {mutableStateOf(listOf<String>())}
    var existingTaskLinks by rememberSaveable {mutableStateOf(listOf<String>())}
    var selectedTaskLink by remember {mutableStateOf<String?>(null)}
    var taskSpecs by rememberSaveable { mutableStateOf(listOf<String>()) }
    var reminder by rememberSaveable { mutableStateOf(existing?.reminderAt?.let {java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it),java.time.ZoneId.systemDefault()).toString()}.orEmpty()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable { mutableStateOf(false) }
    var draftReady by rememberSaveable { mutableStateOf(false) }
    var draftStatus by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var conflictingDraft by remember { mutableStateOf<JSONObject?>(null) }
    val undo=remember { mutableStateListOf<TextFieldValue>() }; val redo=remember { mutableStateListOf<TextFieldValue>() }
    fun edit(next: TextFieldValue) {
        if(next.text!=body.text) { undo.add(body); if(undo.size>50) undo.removeAt(0); redo.clear() }
        body=next
    }
    var sourceRevision by remember { mutableStateOf(existing?.toString() ?: "<new>") }
    LaunchedEffect(draftKey) {
        if(draftReady) return@LaunchedEffect
        try {
            val recovered=withContext(Dispatchers.IO) { drafts.read(draftKey) }
            if(recovered!=null) {
                if(recovered.optString("source")==sourceRevision) {
                    id=recovered.optString("id",id); title=recovered.optString("title",title)
                    body=TextFieldValue(recovered.optString("details",body.text)); tags=recovered.optString("tags",tags)
                    pinned=recovered.optBoolean("pinned",pinned); archived=recovered.optBoolean("archived",archived)
                    trashed=recovered.optBoolean("trashed",trashed); locked=recovered.optBoolean("locked",locked);hidden=recovered.optBoolean("hidden",hidden)
                    detachedTasks=recovered.optJSONArray("detachedTasks")?.let {a->(0 until a.length()).map {a.getString(it)}} ?: emptyList()
                    existingTaskLinks=recovered.optJSONArray("existingTaskLinks")?.let {a->(0 until a.length()).map {a.getString(it)}} ?: emptyList()
                    taskSpecs=recovered.optJSONArray("taskSpecs")?.let { array->(0 until array.length()).map {array.getString(it)} } ?: taskSpecs
                    reminder=recovered.optString("reminder",reminder);background=recovered.optString("background",background);color=recovered.optString("color",color);border=recovered.optBoolean("border",border)
                    draftStatus="تم استرجاع المسودة / Draft recovered"
                } else {
                    conflictingDraft=recovered
                    error="Stored note changed since this draft. Recover the draft as a separate note to preserve both versions."
                }
            }
            draftReady=error==null
        } catch(failure: Exception) { error="Unable to recover note draft: ${failure.message}" }
    }
    suspend fun persistNote(close: Boolean): Boolean {
        if(pending || rootWritePending || !draftReady || conflictingDraft!=null) return false
        if(title.isBlank() && body.text.isBlank() && expected.dayItems.none {it.id==id}) {if(close) onDismiss();return true}
        if(!color.matches(Regex("#[0-9a-fA-F]{6}")) || (background.isNotBlank() && !background.matches(Regex("#[0-9a-fA-F]{6}")))) {
            error="اختر لونًا صحيحًا / Choose a valid color";return false
        }
        val reminderAt=if(reminder.isBlank()) null else try {java.time.LocalDateTime.parse(reminder).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()} catch(_:Exception) {error="تاريخ التذكير غير صحيح / Invalid reminder date";return false}
        val note=(expected.dayItems.firstOrNull {it.id==id} ?: DayItemV11(id,date.toString(),calendarId,DayItemTypeV11.NOTE,""))
            .copy(title=title.trim(),details=body.text,reminderAt=reminderAt,updatedAt=System.currentTimeMillis(),noteTags=tags,notePinned=pinned,noteArchived=archived,noteTrashed=trashed,noteLocked=locked,noteHidden=hidden,noteBackground=background.takeIf {it.isNotBlank()})
        val linkedTasks=taskSpecs.map {spec ->
            val key=spec.substringBefore('\n'); val label=spec.substringAfter('\n')
            val taskId=UUID.nameUUIDFromBytes(("note-checklist:$id:$key").toByteArray(Charsets.UTF_8)).toString()
            expected.tasks.firstOrNull {(it.id==taskId||it.id==key)&&it.linkedNoteId==id} ?: TaskV11(id=taskId,title=label,date=date.toString(),calendarId=calendarId,linkedNoteId=id)
        }
        val linkedExisting=existingTaskLinks.mapNotNull {key->expected.tasks.firstOrNull {it.id==key}?.copy(linkedNoteId=id)}
        pending=true
        return try {
            val next=withContext(Dispatchers.IO) {com.todoshift.app.backup.Neo4LocalBackup(context.applicationContext).saveNote(expected,expectedMetadata,note,color,border,linkedTasks=(linkedTasks+linkedExisting).distinctBy {it.id}.filterNot {it.id in detachedTasks},detachedTaskIds=detachedTasks.toSet())}
            expected=next;expectedMetadata=withContext(Dispatchers.IO) {styleStore.snapshot().toString()};sourceRevision=note.toString()
            detachedTasks=emptyList();existingTaskLinks=emptyList()
            onProtectionChanged(note.noteLocked);committed(next);draftStatus="تم الحفظ / Saved";error=null
            withContext(Dispatchers.IO) {drafts.remove(draftKey)}
            if(close) onSave(note)
            true
        } catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {error="تعذر الحفظ؛ التعديلات والمسودة محفوظة لإعادة المحاولة / Save failed; edits and draft retained";false}
        finally {pending=false}
    }
    suspend fun keepDraft() {
      try {
        val value=JSONObject().put("id",id).put("source",sourceRevision).put("title",title).put("details",body.text).put("tags",tags)
            .put("pinned",pinned).put("archived",archived).put("trashed",trashed).put("locked",locked).put("hidden",hidden)
            .put("taskSpecs",org.json.JSONArray(taskSpecs)).put("detachedTasks",org.json.JSONArray(detachedTasks)).put("existingTaskLinks",org.json.JSONArray(existingTaskLinks)).put("reminder",reminder).put("background",background).put("color",color).put("border",border)
        withContext(Dispatchers.IO+NonCancellable) { drafts.save(draftKey,value) }
      } catch(_:Exception) {error="تعذر حفظ المسودة؛ لن تُحذف التعديلات / Draft save failed; edits retained"}
    }
    LaunchedEffect(title,body.text,tags,pinned,archived,trashed,locked,hidden,background,color,border,reminder,taskSpecs,detachedTasks,existingTaskLinks,draftReady) {
        if(!draftReady || pending) return@LaunchedEffect
        delay(600)
        try { keepDraft();withContext(NonCancellable) {persistNote(false)} }
        catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {error="تعذر حفظ المسودة؛ احتفظ بالمحرر مفتوحًا / Draft save failed; keep editor open"}
    }
    val latestFlush by rememberUpdatedState<suspend () -> Unit>({if(draftReady) {keepDraft();persistNote(false)}})
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) scope.launch(NonCancellable) { latestFlush() }
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    val gate=LocalNeoLeaveGate.current
    val leave=rememberNeoEditorExit(listOf(title,body.text,tags,pinned,archived,trashed,locked,hidden,background,color,border,reminder,taskSpecs),onDismiss,pending)
    if(confirmTrashNote) AlertDialog(onDismissRequest={confirmTrashNote=false},title={Text(neoLabel("نقل الملاحظة للمهملات؟","Move note to Trash?"))},text={Text(neoLabel("يمكن استعادتها لاحقًا","You can restore it later"))},confirmButton={TextButton(onClick={trashed=true;confirmTrashNote=false}) {Text(neoLabel("نقل","Move"))}},dismissButton={TextButton(onClick={confirmTrashNote=false}) {Text(neoLabel("إلغاء","Cancel"))}})
    val effectiveBackground=(background.ifBlank {NeoLaunchPreferences(context).defaultNoteBackground}).takeIf {it.isNotBlank()}?.let {runCatching {android.graphics.Color.parseColor(it)}.getOrNull()}
    AlertDialog(containerColor=effectiveBackground?.let {androidx.compose.ui.graphics.Color(it)} ?: MaterialTheme.colorScheme.surface,
        textContentColor=effectiveBackground?.let {if(androidx.core.graphics.ColorUtils.calculateLuminance(it)>0.45) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White} ?: MaterialTheme.colorScheme.onSurface,
        onDismissRequest={if(!pending&&!rootWritePending) scope.launch {keepDraft();if(persistNote(true)) onDismiss()}},
        properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn),
        title={Text(neoLabel("ملاحظة", "Note")+" • $date")},
        text={Column(Modifier.heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(title,{if(!pending) title=it},label={Text(neoLabel("عنوان اختياري","Optional title"))},modifier=Modifier.fillMaxWidth())
            NeoNoteFormatting(body,{if(!pending) edit(it)},undo={if(undo.isNotEmpty()){redo.add(body);body=undo.removeAt(undo.lastIndex)}},redo={if(redo.isNotEmpty()){undo.add(body);body=redo.removeAt(redo.lastIndex)}},canUndo=undo.isNotEmpty()&&!pending,canRedo=redo.isNotEmpty()&&!pending)
            Row {
                FilterChip(preview,{preview=!preview},label={Text(neoLabel("معاينة وتحديد القوائم","Preview / checklists"))})
                TextButton(onClick={options=!options}) {Text(neoLabel("تنظيم ومظهر","Organize / style"))}
            }
            if(preview) NeoNotePreview(body.text, onChecklist={ line ->
                if(!pending) { val lines=body.text.lines().toMutableList();val old=lines[line]; lines[line]=if(old.contains("[ ]")) old.replaceFirst("[ ]","[x]") else old.replaceFirst(Regex("\\[[xX]\\]"),"[ ]");edit(TextFieldValue(lines.joinToString("\n"))) }
            },onCreateTask={line,label->
                if(!pending) {val lines=body.text.lines().toMutableList()
                    val found=Regex("<!--task:([A-Za-z0-9_-]+)-->").find(lines[line])
                    val key=found?.groupValues?.get(1) ?: expected.tasks.firstOrNull {it.linkedNoteId==id&&it.title==label}?.id ?: UUID.randomUUID().toString()
                    if(found==null) {lines[line]+=" <!--task:$key-->";edit(TextFieldValue(lines.joinToString("\n")))}
                    val spec="$key\n$label";taskSpecs=taskSpecs.filterNot {it.substringBefore('\n')==key}+spec}
            }) else OutlinedTextField(body,{if(!pending) edit(it)},label={Text(neoLabel("النص والتنسيق","Text and formatting"))},visualTransformation=remember {NeoNoteVisualTransformation()},minLines=6,maxLines=16,modifier=Modifier.fillMaxWidth())
            if(taskSpecs.isNotEmpty()) Text(neoLabel("${taskSpecs.size} بنود مرتبطة بمهام عند الحفظ","${taskSpecs.size} checklist links to save"),style=MaterialTheme.typography.labelSmall)
            val openTask=LocalNeoOpenNoteTask.current
            expected.tasks.filter {it.linkedNoteId==id && it.id !in detachedTasks}.forEach {task ->
                Row {
                    TextButton(enabled=openTask!=null&&!pending,onClick={scope.launch {keepDraft();if(persistNote(false)) openTask?.invoke(task.id)}}) {Text(neoLabel("فتح المهمة: ","Open task: ")+task.title)}
                    TextButton(enabled=!pending,onClick={
                        detachedTasks=detachedTasks+task.id
                        val tokens=Regex("<!--task:([A-Za-z0-9_-]+)-->").findAll(body.text).filter {match ->
                            val key=match.groupValues[1];key==task.id || UUID.nameUUIDFromBytes(("note-checklist:$id:$key").toByteArray(Charsets.UTF_8)).toString()==task.id
                        }.map {it.value}.toList()
                        var text=body.text;tokens.forEach {text=text.replace(it,"")};edit(TextFieldValue(text))
                        taskSpecs=taskSpecs.filterNot {spec->val key=spec.substringBefore('\n');key==task.id || UUID.nameUUIDFromBytes(("note-checklist:$id:$key").toByteArray(Charsets.UTF_8)).toString()==task.id}
                    }) {Text(neoLabel("إزالة الربط","Unlink"))}
                }
            }
            if(options) {
                val linkable=expected.tasks.filter {it.linkedNoteId==null && it.id !in existingTaskLinks}
                if(linkable.isNotEmpty()) {
                    NeoChoice(neoLabel("ربط مهمة موجودة","Link existing task"),listOf<String?>(null)+linkable.map {it.id},selectedTaskLink,{key->linkable.firstOrNull {it.id==key}?.title ?: neoLabel("اختر مهمة","Choose task")},{selectedTaskLink=it})
                    TextButton(enabled=selectedTaskLink!=null,onClick={existingTaskLinks=existingTaskLinks+requireNotNull(selectedTaskLink);selectedTaskLink=null}) {Text(neoLabel("ربط","Link"))}
                }
                OutlinedTextField(reminder,{reminder=it},label={Text(neoLabel("تذكير اختياري: 2026-11-14T18:30","Optional reminder: 2026-11-14T18:30"))})
                if(reminder.isNotBlank()) NeoItemReminderRecurrenceControls(id)
                OutlinedTextField(tags,{tags=it},label={Text(neoLabel("وسوم مفصولة بفواصل","Tags, comma separated"))})
                NeoSwitch(neoLabel("تثبيت","Pin"),pinned){pinned=it}
                NeoSwitch(neoLabel("أرشفة","Archive"),archived){archived=it}
                NeoSwitch(neoLabel("سلة المهملات (يمكن الاستعادة)","Trash (recoverable)"),trashed){if(it) confirmTrashNote=true else trashed=false}
                val security=remember { V11SecurityStore(context.applicationContext) }
                fun canProtect(): Boolean { val settings=security.load();return settings.mode!=V11LockMode.NONE || settings.biometricEnabled }
                NeoSwitch(neoLabel("قفل الملاحظة","Lock note"),locked){if(!it||canProtect())locked=it else error="Configure app PIN or biometrics in Settings before protecting a note"}
                NeoSwitch(neoLabel("إخفاء الملاحظة","Hide note"),hidden){hidden=it}
                Text(neoLabel("خلفية الملاحظة","Note background"))
                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("" to neoLabel("افتراضي","Default"),"#FFF4B8" to neoLabel("أصفر","Yellow"),"#D9EFDA" to neoLabel("أخضر","Green"),"#D6E9FF" to neoLabel("أزرق","Blue"),"#F9DCE9" to neoLabel("وردي","Pink"),"#E8DDF5" to neoLabel("بنفسجي","Purple"),"#FFE1C3" to neoLabel("برتقالي","Orange"),"#E3E6EB" to neoLabel("رمادي","Gray")).forEach {(value,label) -> FilterChip(background==value,{background=value},label={Text(label)},colors=FilterChipDefaults.filterChipColors(containerColor=if(value.isBlank()) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(value))))}
                }
                OutlinedTextField(color,{color=it},label={Text(neoLabel("لون التقويم #RRGGBB","Calendar color #RRGGBB"))})
                NeoSwitch(neoLabel("حد حول النص","Text border"),border){border=it}
            }
            Row {
                TextButton(onClick={val content=noteDisplayText(body.text);(context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText(title,content))}) {Text(neoLabel("نسخ","Copy"))}
                TextButton(onClick={context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,listOf(title,noteDisplayText(body.text)).filter {it.isNotBlank()}.joinToString("\n")),null))}) {Text(neoLabel("مشاركة","Share"))}
            }
            Text(draftStatus,style=MaterialTheme.typography.labelSmall)
            conflictingDraft?.let { recovered ->
                TextButton(onClick={
                    id=UUID.randomUUID().toString();title=recovered.optString("title");body=TextFieldValue(recovered.optString("details"));tags=recovered.optString("tags")
                    taskSpecs=emptyList();conflictingDraft=null;error=null;draftReady=true
                }) {Text(neoLabel("استرجاع المسودة كملاحظة جديدة","Recover draft as a new note"))}
            }
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }},
        confirmButton={TextButton(enabled=!pending&&!rootWritePending&&draftReady,onClick={scope.launch {keepDraft();if(persistNote(true)) gate?.takeSavedExit()?.invoke()}}) {Text(neoLabel("تم","Done"))}},
        dismissButton={TextButton(enabled=!pending&&!rootWritePending,onClick={scope.launch {keepDraft();if(persistNote(true)) onDismiss()}}) {Text(neoLabel("إغلاق","Close"))}})
}
