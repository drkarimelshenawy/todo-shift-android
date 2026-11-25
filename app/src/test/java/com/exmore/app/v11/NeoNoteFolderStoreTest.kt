package com.exmore.app.v11

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

    @Test fun nestedFoldersKeepColorsLockAndSafeParents() {
        val store=NeoSmartStore(context)
        val top=store.createNoteFolder("Hospital",color="#368FC0",locked=true)
        val child=store.createNoteFolder("2026",top.id,color="#F2A23A")
        val deep=store.createNoteFolder("Notes",child.id)
        assertEquals(top.id,store.noteFolders().first{it.id==child.id}.parentId)
        assertEquals("#368FC0",store.noteFolders().first{it.id==top.id}.color)
        assertTrue(store.folderLocked(deep.id))
        assertFalse(store.folderLocked(deep.id,setOf(top.id)))
        store.moveNotesToFolder(listOf("note-a"),deep.id)
        val before=store.snapshot().toString()
        try {store.moveNoteFolder(top.id,deep.id);fail("Cycle allowed")}
        catch(_:IllegalArgumentException) {}
        assertEquals(before,store.snapshot().toString())
        store.moveNoteFolder(deep.id,null)
        assertEquals(null,store.noteFolders().first{it.id==deep.id}.parentId)
        assertFalse(store.folderLocked(deep.id))
    }

    @Test fun deleteNestedFolderReparentsWithoutDeletingNoteOrChild() {
        val store=NeoSmartStore(context)
        val root=store.createNoteFolder("Root")
        val parent=store.createNoteFolder("Parent",root.id)
        val child=store.createNoteFolder("Child",parent.id)
        store.moveNotesToFolder(listOf("note-a"),parent.id)
        store.moveNotesToFolder(listOf("note-b"),child.id)
        store.removeNoteFolder(parent.id)
        assertEquals(root.id,store.noteFolderAssignments()["note-a"])
        assertEquals(child.id,store.noteFolderAssignments()["note-b"])
        assertEquals(root.id,store.noteFolders().first{it.id==child.id}.parentId)
        assertFalse(store.noteFolders().any{it.id==parent.id})
    }

    @Test fun legacyFoldersRemainReadableAndInvalidHierarchyIsRejected() {
        val legacy=JSONObject().put("version",1)
            .put("noteFolders",org.json.JSONArray().put(JSONObject()
                .put("id","old").put("name","Old")))
        NeoSmartStore.validate(legacy)
        val store=NeoSmartStore(context)
        store.update{it.put("noteFolders",legacy.getJSONArray("noteFolders"))}
        assertEquals(null,store.noteFolders().first().parentId)
        assertEquals("#D8AD55",store.noteFolders().first().color)
        val cycle=JSONObject().put("version",1)
            .put("noteFolders",org.json.JSONArray()
                .put(JSONObject().put("id","a").put("name","A").put("parentId","b"))
                .put(JSONObject().put("id","b").put("name","B").put("parentId","a")))
        try {NeoSmartStore.validate(cycle);fail("Cycle accepted")}
        catch(_:IllegalArgumentException) {}
        val orphan=JSONObject().put("version",1)
            .put("noteFolders",org.json.JSONArray()
                .put(JSONObject().put("id","a").put("name","A").put("parentId","missing")))
        try {NeoSmartStore.validate(orphan);fail("Orphan accepted")}
        catch(_:IllegalArgumentException) {}
    }

    @Test fun siblingsCanHaveSameNameButNotSameParent() {
        val store=NeoSmartStore(context)
        val a=store.createNoteFolder("A")
        val b=store.createNoteFolder("B")
        store.createNoteFolder("Review",a.id)
        store.createNoteFolder("Review",b.id)
        try {store.createNoteFolder("REVIEW",a.id);fail("Duplicate sibling")}
        catch(_:IllegalArgumentException) {}
        assertEquals(4,store.noteFolders().size)
    }
}
