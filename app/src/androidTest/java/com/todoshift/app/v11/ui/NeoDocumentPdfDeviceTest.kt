package com.todoshift.app.v11.ui

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Exercises native PdfDocument and PdfRenderer on Android, not Robolectric mocks. */
@RunWith(AndroidJUnit4::class)
class NeoDocumentPdfDeviceTest {
    private fun verifyPdf(source: String, expectedMinimumPages: Int): Int {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("notes-a4-", ".pdf", context.cacheDir)
        try {
            val exported = FileOutputStream(file).use { output ->
                NeoDocumentPdf.write(context, "pdf-test-no-attachments", "اختبار A4", source, output)
            }
            assertTrue("PDF export produced no bytes", file.length() > 100)
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    assertEquals("Exported page count disagrees with PDF", exported, renderer.pageCount)
                    assertTrue("Document lost pages", renderer.pageCount >= expectedMinimumPages)
                    for (index in 0 until renderer.pageCount) {
                        renderer.openPage(index).use { page ->
                            assertEquals("A4 width in PDF points", 595, page.width)
                            assertEquals("A4 height in PDF points", 842, page.height)
                        }
                    }
                }
            }
            return exported
        } finally {
            file.delete()
        }
    }

    @Test fun longArabicEnglishParagraphSpansMultipleRealA4Pages() {
        val source = (1..1600).joinToString(" ") { "كلمة English" }
        verifyPdf(source, expectedMinimumPages = 2)
    }

    @Test fun trailingManualBreakDoesNotCreateEmptyPdfPage() {
        val baseline = verifyPdf("Body", expectedMinimumPages = 1)
        val withTrailingBreak = verifyPdf(
            "Body\n" + NeoDocumentBlocks.PAGE_BREAK, expectedMinimumPages = 1
        )
        assertEquals("Trailing page break created an empty page", baseline, withTrailingBreak)
    }

    @Test fun longMultilingualTableCellExportsReadableA4Pages() {
        val longCell = (1..1100).joinToString(" ") { "عربي English" }
        val source = NeoDocumentBlocks.serializeTable(
            listOf(listOf("الوصف", "Description"), listOf(longCell, "Value"))
        )
        verifyPdf(source, expectedMinimumPages = 2)
    }
}
