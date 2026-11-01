package com.todoshift.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.todoshift.app.R
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

class CalendarWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId =
        intent.getIntExtra(
            android.appwidget.AppWidgetManager
                .EXTRA_APPWIDGET_ID,
            -1
        )

    private var month: YearMonth =
        CalendarWidgetState.month(
            context,
            appWidgetId
        )

    private var db: V11Database =
        V11Database()

    private val cells =
        mutableListOf<WidgetDay>()

    override fun onCreate() {
        reload()
    }

    override fun onDataSetChanged() {
        reload()
    }

    private fun reload() {
        month = CalendarWidgetState.month(
            context,
            appWidgetId
        )

        db = runCatching {
            V11Runtime(context).start()
        }.getOrElse {
            V11Database()
        }

        cells.clear()

        val first = month.atDay(1)

        // Saturday = first column.
        val offset =
            (first.dayOfWeek.value + 2) % 7

        repeat(offset) {
            cells += WidgetDay(
                date = null,
                label = ""
            )
        }

        for (
            day in 1..month.lengthOfMonth()
        ) {
            val date = month.atDay(day)

            val entries = db.days.filter {
                it.date == date.toString() &&
                    db.calendars
                        .firstOrNull { c ->
                            c.id == it.calendarId
                        }
                        ?.visible != false
            }

            val segments =
                entries.flatMap { it.segments }

            val label =
                when {
                    segments.isEmpty() -> ""

                    segments.size == 1 ->
                        shortLabel(
                            segments.first().kind
                        )

                    else ->
                        "${segments.size} ش"
                }

            cells += WidgetDay(
                date = date,
                label = label
            )
        }

        while (cells.size < 42) {
            cells += WidgetDay(
                date = null,
                label = ""
            )
        }
    }

    private fun shortLabel(
        kind: ShiftKind
    ): String =
        when (kind) {
            ShiftKind.FULL_DAY -> "24س"
            ShiftKind.LONG -> "Long"
            ShiftKind.NIGHT -> "Night"
            ShiftKind.MORNING -> "ص"
            ShiftKind.EVENING -> "م"
            ShiftKind.OFF -> "Off"
        }

    override fun getCount(): Int =
        cells.size

    override fun getViewAt(
        position: Int
    ): RemoteViews {
        val cell =
            cells.getOrNull(position)
                ?: WidgetDay(null, "")

        val views = RemoteViews(
            context.packageName,
            R.layout.widget_calendar_day
        )

        val date = cell.date

        if (date == null) {
            views.setTextViewText(
                R.id.day_number,
                ""
            )
            views.setTextViewText(
                R.id.day_shift,
                ""
            )
            return views
        }

        views.setTextViewText(
            R.id.day_number,
            date.dayOfMonth.toString()
        )

        views.setTextViewText(
            R.id.day_shift,
            cell.label
        )

        if (date == LocalDate.now()) {
            views.setInt(
                R.id.day_number,
                "setBackgroundResource",
                R.drawable
                    .widget_today_background
            )
            views.setTextColor(
                R.id.day_number,
                Color.rgb(
                    13,
                    35,
                    68
                )
            )
        } else {
            views.setInt(
                R.id.day_number,
                "setBackgroundResource",
                android.R.color.transparent
            )
        }

        val fill = Intent().apply {
            putExtra(
                ToDoShiftWidget
                    .EXTRA_OPEN_DATE,
                date.toString()
            )
        }

        views.setOnClickFillInIntent(
            R.id.day_root,
            fill
        )

        return views
    }

    override fun getLoadingView():
        RemoteViews? = null

    override fun getViewTypeCount(): Int =
        1

    override fun getItemId(
        position: Int
    ): Long =
        position.toLong()

    override fun hasStableIds(): Boolean =
        true

    override fun onDestroy() = Unit

    data class WidgetDay(
        val date: LocalDate?,
        val label: String
    )
}
