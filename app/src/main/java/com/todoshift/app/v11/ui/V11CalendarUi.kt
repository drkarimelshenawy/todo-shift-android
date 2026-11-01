package com.todoshift.app.v11.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

private fun v11Color(hex: String): Color =
    runCatching {
        Color(AndroidColor.parseColor(hex))
    }.getOrDefault(Color(0xFF90CAF9))

private fun monthName(month: Int): String =
    listOf(
        "يناير", "فبراير", "مارس", "أبريل",
        "مايو", "يونيو", "يوليو", "أغسطس",
        "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    ).getOrElse(month - 1) { "" }

@Composable
fun V11CalendarScreen(
    month: YearMonth,
    initialDate: LocalDate = LocalDate.now(),
    db: V11Database,
    workplaces: List<Workplace>,
    uiSettings: V11UiSettings,
    onMonthChange: (YearMonth) -> Unit,
    onDatabaseChange: (V11Database) -> Unit,
    onUiSettingsChange: (V11UiSettings) -> Unit
) {
    var selectedDate by remember(initialDate) {
        mutableStateOf(initialDate)
    }
    var quickAddOpen by remember { mutableStateOf(false) }
    var calendarManagerOpen by remember { mutableStateOf(false) }

    val visibleCalendarIds = remember(db.calendars) {
        db.calendars
            .filter { it.visible }
            .map { it.id }
            .toSet()
    }

    Scaffold(
        containerColor = Color(0xFFF7F8FA),
        floatingActionButtonPosition =
            if (uiSettings.fabSide == FabSide.LEFT)
                FabPosition.Start
            else
                FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { quickAddOpen = true },
                shape = CircleShape,
                containerColor = Color(0xFF213555),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "إضافة"
                )
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            V11CompactHeader(
                onCalendars = {
                    calendarManagerOpen = true
                }
            )

            Spacer(Modifier.height(10.dp))

            V11TodayDashboard(
                db = db,
                workplaces = workplaces,
                visibleCalendarIds = visibleCalendarIds
            )

            Spacer(Modifier.height(10.dp))

            V11MonthSelector(
                month = month,
                onPrevious = {
                    onMonthChange(month.minusMonths(1))
                },
                onNext = {
                    onMonthChange(month.plusMonths(1))
                },
                onToday = {
                    selectedDate = LocalDate.now()
                    onMonthChange(
                        YearMonth.from(LocalDate.now())
                    )
                }
            )

            Spacer(Modifier.height(7.dp))

            V11CalendarGrid(
                month = month,
                db = db,
                workplaces = workplaces,
                uiSettings = uiSettings,
                visibleCalendarIds = visibleCalendarIds,
                selectedDate = selectedDate,
                onSelectedDate = {
                    selectedDate = it
                },
                onPrevious = {
                    onMonthChange(month.minusMonths(1))
                },
                onNext = {
                    onMonthChange(month.plusMonths(1))
                }
            )

            Spacer(Modifier.height(10.dp))

            V11SelectedDayPanel(
                date = selectedDate,
                db = db,
                workplaces = workplaces,
                visibleCalendarIds = visibleCalendarIds,
                onAdd = {
                    quickAddOpen = true
                },
                onDeleteSegment = { segmentId ->
                    onDatabaseChange(
                        V11DayController.deleteSegment(
                            db,
                            segmentId
                        )
                    )
                }
            )
        }
    }

    if (quickAddOpen) {
        V11QuickAddSheet(
            date = selectedDate,
            db = db,
            workplaces = workplaces,
            onDismiss = {
                quickAddOpen = false
            },
            onSave = {
                onDatabaseChange(it)
                quickAddOpen = false
            }
        )
    }

    if (calendarManagerOpen) {
        V11CalendarManagerSheet(
            db = db,
            onDismiss = {
                calendarManagerOpen = false
            },
            onDatabaseChange = onDatabaseChange
        )
    }
}

@Composable
private fun V11CompactHeader(
    onCalendars: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF162A46)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Column {
                Text(
                    "ToDo Shift",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF162A46)
                )
                Text(
                    "تقويم الشيفتات",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }

        FilledTonalButton(
            onClick = onCalendars,
            contentPadding = PaddingValues(
                horizontal = 12.dp,
                vertical = 6.dp
            )
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text("التقاويم", fontSize = 12.sp)
        }
    }
}

