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

    /** Move every reference in one database commit; ambiguous same-day merges are rejected. */
    fun transferAndDelete(db:V11Database, source:String, target:String):V11Database {
        require(source!=target && db.calendars.size>1)
        val from=db.calendars.single {it.id==source};val to=db.calendars.single {it.id==target}
        require(from.financial==to.financial) {"Choose a calendar with the same financial setting"}
        val dates=db.days.filter {it.calendarId==source}.map {it.date}.toSet()
        require(db.days.none {it.calendarId==target && it.date in dates}) {"Target contains matching work dates; choose an empty calendar to preserve all historical records"}
        return db.copy(calendars=db.calendars.filterNot {it.id==source},
            days=db.days.map {if(it.calendarId==source)it.copy(calendarId=target) else it},
            tasks=db.tasks.map {if(it.calendarId==source)it.copy(calendarId=target) else it},
            dayItems=db.dayItems.map {if(it.calendarId==source)it.copy(calendarId=target) else it},
            incomeSources=db.incomeSources.map {if(it.calendarId==source)it.copy(calendarId=target) else it},
            settlements=db.settlements.map {if(it.calendarId==source)it.copy(calendarId=target) else it},
            auditLog=db.auditLog+AuditEntryV11(UUID.randomUUID().toString(),"CALENDAR_TRANSFER","CALENDAR",source,"All references moved to $target; record identities retained"))
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
