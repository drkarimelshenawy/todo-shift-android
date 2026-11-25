package com.exmore.app.v11.ui

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoDocumentDocxTest {
    private fun export(text:String,title:String="Doc"):Map<String,ByteArray> {
        val bytes=ByteArrayOutputStream()
        NeoDocumentDocx.write(RuntimeEnvironment.getApplication(),"note-test",
            title,text,bytes)
        val files=linkedMapOf<String,ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes.toByteArray())).use {zip ->
            while(true) {
                val entry=zip.nextEntry ?: break
                files[entry.name]=zip.readBytes()
                zip.closeEntry()
            }
        }
        return files
    }

    private fun xmlDocument(value:ByteArray):org.w3c.dom.Document {
        val factory=DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware=true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false)
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(value))
    }

    @Test fun docxProducesValidOfficePartsWithEditableTableAndPageBreak() {
        val body="# Heading\nالعنوان\n"+
            NeoDocumentBlocks.table(3,2)+"\n"+
            NeoDocumentBlocks.PAGE_BREAK+"\nEnd"
        val files=export(body,"Test Document")
        assertTrue(files.keys.containsAll(listOf(
            "[Content_Types].xml","_rels/.rels",
            "word/document.xml","word/_rels/document.xml.rels")))
        files.filterKeys{it.endsWith(".xml")||it.endsWith(".rels")}
            .forEach{(_,bytes)->xmlDocument(bytes)}
        val xml=String(files.getValue("word/document.xml"),Charsets.UTF_8)
        assertTrue(xml.contains("Test Document"))
        assertTrue(xml.contains("العنوان"))
        assertTrue(xml.contains("<w:tbl>"))
        assertTrue(xml.contains("<w:tc>"))
        assertTrue(xml.contains("w:type=\"page\""))
        assertTrue(xml.contains("<w:pgSz w:w=\"11906\" w:h=\"16838\""))
    }

    @Test fun formattedTextPreservesBoldArabicAndSelectedFontFamily() {
        val content="<face key=\"amiri\"><b>مرحبا</b></face>\n"+
            "<face key=\"oswald\">Hello</face>"
        val xml=String(export(content).getValue("word/document.xml"),Charsets.UTF_8)
        assertTrue(xml.contains("مرحبا"))
        assertTrue(xml.contains("<w:b/>"))
        assertTrue(xml.contains("w:ascii=\"Amiri\""))
        assertTrue(xml.contains("w:ascii=\"Oswald\""))
        assertTrue(xml.contains("w:bidi w:val=\"1\""))
    }

    @Test fun missingPrivateImagesDoNotLeakNamesOrDocumentStoragePaths() {
        val name="neo-attachment-01234567-89ab-cdef-0123-456789abcdef.png"
        val body="Before\n"+NeoDocumentBlocks.image(name)+"\nAfter"
        val files=export(body)
        assertFalse(files.keys.any{it.startsWith("word/media/")})
        val xml=String(files.getValue("word/document.xml"),Charsets.UTF_8)
        assertTrue(xml.contains("[Image unavailable]"))
        assertFalse(xml.contains(name))
        assertFalse(xml.contains("filesDir"))
    }

    @Test fun userMarkupAndControlCharactersAreEscapedAsDocumentText() {
        val files=export("<script>hello & bye</script>")
        val xml=String(files.getValue("word/document.xml"),Charsets.UTF_8)
        assertTrue(xml.contains("&lt;script&gt;"))
        assertTrue(xml.contains("&amp; bye"))
        assertFalse(xml.contains("<script>"))
        xmlDocument(files.getValue("word/document.xml"))
    }
}
