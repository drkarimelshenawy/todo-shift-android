package com.todoshift.app.v11

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-encrypted local note revisions, separate from the working draft.
 *
 * No plaintext is written to the smart metadata preferences. The underlying
 * draft cipher uses Android Keystore and atomic, AAD-bound files. The local
 * revisions are portable only inside password-encrypted backups, while
 * unencrypted exports intentionally omit this private history.
 */
internal class NeoNoteHistoryStore(context: Context) {
    internal data class Revision(
        val savedAt:Long,
        val title:String,
        val details:String
    )
    private val drafts=NeoNoteDraftStore(context.applicationContext)
    private fun key(noteId:String)="note-history-v49:$noteId"

    fun revisions(noteId:String):List<Revision> = synchronized(monitor) {
        require(noteId.isNotBlank() && noteId.length<=200)
        val stored=drafts.read(key(noteId))?.optJSONArray("revisions") ?: return@synchronized emptyList()
        (0 until stored.length()).map {i ->
            stored.getJSONObject(i).let {o ->
                Revision(o.getLong("savedAt"),o.getString("title"),o.getString("details"))
            }
        }
    }

    /**
     * Preserve the prior committed content only after the new note was
     * successfully saved. Avoid making a new version for metadata-only edits.
     */
    fun savePrior(noteId:String,oldTitle:String,oldDetails:String,savedAt:Long,force:Boolean=false) =
        synchronized(monitor) {
            require(noteId.isNotBlank() && noteId.length<=200)
            require(oldTitle.length<=1000 && oldDetails.length<=300_000)
            val previous=revisions(noteId)
            // Autosave happens frequently; revisions must be meaningful
            // checkpoints rather than filling the 12 slots while typing.
            if(previous.firstOrNull()?.let {
                (it.title==oldTitle && it.details==oldDetails) ||
                    (!force && savedAt-it.savedAt in 0 until MIN_INTERVAL_MS)
            }==true) return@synchronized
            val next=(listOf(Revision(savedAt,oldTitle,oldDetails))+previous)
                .distinctBy{it.title to it.details}.take(MAX_REVISIONS)
            val encoded=JSONArray()
            next.forEach {r ->
                encoded.put(JSONObject().put("savedAt",r.savedAt)
                    .put("title",r.title).put("details",r.details))
            }
            drafts.save(key(noteId),JSONObject().put("format",1).put("revisions",encoded))
        }

    /**
     * Plaintext JSON is created only in memory while preparing an encrypted
     * backup, and never written to an unencrypted portable archive.
     */
    fun portableSnapshot(eligibleIds:Collection<String>):JSONObject = synchronized(monitor) {
        require(eligibleIds.all {it.isNotBlank() && it.length<=200})
        // An app may contain thousands of notes; only nonempty histories
        // consume one of the bounded portable-history slots.
        val notes=JSONArray()
        eligibleIds.distinct().forEach {id ->
            val history=revisions(id)
            if(history.isNotEmpty()) {
                val entry=JSONObject().put("id",id)
                val versions=JSONArray()
                history.forEach {item ->
                    versions.put(JSONObject().put("savedAt",item.savedAt)
                        .put("title",item.title).put("details",item.details))
                }
                require(notes.length()<MAX_PORTABLE_NOTES) {
                    "Too many note histories for this encrypted backup"
                }
                notes.put(entry.put("revisions",versions))
            }
        }
        val output=JSONObject().put("format",1).put("notes",notes)
        require(output.toString().toByteArray(Charsets.UTF_8).size<=MAX_PORTABLE_BYTES) {
            "Encrypted note history exceeds its 2 MiB backup limit"
        }
        output
    }

    /**
     * Strict pure parse before restore modifies any user data. Parsed archive
     * can be merge-restored without duplicating identical note versions.
     */
    fun validatePortable(document:JSONObject,eligibleIds:Set<String>):Map<String,List<Revision>> {
        require(document.getInt("format")==1)
        val notes=document.getJSONArray("notes")
        require(notes.length()<=MAX_PORTABLE_NOTES)
        val result=linkedMapOf<String,List<Revision>>()
        for(index in 0 until notes.length()) {
            val entry=notes.getJSONObject(index)
            val id=entry.getString("id")
            require(id.isNotBlank() && id.length<=200 && id in eligibleIds &&
                id !in result) {"Invalid note revision owner"}
            val versions=entry.getJSONArray("revisions")
            require(versions.length() in 1..MAX_REVISIONS)
            val list=(0 until versions.length()).map {i ->
                val revision=versions.getJSONObject(i)
                val title=revision.getString("title")
                val details=revision.getString("details")
                require(title.length<=1000 && details.length<=300_000)
                val timestamp=revision.getLong("savedAt")
                require(timestamp>=0)
                Revision(timestamp,title,details)
            }
            result[id]=list
        }
        return result
    }

    /**
     * Imported plaintext never touches the Smart preferences; each revision
     * is immediately encrypted with this device's Android Keystore key.
     * Calling again is idempotent.
     */
    fun mergePortable(incoming:Map<String,List<Revision>>) = synchronized(monitor) {
        incoming.forEach {(id,versions) ->
            require(id.isNotBlank() && id.length<=200 && versions.size<=MAX_REVISIONS)
            val combined=(revisions(id)+versions)
                .sortedByDescending{it.savedAt}
                .distinctBy{it.title to it.details}
                .take(MAX_REVISIONS)
            val encoded=JSONArray()
            combined.forEach {revision ->
                encoded.put(JSONObject().put("savedAt",revision.savedAt)
                    .put("title",revision.title).put("details",revision.details))
            }
            drafts.save(key(id),JSONObject().put("format",1).put("revisions",encoded))
        }
    }

    companion object {
        const val MAX_REVISIONS=12
        const val MAX_PORTABLE_NOTES=500
        const val MAX_PORTABLE_BYTES=2*1024*1024
        const val MIN_INTERVAL_MS=5*60*1000L
        private val monitor=Any()
    }
}
