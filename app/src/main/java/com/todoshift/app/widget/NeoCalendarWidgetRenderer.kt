package com.todoshift.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.todoshift.app.MainActivity
import com.todoshift.app.R
import com.todoshift.app.data.AppStorage
import com.todoshift.app.v11.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** One paint surface and weighted RemoteViews rows: shared edges cannot drift per cell. IO only. */
object NeoCalendarWidgetRenderer {
    fun build(context: Context, id: Int, month: YearMonth, db: V11Database?, width: Int, height: Int): RemoteViews {
        val settings=Neo4WidgetSettings(context);val options=settings.load(Neo4WidgetKind.CALENDAR)
        val p=NeoAppearanceStore(context).load();val skin=settings.skin(Neo4WidgetKind.CALENDAR)
        val english=settings.english();val locale=if(english) Locale.ENGLISH else Locale.forLanguageTag("ar")
        val rtl=settings.calendarRtl(p);val direction=if(rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        val views=RemoteViews(context.packageName,R.layout.widget_calendar_v2)
        views.setImageViewBitmap(R.id.widget_panel,Neo4WidgetPainter.panel(skin.background,skin.accent,options.backgroundOpacity,(width*2).coerceIn(280,640),(height*2).coerceIn(280,900)))
        views.setTextViewText(R.id.widget_title,month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy",locale)))
        listOf(R.id.widget_title,R.id.widget_prev,R.id.widget_next).forEach { views.setTextColor(it,skin.text) }
        views.setViewVisibility(R.id.widget_summary,if(db==null) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_summary,if(english) "Unable to read data; open app" else "تعذر قراءة البيانات؛ افتح التطبيق")
        views.setTextColor(R.id.widget_summary,skin.text)
        val hidden=options.hideContent || db==null
        views.setViewVisibility(R.id.widget_grid,if(hidden) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_month_paint,if(hidden) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_weekdays,if(hidden) View.GONE else View.VISIBLE)
        val weekdayIds=listOf(R.id.widget_weekday_0,R.id.widget_weekday_1,R.id.widget_weekday_2,R.id.widget_weekday_3,R.id.widget_weekday_4,R.id.widget_weekday_5,R.id.widget_weekday_6)
        val ar=listOf("اثن","ثلا","أرب","خمي","جمع","سبت","أحد")
        views.setInt(R.id.widget_weekdays,"setLayoutDirection",direction)
        weekdayIds.forEachIndexed { index,res ->
            val day=(p.firstWeekday-1+index)%7+1
            views.setTextViewText(res,if(english) java.time.DayOfWeek.of(day).getDisplayName(TextStyle.SHORT,locale) else ar[day-1]);views.setTextColor(res,skin.text)
        }
        fun pending(intent: Intent, request: Int)=PendingIntent.getActivity(context,request,intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.widget_title,pending(Intent(context,MainActivity::class.java).setData(Uri.parse("todoshift://widget/$id/open")),id))
        listOf(R.id.widget_prev to ToDoShiftWidget.ACTION_PREV,R.id.widget_next to ToDoShiftWidget.ACTION_NEXT).forEach { (res,action) ->
            val intent=Intent(context,ToDoShiftWidget::class.java).setAction(action).setData(Uri.parse("todoshift://widget/$id/$action")).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id)
            views.setOnClickPendingIntent(res,PendingIntent.getBroadcast(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        views.removeAllViews(R.id.widget_grid)
        if(!hidden && db!=null) {
            val places=AppStorage(context).load().workplaces
            val colors=places.associate { it.id to runCatching { Color.parseColor(it.colorHex) }.getOrDefault(Color.GRAY) }
            val dates=NeoMonthGrid.dates(month,p.firstWeekday);val rows=dates.size/7
            val gridHeight=(height-80).coerceAtLeast(rows*22);val cellHeight=gridHeight/rows
            views.setImageViewBitmap(R.id.widget_month_paint,Neo4WidgetPainter.monthGrid(db,colors,p,month,rtl,skin.accent,(width*2).coerceIn(280,640),(gridHeight*2).coerceIn(200,900)))
            val indexed=NeoWidgetContentModel.indexedSegments(db)
            val visible=db.calendars.filter { it.visible }.map { it.id }.toSet()
            dates.chunked(7).forEach { week ->
                val row=RemoteViews(context.packageName,R.layout.widget_calendar_row)
                row.setInt(R.id.widget_week_row,"setLayoutDirection",direction)
                week.forEach { date ->
                    val cell=RemoteViews(context.packageName,R.layout.widget_calendar_cell)
                    val segments=indexed[date.toString()].orEmpty()
                    val names=segments.map { s -> places.firstOrNull { it.id==s.workplaceId }?.name ?: NeoWidgetContentModel.shiftLabel(s.kind,english) }.distinct().joinToString(" / ")
                    val notes=(db.days.filter { it.date==date.toString() && it.calendarId in visible }.map { it.note } +
                        db.dayItems.filter { it.date==date.toString() && it.type==DayItemTypeV11.NOTE && (it.calendarId==null || it.calendarId in visible) }.map { it.title }).filter { it.isNotBlank() }.joinToString(" • ")
                    val ink=segments.firstOrNull()?.let { segment ->
                        val custom=p.shiftPaints[segment.kind]?.takeIf { p.showShiftPaint && it.mode!=NeoPaintMode.INHERIT }?.first
                        val background=custom?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: colors[segment.workplaceId]
                        background?.let { if(Color.luminance(it)>.179) Color.BLACK else Color.WHITE }
                    } ?: skin.text
                    val text=if(YearMonth.from(date)==month) ink else (ink and 0xffffff) or (95 shl 24)
                    cell.setTextViewText(R.id.day_number,date.dayOfMonth.toString());cell.setTextColor(R.id.day_number,text)
                    cell.setTextViewText(R.id.day_shift,names);cell.setTextColor(R.id.day_shift,text)
                    cell.setTextViewText(R.id.day_note,if(notes.isEmpty()) "" else "• $notes");cell.setTextColor(R.id.day_note,text)
                    cell.setViewVisibility(R.id.day_shift,if(options.showDetails && cellHeight>=38) View.VISIBLE else View.GONE)
                    cell.setViewVisibility(R.id.day_note,if(options.showDetails && cellHeight>=65) View.VISIBLE else View.GONE)
                    cell.setInt(R.id.day_shift,"setMaxLines",if(cellHeight>=65 && notes.isEmpty()) 2 else 1)
                    cell.setInt(R.id.day_note,"setMaxLines",if(cellHeight>=85) 2 else 1)
                    cell.setContentDescription(R.id.day_root,"$date • $names • $notes")
                    val intent=Intent(context,MainActivity::class.java).setAction("com.todoshift.app.OPEN_DATE").setData(Uri.parse("todoshift://widget/$id/date/$date")).putExtra(ToDoShiftWidget.EXTRA_OPEN_DATE,date.toString())
                    cell.setOnClickPendingIntent(R.id.day_root,pending(intent,id))
                    row.addView(R.id.widget_week_row,cell)
                }
                views.addView(R.id.widget_grid,row)
            }
        }
        return views
    }
}
