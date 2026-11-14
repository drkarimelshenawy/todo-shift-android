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
    @Test fun selectedWeeklyDaysAndCustomQuietIntervalRespectBoundaries() {
        val previous=TimeZone.getDefault();TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        try {
            val context=RuntimeEnvironment.getApplication();val policy=ReminderPolicy(context)
            fun ms(value:String)=ZonedDateTime.parse(value).toInstant().toEpochMilli()
            policy.configure("weekly-selected","WEEKLY",weekdays=setOf(1,3))
            assertEquals(ms("2026-11-16T10:00:00Z"),policy.next("weekly-selected",ms("2026-11-14T10:00:00Z"),now=ms("2026-11-14T11:00:00Z")))
            assertEquals(ms("2026-11-18T10:00:00Z"),policy.next("weekly-selected",ms("2026-11-14T10:00:00Z"),now=ms("2026-11-16T10:00:00Z")))
            NeoSmartStore(context).update {it.put("quietHours",true);it.put("quietStartMinute",12*60);it.put("quietEndMinute",14*60)}
            assertTrue(NeoNotificationPolicy.quiet(context,ms("2026-11-14T12:00:00Z")))
            assertFalse(NeoNotificationPolicy.quiet(context,ms("2026-11-14T14:00:00Z")))
        } finally {TimeZone.setDefault(previous)}
    }

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
    @Test fun widgetTaskProjectionExcludesProtectedAndMissingNoteSources() {
        val task=TaskV11("linked","Private fixture",date="2026-11-14",linkedNoteId="note")
        val locked=DayItemV11("note","2026-11-14",type=DayItemTypeV11.NOTE,title="Private fixture",noteLocked=true)
        val db=V11Database(tasks=listOf(task),dayItems=listOf(locked))
        assertTrue(NeoWidgetContentModel.tasks(db,java.time.LocalDate.parse("2026-11-14")).isEmpty())
        assertTrue(NeoWidgetContentModel.tasks(db.copy(dayItems=emptyList()),java.time.LocalDate.parse("2026-11-14")).isEmpty())
        assertEquals(1,NeoWidgetContentModel.tasks(db.copy(dayItems=listOf(locked.copy(noteLocked=false))),java.time.LocalDate.parse("2026-11-14")).size)
    }

}
