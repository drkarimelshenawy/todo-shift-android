package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.backup.Neo4DataGate
import com.todoshift.app.reminders.ToDoShiftReminder
import com.todoshift.app.widget.Neo4WidgetRefresh

/** Write-ahead service intent, replayed from confirmed storage after interruption. */
class NeoCommittedEffects(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("todo_shift_pending_effects", Context.MODE_PRIVATE)

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
                prefs.edit().putBoolean("pending", true).putStringSet("cancelIds", ids).commit()
            ) {
                "Cannot record pending refresh; data was not written"
            }
        }

    fun reconcile(
        confirmed: V11Database,
        cancel: (String) -> Unit = { ToDoShiftReminder.cancel(app, it) },
        schedule: (V11Database) -> Unit = { ToDoShiftReminder.rescheduleAll(app, it) },
        widgets: () -> Unit = { Neo4WidgetRefresh.schedule(app) },
    ) =
        synchronized(Neo4DataGate.monitor) {
            if (!prefs.getBoolean("pending", false)) return@synchronized
            prefs.getStringSet("cancelIds", emptySet()).orEmpty().forEach(cancel)
            schedule(confirmed)
            widgets()
            check(prefs.edit().clear().commit()) { "Refresh completed; acknowledgement pending" }
        }
}
