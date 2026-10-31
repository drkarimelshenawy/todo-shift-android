package com.todoshift.app.data

import java.time.LocalDate

data class DataHealthReport(
    val healthy: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

object DataHealthEngine {

    fun inspect(
        data: AppData
    ): DataHealthReport {

        val errors =
            mutableListOf<String>()

        val warnings =
            mutableListOf<String>()

        val workplaceIds =
            data.workplaces
                .map {
                    it.id
                }
                .toSet()

        val duplicateWorkplaces =
            data.workplaces
                .groupBy {
                    it.id
                }
                .filterValues {
                    it.size > 1
                }

        if (
            duplicateWorkplaces
                .isNotEmpty()
        ) {
            errors +=
                "يوجد تكرار في معرفات أماكن العمل"
        }

        val duplicateDates =
            data.shifts
                .groupBy {
                    it.date
                }
                .filterValues {
                    it.size > 1
                }

        if (
            duplicateDates
                .isNotEmpty()
        ) {
            errors +=
                "يوجد أكثر من سجل شيفت لنفس اليوم"
        }

        data.shifts.forEach { shift ->

            val validDate =
                runCatching {
                    LocalDate.parse(
                        shift.date
                    )
                }.isSuccess

            if (!validDate) {
                errors +=
                    "تاريخ غير صالح: ${shift.date}"
            }

            shift.workplaceIds
                .filterNotNull()
                .forEach { id ->

                    if (
                        id !in workplaceIds
                    ) {
                        warnings +=
                            "اليوم ${shift.date} مرتبط بمكان عمل غير موجود"
                    }
                }
        }

        data.workplaces.forEach {
            if (
                it.name.isBlank()
            ) {
                warnings +=
                    "يوجد مكان عمل بدون اسم"
            }

            if (
                it.fullDayRate < 0 ||
                it.longRate < 0 ||
                it.nightRate < 0 ||
                it.morningRate < 0 ||
                it.eveningRate < 0
            ) {
                warnings +=
                    "يوجد سعر سالب في ${it.name}"
            }
        }

        return DataHealthReport(
            healthy =
                errors.isEmpty(),
            errors =
                errors.distinct(),
            warnings =
                warnings.distinct()
        )
    }
}
