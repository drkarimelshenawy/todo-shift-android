package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NeoPainterBar(
    db: V11Database,
    workplaces: List<Workplace>,
    calendar: String?,
    dates: Set<LocalDate>,
    clicked: LocalDate,
    request: Int,
    bulk: Boolean,
    onBulk: (Boolean) -> Unit,
    onClear: () -> Unit,
    onCommit: (V11Database) -> Unit,
    onExit: () -> Unit,
    onPreview: (V11Database?) -> Unit = {},
) {
    val commit = rememberNeoCommit(onCommit)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var committing by remember { mutableStateOf(false) }
    val navigator = LocalNeoFeatures.current
    val store = remember { NeoSmartStore(context.applicationContext) }
    val saved = remember { store.snapshot() }
    var workplace by rememberSaveable {
        mutableStateOf(
            saved.optString("paintWorkplace").takeIf { id -> workplaces.any { it.id == id } }
                ?: workplaces.firstOrNull()?.id
        )
    }
    var kind by rememberSaveable {
        mutableStateOf(
            runCatching { ShiftKind.valueOf(saved.optString("paintKind")) }
                .getOrDefault(ShiftKind.FULL_DAY)
        )
    }
    var erase by rememberSaveable { mutableStateOf(false) }
    var typesOpen by rememberSaveable { mutableStateOf(false) }
    var review by remember { mutableStateOf<NeoPaintReview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val write = LocalNeoEditorWriteState.current
    var queue by rememberSaveable(
        stateSaver = androidx.compose.runtime.saveable.listSaver<List<NeoPaintEdit>, String>(
            save = { it.map(NeoPaintEdit::encode) }, restore = { it.map(NeoPaintEdit::decode) },
        )
    ) { mutableStateOf(emptyList<NeoPaintEdit>()) }
    var inFlight by remember { mutableStateOf(false) }
    var queuePaused by remember { mutableStateOf(false) }
    var finishWhenSaved by remember { mutableStateOf(false) }
    val rawCommit = LocalNeoCommitRequest.current
    val draft = runCatching { queue.fold(db) { current, edit -> edit.apply(current) } }
    LaunchedEffect(db, queue) { onPreview(draft.getOrNull().takeIf { queue.isNotEmpty() }) }
    DisposableEffect(Unit) { onDispose { onPreview(null) } }
    LaunchedEffect(queue, db, write.pending, write.error, queuePaused) {
        if (queue.isEmpty() || inFlight || write.pending || write.error != null || queuePaused) return@LaunchedEffect
        val head = queue.first()
        try {
            val next = head.apply(db)
            inFlight = true
            val done: (Boolean) -> Unit = { saved ->
                inFlight = false
                if (saved) { queue = queue.drop(1); onClear() }
                else { queuePaused = true; error = "لم يتأكد الحفظ؛ الرسم محفوظ كمسودة / Save not confirmed; painting draft retained" }
            }
            if (rawCommit != null) rawCommit(next, done)
            else { onCommit(next); done(true) }
        } catch (failure: Exception) {
            inFlight = false
            queuePaused = true
            error = failure.message
        }
    }
    LaunchedEffect(queue, finishWhenSaved, inFlight) {
        if (finishWhenSaved && queue.isEmpty() && !inFlight) { onPreview(null); onExit() }
    }
    var confirmExit by remember { mutableStateOf(false) }
    var exitDestination by remember { mutableStateOf<(() -> Unit)?>(null) }
    var toolsOpen by remember { mutableStateOf(false) }
    fun requestExit(next: () -> Unit) {
        if (committing || write.pending || queue.isNotEmpty()) { finishWhenSaved = true; return }
        if (dates.isNotEmpty()) {
            exitDestination = next
            confirmExit = true
        } else {
            typesOpen = false
            onExit()
            next()
        }
    }
    neoRegisterLeaveGuard { requestExit(it) }
    BackHandler {
        when {
            committing || write.pending || queue.isNotEmpty() -> { finishWhenSaved = true }
            review != null -> review = null
            typesOpen -> typesOpen = false
            else -> requestExit {}
        }
    }
    LaunchedEffect(workplaces) {
        if (workplace != null && workplaces.none { it.id == workplace }) {
            workplace = null
            typesOpen = false
            error = "Workplace no longer available; select a workplace before painting"
        }
    }
    if (confirmExit)
        AlertDialog(
            onDismissRequest = {
                confirmExit = false
                exitDestination = null
            },
            title = { Text(neoLabel("تجاهل الأيام المحددة؟", "Discard selected dates?")) },
            text = {
                Text(
                    neoLabel(
                        "لم تُحفظ هذه المجموعة بعد؛ لن تُحذف الورديات المحفوظة.",
                        "This selection is not saved; existing shifts will not be deleted.",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val next = exitDestination
                        confirmExit = false
                        exitDestination = null
                        onClear()
                        onExit()
                        next?.invoke()
                    }
                ) {
                    Text(neoLabel("تجاهل", "Discard"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmExit = false
                        exitDestination = null
                    }
                ) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    fun apply(selected: Set<LocalDate>) {
        if (selected.isEmpty() || calendar == null) return
        try {
            val result = NeoPaintOperations.preview(db, selected, calendar, kind, workplace, erase)
            if (result.conflicts.isNotEmpty() || erase || selected.size > 1) review = result
            else {
                commit(result.candidate) { onClear() }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(request) {
        if (request > 0 && !bulk && calendar != null) {
            try {
                val base = queue.fold(db) { current, edit -> edit.apply(current) }
                queue = queue + NeoPaintEdit.tap(base, clicked, calendar, kind, workplace, erase)
                finishWhenSaved = false
            } catch (failure: Exception) { error = failure.message }
        }
    }
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            if (typesOpen)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ShiftKind.entries.forEach { type ->
                        FilterChip(
                            selected = kind == type && !erase,
                            onClick = {
                                kind = type
                                erase = false
                                typesOpen = false
                                runCatching { store.update { it.put("paintKind", type.name) } }
                                    .onFailure { error = it.message }
                            },
                            leadingIcon = {
                                Box(
                                    Modifier.size(10.dp)
                                        .background(
                                            neoHexColor(
                                                if (type == ShiftKind.OFF) "#748195"
                                                else
                                                    LocalNeoAppearance.current.shiftPaints[type]
                                                        ?.takeIf { it.mode != NeoPaintMode.INHERIT }
                                                        ?.first
                                                        ?: workplaces
                                                            .firstOrNull { it.id == workplace }
                                                            ?.colorHex
                                                        ?: "#00A4B8"
                                            ),
                                            CircleShape,
                                        )
                                )
                            },
                            label = { Text(neoShiftLabel(type)) },
                        )
                    }
                }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val favoriteWorkplace = saved.optString("favoriteWorkplace")
                    val favoriteKind =
                        runCatching { ShiftKind.valueOf(saved.optString("favoriteKind")) }
                            .getOrNull()
                    if (favoriteKind != null && workplaces.any { it.id == favoriteWorkplace })
                        AssistChip(
                            onClick = {
                                workplace = favoriteWorkplace
                                kind = favoriteKind
                                erase = false
                                typesOpen = false
                                runCatching {
                                        store.update {
                                            it.put("paintWorkplace", favoriteWorkplace)
                                                .put("paintKind", favoriteKind.name)
                                        }
                                    }
                                    .onFailure { error = it.message }
                            },
                            label = {
                                Text("★ " + workplaces.first { it.id == favoriteWorkplace }.name)
                            },
                        )
                    workplaces.forEach { place ->
                        val color = neoHexColor(place.colorHex)
                        val ink = if (color.luminance() > .179f) Color.Black else Color.White
                        FilterChip(
                            selected = workplace == place.id && !erase,
                            onClick = {
                                workplace = place.id
                                erase = false
                                typesOpen = true
                                runCatching { store.update { it.put("paintWorkplace", place.id) } }
                                    .onFailure { error = it.message }
                            },
                            leadingIcon = {
                                Box(Modifier.size(12.dp).background(color, CircleShape))
                            },
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    containerColor = color.copy(alpha = .12f),
                                    selectedContainerColor = color,
                                    selectedLabelColor = ink,
                                    selectedLeadingIconColor = ink,
                                ),
                            label = { Text(place.name, maxLines = 1) },
                        )
                    }
                    FilledIconToggleButton(
                    checked = erase,
                    onCheckedChange = {
                        erase = it
                        typesOpen = false
                    },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, neoLabel("ممحاة", "Eraser"))
                }
                    if (workplaces.isEmpty())
                        TextButton(onClick = { navigator.open("WORKPLACES") }) {
                            Text(
                                neoLabel("أضف مكان عمل من الإعدادات", "Add a workplace in Settings")
                            )
                        }
                }
                Box {
                    IconButton(onClick = { toolsOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, neoLabel("أدوات الرسم", "Painting tools"))
                    }
                    DropdownMenu(expanded = toolsOpen, onDismissRequest = { toolsOpen = false }) {
                        val undo = LocalNeoUndo.current
                        DropdownMenuItem(
                            text = { Text(neoLabel("تراجع", "Undo")) },
                            enabled = undo.enabled && queue.isEmpty() && !write.pending,
                            onClick = {
                                toolsOpen = false
                                undo.perform()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(neoLabel("أيام متعددة", "Multiple dates")) },
                            enabled = queue.isEmpty() && !write.pending,
                            onClick = {
                                toolsOpen = false
                                onBulk(!bulk)
                                onClear()
                            },
                        )
                    }
                }
                IconButton(onClick = { requestExit {} }) {
                    Icon(Icons.Rounded.Check, neoLabel("تم", "Done"))
                }
            }
            if (bulk)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        neoLabel("المحدد: ${dates.size}", "Selected: ${dates.size}"),
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    TextButton(
                        enabled = dates.isNotEmpty() && !write.pending,
                        onClick = { apply(dates) },
                    ) {
                        Text(neoLabel("مراجعة ${dates.size}", "Review ${dates.size}"))
                    }
                }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (queuePaused) Row {
                TextButton(onClick = { error = null; queuePaused = false }) { Text(neoLabel("إعادة المحاولة", "Retry")) }
                TextButton(enabled = !inFlight, onClick = { queue = emptyList(); queuePaused = false; error = null; finishWhenSaved = false }) {
                    Text(neoLabel("تجاهل الرسم غير المحفوظ", "Discard unsaved painting"))
                }
            }
        }
    }
    review?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!committing) review = null },
            title = { Text(neoLabel("مراجعة الرسم", "Review painting")) },
            text = {
                Column(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    neoPaintChanges(preview).forEach { change ->
                        val name =
                            db.calendars.firstOrNull { it.id == change.calendarId }?.name
                                ?: change.calendarId
                        Text(
                            "${change.date} • $name",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                        change.before
                            .filter { before -> change.after.none { it.id == before.id } }
                            .forEach { segment ->
                                val place =
                                    workplaces
                                        .firstOrNull { it.id == segment.workplaceId }
                                        ?.name
                                        .orEmpty()
                                Text(
                                    neoLabel("إزالة: ", "Remove: ") +
                                        neoShiftLabel(segment.kind) +
                                        " • $place • " +
                                        neoSegmentClock(segment, LocalNeoEnglish.current) +
                                        " • ${segment.id}"
                                )
                            }
                        change.after
                            .filter { after -> change.before.none { it.id == after.id } }
                            .forEach { segment ->
                                val place =
                                    workplaces
                                        .firstOrNull { it.id == segment.workplaceId }
                                        ?.name
                                        .orEmpty()
                                Text(
                                    neoLabel("إضافة: ", "Add: ") +
                                        neoShiftLabel(segment.kind) +
                                        " • $place • " +
                                        neoSegmentClock(segment, LocalNeoEnglish.current)
                                )
                            }
                    }
                    Text(
                        neoLabel(
                            "تُحفظ الملاحظات والمرفقات والمقاطع غير المتعارضة. تأكيد الاستبدال يزيل المقاطع المسماة كاملة فقط.",
                            "Notes, attachments and unrelated segments are preserved. Replacement removes only the named whole segments.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !committing && !write.pending,
                    onClick = {
                        if (db != preview.before) {
                            error = "Data changed; review again"
                            review = null
                        } else {
                            committing = true
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        com.todoshift.app.backup.createNeoSafetyCopy(
                                            context.applicationContext,
                                            preview.before,
                                        )
                                    }
                                    commit(preview.candidate) {
                                        onClear()
                                        review = null
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Safety copy failed; no change saved"
                                } finally {
                                    committing = false
                                }
                            }
                        }
                    },
                ) {
                    Text(neoLabel("تأكيد", "Confirm"))
                }
            },
            dismissButton = {
                TextButton(enabled = !committing && !write.pending, onClick = { review = null }) {
                    Text(neoLabel("إلغاء", "Cancel"))
                }
            },
        )
    }
}
