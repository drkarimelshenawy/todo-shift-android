package com.todoshift.app.v11

import java.util.UUID

object V11DayOrganizerController {

    fun add(
        db: V11Database,
        date: String,
        calendarId: String?,
        type: DayItemTypeV11,
        title: String,
        details: String = ""
    ): V11Database {
        if (title.isBlank()) return db

        val item = DayItemV11(
            id = UUID.randomUUID().toString(),
            date = date,
            calendarId = calendarId,
            type = type,
            title = title.trim(),
            details = details.trim()
        )

        return db.copy(
            dayItems = db.dayItems + item
        )
    }

    fun toggle(
        db: V11Database,
        id: String
    ): V11Database =
        db.copy(
            dayItems = db.dayItems.map {
                if (it.id == id)
                    it.copy(completed = !it.completed)
                else
                    it
            }
        )

    fun delete(
        db: V11Database,
        id: String
    ): V11Database =
        db.copy(
            dayItems = db.dayItems.filterNot {
                it.id == id
            }
        )
}

object V11PaintConflictController {

    fun apply(
        db: V11Database,
        date: String,
        calendarId: String,
        kind: ShiftKind,
        workplaceId: String?
    ): V11Database {

        val entry = db.days.firstOrNull {
            it.date == date &&
                it.calendarId == calendarId
        }

        val incoming = ShiftSegmentV11(
            id = UUID.randomUUID().toString(),
            kind = kind,
            workplaceId =
                if (kind == ShiftKind.OFF)
                    null
                else
                    workplaceId
        )

        val existing = entry?.segments ?: emptyList()

        val analysis =
            V11ShiftConflictEngine.analyse(
                existing,
                incoming
            )

        val result =
            if (analysis.canAddDirectly) {
                analysis.options
                    .firstOrNull()
                    ?.resultingSegments
                    ?: (existing + incoming)
            } else {
                analysis.options
                    .firstOrNull {
                        it.action !=
                            ShiftConflictActionV11.REJECT
                    }
                    ?.resultingSegments
                    ?: existing
            }

        val newEntry =
            (entry ?: DayEntryV11(
                id = UUID.randomUUID().toString(),
                date = date,
                calendarId = calendarId
            )).copy(
                segments = result
            )

        return db.copy(
            days =
                db.days.filterNot {
                    it.date == date &&
                        it.calendarId == calendarId
                } + newEntry
        )
    }
}
