package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.*

/** One in-flight verification per UI entrance; only the Boolean crosses back to Main. */
internal class NeoSecurityVerifier(private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set

    fun submit(check: () -> Boolean, result: (Boolean) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val verified = withContext(Dispatchers.Default) {
                    try { check() }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { false }
                }
                result(verified)
            } finally { busy = false }
        }
    }
}

@Composable
internal fun rememberNeoSecurityVerifier(): NeoSecurityVerifier {
    val scope = rememberCoroutineScope()
    return remember(scope) { NeoSecurityVerifier(scope) }
}
