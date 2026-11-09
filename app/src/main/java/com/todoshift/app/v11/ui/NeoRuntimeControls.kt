package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.v11.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val LocalNeoNavigateDate = staticCompositionLocalOf<(java.time.LocalDate) -> Unit> { {} }
val LocalNeoRuntimeReduced = staticCompositionLocalOf { false }
val LocalNeoFocus = staticCompositionLocalOf { false }
val LocalNeoFocusChange = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

@Composable
fun NeoFocusControls() {
    val focus = LocalNeoFocus.current
    val change = LocalNeoFocusChange.current
    NeoSwitch(neoLabel("تركيز مؤقت", "Temporary focus"), focus, change)
    Text(
        neoLabel(
            "يخفي اللوحات الاختيارية ويقلل المؤثرات دون تعديل تفضيلاتك أو تذكيراتك",
            "Hides optional panels and reduces effects without changing saved preferences or reminders",
        )
    )
}

@Composable
fun NeoNotificationControls(db: V11Database) {
    val context = LocalContext.current
    val store = remember { NeoSmartStore(context) }
    val scope = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val data = remember(revision) { store.snapshot() }
    fun save(key: String, value: Boolean) {
        try {
            store.update { it.put(key, value) }
            revision++
            scope.launch {
                withContext(Dispatchers.IO) {
                    runCatching {
                            db.tasks.forEach {
                                com.todoshift.app.reminders.ToDoShiftReminder.cancel(context, it.id)
                            }
                            db.dayItems.forEach {
                                com.todoshift.app.reminders.ToDoShiftReminder.cancel(context, it.id)
                            }
                            db.days
                                .flatMap { it.segments }
                                .forEach {
                                    com.todoshift.app.reminders.ToDoShiftReminder.cancel(
                                        context,
                                        "shift-${it.id}",
                                    )
                                }
                            com.todoshift.app.reminders.ToDoShiftReminder.rescheduleAll(context, db)
                        }
                        .onFailure { error = it.message }
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }
    NeoSwitch(neoLabel("تذكيرات المهام", "Task reminders"), data.optBoolean("notify_tasks", true)) {
        save("notify_tasks", it)
    }
    NeoSwitch(
        neoLabel("المواعيد والملاحظات", "Events and notes"),
        data.optBoolean("notify_events", true),
    ) {
        save("notify_events", it)
    }
    NeoSwitch(
        neoLabel("الشيفتات قبل ٣٠ دقيقة", "Shifts 30 minutes before"),
        data.optBoolean("notify_shifts", false),
    ) {
        save("notify_shifts", it)
    }
    NeoSwitch(
        neoLabel("هدوء ٢٢:٠٠–٠٧:٠٠؛ يؤجَّل التنبيه", "Quiet 22:00–07:00; defer notifications"),
        data.optBoolean("quietHours", false),
    ) {
        save("quietHours", it)
    }
    TextButton(
        onClick = {
            context.startActivity(
                android.content
                    .Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        }
    ) {
        Text(neoLabel("أذونات Android", "Android permissions"))
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
