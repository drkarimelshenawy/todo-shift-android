package com.exmore.app.appcore

import android.content.Context
import android.os.SystemClock
import android.util.AtomicFile
import com.exmore.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/** Explicitly paired pilot only. Sends version and server timestamp, never user records. */
object AppCorePilot {
    private val lock = Any()
    private val worker = Executors.newSingleThreadExecutor()
    private val tokenPattern = Regex("[a-f0-9]{64}")
    private var generation = 0L
    private var busy = false
    private var next = 0L
    @Volatile var status = "idle"
        private set
    private fun store(context: Context) = AtomicFile(File(context.noBackupFilesDir, "appcore-pilot-v1"))
    private fun token(context: Context): String? = runCatching {
        store(context).readFully().toString(Charsets.UTF_8).takeIf { tokenPattern.matches(it) }
    }.getOrNull()
    fun isPaired(context: Context) = synchronized(lock) { token(context) != null }
    fun setToken(context: Context, value: String) {
        require(value.isEmpty() || tokenPattern.matches(value))
        val app = context.applicationContext
        synchronized(lock) {
            val file = store(app)
            if (value.isEmpty()) {
                file.delete()
                check(token(app) == null)
            } else {
                val stream = file.startWrite()
                try { stream.write(value.toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
                catch (e: Exception) { file.failWrite(stream); throw e }
            }
            generation++
            next = 0L
            status = if (value.isEmpty()) "idle" else "pending"
        }
        if (value.isNotEmpty()) heartbeat(app)
    }
    internal fun requestBody(token: String, code: Int): JSONObject {
        require(tokenPattern.matches(token) && code in 1..2100000000)
        val write = JSONObject()
            .put("update", JSONObject().put("name", "projects/appcore-2026/databases/(default)/documents/pairedDevices/exmore/items/$token")
                .put("fields", JSONObject().put("versionCode", JSONObject().put("integerValue", code.toString()))))
            .put("updateMask", JSONObject().put("fieldPaths", JSONArray().put("versionCode")))
            .put("currentDocument", JSONObject().put("exists", true))
            .put("updateTransforms", JSONArray().put(JSONObject().put("fieldPath", "lastSeen").put("setToServerValue", "REQUEST_TIME")))
        return JSONObject().put("writes", JSONArray().put(write))
    }
    fun heartbeat(context: Context) {
        val app = context.applicationContext
        val job = synchronized(lock) {
            val value = token(app) ?: return
            if (busy || SystemClock.elapsedRealtime() < next) return
            busy = true
            status = "pending"
            value to generation
        }
        worker.execute {
            val trace = AppCoreDiagnostics.startHeartbeatTrace(app)
            var success = false
            try {
                val conn = URL("https://firestore.googleapis.com/v1/projects/appcore-2026/databases/(default)/documents:commit").openConnection() as HttpsURLConnection
                try {
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    conn.instanceFollowRedirects = false
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    val bytes = requestBody(job.first, BuildConfig.VERSION_CODE).toString().toByteArray(Charsets.UTF_8)
                    conn.setFixedLengthStreamingMode(bytes.size)
                    if (synchronized(lock) { generation != job.second }) return@execute
                    conn.outputStream.use { it.write(bytes) }
                    success = conn.responseCode == 200
                } finally { conn.disconnect() }
            } catch (_: Exception) { /* fail offline; no credentials or payloads in logs */ }
            finally {
                runCatching { trace?.stop() }
                val changed = synchronized(lock) {
                    busy = false
                    if (generation == job.second) {
                        next = SystemClock.elapsedRealtime() + if (success) 3660000L else 60000L
                        status = if (success) "accepted" else "retry"
                        false
                    } else true
                }
                if (changed) heartbeat(app)
            }
        }
    }
}
