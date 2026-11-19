package com.todoshift.app.v11

import android.content.Context

/**
 * A recovered copy has its own attachment ownership and names.
 * The original note and all of its media remain untouched.
 */
internal object NeoNoteDraftRecovery {
    fun copyMedia(context:Context,oldId:String,newId:String,source:String):String {
        require(oldId.isNotBlank() && newId.isNotBlank() && oldId!=newId)
        val store=NeoAttachmentStore(context.applicationContext)
        var text=source
        // A failed import must not leave a partially recovered media owner.
        // The new owner is unique to this recovery operation.
        // Only import files actually referenced by the recovered draft: stale
        // owner attachments must not consume quota or cause recovery to fail.
        try {
            store.list().filter { it.owner == oldId && source.contains(it.name) }.forEach { media ->
                val imported=store.import(newId,store.uri(media.name))
                text=text.replace(media.name,imported.name)
            }
        } catch (failure:Exception) {
            try {
                store.purgeOwners(setOf(newId))
            } catch (cleanupFailure:Exception) {
                failure.addSuppressed(cleanupFailure)
            }
            throw failure
        }
        // Linked checklist task ids belong to the original note. This copy
        // keeps the visible checklist but does not silently relink tasks.
        return text.replace(Regex(" ?<!--task:[A-Za-z0-9_-]+-->"),"")
    }
}
