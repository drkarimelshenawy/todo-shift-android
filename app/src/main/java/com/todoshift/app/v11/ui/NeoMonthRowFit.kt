package com.todoshift.app.v11.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A month grid always consumes only its allotted height.
 * The number of visible caption lines must not participate in sizing.
 * The calendar never introduces a vertical scroll for 4/5/6-week months.
 */
internal fun fittedCalendarRowHeight(
    availableHeight: Dp,
    weekdayHeaderHeight: Dp,
    weekRows: Int,
): Dp = (availableHeight - weekdayHeaderHeight).coerceAtLeast(0.dp) / weekRows.coerceAtLeast(1)
