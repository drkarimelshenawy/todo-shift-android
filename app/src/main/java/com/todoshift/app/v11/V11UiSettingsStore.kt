package com.todoshift.app.v11

import android.content.Context

class V11UiSettingsStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "todo_shift_v11_ui",
            Context.MODE_PRIVATE
        )

    fun load(): V11UiSettings {

        val highlight =
            runCatching {
                TodayHighlightStyle.valueOf(
                    prefs.getString(
                        "todayHighlightStyle",
                        TodayHighlightStyle.BORDER_TINT.name
                    )!!
                )
            }.getOrDefault(
                TodayHighlightStyle.BORDER_TINT
            )

        val side =
            runCatching {
                FabSide.valueOf(
                    prefs.getString(
                        "fabSide",
                        FabSide.RIGHT.name
                    )!!
                )
            }.getOrDefault(FabSide.RIGHT)

        return V11UiSettings(
            todayHighlightStyle = highlight,
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
                )
        )
    }

    fun save(settings: V11UiSettings) {
        prefs.edit()
            .putString(
                "todayHighlightStyle",
                settings.todayHighlightStyle.name
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
            .apply()
    }
}
