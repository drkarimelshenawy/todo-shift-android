package com.todoshift.app.v11

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import com.todoshift.app.MainActivity
import com.todoshift.app.QuickTaskActivity
import com.todoshift.app.R
import com.todoshift.app.widget.TasksWidgetProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NeoWidgetIntegrationTest {
    @Test
    fun widgetPlusLaunchesDialogActivityWithoutChangingStoredRecords() {
        val c = RuntimeEnvironment.getApplication()
        val before = V11Runtime(c).start()
        val shadow = Shadows.shadowOf(AppWidgetManager.getInstance(c))
        val id = shadow.createWidget(TasksWidgetProvider::class.java, R.layout.widget_tasks)
        assertTrue(
            shadow
                .getViewFor(id)
                .findViewById<android.view.View>(R.id.tasks_widget_add)
                .performClick()
        )
        val intent = Shadows.shadowOf(c).nextStartedActivity
        assertEquals(QuickTaskActivity::class.java.name, intent.component!!.className)
        assertNotEquals(MainActivity::class.java.name, intent.component!!.className)
        assertEquals(before, V11Storage(c).load())
    }

    @Test
    fun quickTaskWindowAndMainLauncherKeepSeparateIdentities() {
        val c = RuntimeEnvironment.getApplication()
        val quick =
            c.packageManager.getActivityInfo(
                ComponentName(c, QuickTaskActivity::class.java),
                PackageManager.GET_META_DATA,
            )
        assertEquals(R.style.Theme_ToDoShift_QuickTask, quick.theme)
        val main =
            c.packageManager.getActivityInfo(
                ComponentName(c, MainActivity::class.java),
                PackageManager.GET_META_DATA,
            )
        assertTrue(main.metaData.getInt("android.app.shortcuts") != 0)
        assertTrue(quick.exported)
    }

    @Test
    fun widgetCompletionUsesTheConfirmedStoreAndPreservesUnrelatedTasks() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val task = TaskV11("widget-complete-fixture", "Fixture", date = "2026-11-10")
        val other = task.copy(id = "untouched-fixture")
        runtime.saveIfCurrent(before, before.copy(tasks = listOf(task, other)))
        val intent =
            android.content
                .Intent(c, TasksWidgetProvider::class.java)
                .setAction(TasksWidgetProvider.ACTION_COMPLETE)
                .putExtra(TasksWidgetProvider.EXTRA_TASK_ID, task.id)
        c.sendBroadcast(intent)
        val deadline = System.currentTimeMillis() + 10000
        while (
            System.currentTimeMillis() < deadline &&
                runtime.start().tasks.first { it.id == task.id }.status != TaskStatusV11.COMPLETED
        ) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertEquals(
            TaskStatusV11.COMPLETED,
            runtime.start().tasks.first { it.id == task.id }.status,
        )
        assertEquals(other, runtime.start().tasks.first { it.id == other.id })
    }
}
