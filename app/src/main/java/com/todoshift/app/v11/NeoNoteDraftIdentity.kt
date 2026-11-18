package com.todoshift.app.v11

/**
 * Recover automatically only when a draft belongs to the note being opened.
 * New notes used to share a date-scoped key, so the "<new>" fingerprint
 * by itself must never turn a fresh note into an unrelated older note.
 */
internal object NeoNoteDraftIdentity {
    fun canAutoRecover(
        draftId:String,
        openedId:String,
        draftSource:String,
        revision:String,
        currentSource:String?
    ):Boolean {
        if(draftId.isBlank() || draftId!=openedId) return false
        val oldCurrent=currentSource?.replace(", notePreviewHidden=false","")
        val oldRevision=revision.replace(", notePreviewHidden=false","")
        return draftSource==revision ||
            (currentSource!=null && draftSource==currentSource) ||
            draftSource==oldRevision ||
            (oldCurrent!=null && draftSource==oldCurrent) ||
            (oldCurrent!=null &&
                draftSource==oldCurrent.replace(", noteCalendarLinked=true",""))
    }
}
