package com.todoshift.app.data

class HistoryController {

    private val engine =
        HistoryEngine(
            maxHistory = 40
        )

    fun beforeChange(
        current: AppData
    ) {
        engine.commit(
            current
        )
    }

    fun undo(
        current: AppData
    ): AppData? =
        engine.undo(
            current
        )

    fun redo(
        current: AppData
    ): AppData? =
        engine.redo(
            current
        )

    fun canUndo(): Boolean =
        engine.canUndo()

    fun canRedo(): Boolean =
        engine.canRedo()
}
