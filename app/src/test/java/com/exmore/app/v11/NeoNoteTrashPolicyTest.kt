package com.exmore.app.v11

import org.junit.Assert.*
import org.junit.Test

class NeoNoteTrashPolicyTest {
    private fun note(id:String="n",deletedAt:Long?=null,trashed:Boolean=true)=
        DayItemV11(id,"2026-11-18",null,DayItemTypeV11.NOTE,"Test",
            noteTrashed=trashed,noteTrashedAt=deletedAt)
    @Test fun legacyTrashNeverExpiresWithoutTimestamp() {
        assertTrue(NeoNoteTrashPolicy.expiredIds(listOf(note()),30,10_000_000_000L).isEmpty())
    }
    @Test fun retentionStartsAtTimeOfDeletionNotCreation() {
        val removed=1_000_000_000L
        assertTrue(NeoNoteTrashPolicy.expiredIds(listOf(note(deletedAt=removed)),30,
            removed+29*86_400_000L).isEmpty())
        assertEquals(setOf("n"),NeoNoteTrashPolicy.expiredIds(listOf(note(deletedAt=removed)),
            30,removed+30*86_400_000L))
    }
    @Test fun noAutomaticDeletionWhenDisabled() {
        assertTrue(NeoNoteTrashPolicy.expiredIds(listOf(note(deletedAt=1)),0,
            Long.MAX_VALUE).isEmpty())
    }
    @Test fun permanentRemovalOnlyAffectsTrashedNotesAndPreservesTasks() {
        val active=note("active",trashed=false)
        val trash=note("trash",deletedAt=100)
        val task=TaskV11(id="task",title="Keep",date="2026-11-18",linkedNoteId="trash")
        val next=NeoNoteTrashPolicy.remove(V11Database(dayItems=listOf(active,trash),
            tasks=listOf(task)),setOf("active","trash"))
        assertEquals(listOf("active"),next.dayItems.map{it.id})
        assertEquals(1,next.tasks.size)
        assertNull(next.tasks.single().linkedNoteId)
    }
}
