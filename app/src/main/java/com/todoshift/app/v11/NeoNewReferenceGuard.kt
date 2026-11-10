package com.todoshift.app.v11

/**
 * Reject only new/changed missing targets; historical references are preserved without migration.
 */
internal object NeoNewReferenceGuard {
    fun validate(previous: V11Database, next: V11Database, knownWorkplaces: () -> Set<String>) {
        val calendars = next.calendars.map { it.id }.toSet()
        val places by lazy(knownWorkplaces)
        val shifts = next.days.flatMap { it.segments }.map { it.id }.toSet()
        val oldTasks = previous.tasks.associateBy { it.id }
        next.tasks.forEach { task ->
            val old = oldTasks[task.id]
            if (old == null || old.calendarId != task.calendarId)
                require(task.calendarId == null || task.calendarId in calendars) {
                    "Creation calendar no longer exists; draft retained"
                }
            if (old == null || old.workplaceId != task.workplaceId)
                require(task.workplaceId == null || task.workplaceId in places) {
                    "Workplace no longer exists; draft retained"
                }
            if (old == null || old.linkedShiftId != task.linkedShiftId)
                require(task.linkedShiftId == null || task.linkedShiftId in shifts) {
                    "Linked shift no longer exists; draft retained"
                }
        }
        val oldItems = previous.dayItems.associateBy { it.id }
        next.dayItems.forEach { item ->
            val old = oldItems[item.id]
            if (old == null || old.calendarId != item.calendarId)
                require(item.calendarId == null || item.calendarId in calendars) {
                    "Creation calendar no longer exists; draft retained"
                }
        }
        val oldSegments = previous.days.flatMap { it.segments }.associateBy { it.id }
        next.days
            .flatMap { it.segments }
            .forEach { segment ->
                val old = oldSegments[segment.id]
                if (old == null || old.workplaceId != segment.workplaceId)
                    require(segment.workplaceId == null || segment.workplaceId in places) {
                        "Workplace no longer exists; shift draft retained"
                    }
            }
    }

    fun validateWorkplaceRemoval(db: V11Database, before: Set<String>, after: Set<String>) {
        val removed = before - after
        val referenced =
            db.tasks.mapNotNull { it.workplaceId }.toSet() +
                db.days.flatMap { it.segments }.mapNotNull { it.workplaceId }
        require(removed.intersect(referenced).isEmpty()) {
            "Workplace is referenced by saved tasks or shifts; no removal was made"
        }
    }
}
