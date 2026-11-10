package com.todoshift.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.todoshift.app.MainActivity
import com.todoshift.app.R
import com.todoshift.app.reminders.ToDoShiftReminder
import com.todoshift.app.v11.TaskStatusV11
import com.todoshift.app.v11.V11Runtime
import kotlinx.coroutines.launch

class TasksWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_OPEN = "com.todoshift.app.widget.TASK_OPEN"

        const val ACTION_COMPLETE = "com.todoshift.app.widget.TASK_COMPLETE"

        const val EXTRA_TASK_ID = "todo_shift_widget_task_id"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)

            val ids =
                manager.getAppWidgetIds(ComponentName(context, TasksWidgetProvider::class.java))

            ids.forEach { updateWidget(context, manager, it) }

            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.tasks_widget_list)
            }
        }

        private fun applyTasksTheme(context: Context, views: RemoteViews) {
            val settings =
                com.todoshift.app.v11.V11UiSettingsStore(context.applicationContext).load()

            val resolved =
                when (settings.widgetTheme) {
                    com.todoshift.app.v11.V11WidgetTheme.FOLLOW_APP ->
                        if (settings.appTheme == com.todoshift.app.v11.V11AppTheme.MIDNIGHT) {
                            com.todoshift.app.v11.V11WidgetTheme.DARK
                        } else {
                            com.todoshift.app.v11.V11WidgetTheme.LIGHT
                        }
                    else -> settings.widgetTheme
                }

            val background: Int
            val foreground: Int
            val secondary: Int

            when (resolved) {
                com.todoshift.app.v11.V11WidgetTheme.DARK -> {
                    background = android.graphics.Color.rgb(22, 27, 34)
                    foreground = android.graphics.Color.WHITE
                    secondary = android.graphics.Color.LTGRAY
                }
                com.todoshift.app.v11.V11WidgetTheme.TRANSPARENT -> {
                    background = android.graphics.Color.TRANSPARENT
                    foreground = android.graphics.Color.rgb(23, 32, 51)
                    secondary = android.graphics.Color.GRAY
                }
                else -> {
                    background = android.graphics.Color.rgb(248, 250, 252)
                    foreground = android.graphics.Color.rgb(23, 32, 51)
                    secondary = android.graphics.Color.GRAY
                }
            }

            views.setInt(R.id.tasks_widget_root, "setBackgroundColor", background)

            views.setTextColor(R.id.tasks_widget_title, foreground)

            views.setTextColor(R.id.tasks_widget_count, secondary)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_tasks)

            val count =
                runCatching {
                        val db = V11Runtime(context).start()
                        val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
                        db.tasks.count {
                            it.status != TaskStatusV11.COMPLETED &&
                                (it.calendarId == null || it.calendarId in visible)
                        }
                    }
                    .getOrNull()

            views.setTextViewText(R.id.tasks_widget_count, count?.toString() ?: "!")

            val service =
                Intent(context, TasksWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    data = Uri.parse("todoshift://tasks-widget/$id")
                }

            views.setRemoteAdapter(R.id.tasks_widget_list, service)

            views.setEmptyView(R.id.tasks_widget_list, R.id.tasks_widget_empty)

            fun taskActivityIntent(actionName: String, taskId: String? = null): Intent =
                Intent(context, MainActivity::class.java).apply {
                    putExtra("todo_shift_task_action", actionName)
                    taskId?.let { putExtra("todo_shift_task_open_id", it) }
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

            val openApp =
                PendingIntent.getActivity(
                    context,
                    id * 100 + 1,
                    taskActivityIntent("list"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            views.setOnClickPendingIntent(R.id.tasks_widget_title, openApp)

            val addTask =
                PendingIntent.getActivity(
                    context,
                    id * 100 + 3,
                    Intent(context, com.todoshift.app.QuickTaskActivity::class.java)
                        .setData(Uri.parse("todoshift://quick-add/$id")),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            views.setOnClickPendingIntent(R.id.tasks_widget_add, addTask)

            val complete =
                Intent(context, TasksWidgetProvider::class.java).apply {
                    action = ACTION_COMPLETE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                }

            val template =
                PendingIntent.getBroadcast(
                    context,
                    id * 100 + 2,
                    complete,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )

            views.setPendingIntentTemplate(R.id.tasks_widget_list, template)

            applyTasksTheme(context, views)
            val neoSettings = Neo4WidgetSettings(context)
            if (neoSettings.modern(Neo4WidgetKind.TASKS)) {
                val options = neoSettings.load(Neo4WidgetKind.TASKS)
                neoSettings
                    .skin(Neo4WidgetKind.TASKS)
                    .apply(
                        views,
                        R.id.tasks_widget_root,
                        R.id.tasks_widget_title,
                        R.id.tasks_widget_count,
                        R.id.tasks_widget_empty,
                    )
                views.setViewVisibility(
                    R.id.tasks_widget_list,
                    if (options.hideContent) android.view.View.GONE else android.view.View.VISIBLE,
                )
                if (options.hideContent) {
                    views.setViewVisibility(R.id.tasks_widget_empty, android.view.View.VISIBLE)
                    views.setTextViewText(R.id.tasks_widget_count, "")
                    views.setTextViewText(
                        R.id.tasks_widget_empty,
                        if (neoSettings.english()) "Content hidden" else "المحتوى مخفي",
                    )
                }
                views.setTextViewText(
                    R.id.tasks_widget_title,
                    if (neoSettings.english()) "Tasks" else "المهام",
                )
            }
            manager.updateAppWidget(id, views)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_OPEN) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

            val openTask =
                Intent(context, MainActivity::class.java).apply {
                    putExtra("todo_shift_task_action", "open")
                    putExtra("todo_shift_task_open_id", taskId)
                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }

            context.startActivity(openTask)
            return
        }

        if (intent.action != ACTION_COMPLETE) return

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        val pending = goAsync()
        kotlinx.coroutines
            .CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
            .launch {
                try {
                    runCatching { completeTaskOnIo(context.applicationContext, taskId) }
                } finally {
                    pending.finish()
                }
            }
    }

    private fun completeTaskOnIo(context: Context, taskId: String) {
        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
            val runtime = V11Runtime(context)
            val db = runCatching { runtime.start() }.getOrNull() ?: return

            val task = db.tasks.firstOrNull { it.id == taskId } ?: return

            if (task.status == TaskStatusV11.COMPLETED) {
                return
            }

            val now = System.currentTimeMillis()

            val updated =
                db.copy(
                    tasks =
                        db.tasks.map {
                            if (it.id == taskId) {
                                it.copy(
                                    status = TaskStatusV11.COMPLETED,
                                    completedAt = now,
                                    updatedAt = now,
                                )
                            } else it
                        }
                )

            com.todoshift.app.backup.Neo4LocalBackup(context).commitDatabase(db, updated)
            runCatching {
                com.todoshift.app.v11.NeoCommittedEffects(context).reconcileCurrent()
            }
            ToDoShiftReminder.cancel(context, taskId)
            refreshAll(context)
        }
    }
}
