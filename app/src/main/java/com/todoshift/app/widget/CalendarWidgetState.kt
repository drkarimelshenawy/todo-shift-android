package com.todoshift.app.widget

import android.content.Context
import java.time.YearMonth

object CalendarWidgetState {

    private const val PREF =
        "todo_shift_widget_state"

    fun month(
        context: Context,
        widgetId: Int
    ): YearMonth {
        val prefs =
            context.getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
            )

        return runCatching {
            YearMonth.parse(
                prefs.getString(
                    "month_$widgetId",
                    null
                )
            )
        }.getOrNull()
            ?: YearMonth.now()
    }

    fun saveMonth(
        context: Context,
        widgetId: Int,
        month: YearMonth
    ) {
        context.getSharedPreferences(
            PREF,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                "month_$widgetId",
                month.toString()
            )
            .apply()
    }

    fun delete(
        context: Context,
        widgetId: Int
    ) {
        context.getSharedPreferences(
            PREF,
            Context.MODE_PRIVATE
        )
            .edit()
            .remove("month_$widgetId")
            .apply()
    }
}
