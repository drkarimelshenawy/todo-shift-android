package com.todoshift.app.v11.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.todoshift.app.data.Workplace
import com.todoshift.app.reminders.ToDoShiftReminder
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun V11DayDetailsScreen(
    initialDate: LocalDate,
    db: V11Database,
    workplaces: List<Workplace>,
    activeCalendarId: String?,
    onBack: () -> Unit,
    onDatabaseChange: (V11Database) -> Unit,
    financeAuthorized: Boolean = false,
    financeAmountsVisible: Boolean = false,
) {
    var editingNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    val noteNavigator = LocalNeoFeatures.current
    val showDayMoney = !LocalV11NeoEnabled.current || (financeAuthorized && financeAmountsVisible)
    var date by remember(initialDate) { mutableStateOf(initialDate) }

    var addType by rememberSaveable { mutableStateOf<DayItemTypeV11?>(null) }

    var pendingEditSegmentId by rememberSaveable { mutableStateOf<String?>(null) }

    var pendingDeleteSegmentId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    fun ensureNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val entries = db.days.filter { it.date == date.toString() }

    val items =
        db.dayItems
            .filter { it.date == date.toString() }
            .sortedWith(
                compareByDescending<DayItemV11> { it.title.startsWith("📌") }
                    .thenBy { it.createdAt }
            )

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .then(
                                if (LocalV11NeoEnabled.current) Modifier.heightIn(min = 58.dp)
                                else Modifier.statusBarsPadding().height(58.dp)
                            )
                            .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, neoLiteral("رجوع", "Back"))
                    }

                    IconButton(onClick = { date = date.minusDays(1) }) {
                        Icon(Icons.Default.ChevronLeft, neoLiteral("اليوم السابق", "Previous day"))
                    }

                    Spacer(Modifier.weight(1f))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(date.toString(), fontWeight = FontWeight.Black)

                        if (date == LocalDate.now()) {
                            Text(
                                neoLiteral("اليوم", "Today"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF4F709C),
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = { date = date.plusDays(1) }) {
                        Icon(Icons.Default.ChevronRight, neoLiteral("اليوم التالي", "Next day"))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp)
        ) {
            Text(
                neoLiteral("الشيفتات", "Shifts"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )

            Spacer(Modifier.height(8.dp))

            if (entries.flatMap { it.segments }.isEmpty()) {
                V11EmptyCard(neoLiteral("لا توجد شيفتات في هذا اليوم", "No shifts on this day"))
            }

            entries.forEach { entry ->
                val calendar = db.calendars.firstOrNull { it.id == entry.calendarId }

                entry.segments.forEach { segment ->
                    val place = workplaces.firstOrNull { it.id == segment.workplaceId }

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { pendingDeleteSegmentId = segment.id }) {
                                Icon(
                                    Icons.Default.Delete,
                                    neoLiteral("حذف", "Delete"),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }

                            if (segment.kind != ShiftKind.OFF) {
                                IconButton(onClick = { pendingEditSegmentId = segment.id }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription =
                                            neoLiteral("تعديل وقت الشيفت", "Edit shift time"),
                                    )
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(neoShiftText(segment.kind), fontWeight = FontWeight.Black)

                                Text(
                                    listOfNotNull(place?.name, calendar?.name).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                )

                                segment.timeRange()?.let { range ->
                                    Text(
                                        "${timeText(range.startMinute)} - ${timeText(range.endMinute)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                            if (LocalV11NeoEnabled.current)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else Color.Gray,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            val dueObligations = remember(db, date) { V11ObligationEngine.itemsForDate(db, date) }

            if (dueObligations.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))

                Text(
                    neoLiteral("التزامات اليوم", "Today's commitments"),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                )

                Spacer(Modifier.height(6.dp))

                dueObligations.forEach { info ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Column(modifier = Modifier.padding(11.dp)) {
                            Text(info.obligation.name, fontWeight = FontWeight.Black)

                            Text(
                                neoObligationState(info.state),
                                style = MaterialTheme.typography.labelMedium,
                            )

                            Text(
                                if (!showDayMoney) "••••••"
                                else
                                    neoLiteral(
                                        "المستحق: ${
                                    "%.2f".format(
                                        info.expected
                                    )
                                } ج • المتبقي: ${
                                    "%.2f".format(
                                        info.remaining
                                    )
                                } ج",
                                        "Expected: ${
                                    "%.2f".format(
                                        info.expected
                                    )
                                } EGP • Remaining: ${
                                    "%.2f".format(
                                        info.remaining
                                    )
                                } EGP",
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                V11AddItemButton(
                    neoLiteral("مهمة", "Task"),
                    Icons.Default.TaskAlt,
                    Modifier.weight(1f),
                ) {
                    addType = DayItemTypeV11.TASK
                }

                V11AddItemButton(
                    neoLiteral("مشوار", "Errand"),
                    Icons.Default.DirectionsCar,
                    Modifier.weight(1f),
                ) {
                    addType = DayItemTypeV11.ERRAND
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                V11AddItemButton(
                    neoLiteral("تذكير", "Reminder"),
                    Icons.Default.Notifications,
                    Modifier.weight(1f),
                ) {
                    addType = DayItemTypeV11.REMINDER
                }

                V11AddItemButton(
                    neoLiteral("ملاحظة", "Note"),
                    Icons.Default.Notes,
                    Modifier.weight(1f),
                ) {
                    addType = DayItemTypeV11.NOTE
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                neoLiteral("تنظيم اليوم", "Day organizer"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                V11EmptyCard(
                    neoLiteral(
                        "لا توجد مهام أو مشاوير أو تذكيرات",
                        "No tasks, errands or reminders",
                    )
                )
            }

            items.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                // Reminder effects follow a confirmed database commit in V11Home.

                                onDatabaseChange(V11DayOrganizerController.delete(db, item.id))
                            }
                        ) {
                            Icon(Icons.Default.Delete, neoLiteral("حذف", "Delete"))
                        }

                        if (LocalV11NeoEnabled.current && item.type == DayItemTypeV11.NOTE) {
                            IconButton(onClick = { editingNoteId = item.id }) {
                                Icon(Icons.Default.Edit, neoLabel("تعديل الملاحظة", "Edit note"))
                            }
                            TextButton(onClick = { noteNavigator.open("ATTACHMENTS", item.id) }) {
                                Text(neoLabel("مرفق", "Attach"))
                            }
                        }
                        if (item.type != DayItemTypeV11.NOTE) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = {
                                    val updated = V11DayOrganizerController.toggle(db, item.id)

                                    val changed = updated.dayItems.firstOrNull { it.id == item.id }

                                    if (changed?.completed == true) {
                                        ToDoShiftReminder.cancel(context, item.id)
                                    } else if (changed != null) {
                                        ToDoShiftReminder.schedule(context, changed)
                                    }

                                    onDatabaseChange(updated)
                                },
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Text(item.title, fontWeight = FontWeight.Bold)

                            if (item.details.isNotBlank()) {
                                Text(item.details, style = MaterialTheme.typography.bodySmall)
                            }

                            Text(
                                itemTypeLabel(item.type),
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (LocalV11NeoEnabled.current)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else Color.Gray,
                            )

                            item.reminderAt?.let { millis ->
                                val reminderTime =
                                    java.time.Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())

                                Text(
                                    neoLiteral(
                                        "تنبيه ${"%02d:%02d".format(
                                        reminderTime.hour,
                                        reminderTime.minute,
                                    )}",
                                        "Reminder ${"%02d:%02d".format(
                                        reminderTime.hour,
                                        reminderTime.minute,
                                    )}",
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    pendingEditSegmentId?.let { segmentId ->
        val segment = db.days.flatMap { it.segments }.firstOrNull { it.id == segmentId }
        if (segment == null) {
            LaunchedEffect(segmentId) { pendingEditSegmentId = null }
        } else {
            val range = segment.timeRange()
            var startText by
                rememberSaveable(segmentId) {
                    mutableStateOf(
                        range?.startMinute?.let { "%02d:%02d".format(it / 60, it % 60) } ?: ""
                    )
                }
            var endText by
                rememberSaveable(segmentId) {
                    mutableStateOf(
                        range?.endMinute?.let {
                            if (it == 1440) "24:00" else "%02d:%02d".format((it / 60) % 24, it % 60)
                        } ?: ""
                    )
                }
            var editError by remember(segmentId) { mutableStateOf<String?>(null) }
            fun parseTime(value: String, end: Boolean): Int? {
                val parts = value.trim().split(":")
                if (parts.size != 2) return null
                val h = parts[0].toLocalizedIntOrNull() ?: return null
                val m = parts[1].toLocalizedIntOrNull() ?: return null
                if (m !in 0..59 || h !in 0..24 || (h == 24 && (m != 0 || !end))) return null
                return h * 60 + m
            }
            val leaveTime =
                rememberNeoEditorExit(listOf(startText, endText), { pendingEditSegmentId = null })
            AlertDialog(
                onDismissRequest = leaveTime,
                title = { Text(neoLiteral("تعديل أوقات الشيفت", "Edit shift times")) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            neoLiteral(
                                "الشيفت العابر لمنتصف الليل يظل في يوم بدايته فقط، بحد أقصى 24 ساعة.",
                                "An overnight shift stays on its start date, up to 24 hours.",
                            )
                        )
                        OutlinedTextField(
                            value = startText,
                            onValueChange = { startText = it },
                            label = { Text(neoLiteral("البداية HH:mm", "Start HH:mm")) },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = endText,
                            onValueChange = { endText = it },
                            label = { Text(neoLiteral("النهاية HH:mm", "End HH:mm")) },
                            singleLine = true,
                        )
                        editError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val start = parseTime(startText, false)
                            val end = parseTime(endText, true)
                            if (start == null || end == null) {
                                editError =
                                    neoLiteral(
                                        "أدخل الوقت بصيغة HH:mm",
                                        "Enter time in HH:mm format",
                                    )
                            } else {
                                try {
                                    val normalizedEnd = if (end <= start) end + 1440 else end
                                    val updated =
                                        V11DayController.editSegmentTime(
                                            db,
                                            segmentId,
                                            start,
                                            normalizedEnd,
                                        )
                                    onDatabaseChange(updated)
                                    pendingEditSegmentId = null
                                } catch (e: IllegalArgumentException) {
                                    editError =
                                        e.message
                                            ?: neoLiteral("تعذر تعديل الوقت", "Cannot change time")
                                }
                            }
                        }
                    ) {
                        Text(neoLiteral("حفظ", "Save"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = leaveTime) { Text(neoLiteral("إلغاء", "Cancel")) }
                },
            )
        }
    }

    pendingDeleteSegmentId?.let { segmentId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSegmentId = null },
            title = { Text(neoLiteral("حذف الشيفت", "Delete shift")) },
            text = { Text(neoLiteral("هل تريد حذف هذا الشيفت؟", "Delete this shift?")) },
            confirmButton = {
                Button(
                    onClick = {
                        onDatabaseChange(V11DayController.deleteSegment(db, segmentId))
                        pendingDeleteSegmentId = null
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                ) {
                    Text(neoLiteral("حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSegmentId = null }) {
                    Text(neoLiteral("إلغاء", "Cancel"))
                }
            },
        )
    }

    if (LocalV11NeoEnabled.current && (editingNoteId != null || addType == DayItemTypeV11.NOTE)) {
        val existing = db.dayItems.firstOrNull { it.id == editingNoteId }
        NeoDayNoteDialog(
            date,
            existing,
            {
                editingNoteId = null
                addType = null
            },
        ) { note ->
            val savedNote = if (existing == null) note.copy(calendarId = activeCalendarId) else note
            onDatabaseChange(
                db.copy(dayItems = db.dayItems.filterNot { it.id == savedNote.id } + savedNote)
            )
            editingNoteId = null
            addType = null
        }
    }
    addType
        ?.takeIf { !LocalV11NeoEnabled.current || it != DayItemTypeV11.NOTE }
        ?.let { type ->
            V11AddDayItemDialog(
                type = type,
                dateForReminder = date.toString(),
                onDismiss = { addType = null },
                onSave = { title, details, reminderAt ->
                    val calendarId =
                        db.calendars.firstOrNull { it.id == activeCalendarId }?.id
                            ?: db.calendars.sortedBy { it.order }.firstOrNull()?.id

                    val updated =
                        V11DayOrganizerController.add(
                            db = db,
                            date = date.toString(),
                            calendarId = calendarId,
                            type = type,
                            title = title,
                            details = details,
                            reminderAt = reminderAt,
                        )

                    val created = updated.dayItems.lastOrNull()

                    if (created != null && created.reminderAt != null) {
                        ensureNotificationPermission()

                        // Scheduling follows a confirmed commit in V11Home.
                    }

                    onDatabaseChange(updated)

                    addType = null
                },
            )
        }
}

@Composable
private fun V11EmptyCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color =
            if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.surfaceVariant
            else Color(0xFFF2F4F7),
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            color =
                if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                else Color.Gray,
        )
    }
}

@Composable
private fun V11AddItemButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(text)
    }
}

