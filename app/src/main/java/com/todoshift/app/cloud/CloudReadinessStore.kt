package com.todoshift.app.cloud

import android.content.Context

class CloudReadinessStore(context: Context) {
    private val prefs = context.getSharedPreferences("todo_shift_cloud_readiness", Context.MODE_PRIVATE)
    private val history = context.getSharedPreferences("todo_shift_backup_local_history", Context.MODE_PRIVATE)
    fun schedule(): BackupSchedule = BackupSchedule(runCatching { BackupFrequency.valueOf(prefs.getString("frequency", "DISABLED") ?: "DISABLED") }.getOrDefault(BackupFrequency.DISABLED), prefs.getBoolean("wifiOnly", true))
    fun save(value: BackupSchedule) { prefs.edit().putString("frequency", value.frequency.name).putBoolean("wifiOnly", value.wifiOnly).apply() }
    // Local successes only. Never set these from a failed or disabled cloud operation.
    fun localSuccess(bytes: Long) { history.edit().putLong("lastLocalSuccess", System.currentTimeMillis()).putLong("lastLocalBytes", bytes).apply() }
    fun lastLocalSuccess() = history.getLong("lastLocalSuccess", 0L)
    fun lastLocalBytes() = history.getLong("lastLocalBytes", 0L)
}
