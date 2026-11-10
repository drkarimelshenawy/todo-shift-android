package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.backup.Neo4DataGate
import com.todoshift.app.reminders.ToDoShiftReminder
import com.todoshift.app.widget.Neo4WidgetRefresh

/**
 * Write-ahead service intent. Reconciliation always reads the confirmed database, never a draft.
 */
class NeoCommittedEffects(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("todo_shift_pending_effects", Context.MODE_PRIVATE)

    fun prepare(previous: V11Database) =
        synchronized(Neo4DataGate.monitor) {
            val ids =
                prefs.getStringSet("cancelIds", emptySet()).orEmpty().toSet() +
                    previous.tasks.map { it.id } +
                    previous.dayItems.map { it.id } +
                    previous.days.flatMap { it.segments }.map { "shift-${it.id}" }
            check(
                prefs.edit().putBoolean("pending", true).putStringSet("cancelIds", ids).commit()
            ) {
                "Cannot record pending reminder refresh; data was not written"
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
