package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4DataGate

/** Idempotent external-task commit; a stable draft ID also resolves an ambiguous write result. */
internal class NeoTaskCommit(
    private val read: () -> V11Database,
    private val write: (V11Database, V11Database) -> Unit,
    private val synchronizeServices: (TaskV11) -> Unit,
) {
    sealed interface Result {
        data class Committed(val servicesPending: Boolean) : Result

        data object Conflict : Result

        data object Retry : Result
    }

    fun submit(task: TaskV11): Result {
        val persisted: Result = synchronized(Neo4DataGate.monitor) {
            fun same(stored: TaskV11) =
                stored.copy(createdAt = task.createdAt, updatedAt = task.updatedAt) == task
            try {
                val current = read()
                val stored = current.tasks.firstOrNull { it.id == task.id }
                if (stored != null && !same(stored)) return@synchronized Result.Conflict
                if (stored == null) {
                    try {
                        write(current, current.copy(tasks = current.tasks + task))
                    } catch (failure: Exception) {
                        // Never repeat an uncertain write until the committed store has been
                        // inspected.
                        val confirmedDatabase = read()
                        val confirmed = confirmedDatabase.tasks.firstOrNull { it.id == task.id }
                        if (confirmed == null) return@synchronized Result.Retry
                        if (!same(confirmed)) return@synchronized Result.Conflict
                        // SharedPreferences may expose a failed disk write through its memory
                        // cache.
                        // Require a positive write acknowledgement before calling it committed.
                        write(confirmedDatabase, confirmedDatabase)
                    }
                } else {
                    write(current, current)
                }
                Result.Committed(false)
            } catch (failure: Exception) {
                Result.Retry
            }
        }
        if (persisted !is Result.Committed) return persisted
        // Widget/AlarmManager Binder calls must never hold the database transaction gate.
        // The durable data commit is already acknowledged independently of service delivery.
        return Result.Committed(runCatching { synchronizeServices(task) }.isFailure)
    }
}
