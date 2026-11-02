package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth

enum class ObligationMonthStateV11 {
    NOT_DUE,
    DUE,
    PARTIAL,
    PAID,
    OVERDUE
}

data class ObligationMonthInfoV11(
    val obligation: ObligationV11,
    val month: YearMonth,
    val dueDate: LocalDate?,
    val expected: Double,
    val paid: Double,
    val remaining: Double,
    val state: ObligationMonthStateV11
)

object V11ObligationEngine {

    fun dueDate(
        obligation: ObligationV11,
        month: YearMonth
    ): LocalDate? {
        if (
            obligation.status ==
            ObligationStatusV11.CANCELLED
        ) return null

        val start =
            runCatching {
                LocalDate.parse(
                    obligation.startDate
                )
            }.getOrNull()
                ?: return null

        val startMonth =
            YearMonth.from(start)

        if (month < startMonth) {
            return null
        }

        obligation.endDate
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                runCatching {
                    LocalDate.parse(it)
                }.getOrNull()
            }
            ?.let { end ->
                if (
                    month >
                    YearMonth.from(end)
                ) {
                    return null
                }
            }

        val monthsFromStart =
            (
                month.year -
                    startMonth.year
                ) * 12 +
                month.monthValue -
                startMonth.monthValue

        val count =
            obligation.installmentCount

        if (
            count != null &&
            count > 0 &&
            monthsFromStart >= count
        ) {
            return null
        }

        val requestedDay =
            obligation.dueDay
                ?: start.dayOfMonth

        val safeDay =
            requestedDay.coerceIn(
                1,
                month.lengthOfMonth()
            )

        return month.atDay(
            safeDay
        )
    }

    fun expectedForMonth(
        db: V11Database,
        obligation: ObligationV11,
        month: YearMonth
    ): Double {

        if (
            obligation.status ==
            ObligationStatusV11.PAUSED ||
            obligation.status ==
            ObligationStatusV11.CANCELLED
        ) {
            return 0.0
        }

        if (
            dueDate(
                obligation,
                month
            ) == null
        ) {
            return 0.0
        }

        val remainingTotal =
            V11AccountsController
                .obligationRemaining(
                    db,
                    obligation
                )

        if (
            remainingTotal != null &&
            remainingTotal <= 0.0
        ) {
            return 0.0
        }

        val nominal =
            obligation.installmentAmount
                ?: remainingTotal
                ?: 0.0

        return if (
            remainingTotal != null
        ) {
            minOf(
                nominal,
                remainingTotal
            )
        } else {
            nominal
        }.coerceAtLeast(0.0)
    }

    fun paidForMonth(
        db: V11Database,
        obligationId: String,
        month: YearMonth
    ): Double =
        db.obligationPayments
            .filter {
                it.obligationId ==
                    obligationId &&
                    it.dueMonth ==
                    month.toString()
            }
            .sumOf {
                it.amount
            }

    fun monthInfo(
        db: V11Database,
        obligation: ObligationV11,
        month: YearMonth,
        today: LocalDate =
            LocalDate.now()
    ): ObligationMonthInfoV11 {

        val due =
            dueDate(
                obligation,
                month
            )

        val expected =
            expectedForMonth(
                db,
                obligation,
                month
            )

        val paid =
            paidForMonth(
                db,
                obligation.id,
                month
            )

        val remaining =
            (
                expected -
                    paid
                ).coerceAtLeast(0.0)

        val state =
            when {
                due == null ||
                    expected <= 0.0 ->
                    ObligationMonthStateV11
                        .NOT_DUE

                remaining <= 0.0001 ->
                    ObligationMonthStateV11
                        .PAID

                paid > 0.0 ->
                    if (
                        due.isBefore(today)
                    ) {
                        ObligationMonthStateV11
                            .OVERDUE
                    } else {
                        ObligationMonthStateV11
                            .PARTIAL
                    }

                due.isBefore(today) ->
                    ObligationMonthStateV11
                        .OVERDUE

                else ->
                    ObligationMonthStateV11
                        .DUE
            }

        return ObligationMonthInfoV11(
            obligation = obligation,
            month = month,
            dueDate = due,
            expected = expected,
            paid = paid,
            remaining = remaining,
            state = state
        )
    }

    fun monthItems(
        db: V11Database,
        month: YearMonth
    ): List<ObligationMonthInfoV11> =
        db.obligations
            .map {
                monthInfo(
                    db,
                    it,
                    month
                )
            }
            .filter {
                it.dueDate != null &&
                    it.expected > 0.0
            }
            .sortedBy {
                it.dueDate
            }

    fun itemsForDate(
        db: V11Database,
        date: LocalDate
    ): List<ObligationMonthInfoV11> =
        monthItems(
            db,
            YearMonth.from(date)
        ).filter {
            it.dueDate == date
        }

    fun totalDue(
        db: V11Database,
        month: YearMonth
    ): Double =
        monthItems(
            db,
            month
        ).sumOf {
            it.expected
        }

    fun totalRemaining(
        db: V11Database,
        month: YearMonth
    ): Double =
        monthItems(
            db,
            month
        ).sumOf {
            it.remaining
        }

    fun stateLabel(
        state:
            ObligationMonthStateV11
    ): String =
        when (state) {
            ObligationMonthStateV11
                .NOT_DUE ->
                "غير مستحق"

            ObligationMonthStateV11
                .DUE ->
                "مستحق"

            ObligationMonthStateV11
                .PARTIAL ->
                "مسدد جزئيا"

            ObligationMonthStateV11
                .PAID ->
                "مسدد"

            ObligationMonthStateV11
                .OVERDUE ->
                "متأخر"
        }
}
