package com.todoshift.app.v11

import com.todoshift.app.reminders.ReminderPolicy
import com.todoshift.app.widget.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.ZonedDateTime
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class ReminderDeliveryPolicyTest {
    @Test fun quietHoursKeepOriginalRecognitionTime() {
        val context=RuntimeEnvironment.getApplication()
        NeoSmartStore(context).update {it.put("quietHours",true)}
        val late=ZonedDateTime.parse("2026-11-14T23:15:00Z").toInstant().toEpochMilli()
        assertEquals(late,NeoNotificationPolicy.deliveryTime(context,late))
        NeoSmartStore(context).update {it.put("notificationsEnabled",false)}
        assertFalse(NeoNotificationPolicy.enabled(context,"tasks"))
    }
    @Test fun missedMonthlyReminderPreservesMonthEndAnchorAndSkipsOldOccurrences() {
        val previous=TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val context=RuntimeEnvironment.getApplication()
            val policy=ReminderPolicy(context)
            policy.configure("end-of-month","MONTHLY")
            fun ms(value:String)=ZonedDateTime.parse(value).toInstant().toEpochMilli()
            val original=ms("2026-01-31T10:00:00Z")
            assertEquals(ms("2026-03-31T10:00:00Z"),policy.next("end-of-month",original,now=ms("2026-03-01T00:00:00Z")))
            assertEquals(ms("2027-01-31T10:00:00Z"),policy.next("end-of-month",original,now=ms("2026-12-31T12:00:00Z")))
        } finally {TimeZone.setDefault(previous)}
    }
    @Test fun separateWidgetInstancesKeepPrivacySettingsWhenPresetChanges() {
        val context=RuntimeEnvironment.getApplication()
        val first=Neo4WidgetSettings(context,101)
        val second=Neo4WidgetSettings(context,102)
        first.initialize(Neo4WidgetKind.TASKS)
        second.initialize(Neo4WidgetKind.TASKS)
        first.save(Neo4WidgetKind.TASKS,Neo4WidgetOptions(hideContent=true,backgroundOpacity=.2f))
        Neo4WidgetSettings(context).save(Neo4WidgetKind.TASKS,Neo4WidgetOptions(showDetails=true))
        assertTrue(first.load(Neo4WidgetKind.TASKS).hideContent)
        assertFalse(second.load(Neo4WidgetKind.TASKS).showDetails)
        assertEquals(.2f,first.load(Neo4WidgetKind.TASKS).backgroundOpacity,0.001f)
    }
}
