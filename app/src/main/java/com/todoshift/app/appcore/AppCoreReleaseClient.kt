package com.todoshift.app.appcore

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Blocking lookup: callers must use a background thread. Never installs an APK. */
object AppCoreReleaseClient {
    data class Release(val code: Int, val version: String, val url: String)
    fun check(currentCode: Int): Release? {
        require(currentCode >= 0)
        val connection = URL("https://firestore.googleapis.com/v1/projects/appcore-2026/databases/(default)/documents/publishedReleases/exmore").openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = false
            if (connection.responseCode == 404) return null
            check(connection.responseCode == 200) { "Release lookup unavailable" }
            val fields = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONObject("fields")
            check(fields.getJSONObject("appId").getString("stringValue") == "exmore")
            check(!fields.getJSONObject("mandatory").getBoolean("booleanValue"))
            val code = fields.getJSONObject("code").getString("integerValue").toInt()
            require(code in 1..2100000000)
            val version = fields.getJSONObject("version").getString("stringValue")
            val url = URL(fields.getJSONObject("url").getString("stringValue"))
            require(url.protocol == "https" && url.userInfo == null && url.host.isNotBlank())
            return if (code > currentCode) Release(code, version, url.toExternalForm()) else null
        } finally {
            connection.disconnect()
        }
    }
}
