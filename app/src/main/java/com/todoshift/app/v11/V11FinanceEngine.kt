package com.todoshift.app.v11

import java.time.YearMonth

data class ShiftKindSummaryV11(
    val kind: ShiftKind,
    val count: Int,
    val amount: Double
)

data class WorkplaceSummaryV11(
    val workplaceId: String?,
    val count: Int,
    val amount: Double
)

data class MonthFinanceV11(
    val month: String,
    val expected: Double,
    val received: Double,
    val settlements: Double,
    val difference: Double,
    val status: SettlementStatusV11,
    val shiftCount: Int,
    val byShiftKind: List<ShiftKindSummaryV11>,
    val byWorkplace: List<WorkplaceSummaryV11>
)

object V11FinanceEngine {

    fun month(
        db: V11Database,
        month: YearMonth,
        calendarIds: Set<String>? = null
    ): MonthFinanceV11 {

        val monthText = month.toString()

        val days = db.days.filter { day ->
            day.date.startsWith(monthText) &&
                (
                    calendarIds == null ||
                    day.calendarId in calendarIds
                )
        }

        val segments = days.flatMap { day ->
            day.segments.map { day to it }
        }.filter {
            it.second.kind != ShiftKind.OFF
        }

        val expected = segments.sumOf { (day, segment) ->
            V11RateEngine.segmentAmount(
                segment = segment,
                date = day.date,
                rates = db.rates
            )
        }

        val received = db.payments
            .filter { it.accrualMonth == monthText }
            .sumOf { it.amount }

        val settlements = db.settlements
            .filter { it.accrualMonth == monthText }
            .filter {
                it.action == SettlementActionV11.WRITE_OFF ||
                    it.action == SettlementActionV11.CANCEL_ENTITLEMENT ||
                    it.action == SettlementActionV11.ADJUSTMENT
            }
            .sumOf { it.amount }

        val difference =
            expected - received - settlements

        val status = when {
            expected <= 0.0 ->
                SettlementStatusV11.PAID

            received <= 0.0 && settlements <= 0.0 ->
                SettlementStatusV11.DUE

            difference > 0.009 ->
                SettlementStatusV11.PARTIALLY_PAID

            else ->
                SettlementStatusV11.PAID
        }

        val byKind = ShiftKind.entries.mapNotNull { kind ->
            val rows = segments.filter {
                it.second.kind == kind
            }

            if (rows.isEmpty()) {
                null
            } else {
                ShiftKindSummaryV11(
                    kind = kind,
                    count = rows.size,
                    amount = rows.sumOf { (day, segment) ->
                        V11RateEngine.segmentAmount(
                            segment,
                            day.date,
                            db.rates
                        )
                    }
                )
            }
        }

        val byWorkplace = segments
            .groupBy { it.second.workplaceId }
            .map { (workplaceId, rows) ->
                WorkplaceSummaryV11(
                    workplaceId = workplaceId,
                    count = rows.size,
                    amount = rows.sumOf { (day, segment) ->
                        V11RateEngine.segmentAmount(
                            segment,
                            day.date,
                            db.rates
                        )
                    }
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
            byWorkplace = byWorkplace
        )
    }
}
