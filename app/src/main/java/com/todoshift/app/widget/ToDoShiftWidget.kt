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

            manager.notifyAppWidgetViewDataChanged(
                ids,
                R.id.widget_grid
            )
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int
        ) {
            val month =
                CalendarWidgetState.month(
                    context,
                    widgetId
                )

            val db =
                runCatching {
                    V11Runtime(context)
                        .start()
                }.getOrNull()

            val count =
                db?.days
                    ?.filter { entry ->
                        entry.date.startsWith(
                            month.toString()
                        ) &&
                        db.calendars
                            .firstOrNull { calendar ->
                                calendar.id ==
                                    entry.calendarId
                            }
                            ?.visible != false
                    }
                    ?.flatMap {
                        it.segments
                    }
                    ?.count {
                        it.kind.name != "OFF"
                    }
                    ?: 0

            val views =
                RemoteViews(
                    context.packageName,
                    R.layout.widget_calendar
                )

            applyWidgetTheme(
                context = context,
                views = views
            )

            views.setTextViewText(
                R.id.widget_title,
                arabicMonth(month)
            )

            views.setTextViewText(
                R.id.widget_summary,
                if (count == 0)
                    "لا توجد شيفتات"
                else
                    "$count شيفت"
            )

            val serviceIntent =
                Intent(
                    context,
                    CalendarWidgetService::
                        class.java
                ).apply {
                    putExtra(
                        AppWidgetManager
                            .EXTRA_APPWIDGET_ID,
                        widgetId
                    )

                    data = Uri.parse(
                        "todoshift://widget/" +
                            widgetId +
                            "/" +
                            month
                    )
                }

            views.setRemoteAdapter(
                R.id.widget_grid,
                serviceIntent
            )

            val prevIntent =
                Intent(
                    context,
                    ToDoShiftWidget::
                        class.java
                ).apply {
                    action = ACTION_PREV
                    putExtra(
                        AppWidgetManager
                            .EXTRA_APPWIDGET_ID,
                        widgetId
                    )
                }

            val nextIntent =
                Intent(
                    context,
                    ToDoShiftWidget::
                        class.java
                ).apply {
                    action = ACTION_NEXT
                    putExtra(
                        AppWidgetManager
                            .EXTRA_APPWIDGET_ID,
                        widgetId
                    )
                }

            views.setOnClickPendingIntent(
                R.id.widget_prev,
                PendingIntent.getBroadcast(
                    context,
                    widgetId * 10 + 1,
                    prevIntent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )
            )

            views.setOnClickPendingIntent(
                R.id.widget_next,
                PendingIntent.getBroadcast(
                    context,
                    widgetId * 10 + 2,
                    nextIntent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )
            )

            val openApp =
                Intent(
                    context,
                    MainActivity::class.java
                )

            views.setOnClickPendingIntent(
                R.id.widget_title,
                PendingIntent.getActivity(
                    context,
                    widgetId * 10 + 3,
                    openApp,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_IMMUTABLE
                )
            )

            val templateIntent =
                Intent(
                    context,
                    MainActivity::class.java
                ).apply {
                    action =
                        "com.todoshift.app.OPEN_DATE"
                }

            val template =
                PendingIntent.getActivity(
                    context,
                    widgetId * 10 + 4,
                    templateIntent,
                    PendingIntent
                        .FLAG_UPDATE_CURRENT or
                        PendingIntent
                            .FLAG_MUTABLE
                )

            views.setPendingIntentTemplate(
                R.id.widget_grid,
                template
            )

            val neoSettings = Neo4WidgetSettings(context)
            if (neoSettings.modern(Neo4WidgetKind.CALENDAR)) {
                val options = neoSettings.load(Neo4WidgetKind.CALENDAR)
                neoSettings.skin(Neo4WidgetKind.CALENDAR).apply(views, R.id.widget_root, R.id.widget_title, R.id.widget_summary, R.id.widget_prev, R.id.widget_next)
                views.setViewVisibility(R.id.widget_grid, if (options.hideContent) android.view.View.GONE else android.view.View.VISIBLE)
                views.setViewVisibility(R.id.widget_summary, if (options.showDetails && !options.hideContent) android.view.View.VISIBLE else android.view.View.GONE)
                views.setTextViewText(R.id.widget_title, month.atDay(1).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", if (neoSettings.english()) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("ar"))))
            }

            val appearance = com.todoshift.app.v11.NeoAppearanceStore(context).load()
            val skin = neoSettings.skin(Neo4WidgetKind.CALENDAR)
            views.setInt(R.id.widget_root,"setBackgroundColor",Color.TRANSPARENT)
            views.setImageViewBitmap(R.id.widget_panel,Neo4WidgetPainter.panel(skin.background,skin.accent))
            val rtl = neoSettings.calendarRtl(appearance)
            val direction = if(rtl) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR
            views.setInt(R.id.widget_grid,"setLayoutDirection",direction)
            views.setInt(R.id.widget_weekdays,"setLayoutDirection",direction)
            val weekdayIds=listOf(R.id.widget_weekday_0,R.id.widget_weekday_1,R.id.widget_weekday_2,R.id.widget_weekday_3,R.id.widget_weekday_4,R.id.widget_weekday_5,R.id.widget_weekday_6)
            val locale=if(neoSettings.english()) java.util.Locale.ENGLISH else java.util.Locale.forLanguageTag("ar")
            val ar=listOf("اثن","ثلا","أرب","خمي","جمع","سبت","أحد")
            weekdayIds.forEachIndexed { index,id ->
                val day=(appearance.firstWeekday-1+index)%7+1
                views.setTextViewText(id,if(neoSettings.english()) java.time.DayOfWeek.of(day).getDisplayName(java.time.format.TextStyle.SHORT,locale) else ar[day-1])
                views.setTextColor(id,skin.text)
            }
            views.setViewVisibility(R.id.widget_weekdays,if(neoSettings.load(Neo4WidgetKind.CALENDAR).hideContent) android.view.View.GONE else android.view.View.VISIBLE)
            manager.updateAppWidget(
                widgetId,
                views
            )

            manager
                .notifyAppWidgetViewDataChanged(
                    widgetId,
                    R.id.widget_grid
                )
        }

        private fun applyWidgetTheme(
            context: Context,
            views: RemoteViews
        ) {
            val settings =
                V11UiSettingsStore(
                    context.applicationContext
                ).load()

            val resolved =
                when (
                    settings.widgetTheme
                ) {
                    V11WidgetTheme.FOLLOW_APP ->
                        when (
                            settings.appTheme
                        ) {
                            V11AppTheme.MIDNIGHT ->
                                V11WidgetTheme.DARK

                            else ->
                                V11WidgetTheme.LIGHT
                        }

                    else ->
                        settings.widgetTheme
                }

            val background: Int
            val primaryText: Int
            val secondaryText: Int

            when (resolved) {
                V11WidgetTheme.DARK -> {
                    background =
                        Color.rgb(
                            22,
                            27,
                            34
                        )
                    primaryText =
                        Color.rgb(
                            230,
                            237,
                            243
                        )
                    secondaryText =
                        Color.rgb(
                            170,
                            183,
                            196
                        )
                }

                V11WidgetTheme.TRANSPARENT -> {
                    background =
                        Color.TRANSPARENT
                    primaryText =
                        Color.rgb(
                            23,
                            32,
                            51
                        )
                    secondaryText =
                        Color.rgb(
                            71,
                            85,
                            105
                        )
                }

                else -> {
                    background =
                        Color.rgb(
                            248,
                            250,
                            252
                        )
                    primaryText =
                        Color.rgb(
                            13,
                            35,
                            68
                        )
                    secondaryText =
                        Color.rgb(
                            100,
                            116,
                            139
                        )
                }
            }

            views.setInt(
                R.id.widget_root,
                "setBackgroundColor",
                background
            )

            listOf(
                R.id.widget_prev,
                R.id.widget_title,
                R.id.widget_next
            ).forEach {
                views.setTextColor(
                    it,
                    primaryText
                )
            }

            views.setTextColor(
                R.id.widget_summary,
                secondaryText
            )
        }

        private fun arabicMonth(
            month: YearMonth
        ): String {
            val names = listOf(
                "يناير",
                "فبراير",
                "مارس",
                "أبريل",
                "مايو",
                "يونيو",
                "يوليو",
                "أغسطس",
                "سبتمبر",
                "أكتوبر",
                "نوفمبر",
                "ديسمبر"
            )

            return "${
                names[
                    month.monthValue - 1
                ]
            } ${month.year}"
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
