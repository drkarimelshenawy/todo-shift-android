package com.todoshift.app.v11

enum class ShiftKind {
    FULL_DAY,
    LONG,
    NIGHT,
    MORNING,
    EVENING,
    OFF
}

/*
 * Minutes are measured from 00:00.
 * startMinute is inclusive.
 * endMinute is exclusive.
 */
data class ShiftTimeRangeV11(
    val startMinute: Int,
    val endMinute: Int
) {
    val durationMinutes: Int
        get() = endMinute - startMinute

    fun overlaps(other: ShiftTimeRangeV11): Boolean =
        startMinute < other.endMinute &&
            other.startMinute < endMinute
}

fun ShiftKind.defaultTimeRangeV11(): ShiftTimeRangeV11? =
    when (this) {
        ShiftKind.FULL_DAY ->
            ShiftTimeRangeV11(0, 1440)

        ShiftKind.LONG ->
            ShiftTimeRangeV11(0, 720)

        ShiftKind.MORNING ->
            ShiftTimeRangeV11(0, 480)

        ShiftKind.EVENING ->
            ShiftTimeRangeV11(480, 720)

        ShiftKind.NIGHT ->
            ShiftTimeRangeV11(720, 1440)

        ShiftKind.OFF ->
            null
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
    val note: String = "",
    val startMinute: Int? = null,
    val endMinute: Int? = null
) {
    fun timeRange(): ShiftTimeRangeV11? {
        if (kind == ShiftKind.OFF) return null

        val start = startMinute
        val end = endMinute

        return if (
            start != null &&
            end != null &&
            start in 0..1439 &&
            end in 1..1440 &&
            end > start
        ) {
            ShiftTimeRangeV11(start, end)
        } else {
            kind.defaultTimeRangeV11()
        }
    }
}

enum class DayItemTypeV11 {
    TASK,
    ERRAND,
    REMINDER,
    NOTE
}

data class DayItemV11(
    val id: String,
    val date: String,
    val calendarId: String? = null,
    val type: DayItemTypeV11,
    val title: String,
    val details: String = "",
    val completed: Boolean = false,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
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
    val active: Boolean = true,
    val fixedMonthlyAmount: Double? = null,
    val order: Int = 0
)

data class PaymentMethodV11(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val order: Int = 0
)

data class PaymentAllocationV11(
    val id: String,
    val accrualMonth: String,
    val sourceId: String? = null,
    val amount: Double
)

data class PaymentV11(
    val id: String,
    val amount: Double,
    val receivedDate: String,

    /*
     * Kept for backward compatibility with Schema 11.
     * New multi-month payments use allocations.
     */
    val accrualMonth: String,

    val sourceId: String? = null,
    val paymentMethodId: String? = null,
    val note: String = "",
    val linkedShiftIds: List<String> = emptyList(),
    val allocations: List<PaymentAllocationV11> = emptyList()
)

enum class ObligationTypeV11 {
    INSTALLMENT,
    DEBT,
    LOAN,
    CREDIT_CARD,
    ASSOCIATION,
    RECURRING_BILL,
    OTHER
}

enum class ObligationStatusV11 {
    ACTIVE,
    PAID,
    PAUSED,
    CANCELLED
}

data class ObligationV11(
    val id: String,
    val name: String,
    val type: ObligationTypeV11,
    val totalAmount: Double? = null,
    val installmentAmount: Double? = null,
    val startDate: String,
    val endDate: String? = null,
    val dueDay: Int? = null,
    val installmentCount: Int? = null,
    val reminderDaysBefore: Int = 3,
    val status: ObligationStatusV11 =
        ObligationStatusV11.ACTIVE,
    val note: String = ""
)

data class ObligationPaymentV11(
    val id: String,
    val obligationId: String,
    val dueMonth: String,
    val amount: Double,
    val paidDate: String,
    val paymentMethodId: String? = null,
    val note: String = ""
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
    val createdAt: Long =
        System.currentTimeMillis()
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
    val createdAt: Long =
        System.currentTimeMillis()
)

data class V11Database(
    val schemaVersion: Int = 12,

    val calendars:
        List<AppCalendarV11> = emptyList(),

    val days:
        List<DayEntryV11> = emptyList(),

    val rates:
        List<RateHistoryEntryV11> = emptyList(),

    val incomeSources:
        List<IncomeSourceV11> = emptyList(),

    val paymentMethods:
        List<PaymentMethodV11> = emptyList(),

    val payments:
        List<PaymentV11> = emptyList(),

    val settlements:
        List<SettlementV11> = emptyList(),

    val monthSnapshots:
        List<MonthSnapshotV11> = emptyList(),

    val auditLog:
        List<AuditEntryV11> = emptyList(),

    val dayItems:
        List<DayItemV11> = emptyList(),

    val obligations:
        List<ObligationV11> = emptyList(),

    val obligationPayments:
        List<ObligationPaymentV11> = emptyList()
)
