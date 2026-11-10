package com.todoshift.app.v11.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.YearMonth

private fun schedulesColor(hex: String): Color =
    runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color.Gray)

@Composable
fun V11SchedulesScreen(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    onDatabaseChange: (V11Database) -> Unit,
    onMonthChange: (YearMonth) -> Unit = {},
    financeAuthorized: Boolean = false,
    financeAmountsVisible: Boolean = false,
) {
    val commit = rememberNeoCommit(onDatabaseChange)
    var timelineDay by remember { mutableStateOf<Pair<java.time.LocalDate, String>?>(null) }
    androidx.activity.compose.BackHandler(enabled = timelineDay != null) { timelineDay = null }
    timelineDay?.let { (date, calendarId) ->
        V11DayDetailsScreen(
            date,
            db,
            workplaces,
            calendarId,
            onBack = { timelineDay = null },
            onDatabaseChange = onDatabaseChange,
            financeAuthorized = financeAuthorized,
            financeAmountsVisible = financeAmountsVisible,
        )
        return
    }
    if (LocalV11NeoEnabled.current) {
        NeoRescueSchedules(
            db,
            workplaces,
            month,
            onMonthChange,
            onDatabaseChange,
            { date, id -> timelineDay = date to id },
        )
        return
    }
    var newCalendarName by remember { mutableStateOf("") }

    var editingCalendar by remember { mutableStateOf<AppCalendarV11?>(null) }

    val monthDays = db.days.filter { it.date.startsWith(month.toString()) }

    val segments = monthDays.flatMap { it.segments }

    val workSegments = segments.filter { it.kind != ShiftKind.OFF }

    val offCount = segments.count { it.kind == ShiftKind.OFF }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        if (LocalV11NeoEnabled.current) {
            V11NeoSectionHero(
                neoLabel(neoLiteral("خطك الزمني", "Your timeline"), "Shift timeline"),
                neoLabel("الشيفتات والتقاويم • $month", "Shifts and calendars • $month"),
            )
            NeoShiftTimeline(
                db,
                workplaces,
                month,
                onMonthChange,
                onOpenDay = { date, calendarId -> timelineDay = date to calendarId },
            )
        }
        if (!LocalV11NeoEnabled.current || LocalNeoAppearance.current.showDescriptions)
            Text(
                neoLiteral(
                    "إدارة التقاويم والجداول وملخص الشهر",
                    "Manage calendars, schedules and monthly overview",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                    else Color.Gray,
            )

        Spacer(Modifier.height(16.dp))

        Text(
            neoLiteral("تقويماتي", "My calendars"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )

        Spacer(Modifier.height(8.dp))

        val orderedCalendars = db.calendars.sortedBy { it.order }

        orderedCalendars.forEachIndexed { index, calendar ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                onDatabaseChange(
                                    V11CalendarController.deleteCalendar(db, calendar.id)
                                )
                            },
                            enabled = db.calendars.size > 1,
                        ) {
                            Icon(Icons.Default.Delete, neoLiteral("حذف", "Delete"))
                        }

                        IconButton(onClick = { editingCalendar = calendar }) {
                            Icon(Icons.Default.Edit, neoLiteral("تعديل", "Edit"))
                        }

                        IconButton(
                            onClick = {
                                onDatabaseChange(
                                    V11CalendarController.toggleVisibility(db, calendar.id)
                                )
                            }
                        ) {
                            Icon(
                                if (calendar.visible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                neoLiteral("إظهار أو إخفاء", "Show or hide"),
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(calendar.name, fontWeight = FontWeight.Black)

                            Text(
                                if (calendar.visible)
                                    neoLiteral("ظاهر في التقويم", "Visible in calendar")
                                else neoLiteral("مخفي", "Hidden"),
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                    if (LocalV11NeoEnabled.current)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else Color.Gray,
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Box(
                            modifier =
                                Modifier.size(20.dp)
                                    .clip(CircleShape)
                                    .background(schedulesColor(calendar.colorHex))
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        IconButton(
                            onClick = {
                                onDatabaseChange(
                                    V11CalendarController.moveCalendar(db, calendar.id, -1)
                                )
                            },
                            enabled = index > 0,
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                neoLiteral("تحريك لأعلى", "Move up"),
                            )
                        }

                        Text(
                            neoLiteral("ترتيب ${index + 1}", "Order ${index + 1}"),
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.labelMedium,
                        )

                        IconButton(
                            onClick = {
                                onDatabaseChange(
                                    V11CalendarController.moveCalendar(db, calendar.id, 1)
                                )
                            },
                            enabled = index < orderedCalendars.lastIndex,
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                neoLiteral("تحريك لأسفل", "Move down"),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    if (newCalendarName.isNotBlank()) {
                        onDatabaseChange(
                            V11CalendarController.addCalendar(db, newCalendarName.trim(), "#4F709C")
                        )
                        newCalendarName = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, null)
                Text(neoLiteral("إضافة", "Add"))
            }

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                value = newCalendarName,
                onValueChange = { newCalendarName = it },
                modifier = Modifier.weight(1f),
                label = { Text(neoLiteral("تقويم جديد", "New calendar")) },
                singleLine = true,
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            neoLiteral("ملخص ${month}", "Overview ${month}"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SummaryCard(
                neoLiteral("أيام العمل", "Work days"),
                monthDays.count { it.segments.any { segment -> segment.kind != ShiftKind.OFF } },
                Modifier.weight(1f),
            )

            SummaryCard(neoLiteral("الإجازات", "Days off"), offCount, Modifier.weight(1f))

            SummaryCard(neoLiteral("الشيفتات", "Shifts"), workSegments.size, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))

        listOf(
                ShiftKind.FULL_DAY,
                ShiftKind.LONG,
                ShiftKind.MORNING,
                ShiftKind.EVENING,
                ShiftKind.NIGHT,
            )
            .forEach { kind ->
                val count = workSegments.count { it.kind == kind }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(count.toString(), fontWeight = FontWeight.Black)

                    Text(neoShiftText(kind))
                }

                HorizontalDivider()
            }

        Spacer(Modifier.height(22.dp))

        Text(
            neoLiteral("أماكن العمل هذا الشهر", "Workplaces this month"),
            fontWeight = FontWeight.Black,
        )

        Spacer(Modifier.height(8.dp))

        workplaces.forEach { place ->
            val count = workSegments.count { it.workplaceId == place.id }

            if (count > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(neoLiteral("$count شيفت", "$count shifts"))

                    Spacer(Modifier.weight(1f))

                    Text(place.name, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.width(8.dp))

                    Box(
                        Modifier.size(15.dp)
                            .clip(CircleShape)
                            .background(schedulesColor(place.colorHex))
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    editingCalendar?.let { calendar ->
        V11EditCalendarDialog(
            calendar = calendar,
            onDismiss = { editingCalendar = null },
            onSave = { updated ->
                commit(V11CalendarController.updateCalendar(db, updated)) { editingCalendar = null }
            },
        )
    }
}

@Composable
private fun V11EditCalendarDialog(
    calendar: AppCalendarV11,
    onDismiss: () -> Unit,
    onSave: (AppCalendarV11) -> Unit,
) {
    var name by rememberSaveable(calendar.id) { mutableStateOf(calendar.name) }

    var colorHex by rememberSaveable(calendar.id) { mutableStateOf(calendar.colorHex) }

    val colors =
        listOf(
            "#4F709C",
            "#9DD8AE",
            "#FFE28A",
            "#D7ACE8",
            "#91C1EE",
            "#FFB4A2",
            "#B8C0FF",
            "#A8DADC",
        )

    val leave = rememberNeoEditorExit(listOf(name, colorHex), onDismiss)
    AlertDialog(
        onDismissRequest = leave,
        title = { Text(neoLiteral("تعديل التقويم", "Edit calendar")) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(neoLiteral("اسم التقويم", "Calendar name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(14.dp))

                Text(neoLiteral("لون التقويم", "Calendar color"), fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    colors.take(4).forEach { hex ->
                        CalendarColorChoice(
                            hex = hex,
                            selected = colorHex == hex,
                            onClick = { colorHex = hex },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    colors.drop(4).forEach { hex ->
                        CalendarColorChoice(
                            hex = hex,
                            selected = colorHex == hex,
                            onClick = { colorHex = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            leave.SaveButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(calendar.copy(name = name.trim(), colorHex = colorHex))
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(neoLiteral("حفظ", "Save"))
            }
        },
        dismissButton = { TextButton(onClick = leave) { Text(neoLiteral("إلغاء", "Cancel")) } },
    )
}

@Composable
private fun CalendarColorChoice(hex: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(if (selected) 42.dp else 36.dp).clickable { onClick() },
        shape = CircleShape,
        color = schedulesColor(hex),
        border =
            if (selected)
                androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            else null,
    ) {}
}

@Composable
private fun SummaryCard(label: String, value: Int, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )

            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
