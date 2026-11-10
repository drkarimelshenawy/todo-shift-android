package com.todoshift.app.v11

import android.app.job.JobParameters
import android.app.job.JobService
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*

/** Best-effort OS retry; data confirmation never depends on OS delivery timing. */
class NeoEffectsJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var work: Job? = null
    private var stopped = AtomicBoolean(false)

    override fun onStartJob(params: JobParameters): Boolean {
        val token = AtomicBoolean(false)
        stopped = token
        work =
            scope.launch {
                var retry = true
                try {
                    ensureActive()
                    NeoCommittedEffects(applicationContext).reconcileCurrent()
                    retry = false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the durable pending marker and request platform backoff.
                } finally {
                    if (!token.get()) jobFinished(params, retry)
                }
            }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        stopped.set(true)
        work?.cancel()
        return true
    }

    override fun onDestroy() {
        stopped.set(true)
        scope.cancel()
        super.onDestroy()
    }
}
