package com.exmore.app.v11.ui

/** Presentation-only mode: never changes the note source or persistence format. */
internal data class NeoNoteToolVisibility(
    val showFormatting:Boolean,
    val showDocumentTools:Boolean
)

/** Selection formatting is discoverable without forcing the document workspace open. */
internal fun neoNoteToolVisibility(
    editing:Boolean,
    hasTextSelection:Boolean,
    formattingRequested:Boolean,
    advancedRequested:Boolean
):NeoNoteToolVisibility = NeoNoteToolVisibility(
    showFormatting=editing && (hasTextSelection || formattingRequested || advancedRequested),
    showDocumentTools=advancedRequested
)
