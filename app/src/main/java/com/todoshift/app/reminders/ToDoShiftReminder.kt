package com.todoshift.app.reminders

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.todoshift.app.MainActivity
import com.todoshift.app.R
import com.todoshift.app.v11.DayItemV11
import com.todoshift.app.v11.TaskStatusV11
import com.todoshift.app.v11.TaskV11
import com.todoshift.app.v11.V11Database
import com.todoshift.app.v11.V11Runtime
import kotlinx.coroutines.launch

object ToDoShiftReminder {

    const val CHANNEL_ID = "todo_shift_reminders"

    const val QUIET_CHANNEL_ID = "todo_shift_reminders_quiet_v44"
    const val EXTRA_SOURCE_AT = "source_reminder_at"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"

    const val ACTION_REMINDER = "com.todoshift.app.REMINDER"

    const val EXTRA_ID = "reminder_id"

    const val EXTRA_TITLE = "reminder_title"

    const val EXTRA_DETAILS = "reminder_details"

    const val EXTRA_DATE = "reminder_date"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val channel =
            NotificationChannel(
                    CHANNEL_ID,
                    "تذكيرات ToDo Shift",
                    NotificationManager.IMPORTANCE_HIGH,
                )
                .apply {
                    description = "تنبيهات المهام والمشاوير والتذكيرات"
                    enableVibration(true)
                }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(NotificationChannel(QUIET_CHANNEL_ID,
            "تذكيرات صامتة / Silent reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            setSound(null,null)
            enableVibration(false)
            vibrationPattern = longArrayOf(0)
        })
    }

    fun schedule(context: Context, item: DayItemV11) {
        if (!item.noteVisible || item.noteTrashed) { cancel(context,item.id); return }
        if (
            !com.todoshift.app.v11.NeoNotificationPolicy.enabled(
                context,
                if (item.id.startsWith("shift-")) "shifts" else "events",
            )
        )
            return
        val whenMs =
            ReminderPolicy(context).next(item.id,item.reminderAt ?: return) ?: return

        if (item.completed) {
            cancel(context, item.id)
            return
        }

        if (whenMs <= System.currentTimeMillis()) {
            return
        }

        createChannel(context)

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent =
            Intent(context, ToDoShiftReminderReceiver::class.java).apply {
                action = ACTION_REMINDER

                putExtra(EXTRA_ID, item.id)

                putExtra(EXTRA_SCHEDULED_AT, whenMs)
                putExtra(EXTRA_SOURCE_AT,item.reminderAt)
                putExtra(EXTRA_TITLE, if(item.notePrivate || protected(context)) "ToDo Shift" else item.title)

                putExtra(EXTRA_DETAILS, if(item.notePrivate || protected(context)) "" else item.details)

                putExtra(EXTRA_DATE, item.date)
            }

        val pending =
            PendingIntent.getBroadcast(
                context,
                requestCode(item.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        }
    }

    fun schedule(context: Context, task: TaskV11) {
        if (!com.todoshift.app.v11.NeoNotificationPolicy.enabled(context, "tasks")) return
        val whenMs =
            ReminderPolicy(context).next(task.id,task.reminderAt ?: return,task.repeat.name) ?: return

        if (task.status == TaskStatusV11.COMPLETED) {
            cancel(context, task.id)
            return
        }

        if (whenMs <= System.currentTimeMillis()) {
            return
        }

        createChannel(context)

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent =
            Intent(context, ToDoShiftReminderReceiver::class.java).apply {
                action = ACTION_REMINDER

                putExtra(EXTRA_ID, task.id)

                putExtra(EXTRA_SCHEDULED_AT, whenMs)
                putExtra(EXTRA_SOURCE_AT,task.reminderAt)
                putExtra(EXTRA_TITLE, if(protected(context)) "ToDo Shift" else task.title)

                putExtra(EXTRA_DETAILS, if(protected(context)) "" else task.details)

                putExtra(EXTRA_DATE, task.date)
            }

        val pending =
            PendingIntent.getBroadcast(
                context,
                requestCode(task.id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        }
    }

    fun cancel(context: Context, id: String) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent =
            Intent(context, ToDoShiftReminderReceiver::class.java).apply {
                action = ACTION_REMINDER
            }

        val pending =
            PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )

        if (pending != null) {
            alarm.cancel(pending)
            pending.cancel()
        }

        NotificationManagerCompat.from(context).cancel(requestCode(id))
    }

    fun rescheduleAll(
        context: Context,
        db: V11Database,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        createChannel(context)
        if (
            com.todoshift.app.v11
                .NeoSmartStore(context)
                .snapshot()
                .optBoolean("notify_shifts", false)
        ) {
            com.todoshift.app.v11.neoShiftOccurrences(db).forEach { shift ->
                val whenMs =
                    shift.start
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli() - com.todoshift.app.v11.NeoNotificationPolicy.shiftLeadMinutes(context) * 60_000L
                if (
                    ReminderPolicy(context).next("shift-${shift.segment.id}",whenMs,now=nowMillis) != null
                )
                    schedule(
                        context,
                        DayItemV11(
                            "shift-${shift.segment.id}",
                            shift.entry.date,
                            type = com.todoshift.app.v11.DayItemTypeV11.REMINDER,
                            title = "ToDo Shift • ${shift.segment.kind}",
                            reminderAt = whenMs,
                        ),
                    )
            }
        }

        db.dayItems.forEach { schedule(context,it) }
        db.tasks.forEach { schedule(context,it) }
    }

    private fun protected(context: Context): Boolean = runCatching {
        val security = com.todoshift.app.v11.V11SecurityStore(context).load()
        security.mode != com.todoshift.app.v11.V11LockMode.NONE || security.biometricEnabled
    }.getOrDefault(true)

    fun show(context: Context, id: String, title: String, details: String, date: String) {
        createChannel(context)

        if (
            Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val safeTitle = if(protected(context)) "ToDo Shift" else title
        val safeDetails = if(protected(context)) "افتح التطبيق للتفاصيل / Open app for details" else details
        val channelId = if(com.todoshift.app.v11.NeoNotificationPolicy.quiet(context)) QUIET_CHANNEL_ID else CHANNEL_ID
        val openIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

                action = "com.todoshift.app.OPEN_DATE"

                putExtra("todo_shift_open_date", date)
                if(!id.startsWith("shift-")) {
                    putExtra("todo_shift_task_action", "open")
                    putExtra("todo_shift_task_open_id",id)
                }
            }

        val contentIntent =
            PendingIntent.getActivity(
                context,
                requestCode(id) + 1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val publicNotification =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ToDo Shift")
                .setContentText("افتح التطبيق للتفاصيل / Open the app for details")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(safeTitle.ifBlank { "تذكير ToDo Shift" })
                .setContentText(safeDetails.ifBlank { date })
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(safeDetails.ifBlank { "لديك تذكير يوم $date" })
                )
                .setSilent(com.todoshift.app.v11.NeoNotificationPolicy.quiet(context))
                .addAction(0,"غفوة / Snooze", PendingIntent.getActivity(context,requestCode(id)+2,
                    Intent(context, ReminderSnoozeActivity::class.java).putExtra(EXTRA_ID,id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()

        NotificationManagerCompat.from(context).notify(requestCode(id), notification)
    }

    private fun requestCode(id: String): Int = id.hashCode() and 0x7fffffff
}

class ToDoShiftReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ToDoShiftReminder.ACTION_REMINDER) {
            return
        }

        val id = intent.getStringExtra(ToDoShiftReminder.EXTRA_ID) ?: return

        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob()+kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                runCatching {
                    val app=context.applicationContext
                    val db=V11Runtime(app).start()
                    val task=db.tasks.firstOrNull {it.id==id}
                    val item=db.dayItems.firstOrNull {it.id==id}
                    val shift=if(id.startsWith("shift-")) com.todoshift.app.v11.neoShiftOccurrences(db).firstOrNull {"shift-${it.segment.id}"==id} else null
                    val category=if(task!=null) "tasks" else if(shift!=null) "shifts" else "events"
                    if(!com.todoshift.app.v11.NeoNotificationPolicy.enabled(app,category)) return@runCatching
                    if(task==null && item==null && shift==null) return@runCatching
                    if(task?.status==TaskStatusV11.COMPLETED || item?.completed==true || item?.noteVisible==false || item?.noteTrashed==true) return@runCatching
                    val source=intent.getLongExtra(ToDoShiftReminder.EXTRA_SOURCE_AT,0L)
                    val currentSource=task?.reminderAt ?: item?.reminderAt ?: shift?.start?.atZone(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli()?.minus(com.todoshift.app.v11.NeoNotificationPolicy.shiftLeadMinutes(app)*60_000L)
                    if(currentSource==null || source!=currentSource) return@runCatching
                    when {task!=null->ToDoShiftReminder.schedule(app,task);item!=null->ToDoShiftReminder.schedule(app,item)}
                    val scheduled=intent.getLongExtra(ToDoShiftReminder.EXTRA_SCHEDULED_AT,0L)
                    val now=System.currentTimeMillis()
                    // A delayed broadcast must never replay yesterday's reminders in a flood.
                    if(scheduled<=0 || now<scheduled-60_000L || now-scheduled>15*60_000L) return@runCatching
                    val private=item?.notePrivate==true
                    ToDoShiftReminder.show(app,id,
                        if(private) "ToDo Shift" else task?.title ?: item?.title ?: "ToDo Shift",
                        if(private) "افتح التطبيق للتفاصيل / Open app for details" else task?.details ?: item?.details ?: "",
                        task?.date ?: item?.date ?: shift!!.entry.date)
                }
            } finally { pending.finish() }
        }
    }
}

class ToDoShiftBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
                intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pending = goAsync()
        kotlinx.coroutines
            .CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
            .launch {
                try {
                    runCatching {
                        val db = V11Runtime(context.applicationContext).start()
                        ToDoShiftReminder.rescheduleAll(context.applicationContext, db)
                    }
                } finally {
                    pending.finish()
                }
            }
    }
}
