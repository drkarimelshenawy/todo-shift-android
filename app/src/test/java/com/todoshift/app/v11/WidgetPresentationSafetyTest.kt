package com.todoshift.app.v11

import com.todoshift.app.widget.*
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetPresentationSafetyTest {
    private val now = LocalDateTime.parse("2026-11-10T10:00:00")

    @Test
    fun protectedFinanceNeverUsesAppUnlockAndHiddenWidgetsExposeNoRows() {
        val db =
            V11Database(
                payments = listOf(PaymentV11("p", 9000.0, "2026-11-10", "2026-11")),
                tasks = listOf(TaskV11("t", "Private fixture", date = "2026-11-10")),
            )
        val value =
            NeoWidgetContentModel.project(
                Neo4WidgetKind.FINANCE,
                db,
                Neo4WidgetOptions(showAmounts = true),
                V11SecuritySettings(protectAccounts = true),
                false,
                now,
                true,
            )
        assertEquals("••••••", value.value)
        for (kind in Neo4WidgetKind.entries) {
            val hidden =
                NeoWidgetContentModel.project(
                    kind,
                    db,
                    Neo4WidgetOptions(hideContent = true),
                    V11SecuritySettings(),
                    false,
                    now,
                    true,
                )
            assertEquals("Hidden", hidden.value)
            assertTrue(hidden.rows.isEmpty())
        }
        assertEquals(
            "Unable to read data",
            NeoWidgetContentModel.project(
                    Neo4WidgetKind.TASKS,
                    null,
                    Neo4WidgetOptions(),
                    null,
                    true,
                    now,
                    true,
                )
                .value,
        )
    }

    @Test
    fun taskProjectionPreservesWidgetOrderingAndCalendarVisibility() {
        val db =
            V11Database(
                calendars =
                    listOf(
                        AppCalendarV11("a", "Shown", "#00A4B8"),
                        AppCalendarV11("b", "Hidden", "#6C3A9B", visible = false),
                    ),
                tasks =
                    listOf(
                        TaskV11("later", "Later", date = "2026-11-11", calendarId = "a"),
                        TaskV11("early", "Early", date = "2026-11-09"),
                        TaskV11("hidden", "Hidden", date = "2026-11-08", calendarId = "b"),
                    ),
            )
        assertEquals(
            listOf("early", "later"),
            NeoWidgetContentModel.tasks(db, now.toLocalDate()).map { it.id },
        )
    }

    @Test
    fun corruptPrivacyPreferencesFailClosedAndKeepRawBytes() {
        val c = RuntimeEnvironment.getApplication()
        val p = c.getSharedPreferences("todo_shift_neo4_widgets", 0)
        for (raw in
            listOf("{corrupt", "{\"hideContent\":\"not-a-boolean\",\"showAmounts\":true}")) {
            p.edit().putString("TASKS", raw).commit()
            assertTrue(Neo4WidgetSettings(c).load(Neo4WidgetKind.TASKS).hideContent)
            assertEquals(raw, p.getString("TASKS", null))
        }
        val privacy = c.getSharedPreferences("todo_shift_neo4_widget_privacy", 0)
        check(privacy.edit().putString("amountsHidden", "corrupt-type").commit())
        assertTrue(Neo4WidgetPrivacy.amountsHidden(c))
        assertEquals("corrupt-type", privacy.getString("amountsHidden", null))
    }

    @Test
    fun widgetOnlyReplayNeverCancelsOrReschedulesAlarmsAndFailureRemainsPending() {
        val c = RuntimeEnvironment.getApplication()
        val q = NeoCommittedEffects(c)
        q.prepareWidgets()
        try {
            q.reconcile(
                V11Database(),
                { fail("No alarm cancellation") },
                { fail("No alarm schedule") },
                { error("Injected widget delivery failure") },
            )
            fail("Must remain pending")
        } catch (_: IllegalStateException) {}
        var widgets = 0
        q.reconcile(
            V11Database(),
            { fail("No alarm cancellation") },
            { fail("No alarm schedule") },
            { widgets++ },
        )
        assertEquals(1, widgets)
        assertFalse(
            c.getSharedPreferences(NeoCommittedEffects.STORE_NAME, 0).getBoolean("pending", false)
        )
    }

    @Test
    fun customWidgetAccentMeetsContrastOnItsOpaqueReferenceSurface() {
        val c = RuntimeEnvironment.getApplication()
        val store = Neo4WidgetSettings(c)
        for (theme in listOf(Neo4WidgetTheme.LIGHT, Neo4WidgetTheme.DARK)) for (accent in
            listOf("#397BA2", "#D8AD55", "#6C3A9B", "#FFFFFF", "#000000")) {
            store.save(Neo4WidgetKind.TASKS, Neo4WidgetOptions(theme = theme, accent = accent))
            val skin = store.skin(Neo4WidgetKind.TASKS)
            assertTrue(
                androidx.core.graphics.ColorUtils.calculateContrast(skin.accent, skin.background) >=
                    4.5
            )
        }
    }

    @Test
    fun cachedTaskRowsCannotLeakAfterHidePreferenceChangesBeforeAdapterReload() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val old = runtime.start()
        runtime.saveIfCurrent(
            old,
            old.copy(
                tasks = listOf(TaskV11("private", "Cached private fixture", date = "2026-11-10"))
            ),
        )
        val factory = TasksWidgetFactory(c, android.content.Intent())
        factory.onCreate()
        assertEquals(1, factory.getCount())
        c.getSharedPreferences("todo_shift_neo4_widgets", 0)
            .edit()
            .putString("TASKS", "{\"hideContent\":true}")
            .commit()
        assertEquals(0, factory.getCount())
        assertNull(factory.getViewAt(0))
    }

    @Test
    fun cachedCalendarRendererChecksLiveHideFlagBeforeSendingAnyPaintOrLabels() {
        val c = RuntimeEnvironment.getApplication()
        V11Runtime(c).start()
        val prefs = c.getSharedPreferences("todo_shift_neo4_widgets", 0)
        prefs.edit().putString("CALENDAR", "{\"hideContent\":false,\"showDetails\":true}").commit()
        val factory = CalendarWidgetFactory(c, android.content.Intent())
        factory.onCreate()
        factory.onDataSetChanged()
        prefs.edit().putString("CALENDAR", "{\"hideContent\":true}").commit()
        val view = factory.getViewAt(15).apply(c, android.widget.FrameLayout(c))
        assertEquals(
            android.view.View.GONE,
            view.findViewById<android.view.View>(com.todoshift.app.R.id.day_shift).visibility,
        )
        assertEquals(
            android.view.View.GONE,
            view.findViewById<android.view.View>(com.todoshift.app.R.id.day_paint).visibility,
        )
    }
}
