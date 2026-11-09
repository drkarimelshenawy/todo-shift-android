package com.todoshift.app.storage

import android.content.SharedPreferences
import com.todoshift.app.data.*
import com.todoshift.app.v11.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

/** Synthetic preferences, never Android device storage. Exercises production parsers/runtime. */
class SafePersistenceTest {
    private class MemoryPrefs {
        val values = mutableMapOf<String, Any>()
        var writes = 0
        var commitSucceeds = true
        val prefs = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when (method.name) {
                "contains" -> values.containsKey(key)
                "getAll" -> values.toMap()
                "getString" -> if (values.containsKey(key)) values[key] as String else args!![1]
                "getBoolean" -> if (values.containsKey(key)) values[key] as Boolean else args!![1]
                "edit" -> editor()
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
        private fun editor(): SharedPreferences.Editor {
            val updates = mutableMapOf<String, Any?>()
            return Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
                when (method.name) {
                    "putString", "putBoolean" -> { updates[args!![0] as String] = args[1]; proxy }
                    "commit", "apply" -> {
                        writes++
                        if (commitSucceeds) updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                        if (method.name == "commit") commitSucceeds else null
                    }
                    else -> error("Unexpected editor call: ${method.name}")
                }
            } as SharedPreferences.Editor
        }
        fun seed(raw: String) { values["database"] = raw }
    }
    private val dates = listOf(7, 8, 11, 17, 23, 28).map { "2026-11-${it.toString().padStart(2, '0')}" }
    private fun fixture() = V11Database(
        calendars = listOf(AppCalendarV11("calendar-a", "Synthetic", "#123456")),
        days = dates.mapIndexed { i, date -> DayEntryV11("day-$i", date, "calendar-a",
            listOf(ShiftSegmentV11(id = "segment-$i", kind = ShiftKind.FULL_DAY, workplaceId = "place-a", note = "synthetic-$i", exceptionalAmount = 42.5, startMinute = 480, endMinute = 1920))) }
    )
    private fun legacy() = AppData(
        workplaces = listOf(Workplace(id = "place-a", name = "Synthetic", colorHex = "#123456")),
        shifts = dates.map { ShiftEntry(date = it, type = ShiftType.FULL_DAY, workplaceIds = listOf("place-a")) },
        payments = emptyList()
    )
    private fun runtime(old: MemoryPrefs, current: MemoryPrefs) = V11Runtime(AppStorage(old.prefs), V11Storage(current.prefs), {}, {})
    private fun json(db: V11Database = fixture()) = V11Storage(MemoryPrefs().prefs).exportJson(db)
    private fun oldJson() = AppStorage(MemoryPrefs().prefs).exportJson(legacy())
    private fun expectFailure(action: () -> Unit) {
        try { action(); fail("Expected safe failure") } catch (expected: Exception) { /* no fabricated data */ }
    }

