package com.todoshift.app.v11

import android.content.Context
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class NeoNoteFolderStoreTest {
    private val context:Context get()=RuntimeEnvironment.getApplication()
    private fun reset() {
        assertTrue(context.getSharedPreferences(NeoSmartStore.NAME,Context.MODE_PRIVATE)
            .edit().clear().commit())
    }
    @Before fun setup()=reset()
    @After fun teardown()=reset()

    @Test fun createRenameMoveAndDeletePreservesNoteMembershipSafely() {
        val store=NeoSmartStore(context)
        val work=store.createNoteFolder("العمل")
        val personal=store.createNoteFolder("Personal")
        assertEquals(2,store.noteFolders().size)
        store.moveNotesToFolder(listOf("note1","note2"),work.id)
        assertEquals(work.id,store.noteFolderAssignments()["note1"])
        store.renameNoteFolder(work.id,"شيفتات")
        assertEquals("شيفتات",store.noteFolders().first{it.id==work.id}.name)
        store.moveNotesToFolder(listOf("note1"),personal.id)
        assertEquals(personal.id,store.noteFolderAssignments()["note1"])
        store.removeNoteFolder(work.id)
        assertEquals(personal.id,store.noteFolderAssignments()["note1"])
        assertFalse(store.noteFolderAssignments().containsKey("note2"))
        assertEquals(listOf("Personal"),store.noteFolders().map{it.name})
        store.moveNotesToFolder(listOf("note1"),null)
        assertTrue(store.noteFolderAssignments().isEmpty())
    }

    @Test fun duplicateNamesAndUnknownFoldersDoNotOverwriteData() {
        val store=NeoSmartStore(context)
        val original=store.createNoteFolder("Work")
        try {store.createNoteFolder(" work ");fail("Duplicate folder must fail")}
        catch(_:IllegalArgumentException) {}
        try {store.moveNotesToFolder(listOf("note1"),"not-a-folder");fail("Unknown folder must fail")}
        catch(_:IllegalArgumentException) {}
        assertEquals(listOf(original),store.noteFolders())
        assertTrue(store.noteFolderAssignments().isEmpty())
    }

    @Test fun importedAssignmentsMustReferToRealFolders() {
        val invalid=JSONObject().put("version",1)
            .put("noteFolderAssignments",JSONObject().put("note1","unknown"))
        try {NeoSmartStore.validate(invalid);fail("Orphan folder reference accepted")}
        catch(_:IllegalArgumentException) {}
    }

    @Test fun favoriteNotesAreIndependentFromFoldersAndPersist() {
        val store=NeoSmartStore(context)
        val folder=store.createNoteFolder("Ward")
        store.moveNotesToFolder(listOf("note-1"),folder.id)
        store.setNoteFavorites(listOf("note-1","note-2"),true)
        assertEquals(setOf("note-1","note-2"),NeoSmartStore(context).noteFavorites())
        assertEquals(folder.id,store.noteFolderAssignments()["note-1"])
        store.setNoteFavorites(listOf("note-1"),false)
        assertEquals(setOf("note-2"),store.noteFavorites())
        assertEquals(folder.id,store.noteFolderAssignments()["note-1"])
    }

    @Test fun invalidFavoriteValueIsRejectedDuringBackupValidation() {
        val invalid=JSONObject().put("version",1)
            .put("noteFavorites",JSONObject().put("note-1",false))
        try {NeoSmartStore.validate(invalid);fail("False favorite metadata must not be imported")}
        catch(_:IllegalArgumentException) {}
    }
}
