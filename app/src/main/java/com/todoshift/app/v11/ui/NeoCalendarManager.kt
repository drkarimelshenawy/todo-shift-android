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
    var deleting by remember { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf<String?>(null) }
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
                Modifier.fillMaxWidth().imePadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Only the calendar list scrolls when long; the dialog itself
                // wraps its content, with actions immediately below the rows.
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(max = 264.dp)
                        .verticalScroll(rememberScrollState()),
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
                                enabled = db.calendars.size > 1,
                                onClick = {deleting=id;destination=null},
                            ) {
                                Text(neoLabel("حذف أو نقل السجلات", "Delete or transfer records"))
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
    deleting?.let {id ->
        val referenced=db.days.any {it.calendarId==id}||db.tasks.any {it.calendarId==id}||db.dayItems.any {it.calendarId==id}||db.incomeSources.any {it.calendarId==id}||db.settlements.any {it.calendarId==id}
        val targets=db.calendars.filter {it.id!=id && it.financial==db.calendars.first {c->c.id==id}.financial}
        AlertDialog(onDismissRequest={deleting=null},title={Text(neoLabel("حذف التقويم","Delete calendar"))},text={Column {
            Text(if(referenced) neoLabel("انقل السجلات إلى تقويم آخر؛ لن تُحذف بياناتك. اختر تقويمًا بلا أيام عمل متعارضة.","Move all records to another calendar. Existing IDs and history are preserved; select a calendar without matching work dates.") else neoLabel("التقويم فارغ. تأكيد الحذف؟","This calendar is empty. Delete it?"))
            if(referenced) NeoChoice(neoLabel("التقويم المستلم","Destination calendar"),listOf<String?>(null)+targets.map {it.id},destination,{key->targets.firstOrNull {it.id==key}?.name ?: neoLabel("اختر تقويمًا","Select calendar")},{destination=it})
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        }},confirmButton={TextButton(enabled=!referenced||destination!=null,onClick={try {
            val next=if(referenced) V11CalendarController.transferAndDelete(db,id,requireNotNull(destination)) else V11CalendarController.deleteCalendar(db,id)
            commit(next) {if(primary==id) next.calendars.firstOrNull {it.id==destination}?.let {onPrimary(it.id)};deleting=null;editing=null;form=false}
        } catch(failure:Exception) {error=failure.message}}){Text(neoLabel("تأكيد","Confirm"))}},dismissButton={TextButton(onClick={deleting=null;error=null}){Text(neoLabel("إلغاء","Cancel"))}})
    }

}
