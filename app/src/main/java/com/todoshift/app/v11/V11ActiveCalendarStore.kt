package com.todoshift.app.v11

import android.content.Context

class V11ActiveCalendarStore(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            "todo_shift_v11_ui_state",
            Context.MODE_PRIVATE
        )

    fun load(): String? =
        prefs.getString(
            KEY_ACTIVE_CALENDAR,
            null
        )

    fun save(calendarId: String?) {
        val editor = prefs.edit().apply {
            if (calendarId == null) {
                remove(KEY_ACTIVE_CALENDAR)
            } else {
                putString(
                    KEY_ACTIVE_CALENDAR,
                    calendarId
                )
            }
        }
        check(editor.commit()) { "Unable to save primary calendar" }
    }

    companion object {
        private const val KEY_ACTIVE_CALENDAR =
            "active_calendar_id"
    }
}
