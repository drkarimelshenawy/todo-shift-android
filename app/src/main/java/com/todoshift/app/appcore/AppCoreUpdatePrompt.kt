package com.todoshift.app.appcore

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import com.todoshift.app.BuildConfig
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AppCoreUpdatePrompt {
    private val executor = Executors.newSingleThreadExecutor()
    private val checked = AtomicBoolean(false)
    fun check(activity: Activity) {
        if (!checked.compareAndSet(false, true)) return
        executor.execute {
            val release = runCatching { AppCoreReleaseClient.check(BuildConfig.VERSION_CODE) }.getOrNull() ?: return@execute
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
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
