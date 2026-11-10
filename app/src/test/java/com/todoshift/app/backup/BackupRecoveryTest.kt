package com.todoshift.app.backup

import android.content.Context
import com.todoshift.app.v11.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRecoveryTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private fun backup() = Neo4LocalBackup(context)

    private fun archive(): ByteArray {
        val db = V11Runtime(context).start()
        return ByteArrayOutputStream().also { backup().export(db, it) }.toByteArray()
    }

    private fun fails(action: () -> Unit) {
        var failed = false
        try {
            action()
        } catch (_: Exception) {
            failed = true
        }
        assertTrue("Expected operation to fail closed", failed)
    }

    @Test
    fun normalRoundTripRequiresExactConfirmation() {
        val prepared = backup().preview(archive().inputStream())
        fails { backup().restore(prepared, "wrong-token") }
        val before = V11Storage(context).load()
        assertEquals(before, backup().restore(prepared, prepared.token).database)
    }

    @Test
    fun corruptStoreIsPreservedAndOnlyExplicitRecoveryCanReplaceIt() {
        val prepared = backup().preview(archive().inputStream())
        val prefs = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE)
        prefs.edit().putString("database", "{broken-exact-original").commit()
        fails { backup().restore(prepared, prepared.token) }
        assertEquals("{broken-exact-original", prefs.getString("database", null))
        assertFalse(File(context.filesDir, "neo4-restore-journal.json").exists())
        val result = backup().recoverFromBackup(prepared, prepared.token)
        assertEquals(result.database, V11Storage(context).load())
        val quarantine =
            context.filesDir.listFiles()!!.single { it.name.startsWith("neo41-before-recovery-") }
        assertTrue(quarantine.readText().contains("{broken-exact-original"))
    }

    @Test
    fun checksumMismatchCannotReachRestoreWrites() {
        val bytes = archive()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { out ->
            ZipInputStream(bytes.inputStream()).use { input ->
                while (true) {
                    val e = input.nextEntry ?: break
                    val data = input.readBytes()
                    out.putNextEntry(ZipEntry(e.name))
                    out.write(if (e.name == "data/v11.json") data + ' '.code.toByte() else data)
                    out.closeEntry()
                }
            }
        }
        val before = V11Storage(context).load()
        fails { backup().preview(output.toByteArray().inputStream()) }
        assertEquals(before, V11Storage(context).load())
    }

    @Test
    fun interruptedRestoreRollsBackBeforeRuntimeLoads() {
        archive()
        val prefs = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE)
        val original = prefs.getString("database", null)!!
        val previous =
            JSONObject()
                .put(
                    "todo_shift_v11",
                    JSONObject()
                        .put("database", JSONObject().put("type", "string").put("value", original))
                        .put(
                            "migrated_from_rc1",
                            JSONObject().put("type", "boolean").put("value", true),
                        ),
                )
        File(context.filesDir, "neo4-restore-journal.json")
            .writeText(JSONObject().put("format", 1).put("previous", previous).toString())
        prefs.edit().putString("database", "{interrupted").commit()
        V11Runtime(context).start()
        assertEquals(original, prefs.getString("database", null))
        assertFalse(File(context.filesDir, "neo4-restore-journal.json").exists())
    }

    @Test
    fun secretsAreNotPortableAndRestoreDoesNotReplaceThem() {
        context
            .getSharedPreferences("todo_shift_security_v11", Context.MODE_PRIVATE)
            .edit()
            .putString("sentinel", "private")
            .commit()
        val bytes = archive()
        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                assertFalse(e.name.contains("security"))
                zip.readBytes()
            }
        }
        val p = backup().preview(bytes.inputStream())
        backup().restore(p, p.token)
        assertEquals(
            "private",
            context
                .getSharedPreferences("todo_shift_security_v11", Context.MODE_PRIVATE)
                .getString("sentinel", null),
        )
    }

    @Test
    fun staleExportCannotClaimToBackUpOldUiSnapshot() {
        val before = V11Runtime(context).start()
        val next = before.copy(tasks = listOf(TaskV11("new", "New task", date = "2026-11-09")))
        V11Runtime(context).saveIfCurrent(before, next)
        fails { backup().export(before, ByteArrayOutputStream()) }
        assertEquals(next, V11Storage(context).load())
    }

    @Test
    fun archiveRoundTripPreservesRecordIdentitiesAndShiftTimes() {
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val calendar = before.calendars.first().id
        val next =
            before.copy(
                days =
                    listOf(
                        DayEntryV11(
                            "day-synthetic",
                            "2026-11-09",
                            calendar,
                            listOf(
                                ShiftSegmentV11(
                                    "shift-synthetic",
                                    ShiftKind.NIGHT,
                                    startMinute = 1200,
                                    endMinute = 1800,
                                )
                            ),
                        )
                    ),
                tasks =
                    listOf(
                        TaskV11(
                            "task-synthetic",
                            "Synthetic task",
                            date = "2026-11-09",
                            calendarId = calendar,
                        )
                    ),
            )
        runtime.saveIfCurrent(before, next)
        val out = ByteArrayOutputStream()
        backup().export(next, out)
        val prepared = backup().preview(out.toByteArray().inputStream())
        assertEquals(next, backup().restore(prepared, prepared.token).database)
    }

    @Test
    fun validChecksumsCannotHideIncorrectPreferenceTypes() {
        val content = linkedMapOf<String, ByteArray>()
        ZipInputStream(archive().inputStream()).use { input ->
            while (true) {
                val e = input.nextEntry ?: break
                content[e.name] = input.readBytes()
            }
        }
        val path = "prefs/todo_shift_v11_ui.json"
        content[path] =
            JSONObject(String(content.getValue(path)))
                .put(
                    "showWorkplaceNames",
                    JSONObject().put("type", "string").put("value", "wrong-type"),
                )
                .toString()
                .toByteArray()
        val manifest = JSONObject(String(content.getValue("manifest.json")))
        val sections = manifest.getJSONArray("sections")
        for (index in 0 until sections.length()) {
            val item = sections.getJSONObject(index)
            if (item.getString("name") == path) {
                item.put("bytes", content.getValue(path).size)
                item.put("sha256", Neo4BackupFormat.sha256(content.getValue(path)))
            }
        }
        content["manifest.json"] = manifest.toString().toByteArray()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            content.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        val before = V11Storage(context).load()
        fails { backup().preview(output.toByteArray().inputStream()) }
        assertEquals(before, V11Storage(context).load())
    }

    @Test
    fun selectingPrimaryCalendarDoesNotRewriteVisibilityOrRecords() {
        val db = V11Runtime(context).start()
        val prefs = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE)
        val before = prefs.getString("database", null)
        val primary = V11ActiveCalendarStore(context)
        primary.save(db.calendars.first().id)
        assertEquals(db.calendars.first().id, primary.load())
        assertEquals(before, prefs.getString("database", null))
        assertEquals(db, V11Storage(context).load())
    }

    @Test
    fun interruptedRestoreAtEachPreferenceBoundaryRecoversPreviousState() {
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val candidate =
            before.copy(
                tasks = listOf(TaskV11("restore-fixture", "New fixture", date = "2026-11-10"))
            )
        val output = ByteArrayOutputStream()
        runtime.saveIfCurrent(before, candidate)
        backup().export(candidate, output)
        runtime.saveIfCurrent(candidate, before)
        for (boundary in
            listOf("journal", "assets", "prefs:todo_shift_v11", "prefs:${NeoSmartStore.NAME}")) {
            val coordinator = backup()
            val prepared = coordinator.preview(output.toByteArray().inputStream())
            var interrupted = false
            coordinator.restoreCheckpoint = {
                if (it == boundary) {
                    interrupted = true
                    throw AssertionError("Simulated interruption")
                }
            }
            try {
                coordinator.restore(prepared, prepared.token)
                fail("Must interrupt at $boundary")
            } catch (expected: AssertionError) {}
            assertTrue("Boundary reached: $boundary", interrupted)
            assertEquals(before, V11Runtime(context).start())
            assertFalse(File(context.filesDir, "neo4-restore-journal.json").exists())
        }
    }
}
