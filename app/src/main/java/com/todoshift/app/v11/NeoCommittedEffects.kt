package com.todoshift.app.v11

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import com.todoshift.app.backup.Neo4DataGate
import com.todoshift.app.reminders.ToDoShiftReminder
import com.todoshift.app.widget.Neo4WidgetRefresh

/** Service intent is journaled with the data; replay always uses the confirmed database. */
class NeoCommittedEffects(context: Context) {
    companion object {
        const val RETRY_JOB_ID = 0x544453
        const val STORE_NAME = "todo_shift_pending_effects"
    }

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

    fun prepare(previous: V11Database, next: V11Database) =
        synchronized(Neo4DataGate.monitor) {
            val tasks = next.tasks.associateBy { it.id }
            val events = next.dayItems.associateBy { it.id }
            val shifts =
                next.days
                    .flatMap { day ->
                        day.segments.map { it.id to Triple(day.date, day.calendarId, it) }
                    }
                    .toMap()
            val changed =
                previous.tasks.filter { tasks[it.id] != it }.map { it.id } +
                    previous.dayItems.filter { events[it.id] != it }.map { it.id } +
                    previous.days.flatMap { day ->
                        day.segments
                            .filter { shifts[it.id] != Triple(day.date, day.calendarId, it) }
                            .map { "shift-${it.id}" }
                    }
            val ids = prefs.getStringSet("cancelIds", emptySet()).orEmpty().toSet() + changed
            check(
                prefs
                    .edit()
                    .putBoolean("pending", true)
                    .putBoolean("widgetsOnly", false)
                    .putStringSet("cancelIds", ids)
                    .commit()
            ) {
                "Cannot record pending refresh; data was not written"
            }
            queueRetry()
            Unit
        }

    /** Appearance/widgets never change alarm state; a pending business replay takes precedence. */
    fun prepareWidgets() =
        synchronized(Neo4DataGate.monitor) {
            if (!prefs.getBoolean("pending", false))
                check(
                    prefs
                        .edit()
                        .putBoolean("pending", true)
                        .putBoolean("widgetsOnly", true)
                        .commit()
                ) {
                    "Cannot record widget refresh"
                }
            queueRetry()
        }

    private fun queueRetry() {
        // Android may reject/defer jobs. Keep the durable marker and boot/app-open fallback.
        runCatching {
            val scheduler = app.getSystemService(JobScheduler::class.java)
            val job =
                JobInfo.Builder(RETRY_JOB_ID, ComponentName(app, NeoEffectsJobService::class.java))
                    .setPersisted(true)
                    .setMinimumLatency(1000)
                    .setOverrideDeadline(60000)
                    .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .build()
            check(scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS)
        }
    }

    /** Read and replay under one gate; a later writer cannot race a stale service snapshot. */
    fun reconcileCurrent() =
        synchronized(Neo4DataGate.monitor) { reconcile(V11Runtime(app).start()) }

    internal fun reconcile(
        confirmed: V11Database,
        cancel: (String) -> Unit = { ToDoShiftReminder.cancel(app, it) },
        schedule: (V11Database) -> Unit = { ToDoShiftReminder.rescheduleAll(app, it) },
        widgets: () -> Unit = { Neo4WidgetRefresh.refreshAll(app) },
    ) =
        synchronized(Neo4DataGate.monitor) {
            if (!prefs.getBoolean("pending", false)) return@synchronized
            if (!prefs.getBoolean("widgetsOnly", false)) {
                prefs.getStringSet("cancelIds", emptySet()).orEmpty().forEach(cancel)
                schedule(confirmed)
            }
            widgets()
            check(prefs.edit().clear().commit()) { "Refresh completed; acknowledgement pending" }
            runCatching { app.getSystemService(JobScheduler::class.java).cancel(RETRY_JOB_ID) }
            Unit
        }
}
