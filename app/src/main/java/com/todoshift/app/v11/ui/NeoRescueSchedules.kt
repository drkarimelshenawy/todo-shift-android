package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun NeoRescueSchedules(
    db: V11Database,
    places: List<Workplace>,
    month: YearMonth,
    onMonth: (YearMonth) -> Unit,
    onChange: (V11Database) -> Unit,
    onDay: (LocalDate, String) -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    val nav = LocalNeoFeatures.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(NeoSpace.page),
        verticalArrangement = Arrangement.spacedBy(NeoSpace.gap),
    ) {
        Row(Modifier.fillMaxWidth()) {
            listOf(
                    neoLabel("الورديات", "Shifts"),
                    neoLabel("الملخص", "Summary"),
                    neoLabel("التقاويم", "Calendars"),
                )
                .forEachIndexed { index, title ->
                    TextButton(
                        onClick = { selected = index },
                        modifier = Modifier.weight(1f),
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor =
                                    if (selected == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                    ) {
                        Text(title)
                    }
                }
        }
        when (selected) {
            0 -> NeoShiftTimeline(db, places, month, onMonth, onDay)
            1 -> {
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { onMonth(month.minusMonths(1)) }) { Text("‹") }
                    Text(neoMonthTitle(month, LocalNeoEnglish.current), Modifier.weight(1f))
                    TextButton(onClick = { onMonth(month.plusMonths(1)) }) { Text("›") }
                }
                val visible = db.calendars.filter { it.visible }.map { it.id }.toSet()
                val days =
                    db.days.filter {
                        it.calendarId in visible && it.date.startsWith(month.toString())
                    }
                val segments = days.flatMap { it.segments }
                listOf(
                        neoLabel("الورديات", "Shifts") to
                            segments.count { it.kind != ShiftKind.OFF },
                        neoLabel("أيام العمل", "Worked days") to
                            days
                                .filter { d -> d.segments.any { it.kind != ShiftKind.OFF } }
                                .map { it.date }
                                .distinct()
                                .size,
                        neoLabel("الراحة", "Off") to segments.count { it.kind == ShiftKind.OFF },
                    )
                    .forEach { (label, count) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = { Text(count.toString()) },
                        )
                    }
                ShiftKind.entries.forEach { kind ->
                    val count = segments.count { it.kind == kind }
                    if (count > 0)
                        ListItem(
                            headlineContent = { Text(neoShiftLabel(kind)) },
                            trailingContent = { Text(count.toString()) },
                        )
                }
                places.forEach { place ->
                    val count = segments.count { it.workplaceId == place.id }
                    if (count > 0)
                        ListItem(
                            headlineContent = { Text(place.name) },
                            leadingContent = {
                                Box(
                                    Modifier.size(12.dp)
                                        .background(neoHexColor(place.colorHex), CircleShape)
                                )
                            },
                            trailingContent = { Text(count.toString()) },
                        )
                }
                NeoSettingsRow(neoLabel("مقارنة شهرين", "Compare months")) { nav.open("COMPARE") }
                NeoSettingsRow(neoLabel("قوالب الورديات", "Shift templates")) {
                    nav.open("SHIFT_TEMPLATE")
                }
            }
            2 -> {
                db.calendars
                    .sortedBy { it.order }
                    .forEach { calendar ->
                        ListItem(
                            headlineContent = { Text(calendar.name) },
                            leadingContent = {
                                Box(
                                    Modifier.size(12.dp)
                                        .background(neoHexColor(calendar.colorHex), CircleShape)
                                )
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        onChange(
                                            V11CalendarController.toggleVisibility(db, calendar.id)
                                        )
                                    }
                                ) {
                                    Icon(
                                        if (calendar.visible) Icons.Rounded.Visibility
                                        else Icons.Rounded.VisibilityOff,
                                        neoLabel("إظهار أو إخفاء", "Show or hide"),
                                    )
                                }
                            },
                        )
                    }
                NeoSettingsRow(
                    neoLabel(
                        "إدارة التقاويم والوجهة الافتراضية",
                        "Manage calendars and default destination",
                    )
                ) {
                    nav.open("CALENDAR_MANAGER")
                }
            }
        }
    }
}
