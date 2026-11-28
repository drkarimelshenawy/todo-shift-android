package com.exmore.app.appcore

import android.content.Context
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.concurrent.Executors

/** Optional connected features only. Never gates local startup or storage. */
object AppCoreConfig {
    private val refreshing = java.util.concurrent.atomic.AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private const val base = "https://appcore-poc.dr-karimelshenawy.workers.dev/api/v1/"
    private val scopeLock = Any()
    private var epoch = 0L
    @Volatile private var current: JSONObject? = null
    internal fun endpoint(pilot: Boolean) = base + (if (pilot) "pilot/" else "") + "apps/exmore/android/config"
    internal fun storageKey(pilot: Boolean) = "appcore_config_v1" + if (pilot) "_pilot" else ""
    fun invalidateScope() = synchronized(scopeLock) { epoch++; current = null }
    private fun update(value: JSONObject?, ticket: Long) = synchronized(scopeLock) {
        if (ticket == epoch) current = value
    }
    fun maintenance(): Boolean = current?.takeIf { valid(it) }?.optBoolean("maintenance") ?: false
    fun flag(key: String): Boolean = current?.takeIf { valid(it) }?.optJSONObject("flags")?.optBoolean(key) ?: false
    private fun integer(value: Any?): Long? = when (value) {
        is Int -> value.toLong()
        is Long -> value.takeIf { it in -9007199254740991L..9007199254740991L }
        else -> null
    }
    private fun valid(value: JSONObject): Boolean {
        val now = System.currentTimeMillis()
        val expiry = integer(value.opt("expiresAt")) ?: return false
        val revision = integer(value.opt("revision")) ?: return false
        if (value.keys().asSequence().toSet() != setOf("schemaVersion", "projectId", "platform", "revision", "expiresAt", "maintenance", "flags")) return false
        val flags = value.optJSONObject("flags") ?: return false
        return integer(value.opt("schemaVersion")) == 1L && value.opt("projectId") == "exmore" &&
            value.opt("platform") == "android" && revision > 0 &&
            expiry > now && expiry <= now + 86400000L && value.opt("maintenance") is Boolean &&
            flags.length() <= 50 && flags.keys().asSequence().all { it.matches(Regex("[a-z][a-z0-9_]{0,39}")) && it !in setOf("constructor", "prototype") && flags.opt(it) is Boolean }
    }
    fun refresh(context: Context, onComplete: (() -> Unit)? = null) {
        val app = context.applicationContext
        if (!refreshing.compareAndSet(false, true)) return
        val ticket = synchronized(scopeLock) { epoch }
        val pilot = AppCorePilot.isPaired(app)
        executor.execute {
            try {
            val prefs = app.getSharedPreferences(storageKey(pilot), Context.MODE_PRIVATE)
            val cached = runCatching { JSONObject(prefs.getString("config", "{}") ?: "{}") }.getOrNull()
            update(cached?.takeIf { valid(it) && it.optLong("revision") >= prefs.getLong("revision", 0) }, ticket)
            runCatching {
                val conn = URL(endpoint(pilot)).openConnection() as HttpsURLConnection
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
                        update(value, ticket)
                    }
                } finally { conn.disconnect() }
            }
            } finally {
                refreshing.set(false)
                if (synchronized(scopeLock) { epoch == ticket }) onComplete?.invoke()
            }
        }
    }
}
