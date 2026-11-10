package com.todoshift.app.widget

import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

/**
 * Shared read-only projection for RemoteViews and Studio; no stores, scheduling or write effects.
 */
data class NeoWidgetContent(
    val title: String,
    val value: String,
    val detail: String = "",
    val rows: List<String> = emptyList(),
)

object NeoWidgetContentModel {
    fun shiftLabel(kind: ShiftKind, en: Boolean): String =
        when (kind) {
            ShiftKind.FULL_DAY -> "24h"
            ShiftKind.LONG -> if (en) "Long" else "طويلة"
            ShiftKind.NIGHT -> if (en) "Night" else "ليلية"
            ShiftKind.MORNING -> if (en) "Morning" else "صباحية"
            ShiftKind.EVENING -> if (en) "Evening" else "مسائية"
            ShiftKind.OFF -> if (en) "Off" else "راحة"
        }

    fun tasks(db: V11Database, today: LocalDate): List<TaskV11> {
        val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
        return db.tasks
            .filter {
                it.status != TaskStatusV11.COMPLETED &&
                    (it.calendarId == null || it.calendarId in visible)
            }
            .sortedWith(
                compareBy<TaskV11> { if (it.date <= today.toString()) 0 else 1 }
                    .thenBy { it.date }
                    .thenBy { it.time ?: "99:99" }
            )
    }

    fun indexedSegments(db: V11Database): Map<String, List<ShiftSegmentV11>> {
        val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
        return db.days
            .filter { it.calendarId in visible }
            .groupBy { it.date }
            .mapValues { (_, days) -> days.flatMap { it.segments } }
    }

    fun segments(db: V11Database, date: LocalDate): List<ShiftSegmentV11> =
        indexedSegments(db)[date.toString()].orEmpty()

    fun todayShift(db: V11Database, now: LocalDateTime): NeoShiftOccurrence? =
        neoShiftOccurrences(db, db.calendars.filter { it.visible }.map { it.id }.toSet()).let { all
            ->
            all.firstOrNull {
                it.segment.startMinute != null &&
                    it.segment.endMinute != null &&
                    now >= it.start &&
                    now < it.end
            }
                ?: all.firstOrNull {
                    it.entry.date == now.toLocalDate().toString() &&
                        (it.segment.startMinute == null || it.start > now)
                }
        }

    fun project(
        kind: Neo4WidgetKind,
        db: V11Database?,
        options: Neo4WidgetOptions,
        security: V11SecuritySettings?,
        amountsHidden: Boolean,
        now: LocalDateTime,
        en: Boolean,
    ): NeoWidgetContent {
        fun label(ar: String, english: String) = if (en) english else ar
        val title =
            when (kind) {
                Neo4WidgetKind.CALENDAR -> label("التقويم", "Calendar")
                Neo4WidgetKind.TASKS -> label("المهام", "Tasks")
                Neo4WidgetKind.TODAY -> label("شيفت اليوم", "Today's shift")
                Neo4WidgetKind.FINANCE -> label("حسابات الشهر", "Monthly finance")
            }
        if (options.hideContent) return NeoWidgetContent(title, label("المحتوى مخفي", "Hidden"))
        if (db == null)
            return NeoWidgetContent(
                title,
                label("تعذر قراءة البيانات", "Unable to read data"),
                label("افتح التطبيق للاستعادة بأمان", "Open ToDo Shift to recover safely"),
            )
        return when (kind) {
            Neo4WidgetKind.TASKS -> {
                val tasks = tasks(db, now.toLocalDate())
                NeoWidgetContent(title, tasks.size.toString(), rows = tasks.map { it.title })
            }
            Neo4WidgetKind.CALENDAR -> {
                val shifts = segments(db, now.toLocalDate())
                NeoWidgetContent(
                    title,
                    neoMonthTitle(YearMonth.from(now), en),
                    if (options.showDetails) shifts.joinToString(" / ") { shiftLabel(it.kind, en) }
                    else "",
                )
            }
            Neo4WidgetKind.FINANCE -> {
                if (
                    security == null ||
                        !options.showAmounts ||
                        security.protectAccounts ||
                        security.hideAmounts ||
                        amountsHidden
                )
                    NeoWidgetContent(
                        title,
                        "••••••",
                        label("افتح التطبيق لعرض الحسابات بأمان", "Open app to view securely"),
                    )
                else {
                    val finance = V11FinanceEngine.month(db, YearMonth.from(now))
                    NeoWidgetContent(
                        title,
                        String.format(Locale.US, "%,.2f EGP", finance.difference),
                        if (options.showDetails) label("المتبقي • ", "Remaining • ") + finance.month
                        else finance.month,
                    )
                }
            }
            Neo4WidgetKind.TODAY -> {
                val shift = todayShift(db, now)
                val name =
                    shift?.segment?.kind?.let {
                        when (it) {
                            ShiftKind.FULL_DAY -> "24h"
                            ShiftKind.LONG -> label("طويلة", "Long")
                            ShiftKind.NIGHT -> label("ليلية", "Night")
                            ShiftKind.MORNING -> label("صباحية", "Morning")
                            ShiftKind.EVENING -> label("مسائية", "Evening")
                            ShiftKind.OFF -> label("راحة", "Off")
                        }
                    }
                NeoWidgetContent(
                    title,
                    name
                        ?: label(
                            "لا يوجد شيفت جارٍ أو قادم اليوم",
                            "No current or next shift today",
                        ),
                    shift
                        ?.let {
                            neoSegmentClock(it.segment, en) +
                                (if (options.showDetails) " • " + it.entry.date else "")
                        }
                        .orEmpty(),
                )
            }
        }
    }
}
