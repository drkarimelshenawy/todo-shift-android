package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
) {
    val commit = rememberNeoCommit(onCommit)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var committing by remember { mutableStateOf(false) }
    val navigator = LocalNeoFeatures.current
    val store = remember { NeoSmartStore(context.applicationContext) }
    val saved = remember { store.snapshot() }
    var workplace by remember {
        mutableStateOf(
            saved.optString("paintWorkplace").takeIf { id -> workplaces.any { it.id == id } }
                ?: workplaces.firstOrNull()?.id
        )
    }
    var kind by remember {
        mutableStateOf(
            runCatching { ShiftKind.valueOf(saved.optString("paintKind")) }
                .getOrDefault(ShiftKind.FULL_DAY)
        )
    }
    var erase by remember { mutableStateOf(false) }
    var typesOpen by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf<NeoPaintReview?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
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
    LaunchedEffect(request) { if (request > 0 && !bulk) apply(setOf(clicked)) }
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
                    if (workplaces.isEmpty())
                        TextButton(onClick = { navigator.open("WORKPLACES") }) {
                            Text(
                                neoLabel("أضف مكان عمل من الإعدادات", "Add a workplace in Settings")
                            )
                        }
                }
                FilterChip(
                    selected = erase,
                    onClick = {
                        erase = !erase
                        typesOpen = false
                    },
                    label = { Text(neoLabel("ممحاة", "Eraser")) },
                )
                TextButton(onClick = onExit) { Text("✓ " + neoLabel("تم", "Done")) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val undo = LocalNeoUndo.current
                IconButton(enabled = undo.enabled, onClick = undo.perform) {
                    Icon(Icons.Rounded.Undo, neoLabel("تراجع", "Undo"))
                }
                FilterChip(
                    selected = bulk,
                    onClick = {
                        onBulk(!bulk)
                        onClear()
                    },
                    label = { Text(neoLabel("أيام متعددة", "Multiple dates")) },
                )
                if (bulk)
                    TextButton(enabled = dates.isNotEmpty(), onClick = { apply(dates) }) {
                        Text(neoLabel("مراجعة ${dates.size}", "Review ${dates.size}"))
                    }
                else
                    Text(
                        neoShiftLabel(kind),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    review?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!committing) review = null },
            title = { Text(neoLabel("مراجعة الرسم", "Review painting")) },
            text = {
                Text(
                    if (preview.conflicts.isEmpty())
                        neoLabel(
                            "تأكيد تعديل الأيام المحددة؟",
                            "Confirm changes to selected dates?",
                        )
                    else
                        preview.conflicts.joinToString("\n") +
                            neoLabel(
                                "\nسيتم استبدال الشيفتات المتعارضة فقط بعد تأكيدك",
                                "\nConflicting shifts are replaced only after confirmation",
                            )
                )
            },
            confirmButton = {
                Button(
                    enabled = !committing,
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
                TextButton(enabled = !committing, onClick = { review = null }) {
                    Text(neoLabel("إلغاء", "Cancel"))
                }
            },
        )
    }
}