    @Test fun validV11RetainsExactShiftIdentityAndPayload() {
        val prefs = MemoryPrefs().apply { seed(json()) }; val store = V11Storage(prefs.prefs)
        assertTrue(store.read() is StorageRead.Loaded)
        assertEquals(fixture(), store.load())
        store.save(store.load())
        assertEquals(fixture(), V11Storage(prefs.prefs).load())
        assertEquals(dates, store.load().days.map { it.date })
    }
    @Test fun intentionallyEmptyDatabaseLoadsAndCanBeSaved() {
        val prefs = MemoryPrefs().apply { seed(json(V11Database())) }; val store = V11Storage(prefs.prefs)
        assertEquals(V11Database(), store.load()); store.save(V11Database())
        assertEquals(1, prefs.writes)
    }
    @Test fun missingIsDistinctAndReadOnly() {
        val current = MemoryPrefs(); val old = MemoryPrefs()
        assertSame(StorageRead.Missing, V11Storage(current.prefs).read())
        assertSame(StorageRead.Missing, AppStorage(old.prefs).read())
        expectFailure { V11Storage(current.prefs).load() }
        expectFailure { V11Storage(current.prefs).save(V11Database()) }
        assertEquals(0, current.writes); assertEquals(0, old.writes)
    }
    @Test fun malformedJsonCannotBeOverwrittenByEmptySave() = assertRejected("{broken")
    @Test fun missingRequiredDaysCannotBeReadAsEmpty() = assertRejected(JSONObject(json()).apply { remove("days") }.toString())
    @Test fun malformedNestedArrayCannotBeReadAsEmpty() = assertRejected(JSONObject(json()).apply {
        getJSONArray("days").getJSONObject(0).put("segments", JSONObject())
    }.toString())
    @Test fun malformedNestedElementFailsWithoutDiscardingOtherDates() = assertRejected(JSONObject(json()).apply {
        getJSONArray("days").getJSONObject(3).getJSONArray("segments").put(0, "broken")
    }.toString())
    @Test fun unsupportedSchemaFails() = assertRejected(JSONObject(json()).put("schemaVersion", 99).toString())
    @Test fun fractionalSchemaFails() = assertRejected(JSONObject(json()).put("schemaVersion", 13.5).toString())
    @Test fun unknownShiftKindFailsInsteadOfBecomingOff() = assertRejected(JSONObject(json()).apply {
        getJSONArray("days").getJSONObject(0).getJSONArray("segments").getJSONObject(0).put("kind", "UNKNOWN")
    }.toString())
    @Test fun orphanedCalendarFailsWithoutDeletingDays() = assertRejected(JSONObject(json()).apply {
        getJSONArray("days").getJSONObject(0).put("calendarId", "missing")
    }.toString())
    private fun assertRejected(raw: String) {
        val current = MemoryPrefs().apply { seed(raw) }
        val old = MemoryPrefs().apply { seed(oldJson()) }
        val store = V11Storage(current.prefs)
        assertTrue(store.read() is StorageRead.Failed)
        expectFailure { store.load() }
        expectFailure { runtime(old, current).start().let { loaded ->
            // Same automatic sync/save sequence as V11Home: it must be unreachable.
            val repaired = V11WorkplaceSync.repairAndSync(loaded, AppStorage(old.prefs).load().workplaces)
            if (repaired != loaded) store.save(repaired)
        } }
        expectFailure { store.save(V11Database()) }
        assertEquals(raw, current.values["database"])
        assertEquals(0, current.writes); assertEquals(0, old.writes)
    }
    @Test fun wrongPreferenceTypeIsFailedNotMissing() {
        val current = MemoryPrefs().apply { values["database"] = true }
        assertTrue(V11Storage(current.prefs).read() is StorageRead.Failed)
        expectFailure { runtime(MemoryPrefs(), current).start() }
        assertEquals(true, current.values["database"]); assertEquals(0, current.writes)
    }
    @Test fun historicalV11SchemasLoadWithoutInventingNewCollections() {
        for (version in listOf(11, 12)) {
            val root = JSONObject(json()).put("schemaVersion", version).apply {
                remove("tasks")
                if (version == 11) { remove("dayItems"); remove("obligations"); remove("obligationPayments") }
            }
            val prefs = MemoryPrefs().apply { seed(root.toString()) }
            assertEquals(fixture().days, V11Storage(prefs.prefs).load().days)
            assertEquals(0, prefs.writes)
        }
    }
    @Test fun legacyFailureBlocksMigrationAndPreservesRawPayload() {
        for (raw in listOf("{broken", JSONObject(oldJson()).put("shifts", JSONObject()).toString(),
            JSONObject(oldJson()).put("schemaVersion", 99).toString())) {
            val old = MemoryPrefs().apply { seed(raw) }; val current = MemoryPrefs()
            assertTrue(AppStorage(old.prefs).read() is StorageRead.Failed)
            expectFailure { runtime(old, current).start() }
            expectFailure { AppStorage(old.prefs).save(legacy()) }
            assertEquals(raw, old.values["database"])
            assertEquals(0, old.writes); assertEquals(0, current.writes)
        }
    }
    @Test fun migrationPreservesAllSixDatesAndRestartDoesNotRemigrate() {
        val old = MemoryPrefs().apply { seed(oldJson()) }; val current = MemoryPrefs()
        val migrated = runtime(old, current).start()
        assertEquals(dates, migrated.days.map { it.date })
        assertEquals(setOf("v11-default-work-calendar"), migrated.days.map { it.calendarId }.toSet())
        migrated.days.forEach { day ->
            assertEquals(ShiftKind.FULL_DAY, day.segments.single().kind)
            assertEquals("place-a", day.segments.single().workplaceId)
        }
        assertEquals(oldJson(), old.values["database"])
        val second = runtime(old, current).start()
        assertEquals(migrated, second)
        assertEquals(1, current.writes); assertEquals(0, old.writes)
    }
    @Test fun unversionedLegacyFormatIsSupported() {
        val root = JSONObject(oldJson()).apply { remove("schemaVersion"); remove("dayDetails"); remove("templates") }
        val old = MemoryPrefs().apply { seed(root.toString()) }; val current = MemoryPrefs()
        assertEquals(legacy().shifts, AppStorage(old.prefs).load().shifts)
        assertEquals(dates, runtime(old, current).start().days.map { it.date })
    }
    @Test fun firstInstallPersistsWorkplaceIdsBeforeMigrationAndRestart() {
        val old = MemoryPrefs(); val current = MemoryPrefs()
        val first = runtime(old, current).start(); val places = AppStorage(old.prefs).load().workplaces
        assertTrue(places.isNotEmpty()); assertTrue(first.days.isEmpty())
        assertEquals(first, runtime(old, current).start())
        assertEquals(places, AppStorage(old.prefs).load().workplaces)
        assertEquals(1, old.writes); assertEquals(1, current.writes)
    }
    @Test fun missingPreviouslyMigratedDatabaseIsNotRecreated() {
        val current = MemoryPrefs().apply { values["migrated_from_rc1"] = true }; val old = MemoryPrefs()
        expectFailure { runtime(old, current).start() }
        assertEquals(0, old.writes); assertEquals(0, current.writes)
    }
    @Test fun ambiguousLegacyStateIsNotFirstInstall() {
        val old = MemoryPrefs().apply { values["unrelated-existing-key"] = true }; val current = MemoryPrefs()
        expectFailure { runtime(old, current).start() }
        assertEquals(0, old.writes); assertEquals(0, current.writes)
    }
    @Test fun failedMigrationCommitDoesNotReportSuccessfulStartup() {
        val old = MemoryPrefs().apply { seed(oldJson()) }; val current = MemoryPrefs().apply { commitSucceeds = false }
        expectFailure { runtime(old, current).start() }
        assertFalse(current.values.containsKey("database")); assertFalse(current.values.containsKey("migrated_from_rc1"))
    }
    @Test fun intentionallyEmptyLegacyWorkplacesAreNotReplaced() {
        val root = JSONObject(oldJson()).put("workplaces", JSONArray()).put("shifts", JSONArray())
        val old = MemoryPrefs().apply { seed(root.toString()) }
        assertTrue(AppStorage(old.prefs).load().workplaces.isEmpty()); assertEquals(0, old.writes)
    }
    @Test fun existingV11DoesNotAllowFailedLegacyLoadToReachHomeSync() {
        val current = MemoryPrefs().apply { seed(json()) }; val old = MemoryPrefs().apply { seed("broken") }
        var syncReached = false
        expectFailure {
            val database = runtime(old, current).start()
            val legacy = AppStorage(old.prefs).load() // V11Home boot loads BOTH before creating state.
            syncReached = true
            V11WorkplaceSync.repairAndSync(database, legacy.workplaces)
        }
        assertFalse(syncReached); assertEquals(0, current.writes); assertEquals(0, old.writes)
        assertEquals(json(), current.values["database"])
    }
    @Test fun ambiguousV11PreferencesAreNotFirstInstall() {
        val current = MemoryPrefs().apply { values["unknown-existing-key"] = true }; val old = MemoryPrefs()
        expectFailure { runtime(old, current).start() }
        assertEquals(0, old.writes); assertEquals(0, current.writes)
    }
    @Test fun malformedLegacyNestedArraysBlockMigration() {
        val root = JSONObject(oldJson()).apply { getJSONArray("shifts").getJSONObject(2).put("workplaceIds", "broken") }
        val old = MemoryPrefs().apply { seed(root.toString()) }; val current = MemoryPrefs()
        expectFailure { runtime(old, current).start() }
        assertEquals(root.toString(), old.values["database"])
        assertEquals(0, old.writes); assertEquals(0, current.writes)
    }
    @Test fun failedFirstInstallCommitBlocksMigration() {
        val old = MemoryPrefs().apply { commitSucceeds = false }; val current = MemoryPrefs()
        expectFailure { runtime(old, current).start() }
        assertEquals(0, current.writes)
    }
    @Test fun missingArraysInHistoricalSchemasFailSafely() {
        for (version in listOf(11, 12, 13)) {
            assertRejected(JSONObject(json()).put("schemaVersion", version).apply { remove("days") }.toString())
        }
    }
    @Test fun failedOrdinaryCommitIsReported() {
        val current = MemoryPrefs().apply { seed(json()); commitSucceeds = false }
        expectFailure { V11Storage(current.prefs).save(V11Database()) }
        assertEquals(json(), current.values["database"])
    }

