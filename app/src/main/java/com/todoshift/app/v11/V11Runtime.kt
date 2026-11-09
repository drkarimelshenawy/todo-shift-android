package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.data.AppStorage
import com.todoshift.app.storage.StorageRead
import java.time.LocalDate

class V11Runtime internal constructor(
    private val oldStorage: AppStorage,
    private val storage: V11Storage,
    private val recover: () -> Unit,
    private val refreshWidgets: () -> Unit
) {
    constructor(context: Context) : this(
        AppStorage(context.applicationContext), V11Storage(context.applicationContext),
        { com.todoshift.app.backup.Neo4LocalBackup(context.applicationContext).recoverIfNeeded() },
        { com.todoshift.app.widget.Neo4WidgetRefresh.schedule(context.applicationContext) }
    )

    fun start(): V11Database {
        return synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
            recover()

            when (val result = storage.read()) {
                is StorageRead.Loaded -> return@synchronized result.data
                is StorageRead.Failed -> throw IllegalStateException("V11 load failed; no initialization allowed", result.cause)
                StorageRead.Missing -> check(!storage.wasMigrated()) {
                    "Previously migrated V11 database is missing; refusing to recreate it"
                }
            }
            val oldData = when (val result = oldStorage.read()) {
                is StorageRead.Loaded -> result.data
                is StorageRead.Failed -> throw IllegalStateException("Legacy load failed; migration blocked", result.cause)
                StorageRead.Missing -> {
                    check(storage.isPristine()) { "V11 preferences exist; first install cannot be verified" }
                    oldStorage.initializeFirstInstall()
                }
            }

            /*
             * RC1 did not store historical effective dates.
             * We seed the imported rates from the earliest legacy
             * shift date where possible so existing RC1 shifts remain
             * financially calculable after migration.
             */
            val effectiveFrom =
                oldData.shifts
                    .mapNotNull {
                        runCatching {
                            LocalDate.parse(it.date)
                        }.getOrNull()
                    }
                    .minOrNull()
                    ?.toString()
                    ?: LocalDate.now().toString()

            storage.migrateIfNeeded(
                oldData = oldData,
                effectiveFrom = effectiveFrom
            )
        }
    }

    fun save(db: V11Database) = synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
        storage.save(db)
        runCatching { refreshWidgets() }
    }

    /** Compare the complete expected snapshot under the same gate as restore. */
    fun saveIfCurrent(expected: V11Database, next: V11Database) = synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
        check(storage.load() == expected) { "Data changed elsewhere. Reopen before editing; no overwrite was made." }
        storage.save(next)
        // A widget failure must not turn a successful database commit into a false failure.
        runCatching { refreshWidgets() }
    }

    fun exportJson(db: V11Database): String =
        storage.exportJson(db)

    fun importJson(raw: String): V11Database =
        storage.importJson(raw)
}
