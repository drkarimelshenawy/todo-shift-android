package com.todoshift.app.v11

import java.time.LocalDate

object V11RateEngine {

    fun applicableRate(
        rates: List<RateHistoryEntryV11>,
        workplaceId: String?,
        kind: ShiftKind,
        date: String
    ): Double {
        if (workplaceId == null || kind == ShiftKind.OFF) return 0.0

        val target = runCatching { LocalDate.parse(date) }.getOrNull()
            ?: return 0.0

        return rates
            .asSequence()
            .filter {
                it.workplaceId == workplaceId &&
                    it.shiftKind == kind
            }
            .mapNotNull { rate ->
                val effective =
                    runCatching {
                        LocalDate.parse(rate.effectiveFrom)
                    }.getOrNull()

                if (
                    effective != null &&
                    !effective.isAfter(target)
                ) {
                    rate to effective
                } else {
                    null
                }
            }
            .maxByOrNull { it.second }
            ?.first
            ?.amount
            ?: 0.0
    }

    fun segmentAmount(
        segment: ShiftSegmentV11,
        date: String,
        rates: List<RateHistoryEntryV11>
    ): Double {
        segment.exceptionalAmount?.let { return it }

        return applicableRate(
            rates = rates,
            workplaceId = segment.workplaceId,
            kind = segment.kind,
            date = date
        )
    }

    fun dayAmount(
        day: DayEntryV11,
        rates: List<RateHistoryEntryV11>
    ): Double {
        return day.segments.sumOf {
            segmentAmount(
                segment = it,
                date = day.date,
                rates = rates
            )
        }
    }
}
