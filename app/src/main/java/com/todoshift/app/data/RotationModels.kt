package com.todoshift.app.data

import java.util.UUID

data class RotationPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val steps: List<RotationStep>
)

object RotationPresetEngine {

    fun defaultPreset(
        workplaceId: String?
    ): RotationPreset =
        RotationPreset(
            name = "شيفت / إجازة",
            steps = listOf(
                RotationStep(
                    type = ShiftType.FULL_DAY,
                    workplaceIds = listOf(workplaceId)
                ),
                RotationStep(
                    type = ShiftType.OFF,
                    workplaceIds = emptyList()
                )
            )
        )

    fun stepLabel(
        step: RotationStep,
        workplaces: List<Workplace>
    ): String {

        val names =
            step.workplaceIds
                .filterNotNull()
                .mapNotNull { id ->
                    workplaces
                        .firstOrNull {
                            it.id == id
                        }
                        ?.name
                }

        val type =
            when (step.type) {
                ShiftType.FULL_DAY ->
                    "24 ساعة"

                ShiftType.LONG_NIGHT ->
                    "Long / Night"

                ShiftType.THREE_SHIFTS ->
                    "3 شيفتات"

                ShiftType.OFF ->
                    "إجازة"
            }

        return if (names.isEmpty())
            type
        else
            "$type - ${names.joinToString(" / ")}"
    }
}
