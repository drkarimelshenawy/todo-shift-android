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

data class MissingRateV11(
    val date: String,
    val segmentId: String,
    val workplaceId: String?,
    val kind: ShiftKind,
)

/** Finance projections are read-only; never create or guess dated rates. */
object V11FinanceEngine {
    /**
     * A zero caused by a missing effective-dated rate is not an actual zero
     * salary. Do not back-fill guessed prices into historical earnings.
     */
    fun missingRateDetails(db: V11Database, month: YearMonth): List<MissingRateV11> {
        val salariedWorkplaces = db.incomeSources.filter { source ->
            (source.fixedMonthlyAmount != null || source.compensationHistory.isNotEmpty()) &&
                (if (source.compensationHistory.isEmpty()) source.active else
                    source.compensationHistory
                        .filter { !YearMonth.parse(it.effectiveMonth).isAfter(month) }
                        .maxByOrNull { it.effectiveMonth }?.shiftBased == false)
        }.mapNotNull { it.workplaceId }.toSet()
        return db.days.asSequence()
            .filter { it.date.startsWith(month.toString()) }
            .flatMap { day -> day.segments.asSequence().filter { shift ->
                shift.kind != ShiftKind.OFF &&
                    shift.exceptionalAmount == null &&
                    shift.workplaceId !in salariedWorkplaces &&
                    db.rates.none { rate ->
                        rate.workplaceId == shift.workplaceId &&
                            rate.shiftKind == shift.kind &&
                            rate.effectiveFrom <= day.date &&
                            runCatching { java.time.LocalDate.parse(rate.effectiveFrom) }.isSuccess
                    }
            }.map { shift -> MissingRateV11(day.date, shift.id, shift.workplaceId, shift.kind) } }
            .toList()
    }

    fun missingRateCount(db: V11Database, month: YearMonth): Int =
        missingRateDetails(db, month).size

    fun monthlyAmount(source: IncomeSourceV11, month: YearMonth): Double =
        if (source.compensationHistory.isEmpty()) {
            if (source.active) source.fixedMonthlyAmount ?: 0.0 else 0.0
        } else source.compensationHistory
            .filter { !YearMonth.parse(it.effectiveMonth).isAfter(month) }
            .maxByOrNull { YearMonth.parse(it.effectiveMonth) }?.amount ?: 0.0

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

        // A fixed salary is earned once per month, independently of the shift count.
        // Keep every recorded shift in operational counts; only compensation changes here.
        val monthlySources = db.incomeSources.filter {
            included(it.calendarId) && (it.fixedMonthlyAmount != null || it.compensationHistory.isNotEmpty()) &&
                (if(it.compensationHistory.isEmpty()) it.active else it.compensationHistory.filter {period->!YearMonth.parse(period.effectiveMonth).isAfter(month)}.maxByOrNull {period->period.effectiveMonth}?.shiftBased==false)
        }
        val salariedWorkplaces = monthlySources.mapNotNull { it.workplaceId }.toSet()
        val payableSegments = segments.filter { (_, segment) ->
            segment.workplaceId !in salariedWorkplaces || segment.exceptionalAmount != null
        }
        val monthlyCents = monthlySources.sumOf { LocalizedNumbers.cents(monthlyAmount(it, month)) }
        val expected =
            LocalizedNumbers.amount(
                monthlyCents + payableSegments.sumOf { (day, segment) ->
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
                db.payments.filter { it.reversedAt == null }.sumOf { payment ->
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
                                if (segment.workplaceId in salariedWorkplaces && segment.exceptionalAmount == null) 0.0
                                else V11RateEngine.segmentAmount(segment, day.date, db.rates)
                            },
                    )
                }
            }

        val byWorkplace =
            (segments.map { it.second.workplaceId } + monthlySources.map { it.workplaceId }).distinct()
                .map { workplaceId ->
                    val rows = segments.filter { it.second.workplaceId == workplaceId }
                    WorkplaceSummaryV11(
                        workplaceId = workplaceId,
                        count = rows.size,
                        amount = LocalizedNumbers.amount(
                            payableSegments.filter { it.second.workplaceId == workplaceId }.sumOf { (day, segment) ->
                                LocalizedNumbers.cents(V11RateEngine.segmentAmount(segment, day.date, db.rates))
                            } + monthlySources.filter { it.workplaceId == workplaceId }.sumOf {
                                LocalizedNumbers.cents(monthlyAmount(it, month))
                            }
                        ),
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
