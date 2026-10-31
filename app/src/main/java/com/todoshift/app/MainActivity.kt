package com.todoshift.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToDoShiftApp()
            }
        }
    }
}

data class WorkPlace(
    val id: Int,
    val name: String,
    val color: Color
)

enum class ShiftLayout {
    FULL_DAY,
    LONG_NIGHT,
    THREE_SHIFTS,
    OFF
}

data class DaySchedule(
    val layout: ShiftLayout,
    val places: List<Int?>
)

private val defaultPlaces = listOf(
    WorkPlace(1, "مطامير", Color(0xFF9DD8AE)),
    WorkPlace(2, "الشاملة", Color(0xFFFFE28A)),
    WorkPlace(3, "البنا", Color(0xFFD7ACE8)),
    WorkPlace(4, "الماستر", Color(0xFF91C1EE))
)

@Composable
fun ToDoShiftApp() {
    var currentTab by remember { mutableIntStateOf(0) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var schedules by remember { mutableStateOf(mapOf<LocalDate, DaySchedule>()) }
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F8FB)
    ) {
        Scaffold(
            containerColor = Color(0xFFF7F8FB),
            bottomBar = {
                BottomNavigation(
                    selected = currentTab,
                    onSelect = { currentTab = it }
                )
            }
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
            ) {
                when (currentTab) {
                    0 -> CalendarScreen(
                        month = currentMonth,
                        schedules = schedules,
                        onPrevious = {
                            currentMonth = currentMonth.minusMonths(1)
                        },
                        onNext = {
                            currentMonth = currentMonth.plusMonths(1)
                        },
                        onDayClick = {
                            editingDate = it
                        }
                    )

                    1 -> SimplePage(
                        title = "جداولي",
                        description = "إدارة جداول العمل والتكليفات"
                    )

                    2 -> SimplePage(
                        title = "المالية",
                        description = "المستحقات والقبض والتقارير المالية"
                    )

                    3 -> SimplePage(
                        title = "المزيد",
                        description = "أماكن العمل والتخصيص والإعدادات"
                    )
                }
            }
        }

        editingDate?.let { date ->
            DayEditorDialog(
                date = date,
                existing = schedules[date],
                places = defaultPlaces,
                onDismiss = {
                    editingDate = null
                },
                onDelete = {
                    schedules = schedules - date
                    editingDate = null
                },
                onSave = { schedule ->
                    schedules = schedules + (date to schedule)
                    editingDate = null
                }
            )
        }
    }
}

