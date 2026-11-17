package com.todoshift.app.v11.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.todoshift.app.v11.NeoNotePresentation
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoNoteBundledFontsTest {
    @Test fun sixOfflineFontsAreAvailableAlongsideLegacyValues() {
        assertEquals(listOf("system","amiri","tajawal","reem"),
            NeoNoteTypefaces.arabicChoices)
        assertEquals(listOf("system","montserrat","lora","oswald"),
            NeoNoteTypefaces.englishChoices)
        listOf("amiri","tajawal","reem","montserrat","lora","oswald",
            "serif","mono","sans").forEach {key ->
            assertTrue(key in NeoNotePresentation.FONT_KEYS)
            assertNotNull(NeoNoteTypefaces.family(key))
        }
    }

    @Test fun selectedTextFontSurvivesSerializationAndReopening() {
        val source="سلام Work"
        val state=NeoRichEditorState.open(source)
            .copy(value=TextFieldValue(source,TextRange(0,4)))
            .face("amiri")
        val stored=state.stored()
        assertTrue(stored.contains("<face key=\"amiri\">"))
        val reopened=NeoRichEditorState.open(stored)
        assertEquals(source,reopened.value.text)
        assertTrue(reopened.characters.take(4).all{it.face=="amiri"})
        assertTrue(reopened.characters.drop(5).all{it.face==null})
    }

    @Test fun explicitFaceWinsOverNoteDefaultWithoutMovingOffsets() {
        val state=NeoRichEditorState.open(
            "<face key=\"reem\">عربي</face> Latin")
        val transformed=NeoNoteTypefaces.styled(state.value.annotatedString,
            "amiri","montserrat")
        assertEquals("عربي Latin",transformed.text)
        assertTrue(transformed.spanStyles.any{
            it.item.fontFamily==NeoNoteTypefaces.family("reem")
        })
        assertTrue(transformed.spanStyles.any{
            it.item.fontFamily==NeoNoteTypefaces.family("montserrat")
        })
    }

    @Test fun styledHtmlExportsSafeFamilyInsteadOfRawInternalTag() {
        val html=NeoNoteExporter.html("Title",
            "<face key=\"lora\">Document</face>")
        assertTrue(html.contains("font-family:Lora"))
        assertFalse(html.contains("face key"))
    }
}
