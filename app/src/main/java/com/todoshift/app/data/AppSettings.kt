package com.todoshift.app.data

import android.content.Context

data class UserSettings(
    val currency: String = "ج",
    val weekStartsSaturday: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val darkMode: Boolean = false
)

class SettingsStore(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            "todo_shift_settings",
            Context.MODE_PRIVATE
        )

    fun load(): UserSettings =
        UserSettings(
            currency =
                prefs.getString(
                    "currency",
                    "ج"
                ) ?: "ج",

            weekStartsSaturday =
                prefs.getBoolean(
                    "week_starts_saturday",
                    true
                ),

            notificationsEnabled =
                prefs.getBoolean(
                    "notifications_enabled",
                    true
                ),

            darkMode =
                prefs.getBoolean(
                    "dark_mode",
                    false
                )
        )

    fun save(
        settings: UserSettings
    ) {
        prefs.edit()
            .putString(
                "currency",
                settings.currency
            )
            .putBoolean(
                "week_starts_saturday",
                settings.weekStartsSaturday
            )
            .putBoolean(
                "notifications_enabled",
                settings.notificationsEnabled
            )
            .putBoolean(
                "dark_mode",
                settings.darkMode
            )
            .apply()
    }
}
