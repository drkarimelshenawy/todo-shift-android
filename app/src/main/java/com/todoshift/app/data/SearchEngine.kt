package com.todoshift.app.data

data class ShiftSearchResult(
    val shift: ShiftEntry,
    val workplaceNames: List<String>
)

object SearchEngine {

    fun search(
        data: AppData,
        query: String
    ): List<ShiftSearchResult> {

        val q =
            query.trim()
                .lowercase()

        if (q.isBlank())
            return emptyList()

        return data.shifts
            .map { shift ->

                val names =
                    shift.workplaceIds
                        .filterNotNull()
                        .mapNotNull { id ->
                            data.workplaces
                                .firstOrNull {
                                    it.id == id
                                }
                                ?.name
                        }

                ShiftSearchResult(
                    shift = shift,
                    workplaceNames = names
                )
            }
            .filter { result ->

                result.shift.date
                    .lowercase()
                    .contains(q) ||

                result.shift.note
                    .lowercase()
                    .contains(q) ||

                result.workplaceNames
                    .any {
                        it.lowercase()
                            .contains(q)
                    }
            }
            .sortedByDescending {
                it.shift.date
            }
    }
}
