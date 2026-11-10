package com.todoshift.app.v11

import android.app.Notification
import android.app.NotificationManager
import com.todoshift.app.reminders.ToDoShiftReminder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/** Notification model check only; OS/launcher lock-screen policy requires physical acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReminderPublicVersionTest {
    @Test
    fun publicVersionContainsNoPrivateTitleDetailsOrDate() {
        val c = RuntimeEnvironment.getApplication()
        ToDoShiftReminder.show(
            c,
            "synthetic-private",
            "Private fixture title",
            "Private fixture details",
            "2026-11-10",
        )
        val notifications =
            Shadows.shadowOf(c.getSystemService(NotificationManager::class.java)).allNotifications
        val posted = notifications.single()
        assertEquals(Notification.VISIBILITY_PRIVATE, posted.visibility)
        assertEquals("Private fixture title", posted.extras.getString(Notification.EXTRA_TITLE))
        val public = requireNotNull(posted.publicVersion)
        assertFalse(public.extras.toString().contains("Private fixture"))
        assertFalse(public.extras.toString().contains("2026-11-10"))
        assertNotNull(public.contentIntent)
    }
}
