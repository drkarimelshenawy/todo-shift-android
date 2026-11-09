package com.todoshift.app.v11

import android.content.Context

class V11UiSettingsStore(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            "todo_shift_v11_ui",
            Context.MODE_PRIVATE
        )

    fun load(): V11UiSettings {

        val highlight =
            enumValue(
                key = "todayHighlightStyle",
                default =
                    TodayHighlightStyle.BORDER_TINT
            )

        val side =
            enumValue(
                key = "fabSide",
                default = FabSide.RIGHT
            )

        val appTheme =
            enumValue(
                key = "appTheme",
                default =
                    V11AppTheme.TODO_SHIFT
            )

        val calendarStyle =
            enumValue(
                key = "calendarStyle",
                default =
                    V11CalendarStyle.CLASSIC
            )

        val widgetTheme =
            enumValue(
                key = "widgetTheme",
                default =
                    V11WidgetTheme.FOLLOW_APP
            )

        return V11UiSettings(
            todayHighlightStyle =
                highlight,
            todayHighlightColor =
                prefs.getString(
                    "todayHighlightColor",
                    "#213555"
                ) ?: "#213555",
            fabSide = side,
            showWorkplaceNames =
                prefs.getBoolean(
                    "showWorkplaceNames",
                    false
                ),
            appTheme = appTheme,
            calendarStyle =
                calendarStyle,
            widgetTheme =
                widgetTheme,
            experience = enumValue("experience", V11Experience.CLASSIC),
            premiumBrightness = enumValue("premiumBrightness", V11PremiumBrightness.SYSTEM)
        )
    }

    fun save(
        settings: V11UiSettings
    ) {
        val editor = prefs.edit()
            .putString(
                "todayHighlightStyle",
                settings
                    .todayHighlightStyle
                    .name
            )
            .putString(
                "todayHighlightColor",
                settings.todayHighlightColor
            )
            .putString(
                "fabSide",
                settings.fabSide.name
            )
            .putBoolean(
                "showWorkplaceNames",
                settings.showWorkplaceNames
            )
            .putString(
                "appTheme",
                settings.appTheme.name
            )
            .putString(
                "calendarStyle",
                settings.calendarStyle.name
            )
            .putString(
                "widgetTheme",
                settings.widgetTheme.name
            )
            .putString("experience", settings.experience.name)
            .putString("premiumBrightness", settings.premiumBrightness.name)
        check(editor.commit()) { "Unable to save UI settings" }
    }

    private inline fun <
        reified T : Enum<T>
    > enumValue(
        key: String,
        default: T
    ): T =
        runCatching {
            enumValueOf<T>(
                prefs.getString(
                    key,
                    default.name
                ) ?: default.name
            )
        }.getOrDefault(default)
}
