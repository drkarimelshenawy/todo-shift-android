package com.todoshift.app.v11.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import com.todoshift.app.data.Workplace
import com.todoshift.app.reminders.ToDoShiftReminder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

private enum class TaskFilterV11 {
    TODAY,
    UPCOMING,
    OVERDUE,
    COMPLETED,
    ALL
}

@Composable
fun V11TasksScreen(
    db: V11Database,
    workplaces: List<Workplace>,
    onDatabaseChange: (V11Database) -> Unit
) {
    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts
                    .RequestPermission()
        ) {
            // Scheduling remains valid.
            // Android will display notifications
            // once permission is granted.
        }

    fun ensureNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission
                    .POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission
                    .POST_NOTIFICATIONS
            )
        }
    }

    var filter by remember {
        mutableStateOf(TaskFilterV11.TODAY)
    }

    var editingTask by remember {
        mutableStateOf<TaskV11?>(null)
    }

    var showEditor by remember {
        mutableStateOf(false)
    }

    val today = LocalDate.now().toString()

    val filteredTasks = remember(db.tasks, filter, today) {
        db.tasks
            .filter { task ->
                when (filter) {
                    TaskFilterV11.TODAY ->
                        task.date == today &&
                            task.status != TaskStatusV11.COMPLETED

                    TaskFilterV11.UPCOMING ->
                        task.date > today &&
                            task.status != TaskStatusV11.COMPLETED

                    TaskFilterV11.OVERDUE ->
                        task.date < today &&
                            task.status != TaskStatusV11.COMPLETED

                    TaskFilterV11.COMPLETED ->
                        task.status == TaskStatusV11.COMPLETED

                    TaskFilterV11.ALL -> true
                }
            }
            .sortedWith(
                compareBy<TaskV11> { it.date }
                    .thenBy { it.time ?: "99:99" }
                    .thenByDescending {
                        when (it.priority) {
                            TaskPriorityV11.URGENT -> 4
                            TaskPriorityV11.HIGH -> 3
                            TaskPriorityV11.NORMAL -> 2
                            TaskPriorityV11.LOW -> 1
                        }
                    }
            )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TaskSummaryV11(
                tasks = db.tasks,
                today = today
            )

            TaskFiltersV11(
                selected = filter,
                onSelected = { filter = it }
            )

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (filter) {
                            TaskFilterV11.TODAY ->
                                "لا توجد مهام لليوم"
                            TaskFilterV11.UPCOMING ->
                                "لا توجد مهام قادمة"
                            TaskFilterV11.OVERDUE ->
                                "لا توجد مهام متأخرة"
                            TaskFilterV11.COMPLETED ->
                                "لا توجد مهام مكتملة"
                            TaskFilterV11.ALL ->
                                "لا توجد مهام بعد"
                        },
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 6.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCardV11(
                            task = task,
                            onToggleComplete = {
                                val now =
                                    System.currentTimeMillis()

                                val completed =
                                    task.status !=
                                        TaskStatusV11.COMPLETED

                                val updatedTask =
                                    task.copy(
                                        status =
                                            if (completed)
                                                TaskStatusV11.COMPLETED
                                            else
                                                TaskStatusV11.TODO,
                                        completedAt =
                                            if (completed)
                                                now
                                            else
                                                null,
                                        updatedAt = now
                                    )

                                if (completed) {
                                    ToDoShiftReminder.cancel(
                                        context,
                                        task.id
                                    )
                                } else {
                                    ToDoShiftReminder.schedule(
                                        context,
                                        updatedTask
                                    )
                                }

                                onDatabaseChange(
                                    db.copy(
                                        tasks = db.tasks.map {
                                            if (it.id == task.id)
                                                updatedTask
                                            else
                                                it
                                        }
                                    )
                                )
                            },
                            onEdit = {
                                editingTask = task
                                showEditor = true
                            },
                            onDelete = {
                                ToDoShiftReminder.cancel(
                                    context,
                                    task.id
                                )

                                onDatabaseChange(
                                    db.copy(
                                        tasks =
                                            db.tasks.filterNot {
                                                it.id == task.id
                                            }
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingTask = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "إضافة مهمة"
            )
        }
    }

    if (showEditor) {
        TaskEditorDialogV11(
            existing = editingTask,
            db = db,
            workplaces = workplaces,
            onDismiss = {
                showEditor = false
                editingTask = null
            },
            onSave = { task ->
                val exists =
                    db.tasks.any { it.id == task.id }

                val updated =
                    if (exists) {
                        db.tasks.map {
                            if (it.id == task.id) task else it
                        }
                    } else {
                        db.tasks + task
                    }

                ToDoShiftReminder.cancel(
                    context,
                    task.id
                )

                if (task.reminderAt != null) {
                    ensureNotificationPermission()
                }

                ToDoShiftReminder.schedule(
                    context,
                    task
                )

                onDatabaseChange(
                    db.copy(tasks = updated)
                )

                showEditor = false
                editingTask = null
            }
        )
    }
}

@Composable
private fun TaskSummaryV11(
    tasks: List<TaskV11>,
    today: String
) {
    val dueToday =
        tasks.count {
            it.date == today &&
                it.status != TaskStatusV11.COMPLETED
        }

    val overdue =
        tasks.count {
            it.date < today &&
                it.status != TaskStatusV11.COMPLETED
        }

    val completed =
        tasks.count {
            it.status == TaskStatusV11.COMPLETED
        }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskStatCardV11(
            modifier = Modifier.weight(1f),
            value = dueToday,
            label = "اليوم"
        )

        TaskStatCardV11(
            modifier = Modifier.weight(1f),
            value = overdue,
            label = "متأخرة"
        )

        TaskStatCardV11(
            modifier = Modifier.weight(1f),
            value = completed,
            label = "مكتملة"
        )
    }
}

