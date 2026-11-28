package com.exmore.app.appcore

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AppCoreReleaseClientTest {
    private fun fields(code: Any = "82", project: String = "exmore", mandatory: Any = false, url: String = "https://example.org/release.apk", version: String = "4.4.3") =
        JSONObject().put("appId", JSONObject().put("stringValue", project))
            .put("code", JSONObject().put("integerValue", code))
            .put("mandatory", JSONObject().put("booleanValue", mandatory))
            .put("url", JSONObject().put("stringValue", url))
            .put("version", JSONObject().put("stringValue", version))

    @Test fun onlyNewerOptionalReleaseIsReported() {
        assertEquals(82, AppCoreReleaseClient.decode(fields(), 81)?.code)
        assertNull(AppCoreReleaseClient.decode(fields(), 82))
        assertNull(AppCoreReleaseClient.decode(fields(), 83))
    }
    @Test fun rejectsInvalidOrForeignReleases() {
        for (value in listOf(fields(project = "altabloh"), fields(mandatory = true),
            fields(mandatory = "false"), fields(code = 82), fields(code = "082"),
            fields(url = "http://example.org/file"), fields(url = "https://user@example.org/file"),
            fields(url = "https://example.org:8080/file"), fields(version = ""))) {
            assertTrue(runCatching { AppCoreReleaseClient.decode(value, 81) }.isFailure)
        }
    }
}
