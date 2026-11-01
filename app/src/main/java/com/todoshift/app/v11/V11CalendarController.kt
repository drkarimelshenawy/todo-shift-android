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

    fun deleteCalendar(
        db: V11Database,
        calendarId: String
    ): V11Database {
        if (db.calendars.size <= 1) return db

        return db.copy(
            calendars =
                db.calendars.filterNot {
                    it.id == calendarId
                },
            days =
                db.days.filterNot {
                    it.calendarId == calendarId
                }
        )
    }
}
