package com.exmore.app.appcore

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Blocking lookup: callers must use a background thread. Never installs an APK. */
object AppCoreReleaseClient {
    const val PROJECT_ID = "exmore"
    data class Release(val code: Int, val version: String, val url: String)
    internal fun decode(fields: JSONObject, currentCode: Int): Release? {
        require(currentCode >= 0)
        require(fields.getJSONObject("appId").get("stringValue") == PROJECT_ID)
        require(fields.getJSONObject("mandatory").get("booleanValue") == false)
        val raw = fields.getJSONObject("code").get("integerValue")
        require(raw is String && raw.matches(Regex("[1-9][0-9]{0,9}")))
        val code = raw.toInt()
        require(code in 1..2100000000)
        val version = fields.getJSONObject("version").get("stringValue")
        require(version is String && version.isNotBlank() && version.length <= 100 && version.none { it.isISOControl() })
        val rawUrl = fields.getJSONObject("url").get("stringValue")
        require(rawUrl is String && rawUrl.length <= 2048 && rawUrl.none { it.isWhitespace() || it.isISOControl() })
        val url = URL(rawUrl)
        require(url.protocol == "https" && url.userInfo == null && url.host.isNotBlank() && url.port in setOf(-1, 443))
        return if (code > currentCode) Release(code, version, url.toExternalForm()) else null
    }
    fun check(currentCode: Int): Release? {
        require(currentCode >= 0)
        val connection = URL("https://firestore.googleapis.com/v1/projects/appcore-2026/databases/(default)/documents/publishedReleases/$PROJECT_ID").openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            if (connection.responseCode == 404) return null
            check(connection.responseCode == 200) { "Release lookup unavailable" }
            val bytes = connection.inputStream.use { stream ->
                val buffer = ByteArray(65537)
                var size = 0
                while (size < buffer.size) {
                    val n = stream.read(buffer, size, buffer.size - size)
                    if (n < 0) break
                    size += n
                }
                require(size <= 65536)
                String(buffer, 0, size, Charsets.UTF_8)
            }
            return decode(JSONObject(bytes).getJSONObject("fields"), currentCode)
        } finally {
            connection.disconnect()
        }
    }
}
