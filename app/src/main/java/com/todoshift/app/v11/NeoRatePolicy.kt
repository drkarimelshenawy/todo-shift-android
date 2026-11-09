package com.todoshift.app.v11

import com.todoshift.app.data.Workplace
import java.time.LocalDate
import java.util.UUID

/** Explicit dated-rate ownership lives in the same atomic database snapshot as its audit record. */
object NeoRatePolicy {
    fun ownedKeys(db: V11Database): Set<String> =
        db.auditLog.filter { it.action == "DATED_RATE_ADD" }.mapNotNull { it.entityId }.toSet()

    fun add(
        db: V11Database,
        workplace: String,
        kind: ShiftKind,
        date: LocalDate,
        amount: Double,
    ): V11Database {
        require(kind != ShiftKind.OFF && amount.isFinite() && amount >= 0)
        require(
            db.rates.none {
                it.workplaceId == workplace &&
                    it.shiftKind == kind &&
                    it.effectiveFrom == date.toString()
            }
        ) {
            "A rate already exists for that date; choose a different effective date"
        }
        require(
            db.monthSnapshots.none { it.reopenedAt == null && it.month >= date.toString().take(7) }
        ) {
            "Historical closed month is protected"
        }
        return db.copy(
            rates =
                db.rates +
                    RateHistoryEntryV11(
                        UUID.randomUUID().toString(),
                        workplace,
                        kind,
                        date.toString(),
                        amount,
                    ),
            auditLog =
                db.auditLog +
                    AuditEntryV11(
                        UUID.randomUUID().toString(),
                        "DATED_RATE_ADD",
                        "WORKPLACE_KIND",
                        "$workplace|${kind.name}",
                        "Effective $date; previous rates retained",
                    ),
        )
    }

    private fun projected(
        db: V11Database,
        places: List<Workplace>,
        date: String?,
        latest: Boolean,
    ): List<Workplace> {
        val keys = ownedKeys(db)
        fun value(place: Workplace, kind: ShiftKind, fallback: Double): Double {
            if ("${place.id}|${kind.name}" !in keys) return fallback
            return if (latest)
                db.rates
                    .filter { it.workplaceId == place.id && it.shiftKind == kind }
                    .maxWithOrNull(
                        compareBy<RateHistoryEntryV11> { it.effectiveFrom }.thenBy { it.id }
                    )
                    ?.amount ?: fallback
            else V11RateEngine.applicableRate(db.rates, place.id, kind, date!!)
        }
        return places.map { p ->
            p.copy(
                fullDayRate = value(p, ShiftKind.FULL_DAY, p.fullDayRate),
                longRate = value(p, ShiftKind.LONG, p.longRate),
                nightRate = value(p, ShiftKind.NIGHT, p.nightRate),
                morningRate = value(p, ShiftKind.MORNING, p.morningRate),
                eveningRate = value(p, ShiftKind.EVENING, p.eveningRate),
            )
        }
    }

    fun display(db: V11Database, places: List<Workplace>) =
        projected(db, places, LocalDate.now().toString(), false)

    fun reconcile(db: V11Database, places: List<Workplace>): V11Database =
        V11WorkplaceSync.repairAndSync(db, projected(db, places, null, true))
}
