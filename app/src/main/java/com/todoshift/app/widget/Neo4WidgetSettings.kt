package com.todoshift.app.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import com.todoshift.app.v11.*
import org.json.JSONObject

enum class Neo4WidgetKind { CALENDAR, TODAY, TASKS, FINANCE }
enum class Neo4WidgetTheme { FOLLOW_APP, LIGHT, DARK, TRANSPARENT }
data class Neo4WidgetOptions(val theme: Neo4WidgetTheme = Neo4WidgetTheme.FOLLOW_APP, val accent: String = "",
    val showDetails: Boolean = false, val hideContent: Boolean = false, val showAmounts: Boolean = false)
class Neo4WidgetSettings(private val context: Context) {
    private val prefs = context.getSharedPreferences("todo_shift_neo4_widgets", Context.MODE_PRIVATE)
    fun explicit(kind: Neo4WidgetKind) = prefs.contains(kind.name)
    fun load(kind: Neo4WidgetKind): Neo4WidgetOptions = runCatching {
        val o = JSONObject(prefs.getString(kind.name, "{}") ?: "{}")
        val legacy = V11UiSettingsStore(context).load().widgetTheme
        val defaultTheme = when(legacy) { V11WidgetTheme.DARK -> Neo4WidgetTheme.DARK; V11WidgetTheme.LIGHT -> Neo4WidgetTheme.LIGHT; V11WidgetTheme.TRANSPARENT -> Neo4WidgetTheme.TRANSPARENT; else -> Neo4WidgetTheme.FOLLOW_APP }
        Neo4WidgetOptions(runCatching { Neo4WidgetTheme.valueOf(o.optString("theme", defaultTheme.name)) }.getOrDefault(defaultTheme),
            o.optString("accent", "").takeIf { it.isEmpty() || it.matches(Regex("#[0-9a-fA-F]{6}")) }.orEmpty(),
            o.optBoolean("showDetails", false), o.optBoolean("hideContent", false), o.optBoolean("showAmounts", false))
    }.getOrDefault(Neo4WidgetOptions())
    fun save(kind: Neo4WidgetKind, value: Neo4WidgetOptions) {
        val editor = prefs.edit().putString(kind.name, JSONObject().put("theme", value.theme.name).put("accent", value.accent)
            .put("showDetails", value.showDetails).put("hideContent", value.hideContent).put("showAmounts", value.showAmounts).toString())
        check(editor.commit()) { "Unable to save widget settings" }
        Neo4WidgetRefresh.schedule(context)
    }
    fun skin(kind: Neo4WidgetKind): Neo4WidgetSkin {
        val p = load(kind); val appearance = NeoAppearanceStore(context).load()
        val dark = when(p.theme) {
            Neo4WidgetTheme.LIGHT -> false; Neo4WidgetTheme.DARK -> true
            else -> when(appearance.brightness) { NeoBrightness.DARK, NeoBrightness.AMOLED -> true; NeoBrightness.LIGHT -> false; else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES }
        }
        val inherited = appearance.accentHex.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) }
        val rawAccent = if (p.accent.isNotEmpty()) Color.parseColor(p.accent) else inherited?.let { Color.parseColor(it) } ?: neo4AccentArgb(appearance.palette).toInt()
        val fraction = if (dark && Color.luminance(rawAccent) < 0.25f) 0.65f else if (!dark && Color.luminance(rawAccent) > 0.2f) 0.6f else 0f
        val target = if (dark) 255 else 0
        val accent = Color.rgb((Color.red(rawAccent) * (1 - fraction) + target * fraction).toInt(), (Color.green(rawAccent) * (1 - fraction) + target * fraction).toInt(), (Color.blue(rawAccent) * (1 - fraction) + target * fraction).toInt())
        return Neo4WidgetSkin(if (p.theme == Neo4WidgetTheme.TRANSPARENT) Color.TRANSPARENT else Color.parseColor(if (dark) "#111D29" else "#EDF5F2"),
            Color.parseColor(if (dark) "#EFF6FC" else "#17303C"), Color.parseColor(if (dark) "#B8C9D7" else "#4B6570"), accent)
    }
    fun modern(kind: Neo4WidgetKind) = explicit(kind) || V11UiSettingsStore(context).load().experience == V11Experience.PREMIUM
    fun english(): Boolean = when(NeoAppearanceStore(context).load().language) { NeoLanguage.ARABIC -> false; NeoLanguage.ENGLISH -> true; else -> context.resources.configuration.locales[0].language != "ar" }
}
data class Neo4WidgetSkin(val background: Int, val text: Int, val secondary: Int, val accent: Int) {
    fun apply(views: RemoteViews, root: Int, vararg textIds: Int) {
        views.setInt(root, "setBackgroundColor", background)
        textIds.forEach { views.setTextColor(it, text) }
        textIds.firstOrNull()?.let { views.setTextColor(it, accent) }
    }
}

object Neo4WidgetPrivacy {
    fun amountsHidden(context: Context): Boolean = context.getSharedPreferences("todo_shift_neo4_widget_privacy", Context.MODE_PRIVATE).getBoolean("amountsHidden", true)
    fun update(context: Context, hidden: Boolean) {
        context.getSharedPreferences("todo_shift_neo4_widget_privacy", Context.MODE_PRIVATE).edit().putBoolean("amountsHidden", hidden).apply()
        if (hidden) {
            val manager = android.appwidget.AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(android.content.ComponentName(context, Neo4FinanceWidget::class.java)).forEach { id ->
                val partial = RemoteViews(context.packageName, com.todoshift.app.R.layout.neo4_widget_summary)
                partial.setTextViewText(com.todoshift.app.R.id.neo4_widget_value, "••••••")
                partial.setTextViewText(com.todoshift.app.R.id.neo4_widget_detail, "")
                manager.partiallyUpdateAppWidget(id, partial)
            }
        }
        Neo4WidgetRefresh.schedule(context)
    }
}
