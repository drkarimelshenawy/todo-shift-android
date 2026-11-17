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
    @Test fun arabicImeCompositionSurvivesBoldTypingAndCommit() {
        var state=NeoRichEditorState.open("").toggle("bold")
        val initial=TextFieldValue("مر",TextRange(2),TextRange(0,2))
        state=state.input(initial)
        assertEquals(initial.annotatedString,state.value.annotatedString)
        assertEquals(TextRange(0,2),state.value.composition)
        assertTrue(state.characters.all{it.bold})
        val replacing=TextFieldValue("مرحبا",TextRange(5),TextRange(0,5))
        state=state.input(replacing)
        assertEquals("مرحبا",state.value.text)
        assertEquals(TextRange(0,5),state.value.composition)
        state=state.input(TextFieldValue("مرحبا",TextRange(5)))
        assertNull(state.value.composition)
        assertTrue(state.characters.all{it.bold})
        assertEquals("<b>مرحبا</b>",state.stored())
        assertFalse(state.value.text.contains("<b>"))
    }

    @Test fun independentStyleSegmentsRemainStableWhenArabicCaretMoves() {
        var note=NeoRichEditorState.open("هنا سطر عربي")
        note=note.input(TextFieldValue(note.value.annotatedString,TextRange(1)))
        note=note.toggle("italic")
        val exported=note.stored()
        assertEquals("هنا سطر عربي",note.value.text)
        assertEquals("هنا سطر عربي",NeoRichEditorState.open(exported).value.text)
        assertTrue(note.characters.take(3).all{it.italic})
        assertTrue(note.characters.drop(3).all{!it.italic})
    }


    @Test fun numberedListContinuesOnEnterAndRichSaveRoundTrips() {
        val initial=NeoRichEditorState.open("1. ملاحظة عربية")
        val after="1. ملاحظة عربية\n"
        val appended=initial.input(TextFieldValue(after,TextRange(after.length)))
        assertEquals("1. ملاحظة عربية\n2. ",appended.value.text)
        assertEquals(appended.value.text,NeoRichEditorState.open(appended.stored()).value.text)
    }

    @Test fun checklistDoesNotShowBracketSyntaxForNewItems() {
        val created=NeoRichEditorState.open("").list("check")
        assertEquals("☐ ",created.value.text)
        val populated=created.input(TextFieldValue("☐ مراجعة",TextRange("☐ مراجعة".length)))
        val complete=populated.toggleChecklist()
        assertEquals("☑ مراجعة",complete.value.text)
        assertEquals("☑ مراجعة",NeoRichEditorState.open(complete.stored()).value.text)
    }

    @Test fun legacyCheckboxAndTaskAnchorRemainReadAndWritable() {
        val old="- [ ] بند قديم <!--task:existing-123-->"
        val read=NeoRichEditorState.open(old)
        assertEquals("- [ ] بند قديم ",read.value.text)
        val checked=read.toggleChecklist()
        assertTrue(checked.stored().startsWith("- [x] بند قديم"))
        assertTrue(checked.stored().contains("<!--task:existing-123-->"))
    }
}
