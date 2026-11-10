package com.todoshift.app.v11

/** UI-only preferences. No finance, task, calendar or backup schema is changed. */
enum class NeoDirection {
    AUTO,
    RTL,
    LTR,
}

enum class NeoTransition {
    FADE,
    SLIDE,
    FLIP,
    ZOOM,
    STACK,
    VERTICAL,
}

enum class NeoSpeed {
    SLOW,
    NORMAL,
    FAST,
}

enum class NeoPerformance {
    AUTO,
    NORMAL,
    LIGHTWEIGHT,
}

enum class NeoTodayStyle {
    NONE,
    CIRCLE,
    GLOW,
    PULSE,
    BACKGROUND,
    UNDERLINE,
    DOTS,
    LABEL,
}

enum class NeoCellSize {
    COMPACT,
    COMFORTABLE,
    LARGE,
}

enum class NeoBrightness {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED,
}

enum class NeoPalette {
    MINT_AURA,
    OCEAN_DEPTH,
    LAVENDER_GLOW,
    PEACH_BLOOM,
    EMERALD_NIGHT,
    ARCTIC_BLUE,
    GOLDEN_HOUR,
    ROSE_MIST,
    MIDNIGHT_TEAL,
    SUNSET_CORAL,
    FOREST_SAGE,
    GRAPHITE_BLUE,
    NEO_CYAN,
    ROYAL_VIOLET,
    MIDNIGHT_BLUE,
    EMERALD_LUXURY,
    CHAMPAGNE_GOLD,
    TITANIUM_SILVER,
    ROSE_QUARTZ,
    ARCTIC_WHITE,
    SUNSET_COPPER,
    MONOCHROME,
}

enum class NeoCardStyle {
    LAYERED,
    OUTLINED,
    FLAT,
}

enum class NeoDensity {
    COMPACT,
    BALANCED,
    COMFORTABLE,
}

enum class NeoTypeface {
    SYSTEM,
    GEOMETRIC,
    EDITORIAL,
}

enum class NeoIconStyle {
    ROUNDED,
    OUTLINED,
}

enum class NeoCalendarView {
    MONTH,
    WEEK,
    DAY,
}

enum class NeoLanguage {
    SYSTEM,
    ARABIC,
    ENGLISH,
}

enum class NeoDashboardCard {
    CURRENT_SHIFT,
    NEXT_SHIFT,
    SUMMARY,
    TASKS,
    NEXT_EVENT,
}

enum class NeoQuickAction {
    ADD_SHIFT,
    ADD_TASK,
    TODAY,
}

enum class NeoTimelineFilter {
    ALL,
    CURRENT,
    UPCOMING,
    PAST,
}

enum class NeoBackground {
    GRADIENT,
    IMAGE,
    CLASSIC,
}

enum class NeoImageTheme {
    SUNSET_BEACH,
    CLOUD_PARADISE,
    FOREST_MIST,
    AURORA_NIGHT,
    LAVENDER_SKY,
    GOLDEN_SUNSET,
    PERSONAL,
}

enum class NeoLayoutPreset {
    MINIMAL,
    BALANCED,
    RICH,
}

enum class NeoPaintMode {
    INHERIT,
    SOLID,
    GRADIENT,
}

enum class NeoGradientDirection {
    VERTICAL,
    HORIZONTAL,
    DIAGONAL,
}

enum class NeoGradientStrength {
    SOFT,
    MEDIUM,
    STRONG,
}

data class NeoShiftPaint(
    val mode: NeoPaintMode = NeoPaintMode.INHERIT,
    val first: String = "#5BC8B2",
    val second: String = "#4D8AE8",
    val direction: NeoGradientDirection = NeoGradientDirection.VERTICAL,
    val strength: NeoGradientStrength = NeoGradientStrength.MEDIUM,
)

