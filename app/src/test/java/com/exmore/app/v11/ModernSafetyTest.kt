package com.exmore.app.v11

import com.exmore.app.backup.Neo4LocalBackup
import com.exmore.app.backup.createNeoSafetyCopy
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ModernSafetyTest {
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun rebootRescheduleDoesNotReplayMissedQuietHourReminder() {
        val now = System.currentTimeMillis()
        val original = now - 30 * 60 * 1000
        assertTrue(now > original)
        NeoSmartStore(context).update { it.put("quietHours", true).put("notify_tasks", true) }
        val task =
            TaskV11(
                "quiet-reboot-fixture",
                "Synthetic reminder",
                date = "2026-11-10",
                reminderAt = original,
            )
        com.exmore.app.reminders.ToDoShiftReminder.rescheduleAll(
            context,
            V11Database(tasks = listOf(task)),
            nowMillis = now,
        )
        val manager =
            context.getSystemService(android.content.Context.ALARM_SERVICE)
                as android.app.AlarmManager
        val alarm =
            org.robolectric.Shadows.shadowOf(manager).scheduledAlarms.firstOrNull {
                org.robolectric.Shadows.shadowOf(it.operation)
                    .savedIntent
                    .getStringExtra("reminder_id") == task.id
            }
        assertNull(alarm)
        assertEquals(original, NeoNotificationPolicy.deliveryTime(context, original))
    }

    @Test
    fun beforeChangeCopyIsPreviewableAndDoesNotChangeCurrentData() {
        val runtime = V11Runtime(context)
        val db = runtime.start()
        val name = createNeoSafetyCopy(context, db)
        val copy = File(context.filesDir, "safety-copies/$name")
        assertTrue(copy.isFile)
        val prepared = Neo4LocalBackup(context).preview(com.exmore.app.backup.openNeoSafetyCopy(copy))
        assertTrue(prepared.token.isNotEmpty())
        assertEquals(db, V11Storage(context).load())
    }

    @Test
    fun staleSafetyCopyCannotAuthorizeAChange() {
        val runtime = V11Runtime(context)
        val old = runtime.start()
        val next =
            old.copy(tasks = old.tasks + TaskV11("fixture", "Synthetic", date = "2026-11-10"))
        runtime.saveIfCurrent(old, next)
        assertThrows(Exception::class.java) { createNeoSafetyCopy(context, old) }
        assertEquals(next, V11Storage(context).load())
    }

    @Test
    fun quietHoursKeepScheduledTimeStoredReminderAndPerformancePreference() {
        NeoSmartStore(context).update { it.put("quietHours", true).put("notify_tasks", true) }
        NeoAppearanceStore(context).save(NeoAppearance(performance = NeoPerformance.LIGHTWEIGHT))
        val original =
            LocalDateTime.of(2026, 11, 10, 23, 30)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val task = TaskV11("fixture", "Synthetic", date = "2026-11-10", reminderAt = original)
        val db = V11Runtime(context).start()
        V11Runtime(context).saveIfCurrent(db, db.copy(tasks = listOf(task)))
        val delivered =
            java.time.Instant.ofEpochMilli(NeoNotificationPolicy.deliveryTime(context, original))
                .atZone(ZoneId.systemDefault())
        assertEquals(10, delivered.dayOfMonth)
        assertEquals(23, delivered.hour)
        assertEquals(30, delivered.minute)
        assertTrue(NeoNotificationPolicy.quiet(context,original))
        assertEquals(NeoPerformance.LIGHTWEIGHT, NeoAppearanceStore(context).load().performance)
        assertTrue(NeoNotificationPolicy.enabled(context, "tasks"))
        assertEquals(original, V11Storage(context).load().tasks.single().reminderAt)
    }
}
