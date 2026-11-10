package com.todoshift.app.v11

/** Review actual whole-segment edits, including a plain eraser operation, without mutating them. */
data class NeoPaintChange(
    val date: String,
    val calendarId: String,
    val before: List<ShiftSegmentV11>,
    val after: List<ShiftSegmentV11>,
)

fun neoPaintChanges(review: NeoPaintReview): List<NeoPaintChange> {
    val old = review.before.days.associateBy { it.date to it.calendarId }
    val next = review.candidate.days.associateBy { it.date to it.calendarId }
    return (old.keys + next.keys).sortedWith(compareBy({ it.first }, { it.second })).mapNotNull {
        key ->
        val before = old[key]?.segments.orEmpty()
        val after = next[key]?.segments.orEmpty()
        if (before == after) null else NeoPaintChange(key.first, key.second, before, after)
    }
}

fun neoSegmentClock(segment: ShiftSegmentV11, english: Boolean): String {
    if (segment.kind == ShiftKind.OFF) return if (english) "Off — no duration" else "راحة — بلا مدة"
    if (segment.startMinute == null || segment.endMinute == null)
        return if (english) "Clock times not recorded" else "أوقات الساعة غير مسجلة"
    val range =
        segment.timeRange() ?: return if (english) "Time not specified" else "الوقت غير محدد"
    fun clock(minute: Int) =
        "%02d:%02d".format(java.util.Locale.ROOT, (minute / 60) % 24, minute % 60) +
            if (minute >= 1440) " (+${minute/1440}d)" else ""
    return "${clock(range.startMinute)}–${clock(range.endMinute)}"
}