data class NeoAppearance(
    val brightness: NeoBrightness = NeoBrightness.SYSTEM,
    val palette: NeoPalette = NeoPalette.CHAMPAGNE_GOLD,
    val accentHex: String = "",
    val intensity: Float = 0.65f,
    val cardStyle: NeoCardStyle = NeoCardStyle.FLAT,
    val cornerRadius: Int = 12,
    val glass: Boolean = false,
    val glow: Boolean = false,
    val motion: Int = 1,
    val reduceMotion: Boolean = false,
    val density: NeoDensity = NeoDensity.COMPACT,
    val fontScale: Float = 1f,
    val typeface: NeoTypeface = NeoTypeface.SYSTEM,
    val iconStyle: NeoIconStyle = NeoIconStyle.ROUNDED,
    val calendarView: NeoCalendarView = NeoCalendarView.MONTH,
    val calendarDensity: NeoDensity = NeoDensity.BALANCED,
    val language: NeoLanguage = NeoLanguage.SYSTEM,
    val dashboardExpanded: Boolean = false,
    val dashboardOrder: List<NeoDashboardCard> = NeoDashboardCard.entries.toList(),
    val hiddenCards: Set<NeoDashboardCard> = emptySet(),
    val quickActions: Set<NeoQuickAction> = NeoQuickAction.entries.toSet(),
    val showShiftNotes: Boolean = true,
    val timelineFilter: NeoTimelineFilter = NeoTimelineFilter.ALL,
    val showFinanceChart: Boolean = false,
    val background: NeoBackground = NeoBackground.CLASSIC,
    val imageTheme: NeoImageTheme = NeoImageTheme.SUNSET_BEACH,
    val customImageFile: String = "",
    val imagePositionX: Float = 0.5f,
    val imagePositionY: Float = 0.5f,
    val imageDim: Float = 0.5f,
    val layoutPreset: NeoLayoutPreset = NeoLayoutPreset.MINIMAL,
    val showGreeting: Boolean = false,
    val showDailySummary: Boolean = false,
    val showNextShift: Boolean = false,
    val showTaskSummary: Boolean = false,
    val showFinanceSummary: Boolean = false,
    val showSecondaryCards: Boolean = false,
    val showEffects: Boolean = false,
    val showOptionalInfo: Boolean = false,
    val showSecondaryIndicators: Boolean = false,
    val showDescriptions: Boolean = false,
    val showShiftPaint: Boolean = true,
    val showCalendarLegend: Boolean = false,
    val showBrand: Boolean = true,
    val cardOpacity: Float = 0.92f,
    val glassStrength: Float = 0.2f,
    val optionalTransitions: Boolean = false,
    val shiftPaints: Map<ShiftKind, NeoShiftPaint> = emptyMap(),
    val uiDirection: NeoDirection = NeoDirection.AUTO,
    val navigationDirection: NeoDirection = NeoDirection.AUTO,
    val gridDirection: NeoDirection = NeoDirection.AUTO,
    val weekdayDirection: NeoDirection = NeoDirection.AUTO,
    val firstWeekday: Int = 6,
    val transition: NeoTransition = NeoTransition.FADE,
    val transitionSpeed: NeoSpeed = NeoSpeed.NORMAL,
    val performance: NeoPerformance = NeoPerformance.AUTO,
    val showNotes: Boolean = true,
    val showNoteIcons: Boolean = true,
    val showShiftNames: Boolean = false,
    val showTaskIndicators: Boolean = true,
    val todayStyle: NeoTodayStyle = NeoTodayStyle.CIRCLE,
    val todayColor: String = "#D8AD55",
    val todayIntensity: Float = 0.6f,
    val todaySpeed: NeoSpeed = NeoSpeed.NORMAL,
    val cellSize: NeoCellSize = NeoCellSize.COMFORTABLE,
    val showOutsideDates: Boolean = true,
    val showTomorrow: Boolean = false,
) {
    val motionMillis: Int
        get() =
            if (
                reduceMotion ||
                    performance == NeoPerformance.LIGHTWEIGHT ||
                    !optionalTransitions ||
                    motion == 0
            )
                0
            else
                (when (transitionSpeed) {
                    NeoSpeed.SLOW -> 440
                    NeoSpeed.NORMAL -> 260
                    NeoSpeed.FAST -> 140
                }) * (if (motion == 1) 3 else 4) / 4
}

fun NeoAppearance.withPreset(preset: NeoLayoutPreset): NeoAppearance =
    copy(
        layoutPreset = preset,
        showGreeting = preset != NeoLayoutPreset.MINIMAL,
        showDailySummary = preset != NeoLayoutPreset.MINIMAL,
        showNextShift = preset != NeoLayoutPreset.MINIMAL,
        showTaskSummary = preset == NeoLayoutPreset.RICH,
        showFinanceSummary = preset == NeoLayoutPreset.RICH,
        showSecondaryCards = preset == NeoLayoutPreset.RICH,
        showOptionalInfo = preset == NeoLayoutPreset.RICH,
        showSecondaryIndicators = preset != NeoLayoutPreset.MINIMAL,
        showDescriptions = preset == NeoLayoutPreset.RICH,
        showCalendarLegend = preset == NeoLayoutPreset.RICH,
        dashboardExpanded = false,
    )

fun NeoAppearance.cardVisible(card: NeoDashboardCard): Boolean =
    card !in hiddenCards &&
        (when (card) {
            NeoDashboardCard.CURRENT_SHIFT,
            NeoDashboardCard.SUMMARY -> showDailySummary
            NeoDashboardCard.NEXT_SHIFT -> showNextShift
            NeoDashboardCard.TASKS -> showTaskSummary
            NeoDashboardCard.NEXT_EVENT -> showOptionalInfo
        })

fun NeoAppearance.withDashboardCard(card: NeoDashboardCard, visible: Boolean): NeoAppearance {
    val current = NeoDashboardCard.entries.filter { cardVisible(it) }.toSet()
    val next = if (visible) current + card else current - card
    return copy(
        hiddenCards = NeoDashboardCard.entries.filter { it !in next }.toSet(),
        showDailySummary =
            NeoDashboardCard.CURRENT_SHIFT in next || NeoDashboardCard.SUMMARY in next,
        showNextShift = NeoDashboardCard.NEXT_SHIFT in next,
        showTaskSummary = NeoDashboardCard.TASKS in next,
        showOptionalInfo = NeoDashboardCard.NEXT_EVENT in next,
    )
}

/** Visual identities never mutate the independent language/direction configuration. */
fun NeoAppearance.preservingLocaleOf(current: NeoAppearance): NeoAppearance =
    copy(
        language = current.language,
        uiDirection = current.uiDirection,
        navigationDirection = current.navigationDirection,
        gridDirection = current.gridDirection,
        weekdayDirection = current.weekdayDirection,
        firstWeekday = current.firstWeekday,
    )

/** Today pulse is independent of page transitions; accessibility/runtime reduction still wins. */
fun neoTodayPulseEnabled(p: NeoAppearance, runtimeReduced: Boolean, focus: Boolean): Boolean =
    p.todayStyle == NeoTodayStyle.PULSE &&
        !p.reduceMotion &&
        p.motion > 0 &&
        p.performance != NeoPerformance.LIGHTWEIGHT &&
        !runtimeReduced &&
        !focus
