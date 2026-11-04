package com.todoshift.app.v11

import java.util.UUID

object V11DayController {

    private fun ensureEditableMonth(db: V11Database, date: String) {
        val month = date.take(7)
        require(db.monthSnapshots.none {
            it.month == month && it.reopenedAt == null
        }) { "الشهر المالي مغلق؛ افتحه رسميًا قبل تعديل الشيفت" }
    }

    private fun recordShiftEdit(
        db: V11Database, segmentId: String, description: String
    ): V11Database = db.copy(
        auditLog = db.auditLog + AuditEntryV11(
            id = UUID.randomUUID().toString(),
            action = "SHIFT_EDIT",
            entityType = "SHIFT_SEGMENT",
            entityId = segmentId,
            description = description
        )
    )

    fun addSegment(
        db: V11Database,
        date: String,
        calendarId: String,
        kind: ShiftKind,
        workplaceId: String?,
        exceptionalAmount: Double? = null,
        note: String = ""
    ): V11Database {

        ensureEditableMonth(db, date)
        require(kind == ShiftKind.OFF || !workplaceId.isNullOrBlank()) {
            "اختر مكان عمل قبل إضافة شيفت مدفوع"
        }
        require(exceptionalAmount == null ||
            (exceptionalAmount.isFinite() && exceptionalAmount >= 0.0)) {
            "المبلغ غير صالح"
        }
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

        return recordShiftEdit(
            db.copy(days = newDays.sortedBy { it.date }),
            segment.id, "Added shift on $date"
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
        ensureEditableMonth(db, owner.date)
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
        val changed = db.copy(days = db.days.map { day ->
            if (day.id == owner.id) day.copy(
                segments = day.segments.map { if (it.id == segmentId) updated else it }
            ) else day
        })
        return recordShiftEdit(changed, segmentId, "Edited shift details on ${owner.date}")
    }

    /** Update an existing shift's same-day time range, preserving its ID and finances. */
    fun editSegmentTime(
        db: V11Database, segmentId: String,
        startMinute: Int, endMinute: Int
    ): V11Database {
        require(startMinute in 0..1439) { "وقت البداية غير صالح" }
        require(endMinute in 1..2879 && endMinute > startMinute && endMinute - startMinute <= 1440) {
            "مدة الشيفت يجب أن تكون أكبر من صفر ولا تتجاوز 24 ساعة"
        }
        val owners = db.days.filter { day ->
            day.segments.any { it.id == segmentId }
        }
        require(owners.size == 1) { "تعذر تحديد الشيفت بأمان" }
        val owner = owners.single()
        ensureEditableMonth(db, owner.date)
        val original = owner.segments.single { it.id == segmentId }
        require(original.kind != ShiftKind.OFF) { "لا يمكن تعيين أوقات للإجازة" }
        val updated = original.copy(startMinute = startMinute, endMinute = endMinute)
        val others = owner.segments.filterNot { it.id == segmentId }
        require(V11ShiftConflictEngine.analyse(others, updated).canAddDirectly) {
            "تعارض في أوقات الشيفت؛ لم يتم تغيير البيانات"
        }
        val changed = db.copy(days = db.days.map { day ->
            if (day.id == owner.id) day.copy(
                segments = day.segments.map { segment ->
                    if (segment.id == segmentId) updated else segment
                }
            ) else day
        })
        return recordShiftEdit(changed, segmentId,
            "Edited shift time on ${owner.date}: $startMinute-$endMinute")
    }

    fun deleteSegment(db: V11Database, segmentId: String): V11Database {
        val owners = db.days.filter { day -> day.segments.any { it.id == segmentId } }
        require(owners.size == 1) { "تعذر تحديد الشيفت بأمان" }
        val owner = owners.single()
        ensureEditableMonth(db, owner.date)
        val days = db.days.mapNotNull { day ->
            if (day.id != owner.id) day else {
                val remaining = day.segments.filterNot { it.id == segmentId }
                if (remaining.isEmpty()) null else day.copy(segments = remaining)
            }
        }
        return recordShiftEdit(db.copy(days = days), segmentId, "Deleted shift on ${owner.date}")
    }

    fun clearDay(db: V11Database, date: String, calendarId: String? = null): V11Database {
        val affected = db.days.filter { it.date == date && (calendarId == null || it.calendarId == calendarId) }
        if (affected.isEmpty()) return db
        ensureEditableMonth(db, date)
        val updated = db.copy(days = db.days.filterNot { it in affected })
        return recordShiftEdit(updated, affected.joinToString(",") { it.id },
            "Cleared ${affected.size} day entries on $date")
    }
}
