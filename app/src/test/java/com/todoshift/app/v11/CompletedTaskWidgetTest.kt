package com.todoshift.app.v11

import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.todoshift.app.R
import com.todoshift.app.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class CompletedTaskWidgetTest {
    @Test fun completedRowsAreStruckThroughAndCanBeHiddenWithoutDeletingRecords() {
        val c=RuntimeEnvironment.getApplication();val runtime=V11Runtime(c);val old=runtime.start()
        val task=TaskV11("completed","Completed widget fixture",date="2026-11-12",status=TaskStatusV11.COMPLETED)
        runtime.saveIfCurrent(old,old.copy(tasks=listOf(task)))
        val factory=TasksWidgetFactory(c,Intent());factory.onCreate();factory.onDataSetChanged()
        assertEquals(1,factory.getCount())
        val view=factory.getViewAt(0)!!.apply(c,FrameLayout(c))
        assertTrue(view.findViewById<TextView>(R.id.task_widget_name).paintFlags and Paint.STRIKE_THRU_TEXT_FLAG != 0)
        assertEquals("✓",view.findViewById<TextView>(R.id.task_widget_check).text.toString())
        Neo4WidgetSettings(c).save(Neo4WidgetKind.TASKS,Neo4WidgetOptions(showCompleted=false))
        factory.onDataSetChanged();assertEquals(0,factory.getCount())
        assertEquals(listOf(task),runtime.start().tasks)
    }
}
