package com.todoshift.app.v11.ui

import com.todoshift.app.v11.*

/** UI-thread bridge for legacy non-composable formatting and event callbacks.
 * Only source literals use this; user-entered text is never passed through a translator.
 * The root resets it when entering Classic. New UI uses scoped composition locals.
 */
internal object NeoLegacyLocale {
    @Volatile var english: Boolean = false
    @Volatile var premium: Boolean = false
}

internal fun neoLiteral(ar: String, en: String): String = if (NeoLegacyLocale.english) en else ar

internal fun neoShiftText(kind: ShiftKind): String {
    if (!NeoLegacyLocale.premium && !NeoLegacyLocale.english) return V11Labels.shift(kind)
    return when (kind) {
        ShiftKind.FULL_DAY -> "24h"
        ShiftKind.LONG -> "Long"
        ShiftKind.NIGHT -> "Night"
        ShiftKind.MORNING -> "Morning"
        ShiftKind.EVENING -> "Evening"
        ShiftKind.OFF -> neoLiteral("إجازة", "Off")
    }
}

internal fun neoObligationState(state: ObligationMonthStateV11): String = when (state) {
    ObligationMonthStateV11.NOT_DUE -> neoLiteral("غير مستحق", "Not due")
    ObligationMonthStateV11.DUE -> neoLiteral("مستحق", "Due")
    ObligationMonthStateV11.PARTIAL -> neoLiteral("مسدد جزئيا", "Partially paid")
    ObligationMonthStateV11.PAID -> neoLiteral("مسدد", "Paid")
    ObligationMonthStateV11.OVERDUE -> neoLiteral("متأخر", "Overdue")
}
