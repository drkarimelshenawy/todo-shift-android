package com.todoshift.app.v11.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.YearMonth

private fun schedulesColor(
    hex: String
): Color =
    runCatching {
        Color(AndroidColor.parseColor(hex))
    }.getOrDefault(Color.Gray)

@Composable
fun V11SchedulesScreen(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    onDatabaseChange: (V11Database) -> Unit
) {
    var newCalendarName by remember {
        mutableStateOf("")
    }

    var editingCalendar by remember {
        mutableStateOf<AppCalendarV11?>(null)
    }

    val monthDays =
        db.days.filter {
            it.date.startsWith(
                month.toString()
            )
        }

    val segments =
        monthDays.flatMap {
            it.segments
        }

    val workSegments =
        segments.filter {
            it.kind != ShiftKind.OFF
        }

    val offCount =
        segments.count {
            it.kind == ShiftKind.OFF
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(14.dp)
    ) {
        if (LocalV11NeoEnabled.current) {
            V11NeoSectionHero("جداول العمل", "التقاويم والشيفتات • ${month}")
        }
        Text(
            "إدارة التقاويم والجداول وملخص الشهر",
            style =
                MaterialTheme.typography
                    .bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "تقويماتي",
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        val orderedCalendars =
            db.calendars
                .sortedBy { it.order }

        orderedCalendars
            .forEachIndexed {
                index,
                calendar ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier =
                            Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11CalendarController
                                            .deleteCalendar(
                                                db,
                                                calendar.id
                                            )
                                    )
                                },
                                enabled =
                                    db.calendars.size > 1
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "حذف"
                                )
                            }

                            IconButton(
                                onClick = {
                                    editingCalendar =
                                        calendar
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    "تعديل"
                                )
                            }

                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11CalendarController
                                            .toggleVisibility(
                                                db,
                                                calendar.id
                                            )
                                    )
                                }
                            ) {
                                Icon(
                                    if (calendar.visible)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    "إظهار أو إخفاء"
                                )
                            }

                            Spacer(
                                Modifier.weight(1f)
                            )

                            Column(
                                horizontalAlignment =
                                    Alignment.End
                            ) {
                                Text(
                                    calendar.name,
                                    fontWeight =
                                        FontWeight.Black
                                )

                                Text(
                                    if (calendar.visible)
                                        "ظاهر في التقويم"
                                    else
                                        "مخفي",
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelSmall,
                                    color = Color.Gray
                                )
                            }

                            Spacer(
                                Modifier.width(10.dp)
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(
                                            schedulesColor(
                                                calendar.colorHex
                                            )
                                        )
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11CalendarController
                                            .moveCalendar(
                                                db,
                                                calendar.id,
                                                -1
                                            )
                                    )
                                },
                                enabled =
                                    index > 0
                            ) {
                                Icon(
                                    Icons.Default
                                        .KeyboardArrowUp,
                                    "تحريك لأعلى"
                                )
                            }

                            Text(
                                "ترتيب ${index + 1}",
                                modifier =
                                    Modifier
                                        .align(
                                            Alignment
                                                .CenterVertically
                                        ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelMedium
                            )

                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11CalendarController
                                            .moveCalendar(
                                                db,
                                                calendar.id,
                                                1
                                            )
                                    )
                                },
                                enabled =
                                    index <
                                    orderedCalendars
                                        .lastIndex
                            ) {
                                Icon(
                                    Icons.Default
                                        .KeyboardArrowDown,
                                    "تحريك لأسفل"
                                )
                            }
                        }
                    }
                }
            }

        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (newCalendarName.isNotBlank()) {
                        onDatabaseChange(
                            V11CalendarController
                                .addCalendar(
                                    db,
                                    newCalendarName.trim(),
                                    "#4F709C"
                                )
                        )
                        newCalendarName = ""
                    }
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    null
                )
                Text("إضافة")
            }

            Spacer(Modifier.width(8.dp))

            OutlinedTextField(
                value = newCalendarName,
                onValueChange = {
                    newCalendarName = it
                },
                modifier =
                    Modifier.weight(1f),
                label = {
                    Text("تقويم جديد")
                },
                singleLine = true
            )
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "ملخص ${month}",
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                "أيام العمل",
                monthDays.count {
                    it.segments.any { segment ->
                        segment.kind !=
                            ShiftKind.OFF
                    }
                },
                Modifier.weight(1f)
            )

            SummaryCard(
                "الإجازات",
                offCount,
                Modifier.weight(1f)
            )

            SummaryCard(
                "الشيفتات",
                workSegments.size,
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        listOf(
            ShiftKind.FULL_DAY,
            ShiftKind.LONG,
            ShiftKind.MORNING,
            ShiftKind.EVENING,
            ShiftKind.NIGHT
        ).forEach { kind ->
            val count =
                workSegments.count {
                    it.kind == kind
                }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    count.toString(),
                    fontWeight =
                        FontWeight.Black
                )

                Text(
                    V11Labels.shift(kind)
                )
            }

            HorizontalDivider()
        }

        Spacer(Modifier.height(22.dp))

        Text(
            "أماكن العمل هذا الشهر",
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        workplaces.forEach { place ->
            val count =
                workSegments.count {
                    it.workplaceId ==
                        place.id
                }

            if (count > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        "$count شيفت"
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        place.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(Modifier.width(8.dp))

                    Box(
                        Modifier
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(
                                schedulesColor(
                                    place.colorHex
                                )
                            )
                    )
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    editingCalendar
        ?.let { calendar ->

            V11EditCalendarDialog(
                calendar = calendar,
                onDismiss = {
                    editingCalendar = null
                },
                onSave = {
                    updated ->

                    onDatabaseChange(
                        V11CalendarController
                            .updateCalendar(
                                db,
                                updated
                            )
                    )

                    editingCalendar = null
                }
            )
        }
}

@Composable
private fun V11EditCalendarDialog(
    calendar: AppCalendarV11,
    onDismiss: () -> Unit,
    onSave: (AppCalendarV11) -> Unit
) {
    var name by remember(calendar.id) {
        mutableStateOf(calendar.name)
    }

    var colorHex by remember(calendar.id) {
        mutableStateOf(calendar.colorHex)
    }

    val colors =
        listOf(
            "#4F709C",
            "#9DD8AE",
            "#FFE28A",
            "#D7ACE8",
            "#91C1EE",
            "#FFB4A2",
            "#B8C0FF",
            "#A8DADC"
        )

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text("تعديل التقويم")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("اسم التقويم")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Text(
                    "لون التقويم",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceEvenly
                ) {
                    colors
                        .take(4)
                        .forEach { hex ->
                            CalendarColorChoice(
                                hex = hex,
                                selected =
                                    colorHex == hex,
                                onClick = {
                                    colorHex = hex
                                }
                            )
                        }
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceEvenly
                ) {
                    colors
                        .drop(4)
                        .forEach { hex ->
                            CalendarColorChoice(
                                hex = hex,
                                selected =
                                    colorHex == hex,
                                onClick = {
                                    colorHex = hex
                                }
                            )
                        }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (
                        name.isNotBlank()
                    ) {
                        onSave(
                            calendar.copy(
                                name =
                                    name.trim(),
                                colorHex =
                                    colorHex
                            )
                        )
                    }
                },
                enabled =
                    name.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun CalendarColorChoice(
    hex: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier =
            Modifier
                .size(
                    if (selected)
                        42.dp
                    else
                        36.dp
                )
                .clickable {
                    onClick()
                },
        shape =
            CircleShape,
        color =
            schedulesColor(hex),
        border =
            if (selected)
                androidx.compose.foundation.BorderStroke(
                    3.dp,
                    MaterialTheme
                        .colorScheme
                        .primary
                )
            else
                null
    ) {}
}

@Composable
private fun SummaryCard(
    label: String,
    value: Int,
    modifier: Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                style =
                    MaterialTheme.typography
                        .titleLarge,
                fontWeight = FontWeight.Black
            )

            Text(
                label,
                style =
                    MaterialTheme.typography
                        .labelSmall
            )
        }
    }
}
