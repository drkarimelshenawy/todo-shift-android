package com.todoshift.app.v11.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

private enum class TaskFilterV11 {
    TODAY,
    UPCOMING,
    OVERDUE,
    COMPLETED,
    ALL,
}

@Composable
fun V11TasksScreen(
    db: V11Database,
    workplaces: List<Workplace>,
    onDatabaseChange: (V11Database) -> Unit,
    initialAction: String? = null,
    initialTaskId: String? = null,
    financeAuthorized: Boolean = false,
    moneyVisible: Boolean = false,
    onQuickAction: (NeoQuickAction) -> Unit = {},
    onInitialActionHandled: () -> Unit = {},
) {
    val commit = rememberNeoCommit(onDatabaseChange)
    val context = LocalContext.current

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            // Scheduling remains valid.
            // Android will display notifications
            // once permission is granted.
        }

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

    var filter by remember { mutableStateOf(TaskFilterV11.TODAY) }

    var editingTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingTask = db.tasks.firstOrNull { it.id == editingTaskId }

    var pendingDelete by remember { mutableStateOf<TaskV11?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialAction, initialTaskId) {
        when (initialAction) {
            "add" -> {
                editingTaskId = null
                showEditor = true
            }
            "open" -> {
                val requestedTask = db.tasks.firstOrNull { it.id == initialTaskId }
                if (requestedTask != null) {
                    editingTaskId = requestedTask?.id
                    showEditor = true
                }
            }
        }
        if (initialAction != null) onInitialActionHandled()
    }

    val neo = LocalV11NeoEnabled.current
    var searchQuery by remember { mutableStateOf("") }
    var priorityFilter by remember { mutableStateOf<TaskPriorityV11?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    val today = neoNow().toLocalDate().toString()
    val visibleIds =
        remember(db.calendars) { db.calendars.filter { it.visible }.map { it.id }.toSet() }
    val visibleTasks =
        remember(db.tasks, visibleIds, neo) {
            db.tasks.filter { !neo || it.calendarId == null || it.calendarId in visibleIds }
        }

    val filteredTasks =
        remember(visibleTasks, filter, today, searchQuery, priorityFilter, categoryFilter, neo) {
            visibleTasks
                .filter { task ->
                    (!neo ||
                        ((searchQuery.isBlank() ||
                            task.title.contains(searchQuery, ignoreCase = true) ||
                            task.details.contains(searchQuery, ignoreCase = true)) &&
                            (priorityFilter == null || task.priority == priorityFilter) &&
                            (categoryFilter == null || task.calendarId == categoryFilter))) &&
                        when (filter) {
                            TaskFilterV11.TODAY ->
                                task.date == today && task.status != TaskStatusV11.COMPLETED

                            TaskFilterV11.UPCOMING ->
                                task.date > today && task.status != TaskStatusV11.COMPLETED

                            TaskFilterV11.OVERDUE ->
                                task.date < today && task.status != TaskStatusV11.COMPLETED

                            TaskFilterV11.COMPLETED -> task.status == TaskStatusV11.COMPLETED

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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = if (neo) neoSpacing() else 0.dp,
                    end = if (neo) neoSpacing() else 0.dp,
                    bottom = 96.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (neo) neoSpacing() else 8.dp),
        ) {
            item(key = "task-controls") {
                Column {
                    if (neo) {
                        NeoDashboard(
                            db,
                            workplaces,
                            onQuickAction = { action ->
                                if (action == NeoQuickAction.ADD_TASK) {
                                    editingTaskId = null
                                    showEditor = true
                                } else onQuickAction(action)
                            },
                            financeAuthorized = financeAuthorized,
                            moneyVisible = moneyVisible,
                        )
                        NeoTaskSearch(
                            searchQuery,
                            { searchQuery = it },
                            priorityFilter,
                            { priorityFilter = it },
                            categoryFilter,
                            { categoryFilter = it },
                            db,
                        )
                        val complete = visibleTasks.count { it.status == TaskStatusV11.COMPLETED }
                        if (
                            LocalNeoAppearance.current.showTaskSummary && visibleTasks.isNotEmpty()
                        ) {
                            LinearProgressIndicator(
                                progress = { complete.toFloat() / visibleTasks.size },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    if (!neo) TaskSummaryV11(tasks = visibleTasks, today = today)

                    TaskFiltersV11(selected = filter, onSelected = { filter = it })
                }
            }
            if (filteredTasks.isEmpty()) {
                item(key = "empty-tasks") {
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (neo) {
                            NeoEmptyState(
                                neoLabel("لا توجد مهام مطابقة", "No matching tasks"),
                                neoLabel(
                                    "غيّر الفلاتر أو استخدم زر + لإضافة مهمة",
                                    "Change filters or use + to add a task",
                                ),
                            )
                        } else {
                            Text(
                                text =
                                    when (filter) {
                                        TaskFilterV11.TODAY ->
                                            neoLiteral("لا توجد مهام لليوم", "No tasks for today")
                                        TaskFilterV11.UPCOMING ->
                                            neoLiteral("لا توجد مهام قادمة", "No upcoming tasks")
                                        TaskFilterV11.OVERDUE ->
                                            neoLiteral("لا توجد مهام متأخرة", "No overdue tasks")
                                        TaskFilterV11.COMPLETED ->
                                            neoLiteral("لا توجد مهام مكتملة", "No completed tasks")
                                        TaskFilterV11.ALL ->
                                            neoLiteral("لا توجد مهام بعد", "No tasks yet")
                                    },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(items = filteredTasks, key = { it.id }) { task ->
                    TaskCardV11(
                        task = task,
                        onToggleComplete = {
                            val now = System.currentTimeMillis()

                            val completed = task.status != TaskStatusV11.COMPLETED

                            val updatedTask =
                                task.copy(
                                    status =
                                        if (completed) TaskStatusV11.COMPLETED
                                        else TaskStatusV11.TODO,
                                    completedAt = if (completed) now else null,
                                    updatedAt = now,
                                )

                            onDatabaseChange(
                                db.copy(
                                    tasks =
                                        db.tasks.map { if (it.id == task.id) updatedTask else it }
                                )
                            )
                        },
                        onEdit = {
                            editingTaskId = task.id
                            showEditor = true
                        },
                        onDelete = { pendingDelete = task },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingTaskId = null
                showEditor = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = neoLiteral("إضافة مهمة", "Add task"))
        }
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(neoLabel("حذف المهمة؟", "Delete task?")) },
            text = { Text(task.title) },
            confirmButton = {
                TextButton(
                    onClick = {
                        commit(db.copy(tasks = db.tasks.filterNot { it.id == task.id })) {
                            pendingDelete = null
                        }
                    }
                ) {
                    Text(neoLabel("حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(neoLabel("إلغاء", "Cancel")) }
            },
        )
    }

    if (showEditor) {
        TaskEditorDialogV11(
            existing = editingTask,
            db = db,
            workplaces = workplaces,
            onDismiss = {
                showEditor = false
                editingTaskId = null
            },
            onSave = { task ->
                val exists = db.tasks.any { it.id == task.id }

                val updated =
                    if (exists) {
                        db.tasks.map { if (it.id == task.id) task else it }
                    } else {
                        db.tasks + task
                    }

                commit(db.copy(tasks = updated)) {
                    if (task.reminderAt != null) ensureNotificationPermission()
                    showEditor = false
                    editingTaskId = null
                }
            },
        )
    }
}

@Composable
private fun TaskSummaryV11(tasks: List<TaskV11>, today: String) {
    val dueToday = tasks.count { it.date == today && it.status != TaskStatusV11.COMPLETED }

    val overdue = tasks.count { it.date < today && it.status != TaskStatusV11.COMPLETED }

    val completed = tasks.count { it.status == TaskStatusV11.COMPLETED }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TaskStatCardV11(
            modifier = Modifier.weight(1f),
            value = dueToday,
            label = neoLiteral("اليوم", "Today"),
        )

        TaskStatCardV11(
            modifier = Modifier.weight(1f),
            value = overdue,
            label = neoLiteral("متأخرة", "Overdue"),
        )

        TaskStatCardV11(
            modifier = Modifier.weight(1f),
            value = completed,
            label = neoLiteral("مكتملة", "Completed"),
        )
    }
}

@Composable
private fun TaskStatCardV11(modifier: Modifier, value: Int, label: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskFiltersV11(selected: TaskFilterV11, onSelected: (TaskFilterV11) -> Unit) {
    if (LocalV11NeoEnabled.current) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            NeoChoice(
                neoLabel("عرض المهام", "Show tasks"),
                TaskFilterV11.entries,
                selected,
                {
                    when (it) {
                        TaskFilterV11.TODAY -> neoLabel("اليوم", "Today")
                        TaskFilterV11.UPCOMING -> neoLabel("القادمة", "Upcoming")
                        TaskFilterV11.OVERDUE -> neoLabel("المتأخرة", "Overdue")
                        TaskFilterV11.COMPLETED -> neoLabel("المكتملة", "Completed")
                        TaskFilterV11.ALL -> neoLabel("الكل", "All")
                    }
                },
                onSelected,
            )
        }
    } else
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TaskFilterV11.entries.forEach { filter ->
                FilterChip(
                    selected = selected == filter,
                    onClick = { onSelected(filter) },
                    label = {
                        Text(
                            when (filter) {
                                TaskFilterV11.TODAY -> neoLiteral("اليوم", "Today")
                                TaskFilterV11.UPCOMING -> neoLiteral("القادمة", "Upcoming")
                                TaskFilterV11.OVERDUE -> neoLiteral("المتأخرة", "Overdue")
                                TaskFilterV11.COMPLETED -> neoLiteral("المكتملة", "Completed")
                                TaskFilterV11.ALL -> neoLiteral("الكل", "All")
                            }
                        )
                    },
                )
            }
        }
}

@Composable
private fun TaskCardV11(
    task: TaskV11,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val navigator = LocalNeoFeatures.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(if (LocalV11NeoEnabled.current) neoSpacing() else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (LocalV11NeoEnabled.current) {
                Checkbox(
                    checked = task.status == TaskStatusV11.COMPLETED,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                )
            } else {
                Icon(
                    imageVector =
                        if (task.status == TaskStatusV11.COMPLETED) Icons.Default.CheckCircle
                        else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = neoLiteral("تغيير حالة المهمة", "Toggle task completion"),
                    modifier =
                        Modifier.size(if (LocalV11NeoEnabled.current) 48.dp else 28.dp).clickable {
                            onToggleComplete()
                        },
                    tint =
                        if (task.status == TaskStatusV11.COMPLETED)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (
                    LocalV11NeoEnabled.current &&
                        task.priority in listOf(TaskPriorityV11.HIGH, TaskPriorityV11.URGENT)
                ) {
                    Text(
                        neoPriorityLabel(task.priority),
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (
                                task.priority in
                                    listOf(TaskPriorityV11.HIGH, TaskPriorityV11.URGENT)
                            )
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )

                if (task.details.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
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
                                TaskPriorityV11.LOW -> neoLiteral("منخفضة", "Low")
                                TaskPriorityV11.NORMAL -> neoLiteral("عادية", "Normal")
                                TaskPriorityV11.HIGH -> neoLiteral("عالية", "High")
                                TaskPriorityV11.URGENT -> neoLiteral("عاجلة", "Urgent")
                            }
                        )

                        if (task.repeat != TaskRepeatV11.NONE) {
                            append(neoLiteral(" • متكررة", " • recurring"))
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (LocalV11NeoEnabled.current) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { menu = true }) { Text("⋮") }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(neoLabel("تعديل", "Edit")) },
                            onClick = {
                                menu = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(neoLabel("المرفقات", "Attachments")) },
                            onClick = {
                                menu = false
                                navigator.open("ATTACHMENTS", task.id)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(neoLabel("حذف", "Delete")) },
                            onClick = {
                                menu = false
                                onDelete()
                            },
                        )
                    }
                }
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = neoLiteral("تعديل", "Edit"))
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = neoLiteral("حذف", "Delete"))
                }
            }
        }
    }
}

