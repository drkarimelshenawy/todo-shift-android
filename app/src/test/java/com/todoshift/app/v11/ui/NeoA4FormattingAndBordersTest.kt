package com.todoshift.app.v11.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.todoshift.app.v11.NeoSmartStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoA4FormattingAndBordersTest {
    @Test fun selectionKeepsBoldItalicUnderlineSizeAndColorWhenCaretMovesAndPageChanges() {
        val original=NeoRichEditorState.open("السطر الأول\nالسطر التالي")
        val end="السطر الأول".length
        val selection=original.copy(value=TextFieldValue(
            original.value.annotatedString,TextRange(0,end)))
        val changed=selection.toggle("bold").toggle("italic").toggle("underline")
            .fontSize(19).ink("#1565C0")
        val moved=changed.input(TextFieldValue(changed.value.annotatedString,
            TextRange(changed.value.text.length)))
        assertTrue(moved.characters.take(end).all {
            it.bold && it.italic && it.underline && it.size==19 &&
                it.color=="#1565C0"
        })
        val stored=moved.stored()
        val restored=NeoRichEditorState.open(stored)
        assertEquals(original.value.text,restored.value.text)
        assertTrue(restored.characters.take(end).all {
            it.bold && it.italic && it.underline && it.size==19 &&
                it.color=="#1565C0"
        })
        assertTrue(restored.characters.drop(end+1).all { !it.bold && !it.underline })
    }

    @Test fun a4CanStartParagraphInRemainingSpaceThenContinueOnNextSheet() {
        val starts=intArrayOf(0,10,20,30)
        val tops=intArrayOf(0,20,40,60)
        val bottoms=floatArrayOf(20f,40f,60f,80f)
        val fragments=NeoA4SoftWrap.slices(40,starts,tops,bottoms,80,60,24)
        assertEquals(2,fragments.size)
        assertEquals(NeoA4SoftWrap.Range(0,10),
            NeoA4SoftWrap.Range(fragments[0].start,fragments[0].end))
        assertEquals(NeoA4SoftWrap.Range(10,40),
            NeoA4SoftWrap.Range(fragments[1].start,fragments[1].end))
        assertEquals(80,fragments.sumOf{it.heightPx})
    }

    @Test fun pageBorderStylesAreBoundedAndEmitValidWordBorders() {
        assertEquals(5,NeoPageBorders.styles.distinct().size)
        assertEquals(NeoPageBorders.NONE,NeoPageBorders.normalize("invalid"))
        assertEquals("",NeoPageBorders.wordBorderXml(NeoPageBorders.NONE))
        for(style in NeoPageBorders.styles.drop(1)) {
            val xml=NeoPageBorders.wordBorderXml(style)
            assertTrue(xml.contains("<w:pgBorders"))
            assertTrue(xml.contains("w:offsetFrom=\"page\""))
            assertEquals(4,"w:val=\\\"".toRegex().findAll(xml).count())
            assertTrue(xml.contains("w:space=\"20\""))
        }
    }

    @Test fun docxUsesSavedPageBorderButNeverMovesTextIntoFrame() {
        val ctx=RuntimeEnvironment.getApplication()
        val id="v68-border-test-note"
        val store=NeoSmartStore(ctx)
        store.update { o ->
            val all=o.optJSONObject("notePageBorders")?:JSONObject()
            all.put(id,NeoPageBorders.DOUBLE)
            o.put("notePageBorders",all)
        }
        try {
            assertEquals(NeoPageBorders.DOUBLE,NeoPageBorders.load(store,id))
            val out=ByteArrayOutputStream()
            NeoDocumentDocx.write(ctx,id,"عنوان المستند","متن عربي",out)
            var doc=""
            ZipInputStream(ByteArrayInputStream(out.toByteArray())).use {zip ->
                while(true) {
                    val entry=zip.nextEntry?:break
                    if(entry.name=="word/document.xml")doc=zip.readBytes().toString(Charsets.UTF_8)
                    zip.closeEntry()
                }
            }
            assertTrue(doc.contains("w:val=\"double\""))
            assertTrue(doc.contains("w:top=\"1134\""))
            assertTrue(doc.contains("متن عربي"))
        } finally {
            store.update { o ->
                o.optJSONObject("notePageBorders")?.remove(id)
            }
        }
    }
}
