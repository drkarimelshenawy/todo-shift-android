package com.todoshift.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.todoshift.app.R
import com.todoshift.app.v11.TaskStatusV11
import com.todoshift.app.v11.TaskV11
import com.todoshift.app.v11.V11Runtime
import java.time.LocalDate

class TasksWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<TaskV11> = emptyList()

    override fun onCreate() {
        reload()
    }

    override fun onDataSetChanged() {
        reload()
    }

    private fun reload() {
        if (Neo4WidgetSettings(context).load(Neo4WidgetKind.TASKS).hideContent) { tasks = emptyList(); return }
        val today = LocalDate.now().toString()

        tasks = runCatching {
            val db = V11Runtime(context).start()
            val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
            db.tasks.filter {
                it.status != TaskStatusV11.COMPLETED && (it.calendarId == null || it.calendarId in visible)
            }
                .sortedWith(
                    compareBy<TaskV11> {
                        if (it.date <= today) 0 else 1
                    }.thenBy {
                        it.date
                    }.thenBy {
                        it.time ?: "99:99"
                    }
                )
        }.getOrDefault(emptyList())
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        val task = tasks.getOrNull(position)
            ?: return null

        val views = RemoteViews(
            context.packageName,
            R.layout.widget_task_row
        )

        views.setTextViewText(
            R.id.task_widget_name,
            task.title
        )

        val overdue =
            task.date < LocalDate.now().toString()

        val meta = buildString {
            if (overdue) append("متأخرة • ")
            append(task.date)
            task.time?.let {
                append(" • ")
                append(it)
            }
            if (
                task.priority.name == "HIGH" ||
                task.priority.name == "URGENT"
            ) {
                append(" • مهمة")
            }
        }

        views.setTextViewText(
            R.id.task_widget_meta,
            meta
        )

        val completeIntent = Intent().apply {
            action = TasksWidgetProvider.ACTION_COMPLETE
            putExtra(
                TasksWidgetProvider.EXTRA_TASK_ID,
                task.id
            )
        }

        views.setOnClickFillInIntent(
            R.id.task_widget_check,
            completeIntent
        )

        val openIntent = Intent().apply {
            action = TasksWidgetProvider.ACTION_OPEN
            putExtra(
                TasksWidgetProvider.EXTRA_TASK_ID,
                task.id
            )
        }

        views.setOnClickFillInIntent(
            R.id.task_widget_content,
            openIntent
        )

        val settings =
            com.todoshift.app.v11.V11UiSettingsStore(
                context.applicationContext
            ).load()

        val dark =
            settings.widgetTheme ==
                com.todoshift.app.v11.V11WidgetTheme.DARK ||
                (
                    settings.widgetTheme ==
                        com.todoshift.app.v11.V11WidgetTheme.FOLLOW_APP &&
                    settings.appTheme ==
                        com.todoshift.app.v11.V11AppTheme.MIDNIGHT
                )

        views.setTextColor(
            R.id.task_widget_name,
            if (dark) android.graphics.Color.WHITE
            else android.graphics.Color.rgb(23, 32, 51)
        )

        views.setTextColor(
            R.id.task_widget_meta,
            if (dark) android.graphics.Color.LTGRAY
            else android.graphics.Color.GRAY
        )

        val neoSettings = Neo4WidgetSettings(context)
        if (neoSettings.modern(Neo4WidgetKind.TASKS)) {
            val skin = neoSettings.skin(Neo4WidgetKind.TASKS)
            views.setTextColor(R.id.task_widget_name, skin.text); views.setTextColor(R.id.task_widget_meta, skin.secondary)
            val en = neoSettings.english()
            views.setTextViewText(R.id.task_widget_meta, (if (overdue) (if (en) "Overdue • " else "متأخرة • ") else "") + task.date + (task.time?.let { " • $it" } ?: ""))
            if (!neoSettings.load(Neo4WidgetKind.TASKS).showDetails) views.setViewVisibility(R.id.task_widget_meta, android.view.View.GONE)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long =
        tasks.getOrNull(position)
            ?.id
            ?.hashCode()
            ?.toLong()
            ?: position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun onDestroy() {
        tasks = emptyList()
    }
}
