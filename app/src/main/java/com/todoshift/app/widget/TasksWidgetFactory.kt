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
        val today = LocalDate.now().toString()

        tasks = runCatching {
            V11Runtime(context)
                .start()
                .tasks
                .filter {
                    it.status != TaskStatusV11.COMPLETED
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
            putExtra(
                TasksWidgetProvider.EXTRA_TASK_ID,
                task.id
            )
        }

        views.setOnClickFillInIntent(
            R.id.task_widget_check,
            completeIntent
        )

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
