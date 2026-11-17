package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NeoPremiumTasksScreen(db: V11Database, workplaces: List<Workplace>, onChange: (V11Database)->Unit,
    initialAction: String?, initialTaskId: String?, onHandled: ()->Unit,
    initialSharedNoteText:String?=null) {
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
    var notesTab by rememberSaveable {
        mutableStateOf(NeoLaunchPreferences(context).openNotes ||
            initialAction=="new_note" || initialAction=="notes")
    }
    LaunchedEffect(Unit) {if(initialAction==null) notesTab=NeoLaunchPreferences(context).openNotes}
    LaunchedEffect(initialAction,initialTaskId) {
        if(initialAction=="tasks" || (initialAction=="open" && db.tasks.any {it.id==initialTaskId})) notesTab=false
        if(initialAction=="notes" || initialAction=="new_note" ||
            (initialAction=="open" && db.dayItems.any {it.id==initialTaskId && it.type==DayItemTypeV11.NOTE})) notesTab=true
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
    var initialNoteTitle by rememberSaveable {mutableStateOf("")}
    var initialNoteBody by rememberSaveable {mutableStateOf("")}
    var noteSearch by rememberSaveable { mutableStateOf("") }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var noteControlsExpanded by rememberSaveable { mutableStateOf(false) }
    var noteFolder by rememberSaveable { mutableStateOf("active") }
    var noteTag by rememberSaveable {mutableStateOf<String?>(null)}

    var activeFolderId by rememberSaveable {mutableStateOf<String?>(null)}
    var noteFolders by remember {mutableStateOf<List<NeoSmartStore.NoteFolder>>(emptyList())}
    var noteFolderAssignments by remember {mutableStateOf<Map<String,String>>(emptyMap())}
    var favoriteNoteIds by remember {mutableStateOf<Set<String>>(emptySet())}
    var folderRefresh by remember {mutableIntStateOf(0)}
    var folderBusy by remember {mutableStateOf(false)}
    var editFolderDialog by remember {mutableStateOf(false)}
    var editingFolderId by remember {mutableStateOf<String?>(null)}
    var folderName by rememberSaveable {mutableStateOf("")}
    var folderColor by rememberSaveable {mutableStateOf("#D8AD55")}
    var folderLocked by rememberSaveable {mutableStateOf(false)}
    var folderParentChoice by rememberSaveable {mutableStateOf<String?>(null)}
    var folderMoveId by remember {mutableStateOf<String?>(null)}
    var pendingUnlockFolder by remember {mutableStateOf<String?>(null)}
    var unlockedFolderIds by remember {mutableStateOf(setOf<String>())}
    val folderColors=listOf("#D8AD55","#388E9C","#5977B9","#8052A5",
        "#BA6580","#4E9479","#C97B42","#64748B")
    val folderSortPrefs=remember(context) {
        context.applicationContext.getSharedPreferences("neo_note_library_v51",
            android.content.Context.MODE_PRIVATE)
    }
    var foldersFirst by rememberSaveable {
        mutableStateOf(folderSortPrefs.getBoolean("folders_first",true))
    }
    var deleteFolderId by remember {mutableStateOf<String?>(null)}
    var moveNotesDialog by remember {mutableStateOf(false)}
    var newNoteFolderTarget by remember {mutableStateOf<String?>(null)}
    LaunchedEffect(folderRefresh) {
        try {
            val (folders,assignments,favorites)=withContext(Dispatchers.IO) {
                Triple(store.noteFolders(),store.noteFolderAssignments(),store.noteFavorites())
            }
            noteFolders=folders
            noteFolderAssignments=assignments
            favoriteNoteIds=favorites
            if(activeFolderId!=null &&
                activeFolderId !in setOf("__all__","__unfiled__") &&
                folders.none{it.id==activeFolderId}) activeFolderId=null
        } catch(failure:Exception) {
            error=failure.message ?: "Could not load note folders"
        }
    }
    var noteSelectionMode by remember {mutableStateOf(false)}
    var selectedNotes by remember {mutableStateOf(setOf<String>())}
    var selectionActionsExpanded by remember {mutableStateOf(false)}
    fun authorizedFolder(id:String?):Boolean {
        if(id==null || id=="__all__" || id=="__unfiled__")return true
        val index=noteFolders.associateBy{it.id}
        var cursor=id
        val seen=mutableSetOf<String>()
        while(cursor!=null && seen.add(cursor)) {
            val item=index[cursor] ?: return false
            if(item.locked && item.id !in unlockedFolderIds)return false
            cursor=item.parentId
        }
        return cursor==null
    }
    fun enterFolder(id:String?) {
        if(!authorizedFolder(id)) { pendingUnlockFolder=id;return }
        activeFolderId=id;noteSearch="";searchExpanded=false
        selectedNotes=emptySet();noteSelectionMode=false
    }
    fun initFolderEditor(id:String?) {
        val existing=noteFolders.firstOrNull{it.id==id}
        editingFolderId=id
        folderName=existing?.name.orEmpty()
        folderColor=existing?.color ?: "#D8AD55"
        folderLocked=existing?.locked ?: false
        folderParentChoice=existing?.parentId ?: activeFolderId?.takeUnless{
            it=="__all__"||it=="__unfiled__"
        }
        editFolderDialog=true
    }
    var confirmTrash by remember {mutableStateOf(false)}
    val noteViewPrefs=remember(context) { context.applicationContext.getSharedPreferences("neo_note_view_v45",android.content.Context.MODE_PRIVATE) }
    var noteSort by rememberSaveable { mutableStateOf(noteViewPrefs.getString("sort","updated")?.takeIf { it in setOf("updated","title","date") } ?: "updated") }
    var noteLayout by rememberSaveable { mutableStateOf(noteViewPrefs.getString("layout","details")?.takeIf { it in setOf("list","details","grid","large") } ?: "details") }
    var hiddenAuthorized by remember { mutableStateOf(false) }
    var hiddenUnlock by remember { mutableStateOf(false) }
    val noteLifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(noteLifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event->if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP){
            hiddenAuthorized=false;unlockedFolderIds=emptySet()
            if(noteFolder=="hidden")noteFolder="active"
            if(activeFolderId!=null && !authorizedFolder(activeFolderId))activeFolderId=null
        }}
        noteLifecycle.addObserver(observer)
        onDispose {noteLifecycle.removeObserver(observer)}
    }
    var period by rememberSaveable { mutableStateOf(NeoTaskPeriod.TODAY) }
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
    fun open(id: String?=null) { editingId=id;editor=true }
    LaunchedEffect(initialAction,initialTaskId,initialSharedNoteText) {
        when(initialAction) {
            "tasks" -> notesTab=false
            "notes" -> notesTab=true
            "new_note" -> {
                notesTab=true
                noteId=null
                initialNoteTitle=""
                initialNoteBody=initialSharedNoteText.orEmpty().take(16_000)
                newNoteFolderTarget=null
                noteEditor=true
            }
            "add" -> open()
            "open" -> when {
                db.tasks.any {it.id==initialTaskId} -> open(initialTaskId)
                db.dayItems.any {it.id==initialTaskId && it.type==DayItemTypeV11.NOTE} -> {
                    notesTab=true;noteId=initialTaskId;noteEditor=true
                }
                else -> missing=true
            }
        }
        if(initialAction!=null) onHandled()
    }
    fun changed(next: V11Database, message: String) {
        if(write.pending) return
        commit(next) { undoCandidate=next;notice=message }
    }

    fun updateFolder(action:()->Unit,message:String) {
        if(folderBusy||write.pending) return
        folderBusy=true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {action()}
                folderRefresh++
                notice=message
            } catch(failure:Exception) {
                error=failure.message ?: "Folder operation could not be saved"
            } finally {folderBusy=false}
        }
    }
    if(confirmTrash) AlertDialog(onDismissRequest={confirmTrash=false;if(!noteSelectionMode)selectedNotes=emptySet()},title={Text(neoLabel("نقل المحدد إلى المهملات؟","Move selected notes to trash?"))},text={Text(neoLabel("يمكن استعادتها من المهملات","You can restore them from Trash"))},confirmButton={TextButton(onClick={changed(db.copy(dayItems=db.dayItems.map {if(it.id in selectedNotes&&!it.noteLocked) it.copy(noteTrashed=true,updatedAt=System.currentTimeMillis()) else it}),neoLiteral("نُقلت إلى المهملات","Moved to Trash"));selectedNotes=emptySet();noteSelectionMode=false;confirmTrash=false}) {Text(neoLabel("نقل","Move"))}},dismissButton={TextButton(onClick={confirmTrash=false;if(!noteSelectionMode)selectedNotes=emptySet()}) {Text(neoLabel("إلغاء","Cancel"))}})
    if(preferencesOpen) AlertDialog(onDismissRequest={preferencesOpen=false},title={Text(neoLabel("المهام والملاحظات","Tasks & Notes"))},text={Column {NeoLaunchPreferencesUi()}},confirmButton={TextButton(onClick={preferencesOpen=false}) {Text(neoLabel("تم","Done"))}})
    if(missing) AlertDialog(onDismissRequest={missing=false},title={Text(neoLiteral("المهمة غير متاحة","Task unavailable"))},
        confirmButton={TextButton(onClick={missing=false}) {Text(neoLiteral("إغلاق","Close"))}})
    pendingUnlockFolder?.let {target ->
        androidx.compose.ui.window.Dialog(onDismissRequest={pendingUnlockFolder=null}) {
            Surface(shape=MaterialTheme.shapes.large) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(neoLabel("مجلد مقفول","Locked folder"),
                        style=MaterialTheme.typography.titleMedium)
                    if(noteSecuritySettings.mode==V11LockMode.NONE) {
                        Text(neoLabel("فعّل PIN أو النمط من إعدادات الحماية قبل فتح المجلد.",
                            "Configure a PIN or pattern in Security settings to unlock this folder."))
                    } else {
                        V11AccountsUnlockScreen(noteSecuritySettings,noteSecurity) {
                            val index=noteFolders.associateBy{it.id}
                            var cursor:String?=target
                            val unlocked=mutableSetOf<String>()
                            val seen=mutableSetOf<String>()
                            while(cursor!=null && seen.add(cursor)) {
                                unlocked.add(cursor)
                                cursor=index[cursor]?.parentId
                            }
                            unlockedFolderIds=unlockedFolderIds+unlocked
                            pendingUnlockFolder=null
                            enterFolder(target)
                        }
                    }
                    TextButton(onClick={pendingUnlockFolder=null}) {
                        Text(neoLabel("إلغاء","Cancel"))
                    }
                }
            }
        }
    }
    if(editFolderDialog) AlertDialog(
        onDismissRequest={editFolderDialog=false},
        title={Text(if(editingFolderId==null) neoLabel("إنشاء مجلد","New folder")
            else neoLabel("تعديل المجلد","Edit folder"))},
        text={
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value=folderName,onValueChange={folderName=it.take(60)},
                    label={Text(neoLabel("اسم المجلد","Folder name"))},singleLine=true
                )
                Text(neoLabel("لون المجلد","Folder color"),
                    style=MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    folderColors.forEach {hex ->
                        FilterChip(
                            selected=folderColor==hex,onClick={folderColor=hex},
                            label={Text("●",color=androidx.compose.ui.graphics.Color(
                                android.graphics.Color.parseColor(hex)))}
                        )
                    }
                }
                val availableParents=(listOf<NeoSmartStore.NoteFolder?>(null)+
                    noteFolders.filter{it.id!=editingFolderId && authorizedFolder(it.id)})
                NeoChoice(
                    neoLabel("المجلد الأب","Inside folder"),
                    availableParents,availableParents.firstOrNull{it?.id==folderParentChoice},
                    {it?.name ?: neoLabel("المكتبة الرئيسية","Library root")},
                    {folderParentChoice=it?.id}
                )
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=folderLocked,onCheckedChange={folderLocked=it})
                    Text(neoLabel("قفل المجلد","Lock folder"))
                }
                Text(neoLabel(
                    "القفل يتطلب PIN أو نمط حماية مفعّلًا. حذف المجلد ينقل محتوياته إلى المجلد الأب.",
                    "Locking requires a configured PIN or pattern. Deleting moves children to the parent."),
                    style=MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton={
            TextButton(enabled=folderName.isNotBlank()&&!folderBusy&&!write.pending,
                onClick={
                    if(folderLocked && noteSecuritySettings.mode==V11LockMode.NONE) {
                        error=neoLiteral("فعّل PIN أو النمط أولًا","Set up PIN or pattern first")
                        return@TextButton
                    }
                    val id=editingFolderId
                    val name=folderName
                    val color=folderColor
                    val locked=folderLocked
                    val parent=folderParentChoice
                    updateFolder({
                        if(id==null) {
                            store.createNoteFolder(name,parent,color,locked)
                        } else {
                            store.moveNoteFolder(id,parent)
                            store.updateNoteFolder(id,name,color,locked)
                        }
                    },neoLiteral("تم حفظ المجلد","Folder saved"))
                    editFolderDialog=false
                }
            ) {Text(neoLabel("حفظ","Save"))}
        },
        dismissButton={TextButton(onClick={editFolderDialog=false}) {
            Text(neoLabel("إلغاء","Cancel"))
        }}
    )
    deleteFolderId?.let {folderId ->
        AlertDialog(
            onDismissRequest={deleteFolderId=null},
            title={Text(neoLabel("حذف المجلد؟","Delete folder?"))},
            text={Text(neoLabel("الملاحظات ستظل محفوظة في كل الملاحظات. لن يُحذف أي محتوى.",
                "Notes stay in All Notes; no content will be deleted."))},
            confirmButton={
                TextButton(enabled=!folderBusy&&!write.pending,onClick={
                    updateFolder({store.removeNoteFolder(folderId)},
                        neoLiteral("تم حذف المجلد والملاحظات محفوظة","Folder deleted; notes preserved"))
                    if(activeFolderId==folderId) activeFolderId=
                        noteFolders.firstOrNull{it.id==folderId}?.parentId
                    deleteFolderId=null
                }) {Text(neoLabel("حذف المجلد","Delete folder"))}
            },
            dismissButton={TextButton(onClick={deleteFolderId=null}) {Text(neoLabel("إلغاء","Cancel"))}}
        )
    }
    if(moveNotesDialog) AlertDialog(
        onDismissRequest={moveNotesDialog=false;if(!noteSelectionMode)selectedNotes=emptySet()},
        title={Text(neoLabel("نقل الملاحظات إلى مجلد","Move notes to folder"))},
        text={
            Column(Modifier.verticalScroll(rememberScrollState())) {
                (listOf<NeoSmartStore.NoteFolder?>(null)+noteFolders).forEach {folder ->
                    TextButton(enabled=!folderBusy&&!write.pending,onClick={
                        val ids=selectedNotes.filter {id ->
                            db.dayItems.any {it.id==id && it.type==DayItemTypeV11.NOTE && !it.noteLocked}
                        }
                        updateFolder({store.moveNotesToFolder(ids,folder?.id)},
                            neoLiteral("تم نقل الملاحظات","Notes moved"))
                        selectedNotes=emptySet()
                        noteSelectionMode=false
                        moveNotesDialog=false
                    }) {Text(folder?.name ?: neoLabel("بدون مجلد","Unfiled"))}
                }
            }
        },
        confirmButton={TextButton(onClick={moveNotesDialog=false;if(!noteSelectionMode)selectedNotes=emptySet()}) {Text(neoLabel("إغلاق","Close"))}}
    )
    if(notesTab && noteControlsExpanded) {
        ModalBottomSheet(onDismissRequest={noteControlsExpanded=false}) {
            val noteTags=db.dayItems.filter {it.type==DayItemTypeV11.NOTE&&!it.noteLocked&&!it.noteHidden}
                .flatMap {it.noteTags.split(",")}.map {it.trim()}.filter {it.isNotBlank()}.distinct().sorted()
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start=16.dp,end=16.dp,bottom=24.dp),
                verticalArrangement=Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(neoLabel("عرض وتنظيم الملاحظات","Notes display & organization"),
                        modifier=Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    TextButton(onClick={noteControlsExpanded=false}) {Text(neoLabel("تم","Done"))}
                }
                Text(neoLabel("القسم","Section"),style=MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "active" to neoLabel("الملاحظات","Notes"),
                        "pinned" to neoLabel("المثبتة","Pinned"),
                        "favorites" to neoLabel("المفضلة","Favorites"),
                        "archive" to neoLabel("الأرشيف","Archive"),
                        "trash" to neoLabel("المهملات","Trash"),
                        "hidden" to neoLabel("المخفية","Hidden")
                    ).forEach { (folder,label) ->
                        FilterChip(selected=noteFolder==folder,onClick={
                            if(folder=="hidden") hiddenAuthorized=true
                            noteFolder=folder
                        },label={Text(label)})
                    }
                }
                if(noteTags.isNotEmpty()) {
                    NeoChoice(
                        neoLabel("التصنيف أو الوسم","Category or tag"),
                        listOf<String?>(null)+noteTags,noteTag,
                        {it ?: neoLabel("الكل","All")},{noteTag=it}
                    )
                }
                HorizontalDivider()
                Text(neoLabel("مجلدات الملاحظات","Note folders"),style=MaterialTheme.typography.titleSmall)
                TextButton(onClick={editingFolderId=null;folderName="";editFolderDialog=true}) {
                    Icon(Icons.Rounded.CreateNewFolder,contentDescription=null)
                    Spacer(Modifier.width(6.dp))
                    Text(neoLabel("مجلد جديد","New folder"))
                }
                noteFolders.forEach { folder ->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(folder.name,modifier=Modifier.weight(1f))
                        IconButton(onClick={editingFolderId=folder.id;folderName=folder.name;editFolderDialog=true}) {
                            Icon(Icons.Rounded.Edit,neoLabel("تعديل المجلد","Rename folder"))
                        }
                        IconButton(onClick={deleteFolderId=folder.id}) {
                            Icon(Icons.Rounded.DeleteOutline,neoLabel("حذف المجلد","Delete folder"))
                        }
                    }
                }
                HorizontalDivider()
                Text(neoLabel("الترتيب حسب","Sort by"),style=MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "updated" to neoLabel("آخر تعديل","Updated"),
                        "title" to neoLabel("العنوان","Title"),
                        "date" to neoLabel("الإنشاء","Created")
                    ).forEach { (key,label) ->
                        FilterChip(
                            selected=noteSort==key,
                            onClick={noteSort=key;noteViewPrefs.edit().putString("sort",key).apply()},
                            label={Text(label,maxLines=1)},
                            modifier=Modifier.weight(1f)
                        )
                    }
                }
                HorizontalDivider()
                Text(neoLabel("شكل الكروت","Card layout"),style=MaterialTheme.typography.titleSmall)
                listOf(
                    listOf("list" to neoLabel("قائمة","List"),"details" to neoLabel("تفاصيل","Details")),
                    listOf("grid" to neoLabel("شبكة","Grid"),"large" to neoLabel("شبكة كبيرة","Large grid"))
                ).forEach { rowOptions ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { (key,label) ->
                            FilterChip(
                                selected=noteLayout==key,
                                onClick={noteLayout=key;noteViewPrefs.edit().putString("layout",key).apply()},
                                label={Text(label)},
                                modifier=Modifier.weight(1f)
                            )
                        }
                    }
                }
                TextButton(onClick={noteFolder="active";noteTag=null;activeFolderId=null}) {
                    Text(neoLabel("مسح فلاتر القسم والوسوم","Clear section and tag filters"))
                }
            }
        }
    }
    val visibleNotes=db.dayItems.filter { note ->
                        note.type==DayItemTypeV11.NOTE &&
                        when(noteFolder) {"archive"->note.noteArchived&&!note.noteTrashed&&!note.noteHidden;"trash"->note.noteTrashed&&!note.noteHidden;"hidden"->hiddenAuthorized&&note.noteHidden;"pinned"->note.notePinned&&!note.noteHidden&&!note.noteArchived&&!note.noteTrashed;"favorites"->note.id in favoriteNoteIds&&!note.noteHidden&&!note.noteArchived&&!note.noteTrashed;else->!note.noteArchived&&!note.noteTrashed&&!note.noteHidden} &&
                        (activeFolderId==null || (activeFolderId=="__unfiled__" &&
                            noteFolderAssignments[note.id]==null) ||
                            noteFolderAssignments[note.id]==activeFolderId) &&
                        (noteTag==null || (!note.noteLocked && note.noteTags.split(",").map {it.trim()}.contains(noteTag))) &&
                        (noteSearch.isBlank() || (!note.noteLocked && listOf(note.title,note.details,note.noteTags).any {it.contains(noteSearch,true)}))
                    }.let { notes -> when(noteSort){"title"->notes.sortedBy {if(it.notePrivate) "" else it.title};"date"->notes.sortedByDescending {it.createdAt};else->notes.sortedByDescending {it.updatedAt}} }.sortedByDescending {it.notePinned}
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalNeoTaskCompact provides options.compact,
            LocalNeoTaskMove provides if(options.sort==NeoTaskSort.MANUAL && optionsReady && !settingsBusy) { { id,delta ->
                saveOptions(options.copy(order=NeoTaskQueries.move(db.tasks,options.order,tasks,id,delta)))
            } } else null) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start=12.dp,end=12.dp,top=8.dp,bottom=96.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item(key="task-header") {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(
                            if(notesTab && noteSelectionMode)
                                neoLabel("محدد: ","Selected: ")+selectedNotes.size
                            else neoLabel("المهام والملاحظات","Tasks & notes"),
                            Modifier.weight(1f),style=MaterialTheme.typography.titleMedium
                        )
                        if(notesTab && noteSelectionMode) {
                            val selectable=visibleNotes.filterNot {it.noteLocked}.map {it.id}.toSet()
                            IconButton(
                                enabled=selectable.isNotEmpty(),
                                onClick={
                                    selectedNotes=if(selectedNotes.containsAll(selectable))
                                        emptySet() else selectable
                                }
                            ) {
                                Icon(Icons.Rounded.SelectAll,
                                    neoLabel("تحديد الكل أو إلغاء التحديد","Select or deselect all"))
                            }
                            Box {
                                IconButton(onClick={selectionActionsExpanded=true}) {
                                    Icon(Icons.Rounded.MoreVert,
                                        neoLabel("إجراءات التحديد","Selection actions"))
                                }
                                DropdownMenu(
                                    expanded=selectionActionsExpanded,
                                    onDismissRequest={selectionActionsExpanded=false}
                                ) {
                                    DropdownMenuItem(
                                        enabled=selectedNotes.isNotEmpty()&&!write.pending,
                                        text={Text(neoLabel("تثبيت المحدد","Pin selected"))},
                                        onClick={
                                            selectionActionsExpanded=false
                                            changed(db.copy(dayItems=db.dayItems.map {
                                                if(it.id in selectedNotes && !it.noteLocked)
                                                    it.copy(notePinned=true,updatedAt=System.currentTimeMillis())
                                                else it
                                            }),neoLiteral("تم تثبيت الملاحظات","Notes pinned"))
                                            selectedNotes=emptySet();noteSelectionMode=false
                                        }
                                    )
                                    DropdownMenuItem(
                                        enabled=selectedNotes.isNotEmpty()&&!write.pending,
                                        text={Text(
                                            if(noteFolder=="archive"||noteFolder=="trash")
                                                neoLabel("استعادة المحدد","Restore selected")
                                            else neoLabel("أرشفة المحدد","Archive selected"))},
                                        onClick={
                                            selectionActionsExpanded=false
                                            val restore=noteFolder=="archive"||noteFolder=="trash"
                                            changed(db.copy(dayItems=db.dayItems.map {
                                                if(it.id in selectedNotes && !it.noteLocked)
                                                    it.copy(noteArchived=!restore,noteTrashed=false,
                                                        updatedAt=System.currentTimeMillis())
                                                else it
                                            }),neoLiteral("تم تحديث الملاحظات","Notes updated"))
                                            selectedNotes=emptySet();noteSelectionMode=false
                                        }
                                    )
                                    DropdownMenuItem(
                                        enabled=selectedNotes.isNotEmpty()&&!folderBusy&&!write.pending,
                                        text={Text(neoLabel("المفضلة","Favorites"))},
                                        onClick={
                                            selectionActionsExpanded=false
                                            val ids=selectedNotes.toList()
                                            val next=ids.any {it !in favoriteNoteIds}
                                            updateFolder({store.setNoteFavorites(ids,next)},
                                                neoLiteral("تم تحديث المفضلة","Favorites updated"))
                                            selectedNotes=emptySet();noteSelectionMode=false
                                        }
                                    )
                                    DropdownMenuItem(
                                        enabled=selectedNotes.isNotEmpty()&&!folderBusy&&!write.pending,
                                        text={Text(neoLabel("نقل إلى مجلد","Move to folder"))},
                                        onClick={
                                            selectionActionsExpanded=false;moveNotesDialog=true
                                        }
                                    )
                                    if(noteFolder!="trash") DropdownMenuItem(
                                        enabled=selectedNotes.isNotEmpty()&&!write.pending,
                                        text={Text(neoLabel("نقل إلى المهملات","Move to Trash"))},
                                        onClick={
                                            selectionActionsExpanded=false;confirmTrash=true
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text={Text(neoLabel("إلغاء التحديد","Exit selection"))},
                                        onClick={
                                            selectionActionsExpanded=false
                                            selectedNotes=emptySet();noteSelectionMode=false
                                        }
                                    )
                                }
                            }
                        } else {
                        IconButton(onClick={searchExpanded=!searchExpanded}) {
                            Icon(if(searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                                neoLabel("بحث","Search"))
                        }
                        Box {
                            var sharedMenu by remember {mutableStateOf(false)}
                            IconButton(onClick={sharedMenu=true}) {
                                Icon(Icons.Rounded.MoreVert,neoLabel("الخيارات والفرز","Options and sorting"))
                            }
                            DropdownMenu(expanded=sharedMenu,onDismissRequest={sharedMenu=false}) {
                                DropdownMenuItem(
                                    text={Text(if(notesTab) neoLabel("فرز وعرض الملاحظات","Notes sorting & layout")
                                        else neoLabel("فلاتر المهام","Task filters"))},
                                    onClick={
                                        sharedMenu=false
                                        if(notesTab) noteControlsExpanded=true else filters=true
                                    }
                                )
                                if(notesTab) DropdownMenuItem(
                                    text={Text(if(noteSelectionMode) neoLabel("إلغاء تحديد الملاحظات","Exit selection")
                                        else neoLabel("تحديد الملاحظات","Select notes"))},
                                    onClick={
                                        sharedMenu=false
                                        noteSelectionMode=!noteSelectionMode
                                        selectedNotes=emptySet()
                                    }
                                )
                                if(!notesTab) {
                                    DropdownMenuItem(
                                        text={Text(if(taskControlsExpanded) neoLabel("إخفاء فلاتر المهام السريعة","Hide quick task filters")
                                            else neoLabel("فلاتر المهام السريعة","Quick task filters"))},
                                        onClick={sharedMenu=false;taskControlsExpanded=!taskControlsExpanded}
                                    )
                                    DropdownMenuItem(
                                        text={Text(neoLabel("قوالب المهام","Task templates"))},
                                        onClick={sharedMenu=false;nav.open("TASK_TEMPLATES")}
                                    )
                                    DropdownMenuItem(
                                        text={Text(neoLabel("مكتبة المرفقات","Attachments"))},
                                        onClick={sharedMenu=false;nav.open("ATTACHMENTS")}
                                    )
                                    DropdownMenuItem(
                                        text={Text(neoLabel("تراجع عن آخر تعديل","Undo last change"))},
                                        enabled=undo.enabled&&!write.pending,
                                        onClick={sharedMenu=false;undo.perform()}
                                    )
                                }
                                DropdownMenuItem(
                                    text={Text(neoLabel("إعدادات المهام والملاحظات","Tasks & notes preferences"))},
                                    onClick={sharedMenu=false;preferencesOpen=true}
                                )
                            }
                        }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical=5.dp),
                        horizontalArrangement=Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(neoLabel("المهام","Tasks"),neoLabel("الملاحظات","Notes"))
                            .forEachIndexed {index,label ->
                                FilterChip(
                                    selected=(notesTab==(index==1)),
                                    onClick={
                                        notesTab=index==1
                                        noteControlsExpanded=false
                                        selectedNotes=emptySet()
                                        noteSelectionMode=false
                                    },
                                    modifier=Modifier.weight(1f).height(48.dp),
                                    label={
                                        Text(
                                            label,
                                            modifier=Modifier.fillMaxWidth(),
                                            textAlign=androidx.compose.ui.text.style.TextAlign.Center,
                                            style=MaterialTheme.typography.titleMedium
                                        )
                                    },
                                    colors=FilterChipDefaults.filterChipColors(
                                        selectedContainerColor=NeoPremium.Navy,
                                        selectedLabelColor=NeoPremium.Cream,
                                        containerColor=MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                    }
                    if(searchExpanded) OutlinedTextField(
                        if(notesTab) noteSearch else search,
                        {if(notesTab) noteSearch=it else search=it},
                        label={Text(neoLabel("بحث","Search"))},
                        singleLine=true,
                        modifier=Modifier.fillMaxWidth()
                    )

                }
                if (!notesTab) {
                    item(key="task-filters") {
                        if(taskControlsExpanded) {
                        NeoPremiumTabs(listOf(neoLabel("الكل","All"),neoLabel("نشطة","Active"),neoLabel("مكتملة","Done")),NeoTaskStatus.entries.indexOf(status),{status=NeoTaskStatus.entries[it]})
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
                            },onEdit={open(task.id)},onDelete={deleteId=task.id})
                        }
                    }
                } else {
                    item(key="note-folder-library") {
                        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(neoLabel("مكتبة الملاحظات","Notes Library"),
                                style=MaterialTheme.typography.titleMedium)
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement=Arrangement.spacedBy(6.dp),
                                verticalAlignment=Alignment.CenterVertically
                            ) {
                                FilterChip(selected=activeFolderId==null,onClick={activeFolderId=null},
                                    label={Text(neoLabel("كل الملاحظات","All notes"))})
                                noteFolders.forEach {folder ->
                                    FilterChip(
                                        selected=activeFolderId==folder.id,
                                        onClick={activeFolderId=folder.id},
                                        label={Text(
                                            folder.name+" ("+noteFolderAssignments.values.count {it==folder.id}+")",
                                            maxLines=1
                                        )}
                                    )
                                }
                                FilterChip(
                                    selected=activeFolderId=="__unfiled__",
                                    onClick={activeFolderId="__unfiled__"},
                                    label={Text(neoLabel("بدون مجلد","Unfiled"))}
                                )
                                AssistChip(
                                    onClick={editingFolderId=null;folderName="";editFolderDialog=true},
                                    label={Text(neoLabel("مجلد جديد +","New folder +"))}
                                )
                            }
                        }
                    }
                    val notes=visibleNotes
                    if(notes.isEmpty()) item {NeoEmptyState(neoLabel("لا توجد ملاحظات مطابقة","No matching notes"),neoLabel("غيّر البحث أو المجلد، أو أضف ملاحظة","Change search or folder, or add a note"))}
                    if(noteLayout=="grid" || noteLayout=="large") {
                        val columns=if(noteLayout=="grid") 3 else 2
                        notes.chunked(columns).forEachIndexed {rowIndex,rowNotes ->
                            item(key="note-grid-$rowIndex") {
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                                    rowNotes.forEach { note ->
                                        Box(Modifier.weight(1f)) {
                            val customColor=if(note.notePrivate) null else (note.noteBackground ?: NeoLaunchPreferences(context).defaultNoteBackground.takeIf {it.isNotBlank()})?.let {runCatching {android.graphics.Color.parseColor(it)}.getOrNull()}
                            val contentColor=customColor?.let {if(androidx.core.graphics.ColorUtils.calculateLuminance(it)>0.45) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White} ?: MaterialTheme.colorScheme.onSurface
                            Card(modifier=Modifier
                                .fillMaxWidth()
                                .then(if(noteLayout=="grid"||noteLayout=="large")
                                    Modifier.height(if(noteLayout=="grid") 142.dp else 184.dp)
                                    else Modifier)
                                .combinedClickable(
                                    onClick={
                                        if(noteSelectionMode) {
                                            if(!note.noteLocked)
                                                selectedNotes=if(note.id in selectedNotes)
                                                    selectedNotes-note.id else selectedNotes+note.id
                                        } else {noteId=note.id;noteEditor=true}
                                    },
                                    onLongClick={
                                        if(!note.noteLocked) {
                                            noteSelectionMode=true
                                            selectedNotes=selectedNotes+note.id
                                        }
                                    }
                                ),
                            colors=CardDefaults.cardColors(
                                containerColor=customColor?.let {androidx.compose.ui.graphics.Color(it)}
                                    ?: MaterialTheme.colorScheme.surface,
                                contentColor=contentColor)) {
                                Column(Modifier.fillMaxSize().padding(if(noteLayout=="grid") 7.dp else 12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                                    if(noteSelectionMode&&!note.noteLocked) Checkbox(checked=note.id in selectedNotes,onCheckedChange={checked->selectedNotes=if(checked) selectedNotes+note.id else selectedNotes-note.id})
                                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top) {
                                        Text(
                                            (if(note.noteLocked) "🔒 " else if(note.notePinned) "📌 " else "") +
                                                note.title.ifBlank {
                                                    if(note.noteLocked || note.notePreviewHidden) neoLabel("ملاحظة","Note")
                                                    else noteDisplayText(note.details).lineSequence().firstOrNull { it.isNotBlank() }?.take(100)
                                                        ?: neoLabel("بدون عنوان","Untitled")
                                                },
                                            modifier=Modifier.weight(1f),
                                            style=MaterialTheme.typography.titleMedium,
                                            maxLines=if(noteLayout=="grid") 2 else 3,
                                            overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        if(!noteSelectionMode) NeoNoteCardMenu(
                                            favorite=note.id in favoriteNoteIds,
                                            pinned=note.notePinned,
                                            archived=note.noteArchived,
                                            trashed=note.noteTrashed,
                                            locked=note.noteLocked,
                                            busy=folderBusy||write.pending,
                                            onOpen={noteId=note.id;noteEditor=true},
                                            onFavorite={
                                                val next=note.id !in favoriteNoteIds
                                                updateFolder(
                                                    {store.setNoteFavorites(listOf(note.id),next)},
                                                    neoLiteral("تم تحديث المفضلة","Favorites updated"))
                                            },
                                            onPin={
                                                changed(db.copy(dayItems=db.dayItems.map {item ->
                                                    if(item.id==note.id) item.copy(
                                                        notePinned=!note.notePinned,
                                                        updatedAt=System.currentTimeMillis()) else item
                                                }),neoLiteral("تم تحديث التثبيت","Pin updated"))
                                            },
                                            onArchive={
                                                changed(db.copy(dayItems=db.dayItems.map {item ->
                                                    if(item.id==note.id) item.copy(
                                                        noteArchived=if(note.noteTrashed) false else !note.noteArchived,
                                                        noteTrashed=false,
                                                        updatedAt=System.currentTimeMillis()) else item
                                                }),neoLiteral("تم تحديث الأرشيف","Archive updated"))
                                            },
                                            onMove={
                                                selectedNotes=setOf(note.id);moveNotesDialog=true
                                            },
                                            onSelect={
                                                selectedNotes=setOf(note.id);noteSelectionMode=true
                                            },
                                            onTrash={
                                                selectedNotes=setOf(note.id);confirmTrash=true
                                            }
                                        )
                                    }
                                        // Password locking and hiding an excerpt are independent settings.
                                        // Neither option removes the note from the list or blocks opening its card.
                                        if(!note.noteLocked && !note.notePreviewHidden && noteLayout!="list") {
                                            if(note.details.isNotBlank()) Text(
                                                noteInline(noteDisplayText(note.details)),
                                                maxLines=if(noteLayout=="grid") 3 else 4,
                                                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style=MaterialTheme.typography.bodyMedium
                                            )
                                            if(note.noteTags.isNotBlank() && noteLayout=="details")
                                                Text(note.noteTags,style=MaterialTheme.typography.labelSmall,maxLines=1)
                                        }
                                        Text(note.date,style=MaterialTheme.typography.labelSmall,
                                            color=contentColor.copy(alpha=0.75f),maxLines=1)
                                }
                            }
                                        }
                                    }
                                    repeat(columns-rowNotes.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    } else {
                        items(notes,key={it.id}) {note ->
                        val customColor=if(note.notePrivate) null else (note.noteBackground ?: NeoLaunchPreferences(context).defaultNoteBackground.takeIf {it.isNotBlank()})?.let {runCatching {android.graphics.Color.parseColor(it)}.getOrNull()}
                        val contentColor=customColor?.let {if(androidx.core.graphics.ColorUtils.calculateLuminance(it)>0.45) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White} ?: MaterialTheme.colorScheme.onSurface
                        Card(modifier=Modifier
                                .fillMaxWidth()
                                .then(if(noteLayout=="grid"||noteLayout=="large")
                                    Modifier.height(if(noteLayout=="grid") 142.dp else 184.dp)
                                    else Modifier)
                                .combinedClickable(
                                    onClick={
                                        if(noteSelectionMode) {
                                            if(!note.noteLocked)
                                                selectedNotes=if(note.id in selectedNotes)
                                                    selectedNotes-note.id else selectedNotes+note.id
                                        } else {noteId=note.id;noteEditor=true}
                                    },
                                    onLongClick={
                                        if(!note.noteLocked) {
                                            noteSelectionMode=true
                                            selectedNotes=selectedNotes+note.id
                                        }
                                    }
                                ),
                            colors=CardDefaults.cardColors(
                                containerColor=customColor?.let {androidx.compose.ui.graphics.Color(it)}
                                    ?: MaterialTheme.colorScheme.surface,
                                contentColor=contentColor)) {
                            Column(Modifier.fillMaxSize().padding(if(noteLayout=="grid") 7.dp else 12.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                                if(noteSelectionMode&&!note.noteLocked) Checkbox(checked=note.id in selectedNotes,onCheckedChange={checked->selectedNotes=if(checked) selectedNotes+note.id else selectedNotes-note.id})
                                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Top) {
                                    Text(
                                            (if(note.noteLocked) "🔒 " else if(note.notePinned) "📌 " else "") +
                                                note.title.ifBlank {
                                                    if(note.noteLocked || note.notePreviewHidden) neoLabel("ملاحظة","Note")
                                                    else noteDisplayText(note.details).lineSequence().firstOrNull { it.isNotBlank() }?.take(100)
                                                        ?: neoLabel("بدون عنوان","Untitled")
                                                },
                                            modifier=Modifier.weight(1f),
                                            style=MaterialTheme.typography.titleMedium,
                                            maxLines=if(noteLayout=="grid") 2 else 3,
                                            overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    if(!noteSelectionMode) NeoNoteCardMenu(
                                            favorite=note.id in favoriteNoteIds,
                                            pinned=note.notePinned,
                                            archived=note.noteArchived,
                                            trashed=note.noteTrashed,
                                            locked=note.noteLocked,
                                            busy=folderBusy||write.pending,
                                            onOpen={noteId=note.id;noteEditor=true},
                                            onFavorite={
                                                val next=note.id !in favoriteNoteIds
                                                updateFolder(
                                                    {store.setNoteFavorites(listOf(note.id),next)},
                                                    neoLiteral("تم تحديث المفضلة","Favorites updated"))
                                            },
                                            onPin={
                                                changed(db.copy(dayItems=db.dayItems.map {item ->
                                                    if(item.id==note.id) item.copy(
                                                        notePinned=!note.notePinned,
                                                        updatedAt=System.currentTimeMillis()) else item
                                                }),neoLiteral("تم تحديث التثبيت","Pin updated"))
                                            },
                                            onArchive={
                                                changed(db.copy(dayItems=db.dayItems.map {item ->
                                                    if(item.id==note.id) item.copy(
                                                        noteArchived=if(note.noteTrashed) false else !note.noteArchived,
                                                        noteTrashed=false,
                                                        updatedAt=System.currentTimeMillis()) else item
                                                }),neoLiteral("تم تحديث الأرشيف","Archive updated"))
                                            },
                                            onMove={
                                                selectedNotes=setOf(note.id);moveNotesDialog=true
                                            },
                                            onSelect={
                                                selectedNotes=setOf(note.id);noteSelectionMode=true
                                            },
                                            onTrash={
                                                selectedNotes=setOf(note.id);confirmTrash=true
                                            }
                                        )
                                }
                                        // Password locking and hiding an excerpt are independent settings.
                                        // Neither option removes the note from the list or blocks opening its card.
                                        if(!note.noteLocked && !note.notePreviewHidden && noteLayout!="list") {
                                            if(note.details.isNotBlank()) Text(
                                                noteInline(noteDisplayText(note.details)),
                                                maxLines=if(noteLayout=="grid") 3 else 4,
                                                overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                style=MaterialTheme.typography.bodyMedium
                                            )
                                            if(note.noteTags.isNotBlank() && noteLayout=="details")
                                                Text(note.noteTags,style=MaterialTheme.typography.labelSmall,maxLines=1)
                                        }
                                        Text(note.date,style=MaterialTheme.typography.labelSmall,
                                            color=contentColor.copy(alpha=0.75f),maxLines=1)
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
        FloatingActionButton(onClick={if(notesTab) {
            initialNoteTitle="";initialNoteBody=""
            newNoteFolderTarget=activeFolderId?.takeUnless{it=="__unfiled__"}
            noteId=null;noteEditor=true
        } else open()},containerColor=NeoPremium.Navy,contentColor=NeoPremium.Gold,modifier=Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
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
            onDismiss = {
                noteEditor=false;noteId=null;initialNoteTitle="";initialNoteBody=""
            },
            initialTitle=if(existing==null) initialNoteTitle else "",
            initialBody=if(existing==null) initialNoteBody else ""
        ) { saved ->
                noteEditor = false; noteId = null
                if(existing==null) {
                    val destination=newNoteFolderTarget
                    if(destination!=null) updateFolder(
                        {store.moveNotesToFolder(listOf(saved.id),destination)},
                        neoLiteral("تم حفظ الملاحظة داخل المجلد","Note saved to folder")
                    )
                }
                newNoteFolderTarget=null
                initialNoteTitle="";initialNoteBody=""
            }
        }
    }
    if(filters) AlertDialog(onDismissRequest={filters=false},title={Text(neoLiteral("فلاتر وعرض المهام","Task filters and view"))},text={
        Column(Modifier.heightIn(max=420.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            NeoChoice(neoLiteral("حالة المهام","Task status"),NeoTaskStatus.entries,status,
                {when(NeoTaskStatus.entries.indexOf(it)) {
                    0 -> neoLiteral("الكل","All")
                    1 -> neoLiteral("نشطة","Active")
                    else -> neoLiteral("مكتملة","Completed")
                }},{status=it})
            NeoChoice(neoLiteral("الفترة","Date range"),NeoTaskPeriod.entries,period,{periodLabel(it)},{period=it})
            NeoChoice(neoLiteral("الأولوية","Priority"),listOf<TaskPriorityV11?>(null)+TaskPriorityV11.entries,priority,{it?.let { neoPriorityLabel(it) } ?: neoLiteral("أي أولوية","Any priority")},{priority=it})
            NeoChoice(neoLiteral("التقويم","Calendar"),listOf<String?>(null)+db.calendars.map {it.id},calendar,{id-> db.calendars.firstOrNull {it.id==id}?.name ?: neoLiteral("كل التقاويم","All calendars")},{calendar=it})
            NeoChoice(neoLiteral("الترتيب","Sort"),NeoTaskSort.entries,options.sort,{when(it){NeoTaskSort.DATE->neoLiteral("التاريخ","Date");NeoTaskSort.PRIORITY->neoLiteral("الأولوية","Priority");NeoTaskSort.MANUAL->neoLiteral("يدوي","Manual")}},{saveOptions(options.copy(sort=it))})
            NeoChoice(neoLabel("كثافة البطاقات","Card density"),listOf(true,false),options.compact,
                { if(it) neoLabel("مضغوط","Compact") else neoLabel("مريح","Comfortable") },
                {saveOptions(options.copy(compact=it))})
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


@Composable
private fun NeoNoteCardMenu(
    favorite:Boolean,
    pinned:Boolean,
    archived:Boolean,
    trashed:Boolean,
    locked:Boolean,
    busy:Boolean,
    onOpen:()->Unit,
    onFavorite:()->Unit,
    onPin:()->Unit,
    onArchive:()->Unit,
    onMove:()->Unit,
    onSelect:()->Unit,
    onTrash:()->Unit
) {
    var expanded by remember {mutableStateOf(false)}
    Box {
        IconButton(modifier=Modifier.size(36.dp),onClick={expanded=true}) {
            Icon(Icons.Rounded.MoreVert,
                neoLabel("إجراءات الملاحظة","Note actions"),
                Modifier.size(20.dp))
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(
                text={Text(neoLabel("فتح الملاحظة","Open note"))},
                onClick={expanded=false;onOpen()}
            )
            HorizontalDivider()
            DropdownMenuItem(
                enabled=!locked&&!busy,
                text={Text(if(favorite) neoLabel("إزالة من المفضلة","Remove favorite")
                    else neoLabel("إضافة للمفضلة","Add favorite"))},
                onClick={expanded=false;onFavorite()}
            )
            DropdownMenuItem(
                enabled=!locked&&!busy,
                text={Text(if(pinned) neoLabel("إلغاء التثبيت","Unpin")
                    else neoLabel("تثبيت","Pin"))},
                onClick={expanded=false;onPin()}
            )
            DropdownMenuItem(
                enabled=!locked&&!busy,
                text={Text(if(archived||trashed) neoLabel("استعادة","Restore")
                    else neoLabel("أرشفة","Archive"))},
                onClick={expanded=false;onArchive()}
            )
            DropdownMenuItem(
                enabled=!locked&&!busy,
                text={Text(neoLabel("نقل إلى مجلد","Move to folder"))},
                onClick={expanded=false;onMove()}
            )
            DropdownMenuItem(
                enabled=!locked,
                text={Text(neoLabel("تحديد مع ملاحظات أخرى","Select multiple"))},
                onClick={expanded=false;onSelect()}
            )
            if(!trashed) DropdownMenuItem(
                enabled=!locked&&!busy,
                text={Text(neoLabel("نقل إلى المهملات","Move to Trash"))},
                onClick={expanded=false;onTrash()}
            )
        }
    }
}
