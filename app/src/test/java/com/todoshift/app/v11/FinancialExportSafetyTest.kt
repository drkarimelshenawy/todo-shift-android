package com.todoshift.app.v11

import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test

class FinancialExportSafetyTest {
    @Test
    fun relockBeforeProviderReturnWritesNoFinancialBytes() {
        var allowed = true
        val output = ByteArrayOutputStream()
        var rejected = false
        try {
            NeoFinancialExport.write(
                { allowed },
                {
                    allowed = false
                    output
                },
                "private,9000",
            )
        } catch (_: IllegalStateException) {
            rejected = true
        }
        assertTrue(rejected)
        assertEquals(0, output.size())
    }

    @Test
    fun lockedExportNeverOpensDestinationAndAuthorizedExportKeepsUtf8Csv() {
        var opened = false
        try {
            NeoFinancialExport.write(
                { false },
                {
                    opened = true
                    ByteArrayOutputStream()
                },
                "private",
            )
            fail("Must refuse")
        } catch (_: IllegalStateException) {}
        assertFalse(opened)
        val out = ByteArrayOutputStream()
        NeoFinancialExport.write({ true }, { out }, "المكان,9000.50")
        assertEquals("\uFEFFالمكان,9000.50", out.toString("UTF-8"))
    }
}
