package com.todoshift.app.data

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyReportV2(
    val month: YearMonth,
    val totalDays: Int,
    val workingDays: Int,
    val offDays: Int,
    val fullDayCount: Int,
    val longNightCount: Int,
    val threeShiftCount: Int,
    val expected: Double,
    val received: Double,
    val balance: Double
)

data class YearReportV2(
    val year: Int,
    val workingDays: Int,
    val expected: Double,
    val received: Double,
    val balance: Double,
    val months: List<MonthlyReportV2>
)

object ReportsEngine {

    fun month(
        data: AppData,
        month: YearMonth
    ): MonthlyReportV2 {

        val shifts =
            data.shifts.filter {
                runCatching {
                    YearMonth.from(
                        LocalDate.parse(it.date)
                    ) == month
                }.getOrDefault(false)
            }

        val finance =
            FinanceEngine.month(
                data,
                month
            )

        return MonthlyReportV2(
            month = month,
            totalDays = shifts.size,
            workingDays =
                shifts.count {
                    it.type != ShiftType.OFF
                },
            offDays =
                shifts.count {
                    it.type == ShiftType.OFF
                },
            fullDayCount =
                shifts.count {
                    it.type ==
                        ShiftType.FULL_DAY
                },
            longNightCount =
                shifts.count {
                    it.type ==
                        ShiftType.LONG_NIGHT
                },
            threeShiftCount =
                shifts.count {
                    it.type ==
                        ShiftType.THREE_SHIFTS
                },
            expected = finance.expected,
            received = finance.received,
            balance =
                finance.expected -
                    finance.received
        )
    }

    fun year(
        data: AppData,
        year: Int
    ): YearReportV2 {

        val months =
            (1..12).map {
                month(
                    data,
                    YearMonth.of(
                        year,
                        it
                    )
                )
            }

        return YearReportV2(
            year = year,
            workingDays =
                months.sumOf {
                    it.workingDays
                },
            expected =
                months.sumOf {
                    it.expected
                },
            received =
                months.sumOf {
                    it.received
                },
            balance =
                months.sumOf {
                    it.balance
                },
            months = months
        )
    }

    fun plainTextMonth(
        data: AppData,
        month: YearMonth
    ): String {

        val r = month(data, month)

        return buildString {
            appendLine("ToDo Shift")
            appendLine("تقرير شهر ${month.monthValue}/${month.year}")
            appendLine()
            appendLine("أيام العمل: ${r.workingDays}")
            appendLine("الإجازات: ${r.offDays}")
            appendLine("24 ساعة: ${r.fullDayCount}")
            appendLine("Long / Night: ${r.longNightCount}")
            appendLine("3 شيفتات: ${r.threeShiftCount}")
            appendLine()
            appendLine("المستحق: ${r.expected}")
            appendLine("المستلم: ${r.received}")
            appendLine("المتبقي: ${r.balance}")
        }
    }
}
