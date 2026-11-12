package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth

/** Calendar geometry shared by Compose and RemoteViews, independent of layout direction. */
object NeoMonthGrid {
    data class Edges(val top: Boolean, val bottom: Boolean, val start: Boolean, val end: Boolean)
    fun start(month: YearMonth, firstWeekday: Int): LocalDate = month.atDay(1).let {
        it.minusDays(((it.dayOfWeek.value - firstWeekday.coerceIn(1,7) + 7) % 7).toLong())
    }
    fun dates(month: YearMonth, firstWeekday: Int): List<LocalDate> {
        val start = start(month, firstWeekday)
        val offset = java.time.temporal.ChronoUnit.DAYS.between(start,month.atDay(1)).toInt()
        val rows = (offset + month.lengthOfMonth() + 6)/7
        return List(rows*7) { start.plusDays(it.toLong()) }
    }
    fun edges(date: LocalDate, month: YearMonth, firstWeekday: Int): Edges {
        if (YearMonth.from(date) != month) return Edges(false,false,false,false)
        val col = (date.dayOfWeek.value-firstWeekday.coerceIn(1,7)+7)%7
        return Edges(YearMonth.from(date.minusDays(7))!=month,
            YearMonth.from(date.plusDays(7))!=month,
            col==0 || date.dayOfMonth==1, col==6 || date.dayOfMonth==month.lengthOfMonth())
    }
}

