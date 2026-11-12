package com.todoshift.app.v11

import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Stable, replay-safe cell edit. A restored pending edit never toggles a saved cell twice. */
data class NeoPaintEdit(val date: String, val calendar: String, val dayId: String,
    val before: List<ShiftSegmentV11>, val after: List<ShiftSegmentV11>) {
    fun apply(db: V11Database): V11Database {
        check(db.calendars.any { it.id == calendar }) { "Calendar removed; review pending painting" }
        check(db.monthSnapshots.none { it.month == date.take(7) && it.reopenedAt == null }) {
            "Month is closed; reopen it before painting"
        }
        val day = db.days.firstOrNull { it.date == date && it.calendarId == calendar }
        if (day?.segments.orEmpty() == after) return db
        check(day?.segments.orEmpty() == before) { "Shift changed elsewhere; pending painting was not applied" }
        val next = (day ?: DayEntryV11(dayId, date, calendar)).copy(segments = after)
        return db.copy(days = db.days.filterNot { it.date == date && it.calendarId == calendar } + next)
    }
    fun encode(): String {
        fun segments(list: List<ShiftSegmentV11>) = JSONArray().apply {
            list.forEach { s -> put(JSONObject().put("id", s.id).put("kind", s.kind.name)
                .put("place", s.workplaceId ?: JSONObject.NULL).put("amount", s.exceptionalAmount ?: JSONObject.NULL)
                .put("note", s.note).put("start", s.startMinute ?: JSONObject.NULL).put("end", s.endMinute ?: JSONObject.NULL)) }
        }
        return JSONObject().put("date",date).put("calendar",calendar).put("day",dayId)
            .put("before",segments(before)).put("after",segments(after)).toString()
    }
    companion object {
        fun decode(raw: String): NeoPaintEdit {
            val o = JSONObject(raw)
            fun segments(a: JSONArray) = List(a.length()) { i ->
                val s = a.getJSONObject(i)
                ShiftSegmentV11(s.getString("id"), ShiftKind.valueOf(s.getString("kind")),
                    if(s.isNull("place")) null else s.getString("place"),
                    if(s.isNull("amount")) null else s.getDouble("amount"), s.getString("note"),
                    if(s.isNull("start")) null else s.getInt("start"), if(s.isNull("end")) null else s.getInt("end"))
            }
            return NeoPaintEdit(o.getString("date"),o.getString("calendar"),o.getString("day"),
                segments(o.getJSONArray("before")),segments(o.getJSONArray("after")))
        }
        fun tap(db: V11Database, date: LocalDate, calendar: String, kind: ShiftKind,
            workplace: String?, erase: Boolean): NeoPaintEdit {
            val old = db.days.firstOrNull { it.date == date.toString() && it.calendarId == calendar }
            val existing = old?.segments.orEmpty()
            val matching = existing.filter { it.kind == kind && (kind == ShiftKind.OFF || it.workplaceId == workplace) }
            val after = when {
                erase -> emptyList()
                matching.isNotEmpty() -> existing.filterNot { it in matching }
                else -> NeoPaintOperations.preview(db,setOf(date),calendar,kind,workplace,false)
                    .candidate.days.first { it.date == date.toString() && it.calendarId == calendar }.segments
            }
            return NeoPaintEdit(date.toString(),calendar,old?.id ?: UUID.randomUUID().toString(),existing,after)
                .also { it.apply(db) }
        }
    }
}
