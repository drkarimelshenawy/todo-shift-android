package com.exmore.app.appcore

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.exmore.app.BuildConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AppCoreUpdatePrompt {
    private val executor = Executors.newSingleThreadExecutor()
    private val checked = AtomicBoolean(false)
    @Volatile private var lastAttempt = -3600000L
    fun check(activity: Activity) {
        if (AppCoreConfig.maintenance() || AppCoreConfig.flag("disable_update_prompt")) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastAttempt < 3600000L || !checked.compareAndSet(false, true)) return
        lastAttempt = now
        executor.execute {
            val release = try { runCatching { AppCoreReleaseClient.check(BuildConfig.VERSION_CODE) }.getOrNull() } finally { checked.set(false) } ?: return@execute
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed || AppCoreConfig.maintenance() || AppCoreConfig.flag("disable_update_prompt")) return@runOnUiThread
                val arabic = java.util.Locale.getDefault().language == "ar"
                AlertDialog.Builder(activity)
                    .setTitle(if (arabic) "تحديث اختياري" else "Optional update")
                    .setMessage(release.version)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(if (arabic) "فتح الرابط" else "Open link") { _, _ ->
                        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.url))) }
                    }.show()
            }
        }
    }
}
