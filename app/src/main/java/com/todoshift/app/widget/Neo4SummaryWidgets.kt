package com.todoshift.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.todoshift.app.MainActivity
import com.todoshift.app.R
import com.todoshift.app.v11.*
import java.time.LocalDateTime
import kotlinx.coroutines.*

object Neo4WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: Job? = null

    @Synchronized
    fun schedule(context: Context) {
        val app = context.applicationContext
        pending?.cancel()
        pending =
            scope.launch {
                delay(180)
                runCatching { refreshAll(app) }
            }
    }

    fun refreshAll(context: Context) {
        ToDoShiftWidget.refreshAll(context)
        TasksWidgetProvider.refreshAll(context)
        val manager = AppWidgetManager.getInstance(context)
        listOf(
                Neo4TodayWidget::class.java to Neo4WidgetKind.TODAY,
                Neo4FinanceWidget::class.java to Neo4WidgetKind.FINANCE,
            )
            .forEach { (type, kind) ->
                manager.getAppWidgetIds(ComponentName(context, type)).forEach {
                    render(context, manager, it, kind)
                }
            }
    }

    fun render(context: Context, manager: AppWidgetManager, id: Int, kind: Neo4WidgetKind) {
        val settings = Neo4WidgetSettings(context)
        val options = settings.load(kind)
        val skin = settings.skin(kind)
        val en = settings.english()
        val db = runCatching { V11Runtime(context).start() }.getOrNull()
        val now = LocalDateTime.now()
        val views = RemoteViews(context.packageName, R.layout.neo4_widget_summary)
        skin.apply(
            views,
            R.id.neo4_widget_root,
            R.id.neo4_widget_title,
            R.id.neo4_widget_value,
            R.id.neo4_widget_detail,
        )
        val security = runCatching { V11SecurityStore(context).load() }.getOrNull()
        val content =
            NeoWidgetContentModel.project(
                kind,
                db,
                options,
                security,
                Neo4WidgetPrivacy.amountsHidden(context),
                now,
                en,
            )
        views.setTextViewText(R.id.neo4_widget_title, content.title)
        views.setTextViewText(R.id.neo4_widget_value, content.value)
        views.setTextViewText(R.id.neo4_widget_detail, content.detail)
        val intent =
            Intent(context, MainActivity::class.java)
                .putExtra("neo4_open_tab", if (kind == Neo4WidgetKind.FINANCE) 2 else 4)
                .setData(android.net.Uri.parse("todoshift://neo4/${kind.name}/$id"))
        val open =
            PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        views.setOnClickPendingIntent(R.id.neo4_widget_root, open)
        manager.updateAppWidget(id, views)
    }
}

abstract class Neo4SummaryWidgetProvider : AppWidgetProvider() {
    abstract val kind: Neo4WidgetKind

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach {
                    runCatching {
                        Neo4WidgetRefresh.render(context.applicationContext, manager, it, kind)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }
}

class Neo4TodayWidget : Neo4SummaryWidgetProvider() {
    override val kind = Neo4WidgetKind.TODAY
}

class Neo4FinanceWidget : Neo4SummaryWidgetProvider() {
    override val kind = Neo4WidgetKind.FINANCE
}
