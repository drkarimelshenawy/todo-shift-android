package com.todoshift.app.v11.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.util.UUID

data class NeoEditorWriteState(
    val pending: Boolean = false,
    val error: String? = null,
    // Actual pending state disables actions immediately; short writes should not flash Saving.
    val showProgress: Boolean = pending,
)

val LocalNeoEditorWriteState = staticCompositionLocalOf { NeoEditorWriteState() }

@Composable
fun TaskEditorDialogV11(
    existing: TaskV11?, db: V11Database, workplaces: List<Workplace>,
    onDismiss: () -> Unit, onSave: (TaskV11) -> Unit,
) = TaskEditorDialogV11(existing, db, workplaces, onDismiss, externalQuick = false, onSave = onSave)

@Composable
fun TaskEditorDialogV11(
    existing: TaskV11?,
    db: V11Database,
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    externalQuick: Boolean,
    onSave: (TaskV11) -> Unit,
) {
    if(!externalQuick) {
        FullTaskEditorDialogV11(existing,db,workplaces,onDismiss,onSave)
        return
    }
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
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(externalQuick) {
        if (externalQuick) { kotlinx.coroutines.delay(150); titleFocus.requestFocus(); keyboard?.show() }
    }
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
    val leave = rememberNeoEditorExit(listOf(title, date), onDismiss)
    AlertDialog(
        onDismissRequest = { leave() },
        title = { Text(neoLabel("مهمة جديدة", "New task")) },
        text = {
            Column(Modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                write.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (write.showProgress) Text(neoLabel("جارٍ الحفظ", "Saving"))
                OutlinedTextField(
                    title,
                    { title = it.take(300) },
                    label = { Text(neoLabel("العنوان", "Title")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                )
                if (!externalQuick) OutlinedButton(
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
                if (!externalQuick) TextButton(onClick = { advanced = true }) {
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
            leave.SaveButton(
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
        dismissButton = { TextButton(enabled = !write.pending, onClick = { leave() }) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}

