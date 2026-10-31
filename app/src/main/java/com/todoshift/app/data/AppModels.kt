package com.todoshift.app.data

data class Workplace(
    val id: String,
    val name: String,
    val colorHex: String,
    val fullDayRate: Double = 0.0,
    val longRate: Double = 0.0,
    val nightRate: Double = 0.0,
    val morningRate: Double = 0.0,
    val eveningRate: Double = 0.0
)

enum class ShiftType {
    FULL_DAY,
    LONG_NIGHT,
    THREE_SHIFTS,
    OFF
}

data class ShiftEntry(
    val date: String,
    val type: ShiftType,
    val workplaceIds: List<String?>,
    val note: String = "",
    val customAmount: Double? = null
)

data class DayTask(
    val id: String,
    val date: String,
    val title: String,
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class DayDetails(
    val date: String,
    val note: String = "",
    val tasks: List<DayTask> = emptyList(),
    val extraAmount: Double = 0.0
)

data class Payment(
    val id: String,
    val date: String,
    val amount: Double,
    val note: String = ""
)

data class ShiftTemplate(
    val id: String,
    val name: String,
    val type: ShiftType,
    val workplaceIds: List<String?>,
    val customAmount: Double? = null
)

data class AppData(
    val workplaces: List<Workplace>,
    val shifts: List<ShiftEntry>,
    val payments: List<Payment>,
    val dayDetails: List<DayDetails> = emptyList(),
    val templates: List<ShiftTemplate> = emptyList()
)
