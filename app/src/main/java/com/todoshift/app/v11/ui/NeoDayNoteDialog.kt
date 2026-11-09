package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.util.UUID

@Composable
fun NeoDayNoteDialog(
    date: LocalDate,
    existing: DayItemV11?,
    onDismiss: () -> Unit,
    onSave: (DayItemV11) -> Unit,
) {
    val context = LocalContext.current
    val styleStore = remember { NeoSmartStore(context.applicationContext) }
    val id = remember(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var color by remember(id) { mutableStateOf(styleStore.noteColor(id)) }
    var border by
        remember(id) {
            mutableStateOf(
                styleStore.snapshot().optJSONObject("noteBorders")?.optBoolean(id, false) ?: false
            )
        }
    var error by remember { mutableStateOf<String?>(null) }
    val originalText =
        existing?.title?.removePrefix("📌 ")?.removePrefix("★ ")?.removePrefix("⚠ ").orEmpty()
    var text by
        remember(existing?.id) {
            mutableStateOf(
                existing
                    ?.title
                    ?.removePrefix("📌 ")
                    ?.removePrefix("★ ")
                    ?.removePrefix("⚠ ")
                    .orEmpty()
            )
        }
    var details by remember(existing?.id) { mutableStateOf(existing?.details.orEmpty()) }
    var marker by
        remember(existing?.id) {
            mutableStateOf(
                when {
                    existing?.title?.startsWith("📌") == true -> "📌"
                    existing?.title?.startsWith("★") == true -> "★"
                    existing?.title?.startsWith("⚠") == true -> "⚠"
                    else -> ""
                }
            )
        }
    val initialMarker = remember(id) { marker }
    val initialBorder = remember(id) { border }
    var confirmDiscard by remember { mutableStateOf(false) }
    fun leave() {
        if (
            text != originalText ||
                details != existing?.details.orEmpty() ||
                color != styleStore.noteColor(id) ||
                marker != initialMarker ||
                border != initialBorder
        )
            confirmDiscard = true
        else onDismiss()
    }
    if (confirmDiscard)
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(neoLabel("تجاهل التعديلات؟", "Discard changes?")) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(neoLabel("تجاهل", "Discard")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    AlertDialog(
        onDismissRequest = { leave() },
        title = { Text(neoLabel("ملاحظة مستقلة", "Independent note") + " • $date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    text,
                    { text = it.take(300) },
                    label = { Text(neoLabel("النص الظاهر بالتقويم", "Calendar note text")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    details,
                    { details = it.take(4000) },
                    label = { Text(neoLabel("تفاصيل", "Details")) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    color,
                    { color = it.take(7) },
                    label = { Text(neoLabel("لون الحد أو النقطة", "Border/dot color")) },
                    isError = !color.matches(Regex("#[0-9a-fA-F]{6}")),
                )
                NeoSwitch(neoLabel("حد حول النص بدل نقطة", "Text border instead of dot"), border) {
                    border = it
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                NeoChoice(
                    neoLabel("رمز", "Marker"),
                    listOf("", "📌", "★", "⚠"),
                    marker,
                    { it.ifEmpty { neoLabel("بدون", "None") } },
                    { marker = it },
                )
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank() && color.matches(Regex("#[0-9a-fA-F]{6}")),
                onClick = {
                    try {
                        styleStore.setNoteColor(id, color)
                        styleStore.update { o ->
                            val borders = o.optJSONObject("noteBorders") ?: org.json.JSONObject()
                            borders.put(id, border)
                            o.put("noteBorders", borders)
                        }
                        onSave(
                            (existing
                                    ?: DayItemV11(
                                        id,
                                        date.toString(),
                                        type = DayItemTypeV11.NOTE,
                                        title = "",
                                    ))
                                .copy(
                                    title =
                                        (if (marker.isBlank()) "" else "$marker ") + text.trim(),
                                    details = details,
                                )
                        )
                    } catch (e: Exception) {
                        error = e.message
                    }
                },
            ) {
                Text(neoLabel("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = { leave() }) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}
