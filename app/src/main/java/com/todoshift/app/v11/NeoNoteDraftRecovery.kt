package com.todoshift.app.v11

import android.content.Context

/**
 * A recovered copy has its own attachment ownership and names.
 * The original note and all of its media remain untouched.
 */
internal object NeoNoteDraftRecovery {
    // Match only complete document image blocks, never filenames mentioned in prose.
    private val imageBlock=Regex("""(?m)^\\[\\[neo-image:(neo-attachment-[A-Za-z0-9-]+\\.(?:png|jpg|pdf|txt|m4a)):([0-9]{1,3}):(left|center|right)\\]\\]$""")

    internal fun referencedImageNames(source:String):Set<String> =
        imageBlock.findAll(source).map { it.groupValues[1] }.toSet()

    internal fun replaceImageReference(source:String,oldName:String,newName:String):String {
        require(NeoAttachmentStore.safeName(oldName) && NeoAttachmentStore.safeName(newName))
        return imageBlock.replace(source) { match ->
            if(match.groupValues[1]==oldName)
                "[[neo-image:$newName:${match.groupValues[2]}:${match.groupValues[3]}]]"
            else match.value
        }
    }

    fun copyMedia(context:Context,oldId:String,newId:String,source:String):String {
        require(oldId.isNotBlank() && newId.isNotBlank() && oldId!=newId)
        val store=NeoAttachmentStore(context.applicationContext)
        var text=source
        // A failed import must not leave a partially recovered media owner.
        // The new owner is unique to this recovery operation.
        // Only import files actually referenced by the recovered draft: stale
        // owner attachments must not consume quota or cause recovery to fail.
        try {
            store.list().filter { it.owner == oldId && referencedImageNames(source).contains(it.name) }.forEach { media ->
                val imported=store.import(newId,store.uri(media.name))
                text=replaceImageReference(text,media.name,imported.name)
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
