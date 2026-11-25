package com.exmore.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.exmore.app.v11.*
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
                                com.exmore.app.reminders.ToDoShiftReminder.cancel(context, it.id)
                            }
                            db.dayItems.forEach {
                                com.exmore.app.reminders.ToDoShiftReminder.cancel(context, it.id)
                            }
                            db.days
                                .flatMap { it.segments }
                                .forEach {
                                    com.exmore.app.reminders.ToDoShiftReminder.cancel(
                                        context,
                                        "shift-${it.id}",
                                    )
                                }
                            com.exmore.app.reminders.ToDoShiftReminder.rescheduleAll(context, db)
                        }
                        .onFailure { error = it.message }
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }
    NeoSwitch(neoLabel("جميع الإشعارات", "All notifications"), data.optBoolean("notificationsEnabled", true)) {
        save("notificationsEnabled", it)
    }
    NeoSwitch(neoLabel("تذكيرات المهام", "Task reminders"), data.optBoolean("notify_tasks", true)) {
        save("notify_tasks", it)
    }
    NeoSwitch(
        neoLabel("المواعيد والأحداث", "Events"),
        data.optBoolean("notify_events", true),
    ) {
        save("notify_events", it)
    }
    NeoSwitch(neoLabel("تذكيرات الملاحظات","Note reminders"),data.optBoolean("notify_notes",data.optBoolean("notify_events",true))) {save("notify_notes",it)}
    NeoSwitch(
        neoLabel("الشيفتات قبل ٣٠ دقيقة", "Shifts 30 minutes before"),
        data.optBoolean("notify_shifts", false),
    ) {
        save("notify_shifts", it)
    }
    NeoSwitch(
        neoLabel("فترة الهدوء؛ تنبيه صامت في موعده", "Quiet hours; deliver silently on time"),
        data.optBoolean("quietHours", false),
    ) {
        save("quietHours", it)
    }
    var quietStart by remember {mutableStateOf(java.time.LocalTime.of(data.optInt("quietStartMinute",1320)/60,data.optInt("quietStartMinute",1320)%60).toString())}
    var quietEnd by remember {mutableStateOf(java.time.LocalTime.of(data.optInt("quietEndMinute",420)/60,data.optInt("quietEndMinute",420)%60).toString())}
    if(data.optBoolean("quietHours",false)) {
        OutlinedTextField(quietStart,{quietStart=it},label={Text(neoLabel("بداية الهدوء HH:mm","Quiet start HH:mm"))},singleLine=true)
        OutlinedTextField(quietEnd,{quietEnd=it},label={Text(neoLabel("نهاية الهدوء HH:mm","Quiet end HH:mm"))},singleLine=true)
        TextButton(onClick={try {
            val start=java.time.LocalTime.parse(quietStart);val end=java.time.LocalTime.parse(quietEnd)
            store.update {it.put("quietStartMinute",start.hour*60+start.minute);it.put("quietEndMinute",end.hour*60+end.minute)}
            revision++;error=null
        } catch(_:Exception) {error="وقت غير صحيح / Invalid time"}}) {Text(neoLabel("حفظ فترة الهدوء","Save quiet interval"))}
    }
    var lead by remember { mutableStateOf(data.optInt("shiftReminderMinutes",30).toString()) }
    OutlinedTextField(lead, { lead=it.filter(Char::isDigit).take(5) },
        label={ Text(neoLabel("تذكير الشيفت قبل (دقيقة)","Shift reminder lead (minutes)")) },singleLine=true)
    TextButton(onClick={
        val minutes=lead.toIntOrNull()
        if(minutes == null || minutes !in 0..10080) error=neoLiteral("أدخل ٠ إلى ١٠٠٨٠ دقيقة","Enter 0 to 10080 minutes")
        else try {
            store.update {it.put("shiftReminderMinutes",minutes)}
            revision++
            scope.launch { withContext(Dispatchers.IO) { com.exmore.app.reminders.ToDoShiftReminder.rescheduleAll(context,db) } }
        } catch(failure:Exception) {error=failure.message}
    }) {Text(neoLabel("حفظ وقت التذكير","Save lead time"))}
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
    if (android.os.Build.VERSION.SDK_INT >= 31) {
        val alarm =
            context.getSystemService(android.content.Context.ALARM_SERVICE)
                as android.app.AlarmManager
        Text(
            neoLabel("دقة التنبيهات: ", "Alarm precision: ") +
                if (alarm.canScheduleExactAlarms()) neoLabel("دقيقة", "Exact")
                else neoLabel("تقريبية حسب النظام", "Approximate, controlled by Android"),
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(
            onClick = {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:${context.packageName}"),
                    )
                )
            }
        ) {
            Text(neoLabel("إذن التنبيه الدقيق", "Exact alarm permission"))
        }
    }
    NeoReminderRecurrenceControls(db)
    // Launcher shortcuts belong to Widgets & Shortcuts, never Notification Center.
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
