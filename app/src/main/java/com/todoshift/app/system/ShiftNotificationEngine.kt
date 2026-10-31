package com.todoshift.app.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object ShiftNotificationEngine {

    const val CHANNEL_ID =
        "todo_shift_reminders"

    fun ensureChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "تذكيرات الشيفتات",
                    NotificationManager
                        .IMPORTANCE_DEFAULT
                ).apply {
                    description =
                        "تنبيهات الشيفتات والمهام القادمة"
                }

            manager.createNotificationChannel(
                channel
            )
        }
    }
}
