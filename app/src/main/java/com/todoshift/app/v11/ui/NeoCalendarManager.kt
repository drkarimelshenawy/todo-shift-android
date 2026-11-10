package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

@Composable
fun NeoCalendarManagerDialog(
    db: V11Database,
    primary: String?,
    onPrimary: (String) -> Unit,
    onChange: (V11Database) -> Unit,
    onDismiss: () -> Unit,
) {
    val commit = rememberNeoCommit(onChange)
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#00A4B8") }
    var form by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val original = db.calendars.firstOrNull { it.id == editing }
    val leave =
        rememberNeoEditorExit(
            listOf(
                if (form && name != original?.name.orEmpty()) name else "",
                if (form && color != (original?.colorHex ?: "#00A4B8")) color else "",
            ),
            onDismiss,
        )
    AlertDialog(
        onDismissRequest = leave,
        title = { Text(neoLabel("التقاويم", "Calendars")) },
        text = {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                db.calendars
                    .sortedBy { it.order }
                    .forEach { calendar ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(12.dp)
                                    .background(neoHexColor(calendar.colorHex), CircleShape)
                            )
                            Text(
                                calendar.name,
                                Modifier.weight(1f).padding(horizontal = 6.dp),
                                maxLines = 2,
                            )
                            IconButton(
                                onClick = {
                                    onChange(
                                        V11CalendarController.toggleVisibility(db, calendar.id)
                                    )
                                }
                            ) {
                                Icon(
                                    if (calendar.visible) Icons.Rounded.Visibility
                                    else Icons.Rounded.VisibilityOff,
                                    neoLabel(
                                        "إظهار أو إخفاء ${calendar.name}",
                                        "Show or hide ${calendar.name}",
                                    ),
                                )
                            }
                            IconButton(onClick = { onPrimary(calendar.id) }) {
                                Icon(
                                    if (primary == calendar.id) Icons.Rounded.CheckCircle
                                    else Icons.Rounded.RadioButtonUnchecked,
                                    neoLabel(
                                        "وجهة الإضافة ${calendar.name}",
                                        "Creation destination ${calendar.name}",
                                    ),
                                    tint =
                                        if (primary == calendar.id)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    editing = calendar.id
                                    name = calendar.name
                                    color = calendar.colorHex
                                    form = true
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    neoLabel("تعديل ${calendar.name}", "Edit ${calendar.name}"),
                                )
                            }
                        }
                    }
                Text(
                    neoLabel(
                        "العين للعرض؛ العلامة لوجهة الإضافة",
                        "Eye: visibility · Check: creation destination",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                )
                if (form) {
                    OutlinedTextField(
                        name,
                        { name = it.take(100) },
                        label = { Text(neoLabel("اسم التقويم", "Calendar name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        color,
                        { color = it.take(7) },
                        label = { Text(neoLabel("اللون #RRGGBB", "Color #RRGGBB")) },
                        isError = !color.matches(Regex("#[0-9a-fA-F]{6}")),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    leave.SaveButton(
                        enabled = name.isNotBlank() && color.matches(Regex("#[0-9a-fA-F]{6}")),
                        onClick = {
                            try {
                                val old = db.calendars.firstOrNull { it.id == editing }
                                commit(
                                    if (old == null)
                                        V11CalendarController.addCalendar(db, name.trim(), color)
                                    else
                                        V11CalendarController.updateCalendar(
                                            db,
                                            old.copy(name = name.trim(), colorHex = color),
                                        )
                                ) {
                                    form = false
                                    name = ""
                                    color = "#00A4B8"
                                    editing = null
                                }
                            } catch (e: Exception) {
                                error = e.message
                            }
                        },
                    ) {
                        Text(neoLabel("حفظ", "Save"))
                    }
                    editing?.let { id ->
                        Row {
                            TextButton(
                                onClick = {
                                    onChange(V11CalendarController.moveCalendar(db, id, -1))
                                }
                            ) {
                                Text(neoLabel("أعلى", "Up"))
                            }
                            TextButton(
                                onClick = {
                                    onChange(V11CalendarController.moveCalendar(db, id, 1))
                                }
                            ) {
                                Text(neoLabel("أسفل", "Down"))
                            }
                            TextButton(
                                enabled =
                                    db.calendars.size > 1 &&
                                        db.days.none { it.calendarId == id } &&
                                        db.tasks.none { it.calendarId == id } &&
                                        db.dayItems.none { it.calendarId == id } &&
                                        db.incomeSources.none { it.calendarId == id } &&
                                        db.settlements.none { it.calendarId == id },
                                onClick = {
                                    onChange(V11CalendarController.deleteCalendar(db, id))
                                    form = false
                                    name = ""
                                    color = "#00A4B8"
                                    editing = null
                                },
                            ) {
                                Text(neoLabel("حذف الفارغ", "Delete empty"))
                            }
                        }
                    }
                } else
                    TextButton(
                        onClick = {
                            editing = null
                            name = ""
                            color = "#00A4B8"
                            form = true
                        }
                    ) {
                        Text(neoLabel("إضافة تقويم", "Add calendar"))
                    }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = leave) { Text(neoLabel("تم", "Done")) } },
    )
}
