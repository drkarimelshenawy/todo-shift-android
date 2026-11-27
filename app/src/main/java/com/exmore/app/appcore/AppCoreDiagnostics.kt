package com.exmore.app.appcore

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

/** Explicit local consent; no tasks, notes, finance data or custom identifiers attached. */
object AppCoreDiagnostics {
    private fun prefs(context: Context) = context.getSharedPreferences("appcore_diagnostics_v1", Context.MODE_PRIVATE)
    fun enabled(context: Context) = prefs(context).getBoolean("enabled", false)
    fun setEnabled(context: Context, enable: Boolean) {
        check(FirebaseApp.getInstance().options.projectId == "appcore-2026")
        // Persist consent before enabling any SDK. On failure attempt to disable all SDKs.
        check(prefs(context).edit().putBoolean("enabled", enable).commit())
        try {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enable)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enable)
            FirebasePerformance.getInstance().setPerformanceCollectionEnabled(enable)
        } catch (failure: Exception) {
            prefs(context).edit().putBoolean("enabled", false).commit()
            runCatching { FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(false) }
            runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false) }
            runCatching { FirebasePerformance.getInstance().setPerformanceCollectionEnabled(false) }
            throw failure
        }
    }
    fun startHeartbeatTrace(context: Context): com.google.firebase.perf.metrics.Trace? =
        if (!enabled(context)) null else runCatching {
            FirebasePerformance.getInstance().newTrace("appcore_pilot_heartbeat").also { it.start() }
        }.getOrNull()
}
