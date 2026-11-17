package com.todoshift.app.v11

import com.todoshift.app.backup.BackupSection
import com.todoshift.app.backup.Neo4LocalBackup
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoNoteHistoryPortableTest {
    private val context get()=RuntimeEnvironment.getApplication()

    @Test fun portableHistoryIsBoundedValidatedAndMergeIdempotent() {
        val store=NeoNoteHistoryStore(context)
        val id="portable-"+System.nanoTime()
        store.savePrior(id,"Title","سجل سري • secret version",1000L,force=true)
        store.savePrior(id,"Title 2","Older text",3000L,force=true)
        val snapshot=store.portableSnapshot(listOf(id))
        assertEquals(1,snapshot.getJSONArray("notes").length())
        val loaded=store.validatePortable(snapshot,setOf(id))
        assertEquals(2,loaded.getValue(id).size)
        store.mergePortable(loaded)
        store.mergePortable(loaded)
        assertEquals(2,store.revisions(id).size)
        NeoNoteDraftStore(context).remove("note-history-v49:$id")
        assertTrue(store.revisions(id).isEmpty())
        store.mergePortable(loaded)
        assertEquals("Older text",store.revisions(id).first().details)
    }

    @Test fun historyOwnerForgeryAndExcessVersionsAreRejected() {
        val records=JSONArray()
        val revisions=JSONArray().put(
            JSONObject().put("savedAt",1234)
                .put("title","secret").put("details","private"))
        records.put(JSONObject().put("id","note-from-other-account")
            .put("revisions",revisions))
        val forged=JSONObject().put("format",1).put("notes",records)
        val store=NeoNoteHistoryStore(context)
        try {
            store.validatePortable(forged,setOf("valid-id"))
            fail("Unowned revision accepted")
        } catch(_:IllegalArgumentException) {}
        val excessive=JSONArray()
        repeat(NeoNoteHistoryStore.MAX_REVISIONS+1) {
            excessive.put(JSONObject().put("savedAt",it).put("title","T").put("details","D"))
        }
        val invalid=JSONObject().put("format",1).put("notes",JSONArray()
            .put(JSONObject().put("id","valid-id").put("revisions",excessive)))
        try {
            store.validatePortable(invalid,setOf("valid-id"))
            fail("Unbounded revisions accepted")
        } catch(_:IllegalArgumentException) {}
    }

    @Test fun plainBackupDoesNotContainHistoryButEncryptedBackupDoes() {
        val runtime=V11Runtime(context)
        val initial=runtime.start()
        val id="encrypted-note-"+System.nanoTime()
        val note=DayItemV11(id,"2026-11-17",
            type=DayItemTypeV11.NOTE,title="Current",details="Live text")
        val db=initial.copy(dayItems=initial.dayItems+note)
        runtime.saveIfCurrent(initial,db)
        NeoNoteHistoryStore(context).savePrior(id,"Private old title",
            "Private historical content",1000L,force=true)
        val backup=Neo4LocalBackup(context)
        fun entries(zip:ByteArray):Set<String> {
            val found=mutableSetOf<String>()
            ZipInputStream(zip.inputStream()).use {reader ->
                while(true) {
                    val next=reader.nextEntry ?: break
                    found+=next.name
                    reader.closeEntry()
                }
            }
            return found
        }
        val plain=ByteArrayOutputStream()
        backup.export(db,plain)
        assertFalse("data/note-history-v54.json" in entries(plain.toByteArray()))
        val secret="portable backup phrase 2026".toCharArray()
        val encrypted=ByteArrayOutputStream()
        backup.exportEncrypted(db,encrypted,BackupSection.entries.toSet(),secret)
        val ciphertext=encrypted.toByteArray()
        assertFalse(String(ciphertext,Charsets.ISO_8859_1).contains(
            "Private historical content"))
        val decoded=backup.preview(ciphertext.inputStream(),secret)
        assertTrue("data/note-history-v54.json" in entries(decoded.bytes))
        assertEquals(1,NeoNoteHistoryStore(context).revisions(id).size)
    }
}
