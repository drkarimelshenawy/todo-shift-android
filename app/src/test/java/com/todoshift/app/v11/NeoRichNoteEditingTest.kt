package com.todoshift.app.v11

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.todoshift.app.v11.ui.NeoRichEditorState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NeoRichNoteEditingTest {
    @Test fun typingStyleStaysEnabledUntilToggledOffWithoutInsertingStars() {
        var note=NeoRichEditorState.open("")
        note=note.toggle("bold")
        assertTrue(note.active("bold"))
        note=note.input(TextFieldValue("hello",TextRange(5)))
        assertEquals("hello",note.value.text)
        assertTrue(note.characters.all{it.bold})
        assertTrue(note.active("bold"))

        note=note.toggle("bold")
        assertFalse(note.active("bold"))
        note=note.input(TextFieldValue("hello world",TextRange(11)))
        assertTrue(note.characters.take(5).all{it.bold})
        assertTrue(note.characters.drop(5).all{!it.bold})
        assertEquals("hello world",NeoRichEditorState.open(note.stored()).value.text)
        assertFalse(note.stored().contains("**"))
    }

    @Test fun caretInsideWordFormatsTheWholeWordButNotNeighbor() {
        val original=NeoRichEditorState.open("one two three")
        val inside=original.copy(value=TextFieldValue(original.value.annotatedString,TextRange(5)))
        val result=inside.toggle("bold")
        assertTrue(result.characters.subList(4,7).all{it.bold})
        assertTrue(result.characters.subList(0,3).all{!it.bold})
        assertEquals("one two three",NeoRichEditorState.open(result.stored()).value.text)
    }

    @Test fun selectedWordsRetainMixedStylesAfterSavingAndReopening() {
        val start=NeoRichEditorState.open("عربي English")
        val selected=start.copy(value=TextFieldValue(start.value.annotatedString,TextRange(0,4)))
        val formatted=selected.toggle("underline").fontSize(24).ink("#1565C0")
        val reopened=NeoRichEditorState.open(formatted.stored())
        assertEquals("عربي English",reopened.value.text)
        assertTrue(reopened.characters.take(4).all{it.underline && it.size==24 && it.color=="#1565C0"})
        assertTrue(reopened.characters.drop(4).all{!it.underline})
    }

    @Test fun oldMarkdownIsDecodedAndTaskIdsSurviveEdits() {
        val original="**Old bold**\n- [ ] do work <!--task:existing-123-->"
        var note=NeoRichEditorState.open(original)
        assertFalse(note.value.text.contains("**"))
        assertFalse(note.value.text.contains("<!--task:"))
        assertTrue(note.characters.first().bold)
        val before=note.value.text
        note=note.input(TextFieldValue(before+" tonight",TextRange(before.length+8)))
        val saved=note.stored()
        assertTrue("Linked task marker must be preserved",saved.contains("<!--task:existing-123-->"))
        val reopened=NeoRichEditorState.open(saved)
        assertEquals(before+" tonight",reopened.value.text)
        assertTrue(reopened.characters.first().bold)
    }

    @Test fun cursorSelectionOnlyMustNotEraseTextStyles() {
        var state=NeoRichEditorState.open("<b>abc</b>")
        state=state.input(TextFieldValue("abc",TextRange(1)))
        assertTrue(state.characters.all{it.bold})
        assertEquals("<b>abc</b>",state.stored())
    }
}
