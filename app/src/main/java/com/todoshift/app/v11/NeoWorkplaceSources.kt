package com.todoshift.app.v11

import com.todoshift.app.data.Workplace

/** Add only missing source links; never rename/reassign historical sources or their allocations. */
object NeoWorkplaceSources {
    fun ensure(db: V11Database, places: List<Workplace>): V11Database {
        val existing = db.incomeSources.mapNotNull { it.workplaceId }.toSet()
        val additions =
            places
                .filter { it.id !in existing }
                .mapIndexed { index, p ->
                    val base = "workplace-${p.id}"
                    var id = base
                    while (db.incomeSources.any { it.id == id }) id =
                        "$base-${java.util.UUID.randomUUID()}"
                    IncomeSourceV11(
                        id,
                        p.name,
                        IncomeSourceTypeV11.SHIFT_BASED,
                        workplaceId = p.id,
                        calendarId = db.calendars.firstOrNull()?.id,
                        order = (db.incomeSources.maxOfOrNull { it.order } ?: -1) + index + 1,
                    )
                }
        return if (additions.isEmpty()) db
        else db.copy(incomeSources = db.incomeSources + additions)
    }
}
