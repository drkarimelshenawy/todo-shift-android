package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import com.todoshift.app.v11.V11Database

/** Completion means durable data commit, never merely enqueueing a UI coroutine. */
val LocalNeoCommitRequest =
    staticCompositionLocalOf<((V11Database, (Boolean) -> Unit) -> Unit)?> { null }

@Composable
fun rememberNeoCommit(fallback: (V11Database) -> Unit): (V11Database, () -> Unit) -> Unit {
    val request = LocalNeoCommitRequest.current
    return { next, afterCommit ->
        if (request == null) {
            fallback(next)
            afterCommit()
        } else request(next) { committed -> if (committed) afterCommit() }
    }
}
