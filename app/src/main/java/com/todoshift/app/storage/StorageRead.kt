package com.todoshift.app.storage

/** Missing is not an empty database. Failed reads must never supply editable defaults. */
sealed interface StorageRead<out T> {
    data object Missing : StorageRead<Nothing>
    data class Loaded<T>(val data: T) : StorageRead<T>
    data class Failed(val cause: Exception) : StorageRead<Nothing>
}

fun <T> StorageRead<T>.requireLoaded(): T = when (this) {
    is StorageRead.Loaded -> data
    StorageRead.Missing -> throw IllegalStateException("Stored database is missing; initialization must be explicit")
    is StorageRead.Failed -> throw IllegalStateException("Unable to read stored database safely", cause)
}
