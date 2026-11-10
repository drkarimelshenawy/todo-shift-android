package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4LocalBackup
import com.todoshift.app.widget.NeoWidgetContentModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Random
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Synthetic desktop regression, not device p95, peak-memory or upgrade acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PremiumLargeBackupTest {
    @Test
    fun fiveYearsAndFullAttachmentAllowanceRoundTripWithoutIdentityOrCentChanges() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val initial = runtime.start()
        val start = LocalDate.of(2022, 1, 1)
        fun date(index: Int) = start.plusDays((index % 1826).toLong()).toString()
        val calendars =
            listOf(
                AppCalendarV11("visible", "Visible", "#00A4B8"),
                AppCalendarV11("hidden", "Hidden", "#6C3A9B", visible = false),
            )
        val fixture =
            initial.copy(
                calendars = calendars,
                tasks =
                    List(10000) { i ->
                        TaskV11(
                            "task-$i",
                            "Fixture مهمة $i",
                            date = date(i),
                            calendarId = calendars[i % 2].id,
                        )
                    },
                days =
                    List(1500) { i ->
                        DayEntryV11(
                            "day-$i",
                            date(i),
                            calendars[i % 2].id,
                            listOf(
                                ShiftSegmentV11(
                                    "am-$i",
                                    ShiftKind.MORNING,
                                    startMinute = 540,
                                    endMinute = 900,
                                    exceptionalAmount = 90.25,
                                ),
                                if (i % 7 == 0) ShiftSegmentV11("off-$i", ShiftKind.OFF)
                                else
                                    ShiftSegmentV11(
                                        "night-$i",
                                        ShiftKind.NIGHT,
                                        startMinute = 1260,
                                        endMinute = 1980,
                                    ),
                            ),
                        )
                    },
                dayItems =
                    List(1000) { i ->
                        DayItemV11(
                            "note-$i",
                            date(i),
                            calendars[i % 2].id,
                            DayItemTypeV11.NOTE,
                            "📌 Fixture $i",
                            createdAt = i.toLong(),
                        )
                    },
                payments =
                    List(2000) { i ->
                        val cents = 900050L + i
                        PaymentV11(
                            "receipt-$i",
                            cents / 100.0,
                            date(i),
                            date(i).take(7),
                            allocations =
                                listOf(
                                    PaymentAllocationV11(
                                        "allocation-a-$i",
                                        "2024-02",
                                        amount = (cents / 2) / 100.0,
                                    ),
                                    PaymentAllocationV11(
                                        "allocation-b-$i",
                                        "2024-03",
                                        amount = (cents - cents / 2) / 100.0,
                                    ),
                                ),
                        )
                    },
            )
        runtime.saveIfCurrent(initial, fixture)
        val folder = File(c.filesDir, "attachments").apply { mkdirs() }
        val random = Random(147)
        val hashes = linkedMapOf<String, String>()
        val attachments = JSONArray()
        repeat(6) { i ->
            val bytes = ByteArray(NeoAttachmentStore.MAX_FILE).also { random.nextBytes(it) }
            "%PDF-1.4\n".toByteArray().copyInto(bytes)
            val name = "neo-attachment-fixture-$i.pdf"
            File(folder, name).writeBytes(bytes)
            hashes[name] = sha(bytes)
            attachments.put(
                JSONObject()
                    .put("owner", "task-$i")
                    .put("name", name)
                    .put("mime", "application/pdf")
            )
        }
        NeoSmartStore(c).update { it.put("attachments", attachments) }
        val beforeProjection = NeoWidgetContentModel.indexedSegments(fixture)
        assertEquals(
            1500,
            beforeProjection.values.sumOf { it.size },
        ) // Hidden calendar excluded, not deleted.
        assertEquals(3000, fixture.days.sumOf { it.segments.size })
        assertTrue(fixture.tasks.any { it.date == "2024-02-29" })
        val heapBefore = usedHeap()
        val backup = Neo4LocalBackup(c)
        val output = ByteArrayOutputStream()
        val exportStart = System.nanoTime()
        backup.export(fixture, output)
        val exportMs = elapsed(exportStart)
        val raw = output.toByteArray()
        val previewStart = System.nanoTime()
        val prepared = backup.preview(raw.inputStream())
        val previewMs = elapsed(previewStart)
        assertEquals(fixture, V11Storage(c).load()) // Preview must be read-only.
        assertEquals(hashes, hashes.keys.associateWith { sha(File(folder, it).readBytes()) })
        runtime.saveIfCurrent(fixture, initial)
        hashes.keys.forEach { assertTrue(File(folder, it).delete()) } // Sandbox only.
        val restoreStart = System.nanoTime()
        backup.restore(prepared, prepared.token)
        val restoreMs = elapsed(restoreStart)
        val restored = V11Storage(c).load()
        assertEquals(fixture, restored)
        assertEquals(beforeProjection, NeoWidgetContentModel.indexedSegments(restored))
        assertEquals(cents(fixture), cents(restored))
        assertEquals(hashes, hashes.keys.associateWith { sha(File(folder, it).readBytes()) })
        assertEquals(6, NeoAttachmentStore(c).list().size)
        println(
            JSONObject()
                .put("fixtureSeed", 147)
                .put(
                    "environment",
                    "Robolectric SDK28 desktop JVM; no physical p95 or peak-memory claim",
                )
                .put("tasks", 10000)
                .put("segments", 3000)
                .put("notes", 1000)
                .put("receipts", 2000)
                .put("attachmentBytes", 6L * NeoAttachmentStore.MAX_FILE)
                .put("zipBytes", raw.size)
                .put("exportMs", exportMs)
                .put("previewMs", previewMs)
                .put("restoreMs", restoreMs)
                .put("heapBeforeBytes", heapBefore)
                .put("heapAfterBytes", usedHeap())
                .toString()
        )
    }

    private fun cents(db: V11Database) =
        db.payments.sumOf { BigDecimal.valueOf(it.amount).movePointRight(2).longValueExact() }

    private fun usedHeap() = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    private fun elapsed(start: Long) = (System.nanoTime() - start) / 1000000

    private fun sha(bytes: ByteArray) =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }
}
