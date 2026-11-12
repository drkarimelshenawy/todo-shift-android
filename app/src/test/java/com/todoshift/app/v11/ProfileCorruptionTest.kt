package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4LocalBackup
import java.io.ByteArrayOutputStream
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileCorruptionTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences("todo_shift_neo4_profiles", 0)
    private fun rejects(action: () -> Unit) {
        try { action(); fail("Corruption must be reported") }
        catch (failure: IllegalStateException) { assertTrue(failure.message!!.contains("preserved")) }
    }

    @Test fun malformedRootCannotBecomeAnEmptySuccessfulCollection() {
        val raw = "{broken-exact-original"
        prefs.edit().putString("profiles", raw).commit()
        val store = Neo4Profiles(context)
        rejects { store.list() }
        rejects { store.save("New", NeoAppearance()) }
        rejects { store.delete("any") }
        assertEquals(raw, prefs.getString("profiles", null))
    }

    @Test fun oneMalformedRecordCannotBeSilentlyDroppedOrMutated() {
        val store = Neo4Profiles(context)
        store.save("Valid", NeoAppearance())
        val root = JSONObject(prefs.getString("profiles", null)!!).put("broken", JSONObject().put("name", "Broken"))
        val raw = root.toString()
        prefs.edit().putString("profiles", raw).commit()
        rejects { store.list() }
        rejects { store.delete("broken") }
        assertEquals(raw, prefs.getString("profiles", null))
    }

    @Test fun backupCannotSilentlyOmitUnreadableProfileAssets() {
        val data = V11Runtime(context).start()
        prefs.edit().putString("profiles", "{broken").commit()
        val output = ByteArrayOutputStream()
        rejects { Neo4LocalBackup(context).export(data, output) }
        assertEquals(0, output.size())
        assertEquals("{broken", prefs.getString("profiles", null))
    }

    @Test fun checksumValidArchiveWithCorruptProfilesIsRejectedBeforeRestore() {
        val data = V11Runtime(context).start()
        val backup = Neo4LocalBackup(context)
        val original = ByteArrayOutputStream().also { backup.export(data, it) }.toByteArray()
        val entries = linkedMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(original.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        val name = "prefs/todo_shift_neo4_profiles.json"
        val corrupt = JSONObject().put("profiles", JSONObject().put("type", "string")
            .put("value", "{broken")).toString().toByteArray()
        entries[name] = corrupt
        val manifest = JSONObject(String(entries.getValue("manifest.json"), Charsets.UTF_8))
        val sections = manifest.getJSONArray("sections")
        for (i in 0 until sections.length()) {
            val section = sections.getJSONObject(i)
            if (section.getString("name") == name) {
                section.put("bytes", corrupt.size)
                section.put("sha256", com.todoshift.app.backup.Neo4BackupFormat.sha256(corrupt))
            }
        }
        entries["manifest.json"] = manifest.toString().toByteArray()
        val raw = ByteArrayOutputStream().also { output ->
            java.util.zip.ZipOutputStream(output).use { zip ->
                entries.forEach { (path, bytes) ->
                    zip.putNextEntry(java.util.zip.ZipEntry(path)); zip.write(bytes); zip.closeEntry()
                }
            }
        }.toByteArray()
        try { backup.preview(raw.inputStream()); fail("Corrupt profile payload must be rejected") }
        catch (_: org.json.JSONException) { }
        assertEquals(data, V11Storage(context).load())
        assertFalse(prefs.contains("profiles"))
    }
}
