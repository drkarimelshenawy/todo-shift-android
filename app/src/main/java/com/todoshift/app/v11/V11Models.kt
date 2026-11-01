package com.todoshift.app.v11

enum class ShiftKind {
    FULL_DAY,
    LONG,
    NIGHT,
    MORNING,
    EVENING,
    OFF
}

data class AppCalendarV11(
    val id: String,
    val name: String,
    val colorHex: String,
    val visible: Boolean = true,
    val order: Int = 0,
    val financial: Boolean = true
)

data class ShiftSegmentV11(
    val id: String,
    val kind: ShiftKind,
    val workplaceId: String? = null,
    val exceptionalAmount: Double? = null,
    val note: String = ""
)

data class DayEntryV11(
    val id: String,
    val date: String,
    val calendarId: String,
    val segments: List<ShiftSegmentV11> = emptyList(),
    val note: String = ""
)

data class RateHistoryEntryV11(
    val id: String,
    val workplaceId: String,
    val shiftKind: ShiftKind,
    val effectiveFrom: String,
    val amount: Double
)

enum class IncomeSourceTypeV11 {
    SHIFT_BASED,
    FIXED_SALARY,
    CONTRACT,
    OTHER
}

data class IncomeSourceV11(
    val id: String,
    val name: String,
    val type: IncomeSourceTypeV11,
    val workplaceId: String? = null,
    val calendarId: String? = null,
    val active: Boolean = true
)

data class PaymentMethodV11(
    val id: String,
    val name: String,
    val active: Boolean = true
)

data class PaymentV11(
    val id: String,
    val amount: Double,
    val receivedDate: String,
    val accrualMonth: String,
    val sourceId: String? = null,
    val paymentMethodId: String? = null,
    val note: String = "",
    val linkedShiftIds: List<String> = emptyList()
)

enum class SettlementStatusV11 {
    DUE,
    PARTIALLY_PAID,
    PAID,
    CANCELLED
}

enum class SettlementActionV11 {
    PAYMENT,
    WRITE_OFF,
    CANCEL_ENTITLEMENT,
    ADJUSTMENT
}

data class SettlementV11(
    val id: String,
    val accrualMonth: String,
    val sourceId: String? = null,
    val workplaceId: String? = null,
    val calendarId: String? = null,
    val action: SettlementActionV11,
    val amount: Double,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class MonthSnapshotV11(
    val id: String,
    val month: String,
    val expectedAmount: Double,
    val receivedAmount: Double,
    val settlementAmount: Double,
    val finalDifference: Double,
    val closedAt: Long,
    val reopenedAt: Long? = null,
    val note: String = ""
)

data class AuditEntryV11(
    val id: String,
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class V11Database(
    val schemaVersion: Int = 11,
    val calendars: List<AppCalendarV11> = emptyList(),
    val days: List<DayEntryV11> = emptyList(),
    val rates: List<RateHistoryEntryV11> = emptyList(),
    val incomeSources: List<IncomeSourceV11> = emptyList(),
    val paymentMethods: List<PaymentMethodV11> = emptyList(),
    val payments: List<PaymentV11> = emptyList(),
    val settlements: List<SettlementV11> = emptyList(),
    val monthSnapshots: List<MonthSnapshotV11> = emptyList(),
    val auditLog: List<AuditEntryV11> = emptyList()
)
