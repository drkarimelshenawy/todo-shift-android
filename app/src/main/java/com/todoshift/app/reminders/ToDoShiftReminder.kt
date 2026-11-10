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

        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, item: DayItemV11) {
        if (
            !com.todoshift.app.v11.NeoNotificationPolicy.enabled(
                context,
                if (item.id.startsWith("shift-")) "shifts" else "events",
            )
        )
            return
        val whenMs =
            com.todoshift.app.v11.NeoNotificationPolicy.deliveryTime(
                context,
                item.reminderAt ?: return,
            )

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

                putExtra(EXTRA_TITLE, item.title)

                putExtra(EXTRA_DETAILS, item.details)

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
            com.todoshift.app.v11.NeoNotificationPolicy.deliveryTime(
                context,
                task.reminderAt ?: return,
            )

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

                putExtra(EXTRA_TITLE, task.title)

                putExtra(EXTRA_DETAILS, task.details)

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

    fun rescheduleAll(context: Context, db: V11Database) {
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
                        .toEpochMilli() - 30 * 60 * 1000
                if (
                    com.todoshift.app.v11.NeoNotificationPolicy.deliveryTime(context, whenMs) >
                        System.currentTimeMillis()
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

        db.dayItems
            .filter {
                !it.completed &&
                    it.reminderAt != null &&
                    com.todoshift.app.v11.NeoNotificationPolicy.deliveryTime(
                        context,
                        it.reminderAt,
                    ) > System.currentTimeMillis()
            }
            .forEach { schedule(context, it) }

        db.tasks
            .filter {
                it.status != TaskStatusV11.COMPLETED &&
                    it.reminderAt != null &&
                    com.todoshift.app.v11.NeoNotificationPolicy.deliveryTime(
                        context,
                        it.reminderAt,
                    ) > System.currentTimeMillis()
            }
            .forEach { schedule(context, it) }
    }

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

        val openIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

                action = "com.todoshift.app.OPEN_DATE"

                putExtra("todo_shift_open_date", date)
            }

        val contentIntent =
            PendingIntent.getActivity(
                context,
                requestCode(id) + 1,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title.ifBlank { "تذكير ToDo Shift" })
                .setContentText(details.ifBlank { date })
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(details.ifBlank { "لديك تذكير يوم $date" })
                )
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

        val title = intent.getStringExtra(ToDoShiftReminder.EXTRA_TITLE).orEmpty()

        val details = intent.getStringExtra(ToDoShiftReminder.EXTRA_DETAILS).orEmpty()

        val date = intent.getStringExtra(ToDoShiftReminder.EXTRA_DATE).orEmpty()

        ToDoShiftReminder.show(
            context = context,
            id = id,
            title = title,
            details = details,
            date = date,
        )
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
