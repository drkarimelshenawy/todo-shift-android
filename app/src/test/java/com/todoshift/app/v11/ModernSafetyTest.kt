package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4LocalBackup
import com.todoshift.app.backup.createNeoSafetyCopy
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
    fun beforeChangeCopyIsPreviewableAndDoesNotChangeCurrentData() {
        val runtime = V11Runtime(context)
        val db = runtime.start()
        val name = createNeoSafetyCopy(context, db)
        val copy = File(context.filesDir, "safety-copies/$name")
        assertTrue(copy.isFile)
        val prepared = Neo4LocalBackup(context).preview(copy.inputStream())
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
    fun quietHoursDeferDeliveryWithoutChangingStoredReminderOrPerformancePreference() {
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
        assertEquals(11, delivered.dayOfMonth)
        assertEquals(7, delivered.hour)
        assertTrue(NeoNotificationPolicy.enabled(context, "tasks"))
        assertEquals(original, V11Storage(context).load().tasks.single().reminderAt)
    }
}
