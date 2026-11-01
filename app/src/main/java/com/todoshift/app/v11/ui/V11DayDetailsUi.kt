package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate

@Composable
fun V11DayDetailsScreen(
    initialDate: LocalDate,
    db: V11Database,
    workplaces: List<Workplace>,
    onBack: () -> Unit,
    onDatabaseChange: (V11Database) -> Unit
) {
    var date by remember(initialDate) {
        mutableStateOf(initialDate)
    }

    var addType by remember {
        mutableStateOf<DayItemTypeV11?>(null)
    }

    val entries = db.days.filter {
        it.date == date.toString()
    }

    val items = db.dayItems
        .filter { it.date == date.toString() }
        .sortedBy { it.createdAt }

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(58.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "رجوع"
                        )
                    }

                    IconButton(
                        onClick = {
                            date = date.minusDays(1)
                        }
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            "اليوم السابق"
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {
                        Text(
                            date.toString(),
                            fontWeight = FontWeight.Black
                        )

                        if (date == LocalDate.now()) {
                            Text(
                                "اليوم",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall,
                                color = Color(0xFF4F709C)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            date = date.plusDays(1)
                        }
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            "اليوم التالي"
                        )
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(14.dp)
        ) {

            Text(
                "الشيفتات",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            if (entries.flatMap { it.segments }.isEmpty()) {
                V11EmptyCard(
                    "لا توجد شيفتات في هذا اليوم"
                )
            }

            entries.forEach { entry ->
                val calendar =
                    db.calendars.firstOrNull {
                        it.id == entry.calendarId
                    }

                entry.segments.forEach { segment ->
                    val place =
                        workplaces.firstOrNull {
                            it.id == segment.workplaceId
                        }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    onDatabaseChange(
                                        V11DayController
                                            .deleteSegment(
                                                db,
                                                segment.id
                                            )
                                    )
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    "حذف",
                                    tint =
                                        MaterialTheme.colorScheme
                                            .error
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            Column(
                                horizontalAlignment =
                                    Alignment.End
                            ) {
                                Text(
                                    V11Labels.shift(
                                        segment.kind
                                    ),
                                    fontWeight =
                                        FontWeight.Black
                                )

                                Text(
                                    listOfNotNull(
                                        place?.name,
                                        calendar?.name
                                    ).joinToString(" • "),
                                    style =
                                        MaterialTheme.typography
                                            .bodySmall
                                )

                                segment.timeRange()
                                    ?.let { range ->
                                        Text(
                                            "${timeText(range.startMinute)} - ${timeText(range.endMinute)}",
                                            style =
                                                MaterialTheme.typography
                                                    .labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                V11AddItemButton(
                    "مهمة",
                    Icons.Default.TaskAlt,
                    Modifier.weight(1f)
                ) {
                    addType = DayItemTypeV11.TASK
                }

                V11AddItemButton(
                    "مشوار",
                    Icons.Default.DirectionsCar,
                    Modifier.weight(1f)
                ) {
                    addType = DayItemTypeV11.ERRAND
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                V11AddItemButton(
                    "تذكير",
                    Icons.Default.Notifications,
                    Modifier.weight(1f)
                ) {
                    addType =
                        DayItemTypeV11.REMINDER
                }

                V11AddItemButton(
                    "ملاحظة",
                    Icons.Default.Notes,
                    Modifier.weight(1f)
                ) {
                    addType = DayItemTypeV11.NOTE
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "تنظيم اليوم",
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(8.dp))

            if (items.isEmpty()) {
                V11EmptyCard(
                    "لا توجد مهام أو مشاوير أو تذكيرات"
                )
            }

            items.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onDatabaseChange(
                                    V11DayOrganizerController
                                        .delete(
                                            db,
                                            item.id
                                        )
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "حذف"
                            )
                        }

                        if (
                            item.type !=
                            DayItemTypeV11.NOTE
                        ) {
                            Checkbox(
                                checked = item.completed,
                                onCheckedChange = {
                                    onDatabaseChange(
                                        V11DayOrganizerController
                                            .toggle(
                                                db,
                                                item.id
                                            )
                                    )
                                }
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {
                            Text(
                                item.title,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            if (item.details.isNotBlank()) {
                                Text(
                                    item.details,
                                    style =
                                        MaterialTheme.typography
                                            .bodySmall
                                )
                            }

                            Text(
                                itemTypeLabel(item.type),
                                style =
                                    MaterialTheme.typography
                                        .labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    addType?.let { type ->
        V11AddDayItemDialog(
            type = type,
            onDismiss = {
                addType = null
            },
            onSave = { title, details ->
                val calendarId =
                    db.calendars
                        .sortedBy { it.order }
                        .firstOrNull { it.visible }
                        ?.id

                onDatabaseChange(
                    V11DayOrganizerController.add(
                        db = db,
                        date = date.toString(),
                        calendarId = calendarId,
                        type = type,
                        title = title,
                        details = details
                    )
                )

                addType = null
            }
        )
    }
}

@Composable
private fun V11EmptyCard(
    text: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF2F4F7)
    ) {
        Text(
            text,
            modifier = Modifier.padding(14.dp),
            color = Color.Gray
        )
    }
}

@Composable
private fun V11AddItemButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text)
    }
}

@Composable
private fun V11AddDayItemDialog(
    type: DayItemTypeV11,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember {
        mutableStateOf("")
    }

    var details by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "إضافة ${itemTypeLabel(type)}"
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("العنوان")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = details,
                    onValueChange = {
                        details = it
                    },
                    label = {
                        Text("التفاصيل")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, details)
                },
                enabled = title.isNotBlank()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

private fun itemTypeLabel(
    type: DayItemTypeV11
): String =
    when (type) {
        DayItemTypeV11.TASK -> "مهمة"
        DayItemTypeV11.ERRAND -> "مشوار"
        DayItemTypeV11.REMINDER -> "تذكير"
        DayItemTypeV11.NOTE -> "ملاحظة"
    }

private fun timeText(
    minute: Int
): String {
    if (minute == 1440) return "24:00"

    val h = minute / 60
    val m = minute % 60

    return "%02d:%02d".format(h, m)
}
