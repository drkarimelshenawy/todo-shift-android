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
import com.todoshift.app.v11.ShiftSegmentV11
import com.todoshift.app.v11.V11Database
import com.todoshift.app.v11.V11Runtime
import com.todoshift.app.v11.V11AppTheme
import com.todoshift.app.v11.V11WidgetTheme
import com.todoshift.app.v11.V11UiSettingsStore
import java.time.LocalDate

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

    private var neo4Modern = false
    private var neo4Options = Neo4WidgetOptions()
    private var neo4Skin = Neo4WidgetSkin(Color.WHITE, Color.BLACK, Color.GRAY, Color.BLUE)
    private var neo4Appearance = com.todoshift.app.v11.NeoAppearance()
    private var neo4Entries = emptyMap<String, List<com.todoshift.app.v11.ShiftSegmentV11>>()

    private var month =
        CalendarWidgetState.month(
            context,
            appWidgetId
        )

    private var db =
        V11Database()

    private val cells =
        mutableListOf<WidgetDay>()

    private var workplaceColors:
        Map<String, Int> = emptyMap()

    private var widgetPrimaryText =
        Color.rgb(
            23,
            32,
            51
        )

    private var widgetSecondaryText =
        Color.rgb(
            71,
            85,
            105
        )

    /*
     * 12 fixed visual bands.
     * Every band represents 120 minutes.
     *
     * This gives deterministic RemoteViews rendering
     * across supported Android versions while keeping
     * actual shift start/end semantics.
     */
    private val bandSizeMinutes = 120

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
                V11Runtime(context)
                    .start()
            }.getOrElse {
                V11Database()
            }

        val neo4Settings = Neo4WidgetSettings(context)
        neo4Modern = neo4Settings.modern(Neo4WidgetKind.CALENDAR)
        neo4Options = neo4Settings.load(Neo4WidgetKind.CALENDAR)
        neo4Skin = neo4Settings.skin(Neo4WidgetKind.CALENDAR)
        neo4Appearance = com.todoshift.app.v11.NeoAppearanceStore(context).load()
        val neo4Ids = db.calendars.filter { it.visible }.map { it.id }.toSet()
        neo4Entries = db.days.filter { it.calendarId in neo4Ids }.groupBy { it.date }.mapValues { (_, entries) -> entries.flatMap { it.segments } }

        val uiSettings =
            V11UiSettingsStore(
                context.applicationContext
            ).load()

        val resolvedWidgetTheme =
            when (
                uiSettings.widgetTheme
            ) {
                V11WidgetTheme.FOLLOW_APP ->
                    if (
                        uiSettings.appTheme ==
                        V11AppTheme.MIDNIGHT
                    )
                        V11WidgetTheme.DARK
                    else
                        V11WidgetTheme.LIGHT

                else ->
                    uiSettings.widgetTheme
            }

        if (
            resolvedWidgetTheme ==
            V11WidgetTheme.DARK
        ) {
            widgetPrimaryText =
                Color.rgb(
                    230,
                    237,
                    243
                )
            widgetSecondaryText =
                Color.rgb(
                    170,
                    183,
                    196
                )
        } else {
            widgetPrimaryText =
                Color.rgb(
                    23,
                    32,
                    51
                )
            widgetSecondaryText =
                Color.rgb(
                    71,
                    85,
                    105
                )
        }

        /*
         * IMPORTANT:
         * Workplace owns the color.
         * Shift kind does not own a color.
         */
        workplaceColors =
            runCatching {
                AppStorage(context)
                    .load()
                    .workplaces
                    .associate { workplace ->

                        val color =
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

                        workplace.id to color
                    }
            }.getOrDefault(
                emptyMap()
            )

        cells.clear()

        val first =
            month.atDay(1)

        // Saturday = first column.
        val offset =
            (first.dayOfWeek.value + 1) % 7

        repeat(offset) {
            cells +=
                WidgetDay(
                    date = null,
                    label = "",
                    bands =
                        List(12) {
                            null
                        }
                )
        }

        for (
            day in
            1..month.lengthOfMonth()
        ) {

            val date =
                month.atDay(day)

            val entries =
                db.days.filter { entry ->

                    entry.date ==
                        date.toString() &&

                        db.calendars
                            .firstOrNull {
                                calendar ->
                                calendar.id ==
                                    entry.calendarId
                            }
                            ?.visible != false
                }

            val segments =
                entries
                    .flatMap {
                        it.segments
                    }

            val working =
                segments.filter {
                    it.kind !=
                        ShiftKind.OFF
                }

            val label =
                when {

                    segments.isEmpty() ->
                        ""

                    working.isEmpty() ->
                        "Off"

                    working.size == 1 ->
                        shortLabel(
                            working.first()
                                .kind
                        )

                    else ->
                        "${working.size} ش"
                }

            cells +=
                WidgetDay(
                    date = date,
                    label = label,
                    bands =
                        buildTimeBands(
                            working
                        )
                )
        }

        while (
            cells.size < 42
        ) {
            cells +=
                WidgetDay(
                    date = null,
                    label = "",
                    bands =
                        List(12) {
                            null
                        }
                )
        }
    }

    /*
     * Convert the real 24-hour timeline to
     * twelve 2-hour visual bands.
     *
     * A band takes the color of the shift
     * occupying the greatest number of minutes
     * inside that band.
     *
     * Therefore custom shift times still influence
     * the rendered result instead of using arbitrary
     * equal stripes.
     */
    private fun buildTimeBands(
        segments:
            List<ShiftSegmentV11>
    ): List<Int?> {

        return List(12) {
            bandIndex ->

            val bandStart =
                bandIndex *
                    bandSizeMinutes

            val bandEnd =
                bandStart +
                    bandSizeMinutes

            segments
                .mapNotNull {
                    segment ->

                    val range =
                        segment.timeRange()
                            ?: return@mapNotNull null

                    val color =
                        workplaceColors[
                            segment.workplaceId
                        ]
                            ?: return@mapNotNull null

                    val overlapStart =
                        maxOf(
                            bandStart,
                            range.startMinute
                        )

                    val overlapEnd =
                        minOf(
                            bandEnd,
                            range.endMinute
                        )

                    val overlap =
                        overlapEnd -
                            overlapStart

                    if (overlap <= 0) {
                        null
                    } else {
                        BandCandidate(
                            color = color,
                            overlapMinutes =
                                overlap,
                            segmentStart =
                                range.startMinute
                        )
                    }
                }
                /*
                 * Largest occupancy wins.
                 * Earlier start is deterministic
                 * tie-breaker.
                 */
                .sortedWith(
                    compareByDescending<
                        BandCandidate
                    > {
                        it.overlapMinutes
                    }.thenBy {
                        it.segmentStart
                    }
                )
                .firstOrNull()
                ?.color
        }
    }

    private fun shortLabel(
        kind: ShiftKind
    ): String =
        if (neo4Modern) when(kind) {
            ShiftKind.FULL_DAY -> "24h"
            ShiftKind.LONG -> "Long"
            ShiftKind.NIGHT -> "Night"
            ShiftKind.MORNING -> "Morning"
            ShiftKind.EVENING -> "Evening"
            ShiftKind.OFF -> "Off"
        } else when (kind) {

            ShiftKind.FULL_DAY ->
                "24س"

            ShiftKind.LONG ->
                "Long"

            ShiftKind.NIGHT ->
                "Night"

            ShiftKind.MORNING ->
                "ص"

            ShiftKind.EVENING ->
                "م"

            ShiftKind.OFF ->
                "Off"
        }

    override fun getCount():
        Int =
        cells.size

    override fun getViewAt(
        position: Int
    ): RemoteViews {

        val cell =
            cells.getOrNull(
                position
            ) ?: WidgetDay(
                date = null,
                label = "",
                bands =
                    List(12) {
                        null
                    }
            )

        val views =
            RemoteViews(
                context.packageName,
                R.layout
                    .widget_calendar_day
            )

        resetCell(
            views
        )

        views.setViewVisibility(R.id.day_paint, android.view.View.GONE)
        val date =
            cell.date
                ?: return views

        views.setTextViewText(
            R.id.day_number,
            date.dayOfMonth
                .toString()
        )

        views.setTextViewText(
            R.id.day_shift,
            cell.label
        )

        if (
            date ==
            LocalDate.now()
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

        applyTimeBands(
            views,
            cell.bands
        )

        if (neo4Modern) {
            val options = neo4Options
            val skin = neo4Skin
            views.setTextColor(R.id.day_number, skin.text)
            views.setInt(R.id.day_number, "setBackgroundColor", skin.background)
            views.setViewVisibility(R.id.day_timeline, android.view.View.GONE)
            views.setViewVisibility(R.id.day_shift, if (options.showDetails && !options.hideContent) android.view.View.VISIBLE else android.view.View.GONE)
            views.setTextColor(R.id.day_shift, skin.secondary)
            if (!options.hideContent) {
                val segments = neo4Entries[date.toString()].orEmpty()
                views.setImageViewBitmap(R.id.day_paint, Neo4WidgetPainter.render(segments, workplaceColors, neo4Appearance))
                views.setViewVisibility(R.id.day_paint, android.view.View.VISIBLE)
            }
        }

        val fill =
            Intent().apply {

                putExtra(
                    ToDoShiftWidget
                        .EXTRA_OPEN_DATE,
                    date.toString()
                )

                data =
                    android.net.Uri
                        .parse(
                            "todoshift://date/$date"
                        )
            }

        views.setOnClickFillInIntent(
            R.id.day_root,
            fill
        )

        return views
    }

    private fun bandIds():
        List<Int> =
        listOf(
            R.id.time_band_01,
            R.id.time_band_02,
            R.id.time_band_03,
            R.id.time_band_04,
            R.id.time_band_05,
            R.id.time_band_06,
            R.id.time_band_07,
            R.id.time_band_08,
            R.id.time_band_09,
            R.id.time_band_10,
            R.id.time_band_11,
            R.id.time_band_12
        )

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
            android.R.color
                .transparent
        )

        views.setTextColor(
            R.id.day_number,
            widgetPrimaryText
        )

        views.setTextColor(
            R.id.day_shift,
            widgetSecondaryText
        )

        bandIds()
            .forEach {
                id ->

                /*
                 * INVISIBLE keeps every time band's
                 * geometry. GONE would collapse time.
                 */
                views.setViewVisibility(
                    id,
                    View.INVISIBLE
                )

                views.setInt(
                    id,
                    "setBackgroundColor",
                    Color.TRANSPARENT
                )
            }
    }

    private fun applyTimeBands(
        views: RemoteViews,
        bands: List<Int?>
    ) {

        val ids =
            bandIds()

        ids.forEachIndexed {
            index,
            id ->

            val color =
                bands
                    .getOrNull(
                        index
                    )

            if (
                color == null
            ) {
                views.setViewVisibility(
                    id,
                    View.INVISIBLE
                )

                views.setInt(
                    id,
                    "setBackgroundColor",
                    Color.TRANSPARENT
                )
            } else {
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
    }

    override fun getLoadingView():
        RemoteViews? =
        null

    override fun getViewTypeCount():
        Int =
        1

    override fun getItemId(
        position: Int
    ): Long {

        val date =
            cells
                .getOrNull(
                    position
                )
                ?.date

        return date
            ?.toEpochDay()
            ?: position.toLong()
    }

    override fun hasStableIds():
        Boolean =
        true

    override fun onDestroy() =
        Unit

    data class WidgetDay(
        val date: LocalDate?,
        val label: String,
        val bands: List<Int?>
    )

    private data class BandCandidate(
        val color: Int,
        val overlapMinutes: Int,
        val segmentStart: Int
    )
}
