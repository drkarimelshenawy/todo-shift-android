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

data class V11UiSettings(
    val todayHighlightStyle: TodayHighlightStyle =
        TodayHighlightStyle.BORDER_TINT,
    val todayHighlightColor: String = "#213555",
    val fabSide: FabSide = FabSide.RIGHT,
    val showWorkplaceNames: Boolean = false
)
