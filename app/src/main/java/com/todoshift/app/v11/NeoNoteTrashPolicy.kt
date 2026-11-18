package com.todoshift.app.v11

/** Conservative retention: legacy notes without a deletion timestamp are never expired. */
internal object NeoNoteTrashPolicy {
    fun expiredIds(notes:List<DayItemV11>,days:Int,now:Long):Set<String> {
        if(days==0 || days !in setOf(7,15,30,60,90)) return emptySet()
        val span=days * 86_400_000L
        return notes.asSequence().filter { note ->
            val removed=note.noteTrashedAt
            note.type==DayItemTypeV11.NOTE && note.noteTrashed &&
                removed!=null && removed>0 && now>=removed && now-removed>=span
        }.map {it.id}.toSet()
    }

    /** Keep tasks; unlink their deleted notes to avoid dangling references. */
    fun remove(db:V11Database,requested:Set<String>):V11Database {
        val eligible=db.dayItems.asSequence()
            .filter{it.type==DayItemTypeV11.NOTE && it.noteTrashed && it.id in requested}
            .map{it.id}.toSet()
        if(eligible.isEmpty()) return db
        return db.copy(
            dayItems=db.dayItems.filterNot{it.id in eligible},
            tasks=db.tasks.map{if(it.linkedNoteId in eligible) it.copy(linkedNoteId=null) else it}
        )
    }

    fun daysRemaining(note:DayItemV11,days:Int,now:Long):Long? {
        val removed=note.noteTrashedAt ?: return null
        if(!note.noteTrashed || days<=0 || removed<=0) return null
        val due=removed+days*86_400_000L
        return ((due-now).coerceAtLeast(0L)+86_399_999L)/86_400_000L
    }
}