@Composable
fun FullTaskEditorDialogV11(
    existing: TaskV11?,
    db: V11Database,
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    onSave: (TaskV11) -> Unit,
) {
    val write = LocalNeoEditorWriteState.current
    val editorContext = LocalContext.current
    var title by rememberSaveable(existing?.id) { mutableStateOf(existing?.title ?: "") }

    var details by rememberSaveable(existing?.id) { mutableStateOf(existing?.details ?: "") }

    var date by
        rememberSaveable(existing?.id) {
            mutableStateOf(existing?.date ?: LocalDate.now().toString())
        }

    var time by rememberSaveable(existing?.id) { mutableStateOf(existing?.time ?: "") }

    var priority by
        rememberSaveable(existing?.id) {
            mutableStateOf(existing?.priority ?: TaskPriorityV11.NORMAL)
        }

    var repeat by
        rememberSaveable(existing?.id) { mutableStateOf(existing?.repeat ?: TaskRepeatV11.NONE) }

    var workplaceId by rememberSaveable(existing?.id) { mutableStateOf(existing?.workplaceId) }

    var calendarId by rememberSaveable(existing?.id) { mutableStateOf(existing?.calendarId) }

    var linkedShiftId by rememberSaveable(existing?.id) { mutableStateOf(existing?.linkedShiftId) }

    var reminderEnabled by
        rememberSaveable(existing?.id) { mutableStateOf(existing?.reminderAt != null) }

    var reminderTime by
        rememberSaveable(existing?.id) {
            mutableStateOf(
                existing?.reminderAt?.let { millis ->
                    runCatching {
                            java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .withSecond(0)
                                .withNano(0)
                                .toString()
                        }
                        .getOrNull()
                } ?: existing?.time ?: ""
            )
        }

    var workplaceMenu by rememberSaveable { mutableStateOf(false) }

    var calendarMenu by rememberSaveable { mutableStateOf(false) }

    var shiftMenu by rememberSaveable { mutableStateOf(false) }

    val initialValues =
        remember(existing?.id) {
            listOf(
                title,
                details,
                date,
                time,
                priority,
                repeat,
                workplaceId,
                calendarId,
                linkedShiftId,
                reminderEnabled,
                reminderTime,
            )
        }
    var discardChanges by rememberSaveable { mutableStateOf(false) }
    fun leaveEditor() {
        if (write.pending) return
        if (
            listOf(
                title,
                details,
                date,
                time,
                priority,
                repeat,
                workplaceId,
                calendarId,
                linkedShiftId,
                reminderEnabled,
                reminderTime,
            ) != initialValues
        )
            discardChanges = true
        else onDismiss()
    }
    if (discardChanges)
        AlertDialog(
            onDismissRequest = { discardChanges = false },
            title = { Text(neoLabel("تجاهل التعديلات؟", "Discard changes?")) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(neoLabel("تجاهل", "Discard")) }
            },
            dismissButton = {
                TextButton(onClick = { discardChanges = false }) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    val dayShifts =
        remember(db.days, date) {
            db.days
                .filter { it.date == date }
                .flatMap { day ->
                    day.segments.map { segment -> Triple(segment.id, day.calendarId, segment) }
                }
        }

    LaunchedEffect(date) {
        if (linkedShiftId != null && dayShifts.none { it.first == linkedShiftId }) {
            linkedShiftId = null
        }
    }

    AlertDialog(
        onDismissRequest = { leaveEditor() },
        title = {
            Text(
                if (existing == null) neoLiteral("مهمة جديدة", "New task")
                else neoLiteral("تعديل المهمة", "Edit task")
            )
        },
        text = {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                write.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (write.pending) Text(neoLabel("جارٍ الحفظ", "Saving"))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(neoLiteral("عنوان المهمة", "Task title")) },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(neoLiteral("ملاحظات", "Notes")) },
                    minLines = 2,
                    maxLines = 4,
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(neoLiteral("التاريخ YYYY-MM-DD", "Date YYYY-MM-DD")) },
                    singleLine = true,
                )

                TextButton(
                    onClick = {
                        val selected =
                            runCatching { java.time.LocalTime.parse(time) }
                                .getOrDefault(java.time.LocalTime.of(9, 0))
                        android.app
                            .TimePickerDialog(
                                editorContext,
                                { _, h, m -> time = java.time.LocalTime.of(h, m).toString() },
                                selected.hour,
                                selected.minute,
                                true,
                            )
                            .show()
                    }
                ) {
                    Text(neoLiteral("اختيار الوقت", "Choose time"))
                }
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(neoLiteral("الوقت HH:MM - اختياري", "Time HH:MM - optional")) },
                    singleLine = true,
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { workplaceMenu = true },
                    ) {
                        Text(
                            workplaces.firstOrNull { it.id == workplaceId }?.name
                                ?: neoLiteral("مكان العمل: بدون ربط", "Workplace: none")
                        )
                    }

                    DropdownMenu(
                        expanded = workplaceMenu,
                        onDismissRequest = { workplaceMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(neoLiteral("بدون ربط", "Not linked")) },
                            onClick = {
                                workplaceId = null
                                workplaceMenu = false
                            },
                        )

                        workplaces.forEach { workplace ->
                            DropdownMenuItem(
                                text = { Text(workplace.name) },
                                onClick = {
                                    workplaceId = workplace.id
                                    workplaceMenu = false
                                },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { calendarMenu = true },
                    ) {
                        Text(
                            db.calendars.firstOrNull { it.id == calendarId }?.name
                                ?: neoLiteral("التقويم: بدون ربط", "Calendar: none")
                        )
                    }

                    DropdownMenu(
                        expanded = calendarMenu,
                        onDismissRequest = { calendarMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(neoLiteral("بدون ربط", "Not linked")) },
                            onClick = {
                                calendarId = null
                                calendarMenu = false
                            },
                        )

                        db.calendars.forEach { calendar ->
                            DropdownMenuItem(
                                text = { Text(calendar.name) },
                                onClick = {
                                    calendarId = calendar.id
                                    calendarMenu = false
                                },
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = dayShifts.isNotEmpty(),
                        onClick = { shiftMenu = true },
                    ) {
                        val selected = dayShifts.firstOrNull { it.first == linkedShiftId }

                        Text(
                            if (selected == null)
                                if (dayShifts.isEmpty())
                                    neoLiteral(
                                        "لا توجد شفتات في هذا اليوم",
                                        "No shifts on this day",
                                    )
                                else neoLiteral("الشفت: بدون ربط", "Shift: none")
                            else {
                                val segment = selected.third

                                val workplaceName =
                                    workplaces.firstOrNull { it.id == segment.workplaceId }?.name

                                buildString {
                                    append(
                                        when (segment.kind) {
                                            ShiftKind.FULL_DAY -> neoLiteral("يوم كامل", "Full day")
                                            ShiftKind.LONG -> neoLiteral("طويل", "Long")
                                            ShiftKind.NIGHT -> neoLiteral("ليلي", "Night")
                                            ShiftKind.MORNING -> neoLiteral("صباحي", "Morning")
                                            ShiftKind.EVENING -> neoLiteral("مسائي", "Evening")
                                            ShiftKind.OFF -> neoLiteral("إجازة", "Off")
                                        }
                                    )

                                    if (workplaceName != null) {
                                        append(" • ")
                                        append(workplaceName)
                                    }
                                }
                            }
                        )
                    }

                    DropdownMenu(expanded = shiftMenu, onDismissRequest = { shiftMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(neoLiteral("بدون ربط", "Not linked")) },
                            onClick = {
                                linkedShiftId = null
                                shiftMenu = false
                            },
                        )

                        dayShifts.forEach { item ->
                            val segment = item.third

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        buildString {
                                            append(
                                                when (segment.kind) {
                                                    ShiftKind.FULL_DAY ->
                                                        neoLiteral("يوم كامل", "Full day")
                                                    ShiftKind.LONG -> neoLiteral("طويل", "Long")
                                                    ShiftKind.NIGHT -> neoLiteral("ليلي", "Night")
                                                    ShiftKind.MORNING ->
                                                        neoLiteral("صباحي", "Morning")
                                                    ShiftKind.EVENING ->
                                                        neoLiteral("مسائي", "Evening")
                                                    ShiftKind.OFF -> neoLiteral("إجازة", "Off")
                                                }
                                            )

                                            workplaces
                                                .firstOrNull { it.id == segment.workplaceId }
                                                ?.name
                                                ?.let {
                                                    append(" • ")
                                                    append(it)
                                                }
                                        }
                                    )
                                },
                                onClick = {
                                    linkedShiftId = item.first
                                    calendarId = item.second

                                    if (segment.workplaceId != null) {
                                        workplaceId = segment.workplaceId
                                    }

                                    shiftMenu = false
                                },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = {
                            reminderEnabled = it

                            if (it && reminderTime.isBlank()) {
                                reminderTime =
                                    time.takeIf { value -> value.isNotBlank() } ?: "09:00"
                            }
                        },
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(neoLiteral("تذكير بالمهمة", "Task reminder"), fontWeight = FontWeight.Bold)
                }

                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderTime,
                        onValueChange = { reminderTime = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(neoLiteral("وقت التذكير HH:MM", "Reminder time HH:MM")) },
                        supportingText = {
                            Text(
                                neoLiteral(
                                    "سيتم التنبيه في تاريخ المهمة",
                                    "Reminder will trigger on task date",
                                )
                            )
                        },
                        singleLine = true,
                    )
                }

                Text(neoLiteral("الأولوية", "Priority"), fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    TaskPriorityV11.entries.forEach {
                        FilterChip(
                            selected = priority == it,
                            onClick = { priority = it },
                            label = {
                                Text(
                                    when (it) {
                                        TaskPriorityV11.LOW -> neoLiteral("منخفضة", "Low")
                                        TaskPriorityV11.NORMAL -> neoLiteral("عادية", "Normal")
                                        TaskPriorityV11.HIGH -> neoLiteral("عالية", "High")
                                        TaskPriorityV11.URGENT -> neoLiteral("عاجلة", "Urgent")
                                    }
                                )
                            },
                        )
                    }
                }

                Text(neoLiteral("التكرار", "Repeat"), fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    TaskRepeatV11.entries.forEach {
                        FilterChip(
                            selected = repeat == it,
                            onClick = { repeat = it },
                            label = {
                                Text(
                                    when (it) {
                                        TaskRepeatV11.NONE -> neoLiteral("بدون", "None")
                                        TaskRepeatV11.DAILY -> neoLiteral("يومي", "Daily")
                                        TaskRepeatV11.WEEKLY -> neoLiteral("أسبوعي", "Weekly")
                                        TaskRepeatV11.MONTHLY -> neoLiteral("شهري", "Monthly")
                                    }
                                )
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    !write.pending &&
                        title.isNotBlank() &&
                        runCatching { LocalDate.parse(date) }.isSuccess &&
                        (!reminderEnabled ||
                            runCatching {
                                    val parts = reminderTime.trim().split(":")

                                    require(parts.size == 2)

                                    val hour = parts[0].toInt()
                                    val minute = parts[1].toInt()

                                    require(hour in 0..23 && minute in 0..59)
                                }
                                .isSuccess),
                onClick = {
                    val now = System.currentTimeMillis()

                    val calculatedReminderAt =
                        if (reminderEnabled) {
                            runCatching {
                                    val reminderDate = LocalDate.parse(date)

                                    val parts = reminderTime.trim().split(":")

                                    require(parts.size == 2)

                                    val hour = parts[0].toInt()

                                    val minute = parts[1].toInt()

                                    require(hour in 0..23 && minute in 0..59)

                                    LocalDateTime.of(
                                            reminderDate,
                                            java.time.LocalTime.of(hour, minute),
                                        )
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                                }
                                .getOrNull()
                        } else {
                            null
                        }

                    onSave(
                        TaskV11(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            details = details.trim(),
                            date = date,
                            time = time.trim().takeIf { it.isNotBlank() },
                            priority = priority,
                            status = existing?.status ?: TaskStatusV11.TODO,
                            repeat = repeat,
                            reminderAt = calculatedReminderAt,
                            workplaceId = workplaceId,
                            calendarId = calendarId,
                            linkedShiftId = linkedShiftId,
                            createdAt = existing?.createdAt ?: now,
                            updatedAt = now,
                            completedAt = existing?.completedAt,
                        )
                    )
                },
            ) {
                Text(neoLiteral("حفظ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = { leaveEditor() }) { Text(neoLiteral("إلغاء", "Cancel")) }
        },
    )
}
