package com.exmore.app.v11

import org.junit.Assert.*
import org.junit.Test

class NeoNoteDraftIdentityTest {
    @Test fun newNoteIsNotHijackedByAnotherNewNotesDateScopedDraft() {
        assertFalse(NeoNoteDraftIdentity.canAutoRecover(
            "old-note","fresh-note","<new>","<new>",null))
    }

    @Test fun existingNoteAcceptsMatchingOwnRevision() {
        assertTrue(NeoNoteDraftIdentity.canAutoRecover(
            "note-1","note-1","before","before","now"))
    }

    @Test fun existingNoteNeverAcceptsChangedStoredRevision() {
        assertFalse(NeoNoteDraftIdentity.canAutoRecover(
            "note-1","note-1","old-draft","current","current"))
    }

    @Test fun olderOptionalPreviewFingerprintRemainsRecoverable() {
        assertTrue(NeoNoteDraftIdentity.canAutoRecover(
            "note-1","note-1",
            "DayItem(id=1)",
            "DayItem(id=1, notePreviewHidden=false)",null))
    }

    @Test fun blankOrMissingDraftIdentityIsNotSafeToApply() {
        assertFalse(NeoNoteDraftIdentity.canAutoRecover(
            "","note-1","<new>","<new>",null))
    }
}
