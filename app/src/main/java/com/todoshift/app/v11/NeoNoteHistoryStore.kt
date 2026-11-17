package com.todoshift.app.v11

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Device-encrypted local note revisions, separate from the working draft.
 *
 * No plaintext is written to the smart metadata preferences. The underlying
 * draft cipher uses Android Keystore and atomic, AAD-bound files. The local
 * revision history intentionally does not become part of portable backup
 * exports until versioned backup/restore support is implemented.
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

    companion object {
        const val MAX_REVISIONS=12
        const val MIN_INTERVAL_MS=5*60*1000L
        private val monitor=Any()
    }
}
