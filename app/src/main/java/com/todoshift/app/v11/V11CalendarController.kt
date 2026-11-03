package com.todoshift.app.v11

import java.util.UUID

object V11CalendarController {

    fun addCalendar(
        db: V11Database,
        name: String,
        colorHex: String
    ): V11Database {
        if (name.isBlank()) return db

        val calendar = AppCalendarV11(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            colorHex = colorHex,
            visible = true,
            order = db.calendars.size
        )

        return db.copy(
            calendars = db.calendars + calendar
        )
    }

    fun updateCalendar(
        db: V11Database,
        calendar: AppCalendarV11
    ): V11Database =
        db.copy(
            calendars = db.calendars.map {
                if (it.id == calendar.id) calendar else it
            }
        )

    fun toggleVisibility(
        db: V11Database,
        calendarId: String
    ): V11Database =
        db.copy(
            calendars = db.calendars.map {
                if (it.id == calendarId)
                    it.copy(visible = !it.visible)
                else
                    it
            }
        )


    fun moveCalendar(
        db: V11Database,
        calendarId: String,
        direction: Int
    ): V11Database {
        val ordered =
            db.calendars
                .sortedBy { it.order }
                .toMutableList()

        val index =
            ordered.indexOfFirst {
                it.id == calendarId
            }

        if (index < 0) return db

        val target =
            index + direction

        if (
            target !in
            ordered.indices
        ) return db

        val current =
            ordered[index]

        ordered[index] =
            ordered[target]

        ordered[target] =
            current

        return db.copy(
            calendars =
                ordered.mapIndexed {
                    order,
                    calendar ->

                    calendar.copy(
                        order = order
                    )
                }
        )
    }

    fun deleteCalendar(
        db: V11Database,
        calendarId: String
    ): V11Database {
        if (db.calendars.size <= 1) return db

        // Preserve calendars referenced by operational or financial records.
        val isReferenced =
            db.days.any { it.calendarId == calendarId } ||
            db.dayItems.any { it.calendarId == calendarId } ||
            db.tasks.any { it.calendarId == calendarId } ||
            db.incomeSources.any { it.calendarId == calendarId } ||
            db.settlements.any { it.calendarId == calendarId }

        if (isReferenced) return db

        return db.copy(
            calendars =
                db.calendars.filterNot {
                    it.id == calendarId
                }
        )
    }
}
