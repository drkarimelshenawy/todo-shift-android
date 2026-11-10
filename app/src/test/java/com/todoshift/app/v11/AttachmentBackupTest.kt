package com.todoshift.app.v11

import androidx.core.content.FileProvider
import com.todoshift.app.backup.Neo4LocalBackup
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AttachmentBackupTest {
    @Test
    fun attachmentPathWhitelistRejectsTraversalAndExecutableTypes() {
        assertFalse(NeoAttachmentStore.safePath("attachments/../../secret"))
        assertFalse(NeoAttachmentStore.safePath("attachments/neo-attachment-a.apk"))
        assertTrue(NeoAttachmentStore.safePath("attachments/neo-attachment-a.pdf"))
    }

    @Test
    fun referencedAttachmentAndMetadataRestoreTogether() {
        val context = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val task = TaskV11("synthetic-owner", "Synthetic task", date = "2026-11-10")
        val db = before.copy(tasks = listOf(task))
        runtime.saveIfCurrent(before, db)
        val folder = File(context.filesDir, "attachments")
        folder.mkdirs()
        val input = File(folder, "synthetic-source.pdf")
        input.writeText("%PDF-1.4\nSynthetic test attachment\n%%EOF")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.attachments", input)
        val attachment = NeoAttachmentStore(context).import(task.id, uri)
        val file = File(folder, attachment.name)
        val original = file.readBytes()
        val output = ByteArrayOutputStream()
        val backup = Neo4LocalBackup(context)
        backup.export(db, output)
        val prepared = backup.preview(output.toByteArray().inputStream())
        assertTrue(file.delete()) // Simulates the missing attachment in this isolated test sandbox.
        backup.restore(prepared, prepared.token)
        assertArrayEquals(original, file.readBytes())
        assertEquals(task.id, NeoAttachmentStore(context).list().single().owner)
        assertEquals(db, V11Storage(context).load())
    }

    @Test
    fun interruptedAssetStageRemovesOnlyNewUnreferencedMatchingFiles() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val task = TaskV11("staged-owner", "Fixture", date = "2026-11-10")
        val candidate = before.copy(tasks = listOf(task))
        runtime.saveIfCurrent(before, candidate)
        val folder = File(c.filesDir, "attachments").apply { mkdirs() }
        val source =
            File(folder, "fixture-source.pdf").apply { writeText("%PDF-1.4\nFixture\n%%EOF") }
        // Each Robolectric method has a fresh dataDir; reattach the actual provider to that root.
        FileProvider()
            .attachInfo(
                c,
                c.packageManager.resolveContentProvider("${c.packageName}.attachments", 0)!!,
            )
        val uri = FileProvider.getUriForFile(c, "${c.packageName}.attachments", source)
        val attachment = NeoAttachmentStore(c).import(task.id, uri)
        val staged = File(folder, attachment.name)
        val coordinator = Neo4LocalBackup(c)
        val output = ByteArrayOutputStream()
        coordinator.export(candidate, output)
        runtime.saveIfCurrent(candidate, before)
        // Simulate a different current store, exclusively inside this test sandbox.
        assertTrue(c.getSharedPreferences(NeoSmartStore.NAME, 0).edit().clear().commit())
        assertTrue(staged.delete())
        val unknown = File(folder, "keep-unknown.bin").apply { writeText("Untouched fixture") }
        val prepared = coordinator.preview(output.toByteArray().inputStream())
        var interrupted = false
        coordinator.restoreCheckpoint = {
            if (it == "assets") {
                interrupted = true
                throw AssertionError("Simulated interruption")
            }
        }
        try {
            coordinator.restore(prepared, prepared.token)
            fail("Must interrupt")
        } catch (expected: AssertionError) {}
        assertTrue(interrupted)
        assertTrue(staged.exists())
        assertEquals(before, V11Runtime(c).start())
        assertFalse(staged.exists())
        assertEquals("Untouched fixture", unknown.readText())
        assertEquals("%PDF-1.4\nFixture\n%%EOF", source.readText())
    }
}
