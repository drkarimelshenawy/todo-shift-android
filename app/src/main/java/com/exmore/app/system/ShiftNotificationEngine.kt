package com.exmore.app.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object ShiftNotificationEngine {

    const val CHANNEL_ID =
        "todo_shift_reminders"

    fun ensureChannel(
        context: Context
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
