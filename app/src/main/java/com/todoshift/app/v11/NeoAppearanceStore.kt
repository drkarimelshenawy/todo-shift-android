package com.todoshift.app.v11

import android.content.Context

/** Separate additive UI preference file; never writes user records or V11 security settings. */
class NeoAppearanceStore(context: Context) {
    private val prefs = context.getSharedPreferences("todo_shift_neo3_appearance", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("todo_shift_v11_ui", Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T =
        runCatching { enumValueOf<T>(prefs.getString(key, default.name) ?: default.name) }.getOrDefault(default)

    fun load(): NeoAppearance {
        val oldBrightness = when (legacy.getString("premiumBrightness", "SYSTEM")) {
            "DARK" -> NeoBrightness.DARK
            "LIGHT" -> NeoBrightness.LIGHT
            else -> NeoBrightness.SYSTEM
        }
        val oldPalette = when (legacy.getString("appTheme", "TODO_SHIFT")) {
            "MIDNIGHT" -> NeoPalette.ROYAL_VIOLET
            "OCEAN" -> NeoPalette.MIDNIGHT_BLUE
            "FOREST" -> NeoPalette.EMERALD_LUXURY
            "SAND" -> NeoPalette.CHAMPAGNE_GOLD
            "MONO" -> NeoPalette.MONOCHROME
            else -> NeoPalette.MINT_AURA
        }
        val order = prefs.getString("dashboardOrder", "")!!.split(",").mapNotNull { name ->
            runCatching { enumValueOf<NeoDashboardCard>(name) }.getOrNull()
        }.distinct()
        val hidden = prefs.getStringSet("hiddenCards", emptySet()).orEmpty()
        val actions = prefs.getStringSet("quickActions", NeoQuickAction.entries.map { it.name }.toSet()).orEmpty()
        val legacyValue = NeoAppearance(
            brightness = readEnum("brightness", oldBrightness), palette = readEnum("palette", oldPalette),
            accentHex = prefs.getString("accentHex", "").orEmpty().takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) }.orEmpty(),
            intensity = prefs.getFloat("intensity", 0.65f).takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.65f,
            cardStyle = readEnum("cardStyle", NeoCardStyle.LAYERED), cornerRadius = prefs.getInt("cornerRadius", 22).coerceIn(8, 32),
            glass = prefs.getBoolean("glass", true), glow = prefs.getBoolean("glow", true),
            motion = prefs.getInt("motion", 1).coerceIn(0, 2), reduceMotion = prefs.getBoolean("reduceMotion", false),
            density = readEnum("density", NeoDensity.BALANCED), fontScale = prefs.getFloat("fontScale", 1f).takeIf { it.isFinite() }?.coerceIn(0.85f, 1.3f) ?: 1f,
            typeface = readEnum("typeface", NeoTypeface.SYSTEM), iconStyle = readEnum("iconStyle", NeoIconStyle.ROUNDED),
            calendarView = readEnum("calendarView", NeoCalendarView.MONTH), calendarDensity = readEnum("calendarDensity", NeoDensity.BALANCED),
            language = readEnum("language", NeoLanguage.SYSTEM), dashboardExpanded = prefs.getBoolean("dashboardExpanded", false),
            dashboardOrder = order + NeoDashboardCard.entries.filterNot { it in order },
            hiddenCards = NeoDashboardCard.entries.filter { it.name in hidden }.toSet(),
            quickActions = NeoQuickAction.entries.filter { it.name in actions }.toSet(),
            showShiftNotes = prefs.getBoolean("showShiftNotes", true), timelineFilter = readEnum("timelineFilter", NeoTimelineFilter.ALL),
            showFinanceChart = prefs.getBoolean("showFinanceChart", false)
        )
        return runCatching { prefs.getString("neo4Appearance", null)?.let { Neo4AppearanceCodec.decode(org.json.JSONObject(it), legacyValue) } ?: legacyValue }.getOrDefault(legacyValue)
    }

    fun save(value: NeoAppearance) {
        prefs.edit().apply {
            putString("neo4Appearance", Neo4AppearanceCodec.encode(value).toString())
            putString("brightness", value.brightness.name); putString("palette", value.palette.name)
            putString("accentHex", value.accentHex); putFloat("intensity", value.intensity)
            putString("cardStyle", value.cardStyle.name); putInt("cornerRadius", value.cornerRadius)
            putBoolean("glass", value.glass); putBoolean("glow", value.glow)
            putInt("motion", value.motion); putBoolean("reduceMotion", value.reduceMotion)
            putString("density", value.density.name); putFloat("fontScale", value.fontScale)
            putString("typeface", value.typeface.name); putString("iconStyle", value.iconStyle.name)
            putString("calendarView", value.calendarView.name); putString("calendarDensity", value.calendarDensity.name)
            putString("language", value.language.name); putBoolean("dashboardExpanded", value.dashboardExpanded)
            putString("dashboardOrder", value.dashboardOrder.joinToString(",") { it.name })
            putStringSet("hiddenCards", value.hiddenCards.map { it.name }.toSet())
            putStringSet("quickActions", value.quickActions.map { it.name }.toSet())
            putBoolean("showShiftNotes", value.showShiftNotes); putString("timelineFilter", value.timelineFilter.name)
            putBoolean("showFinanceChart", value.showFinanceChart)
        }.apply()
    }
}
