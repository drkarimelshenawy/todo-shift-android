package com.exmore.app.appcore

import org.junit.Assert.*
import org.junit.Test

class AppCorePilotTest {
    @Test fun onlyVersionAndTimestampAreWritten() {
        val token = "a".repeat(64)
        val write = AppCorePilot.requestBody(token, 81).getJSONArray("writes").getJSONObject(0)
        val update = write.getJSONObject("update")
        assertTrue(update.getString("name").endsWith("/pairedDevices/exmore/items/$token"))
        assertEquals(setOf("versionCode"), update.getJSONObject("fields").keys().asSequence().toSet())
        assertEquals("81", update.getJSONObject("fields").getJSONObject("versionCode").getString("integerValue"))
        assertTrue(write.getJSONObject("currentDocument").getBoolean("exists"))
        assertEquals("REQUEST_TIME", write.getJSONArray("updateTransforms").getJSONObject(0).getString("setToServerValue"))
    }
    @Test fun rejectsInvalidTokenAndCode() {
        assertTrue(runCatching { AppCorePilot.requestBody("invalid", 81) }.isFailure)
        assertTrue(runCatching { AppCorePilot.requestBody("a".repeat(64), 0) }.isFailure)
    }
}
