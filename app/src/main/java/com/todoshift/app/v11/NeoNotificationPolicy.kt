package com.todoshift.app.v11

import android.content.Context
import java.time.Instant
import java.time.ZoneId

object NeoNotificationPolicy {
    fun enabled(context: Context, category: String): Boolean =
        NeoSmartStore(context).snapshot().optBoolean("notify_$category", true)

    fun deliveryTime(context: Context, original: Long): Long {
        val config = NeoSmartStore(context).snapshot()
        if (!config.optBoolean("quietHours", false)) return original
        val date = Instant.ofEpochMilli(original).atZone(ZoneId.systemDefault())
        return when {
            date.hour >= 22 ->
                date.plusDays(1).withHour(7).withMinute(0).withSecond(0).toInstant().toEpochMilli()
            date.hour < 7 -> date.withHour(7).withMinute(0).withSecond(0).toInstant().toEpochMilli()
            else -> original
        }
    }
}
