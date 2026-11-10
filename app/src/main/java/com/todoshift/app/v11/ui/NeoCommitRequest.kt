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

/** A restored draft must retain its original record revision, not adopt newly loaded data. */
@Composable
fun rememberNeoDraftUnchanged(id: String, record: Any?): Boolean {
    val current =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest((record?.toString() ?: "<absent>").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    val original = androidx.compose.runtime.saveable.rememberSaveable(id) { current }
    return original == current
}

val LocalNeoRejectWrite = staticCompositionLocalOf<(String) -> Unit> { { error(it) } }

@Composable
fun neoWriteAction(action: () -> Unit): () -> Unit {
    val reject = LocalNeoRejectWrite.current
    return {
        try {
            action()
        } catch (failure: Exception) {
            reject(failure.message ?: "Save not confirmed; draft retained")
        }
    }
}

val LocalNeoWorkplaceCommitRequest =
    staticCompositionLocalOf<
        ((List<com.todoshift.app.data.Workplace>, (Boolean) -> Unit) -> Unit)?
    > {
        null
    }

@Composable
fun rememberNeoWorkplaceCommit(
    fallback: (List<com.todoshift.app.data.Workplace>) -> Unit
): (List<com.todoshift.app.data.Workplace>, () -> Unit) -> Unit {
    val request = LocalNeoWorkplaceCommitRequest.current
    return { next, afterCommit ->
        if (request == null) {
            fallback(next)
            afterCommit()
        } else request(next) { committed -> if (committed) afterCommit() }
    }
}

val LocalNeoRestoreCommitted =
    staticCompositionLocalOf<((com.todoshift.app.backup.Neo4RestoredState) -> Unit)?> { null }
