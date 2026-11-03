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

class TasksWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_COMPLETE =
            "com.todoshift.app.widget.TASK_COMPLETE"

        const val EXTRA_TASK_ID =
            "todo_shift_widget_task_id"

        fun refreshAll(context: Context) {
            val manager =
                AppWidgetManager.getInstance(context)

            val ids = manager.getAppWidgetIds(
                ComponentName(
                    context,
                    TasksWidgetProvider::class.java
                )
            )

            ids.forEach {
                updateWidget(context, manager, it)
            }

            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(
                    ids,
                    R.id.tasks_widget_list
                )
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_tasks
            )

            val count = runCatching {
                V11Runtime(context)
                    .start()
                    .tasks
                    .count {
                        it.status != TaskStatusV11.COMPLETED
                    }
            }.getOrDefault(0)

            views.setTextViewText(
                R.id.tasks_widget_count,
                count.toString()
            )

            val service = Intent(
                context,
                TasksWidgetService::class.java
            ).apply {
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    id
                )
                data = Uri.parse(
                    "todoshift://tasks-widget/$id"
                )
            }

            views.setRemoteAdapter(
                R.id.tasks_widget_list,
                service
            )

            views.setEmptyView(
                R.id.tasks_widget_list,
                R.id.tasks_widget_empty
            )

            val openApp = PendingIntent.getActivity(
                context,
                id * 100 + 1,
                Intent(
                    context,
                    MainActivity::class.java
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.tasks_widget_title,
                openApp
            )

            views.setOnClickPendingIntent(
                R.id.tasks_widget_add,
                openApp
            )

            val complete = Intent(
                context,
                TasksWidgetProvider::class.java
            ).apply {
                action = ACTION_COMPLETE
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    id
                )
            }

            val template = PendingIntent.getBroadcast(
                context,
                id * 100 + 2,
                complete,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_MUTABLE
            )

            views.setPendingIntentTemplate(
                R.id.tasks_widget_list,
                template
            )

            manager.updateAppWidget(id, views)
        }
    }

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach {
            updateWidget(context, manager, it)
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(context, intent)

        if (intent.action != ACTION_COMPLETE) return

        val taskId =
            intent.getStringExtra(EXTRA_TASK_ID)
                ?: return

        val runtime = V11Runtime(context)
        val db = runCatching {
            runtime.start()
        }.getOrNull() ?: return

        val task = db.tasks.firstOrNull {
            it.id == taskId
        } ?: return

        if (task.status == TaskStatusV11.COMPLETED) {
            return
        }

        val now = System.currentTimeMillis()

        val updated = db.copy(
            tasks = db.tasks.map {
                if (it.id == taskId) {
                    it.copy(
                        status = TaskStatusV11.COMPLETED,
                        completedAt = now,
                        updatedAt = now
                    )
                } else it
            }
        )

        ToDoShiftReminder.cancel(
            context,
            taskId
        )

        runtime.save(updated)
        refreshAll(context)
    }
}
