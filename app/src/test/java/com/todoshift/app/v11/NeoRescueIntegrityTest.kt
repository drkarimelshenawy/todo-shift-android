package com.todoshift.app.v11

import com.todoshift.app.v11.ui.NeoFeatureCatalog
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NeoRescueIntegrityTest {
    private val context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun todayPulseWorksIndependentlyAndHonorsEveryReductionBoundary() {
        val p = NeoAppearance(todayStyle = NeoTodayStyle.PULSE, optionalTransitions = false)
        assertTrue(neoTodayPulseEnabled(p, false, false))
        assertFalse(neoTodayPulseEnabled(p.copy(reduceMotion = true), false, false))
        assertFalse(
            neoTodayPulseEnabled(p.copy(performance = NeoPerformance.LIGHTWEIGHT), false, false)
        )
        assertFalse(neoTodayPulseEnabled(p, false, true))
        assertFalse(neoTodayPulseEnabled(p, true, false))
    }

    @Test
    fun allTransitionsRespondAndReducedMotionAlwaysUsesIdentity() {
        val p = NeoAppearance(optionalTransitions = true, motion = 2)
        val frames =
            NeoTransition.entries.map {
                com.todoshift.app.v11.ui.neoNavigationFrame(
                    p.copy(transition = it),
                    0f,
                    false,
                    false,
                )
            }
        assertEquals(6, frames.distinct().size)
        for (style in NeoTransition.entries) {
            assertEquals(
                com.todoshift.app.v11.ui.NeoNavigationFrame(),
                com.todoshift.app.v11.ui.neoNavigationFrame(
                    p.copy(transition = style),
                    0f,
                    false,
                    true,
                ),
            )
        }
        assertTrue(
            com.todoshift.app.v11.ui
                .neoNavigationFrame(p.copy(transition = NeoTransition.SLIDE), 0f, true, false)
                .x < 0
        )
    }

    @Test
    fun workplaceSourceRepairIsAdditiveAndIdempotent() {
        val original =
            IncomeSourceV11(
                "existing",
                "Custom historical name",
                IncomeSourceTypeV11.SHIFT_BASED,
                workplaceId = "w",
            )
        val db =
            V11Database(
                incomeSources = listOf(original),
                payments =
                    listOf(PaymentV11("p", 10.0, "2026-11-10", "2026-11", sourceId = "existing")),
            )
        val fixed =
            NeoWorkplaceSources.ensure(
                db,
                listOf(
                    com.todoshift.app.data.Workplace("w", "Renamed", "#00A4B8"),
                    com.todoshift.app.data.Workplace("new", "New workplace", "#6C3A9B"),
                ),
            )
        assertEquals(original, fixed.incomeSources.first())
        assertEquals(db.payments, fixed.payments)
        assertEquals(2, fixed.incomeSources.size)
        assertEquals(
            fixed,
            NeoWorkplaceSources.ensure(
                fixed,
                listOf(com.todoshift.app.data.Workplace("new", "New", "#00A4B8")),
            ),
        )
    }

    @Test
    fun newInstallUsesNeoButExplicitClassicIsPreserved() {
        val store = V11UiSettingsStore(context)
        assertEquals(V11Experience.PREMIUM, store.load().experience)
        store.save(V11UiSettings(experience = V11Experience.CLASSIC))
        assertEquals(V11Experience.CLASSIC, store.load().experience)
        assertEquals(NeoBackground.CLASSIC, NeoAppearance().background)
    }

    @Test
    fun visualProfilePreservesAllIndependentLocaleChoices() {
        val current =
            NeoAppearance(
                language = NeoLanguage.ARABIC,
                uiDirection = NeoDirection.RTL,
                navigationDirection = NeoDirection.LTR,
                gridDirection = NeoDirection.LTR,
                weekdayDirection = NeoDirection.RTL,
                firstWeekday = 1,
            )
        val next =
            NeoAppearance(brightness = NeoBrightness.AMOLED, language = NeoLanguage.ENGLISH)
                .preservingLocaleOf(current)
        assertEquals(current.language, next.language)
        assertEquals(current.uiDirection, next.uiDirection)
        assertEquals(current.navigationDirection, next.navigationDirection)
        assertEquals(current.gridDirection, next.gridDirection)
        assertEquals(current.weekdayDirection, next.weekdayDirection)
        assertEquals(1, next.firstWeekday)
        assertEquals(NeoBrightness.AMOLED, next.brightness)
    }

    @Test
    fun cardVisibilitySwitchesDoNotEnableAnotherCardImplicitly() {
        val p = NeoAppearance().withDashboardCard(NeoDashboardCard.CURRENT_SHIFT, true)
        assertTrue(p.cardVisible(NeoDashboardCard.CURRENT_SHIFT))
        assertFalse(p.cardVisible(NeoDashboardCard.SUMMARY))
        assertTrue(
            p.withDashboardCard(NeoDashboardCard.SUMMARY, true)
                .cardVisible(NeoDashboardCard.CURRENT_SHIFT)
        )
    }

    @Test
    fun detachedCleanupRevalidatesAndPreservesReferencedAndUnknownFiles() {
        val db = V11Runtime(context).start()
        val folder = File(context.filesDir, "attachments")
        folder.mkdirs()
        val linked = File(folder, "neo-attachment-linked.pdf")
        linked.writeText("%PDF-1.4")
        val orphan = File(folder, "neo-attachment-orphan.pdf")
        orphan.writeText("%PDF-1.4")
        val unknown = File(folder, "unknown-user-file.txt")
        unknown.writeText("Keep")
        NeoSmartStore(context).update {
            it.put(
                "attachments",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("owner", "task-fixture")
                            .put("name", linked.name)
                            .put("mime", "application/pdf")
                    ),
            )
        }
        val store = NeoAttachmentStore(context)
        val before = store.detachedNames()
        val another = File(folder, "neo-attachment-new.pdf")
        another.writeText("%PDF-1.4")
        assertThrows(IllegalStateException::class.java) { store.removeDetached(before) }
        assertTrue(orphan.exists())
        assertTrue(linked.exists())
        store.removeDetached(store.detachedNames())
        assertTrue(linked.exists())
        assertTrue(unknown.exists())
        assertFalse(orphan.exists())
        assertFalse(another.exists())
        assertEquals(db, V11Storage(context).load())
    }

    @Test
    fun allFeatureRoutesHaveUniqueIdsBilingualNamesAndNaturalLocations() {
        val entries = NeoFeatureCatalog.entries
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertTrue(
            entries.all {
                it.ar.isNotBlank() &&
                    it.en.isNotBlank() &&
                    it.destination in
                        listOf("Calendar", "Schedules", "Tasks", "Finances", "Settings")
            }
        )
        assertEquals("Finances", NeoFeatureCatalog.get("RATES")!!.destination)
        assertEquals("Schedules", NeoFeatureCatalog.get("SHIFT_TEMPLATE")!!.destination)
        assertEquals("Tasks", NeoFeatureCatalog.get("TASK_TEMPLATES")!!.destination)
    }
}
