package com.todoshift.app.v11

import com.todoshift.app.data.Workplace
import java.time.LocalDate
import java.util.UUID

object V11WorkplaceSync {

    private val payableKinds =
        listOf(
            ShiftKind.FULL_DAY,
            ShiftKind.LONG,
            ShiftKind.NIGHT,
            ShiftKind.MORNING,
            ShiftKind.EVENING
        )

    private fun amount(
        workplace: Workplace,
        kind: ShiftKind
    ): Double =
        when (kind) {
            ShiftKind.FULL_DAY ->
                workplace.fullDayRate
            ShiftKind.LONG ->
                workplace.longRate
            ShiftKind.NIGHT ->
                workplace.nightRate
            ShiftKind.MORNING ->
                workplace.morningRate
            ShiftKind.EVENING ->
                workplace.eveningRate
            ShiftKind.OFF ->
                0.0
        }

    /**
     * Repairs the historical RC7 case where V11 was migrated using one set
     * of generated legacy workplace UUIDs, while AppStorage later generated
     * another set after restart.
     *
     * We only perform ordinal repair when NONE of the V11 workplace IDs
     * match current workplaces and the counts are compatible.
     */
    fun repairWorkplaceIds(
        db: V11Database,
        workplaces: List<Workplace>
    ): V11Database {

        if (
            workplaces.isEmpty() ||
            db.days.isEmpty()
        ) return db

        val segmentIds =
            db.days
                .flatMap { it.segments }
                .mapNotNull { it.workplaceId }
                .distinct()

        if (segmentIds.isEmpty())
            return db

        val currentIds =
            workplaces
                .map { it.id }
                .toSet()

        if (segmentIds.any { it in currentIds })
            return db

        val rateIds =
            db.rates
                .map { it.workplaceId }
                .distinct()

        val oldIds =
            if (rateIds.isNotEmpty())
                rateIds
            else
                segmentIds

        if (
            oldIds.isEmpty() ||
            oldIds.size > workplaces.size
        ) return db

        val mapping =
            oldIds.mapIndexedNotNull {
                index,
                oldId ->

                workplaces
                    .getOrNull(index)
                    ?.id
                    ?.let {
                        oldId to it
                    }
            }.toMap()

        if (mapping.isEmpty())
            return db

        return db.copy(
            days =
                db.days.map { day ->
                    day.copy(
                        segments =
                            day.segments.map { segment ->
                                segment.copy(
                                    workplaceId =
                                        segment.workplaceId
                                            ?.let {
                                                mapping[it] ?: it
                                            }
                                )
                            }
                    )
                },
            rates =
                db.rates.map { rate ->
                    rate.copy(
                        workplaceId =
                            mapping[
                                rate.workplaceId
                            ] ?: rate.workplaceId
                    )
                },
            incomeSources =
                db.incomeSources.map { source ->
                    source.copy(
                        workplaceId =
                            source.workplaceId
                                ?.let {
                                    mapping[it] ?: it
                                }
                    )
                }
        )
    }

    /**
     * Workplace editor is currently backed by legacy AppStorage while V11
     * finance reads RateHistoryEntryV11. RC8 synchronises the two layers.
     *
     * The current workplace price is treated as the whole-shift price,
     * never as an hourly rate.
     */
    fun syncRates(
        db: V11Database,
        workplaces: List<Workplace>
    ): V11Database {

        if (workplaces.isEmpty())
            return db

        val earliestDate =
            db.days
                .mapNotNull {
                    runCatching {
                        LocalDate.parse(it.date)
                    }.getOrNull()
                }
                .minOrNull()
                ?.toString()
                ?: LocalDate.now().toString()

        val workplaceIds =
            workplaces
                .map { it.id }
                .toSet()

        val retained =
            db.rates.filterNot { rate ->
                rate.workplaceId in workplaceIds &&
                    rate.shiftKind in payableKinds
            }

        val synced =
            workplaces.flatMap { workplace ->
                payableKinds.map { kind ->
                    RateHistoryEntryV11(
                        id =
                            UUID.randomUUID()
                                .toString(),
                        workplaceId =
                            workplace.id,
                        shiftKind =
                            kind,
                        effectiveFrom =
                            earliestDate,
                        amount =
                            amount(
                                workplace,
                                kind
                            )
                    )
                }
            }

        return db.copy(
            rates = retained + synced
        )
    }

    fun repairAndSync(
        db: V11Database,
        workplaces: List<Workplace>
    ): V11Database =
        syncRates(
            repairWorkplaceIds(
                db,
                workplaces
            ),
            workplaces
        )
}
