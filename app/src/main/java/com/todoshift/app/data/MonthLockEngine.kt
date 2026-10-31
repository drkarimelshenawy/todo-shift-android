package com.todoshift.app.data

import android.content.Context
import java.time.YearMonth

class MonthLockEngine(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            "todo_shift_month_locks",
            Context.MODE_PRIVATE
        )

    fun isLocked(
        month: YearMonth
    ): Boolean =
        prefs.getBoolean(
            month.toString(),
            false
        )

    fun setLocked(
        month: YearMonth,
        locked: Boolean
    ) {
        prefs.edit()
            .putBoolean(
                month.toString(),
                locked
            )
            .apply()
    }
}
