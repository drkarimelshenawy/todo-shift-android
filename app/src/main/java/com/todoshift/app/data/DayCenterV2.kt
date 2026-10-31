package com.todoshift.app.data

import java.time.LocalDate

data class DayCenterSummary(
    val date: LocalDate,
    val shift: ShiftEntry?,
    val amount: Double,
    val workplaceNames: List<String>,
    val note: String
)

object DayCenterV2 {

    fun summary(
        data: AppData,
        date: LocalDate
    ): DayCenterSummary {

        val shift =
            data.shifts.firstOrNull {
                it.date ==
                    date.toString()
            }

        val names =
            shift
                ?.workplaceIds
                ?.filterNotNull()
                ?.mapNotNull { id ->
                    data.workplaces
                        .firstOrNull {
                            it.id == id
                        }
                        ?.name
                }
                ?: emptyList()

        return DayCenterSummary(
            date = date,
            shift = shift,
            amount =
                shift?.let {
                    FinanceEngine.shiftAmount(
                        it,
                        data.workplaces
                    )
                } ?: 0.0,
            workplaceNames = names,
            note =
                shift?.note
                    .orEmpty()
        )
    }
}
