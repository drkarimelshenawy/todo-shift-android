package com.todoshift.app.v11

import java.util.UUID

object V11DayController {

    fun addSegment(
        db: V11Database,
        date: String,
        calendarId: String,
        kind: ShiftKind,
        workplaceId: String?,
        exceptionalAmount: Double? = null,
        note: String = ""
    ): V11Database {

        val existing = db.days.firstOrNull {
            it.date == date &&
                it.calendarId == calendarId
        }

        val segment = ShiftSegmentV11(
            id = UUID.randomUUID().toString(),
            kind = kind,
            workplaceId =
                if (kind == ShiftKind.OFF) null
                else workplaceId,
            exceptionalAmount =
                if (kind == ShiftKind.OFF) null
                else exceptionalAmount,
            note = note
        )

        val newDays =
            if (existing == null) {
                db.days + DayEntryV11(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    calendarId = calendarId,
                    segments = listOf(segment),
                    note = note
                )
            } else {
                db.days.map {
                    if (it.id == existing.id)
                        it.copy(
                            segments = it.segments + segment
                        )
                    else
                        it
                }
            }

        return db.copy(
            days = newDays.sortedBy { it.date }
        )
    }

    fun editSegment(
        db: V11Database, segmentId: String,
        kind: ShiftKind, workplaceId: String?,
        exceptionalAmount: Double?, note: String,
        validWorkplaceIds: Set<String>
    ): V11Database {
        require(note.length <= 2000) { "الملاحظة طويلة جدًا" }
        require(exceptionalAmount == null ||
            (exceptionalAmount.isFinite() && exceptionalAmount >= 0.0)) { "المبلغ غير صالح" }
        if (kind != ShiftKind.OFF) {
            require(workplaceId != null && workplaceId in validWorkplaceIds) {
                "اختر مكان عمل صالحًا حتى لا تصبح قيمة الشيفت صفرًا"
            }
        }
        val owners = db.days.filter { day ->
            day.segments.any { it.id == segmentId }
        }
        require(owners.size == 1) { "تعذر تحديد الشيفت بأمان" }
        val owner = owners.single()
        val original = owner.segments.single { it.id == segmentId }
        val updated = original.copy(
            kind = kind, workplaceId = if (kind == ShiftKind.OFF) null else workplaceId,
            exceptionalAmount = if (kind == ShiftKind.OFF) null else exceptionalAmount,
            note = note, startMinute = original.startMinute, endMinute = original.endMinute
        )
        val others = owner.segments.filterNot { it.id == segmentId }
        require(V11ShiftConflictEngine.analyse(others, updated).canAddDirectly) {
            "تعارض مع شيفت آخر؛ لم يتم تغيير البيانات"
        }
        return db.copy(days = db.days.map { day ->
            if (day.id == owner.id) day.copy(
                segments = day.segments.map { if (it.id == segmentId) updated else it }
            ) else day
        })
    }

    fun deleteSegment(
        db: V11Database,
        segmentId: String
    ): V11Database {

        val days = db.days.mapNotNull { day ->
            val segments =
                day.segments.filterNot {
                    it.id == segmentId
                }

            if (segments.isEmpty())
                null
            else
                day.copy(segments = segments)
        }

        return db.copy(days = days)
    }

    fun clearDay(
        db: V11Database,
        date: String,
        calendarId: String? = null
    ): V11Database =
        db.copy(
            days = db.days.filterNot {
                it.date == date &&
                    (
                        calendarId == null ||
                        it.calendarId == calendarId
                    )
            }
        )
}
