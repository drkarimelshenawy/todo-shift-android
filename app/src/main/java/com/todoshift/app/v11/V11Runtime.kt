package com.todoshift.app.v11

import com.todoshift.app.widget.ToDoShiftWidget

import android.content.Context
import com.todoshift.app.data.AppStorage
import java.time.LocalDate

class V11Runtime(context: Context) {

    private val appContext = context.applicationContext
    private val oldStorage = AppStorage(appContext)
    private val storage = V11Storage(appContext)

    fun start(): V11Database {

        if (storage.hasDatabase()) {
            return storage.load()
        }

        val oldData = oldStorage.load()

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

        return storage.migrateIfNeeded(
            oldData = oldData,
            effectiveFrom = effectiveFrom
        )
    }

    fun save(db: V11Database) =
        storage.save(db)
        ToDoShiftWidget.refreshAll(context)

    fun exportJson(db: V11Database): String =
        storage.exportJson(db)

    fun importJson(raw: String): V11Database =
        storage.importJson(raw)
}
