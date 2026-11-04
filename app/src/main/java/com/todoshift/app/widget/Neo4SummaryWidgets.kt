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
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Neo4WidgetRefresh {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pending: Job? = null
    @Synchronized fun schedule(context: Context) {
        val app = context.applicationContext
        pending?.cancel(); pending = scope.launch { delay(180); refreshAll(app) }
    }
    fun refreshAll(context: Context) {
        ToDoShiftWidget.refreshAll(context); TasksWidgetProvider.refreshAll(context)
        val manager = AppWidgetManager.getInstance(context)
        listOf(Neo4TodayWidget::class.java to Neo4WidgetKind.TODAY, Neo4FinanceWidget::class.java to Neo4WidgetKind.FINANCE).forEach { (type, kind) ->
            manager.getAppWidgetIds(ComponentName(context, type)).forEach { render(context, manager, it, kind) }
        }
    }
    fun render(context: Context, manager: AppWidgetManager, id: Int, kind: Neo4WidgetKind) {
        val settings = Neo4WidgetSettings(context); val options = settings.load(kind); val skin = settings.skin(kind)
        val en = settings.english(); val db = V11Runtime(context).start(); val now = LocalDateTime.now()
        val views = RemoteViews(context.packageName, R.layout.neo4_widget_summary)
        skin.apply(views, R.id.neo4_widget_root, R.id.neo4_widget_title, R.id.neo4_widget_value, R.id.neo4_widget_detail)
        views.setTextViewText(R.id.neo4_widget_title, if (kind == Neo4WidgetKind.TODAY) (if (en) "Today's shift" else "شيفت اليوم") else (if (en) "Monthly finance" else "حسابات الشهر"))
        if (options.hideContent) {
            views.setTextViewText(R.id.neo4_widget_value, if (en) "Hidden" else "المحتوى مخفي")
            views.setTextViewText(R.id.neo4_widget_detail, "")
        } else if (kind == Neo4WidgetKind.TODAY) {
            val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
            val shift = neoShiftOccurrences(db, visible).firstOrNull { now >= it.start && now < it.end }
                ?: neoShiftOccurrences(db, visible).firstOrNull { it.start.toLocalDate() == now.toLocalDate() && it.start > now }
            views.setTextViewText(R.id.neo4_widget_value, shift?.segment?.kind?.let { when(it) { ShiftKind.FULL_DAY -> "24h"; ShiftKind.LONG -> "Long"; ShiftKind.NIGHT -> "Night"; ShiftKind.MORNING -> "Morning"; ShiftKind.EVENING -> "Evening"; else -> "Off" } } ?: if (en) "No current or next shift today" else "لا يوجد شيفت جارٍ أو قادم اليوم")
            val time = shift?.let { it.start.format(DateTimeFormatter.ofPattern("HH:mm")) + " – " + it.end.format(DateTimeFormatter.ofPattern("HH:mm")) }.orEmpty()
            views.setTextViewText(R.id.neo4_widget_detail, time + if (options.showDetails && shift != null) " • " + shift.entry.date else "")
        } else {
            val security = V11SecurityStore(context).load()
            // Widget hosts are outside the authenticated app session. Never persist or reuse unlock state.
            if (!options.showAmounts || security.protectAccounts || security.hideAmounts || Neo4WidgetPrivacy.amountsHidden(context)) {
                views.setTextViewText(R.id.neo4_widget_value, "••••••")
                views.setTextViewText(R.id.neo4_widget_detail, if (en) "Open app to view securely" else "افتح التطبيق لعرض الحسابات بأمان")
            } else {
                val result = V11FinanceEngine.month(db, YearMonth.from(now))
                views.setTextViewText(R.id.neo4_widget_value, String.format(Locale.US, "%,.2f EGP", result.difference))
                views.setTextViewText(R.id.neo4_widget_detail, if (options.showDetails) (if (en) "Remaining • " else "المتبقي • ") + result.month else result.month)
            }
        }
        val intent = Intent(context, MainActivity::class.java).putExtra("neo4_open_tab", if (kind == Neo4WidgetKind.FINANCE) 2 else 0)
            .setData(android.net.Uri.parse("todoshift://neo4/${kind.name}/$id"))
        val open = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.neo4_widget_root, open)
        manager.updateAppWidget(id, views)
    }
}
abstract class Neo4SummaryWidgetProvider : AppWidgetProvider() {
    abstract val kind: Neo4WidgetKind
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { appWidgetIds.forEach { runCatching { Neo4WidgetRefresh.render(context.applicationContext, manager, it, kind) } } }
            finally { pending.finish() }
        }
    }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }
}
class Neo4TodayWidget : Neo4SummaryWidgetProvider() { override val kind = Neo4WidgetKind.TODAY }
class Neo4FinanceWidget : Neo4SummaryWidgetProvider() { override val kind = Neo4WidgetKind.FINANCE }
