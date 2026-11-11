package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.*

/** One in-flight verification per UI entrance; only the Boolean crosses back to Main. */
internal class NeoSecurityVerifier(private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set

    fun submit(check: () -> Boolean, result: (Boolean) -> Unit) =
        execute(check) { result(it.getOrDefault(false)) }

    fun <T> execute(action: () -> T, result: (Result<T>) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val outcome = withContext(Dispatchers.Default) {
                    try { Result.success(action()) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (failure: Exception) { Result.failure<T>(failure) }
                }
                result(outcome)
            } finally { busy = false }
        }
    }
}

@Composable
internal fun rememberNeoSecurityVerifier(): NeoSecurityVerifier {
    val scope = rememberCoroutineScope()
    return remember(scope) { NeoSecurityVerifier(scope) }
}
