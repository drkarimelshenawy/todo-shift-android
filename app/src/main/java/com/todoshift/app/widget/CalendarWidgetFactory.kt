package com.todoshift.app.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.todoshift.app.R
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

class CalendarWidgetFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {
    private val widgetId = intent.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,-1)
    private var month = CalendarWidgetState.month(context,widgetId)
    private var dates = emptyList<LocalDate>()
    private var db = V11Database()
    private var colors = emptyMap<String,Int>()
    private var names = emptyMap<String,String>()
    private var indexed = emptyMap<String,List<ShiftSegmentV11>>()
    private var notes = emptyMap<String,List<String>>()
    private var appearance = NeoAppearance()
    private var options = Neo4WidgetOptions()
    private var skin = Neo4WidgetSkin(Color.WHITE,Color.BLACK,Color.GRAY,Color.BLUE)
    private var failed = false
    private var english = false
    private var rtl = false
    private var height = 54

    override fun onCreate() = reload()
    override fun onDataSetChanged() = reload()
    private fun reload() {
        failed = false
        month = CalendarWidgetState.month(context,widgetId)
        try {
            db = V11Runtime(context).start()
            val workplaces = AppStorage(context).load().workplaces
            colors = workplaces.associate { it.id to runCatching { Color.parseColor(it.colorHex) }.getOrDefault(Color.GRAY) }
            names = workplaces.associate { it.id to it.name }
            val settings = Neo4WidgetSettings(context)
            options = settings.load(Neo4WidgetKind.CALENDAR)
            skin = settings.skin(Neo4WidgetKind.CALENDAR)
            appearance = NeoAppearanceStore(context).load()
            english = settings.english()
            rtl = settings.calendarRtl(appearance)
            indexed = NeoWidgetContentModel.indexedSegments(db)
            val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
            val dayNotes = db.days.filter { it.calendarId in visible && it.note.isNotBlank() }.groupBy { it.date }
                .mapValues { (_,v) -> v.map { it.note } }
            notes = (db.dayItems.filter { it.type == DayItemTypeV11.NOTE && (it.calendarId == null || it.calendarId in visible) }
                .groupBy { it.date }.mapValues { (_,v) -> v.map { it.title } }.keys + dayNotes.keys)
                .associateWith { date -> dayNotes[date].orEmpty() + db.dayItems.filter {
                    it.date == date && it.type == DayItemTypeV11.NOTE && (it.calendarId == null || it.calendarId in visible)
                }.map { it.title } }
            dates = NeoMonthGrid.dates(month,appearance.firstWeekday)
            val size = runCatching { android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId) }.getOrDefault(android.os.Bundle())
            val available = size.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,350)
            height = ((available-86)/(dates.size/7)).coerceIn(28,100)
        } catch (_: Exception) {
            failed = true
            dates = NeoMonthGrid.dates(month,appearance.firstWeekday)
            indexed = emptyMap(); notes = emptyMap()
        }
    }
    override fun getCount() = dates.size
    override fun getViewAt(position: Int): RemoteViews {
        val v = RemoteViews(context.packageName,R.layout.widget_calendar_day)
        val date = dates.getOrNull(position) ?: return v
        v.setInt(R.id.day_container,"setMinimumHeight",(height * context.resources.displayMetrics.density).toInt())
        v.setInt(R.id.day_root,"setMinimumHeight",(height * context.resources.displayMetrics.density).toInt())
        if (failed) {
            v.setViewVisibility(R.id.day_paint,View.GONE)
            v.setTextViewText(R.id.day_number,if(position==0) "!" else "")
            v.setContentDescription(R.id.day_root,"Unable to read data; open ToDo Shift to recover safely")
            return v
        }
        val currentOptions = Neo4WidgetSettings(context).load(Neo4WidgetKind.CALENDAR)
        val muted = YearMonth.from(date)!=month
        val segments = if(currentOptions.hideContent) emptyList() else indexed[date.toString()].orEmpty()
        val shift = segments.map { names[it.workplaceId] ?: NeoWidgetContentModel.shiftLabel(it.kind,english) }.distinct().joinToString(" / ")
        val note = if(currentOptions.hideContent) "" else notes[date.toString()].orEmpty().joinToString(" • ")
        val ink = segments.firstOrNull()?.let {
            val paint = appearance.shiftPaints[it.kind]?.takeIf { s -> appearance.showShiftPaint && s.mode != NeoPaintMode.INHERIT }
            val background = paint?.first?.let { hex -> runCatching { Color.parseColor(hex) }.getOrNull() } ?: colors[it.workplaceId]
            background?.let { color -> if(Color.luminance(color)>.179) Color.BLACK else Color.WHITE }
        } ?: skin.text
        fun faded(color: Int) = if(muted) (color and 0x00ffffff) or (85 shl 24) else color
        v.setTextViewText(R.id.day_number,date.dayOfMonth.toString())
        v.setTextColor(R.id.day_number,faded(ink))
        v.setTextViewText(R.id.day_shift,shift)
        v.setTextColor(R.id.day_shift,faded(ink))
        v.setTextViewText(R.id.day_note,if(note.isBlank()) "" else "• $note")
        v.setTextColor(R.id.day_note,faded(ink))
        val showDetails = currentOptions.showDetails && !currentOptions.hideContent && height>=42
        v.setViewVisibility(R.id.day_shift,if(showDetails) View.VISIBLE else View.GONE)
        v.setViewVisibility(R.id.day_note,if(showDetails && height>=52) View.VISIBLE else View.GONE)
        v.setImageViewBitmap(R.id.day_paint,Neo4WidgetPainter.calendarCell(segments,colors,appearance,date,month,rtl,skin.accent))
        if (currentOptions.hideContent) v.setViewVisibility(R.id.day_paint,View.GONE)
        v.setContentDescription(R.id.day_root,"$date • $shift • $note")
        v.setOnClickFillInIntent(R.id.day_root,Intent().putExtra(ToDoShiftWidget.EXTRA_OPEN_DATE,date.toString())
            .setData(android.net.Uri.parse("todoshift://date/$date")))
        return v
    }
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount() = 1
    override fun getItemId(position: Int) = dates.getOrNull(position)?.toEpochDay() ?: position.toLong()
    override fun hasStableIds() = true
    override fun onDestroy() = Unit
}
