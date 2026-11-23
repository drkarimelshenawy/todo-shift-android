package com.todoshift.app.v11.ui

import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoRichFormattingV7Test {
    private fun select(state:NeoRichEditorState,start:Int,end:Int):NeoRichEditorState =
        state.copy(value=TextFieldValue(state.value.annotatedString,TextRange(start,end)))

    @Test fun highlightedSubscriptSurvivesSaveReopenAndWysiwygRendering() {
        val original=NeoRichEditorState.open("H2O in water")
        val updated=select(original,1,2).highlight("#FFF59D").script("sub")
        assertTrue(updated.stored().contains("<mark color=\"#FFF59D\">"))
        assertTrue(updated.stored().contains("<sub>2</sub>"))
        val restored=NeoRichEditorState.open(updated.stored())
        assertEquals("H2O in water",restored.value.text)
        assertEquals("#FFF59D",restored.characters[1].highlight)
        assertEquals("sub",restored.characters[1].script)
        assertNull(restored.characters[0].highlight)
        assertNull(restored.characters[2].script)
        assertTrue(restored.value.annotatedString.spanStyles.any {
            it.start<=1 && it.end>=2 && it.item.background!=androidx.compose.ui.graphics.Color.Unspecified
        })
        val plain=NeoNoteExporter.plain(updated.stored())
        assertEquals("H2O in water",plain)
    }

    @Test fun superscriptIsExclusiveWithSubscriptAndCanBeToggledOff() {
        val note=select(NeoRichEditorState.open("E=mc2"),4,5)
        assertEquals("sup",note.script("sup").characters.last().script)
        assertEquals("sub",note.script("sup").script("sub").characters.last().script)
        assertNull(note.script("sup").script("sup").characters.last().script)
        val reloaded=NeoRichEditorState.open(note.script("sup").stored())
        assertEquals("sup",reloaded.characters.last().script)
        assertTrue(reloaded.stored().contains("<sup>2</sup>"))
    }

    @Test fun paragraphAlignmentAndIndentAreLineLocalAndSurviveEditingAndSaving() {
        val start=NeoRichEditorState.open("عنوان\nParagraph\nThird")
        val first=select(start,0,2).paragraphAlign("center").paragraphIndent(1)
        assertEquals(NeoRichParagraphStyle("center",1),first.paragraphs[0])
        assertEquals(NeoRichParagraphStyle(),first.paragraphs[1])
        assertEquals(TextAlign.Center,first.value.annotatedString.paragraphStyles.first().item.textAlign)
        val second=select(first,7,16).paragraphAlign("justify").paragraphIndent(2)
        val saved=second.stored()
        assertTrue(saved.contains("<!--neo-p:center:1-->"))
        assertTrue(saved.contains("<!--neo-p:justify:2-->"))
        assertEquals(3,NeoRichEditorState.open(saved).paragraphs.size)
        assertEquals(start.value.text,NeoRichEditorState.open(saved).value.text)
        assertEquals(NeoRichParagraphStyle("center",1),NeoRichEditorState.open(saved).paragraphs[0])
        assertEquals(NeoRichParagraphStyle("justify",2),NeoRichEditorState.open(saved).paragraphs[1])
    }

    @Test fun pressingEnterInFormattedParagraphInheritsItsFormat() {
        val first=NeoRichEditorState.open("one").paragraphAlign("right").paragraphIndent(2)
        val changed=first.input(TextFieldValue("one\ntwo",TextRange(7)))
        assertEquals(2,changed.paragraphs.size)
        assertEquals(NeoRichParagraphStyle("right",2),changed.paragraphs[0])
        assertEquals(NeoRichParagraphStyle("right",2),changed.paragraphs[1])
        assertEquals(changed.value.text,NeoRichEditorState.open(changed.stored()).value.text)
    }

    @Test fun savedParagraphMetadataDoesNotBreakExistingMarkdownOrTaskLinks() {
        val source="# عنوان\n☐ بند <!--task:stable-id-->"
        val old=NeoRichEditorState.open(source)
        val first=select(old,0,3).paragraphAlign("right")
        val output=first.stored()
        assertTrue(output.startsWith("# عنوان"))
        assertTrue(output.contains("<!--task:stable-id-->"))
        assertEquals(old.value.text,NeoRichEditorState.open(output).value.text)
        assertEquals("عنوان",NeoNoteDocumentTools.headings(output).single().title)
        assertFalse(NeoNoteExporter.plain(output).contains("neo-p:"))
    }

    @Test fun docsAndHtmlCarryVisualFormattingWithoutLeakingHiddenTags() {
        val from=NeoRichEditorState.open("H2O")
        val saved=select(from,1,2).highlight("#FFF59D").script("sub")
            .paragraphAlign("center").paragraphIndent(1).stored()
        val html=NeoNoteExporter.html("Chemistry",saved)
        assertTrue(html.contains("background-color:#FFF59D"))
        assertTrue(html.contains("<sub>2</sub>"))
        assertTrue(html.contains("text-align:center"))
        assertTrue(html.contains("padding-inline-start:24px"))
        assertFalse(html.contains("neo-p:"))
        val out=ByteArrayOutputStream()
        NeoDocumentDocx.write(RuntimeEnvironment.getApplication(),"rich7","Chemistry",saved,out)
        var document=""
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            while(true) {
                val entry=zip.nextEntry ?: break
                if(entry.name=="word/document.xml") document=zip.readBytes().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        assertTrue(document.contains("<w:jc w:val=\"center\"/>"))
        assertTrue(document.contains("<w:ind w:left=\"480\"/>"))
        assertTrue(document.contains("<w:shd w:val=\"clear\" w:fill=\"FFF59D\"/>"))
        assertTrue(document.contains("<w:vertAlign w:val=\"subscript\"/>"))
        assertFalse(document.contains("neo-p:"))
    }

    @Test fun pdfStyledCharSequenceRetainsNativeBackgroundAndBaselineSpans() {
        val sub=select(NeoRichEditorState.open("H2O"),1,2)
            .highlight("#FFF59D").script("sub")
        val styled=NeoDocumentPdf.formattedLine(sub.stored()) as Spanned
        assertEquals("H2O",styled.toString())
        assertTrue(styled.getSpans(1,2,BackgroundColorSpan::class.java).isNotEmpty())
        assertTrue(styled.getSpans(1,2,SubscriptSpan::class.java).isNotEmpty())
        val sup=select(NeoRichEditorState.open("m2"),1,2).script("sup")
        val above=NeoDocumentPdf.formattedLine(sup.stored()) as Spanned
        assertTrue(above.getSpans(1,2,SuperscriptSpan::class.java).isNotEmpty())
    }

    @Test fun olderUnformattedNotesKeepTheirTextAndDataExactly() {
        val original="Plain English\nالسطر الثاني\n1. List"
        val state=NeoRichEditorState.open(original)
        assertEquals(original,state.stored())
        assertEquals(3,state.paragraphs.size)
        assertTrue(state.paragraphs.all {it==NeoRichParagraphStyle()})
    }
}