@Composable
fun CalendarScreen(
    month: YearMonth,
    schedules: Map<LocalDate, DaySchedule>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {

        Spacer(Modifier.height(10.dp))

        Text(
            "ToDo Shift",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "جدول الشيفتات",
            fontSize = 14.sp,
            color = Color(0xFF7A7E86)
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            FilledTonalButton(
                onClick = onPrevious,
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text("‹", fontSize = 28.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    arabicMonth(month.monthValue),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    month.year.toString(),
                    color = Color.Gray
                )
            }

            FilledTonalButton(
                onClick = onNext,
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Text("›", fontSize = 28.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        WeekHeader()

        Spacer(Modifier.height(6.dp))

        MonthGrid(
            month = month,
            schedules = schedules,
            onDayClick = onDayClick
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "أماكن العمل",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            defaultPlaces.forEach {
                LegendItem(it)
            }
        }
    }
}

@Composable
fun WeekHeader() {
    val days = listOf(
        "سبت", "أحد", "اثن", "ثلا", "أرب", "خمي", "جمع"
    )

    Row(Modifier.fillMaxWidth()) {
        days.forEach {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MonthGrid(
    month: YearMonth,
    schedules: Map<LocalDate, DaySchedule>,
    onDayClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)

    val offset = when (first.dayOfWeek.value) {
        6 -> 0
        7 -> 1
        1 -> 2
        2 -> 3
        3 -> 4
        4 -> 5
        else -> 6
    }

    val cells = offset + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { column ->
                    val cell = row * 7 + column
                    val day = cell - offset + 1

                    if (day in 1..month.lengthOfMonth()) {
                        val date = month.atDay(day)

                        DayCell(
                            day = day,
                            schedule = schedules[date],
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDayClick(date)
                            }
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DayCell(
    day: Int,
    schedule: DaySchedule?,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                1.dp,
                Color(0xFFE1E3E7),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
    ) {

        schedule?.let {
            when (it.layout) {

                ShiftLayout.FULL_DAY -> {
                    SchedulePart(
                        placeId = it.places.getOrNull(0),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                ShiftLayout.LONG_NIGHT -> {
                    Column(Modifier.fillMaxSize()) {
                        SchedulePart(
                            placeId = it.places.getOrNull(0),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        SchedulePart(
                            placeId = it.places.getOrNull(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                ShiftLayout.THREE_SHIFTS -> {
                    Column(Modifier.fillMaxSize()) {
                        repeat(3) { index ->
                            SchedulePart(
                                placeId = it.places.getOrNull(index),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }
                }

                ShiftLayout.OFF -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF1F2F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "إجازة",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(5.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SchedulePart(
    placeId: Int?,
    modifier: Modifier
) {
    val place = defaultPlaces.firstOrNull { it.id == placeId }

    Box(
        modifier = modifier.background(
            place?.color ?: Color(0xFFF1F2F4)
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            place?.name ?: "غير محدد",
            fontSize = 8.sp,
            color = Color(0xFF24272B)
        )
    }
}

@Composable
fun DayEditorDialog(
    date: LocalDate,
    existing: DaySchedule?,
    places: List<WorkPlace>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (DaySchedule) -> Unit
) {
    var layout by remember(date) {
        mutableStateOf(existing?.layout ?: ShiftLayout.FULL_DAY)
    }

    var selections by remember(date) {
        mutableStateOf(
            existing?.places ?: listOf<Int?>(places.first().id)
        )
    }

    fun partCount(): Int =
        when (layout) {
            ShiftLayout.FULL_DAY -> 1
            ShiftLayout.LONG_NIGHT -> 2
            ShiftLayout.THREE_SHIFTS -> 3
            ShiftLayout.OFF -> 0
        }

    fun normalize() {
        val count = partCount()

        selections =
            if (count == 0) {
                emptyList()
            } else {
                List(count) { index ->
                    selections.getOrNull(index) ?: places.first().id
                }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "تعديل اليوم",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${date.dayOfMonth} ${arabicMonth(date.monthValue)} ${date.year}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        },

        text = {
            Column {

                Text(
                    "تقسيم اليوم",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                LayoutOption(
                    title = "24 ساعة",
                    selected = layout == ShiftLayout.FULL_DAY
                ) {
                    layout = ShiftLayout.FULL_DAY
                    normalize()
                }

                LayoutOption(
                    title = "Long / Night",
                    selected = layout == ShiftLayout.LONG_NIGHT
                ) {
                    layout = ShiftLayout.LONG_NIGHT
                    normalize()
                }

                LayoutOption(
                    title = "Morning / Evening / Night",
                    selected = layout == ShiftLayout.THREE_SHIFTS
                ) {
                    layout = ShiftLayout.THREE_SHIFTS
                    normalize()
                }

                LayoutOption(
                    title = "إجازة",
                    selected = layout == ShiftLayout.OFF
                ) {
                    layout = ShiftLayout.OFF
                    normalize()
                }

                if (partCount() > 0) {

                    Spacer(Modifier.height(14.dp))

                    Text(
                        "مكان العمل",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    repeat(partCount()) { index ->

                        val label =
                            when (layout) {
                                ShiftLayout.FULL_DAY ->
                                    "اليوم بالكامل"

                                ShiftLayout.LONG_NIGHT ->
                                    if (index == 0) "Long" else "Night"

                                ShiftLayout.THREE_SHIFTS ->
                                    listOf(
                                        "Morning",
                                        "Evening",
                                        "Night"
                                    )[index]

                                else -> ""
                            }

                        Text(
                            label,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(5.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(5.dp)
                        ) {
                            places.forEach { place ->

                                val selected =
                                    selections.getOrNull(index) == place.id

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(place.color)
                                        .border(
                                            if (selected) 3.dp else 1.dp,
                                            if (selected)
                                                Color.Black
                                            else
                                                Color.Transparent,
                                            RoundedCornerShape(9.dp)
                                        )
                                        .clickable {
                                            val newList =
                                                selections.toMutableList()

                                            while (newList.size < partCount()) {
                                                newList.add(places.first().id)
                                            }

                                            newList[index] = place.id
                                            selections = newList
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        place.name,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    normalize()

                    onSave(
                        DaySchedule(
                            layout = layout,
                            places = selections
                        )
                    )
                }
            ) {
                Text("حفظ")
            }
        },

        dismissButton = {
            Row {
                if (existing != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            "حذف",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

@Composable
fun LayoutOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )

        Text(title)
    }
}

@Composable
fun LegendItem(place: WorkPlace) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(11.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(place.color)
        )

        Spacer(Modifier.width(4.dp))

        Text(
            place.name,
            fontSize = 9.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun BottomNavigation(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White
    ) {
        val labels = listOf(
            "التقويم",
            "جداولي",
            "المالية",
            "المزيد"
        )

        labels.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selected == index,
                onClick = {
                    onSelect(index)
                },
                icon = {
                    Text(
                        when (index) {
                            0 -> "▦"
                            1 -> "☷"
                            2 -> "£"
                            else -> "⚙"
                        },
                        fontSize = 18.sp
                    )
                },
                label = {
                    Text(label)
                }
            )
        }
    }
}

@Composable
fun SimplePage(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        Text(
            title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            description,
            color = Color.Gray
        )

        Spacer(Modifier.height(30.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "هذه الشاشة أصبحت جزءًا فعليًا من التنقل، وسيتم بناء وظائفها في المرحلة التالية.",
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

fun arabicMonth(month: Int): String =
    listOf(
        "يناير",
        "فبراير",
        "مارس",
        "أبريل",
        "مايو",
        "يونيو",
        "يوليو",
        "أغسطس",
        "سبتمبر",
        "أكتوبر",
        "نوفمبر",
        "ديسمبر"
    )[month - 1]
