package com.exmore.app.v11

import org.junit.Assert.*
import org.junit.Test

class NeoNoteDraftRecoveryTest {
    private val original="neo-attachment-12345678-1234-1234-1234-123456789abc.jpg"
    private val imported="neo-attachment-abcdefab-1234-1234-1234-123456789abc.jpg"

    @Test fun onlyRealImageBlocksAreRecovered() {
        val text="Mention $original in prose\n[[neo-image:$original:80:center]]\n" +
            "prefix [[neo-image:$original:80:center]]\n[[neo-image:$original:120:center]]"
        assertEquals(setOf(original),NeoNoteDraftRecovery.referencedImageNames(text))
        val result=NeoNoteDraftRecovery.replaceImageReference(text,original,imported)
        assertTrue(result.contains("Mention $original in prose"))
        assertTrue(result.contains("[[neo-image:$imported:80:center]]"))
        assertTrue(result.contains("prefix [[neo-image:$original:80:center]]"))
        // Both standalone image blocks are real attachments and MUST be
        // remapped so a recovered note never points at the original owner.
        assertTrue(result.contains("[[neo-image:$imported:120:center]]"))
        assertFalse(NeoNoteDraftRecovery.referencedImageNames(result).contains(original))
    }

    @Test fun unrelatedImageBlocksStayUnchanged() {
        val other="neo-attachment-99999999-1234-1234-1234-123456789abc.png"
        val text="[[neo-image:$other:40:left]]"
        assertEquals(text,NeoNoteDraftRecovery.replaceImageReference(text,original,imported))
    }
    @Test fun windowsLineEndingsKeepImageReferencesAndProseIntact() {
        val source="Mention $original in prose\r\n[[neo-image:$original:75:right]]\r\nEnd"
        assertEquals(setOf(original),NeoNoteDraftRecovery.referencedImageNames(source))
        val result=NeoNoteDraftRecovery.replaceImageReference(source,original,imported)
        assertEquals("Mention $original in prose\r\n[[neo-image:$imported:75:right]]\r\nEnd",result)
    }

    @Test fun malformedAndInlineImageTokensAreNotRewritten() {
        val source="prefix [[neo-image:$original:80:center]]\n" +
            "[[neo-image:$original:80:center]] trailing text\n" +
            "[[neo-image:$original:80:center]]"
        assertEquals(setOf(original),NeoNoteDraftRecovery.referencedImageNames(source))
        val result=NeoNoteDraftRecovery.replaceImageReference(source,original,imported)
        assertTrue(result.contains("prefix [[neo-image:$original:80:center]]"))
        assertTrue(result.contains("[[neo-image:$original:80:center]] trailing text"))
        assertTrue(result.endsWith("[[neo-image:$imported:80:center]]"))
    }

    @Test fun recoveryRequiresEveryOriginalImageReferenceToBeRemapped() {
        val second="neo-attachment-99999999-1234-1234-1234-123456789abc.png"
        val source="[[neo-image:$original:80:center]]\r\n[[neo-image:$second:60:left]]"
        val references=NeoNoteDraftRecovery.referencedImageNames(source)
        val partiallyRecovered=NeoNoteDraftRecovery.replaceImageReference(source,original,imported)
        assertFalse(NeoNoteDraftRecovery.allReferencesRemapped(references,partiallyRecovered))
        val finalRecovered=NeoNoteDraftRecovery.replaceImageReference(
            partiallyRecovered,second,"neo-attachment-aaaaaaaa-1234-1234-1234-123456789abc.png")
        assertTrue(NeoNoteDraftRecovery.allReferencesRemapped(references,finalRecovered))
        assertTrue(NeoNoteDraftRecovery.allReferencesRemapped(emptySet(), "No images"))
    }

    @Test fun missingRecoveryAttachmentsAreDetectedBeforeImport() {
        val other="neo-attachment-99999999-1234-1234-1234-123456789abc.png"
        val refs=NeoNoteDraftRecovery.referencedImageNames(
            "[[neo-image:$original:80:center]]\n[[neo-image:$other:50:left]]")
        assertEquals(setOf(other),NeoNoteDraftRecovery.missingImageNames(refs,setOf(original)))
        assertTrue(NeoNoteDraftRecovery.missingImageNames(refs,refs).isEmpty())
        assertTrue(NeoNoteDraftRecovery.missingImageNames(emptySet(),emptySet()).isEmpty())
    }
}