@Composable
private fun V11TodayDashboard(
    db: V11Database,
    workplaces: List<Workplace>,
    visibleCalendarIds: Set<String>
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    fun labels(date: LocalDate): String {
        val segments = db.days
            .filter {
                it.date == date.toString() &&
                    it.calendarId in visibleCalendarIds
            }
            .flatMap { it.segments }

        if (segments.isEmpty())
            return "لا يوجد"

        return segments.joinToString(" • ") { segment ->
            val place = workplaces.firstOrNull {
                it.id == segment.workplaceId
            }?.name

            if (place.isNullOrBlank())
                V11Labels.shift(segment.kind)
            else
                "${V11Labels.shift(segment.kind)} - $place"
        }
    }

    val next = db.days
        .filter {
            runCatching {
                LocalDate.parse(it.date)
            }.getOrNull()?.isAfter(today) == true &&
                it.calendarId in visibleCalendarIds &&
                it.segments.any { s ->
                    s.kind != ShiftKind.OFF
                }
        }
        .minByOrNull { it.date }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "اليوم",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF4F709C)
            )
            Text(
                labels(today),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "غدا",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        labels(tomorrow),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "القادم",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        next?.let {
                            "${it.date} • ${
                                it.segments
                                    .filter { s ->
                                        s.kind != ShiftKind.OFF
                                    }
                                    .joinToString("/") { s ->
                                        V11Labels.shift(s.kind)
                                    }
                            }"
                        } ?: "لا يوجد",
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun V11MonthSelector(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {
        FilledTonalButton(
            onClick = onPrevious,
            modifier = Modifier.size(
                width = 58.dp,
                height = 46.dp
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                "‹",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${monthName(month.monthValue)} ${month.year}",
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
            )

            TextButton(
                onClick = onToday,
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    "اليوم",
                    fontSize = 11.sp
                )
            }
        }

        FilledTonalButton(
            onClick = onNext,
            modifier = Modifier.size(
                width = 58.dp,
                height = 46.dp
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                "›",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun V11CalendarGrid(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    uiSettings: V11UiSettings,
    visibleCalendarIds: Set<String>,
    selectedDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val first = month.atDay(1)
    val firstIndex =
        (first.dayOfWeek.value + 2) % 7
    val total = firstIndex + month.lengthOfMonth()
    val rows = (total + 6) / 7

    var drag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        drag = 0f
                    },
                    onHorizontalDrag = { _, amount ->
                        drag += amount
                    },
                    onDragEnd = {
                        when {
                            drag > 90f -> onPrevious()
                            drag < -90f -> onNext()
                        }
                        drag = 0f
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "السبت",
                "الأحد",
                "الاثنين",
                "الثلاثاء",
                "الأربعاء",
                "الخميس",
                "الجمعة"
            ).forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4B5563)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(7) { col ->
                    val cell = row * 7 + col
                    val day =
                        cell - firstIndex + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                    ) {
                        if (
                            day in
                            1..month.lengthOfMonth()
                        ) {
                            val date =
                                month.atDay(day)

                            val dayEntries =
                                db.days.filter {
                                    it.date ==
                                        date.toString() &&
                                        it.calendarId in
                                        visibleCalendarIds
                                }

                            V11DayCell(
                                date = date,
                                entries = dayEntries,
                                workplaces = workplaces,
                                uiSettings = uiSettings,
                                selected =
                                    date == selectedDate,
                                onClick = {
                                    onSelectedDate(date)
                                }
                            )
                        } else {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun V11DayCell(
    date: LocalDate,
    entries: List<DayEntryV11>,
    workplaces: List<Workplace>,
    uiSettings: V11UiSettings,
    selected: Boolean,
    onClick: () -> Unit
) {
    val today = date == LocalDate.now()

    val segments = entries.flatMap {
        it.segments
    }

    val shape = RoundedCornerShape(10.dp)

    var modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .clip(shape)
        .background(
            when {
                today &&
                    uiSettings.todayHighlightStyle in
                    listOf(
                        TodayHighlightStyle.TINT,
                        TodayHighlightStyle.BORDER_TINT
                    ) ->
                    v11Color(
                        uiSettings.todayHighlightColor
                    ).copy(alpha = 0.10f)

                else -> Color.White
            }
        )

    if (
        today &&
        uiSettings.todayHighlightStyle in
        listOf(
            TodayHighlightStyle.BORDER,
            TodayHighlightStyle.BORDER_TINT
        )
    ) {
        modifier = modifier.border(
            2.dp,
            v11Color(
                uiSettings.todayHighlightColor
            ),
            shape
        )
    } else if (selected) {
        modifier = modifier.border(
            2.dp,
            Color(0xFF4F709C),
            shape
        )
    } else {
        modifier = modifier.border(
            1.dp,
            Color(0xFFE5E7EB),
            shape
        )
    }

    Box(
        modifier = modifier.clickable(
            onClick = onClick
        )
    ) {
        if (segments.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                segments
                    .take(4)
                    .forEach { segment ->
                        val place =
                            workplaces.firstOrNull {
                                it.id ==
                                    segment.workplaceId
                            }

                        val bg =
                            if (
                                segment.kind ==
                                ShiftKind.OFF
                            )
                                Color(0xFFECEFF1)
                            else
                                place?.let {
                                    v11Color(it.colorHex)
                                } ?: Color(0xFFE5E7EB)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(bg),
                            contentAlignment =
                                Alignment.Center
                        ) {
                            if (
                                uiSettings
                                    .showWorkplaceNames
                            ) {
                                Text(
                                    if (
                                        segment.kind ==
                                        ShiftKind.OFF
                                    )
                                        "إجازة"
                                    else
                                        place?.name ?: "",
                                    fontSize = 7.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
            }
        }

        if (
            today &&
            uiSettings.todayHighlightStyle ==
            TodayHighlightStyle.RING
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(3.dp)
                    .size(24.dp)
                    .border(
                        2.dp,
                        v11Color(
                            uiSettings.todayHighlightColor
                        ),
                        CircleShape
                    )
            )
        }

        Text(
            date.dayOfMonth.toString(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
                .clip(CircleShape)
                .background(
                    Color.White.copy(
                        alpha = 0.90f
                    )
                )
                .padding(
                    horizontal = 5.dp,
                    vertical = 2.dp
                ),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF111827)
        )

        if (segments.size > 4) {
            Text(
                "+${segments.size - 4}",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun V11SelectedDayPanel(
    date: LocalDate,
    db: V11Database,
    workplaces: List<Workplace>,
    visibleCalendarIds: Set<String>,
    onAdd: () -> Unit,
    onDeleteSegment: (String) -> Unit
) {
    val entries = db.days.filter {
        it.date == date.toString() &&
            it.calendarId in visibleCalendarIds
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 210.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onAdd
                ) {
                    Icon(
                        Icons.Default.Add,
                        null,
                        modifier =
                            Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("إضافة شيفت")
                }

                Text(
                    "${date.dayOfMonth} ${
                        monthName(date.monthValue)
                    }",
                    fontWeight = FontWeight.Black
                )
            }

            if (entries.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "لا توجد شيفتات في هذا اليوم",
                    color = Color.Gray,
                    modifier =
                        Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }

            entries.forEach { entry ->
                val calendar =
                    db.calendars.firstOrNull {
                        it.id == entry.calendarId
                    }

                entry.segments.forEach { segment ->
                    Spacer(Modifier.height(7.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                            .background(
                                Color(0xFFF6F7F9)
                            )
                            .padding(8.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                onDeleteSegment(
                                    segment.id
                                )
                            },
                            modifier =
                                Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                "حذف",
                                tint =
                                    Color(0xFFB3261E)
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
                                V11Labels.shift(
                                    segment.kind
                                ),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            val place =
                                workplaces
                                    .firstOrNull {
                                        it.id ==
                                            segment
                                                .workplaceId
                                    }

                            Text(
                                listOfNotNull(
                                    place?.name,
                                    calendar?.name
                                ).joinToString(" • "),
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            Modifier
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(
                                    workplaces
                                        .firstOrNull {
                                            it.id ==
                                                segment
                                                    .workplaceId
                                        }
                                        ?.let {
                                            v11Color(
                                                it.colorHex
                                            )
                                        }
                                        ?: Color.LightGray
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V11QuickAddSheet(
    date: LocalDate,
    db: V11Database,
    workplaces: List<Workplace>,
    onDismiss: () -> Unit,
    onSave: (V11Database) -> Unit
) {
    val calendars =
        db.calendars.sortedBy { it.order }

    var calendarId by remember {
        mutableStateOf(
            calendars.firstOrNull {
                it.visible
            }?.id
                ?: calendars.firstOrNull()?.id
                ?: ""
        )
    }

    var workplaceId by remember {
        mutableStateOf(
            workplaces.firstOrNull()?.id
        )
    }

    var kind by remember {
        mutableStateOf(
            ShiftKind.FULL_DAY
        )
    }

    var exceptional by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
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
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 28.dp
                )
        ) {
            Text(
                "إضافة سريعة",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )

            Text(
                "${date.dayOfMonth} ${
                    monthName(date.monthValue)
                } ${date.year}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                color = Color.Gray
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "التقويم",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            calendars.forEach { calendar ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            calendarId =
                                calendar.id
                        }
                        .padding(vertical = 5.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected =
                            calendarId ==
                                calendar.id,
                        onClick = {
                            calendarId =
                                calendar.id
                        }
                    )

                    Spacer(Modifier.weight(1f))

                    Text(calendar.name)

                    Spacer(Modifier.width(7.dp))

                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(
                                v11Color(
                                    calendar.colorHex
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "نوع الشيفت",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(6.dp))

            ShiftKind.entries.forEach {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            kind = it
                        }
                        .padding(vertical = 3.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = kind == it,
                        onClick = {
                            kind = it
                        }
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        V11Labels.shift(it)
                    )
                }
            }

            if (kind != ShiftKind.OFF) {
                Spacer(Modifier.height(12.dp))

                Text(
                    "مكان العمل",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                workplaces.forEach { place ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                workplaceId =
                                    place.id
                            }
                            .padding(
                                vertical = 4.dp
                            ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected =
                                workplaceId ==
                                    place.id,
                            onClick = {
                                workplaceId =
                                    place.id
                            }
                        )

                        Spacer(
                            Modifier.weight(1f)
                        )

                        Text(place.name)

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Box(
                            Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                                .background(
                                    v11Color(
                                        place.colorHex
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = exceptional,
                    onValueChange = {
                        exceptional = it
                            .filter { c ->
                                c.isDigit() ||
                                    c == '.'
                            }
                    },
                    label = {
                        Text(
                            "سعر استثنائي (اختياري)"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                },
                label = {
                    Text("ملاحظة")
                },
                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (calendarId.isBlank())
                        return@Button

                    val amount =
                        exceptional
                            .toDoubleOrNull()

                    onSave(
                        V11DayController
                            .addSegment(
                                db = db,
                                date =
                                    date.toString(),
                                calendarId =
                                    calendarId,
                                kind = kind,
                                workplaceId =
                                    workplaceId,
                                exceptionalAmount =
                                    amount,
                                note = note
                            )
                    )
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("حفظ الشيفت")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "يمكن إضافة شيفت آخر لنفس اليوم بعد الحفظ.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V11CalendarManagerSheet(
    db: V11Database,
    onDismiss: () -> Unit,
    onDatabaseChange: (V11Database) -> Unit
) {
    var newName by remember {
        mutableStateOf("")
    }

    var selectedColor by remember {
        mutableStateOf("#4F709C")
    }

    val colors = listOf(
        "#213555",
        "#4F709C",
        "#9DD8AE",
        "#FFE28A",
        "#D7ACE8",
        "#91C1EE",
        "#FFAB91",
        "#80CBC4"
    )

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
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 30.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        Icons.Default.Close,
                        "إغلاق"
                    )
                }

                Text(
                    "إدارة التقاويم",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(Modifier.height(10.dp))

            db.calendars
                .sortedBy { it.order }
                .forEach { calendar ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 5.dp
                            ),
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

                        Text(
                            calendar.name,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    v11Color(
                                        calendar.colorHex
                                    )
                                )
                        )
                    }
                }

            HorizontalDivider()

            Spacer(Modifier.height(14.dp))

            Text(
                "تقويم جديد",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newName,
                onValueChange = {
                    newName = it
                },
                label = {
                    Text("اسم التقويم")
                },
                modifier =
                    Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(7.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(
                                v11Color(color)
                            )
                            .then(
                                if (
                                    selectedColor ==
                                    color
                                )
                                    Modifier.border(
                                        3.dp,
                                        Color.Black,
                                        CircleShape
                                    )
                                else
                                    Modifier
                            )
                            .clickable {
                                selectedColor =
                                    color
                            }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    if (newName.isBlank())
                        return@Button

                    onDatabaseChange(
                        V11CalendarController
                            .addCalendar(
                                db,
                                newName,
                                selectedColor
                            )
                    )

                    newName = ""
                },
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    null
                )
                Spacer(Modifier.width(5.dp))
                Text("إنشاء التقويم")
            }
        }
    }
}
