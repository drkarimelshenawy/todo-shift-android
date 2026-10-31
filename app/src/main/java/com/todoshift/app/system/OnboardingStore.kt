package com.todoshift.app.system

import android.content.Context

class OnboardingStore(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            "todo_shift_onboarding",
            Context.MODE_PRIVATE
        )

    fun isCompleted(): Boolean =
        prefs.getBoolean(
            "completed",
            false
        )

    fun complete() {
        prefs.edit()
            .putBoolean(
                "completed",
                true
            )
            .apply()
    }

    fun reset() {
        prefs.edit()
            .putBoolean(
                "completed",
                false
            )
            .apply()
    }
}
