package com.todoshift.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.todoshift.app.v11.V11AppTheme
import com.todoshift.app.v11.V11WidgetTheme
import com.todoshift.app.v11.V11UiSettingsStore
import android.graphics.Color
import android.widget.RemoteViews
import com.todoshift.app.MainActivity
import com.todoshift.app.R
import com.todoshift.app.v11.V11Runtime
import java.time.YearMonth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ToDoShiftWidget :
    AppWidgetProvider() {

    companion object {
        const val ACTION_PREV =
            "com.todoshift.app.widget.PREV"

        const val ACTION_NEXT =
            "com.todoshift.app.widget.NEXT"

        const val EXTRA_OPEN_DATE =
            "todo_shift_open_date"

        fun refreshAll(
            context: Context
        ) {
            val manager =
                AppWidgetManager
                    .getInstance(context)

            val component =
                ComponentName(
                    context,
                    ToDoShiftWidget::class.java
                )

            val ids =
                manager.getAppWidgetIds(
                    component
                )

            ids.forEach {
                updateWidget(
                    context,
                    manager,
                    it
                )
            }


        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val month=CalendarWidgetState.month(context,widgetId)
            val db=runCatching { V11Runtime(context).start() }.getOrNull()
            val options=manager.getAppWidgetOptions(widgetId)
            val minWidth=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,300).coerceAtLeast(160)
            val minHeight=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,300).coerceAtLeast(140)
            val maxWidth=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,minWidth).coerceAtLeast(minWidth)
            val maxHeight=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,minHeight).coerceAtLeast(minHeight)
            val portrait=NeoCalendarWidgetRenderer.build(context,widgetId,month,db,minWidth,maxHeight)
            val landscape=NeoCalendarWidgetRenderer.build(context,widgetId,month,db,maxWidth,minHeight)
            manager.updateAppWidget(widgetId,RemoteViews(landscape,portrait))
        }
    }

    // Broadcast receivers execute on the app main thread. Reading the database or
    // rendering RemoteViews here can freeze the launcher's host and our splash screen.
    private fun updateOffMain(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ids.forEach { id -> runCatching { updateWidget(context.applicationContext, manager, id) } }
            } finally {
                result.finish()
            }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        updateOffMain(context, manager, ids)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateOffMain(context, manager, intArrayOf(appWidgetId))
    }

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        super.onReceive(
            context,
            intent
        )

        if (
            intent.action != ACTION_PREV &&
            intent.action != ACTION_NEXT
        ) {
            return
        }

        val widgetId =
            intent.getIntExtra(
                AppWidgetManager
                    .EXTRA_APPWIDGET_ID,
                AppWidgetManager
                    .INVALID_APPWIDGET_ID
            )

        if (
            widgetId ==
            AppWidgetManager
                .INVALID_APPWIDGET_ID
        ) {
            return
        }

        val current =
            CalendarWidgetState.month(
                context,
                widgetId
            )

        val target =
            if (
                intent.action ==
                ACTION_PREV
            ) {
                current.minusMonths(1)
            } else {
                current.plusMonths(1)
            }

        CalendarWidgetState.saveMonth(
            context,
            widgetId,
            target
        )

        updateOffMain(context, AppWidgetManager.getInstance(context), intArrayOf(widgetId))
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach {
            CalendarWidgetState.delete(
                context,
                it
            )
        }

        super.onDeleted(
            context,
            appWidgetIds
        )
    }
}

