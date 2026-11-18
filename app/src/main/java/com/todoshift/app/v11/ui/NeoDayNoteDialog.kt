package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatPaint
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun NeoDayNoteDialog(date: LocalDate, existing: DayItemV11?, expectedDatabase: V11Database, calendarId: String?, onDismiss: () -> Unit, initialTitle:String="", initialBody:String="", initialDocumentA4:Boolean=false, onSave: (DayItemV11) -> Unit) {
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
    editorState.SaveableStateProvider(existing?.id ?: "new-note") {NeoRichNoteDialog(date, existing, expectedDatabase, calendarId, onDismiss, onSave, {editorLocked=it}, initialTitle, initialBody,initialDocumentA4)}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NeoRichNoteDialog(date: LocalDate, existing: DayItemV11?, expectedDatabase: V11Database, calendarId: String?, onDismiss: () -> Unit, onSave: (DayItemV11) -> Unit, onProtectionChanged:(Boolean)->Unit, initialTitle:String, initialBody:String,initialDocumentA4:Boolean) {
    val context=LocalContext.current
    val styleStore=remember { NeoSmartStore(context.applicationContext) }
    val drafts=remember { NeoNoteDraftStore(context.applicationContext) }
    val versions=remember {NeoNoteHistoryStore(context.applicationContext)}
    var expectedMetadata by remember { mutableStateOf(styleStore.snapshot().toString()) }
    var expected by remember { mutableStateOf(expectedDatabase) }
    val committed=LocalNeoNoteCommitted.current
    val scope=rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    var closing by remember {mutableStateOf(false)}
    var lastSavedEdit by remember {mutableStateOf<String?>(null)}
    val rootWritePending=LocalNeoEditorWriteState.current.pending
    var id by rememberSaveable(existing?.id) { mutableStateOf(existing?.id ?: UUID.randomUUID().toString()) }
    // Resolve legacy date-key conflicts without overwriting old encrypted drafts.
    var draftKey by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.id ?: id)
    }
    var recoveredLegacyKey by remember {mutableStateOf<Pair<String,String>?>(null)}


    var color by rememberSaveable { mutableStateOf(styleStore.noteColor(id)) }
    var border by rememberSaveable { mutableStateOf(styleStore.snapshot().optJSONObject("noteBorders")?.optBoolean(id,false) ?: false) }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: initialTitle.take(300)) }
    var body by rememberSaveable(stateSaver=TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(existing?.details ?: initialBody.take(16_000)))
    }
    var tags by rememberSaveable { mutableStateOf(existing?.noteTags.orEmpty()) }
    var pinned by rememberSaveable { mutableStateOf(existing?.notePinned ?: false) }
    var archived by rememberSaveable { mutableStateOf(existing?.noteArchived ?: false) }
    var confirmTrashNote by remember {mutableStateOf(false)}
    var trashed by rememberSaveable { mutableStateOf(existing?.noteTrashed ?: false) }
    var locked by rememberSaveable { mutableStateOf(existing?.noteLocked ?: false) }
    var hidden by rememberSaveable { mutableStateOf(existing?.noteHidden ?: false) }
    var previewHidden by rememberSaveable { mutableStateOf(existing?.notePreviewHidden ?: false) }
    var documentA4 by rememberSaveable(existing?.id) {mutableStateOf(existing?.noteDocumentA4 ?: initialDocumentA4)}
    var calendarLinked by rememberSaveable {mutableStateOf(existing?.noteCalendarLinked ?: true)}
    var linkedDate by rememberSaveable {mutableStateOf(existing?.date ?: date.toString())}
    var linkedCalendar by rememberSaveable {mutableStateOf(existing?.calendarId ?: calendarId)}
    var background by rememberSaveable { mutableStateOf(existing?.noteBackground.orEmpty()) }
    // Only new notes inherit the card/calendar color until the paper is customized.
    // Existing notes retain their stored appearance without silent migration.
    var paperCustomized by rememberSaveable(id) {
        mutableStateOf(existing?.noteBackground?.isNotBlank()==true)
    }
    fun selectPaperColor(hex:String) {
        // "Default" returns new notes to automatic card/paper synchronization.
        paperCustomized=hex.isNotBlank()
        background=if(hex.isBlank() && existing==null && color.matches(Regex("#[0-9a-fA-F]{6}")))
            color else hex
    }
    fun selectCardColor(hex:String) {
        color=hex
        // Text fields emit incomplete hex values while typing. Never propagate an
        // incomplete value to the paper background or its color parser.
        if(existing==null && !paperCustomized && hex.matches(Regex("#[0-9a-fA-F]{6}")))
            background=hex
    }
    val launchPrefs=remember {NeoLaunchPreferences(context.applicationContext)}
    val savedPresentation=remember(id) {styleStore.notePresentation(id)}
    var ruledOverride by rememberSaveable(id) {mutableStateOf(savedPresentation.ruled)}
    var arabicFontOverride by rememberSaveable(id) {mutableStateOf(savedPresentation.arabicFont)}
    var englishFontOverride by rememberSaveable(id) {mutableStateOf(savedPresentation.englishFont)}
    val ruledPaper=ruledOverride ?: launchPrefs.defaultNoteRuled
    val arabicTypeface=arabicFontOverride ?: launchPrefs.defaultArabicNoteFont
    val englishTypeface=englishFontOverride ?: launchPrefs.defaultEnglishNoteFont
    var appearanceOpen by remember {mutableStateOf(false)}

    var detachedTasks by rememberSaveable {mutableStateOf(listOf<String>())}
    var existingTaskLinks by rememberSaveable {mutableStateOf(listOf<String>())}
    val linkedTaskSecurity=remember {V11SecurityStore(context.applicationContext)}
    var linkedTasksAuthorized by remember {mutableStateOf(!linkedTaskSecurity.load().protectTasksOnly)}
    var verifyLinkedTasks by remember {mutableStateOf(false)}
    var selectedTaskLink by remember {mutableStateOf<String?>(null)}
    var taskSpecs by rememberSaveable { mutableStateOf(listOf<String>()) }
    var reminder by rememberSaveable { mutableStateOf(existing?.reminderAt?.let {java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it),java.time.ZoneId.systemDefault()).toString()}.orEmpty()) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val checklistPreferences=remember(context) {
        context.applicationContext.getSharedPreferences("neo_note_view_v46",
            android.content.Context.MODE_PRIVATE)
    }
    var moveCompletedChecklistItems by rememberSaveable {
        mutableStateOf(checklistPreferences.getBoolean("move_completed_to_end",false))
    }
    var editing by rememberSaveable(existing?.id) { mutableStateOf(existing == null) }
    // Evaluate the localized default inside the composable scope, not inside
    // rememberSaveable's non-composable state initialization lambda.
    val preferredNoteDirection=if(neoLabel("ar","en")=="ar") "rtl" else "ltr"
    var noteDirection by rememberSaveable(existing?.id) {
        mutableStateOf(preferredNoteDirection)
    }
    var floatingFormat by rememberSaveable { mutableStateOf(false) }
    var noteMoreMenu by remember { mutableStateOf(false) }
    var noteSettingsOpen by rememberSaveable { mutableStateOf(false) }
    var documentAction by remember {mutableStateOf<String?>(null)}
    var formatInitialTab by remember {mutableIntStateOf(0)}
    var showFormatSheet by rememberSaveable {mutableStateOf(false)}
    var paperPaletteOpen by remember { mutableStateOf(false) }
    var mediaShelfVisible by rememberSaveable {mutableStateOf(false)}
    var mediaRequest by remember {mutableStateOf<String?>(null)}
    var advancedCanvas by rememberSaveable {mutableStateOf(false)}
    var focusedParagraph by remember {mutableStateOf<NeoRichEditorState?>(null)}
    var commitFocusedParagraph by remember {mutableStateOf<((NeoRichEditorState)->Unit)?>(null)}
    var a4Preview by rememberSaveable {mutableStateOf(false)}
    var mediaVersion by remember {mutableIntStateOf(0)}
    val noteInputFocus=remember { FocusRequester() }
    val noteKeyboard=LocalSoftwareKeyboardController.current
    var options by rememberSaveable { mutableStateOf(false) }
    var draftReady by rememberSaveable { mutableStateOf(false) }
    var draftStatus by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var conflictingDraft by remember { mutableStateOf<JSONObject?>(null) }
    var draftRecoveryError by remember {mutableStateOf<String?>(null)}
    var legacyRecoveryKey by remember {mutableStateOf<String?>(null)}
    var exitWithDraft by remember {mutableStateOf<(() -> Unit)?>(null)}
    var resolvingConflict by remember {mutableStateOf(false)}
    val undo=remember { mutableStateListOf<TextFieldValue>() }; val redo=remember { mutableStateListOf<TextFieldValue>() }
    fun edit(next: TextFieldValue) {
        if(next.text!=body.text) { undo.add(body); if(undo.size>50) undo.removeAt(0); redo.clear() }
        body=next
    }
    // The IME's composing buffer must remain the source of truth while typing.
    // Re-encoding spans and reloading the field on each Arabic composing edit
    // breaks cursor movement and produces unexpected text layout changes.
    var rich by remember(existing?.id) {mutableStateOf(NeoRichEditorState.open(body.text))}
    var richDirty by remember(existing?.id) {mutableStateOf(false)}
    var lastEditorCommit by remember(existing?.id) {mutableStateOf(body.text)}
    fun noteSource():String=if(richDirty) rich.stored() else body.text
    fun replaceDocumentSource(next:String) {
        require(next.length<=300_000)
        if(rich.value.composition!=null) {
            error=neoLiteral("أنهِ الكلمة الحالية قبل إدراج مستند",
                "Finish composing your current word before inserting a block")
            return
        }
        edit(TextFieldValue(next))
        rich=NeoRichEditorState.open(next)
        lastEditorCommit=next
        richDirty=false
    }
    fun insertDocumentBlock(markup:String) {
        val current=noteSource()
        val visibleCaret=rich.value.selection.end.coerceIn(0,rich.value.text.length)
        val mapping=NeoNoteVisualTransformation()
            .filter(androidx.compose.ui.text.AnnotatedString(current)).offsetMapping
        val caret=mapping.transformedToOriginal(visibleCaret).coerceIn(0,current.length)
        runCatching {
            replaceDocumentSource(NeoDocumentBlocks.insert(current,caret,markup))
        }.onFailure {error=it.message ?: "Cannot insert document block"}
    }
    fun launchMedia(action:String) {
        mediaShelfVisible=true
        mediaRequest=action
    }

    fun editRich(next:NeoRichEditorState) {
        val contentChanged=next.value.text!=rich.value.text ||
            next.characters!=rich.characters || next.hidden!=rich.hidden
        rich=next
        if(contentChanged) richDirty=true
        // Never replace the IME composing buffer with serialized formatting.
        if(richDirty && next.value.composition==null) {
            val stored=next.stored()
            lastEditorCommit=stored
            richDirty=false
            if(stored!=body.text) edit(TextFieldValue(stored))
        }
    }
    // Draft recovery and Undo/Redo can replace source from outside the live editor.
    LaunchedEffect(body.text) {
        if(body.text!=lastEditorCommit && rich.value.composition==null) {
            if(rich.stored()!=body.text) rich=NeoRichEditorState.open(body.text)
            lastEditorCommit=body.text
            richDirty=false
        }
    }
    var sourceRevision by remember { mutableStateOf(expectedDatabase.dayItems.firstOrNull {it.id==id}?.toString() ?: "<new>") }
    LaunchedEffect(draftKey) {
        if(draftReady) return@LaunchedEffect
        try {
            val recovered=withContext(Dispatchers.IO) { drafts.read(draftKey) }
            if(recovered!=null) {
                val currentSource=expectedDatabase.dayItems.firstOrNull {it.id==recovered.optString("id")}?.toString()
                val savedSource=recovered.optString("source")
                // A matching "<new>" source is not proof of matching identity.
                if(NeoNoteDraftIdentity.canAutoRecover(
                    recovered.optString("id"),id,savedSource,sourceRevision,currentSource)) {
                    sourceRevision=recovered.optString("source",sourceRevision)
                    id=recovered.optString("id",id); title=recovered.optString("title",title)
                    body=TextFieldValue(recovered.optString("details",body.text)); tags=recovered.optString("tags",tags)
                    calendarLinked=recovered.optBoolean("calendarLinked",calendarLinked);linkedDate=recovered.optString("linkedDate",linkedDate)
                    linkedCalendar=if(recovered.has("linkedCalendar")) recovered.optString("linkedCalendar").takeIf {it.isNotBlank()} else linkedCalendar
                    pinned=recovered.optBoolean("pinned",pinned); archived=recovered.optBoolean("archived",archived)
                    trashed=recovered.optBoolean("trashed",trashed); locked=recovered.optBoolean("locked",locked);hidden=recovered.optBoolean("hidden",hidden);previewHidden=recovered.optBoolean("previewHidden",previewHidden); documentA4=recovered.optBoolean("documentA4",documentA4)
                    detachedTasks=recovered.optJSONArray("detachedTasks")?.let {a->(0 until a.length()).map {a.getString(it)}} ?: emptyList()
                    existingTaskLinks=recovered.optJSONArray("existingTaskLinks")?.let {a->(0 until a.length()).map {a.getString(it)}} ?: emptyList()
                    taskSpecs=recovered.optJSONArray("taskSpecs")?.let { array->(0 until array.length()).map {array.getString(it)} } ?: taskSpecs
                    reminder=recovered.optString("reminder",reminder);background=recovered.optString("background",background);color=recovered.optString("color",color);border=recovered.optBoolean("border",border)
                    ruledOverride=if(recovered.has("ruledOverride")) recovered.optBoolean("ruledOverride") else null
                    arabicFontOverride=recovered.optString("arabicFontOverride").takeIf {it in NeoNotePresentation.FONT_KEYS}
                    englishFontOverride=recovered.optString("englishFontOverride").takeIf {it in NeoNotePresentation.FONT_KEYS}
                    draftStatus="تم استرجاع المسودة / Draft recovered"
                } else {
                    conflictingDraft=recovered
                    error=neoLiteral(
                        "مسودة قديمة أو تخص ملاحظة أخرى. اختر الاستعادة أو المتابعة.",
                        "An older or unrelated draft exists. Recover or continue safely.")
                }
            }
            draftReady=conflictingDraft==null && draftRecoveryError==null && error==null
        } catch(failure: Exception) {
            draftRecoveryError=failure.message ?: "Unknown draft read error"
            error=neoLiteral("تعذر قراءة المسودة المشفرة؛ لم يتم حذفها.",
                "Could not read encrypted draft; it has not been deleted.")
        }
    }
    fun editVersion():String = org.json.JSONArray(listOf(id,title,noteSource(),tags,pinned,archived,trashed,locked,hidden,previewHidden,documentA4,background,color,border,reminder,calendarLinked,linkedDate,linkedCalendar,taskSpecs,detachedTasks,existingTaskLinks,mediaVersion,ruledOverride,arabicFontOverride,englishFontOverride)).toString()
    suspend fun persistNote(close: Boolean): Boolean {
        while(pending) delay(10)
        if(rootWritePending || !draftReady || conflictingDraft!=null) return false
        if(!close && lastSavedEdit==editVersion()) return true
        val hasMedia=withContext(Dispatchers.IO) {
            NeoAttachmentStore(context.applicationContext).list().any {it.owner==id}
        }
        if(title.isBlank() && noteSource().isBlank() && !hasMedia &&
            expected.dayItems.none {it.id==id}) {
            if(close) onDismiss()
            return true
        }
        if(!color.matches(Regex("#[0-9a-fA-F]{6}")) || (background.isNotBlank() && !background.matches(Regex("#[0-9a-fA-F]{6}")))) {
            error="اختر لونًا صحيحًا / Choose a valid color";return false
        }
        if(runCatching {LocalDate.parse(linkedDate)}.isFailure) {error="تاريخ الملاحظة غير صحيح / Invalid note date";return false}
        val reminderAt=if(reminder.isBlank()) null else try {java.time.LocalDateTime.parse(reminder).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()} catch(_:Exception) {error="تاريخ التذكير غير صحيح / Invalid reminder date";return false}
        val note=(expected.dayItems.firstOrNull {it.id==id} ?: DayItemV11(id,date.toString(),calendarId,DayItemTypeV11.NOTE,""))
            .copy(date=linkedDate,calendarId=linkedCalendar,noteCalendarLinked=calendarLinked,title=title.trim().ifBlank { noteDisplayText(noteSource()).lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }?.take(72) ?: "" },details=noteSource(),reminderAt=reminderAt,updatedAt=System.currentTimeMillis(),noteTags=tags,notePinned=pinned,noteArchived=archived,noteTrashed=trashed,noteLocked=locked,noteHidden=hidden,notePreviewHidden=previewHidden,noteBackground=background.takeIf {it.isNotBlank()},
                noteTrashedAt=if(trashed) (expected.dayItems.firstOrNull{it.id==id}?.noteTrashedAt ?: System.currentTimeMillis()) else null,
                noteDocumentA4=documentA4)
        val linkedTasks=taskSpecs.map {spec ->
            val key=spec.substringBefore('\n'); val label=spec.substringAfter('\n')
            val taskId=UUID.nameUUIDFromBytes(("note-checklist:$id:$key").toByteArray(Charsets.UTF_8)).toString()
            expected.tasks.firstOrNull {(it.id==taskId||it.id==key)&&it.linkedNoteId==id} ?: TaskV11(id=taskId,title=label,date=linkedDate,calendarId=linkedCalendar,linkedNoteId=id)
        }
        val linkedExisting=existingTaskLinks.mapNotNull {key->expected.tasks.firstOrNull {it.id==key}?.copy(linkedNoteId=id)}
        val priorCommitted=expected.dayItems.firstOrNull {it.id==id}
        val savedEdit=editVersion()
        val savedDetails=noteSource()
        closing=close;pending=true
        return try {
            val next=withContext(Dispatchers.IO) {com.todoshift.app.backup.Neo4LocalBackup(context.applicationContext).saveNote(expected,expectedMetadata,note,color,border,
                presentation=NeoNotePresentation(ruledOverride,arabicFontOverride,englishFontOverride),
                linkedTasks=(linkedTasks+linkedExisting).distinctBy {it.id}.filterNot {it.id in detachedTasks},detachedTaskIds=detachedTasks.toSet())}
            expected=next;expectedMetadata=withContext(Dispatchers.IO) {styleStore.snapshot().toString()};sourceRevision=note.toString()
            lastSavedEdit=savedEdit
            if(richDirty) {
                body=TextFieldValue(savedDetails)
                lastEditorCommit=savedDetails
                richDirty=false
            }
            detachedTasks=emptyList();existingTaskLinks=emptyList()
            onProtectionChanged(note.noteLocked);committed(next);draftStatus="تم الحفظ / Saved";error=null
            if(priorCommitted!=null &&
                (priorCommitted.title!=note.title || priorCommitted.details!=note.details)) {
                try {
                    withContext(Dispatchers.IO) {
                        versions.savePrior(id,priorCommitted.title,priorCommitted.details,
                            System.currentTimeMillis())
                    }
                } catch(failure:Exception) {
                    // The current note was already committed; never discard it
                    // because optional local history could not be updated.
                    draftStatus=neoLiteral("تم الحفظ بدون تحديث سجل النسخ",
                        "Saved; history checkpoint unavailable")
                }
            }
            // Cleanup of stale drafts is optional after data was committed.
            try {
                withContext(Dispatchers.IO) {
                    drafts.removeIfVersion(draftKey,savedEdit)
                    recoveredLegacyKey?.let {(oldKey,oldVersion) ->
                        drafts.removeIfVersion(oldKey,oldVersion)
                    }
                }
                recoveredLegacyKey=null
            } catch(_:Exception) {
                draftStatus=neoLiteral("تم الحفظ؛ تنظيف مسودة قديمة تعذر",
                    "Note saved; old draft cleanup unavailable")
            }
            if(close) onSave(note)
            true
        } catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {error="تعذر الحفظ؛ التعديلات والمسودة محفوظة لإعادة المحاولة / Save failed; edits and draft retained";false}
        finally {pending=false;closing=false}
    }
    suspend fun keepDraft():Boolean {
      return try {
        val value=JSONObject().put("editVersion",editVersion()).put("id",id).put("source",sourceRevision).put("title",title).put("details",noteSource()).put("tags",tags)
            .put("calendarLinked",calendarLinked).put("linkedDate",linkedDate).put("linkedCalendar",linkedCalendar.orEmpty()).put("pinned",pinned).put("archived",archived).put("trashed",trashed).put("locked",locked).put("hidden",hidden).put("previewHidden",previewHidden).put("documentA4",documentA4)
            .put("taskSpecs",org.json.JSONArray(taskSpecs)).put("detachedTasks",org.json.JSONArray(detachedTasks)).put("existingTaskLinks",org.json.JSONArray(existingTaskLinks)).put("reminder",reminder).put("background",background).put("color",color).put("border",border)
        ruledOverride?.let {value.put("ruledOverride",it)}
        arabicFontOverride?.let {value.put("arabicFontOverride",it)}
        englishFontOverride?.let {value.put("englishFontOverride",it)}
        withContext(Dispatchers.IO+NonCancellable) { drafts.save(draftKey,value) }
        true
      } catch(_:Exception) {
          error=neoLiteral("تعذر حفظ المسودة؛ انسخ المحتوى قبل الخروج",
              "Draft save failed; copy the content before leaving.")
          false
      }
    }
    fun requestSafeExit(next:()->Unit) {
        if(conflictingDraft!=null || draftRecoveryError!=null) {
            // No changes were made to the conflicting file. It remains recoverable.
            next()
            return
        }
        scope.launch {
            val draftWritten=keepDraft()
            val saved=persistNote(true) // Successful persistence invokes onSave/onDismiss.
            if(!saved && draftWritten) exitWithDraft=next
        }
    }
    LaunchedEffect(title,body.text,rich.value.text,rich.characters,richDirty,tags,pinned,archived,trashed,locked,hidden,previewHidden,documentA4,background,color,border,reminder,taskSpecs,detachedTasks,existingTaskLinks,calendarLinked,linkedDate,linkedCalendar,draftReady,pending,mediaVersion,ruledOverride,arabicFontOverride,englishFontOverride,documentA4) {
        if(!draftReady || pending || editVersion()==lastSavedEdit) return@LaunchedEffect
        delay(600)
        try { keepDraft();withContext(NonCancellable) {persistNote(false)} }
        catch(cancelled:CancellationException) {throw cancelled}
        catch(_:Exception) {error="تعذر حفظ المسودة؛ احتفظ بالمحرر مفتوحًا / Draft save failed; keep editor open"}
    }
    val latestFlush by rememberUpdatedState<suspend () -> Unit>({if(draftReady) {keepDraft();persistNote(false)}})
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var linkedTasksBackgroundAt by remember {mutableLongStateOf(0L)}
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver {_,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                linkedTasksBackgroundAt=System.currentTimeMillis()
                if(linkedTaskSecurity.load().protectTasksOnly&&linkedTaskSecurity.load().autoLockMinutes==0) linkedTasksAuthorized=false
                scope.launch(NonCancellable) { latestFlush() }
            }
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START && linkedTaskSecurity.load().protectTasksOnly && linkedTasksBackgroundAt>0 && System.currentTimeMillis()-linkedTasksBackgroundAt>=linkedTaskSecurity.load().autoLockMinutes*60000L) linkedTasksAuthorized=false
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    neoRegisterLeaveGuard {next -> requestSafeExit(next) }
    if(verifyLinkedTasks) Dialog(onDismissRequest={verifyLinkedTasks=false},properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn)) {
        V11AccountsUnlockScreen(linkedTaskSecurity.load(),linkedTaskSecurity) {linkedTasksAuthorized=true;verifyLinkedTasks=false}
    }
    if(confirmTrashNote) AlertDialog(onDismissRequest={confirmTrashNote=false},title={Text(neoLabel("نقل الملاحظة للمهملات؟","Move note to Trash?"))},text={Text(neoLabel("يمكن استعادتها لاحقًا","You can restore it later"))},confirmButton={TextButton(onClick={trashed=true;confirmTrashNote=false}) {Text(neoLabel("نقل","Move"))}},dismissButton={TextButton(onClick={confirmTrashNote=false}) {Text(neoLabel("إلغاء","Cancel"))}})
    val effectiveBackground=(background.ifBlank {NeoLaunchPreferences(context).defaultNoteBackground}).takeIf {it.isNotBlank()}?.let {runCatching {android.graphics.Color.parseColor(it)}.getOrNull()}
    val baseScheme=MaterialTheme.colorScheme
    val contrast=effectiveBackground?.let {if(androidx.core.graphics.ColorUtils.calculateLuminance(it)>0.45) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White}
    val editorScheme=if(contrast==null) baseScheme else baseScheme.copy(
        surface=androidx.compose.ui.graphics.Color(requireNotNull(effectiveBackground)),onSurface=contrast,onSurfaceVariant=contrast.copy(alpha=.78f),primary=contrast,outline=contrast.copy(alpha=.5f),
        error=if(contrast==androidx.compose.ui.graphics.Color.Black) androidx.compose.ui.graphics.Color(0xFFB3261E) else androidx.compose.ui.graphics.Color(0xFFF2B8B5))
    val openChecklistTask=LocalNeoOpenNoteTask.current
    MaterialTheme(colorScheme=editorScheme) {
    if(appearanceOpen) AlertDialog(
        onDismissRequest={appearanceOpen=false},
        title={Text(neoLabel("مظهر الملاحظة","Note appearance"))},
        text={
            Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(neoLabel(
                    "الإعدادات دي تخص الملاحظة الحالية فقط. ممكن ترجع لإعدادات التطبيق الافتراضية في أي وقت.",
                    "These options apply to this note only. You can return to app defaults anytime."),
                    style=MaterialTheme.typography.bodySmall)
                NeoSwitch(
                    neoLabel("الورق المسطّر","Ruled paper"),ruledPaper
                ) {ruledOverride=it}
                TextButton(onClick={ruledOverride=null}) {
                    Text(neoLabel("اتّباع إعداد التسطير الافتراضي",
                        "Use default ruling")+
                        if(ruledOverride==null) " ✓" else "")
                }
                val arFonts=listOf<String?>(null)+NeoNoteTypefaces.arabicChoices+
                    listOfNotNull(arabicFontOverride).filter{
                        it !in NeoNoteTypefaces.arabicChoices
                    }
                val enFonts=listOf<String?>(null)+NeoNoteTypefaces.englishChoices+
                    listOfNotNull(englishFontOverride).filter{
                        it !in NeoNoteTypefaces.englishChoices
                    }
                NeoChoice(neoLabel("خط النص العربي","Arabic font"),
                    arFonts,arabicFontOverride,{key ->
                        when(key) {
                            null -> neoLabel("حسب إعدادات التطبيق","Follow app default")
                            "amiri" -> "Amiri — "+neoLabel("نسخي","Naskh")
                            "tajawal" -> "Tajawal — "+neoLabel("حديث","Modern")
                            "reem" -> "Reem Kufi — "+neoLabel("كوفي","Kufi")
                            "sans" -> "Sans"
                            "serif" -> "Serif"
                            "mono" -> "Mono"
                            else -> neoLabel("الخط الأساسي","System default")
                        }
                    },{arabicFontOverride=it}
                )
                NeoChoice(neoLabel("خط النص الإنجليزي","English font"),
                    enFonts,englishFontOverride,{key ->
                        when(key) {
                            null -> neoLabel("حسب إعدادات التطبيق","Follow app default")
                            "montserrat" -> "Montserrat"
                            "lora" -> "Lora"
                            "oswald" -> "Oswald"
                            "sans" -> "Sans"
                            "serif" -> "Serif"
                            "mono" -> "Mono"
                            else -> neoLabel("الخط الأساسي","System default")
                        }
                    },{englishFontOverride=it}
                )
                Text(neoLabel(
                    "الخطوط الجديدة مضمّنة داخل التطبيق، وتبقى إعدادات الملاحظة مستقلة عن باقي الملاحظات.",
                    "Bundled typefaces are available offline. Each note keeps its own appearance."),
                    style=MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton={TextButton(onClick={appearanceOpen=false}) {
            Text(neoLabel("تم","Done"))
        }}
    )
    if(paperPaletteOpen) AlertDialog(
        onDismissRequest={paperPaletteOpen=false},
        title={Text(neoLabel("🎨 لون صفحة الملاحظة","🎨 Note paper color"))},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                listOf(
                    listOf(
                        "" to neoLabel("افتراضي","Default"),
                        "#FFF4B8" to neoLabel("أصفر","Yellow"),
                        "#D9EFDA" to neoLabel("أخضر","Green"),
                        "#D6E9FF" to neoLabel("أزرق","Blue")
                    ),
                    listOf(
                        "#F9DCE9" to neoLabel("وردي","Pink"),
                        "#E8DDF5" to neoLabel("بنفسجي","Purple"),
                        "#FFE1C3" to neoLabel("برتقالي","Orange"),
                        "#E3E6EB" to neoLabel("رصاصي","Gray")
                    )
                ).forEach { colors ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        colors.forEach { (hex,label) ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally
                            ) {
                                val swatch=if(hex.isBlank()) MaterialTheme.colorScheme.surface
                                    else androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                                Surface(
                                    onClick={selectPaperColor(hex);paperPaletteOpen=false},
                                    modifier=Modifier.size(48.dp),
                                    shape=androidx.compose.foundation.shape.CircleShape,
                                    color=swatch,
                                    border=androidx.compose.foundation.BorderStroke(
                                        if(background==hex) 3.dp else 1.dp,
                                        if(background==hex) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center) {
                                        if(background==hex) Text("✓",color=androidx.compose.ui.graphics.Color.Black)
                                    }
                                }
                                Text(label,maxLines=1,style=MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton={TextButton(onClick={paperPaletteOpen=false}){Text(neoLabel("إغلاق","Close"))}}
    )
    val hasDocumentBlocks=remember(noteSource()) {
        NeoDocumentBlocks.parse(noteSource()).any {
            it is NeoDocumentBlocks.Block.Image ||
            it is NeoDocumentBlocks.Block.Table ||
            it is NeoDocumentBlocks.Block.PageBreak
        }
    }
    val activeText=if(hasDocumentBlocks) focusedParagraph ?: NeoRichEditorState.open("")
        else rich
    val applyToActiveText:(NeoRichEditorState)->Unit={next ->
        if(!closing) {
            if(hasDocumentBlocks) commitFocusedParagraph?.invoke(next)
            else editRich(next)
        }
    }
    Dialog(onDismissRequest={if(!pending&&!rootWritePending) requestSafeExit(onDismiss)},
        properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true,securePolicy=if(locked||V11SecurityStore(context).load().let {it.protectTasks||it.protectNotes||it.protectApplication}) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit)) {
        // The Android Window, rather than a child padding modifier, owns the
        // system-bar and keyboard-safe bounds. The previous edge-to-edge Dialog
        // measured the footer down to the physical bottom of the display.
        val view=androidx.compose.ui.platform.LocalView.current
        var bottomOcclusionPx by remember(view) {mutableIntStateOf(0)}
        // The view's coordinates can stay unchanged when the IME opens.
        // A layout callback alone therefore misses keyboard appearance.
        // Observe visible-frame changes at the Android window level.
        DisposableEffect(view) {
            val window=(view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            val previous=window?.attributes?.softInputMode
            window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val observer=view.viewTreeObserver
            val listener=android.view.ViewTreeObserver.OnGlobalLayoutListener {
                val visible=android.graphics.Rect()
                view.getWindowVisibleDisplayFrame(visible)
                val location=IntArray(2)
                view.getLocationOnScreen(location)
                val bottom=location[1]+view.height
                val occluded=(bottom-visible.bottom).coerceAtLeast(0)
                if(bottomOcclusionPx!=occluded) {
                    bottomOcclusionPx=occluded
                    val insets=androidx.core.view.ViewCompat.getRootWindowInsets(view)
                    android.util.Log.i("NotesViewport",
                        "bottom="+bottom+" visibleBottom="+visible.bottom+
                        " reserve="+occluded+" ime="+
                        (insets?.getInsets(
                            androidx.core.view.WindowInsetsCompat.Type.ime())?.bottom ?: -1))
                }
            }
            observer.addOnGlobalLayoutListener(listener)
            view.post { listener.onGlobalLayout() }
            onDispose {
                if(observer.isAlive) observer.removeOnGlobalLayoutListener(listener)
                if(previous!=null) window?.setSoftInputMode(previous)
            }
        }
        Surface(
            modifier=Modifier.fillMaxSize(),
            color=effectiveBackground?.let {androidx.compose.ui.graphics.Color(it)} ?: MaterialTheme.colorScheme.surface
        ) {
            Box(Modifier.fillMaxSize()) {
                // Material3 Scaffold measures the bottom bar independently and
                // passes the remaining height to the note content. Previous
                // weighted Column placed the toolbar outside the visible Dialog.
                Scaffold(
                    modifier=Modifier.fillMaxSize(),
                    contentWindowInsets=WindowInsets(0,0,0,0),
                    containerColor=androidx.compose.ui.graphics.Color.Transparent,
                    bottomBar={
                        if(editing) {
                            Column(Modifier.fillMaxWidth()) {
                                NeoNoteQuickBar(
                                    state=activeText,
                                    enabled=!hasDocumentBlocks || focusedParagraph!=null,
                                    onChange=applyToActiveText,
                                    onFormat={
                                        formatInitialTab=0
                                        showFormatSheet=true
                                        noteKeyboard?.hide()
                                    }
                                )
                                // The dialog window may extend below the actually
                                // visible area on API 35+. Reserve precisely the
                                // occluded region reported by the Android window.
                                val imePx=WindowInsets.ime.getBottom(LocalDensity.current)
                                val reservedPx=maxOf(bottomOcclusionPx,imePx)
                                val gap=with(LocalDensity.current){reservedPx.toDp()}
                                if(gap>0.dp) Spacer(Modifier.height(gap))
                            }
                        }
                    }
                ) {innerPadding ->
                Column(Modifier.fillMaxSize().padding(innerPadding)) {
                    BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
              val pageWidth=constraints.maxWidth.toFloat()
              val pageHeight=constraints.maxHeight.toFloat()
              Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(0.dp)) {
                // Stable toolbar order for Arabic and English: Done / undo / redo / insert / format / more.
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=4.dp),
                        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(2.dp)
                    ) {
                        if(false && editing && a4Preview) {
                            TextButton(onClick={a4Preview=false;advancedCanvas=false}) {
                                Text(neoLabel("العودة للكتابة","Back to writing"))
                            }
                            Spacer(Modifier.weight(1f))
                        } else if(editing) {
                            FilledTonalIconButton(
                                enabled=!closing&&!pending&&!rootWritePending,
                                onClick={
                                    scope.launch {
                                        keepDraft()
                                        if(persistNote(false)) {
                                            editing=false
                                            showFormatSheet=false
                                            options=false
                                            advancedCanvas=false
                                            a4Preview=false
                                        }
                                    }
                                }
                            ) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.Check,
                                    neoLabel("تم — حفظ والعودة للقراءة","Done — save and view"))
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(enabled=undo.isNotEmpty()&&!closing,
                                onClick={if(undo.isNotEmpty()){redo.add(body);body=undo.removeAt(undo.lastIndex)}}) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.Undo,
                                    neoLabel("تراجع","Undo"))
                            }
                            IconButton(enabled=redo.isNotEmpty()&&!closing,
                                onClick={if(redo.isNotEmpty()){undo.add(body);body=redo.removeAt(redo.lastIndex)}}) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.Redo,
                                    neoLabel("إعادة","Redo"))
                            }
                            NeoNoteInsertMenu(
                                enabled=!closing&&!pending&&!rootWritePending,
                                insertTable={
                                    insertDocumentBlock(NeoDocumentBlocks.table())
                                    advancedCanvas=true
                                },
                                insertImage={launchMedia("image")},
                                insertCamera={launchMedia("camera")},
                                insertVoice={launchMedia("voice")},
                                insertFile={launchMedia("file")},
                                insertPageBreak={
                                    insertDocumentBlock(NeoDocumentBlocks.PAGE_BREAK)
                                    advancedCanvas=true
                                },
                                openAttachments={mediaShelfVisible=!mediaShelfVisible}
                            )
                            IconButton(onClick={formatInitialTab=0;showFormatSheet=true;noteKeyboard?.hide()}) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.FormatSize,
                                    neoLabel("لوحة التنسيق","Format panel"))
                            }
                        } else {
                            TextButton(onClick={editing=true;advancedCanvas=false;a4Preview=false}) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.Edit,null)
                                Spacer(Modifier.width(5.dp))
                                Text(neoLabel("تعديل","Edit"))
                            }
                            Spacer(Modifier.weight(1f))
                        }
                        Box {
                            IconButton(modifier=Modifier.testTag("notes_more_menu"),onClick={noteMoreMenu=true}) {
                                Icon(androidx.compose.material.icons.Icons.Rounded.MoreVert,
                                    neoLabel("المزيد","More"))
                            }
                            CompositionLocalProvider(LocalLayoutDirection provides if (neoLabel("ar","en")=="ar") LayoutDirection.Rtl else LayoutDirection.Ltr) {
                            DropdownMenu(
                                expanded=noteMoreMenu,
                                onDismissRequest={noteMoreMenu=false}
                            ) {
                                DropdownMenuItem(
                                    text={Text(neoLabel("بحث داخل الملاحظة","Find in note"))},
                                    leadingIcon={Icon(Icons.Rounded.Search, contentDescription=null)},
                                    onClick={noteMoreMenu=false;documentAction="find"})
                                DropdownMenuItem(
                                    text={Text(neoLabel("سجل التعديلات","Version history"))},
                                    leadingIcon={Icon(Icons.Rounded.History, contentDescription=null)},
                                    onClick={noteMoreMenu=false;documentAction="history"})
                                DropdownMenuItem(
                                    text={Text(neoLabel("مشاركة وتصدير","Share & export"))},
                                    leadingIcon={Icon(Icons.Rounded.Share, contentDescription=null)},
                                    onClick={noteMoreMenu=false;documentAction="export"})
                                DropdownMenuItem(
                                    text={Text(neoLabel("الصور والمرفقات","Images and attachments"))},
                                    leadingIcon={Icon(Icons.Rounded.AttachFile, contentDescription=null)},
                                    onClick={noteMoreMenu=false;mediaShelfVisible=true;noteKeyboard?.hide()})
                                DropdownMenuItem(
                                    text={Text(neoLabel("تنسيق الصفحة","Page formatting"))},
                                    leadingIcon={Icon(Icons.Rounded.Description, contentDescription=null)},
                                    onClick={noteMoreMenu=false;editing=true;formatInitialTab=2;showFormatSheet=true;noteKeyboard?.hide()})
                                DropdownMenuItem(
                                    text={Text(neoLabel("خيارات الملاحظة","Note options"))},
                                    leadingIcon={Icon(Icons.Rounded.EventNote, contentDescription=null)},
                                    onClick={noteMoreMenu=false;noteKeyboard?.hide();editing=true;options=true})
                                if(existing==null) DropdownMenuItem(
                                    modifier=Modifier.testTag("notes_recover_old_draft_action"),
                                    text={Text(neoLabel("استرجاع مسودة قديمة","Recover an older draft"))},
                                    onClick={
                                        noteMoreMenu=false
                                        scope.launch {
                                            val oldKey="new-$date-${calendarId?.hashCode() ?: 0}"
                                            val old=runCatching{withContext(Dispatchers.IO){drafts.read(oldKey)}}
                                            if(old.isSuccess && old.getOrNull()!=null) {
                                                legacyRecoveryKey=oldKey
                                                conflictingDraft=old.getOrNull()
                                            } else error=neoLiteral("لا توجد مسودة قديمة متاحة أو تعذرت قراءتها",
                                                "No readable old draft found")
                                        }
                                    })
                            }
                            }
                        }
                    }
                }
                if(editing) {
                    TextField(
                        title,{if(!closing)title=it},singleLine=true,
                        textStyle=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center),
                        placeholder={Text(neoLabel("عنوان الملاحظة","Note title"),modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center)},
                        modifier=Modifier.fillMaxWidth().padding(horizontal=14.dp),
                        colors=TextFieldDefaults.colors(
                            focusedContainerColor=androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor=androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor=androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor=androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                } else if(title.isNotBlank()) {
                    Text(title,modifier=Modifier.fillMaxWidth().padding(
                        horizontal=20.dp,vertical=8.dp),
                        style=MaterialTheme.typography.titleLarge.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center))
                }
                NeoNoteProToolbar(
                    noteId=id,document=rich.value.text,
                    source=noteSource(),title=title,
                    onJump={start,end ->
                        if(rich.value.composition==null) {
                            editing=true
                            rich=rich.copy(value=rich.value.copy(
                                selection=androidx.compose.ui.text.TextRange(start,end)
                            ))
                        }
                    },
                    onRestore={revision ->
                        scope.launch {
                            try {
                                // If the history checkpoint fails, retain the
                                // live edit and surface the error instead.
                                withContext(Dispatchers.IO) {
                                    versions.savePrior(id,title,noteSource(),
                                        System.currentTimeMillis(),force=true)
                                }
                                keepDraft()
                                title=revision.title
                                edit(TextFieldValue(revision.details))
                                rich=NeoRichEditorState.open(revision.details)
                                lastEditorCommit=revision.details
                                richDirty=false
                                editing=true
                                error=null
                            } catch(failure:Exception) {
                                error=failure.message ?: "Could not restore version"
                            }
                        }
                    },
                    onError={error=it},
                    command=documentAction,
                    onCommandHandled={documentAction=null},
                    showToolbar=false
                )
                if(mediaShelfVisible) NeoNoteMediaSection(
                    noteId=id,
                    editable=editing&&!pending&&!closing&&!rootWritePending,
                    onChanged={
                        expectedMetadata=styleStore.snapshot().toString()
                        mediaVersion++
                    },
                    onError={reason -> error=reason},
                    request=mediaRequest,
                    onRequestHandled={mediaRequest=null},
                    onImageImported={name ->
                        insertDocumentBlock(NeoDocumentBlocks.image(name))
                        advancedCanvas=false
                        mediaShelfVisible=false
                    }
                )
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    val direction=when(noteDirection) {
                        "rtl" -> androidx.compose.ui.text.style.TextDirection.Rtl
                        "ltr" -> androidx.compose.ui.text.style.TextDirection.Ltr
                        else -> {
                            val first=rich.value.text.firstOrNull {
                                val kind=Character.getDirectionality(it)
                                kind==Character.DIRECTIONALITY_LEFT_TO_RIGHT ||
                                    kind==Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                                    kind==Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
                            }
                            val kind=first?.let {Character.getDirectionality(it)}
                            if(kind==Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                                kind==Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC ||
                                (kind==null && neoLabel("ar","en")=="ar"))
                                androidx.compose.ui.text.style.TextDirection.Rtl
                            else androidx.compose.ui.text.style.TextDirection.Ltr
                        }
                    }
                    if(editing && !documentA4 && !advancedCanvas && !hasDocumentBlocks) {
                        NeoRuledNoteEditor(
                            value=rich.value,
                            styles=rich.characters,
                            onValueChange={if(!closing) editRich(rich.input(it))},
                            onChecklistToggle={line ->
                                if(!closing && rich.value.composition==null) {
                                    val changed=rich.toggleChecklistAtLine(line)
                                    val formatted=changed.stored()
                                    val rearranged=if(moveCompletedChecklistItems)
                                        NeoSmartLists.moveCompletedToEnd(formatted)
                                    else formatted
                                    if(rearranged!=formatted) editRich(NeoRichEditorState.open(rearranged))
                                    else editRich(changed)
                                }
                            },
                            textDirection=direction,
                            focusRequester=noteInputFocus,
                            modifier=Modifier.fillMaxSize(),
                            ruled=ruledPaper,
                            arabicFont=arabicTypeface,
                            englishFont=englishTypeface
                        )
                    } else {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                .padding(horizontal=if(documentA4) 4.dp else 16.dp)
                        ) {
                            NeoDocumentCanvas(
                                noteId=id,
                                source=noteSource(),
                                editable=editing&&!closing,
                                inlineEditing=editing,
                                onParagraphEdited={startLine,nextParagraph ->
                                    if(!closing) runCatching {
                                        NeoDocumentBlocks.replaceParagraphAt(
                                            noteSource(),startLine,nextParagraph)
                                    }.onSuccess {updated ->
                                        replaceDocumentSource(updated)
                                    }.onFailure {failure ->
                                        error=failure.message ?: "Could not update paragraph"
                                    }
                                },
                                onActiveParagraph={textState,apply ->
                                    focusedParagraph=textState
                                    commitFocusedParagraph=apply
                                },
                                onSourceChange={next ->
                                    if(!closing) replaceDocumentSource(next)
                                },
                                onError={error=it},
                                textDirection=direction,
                                ruled=ruledPaper,
                                arabicFont=arabicTypeface,
                                englishFont=englishTypeface,
                                a4=documentA4,
                                onChecklist={line ->
                                    if(!pending) {
                                        val current=noteSource()
                                        val next=NeoSmartLists.toggleChecklistLine(
                                            current,line,moveCompletedChecklistItems)
                                        if(next!=current) replaceDocumentSource(next)
                                    }
                                }
                            )
                            // Keep two readable lines of scroll room beneath the final
                            // paragraph, also when the formatting bar or IME is present.
                            Spacer(Modifier.height(124.dp))
                        }
                    }
                }
              }

            }
                    if(pending || error!=null) Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=2.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            if(pending) neoLabel("جارٍ الحفظ…","Saving…")
                            else error ?: if(editVersion()!=lastSavedEdit) neoLabel("تعديلات قيد الحفظ","Changes pending save") else draftStatus,
                            style=MaterialTheme.typography.labelSmall,
                            color=if(error!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines=1
                        )
                    }
                }
                }
                if(options && editing) {
                    Surface(
                        modifier=Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.7f),
                        shape=androidx.compose.foundation.shape.RoundedCornerShape(topStart=18.dp,topEnd=18.dp),
                        tonalElevation=8.dp,shadowElevation=10.dp
                    ) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                                Text(neoLabel("خيارات الملاحظة","Note options"),style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                                TextButton(onClick={options=false}) {Text(neoLabel("إغلاق","Close"))}
                            }
                            NeoSwitch(
                                neoLabel("نقل عناصر قائمة التحقق المكتملة إلى نهاية القائمة",
                                    "Move completed checklist items to the end"),
                                moveCompletedChecklistItems
                            ) {enabled ->
                                moveCompletedChecklistItems=enabled
                                checklistPreferences.edit()
                                    .putBoolean("move_completed_to_end",enabled).apply()
                            }
            if(editing && (preview || body.text.lines().any {NeoSmartLists.parse(it)?.kind=="check"})) NeoNotePreview(body.text, checklistsOnly=!preview, onChecklist={ line ->
                if(!pending) {
                    val next=NeoSmartLists.toggleChecklistLine(body.text,line,moveCompletedChecklistItems)
                    if(next!=body.text) edit(TextFieldValue(next))
                }
            },onCreateTask={line,label->
                if(!pending) {val lines=body.text.lines().toMutableList()
                    val found=Regex("<!--task:([A-Za-z0-9_-]+)-->").find(lines[line])
                    val key=found?.groupValues?.get(1) ?: expected.tasks.firstOrNull {it.linkedNoteId==id&&it.title==label}?.id ?: UUID.randomUUID().toString()
                    val taskId=UUID.nameUUIDFromBytes(("note-checklist:$id:$key").toByteArray(Charsets.UTF_8)).toString()
                    val linked=expected.tasks.firstOrNull {(it.id==taskId||it.id==key)&&it.linkedNoteId==id}
                    if(found!=null && linked!=null && openChecklistTask!=null) {
                        scope.launch {keepDraft();if(persistNote(false)) openChecklistTask(linked.id)}
                    } else if(found!=null && linked==null && taskSpecs.none {it.substringBefore('\n')==key}) {
                        error=neoLiteral("المهمة المرتبطة لم تعد موجودة؛ أزل الربط قبل إنشاء مهمة جديدة","The linked task no longer exists; unlink it before creating another task")
                    } else {
                        if(found==null) {lines[line]+=" <!--task:$key-->";edit(TextFieldValue(lines.joinToString("\n")))}
                        val spec="$key\n$label";taskSpecs=taskSpecs.filterNot {it.substringBefore('\n')==key}+spec
                    }
                }
            })
            Regex("<!--task:([A-Za-z0-9_-]+)-->").findAll(body.text).map {it.groupValues[1]}.distinct().filter {key ->
                taskSpecs.none {it.substringBefore('\n')==key} && expected.tasks.none {it.linkedNoteId==id && (it.id==key || it.id==UUID.nameUUIDFromBytes(("note-checklist:$id:$key").toByteArray(Charsets.UTF_8)).toString())}
            }.toList().forEach {key ->
                TextButton(enabled=!pending,onClick={edit(TextFieldValue(body.text.replace("<!--task:$key-->","")))}) {Text(neoLabel("إزالة ربط مهمة غير متاحة","Unlink unavailable task"))}
            }
            if(taskSpecs.isNotEmpty()) Text(neoLabel("${taskSpecs.size} بنود مرتبطة بمهام عند الحفظ","${taskSpecs.size} checklist links to save"),style=MaterialTheme.typography.labelSmall)
            val openTask=LocalNeoOpenNoteTask.current
            expected.tasks.filter {it.linkedNoteId==id && it.id !in detachedTasks}.forEach {task ->
                Row {
                    TextButton(enabled=openTask!=null&&!pending,onClick={scope.launch {keepDraft();if(persistNote(false)) openTask?.invoke(task.id)}}) {Text(if(linkedTasksAuthorized) neoLabel("فتح المهمة: ","Open task: ")+task.title else neoLabel("فتح مهمة محمية","Open protected task"))}
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
            if(editing && options) {
                NeoSwitch(neoLabel("إظهار الملاحظة في التقويم","Show note in calendar"),calendarLinked){calendarLinked=it}
                if(calendarLinked) {
                    NeoDateInput(neoLabel("تاريخ التقويم","Calendar date"),linkedDate){linkedDate=it}
                    NeoChoice(neoLabel("التقويم","Calendar"),listOf<String?>(null)+expected.calendars.map {it.id},linkedCalendar,{key->expected.calendars.firstOrNull {it.id==key}?.name ?: neoLabel("بدون تخصيص","Unassigned")},{linkedCalendar=it})
                }
                if(!linkedTasksAuthorized) TextButton(onClick={verifyLinkedTasks=true}) {Text(neoLabel("التحقق لربط مهمة محمية","Verify to link a protected task"))}
                val linkable=if(!linkedTasksAuthorized) emptyList() else expected.tasks.filter {it.linkedNoteId==null && it.id !in existingTaskLinks}
                if(linkable.isNotEmpty()) {
                    NeoChoice(neoLabel("ربط مهمة موجودة","Link existing task"),listOf<String?>(null)+linkable.map {it.id},selectedTaskLink,{key->linkable.firstOrNull {it.id==key}?.title ?: neoLabel("اختر مهمة","Choose task")},{selectedTaskLink=it})
                    TextButton(enabled=selectedTaskLink!=null&&!pending,onClick={existingTaskLinks=existingTaskLinks+requireNotNull(selectedTaskLink);selectedTaskLink=null}) {Text(neoLabel("ربط","Link"))}
                }
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PushPin,contentDescription=null,
                        tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        NeoSwitch(neoLabel("تثبيت","Pin"),pinned){pinned=it}
                    }
                }
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Archive,contentDescription=null,
                        tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        NeoSwitch(neoLabel("أرشفة","Archive"),archived){archived=it}
                    }
                }
                val security=remember { V11SecurityStore(context.applicationContext) }
                fun canProtect(): Boolean { val settings=security.load();return settings.mode!=V11LockMode.NONE || settings.biometricEnabled }
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        NeoSwitch(neoLabel("قفل الملاحظة بـ PIN أو البصمة","Lock note using PIN or biometrics"),locked){if(!it||canProtect())locked=it else error="Configure app PIN or biometrics in Settings before protecting a note"}
                    }
                }
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VisibilityOff,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        NeoSwitch(neoLabel("إخفاء مقتطف النص من بطاقة الملاحظة","Hide text excerpt on note card"),previewHidden){previewHidden=it}
                    }
                }
                 NeoSwitch(neoLabel("إخفاء الملاحظة من القائمة","Hide note from list"),hidden){hidden=it}
                Text(neoLabel("خلفية الملاحظة","Note background"))
                Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("" to neoLabel("افتراضي","Default"),"#FFF4B8" to neoLabel("أصفر","Yellow"),"#D9EFDA" to neoLabel("أخضر","Green"),"#D6E9FF" to neoLabel("أزرق","Blue"),"#F9DCE9" to neoLabel("وردي","Pink"),"#E8DDF5" to neoLabel("بنفسجي","Purple"),"#FFE1C3" to neoLabel("برتقالي","Orange"),"#E3E6EB" to neoLabel("رمادي","Gray")).forEach {(value,label) -> FilterChip(background==value,{selectPaperColor(value)},label={Text(label)},colors=FilterChipDefaults.filterChipColors(containerColor=if(value.isBlank()) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(value))))}
                }
                OutlinedTextField(color,{selectCardColor(it)},label={Text(neoLabel("لون التقويم #RRGGBB","Calendar color #RRGGBB"))})
            }
            if(editing) Row {
                TextButton(onClick={val content=rich.value.text;(context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText(title,content))}) {Text(neoLabel("نسخ","Copy"))}
                TextButton(onClick={context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,listOf(title,noteDisplayText(body.text)).filter {it.isNotBlank()}.joinToString("\n")),null))}) {Text(neoLabel("مشاركة","Share"))}
            }
            Text(if(pending) neoLabel("جارٍ الحفظ…","Saving…") else if(editVersion()!=lastSavedEdit) neoLabel("تعديلات قيد الحفظ","Changes pending save") else draftStatus,style=MaterialTheme.typography.labelSmall)
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}

                        }
                    }
                }
                if(noteSettingsOpen) NeoNoteSettingsSheet(
                    pinned=pinned,
                    locked=locked,
                    previewHidden=previewHidden,
                    a4Preview=a4Preview,
                    onOutline={noteSettingsOpen=false;documentAction="outline"},
                    onA4={
                        noteSettingsOpen=false;a4Preview=!a4Preview
                        advancedCanvas=a4Preview;noteKeyboard?.hide()
                    },
                    onAttachments={
                        noteSettingsOpen=false;mediaShelfVisible=true;noteKeyboard?.hide()
                    },
                    onRulingFonts={
                        noteSettingsOpen=false;appearanceOpen=true;noteKeyboard?.hide()
                    },
                    onPageColor={
                        noteSettingsOpen=false;paperPaletteOpen=true;noteKeyboard?.hide()
                    },
                    onPreviewHidden={
                        noteSettingsOpen=false;previewHidden=!previewHidden
                    },
                    onPinned={noteSettingsOpen=false;pinned=!pinned},
                    onLocked={
                        noteSettingsOpen=false
                        if(locked) locked=false else {
                            val settings=V11SecurityStore(context.applicationContext).load()
                            if(settings.mode!=V11LockMode.NONE || settings.biometricEnabled)
                                locked=true
                            else error=neoLiteral("فعّل PIN أو البصمة أولًا",
                                "Set up PIN or biometrics first")
                        }
                    },
                    onLinks={
                        noteSettingsOpen=false;noteKeyboard?.hide()
                        editing=true;options=true;showFormatSheet=false
                    },
                    onDismiss={noteSettingsOpen=false}
                )
                if(showFormatSheet && editing) NeoNoteFormatSheet(
                    state=activeText,
                    initialTab=formatInitialTab,
                    onChange=applyToActiveText,
                    direction=noteDirection,
                    onDirectionChange={noteDirection=it},
                    ruledPaper=ruledPaper,
                    onRulingChange={ruledOverride=it},
                    onDefaultRuling={ruledOverride=null},
                    onNoteAppearance={showFormatSheet=false;appearanceOpen=true},
                    onPageColor={showFormatSheet=false;paperPaletteOpen=true},
                    onDismiss={showFormatSheet=false}
                )
                // A blocked save must never hide the recovery action in settings.
                conflictingDraft?.let {stale ->
                    AlertDialog(
                        modifier=Modifier.testTag("notes_draft_conflict"),
                        onDismissRequest={if(!resolvingConflict) onDismiss()},
                        title={Text(neoLabel("تعارض المسودات","Draft conflict"))},
                        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(neoLabel(
                                "هناك مسودة قديمة قد تكون تابعة لملاحظة أخرى. لن نحذفها أو نستبدل البيانات المحفوظة.",
                                "An old draft may belong to another note. It will not be deleted or replace saved data."))
                            TextButton(enabled=!resolvingConflict,onClick={conflictingDraft=null;legacyRecoveryKey=null}) {
                                Text(neoLabel("خروج من الاسترجاع مع الاحتفاظ بالمسودة","Close recovery and keep draft"))
                            }
                        }},
                        confirmButton={
                            TextButton(
                                modifier=Modifier.testTag("notes_recover_copy"),
                                enabled=!resolvingConflict,
                                onClick={
                                    resolvingConflict=true
                                    scope.launch {
                                        try {
                                            val oldKey=legacyRecoveryKey ?: draftKey
                                            val oldId=stale.optString("id")
                                            val newId=UUID.randomUUID().toString()
                                            val copied=withContext(Dispatchers.IO) {
                                                NeoNoteDraftRecovery.copyMedia(
                                                    context.applicationContext,oldId,newId,
                                                    stale.optString("details"))
                                            }
                                            id=newId
                                            draftKey="recovered-"+newId
                                            sourceRevision="<new>"
                                            title=stale.optString("title")
                                            body=TextFieldValue(copied)
                                            rich=NeoRichEditorState.open(copied)
                                            lastEditorCommit=copied
                                            richDirty=false
                                            tags=stale.optString("tags")
                                            linkedDate=stale.optString("linkedDate",linkedDate)
                                            linkedCalendar=stale.optString("linkedCalendar")
                                                .takeIf {it.isNotBlank()}
                                            calendarLinked=stale.optBoolean("calendarLinked",calendarLinked)
                                            background=stale.optString("background",background)
                                            color=stale.optString("color",color)
                                            border=stale.optBoolean("border",border)
                                            pinned=stale.optBoolean("pinned",pinned)
                                            archived=stale.optBoolean("archived",archived)
                                            trashed=stale.optBoolean("trashed",trashed)
                                            hidden=stale.optBoolean("hidden",hidden)
                                            previewHidden=stale.optBoolean("previewHidden",previewHidden); documentA4=stale.optBoolean("documentA4",documentA4)
                                            // A copy must not silently claim old task identities.
                                            taskSpecs=emptyList()
                                            detachedTasks=emptyList()
                                            existingTaskLinks=emptyList()
                                            expectedMetadata=styleStore.snapshot().toString()
                                            val oldVersion=stale.optString("editVersion")
                                            // The historical encrypted source remains untouched.
                                            recoveredLegacyKey=null
                                            legacyRecoveryKey=null
                                            conflictingDraft=null
                                            draftReady=true
                                            error=null
                                            editing=true
                                        } catch(failure:Exception) {
                                            error=neoLiteral(
                                                "تعذر نسخ المسودة ومرفقاتها. الأصل لا يزال محفوظًا.",
                                                "Cannot copy this draft and its media; the original remains intact.")+
                                                " "+(failure.message ?: "")
                                        } finally {
                                            resolvingConflict=false
                                        }
                                    }
                                }
                            ) {Text(neoLabel("استرجاع كنسخة جديدة","Recover as new note"))}
                        },
                        dismissButton={
                            TextButton(
                                modifier=Modifier.testTag("notes_continue_current"),
                                enabled=!resolvingConflict,
                                onClick={
                                    conflictingDraft=null
                                    legacyRecoveryKey=null
                                    error=null
                                    draftReady=true
                                }
                            ) {Text(neoLabel("متابعة الملاحظة الحالية","Continue current note"))}
                        }
                    )
                }
                draftRecoveryError?.let {
                    AlertDialog(
                        onDismissRequest=onDismiss,
                        title={Text(neoLabel("المسودة غير قابلة للقراءة","Draft unreadable"))},
                        text={Text(neoLabel(
                            "تعذر قراءة المسودة المشفرة؛ لم تُحذف. يمكنك متابعة الملاحظة الحالية دون الكتابة فوقها.",
                            "The encrypted draft could not be read, but was not deleted. You may continue without overwriting it."))},
                        confirmButton={
                            TextButton(onClick={
                                draftKey="current-"+UUID.randomUUID().toString()
                                draftRecoveryError=null
                                draftReady=true
                                error=null
                            }) {Text(neoLabel("متابعة الحالية","Continue current"))}
                        },
                        dismissButton={TextButton(onClick=onDismiss) {
                            Text(neoLabel("خروج آمن","Leave safely"))}}
                    )
                }
                exitWithDraft?.let { leave ->
                    AlertDialog(
                        modifier=Modifier.testTag("notes_safe_exit_prompt"),
                        onDismissRequest={exitWithDraft=null},
                        title={Text(neoLabel("فشل الحفظ النهائي","Note save failed"))},
                        text={Text(neoLabel(
                            "الملاحظة لم تُحدَّث، لكن هناك مسودة مشفرة محفوظة. ارجع للتحرير أو اخرج مع الاحتفاظ بالمسودة.",
                            "The note was not updated, but an encrypted draft was saved. Keep editing or leave with the draft preserved."))},
                        confirmButton={
                            TextButton(
                                modifier=Modifier.testTag("notes_leave_with_draft"),
                                onClick={exitWithDraft=null;leave()}
                            ) {Text(neoLabel("خروج مع المسودة","Leave with saved draft"))}
                        },
                        dismissButton={TextButton(onClick={exitWithDraft=null}) {
                            Text(neoLabel("متابعة التحرير","Keep editing"))}}
                    )
                }
            }
        }
    }
}
}
