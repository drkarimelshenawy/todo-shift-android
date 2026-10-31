package com.todoshift.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Notes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.todoshift.app.data.DayEngine
import com.todoshift.app.data.PainterSelection
import com.todoshift.app.data.ShiftPainterEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF213555),
                    secondary = Color(0xFF4F709C),
                    background = Color(0xFFF7F8FA),
                    surface = Color.White
                )
            ) {
                ToDoShiftApp()
            }
        }
    }
}

private fun hexColor(hex: String): Color {
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF90CAF9)
    }
}

private val palette = listOf(
    "#9DD8AE",
    "#FFE28A",
    "#D7ACE8",
    "#91C1EE",
    "#FFAB91",
    "#80CBC4",
    "#F48FB1",
    "#B0BEC5",
    "#CE93D8",
    "#A5D6A7",
    "#FFCC80",
    "#90CAF9"
)

@Composable
fun ToDoShiftApp() {
    val context = LocalContext.current
    val storage = remember { AppStorage(context.applicationContext) }

    var appData by remember {
        mutableStateOf(storage.load())
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    fun updateData(newData: AppData) {
        appData = newData
        storage.save(newData)
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        bottomBar = {
            BottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
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
                0 -> V09CalendarScreen(
                    data = appData,
                    currentMonth = currentMonth,
                    onMonthChange = { currentMonth = it },
                    onDataChange = { updateData(it) }
                )

                1 -> SchedulesDashboard(
                    month = currentMonth,
                    data = appData,
                    onPrevious = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) },
                    onOpenDay = {
                        currentMonth = YearMonth.from(it)
                        editingDate = it
                        currentTab = 0
                    }
                )

                2 -> FinanceScreen(
                    month = currentMonth,
                    data = appData,
                    onPrevious = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) },
                    onSaveData = { updateData(it) }
                )

                3 -> MoreScreen(
                    data = appData,
                    onSaveData = { updateData(it) }
                )
            }
        }
    }

    editingDate?.let { date ->
        val existing = appData.shifts.firstOrNull {
            it.date == date.toString()
        }

        DayEditorDialog(
            date = date,
            existing = existing,
            workplaces = appData.workplaces,
            onDismiss = { editingDate = null },
            onDelete = {
                val updated = appData.copy(
                    shifts = appData.shifts.filterNot {
                        it.date == date.toString()
                    }
                )
                updateData(updated)
                editingDate = null
            },
            onSave = { entry ->
                val shifts = appData.shifts
                    .filterNot { it.date == date.toString() }
                    .toMutableList()

                shifts += entry

                updateData(
                    appData.copy(
                        shifts = shifts.sortedBy { it.date }
                    )
                )

                editingDate = null
            }
        )
    }
}

@Composable
private fun AppHeader(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.End
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun MonthSelector(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalButton(onClick = onPrevious) {
            Text("‹")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${arabicMonth(month.monthValue)} ${month.year}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (onToday != null) {
                TextButton(
                    onClick = onToday,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("الشهر الحالي", fontSize = 12.sp)
                }
            }
        }

        FilledTonalButton(onClick = onNext) {
            Text("›")
        }
    }
}

