package com.todoshift.app.data

import java.time.DayOfWeek

object SmartToolsLabels {

    fun mode(mode: SmartToolMode): String =
        when (mode) {
            SmartToolMode.RANGE ->
                "توزيع على فترة"

            SmartToolMode.WEEKDAYS ->
                "أيام أسبوع محددة"

            SmartToolMode.ROTATION ->
                "دورة عمل"

            SmartToolMode.COPY_DAY ->
                "نسخ يوم"

            SmartToolMode.COPY_WEEK ->
                "نسخ أسبوع"

            SmartToolMode.COPY_MONTH ->
                "نسخ شهر"
        }

    fun weekday(day: DayOfWeek): String =
        when (day) {
            DayOfWeek.SATURDAY -> "السبت"
            DayOfWeek.SUNDAY -> "الأحد"
            DayOfWeek.MONDAY -> "الإثنين"
            DayOfWeek.TUESDAY -> "الثلاثاء"
            DayOfWeek.WEDNESDAY -> "الأربعاء"
            DayOfWeek.THURSDAY -> "الخميس"
            DayOfWeek.FRIDAY -> "الجمعة"
        }
}
