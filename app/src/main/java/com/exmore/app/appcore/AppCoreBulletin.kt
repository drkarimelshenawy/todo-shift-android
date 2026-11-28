package com.exmore.app.appcore

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AppCoreBulletin {
    private val executor = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    @Volatile private var lastProduction = -600000L
    @Volatile private var lastPilot = -600000L
    internal fun storageKey(pilot: Boolean) = "appcore_bulletins" + if (pilot) "_pilot" else ""
    internal fun endpoint(pilot: Boolean) = "https://firestore.googleapis.com/v1/projects/appcore-2026/databases/(default)/documents/" + (if (pilot) "pilotBulletins" else "publicBulletins") + "/exmore"
    fun check(activity: Activity) {
        if (AppCoreConfig.maintenance() || AppCoreConfig.flag("disable_bulletins")) return
        val now = android.os.SystemClock.elapsedRealtime()
        val pilot = AppCorePilot.isPaired(activity.applicationContext)
        if (now - (if (pilot) lastPilot else lastProduction) < 300000L || !busy.compareAndSet(false,true)) return
        if (pilot) lastPilot = now else lastProduction = now
        executor.execute {
            try {
                val prefs = activity.applicationContext.getSharedPreferences(storageKey(pilot), Context.MODE_PRIVATE)
                val conn = URL(endpoint(pilot)).openConnection() as HttpsURLConnection
                val fields = try {
                    conn.connectTimeout=5000; conn.readTimeout=5000; conn.instanceFollowRedirects=false
                    if(conn.responseCode!=200) return@execute
                    val bytes=conn.inputStream.use { stream ->
                        val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(1024)
                        while(out.size()<=16384){ val n=stream.read(buffer); if(n<0)break; out.write(buffer,0,n) }; out.toByteArray()
                    }
                    require(bytes.size<=16384)
                    JSONObject(String(bytes,Charsets.UTF_8)).getJSONObject("fields")
                } finally { conn.disconnect() }
                if(fields.getJSONObject("appId").getString("stringValue")!="exmore") return@execute
                if(!fields.getJSONObject("active").getBoolean("booleanValue")) return@execute
                val revision=fields.getJSONObject("revision").getString("integerValue").toLong()
                val title=fields.getJSONObject("title").getString("stringValue")
                val body=fields.getJSONObject("body").getString("stringValue")
                if(revision<=prefs.getLong("seen",0)||title.length !in 1..100||body.length !in 1..1000) return@execute
                activity.runOnUiThread {
                    if(activity.isFinishing||activity.isDestroyed||AppCorePilot.isPaired(activity.applicationContext)!=pilot||AppCoreConfig.maintenance()||AppCoreConfig.flag("disable_bulletins")) return@runOnUiThread
                    if(activity is androidx.lifecycle.LifecycleOwner && !activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return@runOnUiThread
                    AlertDialog.Builder(activity).setTitle(if (pilot) "[Pilot] $title" else title).setMessage(body).setPositiveButton(android.R.string.ok,null).show()
                    prefs.edit().putLong("seen",revision).apply()
                }
            } catch (_: Exception) { } finally { busy.set(false) }
        }
    }
}
