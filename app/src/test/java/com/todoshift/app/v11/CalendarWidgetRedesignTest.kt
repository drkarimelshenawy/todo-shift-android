package com.todoshift.app.v11

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.*
import com.todoshift.app.R
import com.todoshift.app.data.AppStorage
import com.todoshift.app.data.Workplace
import com.todoshift.app.widget.*
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarWidgetRedesignTest {
    @Test fun externalDaysKeepFaintPaintAndNumbersHaveNoOpaqueBackground() {
        val c=RuntimeEnvironment.getApplication()
        val runtime=V11Runtime(c);val initial=runtime.start();val calendar=initial.calendars.first().id
        val month=YearMonth.of(2026,10)
        val dates=NeoMonthGrid.dates(month,6)
        val outside=dates.first();val inside=month.atDay(10)
        val places=AppStorage(c);places.save(places.load().copy(workplaces=listOf(Workplace("w","مطامير","#20A464"))))
        val shifts=listOf(outside,inside).map { DayEntryV11(it.toString(),it.toString(),calendar,listOf(ShiftSegmentV11("s$it",ShiftKind.FULL_DAY,"w"))) }
        runtime.saveIfCurrent(initial,initial.copy(days=shifts,dayItems=listOf(DayItemV11("note",inside.toString(),type=DayItemTypeV11.NOTE,title="يوم الولادة"))))
        NeoAppearanceStore(c).save(NeoAppearance(language=NeoLanguage.ARABIC,firstWeekday=6))
        c.getSharedPreferences("todo_shift_neo4_widgets",0).edit().putString("CALENDAR","{\"showDetails\":true}").commit()
        CalendarWidgetState.saveMonth(c,42,month)
        android.appwidget.AppWidgetManager.getInstance(c).updateAppWidgetOptions(42,android.os.Bundle().apply {
            putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,430)
        })
        val factory=CalendarWidgetFactory(c,Intent().putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,42));factory.onCreate()
        assertEquals(dates.size,factory.getCount())
        val parent=FrameLayout(c)
        val view=factory.getViewAt(dates.indexOf(inside)).apply(c,parent)
        assertEquals("مطامير",view.findViewById<TextView>(R.id.day_shift).text.toString())
        assertTrue(view.findViewById<TextView>(R.id.day_note).text.contains("يوم الولادة"))
        assertNull(view.findViewById<TextView>(R.id.day_number).background)
        val segment=shifts.first().segments
        val a=Neo4WidgetPainter.calendarCell(segment,mapOf("w" to Color.rgb(32,164,100)),NeoAppearance(),outside,month,true,Color.BLUE)
        val b=Neo4WidgetPainter.calendarCell(segment,mapOf("w" to Color.rgb(32,164,100)),NeoAppearance(),inside,month,true,Color.BLUE)
        assertTrue(Color.alpha(a.getPixel(64,64)) in 1..70)
        assertEquals(255,Color.alpha(b.getPixel(64,64)))
        // Render actual RemoteViews cells in a static test grid for visual review.
        val panel=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL;setPadding(10,10,10,10);setBackgroundColor(Color.rgb(226,230,234)) }
        panel.addView(TextView(c).apply { text="أكتوبر 2026";textSize=20f;gravity=17 },LinearLayout.LayoutParams(392,38))
        val header=LinearLayout(c).apply { layoutDirection=View.LAYOUT_DIRECTION_RTL }
        listOf("سبت","أحد","اثن","ثلا","أرب","خمي","جمع").forEach { label -> header.addView(TextView(c).apply { text=label;gravity=17 },LinearLayout.LayoutParams(56,24)) };panel.addView(header)
        dates.chunked(7).forEachIndexed { row,_ ->
            val line=LinearLayout(c).apply { layoutDirection=View.LAYOUT_DIRECTION_RTL }
            repeat(7) { col ->line.addView(factory.getViewAt(row*7+col).apply(c,line),LinearLayout.LayoutParams(56,60)) }
            panel.addView(line)
        }
        panel.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(440,View.MeasureSpec.EXACTLY));panel.layout(0,0,412,440)
        val image=Bitmap.createBitmap(412,440,Bitmap.Config.ARGB_8888);panel.draw(Canvas(image))
        val out=File("build/reports/widget-preview/calendar-arabic.png");out.parentFile.mkdirs();out.outputStream().use { image.compress(Bitmap.CompressFormat.PNG,100,it) }
        c.getSharedPreferences("todo_shift_neo4_widgets",0).edit().putString("CALENDAR","{\"hideContent\":true}").commit()
        val hidden=factory.getViewAt(dates.indexOf(inside)).apply(c,parent)
        assertEquals(View.GONE,hidden.findViewById<View>(R.id.day_paint).visibility)
        assertEquals("",hidden.findViewById<TextView>(R.id.day_note).text.toString())
    }
}