    @Test fun schema11PaymentWithoutLaterAllocationsKeepsItsIdentity() {
        val root = JSONObject(json()).put("schemaVersion", 11).apply {
            remove("tasks"); remove("dayItems"); remove("obligations"); remove("obligationPayments")
            put("payments", JSONArray().put(JSONObject().put("id", "historical-payment")
                .put("amount", 123.5).put("receivedDate", "2026-11-07").put("accrualMonth", "2026-11")
                .put("linkedShiftIds", JSONArray().put("segment-0"))))
        }
        val prefs = MemoryPrefs().apply { seed(root.toString()) }
        val loaded = V11Storage(prefs.prefs).load()
        assertEquals("historical-payment", loaded.payments.single().id)
        assertEquals(listOf("segment-0"), loaded.payments.single().linkedShiftIds)
        assertEquals(123.5, loaded.payments.single().amount, 0.0)
        assertEquals(fixture().days, loaded.days); assertEquals(0, prefs.writes)
    }
    @Test fun malformedPaymentAllocationArrayCannotEmptyTheDatabase() {
        val root = JSONObject(json()).put("payments", JSONArray().put(JSONObject()
            .put("linkedShiftIds", JSONArray()).put("allocations", JSONObject())))
        assertRejected(root.toString())
    }
    @Test fun legacyPaymentAndWorkplaceIdsSurviveMigration() {
        val data = legacy().copy(payments = listOf(Payment("legacy-payment", "2026-11-07", 123.5)))
        val raw = AppStorage(MemoryPrefs().prefs).exportJson(data)
        val old = MemoryPrefs().apply { seed(raw) }; val current = MemoryPrefs()
        assertEquals(data, AppStorage(old.prefs).load())
        val loaded = runtime(old, current).start()
        assertEquals("legacy-payment", loaded.payments.single().id)
        assertEquals(setOf("place-a"), loaded.rates.map { it.workplaceId }.toSet())
        assertEquals(raw, old.values["database"])
        assertEquals(loaded, runtime(old, current).start())
    }
    @Test fun validCurrentBackupParserStillRoundTripsWithoutWriting() {
        val current = MemoryPrefs(); val store = V11Storage(current.prefs)
        assertEquals(fixture(), store.importJson(json()))
        assertEquals(0, current.writes)
    }

    @Test fun staleSnapshotCannotOverwriteBackgroundChanges() {
        val current = MemoryPrefs().apply { seed(json()) }
        val store = V11Storage(current.prefs)
        val original = store.load()
        val background = original.copy(days = original.days.dropLast(1))
        store.save(background)
        expectFailure { runtime(MemoryPrefs(), current).saveIfCurrent(original, original.copy(days = emptyList())) }
        assertEquals(background, store.load())
        assertEquals(1, current.writes)
    }
    @Test fun confirmedSnapshotSavePreservesUnrelatedRecords() {
        val current = MemoryPrefs().apply { seed(json()) }
        val before = V11Storage(current.prefs).load()
        val next = before.copy(days = before.days.mapIndexed { index, day -> if (index == 0) day.copy(note = "edited") else day })
        runtime(MemoryPrefs(), current).saveIfCurrent(before, next)
        assertEquals(next, V11Storage(current.prefs).load())
        assertEquals(before.days.drop(1), next.days.drop(1))
    }
}
