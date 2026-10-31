package com.todoshift.app.data

import java.util.UUID

object DayEngine {

    fun detailsFor(
        data: AppData,
        date: String
    ): DayDetails {
        return data.dayDetails.firstOrNull {
            it.date == date
        } ?: DayDetails(date = date)
    }

    fun saveDetails(
        data: AppData,
        details: DayDetails
    ): AppData {
        val newList =
            data.dayDetails
                .filterNot { it.date == details.date }
                .toMutableList()

        if (
            details.note.isNotBlank() ||
            details.tasks.isNotEmpty() ||
            details.extraAmount != 0.0
        ) {
            newList += details
        }

        return data.copy(
            dayDetails = newList
        )
    }

    fun addTask(
        data: AppData,
        date: String,
        title: String
    ): AppData {
        if (title.isBlank()) return data

        val details = detailsFor(data, date)

        val task = DayTask(
            id = UUID.randomUUID().toString(),
            date = date,
            title = title.trim()
        )

        return saveDetails(
            data,
            details.copy(
                tasks = details.tasks + task
            )
        )
    }

    fun toggleTask(
        data: AppData,
        date: String,
        taskId: String
    ): AppData {
        val details = detailsFor(data, date)

        return saveDetails(
            data,
            details.copy(
                tasks = details.tasks.map {
                    if (it.id == taskId) {
                        it.copy(
                            completed = !it.completed
                        )
                    } else {
                        it
                    }
                }
            )
        )
    }

    fun deleteTask(
        data: AppData,
        date: String,
        taskId: String
    ): AppData {
        val details = detailsFor(data, date)

        return saveDetails(
            data,
            details.copy(
                tasks = details.tasks.filterNot {
                    it.id == taskId
                }
            )
        )
    }

    fun saveNote(
        data: AppData,
        date: String,
        note: String
    ): AppData {
        val details = detailsFor(data, date)

        return saveDetails(
            data,
            details.copy(note = note)
        )
    }

    fun saveExtraAmount(
        data: AppData,
        date: String,
        amount: Double
    ): AppData {
        val details = detailsFor(data, date)

        return saveDetails(
            data,
            details.copy(
                extraAmount = amount
            )
        )
    }

    fun pendingTasks(
        data: AppData,
        date: String
    ): Int {
        return detailsFor(data, date)
            .tasks
            .count { !it.completed }
    }
}
