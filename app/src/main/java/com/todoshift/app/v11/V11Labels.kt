package com.todoshift.app.v11

object V11Labels {

    fun shift(kind: ShiftKind): String =
        when (kind) {
            ShiftKind.FULL_DAY -> "24 ساعة"
            ShiftKind.LONG -> "Long"
            ShiftKind.NIGHT -> "Night"
            ShiftKind.MORNING -> "Morning"
            ShiftKind.EVENING -> "Evening"
            ShiftKind.OFF -> "إجازة"
        }
}
