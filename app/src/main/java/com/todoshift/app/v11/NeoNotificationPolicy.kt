package com.todoshift.app.v11

import android.content.Context
import java.time.Instant
import java.time.ZoneId

object NeoNotificationPolicy {
    fun enabled(context: Context, category: String): Boolean {
        val config = NeoSmartStore(context).snapshot()
        return config.optBoolean("notificationsEnabled", true) && config.optBoolean("notify_$category", if(category=="notes") config.optBoolean("notify_events",true) else category != "shifts")
    }

    /** Quiet Hours change delivery presentation, never recognition time or source records. */
    fun deliveryTime(context: Context, original: Long): Long = original

    fun quiet(context: Context, at: Long = System.currentTimeMillis()): Boolean {
        val config = NeoSmartStore(context).snapshot()
        if (!config.optBoolean("quietHours", false)) return false
        val local=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
        val minute=local.hour*60+local.minute
        val start=config.optInt("quietStartMinute",22*60).coerceIn(0,1439)
        val end=config.optInt("quietEndMinute",7*60).coerceIn(0,1439)
        return if(start==end) true else if(start<end) minute in start until end else minute>=start || minute<end
    }

    fun shiftLeadMinutes(context: Context): Int = NeoSmartStore(context).snapshot()
        .optInt("shiftReminderMinutes", 30).coerceIn(0, 10080)
}
