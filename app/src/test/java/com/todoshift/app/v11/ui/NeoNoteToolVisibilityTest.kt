package com.todoshift.app.v11.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeoNoteToolVisibilityTest {
    @Test fun quickNoteStartsWithCleanWorkspace() {
        val state=neoNoteToolVisibility(
            editing=true,hasTextSelection=false,formattingRequested=false,advancedRequested=false)
        assertFalse(state.showFormatting)
        assertFalse(state.showDocumentTools)
    }

    @Test fun textSelectionShowsFormattingWithoutAdvancedWorkspace() {
        val state=neoNoteToolVisibility(
            editing=true,hasTextSelection=true,formattingRequested=false,advancedRequested=false)
        assertTrue(state.showFormatting)
        assertFalse(state.showDocumentTools)
    }

    @Test fun advancedWorkspaceKeepsEditorAndDocumentActionsAvailable() {
        val state=neoNoteToolVisibility(
            editing=true,hasTextSelection=false,formattingRequested=false,advancedRequested=true)
        assertTrue(state.showFormatting)
        assertTrue(state.showDocumentTools)
    }

    @Test fun readingModeNeverShowsWritingToolbar() {
        val state=neoNoteToolVisibility(
            editing=false,hasTextSelection=true,formattingRequested=true,advancedRequested=true)
        assertFalse(state.showFormatting)
        assertTrue(state.showDocumentTools)
    }
}
