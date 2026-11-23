package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

class NeoNoteFormatVisualChoicesTest {
    @Test fun nineDistinctListMarkersPreserveExistingEditorStyleIds() {
        assertEquals(
            listOf("bullet", "circle", "square", "check", "number", "numberParen",
                "letter", "letterParen", "arabic"),
            neoNoteListKinds,
        )
        val examples = neoNoteListKinds.map(::neoNoteListPreview)
        assertEquals(9, examples.toSet().size)
        assertTrue(examples.all { it.first.isNotBlank() && it.second.isNotBlank() })
        assertEquals("☐" to "☑", neoNoteListPreview("check"))
        assertEquals("1)" to "2)", neoNoteListPreview("numberParen"))
        assertEquals("A." to "B.", neoNoteListPreview("letter"))
        assertEquals("أ." to "ب.", neoNoteListPreview("arabic"))
    }

    @Test fun borderThumbnailsUseUnchangedWordPdfAndA4Styles() {
        assertEquals(NeoPageBorders.styles, neoNotePageBorderChoices)
        assertEquals(listOf("none", "single", "double", "dashed", "dotted"),
            neoNotePageBorderChoices)
        neoNotePageBorderChoices.forEach {
            assertTrue(NeoPageBorders.valid(it))
            assertEquals(it, NeoPageBorders.normalize(it))
        }
    }
}
