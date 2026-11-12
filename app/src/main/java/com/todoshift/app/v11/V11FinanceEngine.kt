package com.todoshift.app.v11

import java.time.YearMonth

data class ShiftKindSummaryV11(val kind: ShiftKind, val count: Int, val amount: Double)

data class WorkplaceSummaryV11(val workplaceId: String?, val count: Int, val amount: Double)

data class MonthFinanceV11(
    val month: String,
    val expected: Double,
    val received: Double,
    val settlements: Double,
    val difference: Double,
    val status: SettlementStatusV11,
    val shiftCount: Int,
    val byShiftKind: List<ShiftKindSummaryV11>,
    val byWorkplace: List<WorkplaceSummaryV11>,
)

object V11FinanceEngine {

    fun month(
        db: V11Database,
        month: YearMonth,
        calendarIds: Set<String>? = null,
    ): MonthFinanceV11 {

        val monthText = month.toString()
        // Unattributed money remains in the overall total, not in a selected calendar.
        val sources = db.incomeSources.associateBy { it.id }
        fun included(calendarId: String?): Boolean =
            calendarIds == null || (calendarId != null && calendarId in calendarIds)


        val days =
            db.days.filter { day ->
                day.date.startsWith(monthText) &&
                    (calendarIds == null || day.calendarId in calendarIds)
            }

        val segments =
            days
                .flatMap { day -> day.segments.map { day to it } }
                .filter { it.second.kind != ShiftKind.OFF }

        val expected =
            LocalizedNumbers.amount(
                segments.sumOf { (day, segment) ->
                    LocalizedNumbers.cents(
                        V11RateEngine.segmentAmount(
                            segment = segment,
                            date = day.date,
                            rates = db.rates,
                        )
                    )
                }
            )

        val received =
            LocalizedNumbers.amount(
                db.payments.sumOf { payment ->
                    if (payment.allocations.isEmpty()) {
                        if (payment.accrualMonth == monthText && included(sources[payment.sourceId]?.calendarId))
                            LocalizedNumbers.cents(payment.amount)
                        else 0L
                    } else
                        payment.allocations
                            .filter { allocation ->
                                allocation.accrualMonth == monthText &&
                                    included(sources[allocation.sourceId]?.calendarId)
                            }
                            .sumOf { LocalizedNumbers.cents(it.amount) }
                }
            )

        val settlements =
            LocalizedNumbers.amount(
                db.settlements
                    .filter { it.accrualMonth == monthText }
                    .filter { included(it.calendarId ?: sources[it.sourceId]?.calendarId) }
                    .filter {
                        it.action == SettlementActionV11.WRITE_OFF ||
                            it.action == SettlementActionV11.CANCEL_ENTITLEMENT ||
                            it.action == SettlementActionV11.ADJUSTMENT
                    }
                    .sumOf { LocalizedNumbers.cents(it.amount) }
            )

        val difference =
            LocalizedNumbers.amount(
                LocalizedNumbers.cents(expected) -
                    LocalizedNumbers.cents(received) -
                    LocalizedNumbers.cents(settlements)
            )

        val status =
            when {
                expected <= 0.0 -> SettlementStatusV11.PAID

                received <= 0.0 && settlements <= 0.0 -> SettlementStatusV11.DUE

                difference > 0.009 -> SettlementStatusV11.PARTIALLY_PAID

                else -> SettlementStatusV11.PAID
            }

        val byKind =
            ShiftKind.entries.mapNotNull { kind ->
                val rows = segments.filter { it.second.kind == kind }

                if (rows.isEmpty()) {
                    null
                } else {
                    ShiftKindSummaryV11(
                        kind = kind,
                        count = rows.size,
                        amount =
                            rows.sumOf { (day, segment) ->
                                V11RateEngine.segmentAmount(segment, day.date, db.rates)
                            },
                    )
                }
            }

        val byWorkplace =
            segments
                .groupBy { it.second.workplaceId }
                .map { (workplaceId, rows) ->
                    WorkplaceSummaryV11(
                        workplaceId = workplaceId,
                        count = rows.size,
                        amount =
                            rows.sumOf { (day, segment) ->
                                V11RateEngine.segmentAmount(segment, day.date, db.rates)
                            },
                    )
                }

        return MonthFinanceV11(
            month = monthText,
            expected = expected,
            received = received,
            settlements = settlements,
            difference = difference,
            status = status,
            shiftCount = segments.size,
            byShiftKind = byKind,
            byWorkplace = byWorkplace,
        )
    }
}