@Composable
private fun CalendarScreen(
    month: YearMonth,
    data: AppData,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 24.dp)
    ) {
        AppHeader(
            title = "ToDo Shift",
            subtitle = "جدول الشيفتات"
        )

        Spacer(Modifier.height(12.dp))

        MonthSelector(
            month = month,
            onPrevious = onPrevious,
            onNext = onNext,
            onToday = onToday
        )

        Spacer(Modifier.height(8.dp))
        WeekHeader()
        Spacer(Modifier.height(4.dp))

        MonthGrid(
            month = month,
            data = data,
            onDayClick = onDayClick
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "أماكن العمل",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(8.dp))

        if (data.workplaces.isEmpty()) {
            Text(
                "أضف أماكن العمل من تبويب المزيد.",
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        } else {
            data.workplaces.forEach { place ->
                WorkplaceLegend(place)
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun WeekHeader() {
    val days = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")

    Row(Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B5563)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    data: AppData,
    onDayClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val firstIndex = (first.dayOfWeek.value + 2) % 7
    val total = firstIndex + month.lengthOfMonth()
    val rows = (total + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                for (col in 0..6) {
                    val cell = row * 7 + col
                    val day = cell - firstIndex + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                    ) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val shift = data.shifts.firstOrNull {
                                it.date == date.toString()
                            }

                            DayCell(
                                date = date,
                                shift = shift,
                                workplaces = data.workplaces,
                                onClick = { onDayClick(date) }
                            )
                        } else {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(62.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    shift: ShiftEntry?,
    workplaces: List<Workplace>,
    onClick: () -> Unit
) {
    val today = date == LocalDate.now()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White)
            .then(
                if (today)
                    Modifier.border(
                        2.dp,
                        Color(0xFF213555),
                        RoundedCornerShape(9.dp)
                    )
                else
                    Modifier.border(
                        1.dp,
                        Color(0xFFE5E7EB),
                        RoundedCornerShape(9.dp)
                    )
            )
            .clickable(onClick = onClick)
    ) {
        when (shift?.type) {
            ShiftType.FULL_DAY -> {
                ShiftPart(
                    workplaceId = shift.workplaceIds.getOrNull(0),
                    workplaces = workplaces,
                    modifier = Modifier.fillMaxSize()
                )
            }

            ShiftType.LONG_NIGHT -> {
                Column(Modifier.fillMaxSize()) {
                    ShiftPart(
                        shift.workplaceIds.getOrNull(0),
                        workplaces,
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    ShiftPart(
                        shift.workplaceIds.getOrNull(1),
                        workplaces,
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            ShiftType.THREE_SHIFTS -> {
                Column(Modifier.fillMaxSize()) {
                    repeat(3) { index ->
                        ShiftPart(
                            shift.workplaceIds.getOrNull(index),
                            workplaces,
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            ShiftType.OFF -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF1F3F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "إجازة",
                        fontSize = 9.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            null -> Unit
        }

        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    if (today) Color(0xFF213555)
                    else Color.White.copy(alpha = 0.92f)
                )
                .padding(horizontal = 5.dp, vertical = 2.dp),
            color = if (today) Color.White else Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )

        if (!shift?.note.isNullOrBlank()) {
            Text(
                "•",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp),
                fontSize = 18.sp,
                color = Color(0xFF213555)
            )
        }
    }
}

@Composable
private fun ShiftPart(
    workplaceId: String?,
    workplaces: List<Workplace>,
    modifier: Modifier
) {
    val place = workplaces.firstOrNull { it.id == workplaceId }

    Box(
        modifier = modifier.background(
            place?.let { hexColor(it.colorHex) }
                ?: Color(0xFFF3F4F6)
        )
    )
}

@Composable
private fun WorkplaceLegend(place: Workplace) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            place.name,
            fontSize = 14.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(hexColor(place.colorHex))
        )
    }
}

@Composable
private fun DayEditorDialog(
    date: LocalDate,
    existing: ShiftEntry?,
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (ShiftEntry) -> Unit
) {
    var type by remember(existing) {
        mutableStateOf(existing?.type ?: ShiftType.FULL_DAY)
    }

    var selected by remember(existing) {
        mutableStateOf(
            existing?.workplaceIds ?: listOf(
                workplaces.firstOrNull()?.id
            )
        )
    }

    var note by remember(existing) {
        mutableStateOf(existing?.note ?: "")
    }

    var customAmount by remember(existing) {
        mutableStateOf(
            existing?.customAmount?.let {
                if (it == 0.0) "" else it.toString()
            } ?: ""
        )
    }

    fun requiredSegments(t: ShiftType): Int =
        when (t) {
            ShiftType.FULL_DAY -> 1
            ShiftType.LONG_NIGHT -> 2
            ShiftType.THREE_SHIFTS -> 3
            ShiftType.OFF -> 0
        }

    fun normalizeSelection(count: Int) {
        val mutable = selected.toMutableList()

        while (mutable.size < count) {
            mutable += workplaces.firstOrNull()?.id
        }

        selected = mutable.take(count)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تعديل ${date.dayOfMonth} ${arabicMonth(date.monthValue)}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "تقسيم اليوم",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                ShiftTypeOption(
                    title = "24 ساعة",
                    selected = type == ShiftType.FULL_DAY
                ) {
                    type = ShiftType.FULL_DAY
                    normalizeSelection(1)
                }

                ShiftTypeOption(
                    title = "Long / Night",
                    selected = type == ShiftType.LONG_NIGHT
                ) {
                    type = ShiftType.LONG_NIGHT
                    normalizeSelection(2)
                }

                ShiftTypeOption(
                    title = "Morning / Evening / Night",
                    selected = type == ShiftType.THREE_SHIFTS
                ) {
                    type = ShiftType.THREE_SHIFTS
                    normalizeSelection(3)
                }

                ShiftTypeOption(
                    title = "إجازة",
                    selected = type == ShiftType.OFF
                ) {
                    type = ShiftType.OFF
                    selected = emptyList()
                }

                if (type != ShiftType.OFF) {
                    Spacer(Modifier.height(14.dp))

                    val labels = when (type) {
                        ShiftType.FULL_DAY ->
                            listOf("مكان العمل")

                        ShiftType.LONG_NIGHT ->
                            listOf("Long", "Night")

                        ShiftType.THREE_SHIFTS ->
                            listOf("Morning", "Evening", "Night")

                        else -> emptyList()
                    }

                    labels.forEachIndexed { index, label ->
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Spacer(Modifier.height(5.dp))

                        if (workplaces.isEmpty()) {
                            Text(
                                "لا توجد أماكن عمل. أضفها من المزيد.",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        } else {
                            workplaces.forEach { place ->
                                val active =
                                    selected.getOrNull(index) == place.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (active)
                                                hexColor(place.colorHex)
                                                    .copy(alpha = 0.45f)
                                            else
                                                Color(0xFFF7F7F7)
                                        )
                                        .clickable {
                                            val mutable =
                                                selected.toMutableList()

                                            while (mutable.size <= index) {
                                                mutable += null
                                            }

                                            mutable[index] = place.id
                                            selected = mutable
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = active,
                                        onClick = null
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Text(
                                        place.name,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End
                                    )

                                    Box(
                                        Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(
                                                hexColor(place.colorHex)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظة") },
                    singleLine = false,
                    minLines = 2
                )

                if (type != ShiftType.OFF) {
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = {
                            customAmount =
                                it.filter { ch ->
                                    ch.isDigit() || ch == '.'
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("مبلغ استثنائي لليوم - اختياري")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = requiredSegments(type)

                    onSave(
                        ShiftEntry(
                            date = date.toString(),
                            type = type,
                            workplaceIds =
                                if (type == ShiftType.OFF)
                                    emptyList()
                                else
                                    selected.take(count),
                            note = note.trim(),
                            customAmount =
                                customAmount.toDoubleOrNull()
                        )
                    )
                },
                enabled =
                    type == ShiftType.OFF ||
                        (
                            workplaces.isNotEmpty() &&
                                selected.take(
                                    requiredSegments(type)
                                ).all { it != null }
                            )
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
                            color = Color(0xFFB42318)
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
private fun ShiftTypeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected)
                    Color(0xFFE8EEF7)
                else
                    Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )

        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SchedulesDashboard(
    month: YearMonth,
    data: AppData,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val monthEntries = data.shifts
        .filter {
            runCatching {
                YearMonth.from(LocalDate.parse(it.date)) == month
            }.getOrDefault(false)
        }
        .sortedBy { it.date }

    val workEntries =
        monthEntries.filter { it.type != ShiftType.OFF }

    val offCount =
        monthEntries.count { it.type == ShiftType.OFF }

    val segmentCount: Int =
        workEntries.fold(0) { total, entry ->
            total + when (entry.type) {
                ShiftType.FULL_DAY -> 1
                ShiftType.LONG_NIGHT -> 2
                ShiftType.THREE_SHIFTS -> 3
                ShiftType.OFF -> 0
            }
        }

    val finance = FinanceEngine.month(month, data)

    val nextShift = data.shifts
        .filter {
            it.type != ShiftType.OFF &&
                runCatching {
                    !LocalDate.parse(it.date)
                        .isBefore(LocalDate.now())
                }.getOrDefault(false)
        }
        .minByOrNull { it.date }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AppHeader(
            "جداولي",
            "ملخص العمل والشيفتات"
        )

        Spacer(Modifier.height(12.dp))

        MonthSelector(
            month = month,
            onPrevious = onPrevious,
            onNext = onNext
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth()) {
            DashboardCard(
                "أيام العمل",
                workEntries.size.toString(),
                Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            DashboardCard(
                "الشيفتات",
                segmentCount.toString(),
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth()) {
            DashboardCard(
                "الإجازات",
                offCount.toString(),
                Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            DashboardCard(
                "المستحق",
                money(finance.expected),
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "الشيفت القادم",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(8.dp))

        if (nextShift == null) {
            InfoBox("لا يوجد شيفت قادم مسجل.")
        } else {
            val date = LocalDate.parse(nextShift.date)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenDay(date)
                    }
            ) {
                Column(
                    Modifier.padding(14.dp)
                ) {
                    Text(
                        "${date.dayOfMonth} ${arabicMonth(date.monthValue)} ${date.year}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        shiftDescription(
                            nextShift,
                            data.workplaces
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "شيفتات الشهر",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(8.dp))

        if (monthEntries.isEmpty()) {
            InfoBox("لم تسجل أي أيام في هذا الشهر.")
        } else {
            monthEntries.forEach { entry ->
                val date = LocalDate.parse(entry.date)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp)
                        .clickable {
                            onOpenDay(date)
                        }
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (entry.type == ShiftType.OFF)
                                "إجازة"
                            else
                                money(
                                    FinanceEngine.shiftAmount(
                                        entry,
                                        data.workplaces
                                    )
                                ),
                            color = Color(0xFF4B5563),
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "${date.dayOfMonth} ${arabicMonth(date.monthValue)}",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                shiftDescription(
                                    entry,
                                    data.workplaces
                                ),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF213555)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun FinanceScreen(
    month: YearMonth,
    data: AppData,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSaveData: (AppData) -> Unit
) {
    var showPaymentDialog by remember {
        mutableStateOf(false)
    }

    val finance = FinanceEngine.month(month, data)

    val monthPayments = data.payments
        .filter {
            runCatching {
                YearMonth.from(LocalDate.parse(it.date)) == month
            }.getOrDefault(false)
        }
        .sortedByDescending { it.date }

    val status = when {
        finance.expected <= 0.0 &&
            finance.received <= 0.0 ->
            "لا توجد مستحقات"

        finance.received <= 0.0 ->
            "غير مدفوع"

        finance.received < finance.expected ->
            "مدفوع جزئيًا"

        finance.received == finance.expected ->
            "مدفوع بالكامل"

        else ->
            "يوجد مبلغ زائد"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AppHeader(
            "المالية",
            "الحساب الشهري للشيفتات"
        )

        Spacer(Modifier.height(12.dp))

        MonthSelector(
            month = month,
            onPrevious = onPrevious,
            onNext = onNext
        )

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp)
            ) {
                FinanceRow(
                    "عدد أيام العمل",
                    finance.shiftCount.toString()
                )

                FinanceRow(
                    "إجمالي المستحق",
                    "${money(finance.expected)} ج"
                )

                FinanceRow(
                    "تم استلام",
                    "${money(finance.received)} ج"
                )

                FinanceRow(
                    if (finance.balance >= 0)
                        "المتبقي"
                    else
                        "الزيادة",
                    "${money(kotlin.math.abs(finance.balance))} ج"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    status,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Bold,
                    color = when (status) {
                        "مدفوع بالكامل" ->
                            Color(0xFF137333)

                        "يوجد مبلغ زائد" ->
                            Color(0xFF7B1FA2)

                        "غير مدفوع" ->
                            Color(0xFFB42318)

                        else ->
                            Color(0xFF9A6700)
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                showPaymentDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تسجيل دفعة")
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "الدفعات",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(8.dp))

        if (monthPayments.isEmpty()) {
            InfoBox("لا توجد دفعات مسجلة لهذا الشهر.")
        } else {
            monthPayments.forEach { payment ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onSaveData(
                                    data.copy(
                                        payments =
                                            data.payments.filterNot {
                                                it.id == payment.id
                                            }
                                    )
                                )
                            }
                        ) {
                            Text(
                                "حذف",
                                color = Color(0xFFB42318)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "${money(payment.amount)} ج",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                payment.date,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            if (payment.note.isNotBlank()) {
                                Text(
                                    payment.note,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "تفاصيل أماكن العمل",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(8.dp))

        data.workplaces.forEach { place ->
            val amount = data.shifts
                .filter {
                    runCatching {
                        YearMonth.from(
                            LocalDate.parse(it.date)
                        ) == month
                    }.getOrDefault(false) &&
                        it.workplaceIds.contains(place.id)
                }
                .sumOf {
                    FinanceEngine.shiftAmount(
                        it,
                        data.workplaces
                    )
                }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${money(amount)} ج",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.weight(1f))

                    Text(place.name)

                    Spacer(Modifier.width(8.dp))

                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(hexColor(place.colorHex))
                    )
                }
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            month = month,
            onDismiss = {
                showPaymentDialog = false
            },
            onSave = { amount, note ->
                val payment = Payment(
                    id = UUID.randomUUID().toString(),
                    date = LocalDate.now().let {
                        if (YearMonth.from(it) == month)
                            it.toString()
                        else
                            month.atDay(1).toString()
                    },
                    amount = amount,
                    note = note
                )

                onSaveData(
                    data.copy(
                        payments =
                            data.payments + payment
                    )
                )

                showPaymentDialog = false
            }
        )
    }
}

@Composable
private fun FinanceRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            value,
            fontWeight = FontWeight.Bold
        )

        Text(label)
    }
}

@Composable
private fun PaymentDialog(
    month: YearMonth,
    onDismiss: () -> Unit,
    onSave: (Double, String) -> Unit
) {
    var amount by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "تسجيل دفعة - ${arabicMonth(month.monthValue)}"
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount =
                            it.filter { ch ->
                                ch.isDigit() || ch == '.'
                            }
                    },
                    label = {
                        Text("المبلغ")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("ملاحظة - اختياري")
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        amount.toDouble(),
                        note.trim()
                    )
                },
                enabled =
                    amount.toDoubleOrNull()
                        ?.let { it > 0.0 } == true
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun MoreScreen(
    data: AppData,
    onSaveData: (AppData) -> Unit
) {
    var editing by remember {
        mutableStateOf<Workplace?>(null)
    }

    var adding by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        AppHeader(
            "المزيد",
            "إعدادات ToDo Shift"
        )

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    adding = true
                }
            ) {
                Text("+ إضافة")
            }

            Spacer(Modifier.weight(1f))

            Text(
                "أماكن العمل",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(10.dp))

        if (data.workplaces.isEmpty()) {
            InfoBox(
                "لا توجد أماكن عمل. اضغط إضافة."
            )
        }

        data.workplaces.forEach { place ->
            val used =
                data.shifts.any {
                    it.workplaceIds.contains(place.id)
                }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    Modifier.padding(13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                editing = place
                            }
                        ) {
                            Text("تعديل")
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            place.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(Modifier.width(8.dp))

                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    hexColor(place.colorHex)
                                )
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "24 ساعة: ${money(place.fullDayRate)} ج  •  Long: ${money(place.longRate)} ج",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )

                    Text(
                        "Morning: ${money(place.morningRate)}  •  Evening: ${money(place.eveningRate)}  •  Night: ${money(place.nightRate)}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )

                    if (!used) {
                        TextButton(
                            onClick = {
                                onSaveData(
                                    data.copy(
                                        workplaces =
                                            data.workplaces.filterNot {
                                                it.id == place.id
                                            }
                                    )
                                )
                            }
                        ) {
                            Text(
                                "حذف المكان",
                                color = Color(0xFFB42318)
                            )
                        }
                    } else {
                        Text(
                            "المكان مستخدم في الجدول، لذلك لا يمكن حذفه.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(14.dp)
            ) {
                Text(
                    "التخزين",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "بيانات الشيفتات وأماكن العمل والأسعار والدفعات محفوظة محليًا على الهاتف.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            "ToDo Shift • V0.5",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }

    if (adding) {
        WorkplaceEditorDialog(
            initial = null,
            onDismiss = {
                adding = false
            },
            onSave = { place ->
                onSaveData(
                    data.copy(
                        workplaces =
                            data.workplaces + place
                    )
                )
                adding = false
            }
        )
    }

    editing?.let { place ->
        WorkplaceEditorDialog(
            initial = place,
            onDismiss = {
                editing = null
            },
            onSave = { changed ->
                onSaveData(
                    data.copy(
                        workplaces =
                            data.workplaces.map {
                                if (it.id == changed.id)
                                    changed
                                else
                                    it
                            }
                    )
                )
                editing = null
            }
        )
    }
}

@Composable
private fun WorkplaceEditorDialog(
    initial: Workplace?,
    onDismiss: () -> Unit,
    onSave: (Workplace) -> Unit
) {
    var name by remember(initial) {
        mutableStateOf(initial?.name ?: "")
    }

    var color by remember(initial) {
        mutableStateOf(
            initial?.colorHex ?: palette.first()
        )
    }

    var fullDay by remember(initial) {
        mutableStateOf(
            rateText(initial?.fullDayRate)
        )
    }

    var longRate by remember(initial) {
        mutableStateOf(
            rateText(initial?.longRate)
        )
    }

    var night by remember(initial) {
        mutableStateOf(
            rateText(initial?.nightRate)
        )
    }

    var morning by remember(initial) {
        mutableStateOf(
            rateText(initial?.morningRate)
        )
    }

    var evening by remember(initial) {
        mutableStateOf(
            rateText(initial?.eveningRate)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null)
                    "إضافة مكان عمل"
                else
                    "تعديل ${initial.name}"
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("اسم مكان العمل")
                    },
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "لون المكان",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                palette.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(hexColor(item))
                                    .then(
                                        if (color == item)
                                            Modifier.border(
                                                3.dp,
                                                Color.Black,
                                                CircleShape
                                            )
                                        else
                                            Modifier
                                    )
                                    .clickable {
                                        color = item
                                    }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "أسعار الشيفت",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(7.dp))

                RateField(
                    "24 ساعة",
                    fullDay
                ) {
                    fullDay = it
                }

                RateField(
                    "Long",
                    longRate
                ) {
                    longRate = it
                }

                RateField(
                    "Morning",
                    morning
                ) {
                    morning = it
                }

                RateField(
                    "Evening",
                    evening
                ) {
                    evening = it
                }

                RateField(
                    "Night",
                    night
                ) {
                    night = it
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        Workplace(
                            id = initial?.id
                                ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            colorHex = color,
                            fullDayRate =
                                fullDay.toDoubleOrNull() ?: 0.0,
                            longRate =
                                longRate.toDoubleOrNull() ?: 0.0,
                            nightRate =
                                night.toDoubleOrNull() ?: 0.0,
                            morningRate =
                                morning.toDoubleOrNull() ?: 0.0,
                            eveningRate =
                                evening.toDoubleOrNull() ?: 0.0
                        )
                    )
                },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun RateField(
    title: String,
    value: String,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onChange(
                it.filter { ch ->
                    ch.isDigit() || ch == '.'
                }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        label = {
            Text("$title - جنيه")
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = true
    )
}

@Composable
private fun InfoBox(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            textAlign = TextAlign.End,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun BottomNavigation(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = {
                onTabSelected(0)
            },
            icon = {
                Text("▦", fontSize = 20.sp)
            },
            label = {
                Text("التقويم")
            }
        )

        NavigationBarItem(
            selected = currentTab == 1,
            onClick = {
                onTabSelected(1)
            },
            icon = {
                Text("≡", fontSize = 20.sp)
            },
            label = {
                Text("جداولي")
            }
        )

        NavigationBarItem(
            selected = currentTab == 2,
            onClick = {
                onTabSelected(2)
            },
            icon = {
                Text("ج", fontSize = 18.sp)
            },
            label = {
                Text("المالية")
            }
        )

        NavigationBarItem(
            selected = currentTab == 3,
            onClick = {
                onTabSelected(3)
            },
            icon = {
                Text("•••", fontSize = 18.sp)
            },
            label = {
                Text("المزيد")
            }
        )
    }
}

private fun shiftDescription(
    entry: ShiftEntry,
    workplaces: List<Workplace>
): String {
    fun place(index: Int): String {
        val id = entry.workplaceIds.getOrNull(index)
        return workplaces
            .firstOrNull { it.id == id }
            ?.name ?: "غير محدد"
    }

    return when (entry.type) {
        ShiftType.FULL_DAY ->
            "24 ساعة • ${place(0)}"

        ShiftType.LONG_NIGHT ->
            "Long: ${place(0)} • Night: ${place(1)}"

        ShiftType.THREE_SHIFTS ->
            "Morning: ${place(0)} • Evening: ${place(1)} • Night: ${place(2)}"

        ShiftType.OFF ->
            "إجازة"
    }
}

private fun rateText(value: Double?): String {
    if (value == null || value == 0.0)
        return ""

    return if (value % 1.0 == 0.0)
        value.toLong().toString()
    else
        value.toString()
}

private fun money(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(
            Locale.US,
            "%.2f",
            value
        )
    }
}

private fun arabicMonth(month: Int): String {
    return listOf(
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
}


// ============================================================
// TODO SHIFT V0.9 - SHIFT PAINTER + DAY CENTER
// ============================================================

private data class V09PainterState(
    val selection: PainterSelection,
    val selectedDates: Set<LocalDate> = emptySet()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V09CalendarScreen(
    data: AppData,
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDataChange: (AppData) -> Unit
) {
    var painterState by remember {
        mutableStateOf<V09PainterState?>(null)
    }

    var showShiftChooser by remember {
        mutableStateOf(false)
    }

    var selectedDay by remember {
        mutableStateOf<LocalDate?>(null)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {

            AppHeader(
                title = "ToDo Shift",
                subtitle =
                    if (painterState == null)
                        "اضغط على + لإضافة الشيفتات"
                    else
                        "وضع توزيع الشيفتات"
            )

            MonthSelector(
                month = currentMonth,
                onPrevious = {
                    onMonthChange(
                        currentMonth.minusMonths(1)
                    )
                },
                onNext = {
                    onMonthChange(
                        currentMonth.plusMonths(1)
                    )
                },
                onToday = {
                    onMonthChange(
                        YearMonth.now()
                    )
                }
            )

            if (painterState != null) {
                V09PainterBanner(
                    data = data,
                    state = painterState!!,
                    onCancel = {
                        painterState = null
                    }
                )
            }

            WeekHeader()

            V09MonthGrid(
                month = currentMonth,
                data = data,
                painterState = painterState,
                onDayClick = { date ->

                    val state = painterState

                    if (state == null) {
                        selectedDay = date
                    } else {
                        val dates =
                            state.selectedDates.toMutableSet()

                        if (date in dates) {
                            dates.remove(date)
                        } else {
                            dates.add(date)
                        }

                        painterState =
                            state.copy(
                                selectedDates = dates
                            )
                    }
                }
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            data.workplaces.forEach { place ->
                WorkplaceLegend(place)
                Spacer(Modifier.height(5.dp))
            }

            if (painterState != null) {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Text(
                    text =
                        "تم تحديد ${painterState!!.selectedDates.size} يوم",
                    modifier =
                        Modifier.padding(horizontal = 20.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 22.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            if (painterState != null) {
                FloatingActionButton(
                    onClick = {
                        painterState = null
                    },
                    containerColor =
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "إلغاء"
                    )
                }
            }

            FloatingActionButton(
                onClick = {

                    val state = painterState

                    if (state == null) {
                        showShiftChooser = true
                    } else {
                        if (state.selectedDates.isNotEmpty()) {
                            val updated =
                                ShiftPainterEngine.apply(
                                    data = data,
                                    dates =
                                        state.selectedDates,
                                    selection =
                                        state.selection
                                )

                            onDataChange(updated)
                            painterState = null
                        }
                    }
                },
                containerColor =
                    if (painterState == null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.tertiary
            ) {
                Icon(
                    if (painterState == null)
                        Icons.Default.Add
                    else
                        Icons.Default.Check,
                    contentDescription =
                        if (painterState == null)
                            "إضافة شيفت"
                        else
                            "حفظ الشيفتات"
                )
            }
        }
    }

    if (showShiftChooser) {
        V09ShiftChooserSheet(
            data = data,
            onDismiss = {
                showShiftChooser = false
            },
            onChoose = { selection ->
                painterState =
                    V09PainterState(
                        selection = selection
                    )

                showShiftChooser = false
            }
        )
    }

    selectedDay?.let { date ->
        V09DayCenterDialog(
            date = date,
            data = data,
            onDismiss = {
                selectedDay = null
            },
            onDataChange = onDataChange
        )
    }
}

@Composable
private fun V09PainterBanner(
    data: AppData,
    state: V09PainterState,
    onCancel: () -> Unit
) {
    val names =
        state.selection.workplaceIds
            .mapNotNull { id ->
                data.workplaces
                    .firstOrNull {
                        it.id == id
                    }
                    ?.name
            }
            .distinct()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        shape =
            RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text = "جاري توزيع الشيفت",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        "${v09ShiftName(state.selection.type)} • " +
                        names.joinToString(" / "),
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = onCancel
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "إلغاء"
                )
            }
        }
    }
}

@Composable
private fun V09MonthGrid(
    month: YearMonth,
    data: AppData,
    painterState: V09PainterState?,
    onDayClick: (LocalDate) -> Unit
) {
    val first = month.atDay(1)

    // Saturday = 0
    val startOffset =
        (first.dayOfWeek.value + 1) % 7

    val totalDays =
        month.lengthOfMonth()

    val totalCells =
        ((startOffset + totalDays + 6) / 7) * 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {

        for (week in 0 until totalCells / 7) {

            Row(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                for (dayIndex in 0 until 7) {

                    val cell =
                        week * 7 + dayIndex

                    val dayNumber =
                        cell - startOffset + 1

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(2.dp)
                    ) {

                        if (
                            dayNumber in 1..totalDays
                        ) {
                            val date =
                                month.atDay(dayNumber)

                            val shift =
                                data.shifts
                                    .firstOrNull {
                                        it.date ==
                                            date.toString()
                                    }

                            val selected: Boolean =
                                painterState
                                    ?.selectedDates
                                    ?.contains(date)
                                    ?: false

                            val details =
                                DayEngine.detailsFor(
                                    data,
                                    date.toString()
                                )

                            V09DayCell(
                                date = date,
                                shift = shift,
                                workplaces =
                                    data.workplaces,
                                selected = selected,
                                hasNote =
                                    details.note
                                        .isNotBlank(),
                                pendingTasks =
                                    details.tasks
                                        .count {
                                            !it.completed
                                        },
                                onClick = {
                                    onDayClick(date)
                                }
                            )
                        } else {
                            Spacer(
                                modifier =
                                    Modifier.height(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V09DayCell(
    date: LocalDate,
    shift: ShiftEntry?,
    workplaces: List<Workplace>,
    selected: Boolean,
    hasNote: Boolean,
    pendingTasks: Int,
    onClick: () -> Unit
) {
    val today =
        date == LocalDate.now()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(
                RoundedCornerShape(12.dp)
            )
            .then(
                if (selected) {
                    Modifier.border(
                        width = 3.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    )
                } else if (today) {
                    Modifier.border(
                        width = 2.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .tertiary,
                        shape =
                            RoundedCornerShape(
                                12.dp
                            )
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {

        if (shift == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                            .copy(alpha = 0.35f)
                    )
            )
        } else {
            V09ShiftBackground(
                shift = shift,
                workplaces = workplaces
            )
        }

        Text(
            text = date.dayOfMonth.toString(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        if (selected) {
            Box(
                modifier = Modifier
                    .align(
                        Alignment.Center
                    )
                    .size(26.dp)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primary,
                        CircleShape
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                        Modifier.size(17.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .align(
                    Alignment.BottomEnd
                )
                .padding(4.dp),
            horizontalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {

            if (hasNote) {
                Text(
                    text = "●",
                    fontSize = 9.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            if (pendingTasks > 0) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme
                                .colorScheme
                                .errorContainer,
                            CircleShape
                        )
                        .padding(
                            horizontal = 5.dp,
                            vertical = 1.dp
                        )
                ) {
                    Text(
                        text =
                            pendingTasks.toString(),
                        fontSize = 9.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun V09ShiftBackground(
    shift: ShiftEntry,
    workplaces: List<Workplace>
) {

    fun colorFor(
        index: Int
    ): Color {
        val id =
            shift.workplaceIds
                .getOrNull(index)

        val place =
            workplaces.firstOrNull {
                it.id == id
            }

        return if (place != null) {
            hexColor(place.colorHex)
        } else {
            Color.LightGray
        }
    }

    when (shift.type) {

        ShiftType.FULL_DAY -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        colorFor(0)
                    )
            )
        }

        ShiftType.LONG_NIGHT -> {
            Column(
                Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            colorFor(0)
                        )
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            colorFor(1)
                        )
                )
            }
        }

        ShiftType.THREE_SHIFTS -> {
            Column(
                Modifier.fillMaxSize()
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                colorFor(index)
                            )
                    )
                }
            }
        }

        ShiftType.OFF -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.LightGray
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V09ShiftChooserSheet(
    data: AppData,
    onDismiss: () -> Unit,
    onChoose:
        (PainterSelection) -> Unit
) {
    var type by remember {
        mutableStateOf(
            ShiftType.FULL_DAY
        )
    }

    var place1 by remember {
        mutableStateOf<String?>(
            data.workplaces
                .firstOrNull()
                ?.id
        )
    }

    var place2 by remember {
        mutableStateOf<String?>(
            data.workplaces
                .firstOrNull()
                ?.id
        )
    }

    var place3 by remember {
        mutableStateOf<String?>(
            data.workplaces
                .firstOrNull()
                ?.id
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 32.dp
                )
        ) {

            Text(
                text = "إضافة شيفت",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "اختر نوع الشيفت ومكان العمل ثم علّم الأيام على التقويم.",
                fontSize = 13.sp
            )

            Spacer(
                Modifier.height(18.dp)
            )

            Text(
                "نوع الشيفت",
                fontWeight = FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            listOf(
                ShiftType.FULL_DAY,
                ShiftType.LONG_NIGHT,
                ShiftType.THREE_SHIFTS,
                ShiftType.OFF
            ).forEach { item ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            type = item
                        },
                    shape =
                        RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected =
                                type == item,
                            onClick = {
                                type = item
                            }
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                v09ShiftName(item),
                            fontWeight =
                                if (type == item)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                        )
                    }
                }
            }

            if (type != ShiftType.OFF) {

                Spacer(
                    Modifier.height(18.dp)
                )

                Text(
                    "مكان العمل",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                V09WorkplacePicker(
                    title =
                        when (type) {
                            ShiftType.FULL_DAY ->
                                "24 ساعة"
                            ShiftType.LONG_NIGHT ->
                                "Long"
                            ShiftType.THREE_SHIFTS ->
                                "Morning"
                            else -> ""
                        },
                    workplaces =
                        data.workplaces,
                    selectedId = place1,
                    onSelect = {
                        place1 = it
                    }
                )

                if (
                    type ==
                    ShiftType.LONG_NIGHT
                ) {
                    V09WorkplacePicker(
                        title = "Night",
                        workplaces =
                            data.workplaces,
                        selectedId =
                            place2,
                        onSelect = {
                            place2 = it
                        }
                    )
                }

                if (
                    type ==
                    ShiftType.THREE_SHIFTS
                ) {
                    V09WorkplacePicker(
                        title = "Evening",
                        workplaces =
                            data.workplaces,
                        selectedId =
                            place2,
                        onSelect = {
                            place2 = it
                        }
                    )

                    V09WorkplacePicker(
                        title = "Night",
                        workplaces =
                            data.workplaces,
                        selectedId =
                            place3,
                        onSelect = {
                            place3 = it
                        }
                    )
                }
            }

            Spacer(
                Modifier.height(22.dp)
            )

            Button(
                onClick = {

                    val ids =
                        when (type) {
                            ShiftType.FULL_DAY ->
                                listOf(place1)

                            ShiftType.LONG_NIGHT ->
                                listOf(
                                    place1,
                                    place2
                                )

                            ShiftType.THREE_SHIFTS ->
                                listOf(
                                    place1,
                                    place2,
                                    place3
                                )

                            ShiftType.OFF ->
                                emptyList()
                        }

                    onChoose(
                        PainterSelection(
                            type = type,
                            workplaceIds = ids
                        )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    type == ShiftType.OFF ||
                    data.workplaces.isNotEmpty()
            ) {
                Text(
                    "ابدأ تحديد الأيام"
                )
            }
        }
    }
}

@Composable
private fun V09WorkplacePicker(
    title: String,
    workplaces: List<Workplace>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Column(
        modifier =
            Modifier.padding(
                vertical = 8.dp
            )
    ) {

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(6.dp)
        )

        workplaces.forEach { place ->

            val selected =
                place.id == selectedId

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .clickable {
                        onSelect(place.id)
                    }
                    .padding(
                        vertical = 8.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                RadioButton(
                    selected = selected,
                    onClick = {
                        onSelect(place.id)
                    }
                )

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            hexColor(
                                place.colorHex
                            ),
                            CircleShape
                        )
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Text(
                    text = place.name,
                    fontWeight =
                        if (selected)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun V09DayCenterDialog(
    date: LocalDate,
    data: AppData,
    onDismiss: () -> Unit,
    onDataChange: (AppData) -> Unit
) {
    val dateString =
        date.toString()

    val shift =
        data.shifts.firstOrNull {
            it.date == dateString
        }

    val details =
        DayEngine.detailsFor(
            data,
            dateString
        )

    var note by remember(
        dateString,
        details.note
    ) {
        mutableStateOf(
            details.note
        )
    }

    var newTask by remember(
        dateString
    ) {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    var updated =
                        DayEngine.saveNote(
                            data,
                            dateString,
                            note
                        )

                    onDataChange(updated)
                    onDismiss()
                }
            ) {
                Text("حفظ وإغلاق")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إغلاق")
            }
        },
        title = {
            Column {
                Text(
                    text =
                        "${date.dayOfMonth} " +
                        arabicMonth(
                            date.monthValue
                        ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        v09ArabicDay(
                            date
                        ),
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Text(
                    "الشيفت",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                if (shift == null) {
                    Text(
                        "لا يوجد شيفت مسجل لهذا اليوم.",
                        fontSize = 13.sp
                    )
                } else {

                    val names =
                        shift.workplaceIds
                            .mapNotNull { id ->
                                data.workplaces
                                    .firstOrNull {
                                        it.id == id
                                    }
                                    ?.name
                            }

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {
                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                )
                        ) {
                            Text(
                                v09ShiftName(
                                    shift.type
                                ),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            if (
                                names.isNotEmpty()
                            ) {
                                Text(
                                    names.joinToString(
                                        " / "
                                    ),
                                    fontSize = 13.sp
                                )
                            }

                            Text(
                                text =
                                    "القيمة: ${
                                        money(
                                            FinanceEngine
                                                .shiftAmount(
                                                    shift,
                                                    data.workplaces
                                                )
                                        )
                                    }",
                                fontSize = 13.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            val updated =
                                data.copy(
                                    shifts =
                                        data.shifts
                                            .filterNot {
                                                it.date ==
                                                    dateString
                                            }
                                )

                            onDataChange(
                                updated
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription =
                                null
                        )

                        Spacer(
                            Modifier.width(
                                4.dp
                            )
                        )

                        Text(
                            "حذف شيفت اليوم"
                        )
                    }
                }

                HorizontalDivider()

                Spacer(
                    Modifier.height(14.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Notes,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text(
                        "ملاحظات اليوم",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "اكتب أي ملاحظة تخص اليوم..."
                        )
                    },
                    minLines = 3
                )

                Spacer(
                    Modifier.height(18.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TaskAlt,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text(
                        "طلبات ومهام اليوم",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    OutlinedTextField(
                        value = newTask,
                        onValueChange = {
                            newTask = it
                        },
                        modifier =
                            Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(
                                "مهمة جديدة"
                            )
                        }
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    IconButton(
                        onClick = {
                            if (
                                newTask
                                    .isNotBlank()
                            ) {
                                val withNote =
                                    DayEngine
                                        .saveNote(
                                            data,
                                            dateString,
                                            note
                                        )

                                val updated =
                                    DayEngine
                                        .addTask(
                                            withNote,
                                            dateString,
                                            newTask
                                        )

                                onDataChange(
                                    updated
                                )

                                newTask = ""
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription =
                                "إضافة مهمة"
                        )
                    }
                }

                val currentDetails =
                    DayEngine.detailsFor(
                        data,
                        dateString
                    )

                if (
                    currentDetails
                        .tasks
                        .isEmpty()
                ) {
                    Text(
                        text =
                            "لا توجد مهام لهذا اليوم.",
                        fontSize = 13.sp,
                        modifier =
                            Modifier.padding(
                                vertical = 10.dp
                            )
                    )
                } else {
                    currentDetails
                        .tasks
                        .forEach { task ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 4.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Checkbox(
                                    checked =
                                        task.completed,
                                    onCheckedChange = {
                                        val updated =
                                            DayEngine
                                                .toggleTask(
                                                    data,
                                                    dateString,
                                                    task.id
                                                )

                                        onDataChange(
                                            updated
                                        )
                                    }
                                )

                                Text(
                                    text =
                                        task.title,
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),
                                    fontSize =
                                        14.sp
                                )

                                IconButton(
                                    onClick = {
                                        val updated =
                                            DayEngine
                                                .deleteTask(
                                                    data,
                                                    dateString,
                                                    task.id
                                                )

                                        onDataChange(
                                            updated
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription =
                                            "حذف"
                                    )
                                }
                            }
                        }
                }
            }
        }
    )
}

private fun v09ShiftName(
    type: ShiftType
): String {
    return when (type) {
        ShiftType.FULL_DAY ->
            "24 ساعة"

        ShiftType.LONG_NIGHT ->
            "Long / Night"

        ShiftType.THREE_SHIFTS ->
            "Morning / Evening / Night"

        ShiftType.OFF ->
            "إجازة"
    }
}

private fun v09ArabicDay(
    date: LocalDate
): String {
    return when (
        date.dayOfWeek
    ) {
        java.time.DayOfWeek.SATURDAY ->
            "السبت"

        java.time.DayOfWeek.SUNDAY ->
            "الأحد"

        java.time.DayOfWeek.MONDAY ->
            "الاثنين"

        java.time.DayOfWeek.TUESDAY ->
            "الثلاثاء"

        java.time.DayOfWeek.WEDNESDAY ->
            "الأربعاء"

        java.time.DayOfWeek.THURSDAY ->
            "الخميس"

        java.time.DayOfWeek.FRIDAY ->
            "الجمعة"
    }
}
