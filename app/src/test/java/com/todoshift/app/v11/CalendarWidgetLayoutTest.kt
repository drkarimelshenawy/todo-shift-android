package com.todoshift.app.v11

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.todoshift.app.R
import com.todoshift.app.data.AppStorage
import com.todoshift.app.data.Workplace
import com.todoshift.app.widget.*
import java.io.File
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS-w420dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarWidgetLayoutTest {
    @Test fun productionRemoteViewsFillHeightWithEqualRowsAcrossSizesMonthsAndLanguages() {
        val c=RuntimeEnvironment.getApplication();val old=AppStorage(c);old.save(old.load().copy(workplaces=listOf(Workplace("w","الشاملة المطامير","#22A466"))))
        val months=listOf(YearMonth.of(2021,2),YearMonth.of(2026,4),YearMonth.of(2026,8))
        for(language in listOf(NeoLanguage.ENGLISH,NeoLanguage.ARABIC)) for(month in months) for(size in listOf(220 to 240,400 to 520)) {
            NeoAppearanceStore(c).save(NeoAppearance(language=language,firstWeekday=1,gridDirection=if(language==NeoLanguage.ARABIC) NeoDirection.RTL else NeoDirection.LTR))
            val date=month.atDay(2).toString()
            val db=V11Database(calendars=listOf(AppCalendarV11("c","Work","#22A466")),days=listOf(DayEntryV11("d",date,"c",listOf(ShiftSegmentV11("s",ShiftKind.FULL_DAY,"w")))),dayItems=listOf(DayItemV11("note",date,type=DayItemTypeV11.NOTE,title="يوم الولادة")))
            val rv=NeoCalendarWidgetRenderer.build(c,13,month,db,size.first,size.second)
            assertEquals(R.layout.widget_calendar_v2,rv.layoutId)
            val view=rv.apply(c,FrameLayout(c))
            view.measure(View.MeasureSpec.makeMeasureSpec(size.first,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size.second,View.MeasureSpec.EXACTLY));view.layout(0,0,size.first,size.second)
            val grid=view.findViewById<LinearLayout>(R.id.widget_grid)
            val rows=NeoMonthGrid.dates(month,1).size/7
            assertEquals(rows,grid.childCount)
            assertTrue(grid.height>0)
            val heights=(0 until rows).map {grid.getChildAt(it).height}
            assertTrue(heights.max()-heights.min()<=1)
            assertEquals(grid.height,heights.sum())
            for(row in 0 until rows) assertEquals(7,(grid.getChildAt(row) as LinearLayout).childCount)
            val bitmap=Bitmap.createBitmap(size.first,size.second,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
            val file=File("build/reports/widget-preview/${month}-${language}-${size.first}.png");file.parentFile.mkdirs();file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    @Test fun opacityOnlyChangesPanelNotTextAndLiveHiddenRowsExposeNoLabels() {
        val c=RuntimeEnvironment.getApplication();val settings=Neo4WidgetSettings(c)
        val options=Neo4WidgetOptions(backgroundOpacity=.2f,hideContent=true)
        settings.save(Neo4WidgetKind.CALENDAR,options)
        assertEquals(.2f,Neo4WidgetSettings(c).load(Neo4WidgetKind.CALENDAR).backgroundOpacity,.001f)
        val a=Neo4WidgetPainter.panel(Color.WHITE,Color.BLACK,.2f)
        val b=Neo4WidgetPainter.panel(Color.WHITE,Color.BLACK,1f)
        assertTrue(Color.alpha(a.getPixel(120,120))<Color.alpha(b.getPixel(120,120)))
        val db=V11Database(tasks=listOf(TaskV11("private","Hidden fixture",date="2026-11-12")))
        val view=NeoCalendarWidgetRenderer.build(c,12,YearMonth.of(2026,4),db,300,400).apply(c,FrameLayout(c))
        assertEquals(View.GONE,view.findViewById<View>(R.id.widget_grid).visibility)
        assertEquals(0,view.findViewById<LinearLayout>(R.id.widget_grid).childCount)
    }
}
