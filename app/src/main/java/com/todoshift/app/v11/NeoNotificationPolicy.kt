package com.todoshift.app.v11

import android.content.Context
import java.time.Instant
import java.time.ZoneId

object NeoNotificationPolicy {
    fun enabled(context: Context, category: String): Boolean {
        val config = NeoSmartStore(context).snapshot()
        return config.optBoolean("notificationsEnabled", true) && config.optBoolean("notify_$category", category != "shifts")
    }

    /** Quiet Hours change delivery presentation, never recognition time or source records. */
    fun deliveryTime(context: Context, original: Long): Long = original

    fun quiet(context: Context, at: Long = System.currentTimeMillis()): Boolean {
        val config = NeoSmartStore(context).snapshot()
        if (!config.optBoolean("quietHours", false)) return false
        val hour = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).hour
        return hour >= 22 || hour < 7
    }

    fun shiftLeadMinutes(context: Context): Int = NeoSmartStore(context).snapshot()
        .optInt("shiftReminderMinutes", 30).coerceIn(0, 10080)
}