@Composable
private fun V11AddDayItemDialog(
    type: DayItemTypeV11,
    dateForReminder: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Long?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }

    var details by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current

    var reminderHour by rememberSaveable { mutableIntStateOf(9) }

    var reminderMinute by rememberSaveable { mutableIntStateOf(0) }

    var reminderEnabled by rememberSaveable { mutableStateOf(type == DayItemTypeV11.REMINDER) }

    val leave =
        rememberNeoEditorExit(
            listOf(title, details, reminderHour, reminderMinute, reminderEnabled),
            onDismiss,
        )
    AlertDialog(
        onDismissRequest = leave,
        title = { Text(neoLiteral("إضافة ${itemTypeLabel(type)}", "Add ${itemTypeLabel(type)}")) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(neoLiteral("العنوان", "Title")) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(neoLiteral("التفاصيل", "Details")) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (type != DayItemTypeV11.NOTE) {
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(neoLiteral("تنبيه", "Alert"))

                        Spacer(Modifier.weight(1f))

                        if (reminderEnabled) {
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                reminderHour = hour

                                                reminderMinute = minute
                                            },
                                            reminderHour,
                                            reminderMinute,
                                            true,
                                        )
                                        .show()
                                }
                            ) {
                                Text("%02d:%02d".format(reminderHour, reminderMinute))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reminderAt =
                        if (reminderEnabled && type != DayItemTypeV11.NOTE) {
                            LocalDateTime.of(
                                    LocalDate.parse(dateForReminder),
                                    LocalTime.of(reminderHour, reminderMinute),
                                )
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                        } else {
                            null
                        }

                    onSave(title, details, reminderAt)
                },
                enabled = title.isNotBlank(),
            ) {
                Text(neoLiteral("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = leave) { Text(neoLiteral("إلغاء", "Cancel")) } },
    )
}

private fun itemTypeLabel(type: DayItemTypeV11): String =
    when (type) {
        DayItemTypeV11.TASK -> neoLiteral("مهمة", "Task")
        DayItemTypeV11.ERRAND -> neoLiteral("مشوار", "Errand")
        DayItemTypeV11.REMINDER -> neoLiteral("تذكير", "Reminder")
        DayItemTypeV11.NOTE -> neoLiteral("ملاحظة", "Note")
    }

private fun timeText(minute: Int): String {
    if (minute == 1440) return "24:00"

    val h = minute / 60
    val m = minute % 60

    return "%02d:%02d".format(h, m)
}
