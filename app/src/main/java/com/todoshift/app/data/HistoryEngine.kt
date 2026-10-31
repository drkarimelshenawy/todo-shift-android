package com.todoshift.app.data

class HistoryEngine(
    private val maxHistory: Int = 30
) {

    private val undoStack =
        ArrayDeque<AppData>()

    private val redoStack =
        ArrayDeque<AppData>()

    fun commit(
        before: AppData
    ) {
        undoStack.addLast(
            before
        )

        while (
            undoStack.size >
            maxHistory
        ) {
            undoStack.removeFirst()
        }

        redoStack.clear()
    }

    fun canUndo(): Boolean =
        undoStack.isNotEmpty()

    fun canRedo(): Boolean =
        redoStack.isNotEmpty()

    fun undo(
        current: AppData
    ): AppData? {

        if (
            undoStack.isEmpty()
        ) {
            return null
        }

        redoStack.addLast(
            current
        )

        return undoStack
            .removeLast()
    }

    fun redo(
        current: AppData
    ): AppData? {

        if (
            redoStack.isEmpty()
        ) {
            return null
        }

        undoStack.addLast(
            current
        )

        return redoStack
            .removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
