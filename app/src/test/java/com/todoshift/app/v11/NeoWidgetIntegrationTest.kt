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
        // Widget rendering now executes off main; wait for its real click target,
        // rather than assuming the provider performed disk IO synchronously.
        val deadline = System.currentTimeMillis() + 5000L
        var clicked = false
        while (System.currentTimeMillis() < deadline && !clicked) {
            val add = runCatching { shadow.getViewFor(id) }.getOrNull()
                ?.findViewById<android.view.View>(R.id.tasks_widget_add)
            if (add?.hasOnClickListeners() == true) {
                clicked = add.performClick()
            } else {
                Thread.sleep(20)
            }
        }
        assertTrue("Async task widget must expose a functional quick-add button", clicked)
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
        assertNotEquals(main.taskAffinity, quick.taskAffinity)
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

    @Test
    fun titleWidgetProducerAndEntryParserAgreeOnTasks() {
        val c = RuntimeEnvironment.getApplication()
        val entry =
            com.todoshift.app.v11.ui.NeoEntry.routed(
                com.todoshift.app.widget.tasksWidgetEntry(c, "list"),
                1,
            )!!
        assertEquals(4, entry.tab)
    }

    @Test
    fun todayWidgetOpensCalendarAtActualOvernightShiftStartDate() {
        val c = RuntimeEnvironment.getApplication()
        val now = java.time.LocalDateTime.of(2026, 11, 11, 2, 0)
        val db =
            V11Database(
                calendars = listOf(AppCalendarV11("a", "A", "#00A4B8")),
                days =
                    listOf(
                        DayEntryV11(
                            "overnight",
                            "2026-11-10",
                            "a",
                            listOf(
                                ShiftSegmentV11(
                                    "night",
                                    ShiftKind.NIGHT,
                                    startMinute = 1260,
                                    endMinute = 1980,
                                )
                            ),
                        )
                    ),
            )
        val shift = com.todoshift.app.widget.NeoWidgetContentModel.todayShift(db, now)!!
        val intent =
            com.todoshift.app.widget.summaryWidgetEntry(
                c,
                com.todoshift.app.widget.Neo4WidgetKind.TODAY,
                12,
                shift.entry.date,
            )
        val entry = com.todoshift.app.v11.ui.NeoEntry.routed(intent, 1)!!
        assertEquals(0, entry.tab)
        assertEquals(java.time.LocalDate.of(2026, 11, 10), entry.date)
    }
}
