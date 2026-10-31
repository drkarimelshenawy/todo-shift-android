package com.todoshift.app.data

import java.time.LocalDate
import java.util.UUID

data class PainterSelection(
    val type: ShiftType,
    val workplaceIds: List<String?>,
    val customAmount: Double? = null
)

object ShiftPainterEngine {

    fun apply(
        data: AppData,
        dates: Set<LocalDate>,
        selection: PainterSelection
    ): AppData {

        if (dates.isEmpty()) return data

        val dateStrings =
            dates.map { it.toString() }.toSet()

        val newShifts =
            data.shifts
                .filterNot {
                    it.date in dateStrings
                }
                .toMutableList()

        dates.sorted().forEach { date ->
            newShifts += ShiftEntry(
                date = date.toString(),
                type = selection.type,
                workplaceIds =
                    selection.workplaceIds,
                customAmount =
                    selection.customAmount
            )
        }

        return data.copy(
            shifts = newShifts
        )
    }

    fun deleteFromDates(
        data: AppData,
        dates: Set<LocalDate>
    ): AppData {
        val targets =
            dates.map { it.toString() }.toSet()

        return data.copy(
            shifts = data.shifts.filterNot {
                it.date in targets
            }
        )
    }

    fun createTemplate(
        data: AppData,
        name: String,
        selection: PainterSelection
    ): AppData {

        if (name.isBlank()) return data

        return data.copy(
            templates =
                data.templates + ShiftTemplate(
                    id = UUID.randomUUID().toString(),
                    name = name.trim(),
                    type = selection.type,
                    workplaceIds =
                        selection.workplaceIds,
                    customAmount =
                        selection.customAmount
                )
        )
    }

    fun deleteTemplate(
        data: AppData,
        templateId: String
    ): AppData {
        return data.copy(
            templates =
                data.templates.filterNot {
                    it.id == templateId
                }
        )
    }

    fun fromTemplate(
        template: ShiftTemplate
    ): PainterSelection {
        return PainterSelection(
            type = template.type,
            workplaceIds =
                template.workplaceIds,
            customAmount =
                template.customAmount
        )
    }
}
