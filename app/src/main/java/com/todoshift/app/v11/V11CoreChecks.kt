package com.todoshift.app.v11

object V11CoreChecks {

    data class Result(
        val errors: List<String>,
        val warnings: List<String>
    ) {
        val healthy: Boolean
            get() = errors.isEmpty()
    }

    fun check(db: V11Database): Result {

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val calendarIds = db.calendars.map { it.id }.toSet()

        db.days.forEach { day ->
            if (day.calendarId !in calendarIds) {
                errors +=
                    "Day ${day.date} references missing calendar"
            }

            if (day.segments.isEmpty()) {
                warnings +=
                    "Day ${day.date} has no segments"
            }
        }

        val duplicateDays = db.days
            .groupBy { it.id }
            .filterValues { it.size > 1 }

        if (duplicateDays.isNotEmpty()) {
            errors += "Duplicate day IDs detected"
        }

        val duplicateSegments = db.days
            .flatMap { it.segments }
            .groupBy { it.id }
            .filterValues { it.size > 1 }

        if (duplicateSegments.isNotEmpty()) {
            errors += "Duplicate shift segment IDs detected"
        }

        db.rates.forEach {
            if (it.amount < 0.0) {
                errors +=
                    "Negative rate detected: ${it.id}"
            }
        }

        db.payments.forEach {
            if (it.amount < 0.0) {
                errors +=
                    "Negative payment detected: ${it.id}"
            }
        }

        return Result(
            errors = errors,
            warnings = warnings
        )
    }
}
