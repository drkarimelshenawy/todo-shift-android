package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.backup.Neo4LocalBackup
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ModernPersistenceTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun directionsAndAnimationsRoundTripAndDoNotFollowLanguageChanges() {
        val p =
            NeoAppearance(
                language = NeoLanguage.ARABIC,
                uiDirection = NeoDirection.LTR,
                navigationDirection = NeoDirection.RTL,
                gridDirection = NeoDirection.LTR,
                weekdayDirection = NeoDirection.RTL,
                firstWeekday = 1,
                transition = NeoTransition.VERTICAL,
                transitionSpeed = NeoSpeed.SLOW,
                performance = NeoPerformance.NORMAL,
                showTomorrow = true,
                showOutsideDates = false,
                todayStyle = NeoTodayStyle.DOTS,
            )
        val store = NeoAppearanceStore(context)
        store.save(p)
        assertEquals(p, store.load())
        store.save(p.copy(language = NeoLanguage.ENGLISH))
        assertEquals(NeoDirection.LTR, store.load().uiDirection)
        assertEquals(NeoDirection.RTL, store.load().navigationDirection)
    }

    @Test
    fun oldAppearancePayloadPreservesExplicitLanguageDirectionAndAddsSafeDefaults() {
        val p =
            Neo4AppearanceCodec.decode(
                JSONObject().put("formatVersion", 1).put("language", "ARABIC")
            )
        assertEquals(NeoDirection.RTL, p.uiDirection)
        assertEquals(6, p.firstWeekday)
        assertEquals(NeoPerformance.AUTO, p.performance)
    }

    @Test
    fun taskTemplatesAreIdempotentAndEditsAreNotRetroactive() {
        val store = NeoSmartStore(context)
        store.saveTemplate("Morning", listOf("Prepare", "Review"), null, null)
        val t = store.templates().single()
        val date = LocalDate.parse("2026-11-10")
        val db = store.instantiate(t, date, V11Database())
        assertEquals(db, store.instantiate(t, date, db))
        assertEquals(2, db.tasks.size)
        store.saveTemplate("Changed", listOf("New title"), null, null, id = t.id)
        assertEquals(listOf("Prepare", "Review"), db.tasks.map { it.title })
        assertEquals(1, store.templates().size)
        assertEquals(2, store.instantiate(store.templates().single(), date, db).tasks.size)
    }

    @Test
    fun modernMetadataRoundTripsThroughBackupWithoutSecurityKeys() {
        val db = V11Runtime(context).start()
        val store = NeoSmartStore(context)
        store.saveTemplate("Synthetic", listOf("Task"), null, null)
        store.update { it.put("notify_tasks", false).put("quietHours", true) }
        val output = ByteArrayOutputStream()
        val backup = Neo4LocalBackup(context)
        backup.export(db, output)
        val p = backup.preview(output.toByteArray().inputStream())
        store.update { it.put("notify_tasks", true) }
        backup.restore(p, p.token)
        assertFalse(store.snapshot().getBoolean("notify_tasks"))
        assertEquals("Synthetic", store.templates().single().name)
    }

    @Test
    fun invalidMetadataCannotBeWrittenOverExistingTemplates() {
        val store = NeoSmartStore(context)
        store.saveTemplate("Keep", listOf("Original"), null, null)
        var rejected = false
        try {
            store.update { it.put("version", 999) }
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals("Keep", store.templates().single().name)
    }

    @Test
    fun allocationResizingIsCentExactAndKeepsIdsAndMonths() {
        val rows = (1..3).map { PaymentAllocationV11("$it", "2026-0$it", null, 1.0) }
        val next = ModernOperations.resizedAllocations(rows, .01)
        assertEquals(1L, next.sumOf { LocalizedNumbers.cents(it.amount) })
        assertTrue(next.all { it.amount >= 0 })
        assertEquals(rows.map { it.id }, next.map { it.id })
        assertEquals(rows.map { it.accrualMonth }, next.map { it.accrualMonth })
    }
}
