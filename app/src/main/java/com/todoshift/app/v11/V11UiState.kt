package com.todoshift.app.v11

enum class TodayHighlightStyle {
    BORDER,
    TINT,
    RING,
    PULSE,
    BORDER_TINT,
    NONE
}

enum class FabSide {
    LEFT,
    RIGHT
}

enum class V11AppTheme {
    TODO_SHIFT,
    MIDNIGHT,
    OCEAN,
    FOREST,
    SAND,
    MONO
}

enum class V11CalendarStyle {
    CLASSIC,
    COMPACT,
    CARDS
}

enum class V11WidgetTheme {
    FOLLOW_APP,
    LIGHT,
    DARK,
    TRANSPARENT
}

data class V11UiSettings(
    val todayHighlightStyle: TodayHighlightStyle =
        TodayHighlightStyle.BORDER_TINT,
    val todayHighlightColor: String = "#213555",
    val fabSide: FabSide = FabSide.RIGHT,
    val showWorkplaceNames: Boolean = false,
    val appTheme: V11AppTheme =
        V11AppTheme.TODO_SHIFT,
    val calendarStyle: V11CalendarStyle =
        V11CalendarStyle.CLASSIC,
    val widgetTheme: V11WidgetTheme =
        V11WidgetTheme.FOLLOW_APP
)
