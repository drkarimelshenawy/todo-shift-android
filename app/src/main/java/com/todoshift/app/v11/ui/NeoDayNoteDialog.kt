package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalNeoNoteCommitted = staticCompositionLocalOf<(V11Database) -> Unit> { {} }

@Composable
fun NeoDayNoteDialog(
    date: LocalDate,
    existing: DayItemV11?,
    expectedDatabase: V11Database,
    calendarId: String?,
    onDismiss: () -> Unit,
    onSave: (DayItemV11) -> Unit,
) {
    val context = LocalContext.current
    val styleStore = remember { NeoSmartStore(context.applicationContext) }
    val expectedMetadata = remember { styleStore.snapshot().toString() }
    val expected = remember { expectedDatabase }
    val committed = LocalNeoNoteCommitted.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf(false) }
    val id = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    val intendedCalendarId by rememberSaveable(id) { mutableStateOf(calendarId) }
    val noteUnchanged =
        rememberNeoDraftUnchanged("note-$id", expectedDatabase.dayItems.firstOrNull { it.id == id })
    val styleUnchanged = rememberNeoDraftUnchanged("note-style-$id", expectedMetadata)
    var color by rememberSaveable(id) { mutableStateOf(styleStore.noteColor(id)) }
    var border by
        rememberSaveable(id) {
            mutableStateOf(
                styleStore.snapshot().optJSONObject("noteBorders")?.optBoolean(id, false) ?: false
            )
        }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var text by
        rememberSaveable(existing?.id) {
            mutableStateOf(
                existing
                    ?.title
                    ?.removePrefix("📌 ")
                    ?.removePrefix("★ ")
                    ?.removePrefix("⚠ ")
                    .orEmpty()
            )
        }
    var details by rememberSaveable(existing?.id) { mutableStateOf(existing?.details.orEmpty()) }
    var marker by
        rememberSaveable(existing?.id) {
            mutableStateOf(
                when {
                    existing?.title?.startsWith("📌") == true -> "📌"
                    existing?.title?.startsWith("★") == true -> "★"
                    existing?.title?.startsWith("⚠") == true -> "⚠"
                    else -> ""
                }
            )
        }
    val gate = LocalNeoLeaveGate.current
    val leave =
        rememberNeoEditorExit(listOf(text, details, color, marker, border), onDismiss, pending)
    AlertDialog(
        onDismissRequest = { if (!pending) leave() },
        title = { Text(neoLabel("ملاحظة مستقلة", "Independent note") + " • $date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    text,
                    { if (!pending) text = it.take(300) },
                    label = { Text(neoLabel("النص الظاهر بالتقويم", "Calendar note text")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    details,
                    { if (!pending) details = it.take(4000) },
                    label = { Text(neoLabel("تفاصيل", "Details")) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    color,
                    { if (!pending) color = it.take(7) },
                    label = { Text(neoLabel("لون الحد أو النقطة", "Border/dot color")) },
                    isError = !color.matches(Regex("#[0-9a-fA-F]{6}")),
                )
                NeoSwitch(neoLabel("حد حول النص بدل نقطة", "Text border instead of dot"), border) {
                    if (!pending) border = it
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                NeoChoice(
                    neoLabel("رمز", "Marker"),
                    listOf("", "📌", "★", "⚠"),
                    marker,
                    { it.ifEmpty { neoLabel("بدون", "None") } },
                    { if (!pending) marker = it },
                )
            }
        },
        confirmButton = {
            leave.SaveButton(
                enabled = !pending && text.isNotBlank() && color.matches(Regex("#[0-9a-fA-F]{6}")),
                onClick = {
                    if (!noteUnchanged || !styleUnchanged) {
                        error =
                            "Note was saved or changed elsewhere. Draft retained; review the stored note before reopening."
                        return@SaveButton
                    }
                    val note =
                        (existing
                                ?: DayItemV11(
                                    id,
                                    date.toString(),
                                    type = DayItemTypeV11.NOTE,
                                    title = "",
                                    calendarId = intendedCalendarId,
                                ))
                            .copy(
                                title = (if (marker.isBlank()) "" else "$marker ") + text.trim(),
                                details = details,
                            )
                    val savedExit = gate?.takeSavedExit()
                    pending = true
                    error = null
                    scope.launch {
                        try {
                            val next =
                                withContext(Dispatchers.IO) {
                                    com.todoshift.app.backup
                                        .Neo4LocalBackup(context.applicationContext)
                                        .saveNote(expected, expectedMetadata, note, color, border)
                                }
                            committed(next)
                            onSave(note)
                            savedExit?.invoke()
                        } catch (failure: Exception) {
                            error = failure.message ?: "Note save failed; draft retained"
                        } finally {
                            pending = false
                        }
                    }
                },
            ) {
                Text(neoLabel("حفظ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!pending) leave() }) { Text(neoLabel("إلغاء", "Cancel")) }
        },
    )
}
