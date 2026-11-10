package com.todoshift.app.v11.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.util.UUID

data class NeoEditorWriteState(val pending: Boolean = false, val error: String? = null)

val LocalNeoEditorWriteState = staticCompositionLocalOf { NeoEditorWriteState() }

@Composable
fun TaskEditorDialogV11(
    existing: TaskV11?,
    db: V11Database,
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    onSave: (TaskV11) -> Unit,
) {
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title.orEmpty()) }
    var date by
        rememberSaveable(existing?.id) {
            mutableStateOf(existing?.date ?: LocalDate.now().toString())
        }
    var advanced by rememberSaveable { mutableStateOf(existing != null) }
    var draftId by rememberSaveable { mutableStateOf(existing?.id ?: UUID.randomUUID().toString()) }
    var discard by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    val write = LocalNeoEditorWriteState.current
    LaunchedEffect(write.error) { if (write.error != null) submitted = false }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val validDate = runCatching { LocalDate.parse(date) }.getOrNull()
    if (advanced) {
        FullTaskEditorDialogV11(
            existing ?: TaskV11(draftId, title, date = date),
            db,
            workplaces,
            onDismiss,
            onSave,
        )
        return
    }
    fun leave() {
        if (write.pending) return
        if (title.isNotBlank()) discard = true else onDismiss()
    }
    if (discard)
        AlertDialog(
            onDismissRequest = { discard = false },
            title = { Text(neoLabel("تجاهل المسودة؟", "Discard draft?")) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(neoLabel("تجاهل", "Discard")) }
            },
            dismissButton = {
                TextButton(onClick = { discard = false }) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    AlertDialog(
        onDismissRequest = { leave() },
        title = { Text(neoLabel("مهمة جديدة", "New task")) },
        text = {
            Column(Modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                write.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (write.pending) Text(neoLabel("جارٍ الحفظ", "Saving"))
                OutlinedTextField(
                    title,
                    { title = it.take(300) },
                    label = { Text(neoLabel("العنوان", "Title")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        val d = validDate ?: LocalDate.now()
                        DatePickerDialog(
                                context,
                                { _, y, m, day -> date = LocalDate.of(y, m + 1, day).toString() },
                                d.year,
                                d.monthValue - 1,
                                d.dayOfMonth,
                            )
                            .show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(date)
                }
                TextButton(onClick = { advanced = true }) {
                    Text(
                        neoLabel(
                            "المزيد: ملاحظات ووقت وتذكير وروابط",
                            "More: notes, time, reminder and links",
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && validDate != null && !submitted && !write.pending,
                onClick = {
                    submitted = true
                    keyboard?.hide()
                    onSave(TaskV11(draftId, title.trim(), date = date))
                },
            ) {
                Text(neoLabel("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = { leave() }) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}
