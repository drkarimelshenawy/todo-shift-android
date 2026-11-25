package com.exmore.app.appcore

import android.content.Context
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.concurrent.Executors

/** Optional connected features only. Never gates local startup or storage. */
object AppCoreConfig {
    private val executor = Executors.newSingleThreadExecutor()
    private const val endpoint = "https://appcore-poc.dr-karimelshenawy.workers.dev/api/v1/apps/exmore/android/config"
    @Volatile private var current: JSONObject? = null
    fun maintenance(): Boolean = current?.takeIf { valid(it) }?.optBoolean("maintenance") ?: false
    fun flag(key: String): Boolean = current?.takeIf { valid(it) }?.optJSONObject("flags")?.optBoolean(key) ?: false
    private fun valid(value: JSONObject): Boolean {
        val now = System.currentTimeMillis()
        val expiry = value.optLong("expiresAt", 0)
        val flags = value.optJSONObject("flags") ?: return false
        return value.optInt("schemaVersion") == 1 && value.optString("projectId") == "exmore" &&
            value.optString("platform") == "android" && value.optLong("revision") > 0 &&
            expiry > now && expiry <= now + 86400000L && value.opt("maintenance") is Boolean &&
            flags.length() <= 50 && flags.keys().asSequence().all { it.matches(Regex("[a-z][a-z0-9_]{0,39}")) && flags.opt(it) is Boolean }
    }
    fun refresh(context: Context) {
        val app = context.applicationContext
        executor.execute {
            val prefs = app.getSharedPreferences("appcore_config_v1", Context.MODE_PRIVATE)
            val cached = runCatching { JSONObject(prefs.getString("config", "{}") ?: "{}") }.getOrNull()
            current = cached?.takeIf { valid(it) }
            runCatching {
                val conn = URL(endpoint).openConnection() as HttpsURLConnection
                try {
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.instanceFollowRedirects = false
                    if (conn.responseCode != 200) return@runCatching
                    val bytes = conn.inputStream.use { stream ->
                        val buffer = ByteArray(16385)
                        var size = 0
                        while (size < buffer.size) {
                            val n = stream.read(buffer, size, buffer.size - size)
                            if (n < 0) break
                            size += n
                        }
                        require(size <= 16384)
                        String(buffer, 0, size, Charsets.UTF_8)
                    }
                    val value = JSONObject(bytes)
                    if (valid(value) && value.optLong("revision") >= prefs.getLong("revision", 0)) {
                        prefs.edit().putString("config", value.toString()).putLong("revision", value.optLong("revision")).apply()
                        current = value
                    }
                } finally { conn.disconnect() }
            }
        }
    }
}