@Composable
private fun TaskStatCardV11(
    modifier: Modifier,
    value: Int,
    label: String
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                label,
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskFiltersV11(
    selected: TaskFilterV11,
    onSelected: (TaskFilterV11) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TaskFilterV11.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = {
                    onSelected(filter)
                },
                label = {
                    Text(
                        when (filter) {
                            TaskFilterV11.TODAY -> "اليوم"
                            TaskFilterV11.UPCOMING -> "القادمة"
                            TaskFilterV11.OVERDUE -> "المتأخرة"
                            TaskFilterV11.COMPLETED -> "المكتملة"
                            TaskFilterV11.ALL -> "الكل"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun TaskCardV11(
    task: TaskV11,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector =
                    if (
                        task.status ==
                        TaskStatusV11.COMPLETED
                    )
                        Icons.Default.CheckCircle
                    else
                        Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "تغيير حالة المهمة",
                modifier = Modifier
                    .size(28.dp)
                    .clickable {
                        onToggleComplete()
                    },
                tint =
                    if (
                        task.status ==
                        TaskStatusV11.COMPLETED
                    )
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                if (task.details.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.details,
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    buildString {
                        append(task.date)

                        task.time?.let {
                            append(" • ")
                            append(it)
                        }

                        append(" • ")
                        append(
                            when (task.priority) {
                                TaskPriorityV11.LOW ->
                                    "منخفضة"
                                TaskPriorityV11.NORMAL ->
                                    "عادية"
                                TaskPriorityV11.HIGH ->
                                    "عالية"
                                TaskPriorityV11.URGENT ->
                                    "عاجلة"
                            }
                        )

                        if (
                            task.repeat !=
                            TaskRepeatV11.NONE
                        ) {
                            append(" • متكررة")
                        }
                    },
                    style =
                        MaterialTheme.typography.labelSmall,
                    color =
                        MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onEdit
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "تعديل"
                )
            }

            IconButton(
                onClick = onDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف"
                )
            }
        }
    }
}

@Composable
private fun TaskEditorDialogV11(
    existing: TaskV11?,
    db: V11Database,
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    onSave: (TaskV11) -> Unit
) {
    var title by remember(existing?.id) {
        mutableStateOf(existing?.title ?: "")
    }

    var details by remember(existing?.id) {
        mutableStateOf(existing?.details ?: "")
    }

    var date by remember(existing?.id) {
        mutableStateOf(
            existing?.date ?: LocalDate.now().toString()
        )
    }

    var time by remember(existing?.id) {
        mutableStateOf(existing?.time ?: "")
    }

    var priority by remember(existing?.id) {
        mutableStateOf(
            existing?.priority ?: TaskPriorityV11.NORMAL
        )
    }

    var repeat by remember(existing?.id) {
        mutableStateOf(
            existing?.repeat ?: TaskRepeatV11.NONE
        )
    }

    var workplaceId by remember(existing?.id) {
        mutableStateOf(existing?.workplaceId)
    }

    var calendarId by remember(existing?.id) {
        mutableStateOf(existing?.calendarId)
    }

    var linkedShiftId by remember(existing?.id) {
        mutableStateOf(existing?.linkedShiftId)
    }

    var reminderEnabled by remember(existing?.id) {
        mutableStateOf(
            existing?.reminderAt != null
        )
    }

    var reminderTime by remember(existing?.id) {
        mutableStateOf(
            existing?.reminderAt
                ?.let { millis ->
                    runCatching {
                        java.time.Instant
                            .ofEpochMilli(millis)
                            .atZone(
                                ZoneId.systemDefault()
                            )
                            .toLocalTime()
                            .withSecond(0)
                            .withNano(0)
                            .toString()
                    }.getOrNull()
                }
                ?: existing?.time
                ?: ""
        )
    }

    var workplaceMenu by remember {
        mutableStateOf(false)
    }

    var calendarMenu by remember {
        mutableStateOf(false)
    }

    var shiftMenu by remember {
        mutableStateOf(false)
    }

    val dayShifts =
        remember(db.days, date) {
            db.days
                .filter { it.date == date }
                .flatMap { day ->
                    day.segments.map { segment ->
                        Triple(
                            segment.id,
                            day.calendarId,
                            segment
                        )
                    }
                }
        }

    LaunchedEffect(date) {
        if (
            linkedShiftId != null &&
            dayShifts.none {
                it.first == linkedShiftId
            }
        ) {
            linkedShiftId = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null)
                    "مهمة جديدة"
                else
                    "تعديل المهمة"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان المهمة") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظات") },
                    minLines = 2,
                    maxLines = 4
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("التاريخ YYYY-MM-DD") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("الوقت HH:MM - اختياري") },
                    singleLine = true
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            workplaceMenu = true
                        }
                    ) {
                        Text(
                            workplaces
                                .firstOrNull {
                                    it.id == workplaceId
                                }
                                ?.name
                                ?: "مكان العمل: بدون ربط"
                        )
                    }

                    DropdownMenu(
                        expanded = workplaceMenu,
                        onDismissRequest = {
                            workplaceMenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("بدون ربط")
                            },
                            onClick = {
                                workplaceId = null
                                workplaceMenu = false
                            }
                        )

                        workplaces.forEach { workplace ->
                            DropdownMenuItem(
                                text = {
                                    Text(workplace.name)
                                },
                                onClick = {
                                    workplaceId =
                                        workplace.id
                                    workplaceMenu = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            calendarMenu = true
                        }
                    ) {
                        Text(
                            db.calendars
                                .firstOrNull {
                                    it.id == calendarId
                                }
                                ?.name
                                ?: "التقويم: بدون ربط"
                        )
                    }

                    DropdownMenu(
                        expanded = calendarMenu,
                        onDismissRequest = {
                            calendarMenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("بدون ربط")
                            },
                            onClick = {
                                calendarId = null
                                calendarMenu = false
                            }
                        )

                        db.calendars.forEach { calendar ->
                            DropdownMenuItem(
                                text = {
                                    Text(calendar.name)
                                },
                                onClick = {
                                    calendarId =
                                        calendar.id
                                    calendarMenu = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dayShifts.isNotEmpty(),
                        onClick = {
                            shiftMenu = true
                        }
                    ) {
                        val selected =
                            dayShifts.firstOrNull {
                                it.first == linkedShiftId
                            }

                        Text(
                            if (selected == null)
                                if (dayShifts.isEmpty())
                                    "لا توجد شفتات في هذا اليوم"
                                else
                                    "الشفت: بدون ربط"
                            else {
                                val segment =
                                    selected.third

                                val workplaceName =
                                    workplaces
                                        .firstOrNull {
                                            it.id ==
                                                segment.workplaceId
                                        }
                                        ?.name

                                buildString {
                                    append(
                                        when (
                                            segment.kind
                                        ) {
                                            ShiftKind.FULL_DAY ->
                                                "يوم كامل"
                                            ShiftKind.LONG ->
                                                "طويل"
                                            ShiftKind.NIGHT ->
                                                "ليلي"
                                            ShiftKind.MORNING ->
                                                "صباحي"
                                            ShiftKind.EVENING ->
                                                "مسائي"
                                            ShiftKind.OFF ->
                                                "إجازة"
                                        }
                                    )

                                    if (
                                        workplaceName != null
                                    ) {
                                        append(" • ")
                                        append(workplaceName)
                                    }
                                }
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = shiftMenu,
                        onDismissRequest = {
                            shiftMenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("بدون ربط")
                            },
                            onClick = {
                                linkedShiftId = null
                                shiftMenu = false
                            }
                        )

                        dayShifts.forEach { item ->
                            val segment = item.third

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        buildString {
                                            append(
                                                when (
                                                    segment.kind
                                                ) {
                                                    ShiftKind.FULL_DAY ->
                                                        "يوم كامل"
                                                    ShiftKind.LONG ->
                                                        "طويل"
                                                    ShiftKind.NIGHT ->
                                                        "ليلي"
                                                    ShiftKind.MORNING ->
                                                        "صباحي"
                                                    ShiftKind.EVENING ->
                                                        "مسائي"
                                                    ShiftKind.OFF ->
                                                        "إجازة"
                                                }
                                            )

                                            workplaces
                                                .firstOrNull {
                                                    it.id ==
                                                        segment.workplaceId
                                                }
                                                ?.name
                                                ?.let {
                                                    append(" • ")
                                                    append(it)
                                                }
                                        }
                                    )
                                },
                                onClick = {
                                    linkedShiftId =
                                        item.first
                                    calendarId =
                                        item.second

                                    if (
                                        segment.workplaceId !=
                                        null
                                    ) {
                                        workplaceId =
                                            segment.workplaceId
                                    }

                                    shiftMenu = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it

                            if (
                                it &&
                                reminderTime.isBlank()
                            ) {
                                reminderTime =
                                    time.takeIf {
                                        value ->
                                        value.isNotBlank()
                                    } ?: "09:00"
                            }
                        }
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        "تذكير بالمهمة",
                        fontWeight = FontWeight.Bold
                    )
                }

                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = {
                            reminderTime = it
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                "وقت التذكير HH:MM"
                            )
                        },
                        supportingText = {
                            Text(
                                "سيتم التنبيه في تاريخ المهمة"
                            )
                        },
                        singleLine = true
                    )
                }

                Text(
                    "الأولوية",
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    TaskPriorityV11.entries.forEach {
                        FilterChip(
                            selected = priority == it,
                            onClick = {
                                priority = it
                            },
                            label = {
                                Text(
                                    when (it) {
                                        TaskPriorityV11.LOW ->
                                            "منخفضة"
                                        TaskPriorityV11.NORMAL ->
                                            "عادية"
                                        TaskPriorityV11.HIGH ->
                                            "عالية"
                                        TaskPriorityV11.URGENT ->
                                            "عاجلة"
                                    }
                                )
                            }
                        )
                    }
                }

                Text(
                    "التكرار",
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    TaskRepeatV11.entries.forEach {
                        FilterChip(
                            selected = repeat == it,
                            onClick = {
                                repeat = it
                            },
                            label = {
                                Text(
                                    when (it) {
                                        TaskRepeatV11.NONE ->
                                            "بدون"
                                        TaskRepeatV11.DAILY ->
                                            "يومي"
                                        TaskRepeatV11.WEEKLY ->
                                            "أسبوعي"
                                        TaskRepeatV11.MONTHLY ->
                                            "شهري"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    title.isNotBlank() &&
                    runCatching {
                        LocalDate.parse(date)
                    }.isSuccess &&
                    (
                        !reminderEnabled ||
                        runCatching {
                            val parts =
                                reminderTime
                                    .trim()
                                    .split(":")

                            require(parts.size == 2)

                            val hour =
                                parts[0].toInt()
                            val minute =
                                parts[1].toInt()

                            require(
                                hour in 0..23 &&
                                minute in 0..59
                            )
                        }.isSuccess
                    ),
                onClick = {
                    val now =
                        System.currentTimeMillis()

                    val calculatedReminderAt =
                        if (reminderEnabled) {
                            runCatching {
                                val reminderDate =
                                    LocalDate.parse(date)

                                val parts =
                                    reminderTime
                                        .trim()
                                        .split(":")

                                require(
                                    parts.size == 2
                                )

                                val hour =
                                    parts[0].toInt()

                                val minute =
                                    parts[1].toInt()

                                require(
                                    hour in 0..23 &&
                                    minute in 0..59
                                )

                                LocalDateTime.of(
                                    reminderDate,
                                    java.time.LocalTime.of(
                                        hour,
                                        minute
                                    )
                                )
                                    .atZone(
                                        ZoneId.systemDefault()
                                    )
                                    .toInstant()
                                    .toEpochMilli()
                            }.getOrNull()
                        } else {
                            null
                        }

                    onSave(
                        TaskV11(
                            id =
                                existing?.id
                                    ?: UUID.randomUUID()
                                        .toString(),
                            title = title.trim(),
                            details = details.trim(),
                            date = date,
                            time =
                                time.trim()
                                    .takeIf {
                                        it.isNotBlank()
                                    },
                            priority = priority,
                            status =
                                existing?.status
                                    ?: TaskStatusV11.TODO,
                            repeat = repeat,
                            reminderAt =
                                calculatedReminderAt,
                            workplaceId =
                                workplaceId,
                            calendarId =
                                calendarId,
                            linkedShiftId =
                                linkedShiftId,
                            createdAt =
                                existing?.createdAt ?: now,
                            updatedAt = now,
                            completedAt =
                                existing?.completedAt
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}
