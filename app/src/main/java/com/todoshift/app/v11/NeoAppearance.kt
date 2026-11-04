package com.todoshift.app.v11

/** UI-only preferences. No finance, task, calendar or backup schema is changed. */
enum class NeoBrightness { SYSTEM, DARK, LIGHT, AMOLED }
enum class NeoPalette { NEO_CYAN, ROYAL_VIOLET, MIDNIGHT_BLUE, EMERALD_LUXURY, CHAMPAGNE_GOLD, TITANIUM_SILVER, ROSE_QUARTZ, ARCTIC_WHITE, SUNSET_COPPER, MONOCHROME }
enum class NeoCardStyle { LAYERED, OUTLINED, FLAT }
enum class NeoDensity { COMPACT, BALANCED, COMFORTABLE }
enum class NeoTypeface { SYSTEM, GEOMETRIC, EDITORIAL }
enum class NeoIconStyle { ROUNDED, OUTLINED }
enum class NeoCalendarView { MONTH, WEEK, DAY }
enum class NeoLanguage { SYSTEM, ARABIC, ENGLISH }
enum class NeoDashboardCard { CURRENT_SHIFT, NEXT_SHIFT, SUMMARY, TASKS, NEXT_EVENT }
enum class NeoQuickAction { ADD_SHIFT, ADD_TASK, TODAY }
enum class NeoTimelineFilter { ALL, CURRENT, UPCOMING, PAST }

data class NeoAppearance(
    val brightness: NeoBrightness = NeoBrightness.SYSTEM,
    val palette: NeoPalette = NeoPalette.NEO_CYAN,
    val accentHex: String = "",
    val intensity: Float = 0.65f,
    val cardStyle: NeoCardStyle = NeoCardStyle.LAYERED,
    val cornerRadius: Int = 22,
    val glass: Boolean = true,
    val glow: Boolean = true,
    val motion: Int = 1,
    val reduceMotion: Boolean = false,
    val density: NeoDensity = NeoDensity.BALANCED,
    val fontScale: Float = 1f,
    val typeface: NeoTypeface = NeoTypeface.SYSTEM,
    val iconStyle: NeoIconStyle = NeoIconStyle.ROUNDED,
    val calendarView: NeoCalendarView = NeoCalendarView.MONTH,
    val calendarDensity: NeoDensity = NeoDensity.BALANCED,
    val language: NeoLanguage = NeoLanguage.SYSTEM,
    val dashboardExpanded: Boolean = true,
    val dashboardOrder: List<NeoDashboardCard> = NeoDashboardCard.entries.toList(),
    val hiddenCards: Set<NeoDashboardCard> = emptySet(),
    val quickActions: Set<NeoQuickAction> = NeoQuickAction.entries.toSet(),
    val showShiftNotes: Boolean = true,
    val timelineFilter: NeoTimelineFilter = NeoTimelineFilter.ALL,
    val showFinanceChart: Boolean = true
) {
    val motionMillis: Int get() = if (reduceMotion || motion == 0) 0 else if (motion == 1) 150 else 260
}
