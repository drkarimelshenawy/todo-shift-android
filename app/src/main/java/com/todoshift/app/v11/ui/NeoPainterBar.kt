package com.todoshift.app.v11.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate

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
    val context = LocalContext.current
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
                onCommit(result.candidate)
                onClear()
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
                            label = { Text(neoShiftLabel(type)) },
                        )
                    }
                }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val favoriteWorkplace = saved.optString("favoriteWorkplace")
                val favoriteKind =
                    runCatching { ShiftKind.valueOf(saved.optString("favoriteKind")) }.getOrNull()
                if (favoriteKind != null && workplaces.any { it.id == favoriteWorkplace })
                    AssistChip(
                        onClick = {
                            workplace = favoriteWorkplace
                            kind = favoriteKind
                            erase = false
                            typesOpen = false
                        },
                        label = {
                            Text("★ " + workplaces.first { it.id == favoriteWorkplace }.name)
                        },
                    )
                workplaces.forEach { place ->
                    FilterChip(
                        selected = workplace == place.id && !erase,
                        onClick = {
                            workplace = place.id
                            erase = false
                            typesOpen = true
                            runCatching { store.update { it.put("paintWorkplace", place.id) } }
                                .onFailure { error = it.message }
                        },
                        label = { Text(place.name) },
                    )
                }
                FilterChip(
                    selected = erase,
                    onClick = {
                        erase = !erase
                        typesOpen = false
                    },
                    label = { Text(neoLabel("ممحاة", "Eraser")) },
                )
                TextButton(onClick = onExit) { Text(neoLabel("تم", "Done")) }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = bulk,
                    onClick = {
                        onBulk(!bulk)
                        onClear()
                    },
                    label = { Text(neoLabel("تحديد متعدد", "Multiple dates")) },
                )
                if (bulk)
                    TextButton(enabled = dates.isNotEmpty(), onClick = { apply(dates) }) {
                        Text(neoLabel("مراجعة ${dates.size}", "Review ${dates.size}"))
                    }
                if (!erase)
                    Text(
                        neoLabel(
                            "أوقات جديدة من ٩ صباحًا؛ الرسم نسبي",
                            "New times offset from 09:00; bands are decorative",
                        ),
                        style = MaterialTheme.typography.labelSmall,
                    )
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
    review?.let { preview ->
        AlertDialog(
            onDismissRequest = { review = null },
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
                    onClick = {
                        if (db == preview.before) {
                            onCommit(preview.candidate)
                            onClear()
                            review = null
                        } else {
                            error = "Data changed; review again"
                            review = null
                        }
                    }
                ) {
                    Text(neoLabel("تأكيد", "Confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { review = null }) { Text(neoLabel("إلغاء", "Cancel")) }
            },
        )
    }
}
