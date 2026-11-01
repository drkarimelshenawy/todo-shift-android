package com.todoshift.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.todoshift.app.R
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.ShiftKind
import com.todoshift.app.v11.V11Database
import com.todoshift.app.v11.V11Runtime
import java.time.LocalDate
import java.time.YearMonth

class CalendarWidgetFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId =
        intent.getIntExtra(
            android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,
            -1
        )

    private var month =
        CalendarWidgetState.month(context, appWidgetId)

    private var db = V11Database()

    private val cells =
        mutableListOf<WidgetDay>()

    private var workplaceColors:
        Map<String, Int> = emptyMap()

    override fun onCreate() {
        reload()
    }

    override fun onDataSetChanged() {
        reload()
    }

    private fun reload() {

        month =
            CalendarWidgetState.month(
                context,
                appWidgetId
            )

        db =
            runCatching {
                V11Runtime(context).start()
            }.getOrElse {
                V11Database()
            }

        /*
         * Workplaces still live in the legacy AppStorage.
         * Their color is the visual identity of a workplace.
         * Shift type itself has NO color.
         */
        workplaceColors =
            runCatching {
                AppStorage(context)
                    .load()
                    .workplaces
                    .associate { workplace ->

                        val parsed =
                            runCatching {
                                Color.parseColor(
                                    workplace.colorHex
                                )
                            }.getOrDefault(
                                Color.rgb(
                                    100,
                                    116,
                                    139
                                )
                            )

                        workplace.id to parsed
                    }
            }.getOrDefault(
                emptyMap()
            )

        cells.clear()

        val first =
            month.atDay(1)

        // Saturday is first column.
        val offset =
            (first.dayOfWeek.value + 2) % 7

        repeat(offset) {
            cells += WidgetDay(
                date = null,
                label = "",
                colors = emptyList()
            )
        }

        for (
            day in 1..month.lengthOfMonth()
        ) {

            val date =
                month.atDay(day)

            val entries =
                db.days.filter { entry ->

                    entry.date ==
                        date.toString() &&

                    db.calendars
                        .firstOrNull { calendar ->
                            calendar.id ==
                                entry.calendarId
                        }
                        ?.visible != false
                }

            val segments =
                entries.flatMap {
                    it.segments
                }

            val workingSegments =
                segments.filter {
                    it.kind != ShiftKind.OFF
                }

            val label =
                when {

                    segments.isEmpty() ->
                        ""

                    workingSegments.isEmpty() ->
                        "Off"

                    workingSegments.size == 1 ->
                        shortLabel(
                            workingSegments
                                .first()
                                .kind
                        )

                    else ->
                        "${workingSegments.size} ش"
                }

            /*
             * One stripe per segment/workplace.
             * Maximum 4 stripes in compact widget cell.
             *
             * We intentionally DO NOT distinct() the colors:
             * two shifts at the same workplace remain two
             * independent segments.
             */
            val colors =
                workingSegments
                    .mapNotNull {
                        workplaceColors[
                            it.workplaceId
                        ]
                    }
                    .take(4)

            cells += WidgetDay(
                date = date,
                label = label,
                colors = colors
            )
        }

        while (cells.size < 42) {
            cells += WidgetDay(
                date = null,
                label = "",
                colors = emptyList()
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
                ?: WidgetDay(
                    null,
                    "",
                    emptyList()
                )

        val views =
            RemoteViews(
                context.packageName,
                R.layout.widget_calendar_day
            )

        resetCell(views)

        val date =
            cell.date

        if (date == null) {
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

        if (
            date == LocalDate.now()
        ) {
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
        }

        applyColors(
            views,
            cell.colors
        )

        val fill =
            Intent().apply {

                putExtra(
                    ToDoShiftWidget
                        .EXTRA_OPEN_DATE,
                    date.toString()
                )

                data =
                    android.net.Uri.parse(
                        "todoshift://date/$date"
                    )
            }

        views.setOnClickFillInIntent(
            R.id.day_root,
            fill
        )

        return views
    }

    private fun resetCell(
        views: RemoteViews
    ) {

        views.setTextViewText(
            R.id.day_number,
            ""
        )

        views.setTextViewText(
            R.id.day_shift,
            ""
        )

        views.setInt(
            R.id.day_number,
            "setBackgroundResource",
            android.R.color.transparent
        )

        listOf(
            R.id.day_color_1,
            R.id.day_color_2,
            R.id.day_color_3,
            R.id.day_color_4
        ).forEach {

            views.setViewVisibility(
                it,
                View.GONE
            )

            views.setInt(
                it,
                "setBackgroundColor",
                Color.TRANSPARENT
            )
        }
    }

    private fun applyColors(
        views: RemoteViews,
        colors: List<Int>
    ) {

        val ids =
            listOf(
                R.id.day_color_1,
                R.id.day_color_2,
                R.id.day_color_3,
                R.id.day_color_4
            )

        colors
            .take(ids.size)
            .forEachIndexed {
                index,
                color ->

                val id =
                    ids[index]

                views.setViewVisibility(
                    id,
                    View.VISIBLE
                )

                views.setInt(
                    id,
                    "setBackgroundColor",
                    color
                )
            }
    }

    override fun getLoadingView():
        RemoteViews? = null

    override fun getViewTypeCount(): Int =
        1

    override fun getItemId(
        position: Int
    ): Long {

        val date =
            cells
                .getOrNull(position)
                ?.date

        return date
            ?.toEpochDay()
            ?: position.toLong()
    }

    override fun hasStableIds(): Boolean =
        true

    override fun onDestroy() =
        Unit

    data class WidgetDay(
        val date: LocalDate?,
        val label: String,
        val colors: List<Int>
    )
}
