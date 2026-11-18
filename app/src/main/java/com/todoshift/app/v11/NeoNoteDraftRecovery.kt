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
        store.list().filter {it.owner==oldId}.forEach {media ->
            val imported=store.import(newId,store.uri(media.name))
            text=text.replace(media.name,imported.name)
        }
        // Linked checklist task ids belong to the original note. This copy
        // keeps the visible checklist but does not silently relink tasks.
        return text.replace(Regex(" ?<!--task:[A-Za-z0-9_-]+-->"),"")
    }
}
