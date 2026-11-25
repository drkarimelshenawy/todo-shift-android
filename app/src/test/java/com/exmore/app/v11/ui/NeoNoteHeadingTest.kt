package com.exmore.app.v11.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NeoNoteHeadingTest {
    @Test fun headingCanReturnToNormalWithoutChangingNextParagraph() {
        val source="الموضوع\nالتفاصيل"
        val start=NeoRichEditorState.open(source)
        val selected=start.copy(value=start.value.copy(selection=TextRange(3)))
        val h1=neoNoteHeading(selected,1)
        assertEquals("# الموضوع\nالتفاصيل",h1.value.text)
        val normal=neoNoteHeading(h1,0)
        assertEquals(source,normal.value.text)
    }

    @Test fun headingOnlyRewritesSelectedLine() {
        val source="One\nTwo"
        val start=NeoRichEditorState.open(source)
        val selected=start.copy(value=TextFieldValue(source,TextRange(5)))
        assertEquals("One\n## Two",neoNoteHeading(selected,2).value.text)
    }
}
