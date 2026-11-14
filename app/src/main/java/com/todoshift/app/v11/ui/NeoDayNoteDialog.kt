package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

val LocalNeoNoteCommitted = staticCompositionLocalOf<(V11Database) -> Unit> { {} }

@Composable
fun NeoDayNoteDialog(date: LocalDate, existing: DayItemV11?, expectedDatabase: V11Database, calendarId: String?, onDismiss: () -> Unit, onSave: (DayItemV11) -> Unit) {
    val context = LocalContext.current
    val security = remember { V11SecurityStore(context.applicationContext) }
    val editorState=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
    val sectionProtection=remember { security.load().let {it.protectTasks||it.protectApplication} }
    var authorized by remember(existing?.id) { mutableStateOf(existing?.notePrivate != true&&!sectionProtection) }
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,existing?.id) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP&&(existing?.notePrivate==true||sectionProtection)) authorized=false}
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
    editorState.SaveableStateProvider(existing?.id ?: "new-note") {NeoRichNoteDialog(date, existing, expectedDatabase, calendarId, onDismiss, onSave)}
}

@Composable
private fun NeoRichNoteDialog(date: LocalDate, existing: DayItemV11?, expectedDatabase: V11Database, calendarId: String?, onDismiss: () -> Unit, onSave: (DayItemV11) -> Unit) {
    val context=LocalContext.current
    val styleStore=remember { NeoSmartStore(context.applicationContext) }
    val drafts=remember { NeoNoteDraftStore(context.applicationContext) }
    val expectedMetadata=remember { styleStore.snapshot().toString() }
    val expected=remember { expectedDatabase }
    val committed=LocalNeoNoteCommitted.current
    val scope=rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    val rootWritePending=LocalNeoEditorWriteState.current.pending
    var id by rememberSaveable(existing?.id) { mutableStateOf(existing?.id ?: UUID.randomUUID().toString()) }
    val draftKey=remember { existing?.id ?: "new-$date-${calendarId?.hashCode() ?: 0}" }
    val noteUnchanged=rememberNeoDraftUnchanged("note-$id", expectedDatabase.dayItems.firstOrNull { it.id==id })
    val styleUnchanged=rememberNeoDraftUnchanged("note-style-$id", expectedMetadata)
    var color by rememberSaveable { mutableStateOf(styleStore.noteColor(id)) }
    var border by rememberSaveable { mutableStateOf(styleStore.snapshot().optJSONObject("noteBorders")?.optBoolean(id,false) ?: false) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var body by rememberSaveable(stateSaver=TextFieldValue.Saver) { mutableStateOf(TextFieldValue(existing?.details.orEmpty())) }
    var tags by rememberSaveable { mutableStateOf(existing?.noteTags.orEmpty()) }
    var pinned by rememberSaveable { mutableStateOf(existing?.notePinned ?: false) }
    var archived by rememberSaveable { mutableStateOf(existing?.noteArchived ?: false) }
    var trashed by rememberSaveable { mutableStateOf(existing?.noteTrashed ?: false) }
    var locked by rememberSaveable { mutableStateOf(existing?.noteLocked ?: false) }
    var hidden by rememberSaveable { mutableStateOf(existing?.noteHidden ?: false) }
    var background by rememberSaveable { mutableStateOf(existing?.noteBackground.orEmpty()) }
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
    val sourceRevision=remember { existing?.toString() ?: "<new>" }
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
    LaunchedEffect(title,body.text,tags,pinned,archived,trashed,locked,hidden,background,color,border,reminder,taskSpecs,draftReady,pending) {
        if(!draftReady || pending) return@LaunchedEffect
        delay(600)
        try {
            val value=JSONObject().put("id",id).put("source",sourceRevision).put("title",title).put("details",body.text).put("tags",tags)
                .put("pinned",pinned).put("archived",archived).put("trashed",trashed).put("locked",locked).put("hidden",hidden)
                .put("taskSpecs",org.json.JSONArray(taskSpecs)).put("reminder",reminder).put("background",background).put("color",color).put("border",border)
            withContext(Dispatchers.IO) { drafts.save(draftKey,value) }
            draftStatus="المسودة محفوظة تلقائيًا / Draft autosaved"
        } catch(cancelled: CancellationException) { throw cancelled }
        catch(failure: Exception) { error="Draft autosave failed; keep this editor open: ${failure.message}" }
    }
    val gate=LocalNeoLeaveGate.current
    val leave=rememberNeoEditorExit(listOf(title,body.text,tags,pinned,archived,trashed,locked,hidden,background,color,border,reminder,taskSpecs),onDismiss,pending)
    AlertDialog(onDismissRequest={if(!pending&&!rootWritePending) leave()},
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
                if(!pending) {val spec="$line\n$label";taskSpecs=(taskSpecs+spec).distinct();draftStatus="تم ربط بند القائمة؛ ستنشأ المهمة عند الحفظ / Checklist linked; task is created when saved"}
            }) else OutlinedTextField(body,{if(!pending) edit(it)},label={Text(neoLabel("النص والتنسيق","Text and formatting"))},minLines=6,maxLines=16,modifier=Modifier.fillMaxWidth())
            if(taskSpecs.isNotEmpty()) Text(neoLabel("${taskSpecs.size} بنود مرتبطة بمهام عند الحفظ","${taskSpecs.size} checklist links to save"),style=MaterialTheme.typography.labelSmall)
            expectedDatabase.tasks.filter {it.linkedNoteId==id}.forEach {task->Text(neoLabel("مهمة مرتبطة: ","Linked task: ")+task.title,style=MaterialTheme.typography.labelMedium)}
            if(options) {
                OutlinedTextField(reminder,{reminder=it},label={Text(neoLabel("تذكير اختياري: 2026-11-14T18:30","Optional reminder: 2026-11-14T18:30"))})
                OutlinedTextField(tags,{tags=it},label={Text(neoLabel("وسوم مفصولة بفواصل","Tags, comma separated"))})
                NeoSwitch(neoLabel("تثبيت","Pin"),pinned){pinned=it}
                NeoSwitch(neoLabel("أرشفة","Archive"),archived){archived=it}
                NeoSwitch(neoLabel("سلة المهملات (يمكن الاستعادة)","Trash (recoverable)"),trashed){trashed=it}
                val security=remember { V11SecurityStore(context.applicationContext) }
                fun canProtect(): Boolean { val settings=security.load();return settings.mode!=V11LockMode.NONE || settings.biometricEnabled }
                NeoSwitch(neoLabel("قفل الملاحظة","Lock note"),locked){if(!it||canProtect())locked=it else error="Configure app PIN or biometrics in Settings before protecting a note"}
                NeoSwitch(neoLabel("إخفاء الملاحظة","Hide note"),hidden){if(!it||canProtect())hidden=it else error="Configure app PIN or biometrics in Settings before hiding a note"}
                OutlinedTextField(background,{background=it},label={Text(neoLabel("خلفية #RRGGBB أو فارغ للثيم","Background #RRGGBB or blank for theme"))},isError=background.isNotBlank()&&!background.matches(Regex("#[0-9a-fA-F]{6}")))
                OutlinedTextField(color,{color=it},label={Text(neoLabel("لون التقويم #RRGGBB","Calendar color #RRGGBB"))})
                NeoSwitch(neoLabel("حد حول النص","Text border"),border){border=it}
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
        confirmButton={leave.SaveButton(enabled=!pending&&!rootWritePending&&draftReady&&(title.isNotBlank()||body.text.isNotBlank())&&color.matches(Regex("#[0-9a-fA-F]{6}"))&&(background.isBlank()||background.matches(Regex("#[0-9a-fA-F]{6}"))),onClick={
            if(!noteUnchanged||!styleUnchanged) {error="Note changed elsewhere; draft retained. Reopen before saving.";return@SaveButton}
            val reminderAt=if(reminder.isBlank()) null else try {java.time.LocalDateTime.parse(reminder).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()} catch(failure:Exception) {error="Invalid reminder date and time";return@SaveButton}
            val note=(existing?.takeIf {it.id==id}?:DayItemV11(id,date.toString(),calendarId,DayItemTypeV11.NOTE,""))
                .copy(title=title.trim(),details=body.text,reminderAt=reminderAt,updatedAt=System.currentTimeMillis(),noteTags=tags,notePinned=pinned,noteArchived=archived,noteTrashed=trashed,noteLocked=locked,noteHidden=hidden,noteBackground=background.takeIf {it.isNotBlank()})
            val linkedTasks=taskSpecs.map {spec->
                val taskId=UUID.nameUUIDFromBytes(("note-checklist:$id:$spec").toByteArray(Charsets.UTF_8)).toString()
                TaskV11(id=taskId,title=spec.substringAfter('\n'),date=date.toString(),calendarId=calendarId,linkedNoteId=id)
            }
            val savedExit=gate?.takeSavedExit();pending=true;error=null
            scope.launch {
                try {
                    val next=withContext(Dispatchers.IO) {com.todoshift.app.backup.Neo4LocalBackup(context.applicationContext).saveNote(expected,expectedMetadata,note,color,border,linkedTasks=linkedTasks)}
                    withContext(Dispatchers.IO) {drafts.remove(draftKey)}
                    committed(next);onSave(note);savedExit?.invoke()
                } catch(failure:Exception) {error=failure.message?:"Note save failed; draft retained"} finally {pending=false}
            }
        }) {Text(neoLabel("حفظ","Save"))}},
        dismissButton={TextButton(enabled=!pending&&!rootWritePending,onClick={leave()}) {Text(neoLabel("إغلاق","Close"))}})
}
