package com.todoshift.app.v11.ui

import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.R
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
    activeCalendarId: String?,
    onActiveCalendarChange: (String) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onDatabaseChange: (V11Database) -> Unit,
    onUiSettingsChange: (V11UiSettings) -> Unit
) {
    var selectedDate by remember(initialDate) {
        mutableStateOf(initialDate)
    }
    var paletteOpen by remember { mutableStateOf(false) }
    var selectedWorkplace by remember { mutableStateOf<Workplace?>(null) }
    var selectedPaintKind by remember { mutableStateOf<ShiftKind?>(null) }
    var paintMode by remember { mutableStateOf(false) }
    var paintDates by remember { mutableStateOf(setOf<LocalDate>()) }
    var openDay by remember { mutableStateOf<LocalDate?>(null) }
    var showCalendarManager by remember { mutableStateOf(false) }

    BackHandler(enabled = openDay != null) {
        openDay = null
    }

    var pendingConflict by remember {
        mutableStateOf<V11PendingPaintConflict?>(null)
    }

    var pendingPaintQueue by remember {
        mutableStateOf<List<LocalDate>>(emptyList())
    }

    var pendingPaintDb by remember {
        mutableStateOf<V11Database?>(null)
    }

    val visibleCalendarIds = remember(db.calendars) {
        db.calendars
            .filter { it.visible }
            .map { it.id }
            .toSet()
    }

    val activeCalendar = remember(db.calendars, activeCalendarId) {
        db.calendars.firstOrNull { it.id == activeCalendarId }
            ?: db.calendars.sortedBy { it.order }.firstOrNull()
    }

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,
        contentWindowInsets =
            WindowInsets(0, 0, 0, 0),
        floatingActionButtonPosition =
            if (uiSettings.fabSide == FabSide.LEFT)
                FabPosition.Start
            else
                FabPosition.End,
        floatingActionButton = {
            Column(
                horizontalAlignment =
                    if (uiSettings.fabSide == FabSide.LEFT)
                        Alignment.Start
                    else
                        Alignment.End
            ) {
                if (paletteOpen && !paintMode) {
                    V11PaintPalette(
                        workplaces = workplaces,
                        selectedWorkplace = selectedWorkplace,
                        onWorkplace = {
                            selectedWorkplace = it
                        },
                        onShiftKind = { kind ->
                            selectedPaintKind = kind
                            paintDates = emptySet()
                            paintMode = true
                            paletteOpen = false
                        },
                        onClose = {
                            paletteOpen = false
                            selectedWorkplace = null
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                }

                SmallFloatingActionButton(
                    onClick = {
                        if (paintMode) {
                            val kind = selectedPaintKind
                            val calendarId =
                                db.calendars
                                    .firstOrNull {
                                        it.id ==
                                            activeCalendarId
                                    }
                                    ?.id
                                    ?: db.calendars
                                        .sortedBy { it.order }
                                        .firstOrNull()
                                        ?.id

                            if (
                                kind != null &&
                                calendarId != null &&
                                paintDates.isNotEmpty()
                            ) {
                                pendingPaintDb = db
                                pendingPaintQueue =
                                    paintDates.sorted()

                                fun continueQueue(
                                    startDb: V11Database
                                ) {
                                    var updated =
                                        startDb

                                    val queue =
                                        pendingPaintQueue
                                            .toMutableList()

                                    while (
                                        queue.isNotEmpty()
                                    ) {
                                        val date =
                                            queue.removeAt(0)

                                        val pending =
                                            V11PaintConflictResolver
                                                .analyse(
                                                    db = updated,
                                                    date =
                                                        date.toString(),
                                                    calendarId =
                                                        calendarId,
                                                    kind = kind,
                                                    workplaceId =
                                                        if (
                                                            kind ==
                                                            ShiftKind.OFF
                                                        )
                                                            null
                                                        else
                                                            selectedWorkplace
                                                                ?.id
                                                )

                                        if (
                                            pending.analysis
                                                .canAddDirectly
                                        ) {
                                            updated =
                                                V11PaintConflictResolver
                                                    .applyDirect(
                                                        updated,
                                                        pending
                                                    )

                                            pendingPaintDb =
                                                updated

                                            continue
                                        }

                                        pendingPaintDb =
                                            updated

                                        pendingPaintQueue =
                                            queue

                                        pendingConflict =
                                            pending

                                        return
                                    }

                                    pendingPaintDb = null
                                    pendingPaintQueue =
                                        emptyList()

                                    onDatabaseChange(
                                        updated
                                    )
                                }

                                continueQueue(db)
                            }

                            if (pendingConflict == null) {
                                paintMode = false
                                paintDates = emptySet()
                                selectedPaintKind = null
                                selectedWorkplace = null
                                paletteOpen = false
                            }
                        } else {
                            paletteOpen = !paletteOpen

                            if (!paletteOpen) {
                                selectedWorkplace = null
                            }
                        }
                    },
                    shape = CircleShape,
                    containerColor =
                        if (paintMode)
                            Color(0xFF2E7D32)
                        else
                            MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = 0.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        if (paintMode)
                            Icons.Default.Check
                        else
                            Icons.Default.Add,
                        contentDescription =
                            if (paintMode)
                                "حفظ"
                            else
                                "إضافة",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    activeCalendar?.name
                        ?: "التقويم",
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.weight(1f)
                )

                TextButton(
                    onClick = {
                        showCalendarManager = true
                    },
                    contentPadding =
                        PaddingValues(
                            horizontal = 8.dp,
                            vertical = 0.dp
                        )
                ) {
                    Text("إدارة التقاويم")
                }
            }

            V11AnimatedCalendarArea(
                month = month,
                db = db,
                workplaces = workplaces,
                uiSettings = uiSettings,
                visibleCalendarIds = visibleCalendarIds,
                selectedDate = selectedDate,
                paintMode = paintMode,
                paintDates = paintDates,
                onSelectedDate = { date ->
                    if (paintMode) {
                        paintDates =
                            if (date in paintDates)
                                paintDates - date
                            else
                                paintDates + date
                    } else {
                        if (selectedDate == date) {
                            openDay = date
                        } else {
                            selectedDate = date
                        }
                    }
                },
                onMonthChange = onMonthChange,
                onToday = {
                    val today = LocalDate.now()
                    selectedDate = today
                    onMonthChange(YearMonth.from(today))
                },
                modifier = Modifier.weight(1f)
            )

            if (paintMode) {
                V11PaintStatusBar(
                    workplace = selectedWorkplace,
                    kind = selectedPaintKind,
                    count = paintDates.size,
                    onCancel = {
                        paintMode = false
                        paintDates = emptySet()
                        selectedPaintKind = null
                        selectedWorkplace = null
                        paletteOpen = false
                    }
                )
            }
        }
    }

    openDay?.let { day ->
        V11DayDetailsScreen(
            initialDate = day,
            db = db,
            workplaces = workplaces,
            activeCalendarId =
                activeCalendarId,
            onBack = {
                openDay = null
            },
            onDatabaseChange = onDatabaseChange
        )
    }

    pendingConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = {
                pendingConflict = null
                pendingPaintDb = null
                pendingPaintQueue = emptyList()
                paintMode = false
                paintDates = emptySet()
                selectedPaintKind = null
                selectedWorkplace = null
                paletteOpen = false
            },
            title = {
                Text("تعارض في الشيفت")
            },
            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    Text(
                        "اليوم ${conflict.date} يحتوي على شيفت يتداخل زمنياً مع الشيفت الجديد."
                    )

                    Text(
                        "اختر طريقة حل التعارض. لن يتم إنشاء شيفتين متداخلين."
                    )

                    conflict.analysis.options
                        .filter {
                            it.action !=
                                ShiftConflictActionV11
                                    .REJECT
                        }
                        .forEachIndexed {
                            index,
                            option ->

                            OutlinedButton(
                                onClick = {
                                    val base =
                                        pendingPaintDb
                                            ?: db

                                    var updated =
                                        V11PaintConflictResolver
                                            .applyOption(
                                                db = base,
                                                pending =
                                                    conflict,
                                                option =
                                                    option
                                            )

                                    pendingConflict =
                                        null

                                    val remaining =
                                        pendingPaintQueue

                                    val kind =
                                        selectedPaintKind

                                    val calendar =
                                        conflict
                                            .calendarId

                                    if (
                                        kind == null ||
                                        remaining
                                            .isEmpty()
                                    ) {
                                        pendingPaintDb =
                                            null
                                        pendingPaintQueue =
                                            emptyList()

                                        onDatabaseChange(
                                            updated
                                        )
                                        paintMode = false
                                        paintDates = emptySet()
                                        selectedPaintKind = null
                                        selectedWorkplace = null
                                        paletteOpen = false
                                    } else {
                                        val queue =
                                            remaining
                                                .toMutableList()

                                        while (
                                            queue
                                                .isNotEmpty()
                                        ) {
                                            val date =
                                                queue
                                                    .removeAt(
                                                        0
                                                    )

                                            val next =
                                                V11PaintConflictResolver
                                                    .analyse(
                                                        db =
                                                            updated,
                                                        date =
                                                            date
                                                                .toString(),
                                                        calendarId =
                                                            calendar,
                                                        kind =
                                                            kind,
                                                        workplaceId =
                                                            if (
                                                                kind ==
                                                                ShiftKind
                                                                    .OFF
                                                            )
                                                                null
                                                            else
                                                                selectedWorkplace
                                                                    ?.id
                                                    )

                                            if (
                                                next.analysis
                                                    .canAddDirectly
                                            ) {
                                                updated =
                                                    V11PaintConflictResolver
                                                        .applyDirect(
                                                            updated,
                                                            next
                                                        )

                                                continue
                                            }

                                            pendingPaintDb =
                                                updated

                                            pendingPaintQueue =
                                                queue

                                            pendingConflict =
                                                next

                                            return@OutlinedButton
                                        }

                                        pendingPaintDb =
                                            null

                                        pendingPaintQueue =
                                            emptyList()

                                        onDatabaseChange(
                                            updated
                                        )
                                        paintMode = false
                                        paintDates = emptySet()
                                        selectedPaintKind = null
                                        selectedWorkplace = null
                                        paletteOpen = false
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            ) {
                                Text(
                                    when (
                                        option.action
                                    ) {
                                        ShiftConflictActionV11
                                            .REPLACE ->
                                            "استبدال الشيفت القديم"

                                        ShiftConflictActionV11
                                            .SPLIT_EXISTING ->
                                            "تقسيم الشيفت القديم وحفظ الوقت المتبقي"

                                        ShiftConflictActionV11
                                            .TRIM_EXISTING_BEFORE ->
                                            "تقصير الشيفت القديم قبل الجديد"

                                        ShiftConflictActionV11
                                            .TRIM_EXISTING_AFTER ->
                                            "تقصير الشيفت القديم بعد الجديد"

                                        ShiftConflictActionV11
                                            .ADD ->
                                            "إضافة"

                                        else ->
                                            "الحل ${index + 1}"
                                    }
                                )
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingConflict = null
                        pendingPaintDb = null
                        pendingPaintQueue = emptyList()
                        paintMode = false
                        paintDates = emptySet()
                        selectedPaintKind = null
                        selectedWorkplace = null
                        paletteOpen = false
                    }
                ) {
                    Text(
                        "إلغاء بدون تغيير"
                    )
                }
            }
        )
    }

    if (showCalendarManager) {
        V11CalendarManagerSheet(
            db = db,
            activeCalendarId =
                activeCalendarId,
            onActiveCalendarChange =
                onActiveCalendarChange,
            onDismiss = {
                showCalendarManager = false
            },
            onDatabaseChange =
                onDatabaseChange
        )
    }
}

@Composable
private fun V11CompactHeader(
    activeCalendarName: String,
    onManageCalendars: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.todo_shift_icon_source),
            contentDescription = "ToDo Shift",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "ToDo Shift",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF162A46),
                maxLines = 1
            )

            Text(
                activeCalendarName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4F709C),
                maxLines = 1
            )
        }

        TextButton(
            onClick = onManageCalendars,
            contentPadding = PaddingValues(
                horizontal = 8.dp,
                vertical = 0.dp
            )
        ) {
            Text(
                "إدارة التقاويم",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalIconButton(
            onClick = onPrevious,
            modifier = Modifier.size(38.dp)
        ) {
            Text(
                "‹",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                "${monthName(month.monthValue)} ${month.year}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF16181D)
            )

            TextButton(
                onClick = onToday,
                modifier = Modifier.height(25.dp),
                contentPadding = PaddingValues(
                    horizontal = 6.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    "انتقل لتاريخ اليوم",
                    fontSize = 9.sp,
                    color = Color(0xFF4F709C)
                )
            }
        }

        FilledTonalIconButton(
            onClick = onNext,
            modifier = Modifier.size(38.dp)
        ) {
            Text(
                "›",
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun V11AnimatedCalendarArea(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    uiSettings: V11UiSettings,
    visibleCalendarIds: Set<String>,
    selectedDate: LocalDate,
    paintMode: Boolean,
    paintDates: Set<LocalDate>,
    onSelectedDate: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previousMonth by remember { mutableStateOf(month) }

    val direction = remember(month) {
        when {
            month > previousMonth -> 1
            month < previousMonth -> -1
            else -> 0
        }
    }

    LaunchedEffect(month) {
        previousMonth = month
    }

    AnimatedContent(
        modifier = modifier.fillMaxSize(),
        targetState = month,
        transitionSpec = {
            val forward = targetState > initialState

            ContentTransform(
                targetContentEnter =
                    slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { width ->
                            if (forward) width else -width
                        }
                    ) + fadeIn(
                        animationSpec = tween(220)
                    ),
                initialContentExit =
                    slideOutHorizontally(
                        animationSpec = tween(280),
                        targetOffsetX = { width ->
                            if (forward) -width else width
                        }
                    ) + fadeOut(
                        animationSpec = tween(180)
                    )
            )
        },
        label = "monthTransition"
    ) { shownMonth ->

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            V11MonthSelector(
                month = shownMonth,
                onPrevious = {
                    onMonthChange(
                        shownMonth.minusMonths(1)
                    )
                },
                onNext = {
                    onMonthChange(
                        shownMonth.plusMonths(1)
                    )
                },
                onToday = onToday
            )

            Spacer(Modifier.height(2.dp))

            V11CalendarGrid(
                month = shownMonth,
                db = db,
                workplaces = workplaces,
                uiSettings = uiSettings,
                visibleCalendarIds = visibleCalendarIds,
                selectedDate = selectedDate,
                paintMode = paintMode,
                paintDates = paintDates,
                onSelectedDate = onSelectedDate,
                onPrevious = {
                    onMonthChange(
                        shownMonth.minusMonths(1)
                    )
                },
                onNext = {
                    onMonthChange(
                        shownMonth.plusMonths(1)
                    )
                },
                modifier =
                    Modifier.weight(1f)
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
    paintMode: Boolean,
    paintDates: Set<LocalDate>,
    onSelectedDate: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val first = month.atDay(1)

    // Saturday = first column.
    val firstIndex =
        (first.dayOfWeek.value + 1) % 7

    val naturalRows =
        (
            firstIndex +
                month.lengthOfMonth() +
                6
        ) / 7

    val rows =
        maxOf(
            5,
            naturalRows
        )

    val gridStart =
        first.minusDays(
            firstIndex.toLong()
        )

    var drag by remember {
        mutableFloatStateOf(0f)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        drag = 0f
                    },
                    onHorizontalDrag = {
                            _,
                            amount ->
                        drag += amount
                    },
                    onDragEnd = {
                        when {
                            drag > 90f ->
                                onPrevious()

                            drag < -90f ->
                                onNext()
                        }

                        drag = 0f
                    }
                )
            }
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
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
                    modifier =
                        Modifier.weight(1f),
                    textAlign =
                        TextAlign.Center,
                    fontSize = 9.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            Modifier.height(4.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            repeat(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    repeat(7) { col ->
                        val cell =
                            row * 7 + col

                        val date =
                            gridStart.plusDays(
                                cell.toLong()
                            )

                        val isCurrentMonth =
                            YearMonth.from(date) ==
                                month

                        val dayEntries =
                            db.days.filter {
                                it.date ==
                                    date.toString() &&
                                    it.calendarId in
                                    visibleCalendarIds
                            }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.5.dp)
                        ) {
                            V11DayCell(
                                date = date,
                                entries =
                                    dayEntries,
                                workplaces =
                                    workplaces,
                                uiSettings =
                                    uiSettings,
                                selected =
                                    date ==
                                        selectedDate,
                                paintSelected =
                                    paintMode &&
                                        date in
                                        paintDates,
                                muted =
                                    !isCurrentMonth,
                                onClick = {
                                    onSelectedDate(
                                        date
                                    )
                                },
                                cellModifier =
                                    Modifier
                                        .fillMaxSize()
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
    paintSelected: Boolean,
    muted: Boolean = false,
    onClick: () -> Unit,
    cellModifier: Modifier = Modifier
) {
    val today = date == LocalDate.now()

    val segments = entries.flatMap {
        it.segments
    }

    val shape =
        when (
            uiSettings.calendarStyle
        ) {
            V11CalendarStyle.CLASSIC ->
                RoundedCornerShape(9.dp)

            V11CalendarStyle.COMPACT ->
                RoundedCornerShape(5.dp)

            V11CalendarStyle.CARDS ->
                RoundedCornerShape(14.dp)
        }

    val cellPadding =
        when (
            uiSettings.calendarStyle
        ) {
            V11CalendarStyle.CLASSIC ->
                0.dp

            V11CalendarStyle.COMPACT ->
                1.dp

            V11CalendarStyle.CARDS ->
                2.dp
        }

    var modifier = cellModifier
        .padding(cellPadding)
        .fillMaxSize()
        .alpha(
            if (muted)
                0.30f
            else
                1f
        )
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

                else ->
                    MaterialTheme.colorScheme.surface
            }
        )

    if (paintSelected) {
        modifier = modifier.border(
            3.dp,
            MaterialTheme.colorScheme.primary,
            shape
        )
    } else if (
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
            MaterialTheme.colorScheme.secondary,
            shape
        )
    } else {
        modifier = modifier.border(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(
                alpha =
                    if (
                        uiSettings.calendarStyle ==
                        V11CalendarStyle.CARDS
                    )
                        0.85f
                    else
                        0.55f
            ),
            shape
        )
    }

    Box(
        modifier = modifier.clickable(
            onClick = onClick
        )
    ) {
        if (segments.isNotEmpty()) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val totalHeight = maxHeight

                segments.forEach { segment ->
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

                    val range =
                        segment.timeRange()

                    val startFraction =
                        if (range == null)
                            0f
                        else
                            range.startMinute / 1440f

                    val heightFraction =
                        if (range == null)
                            1f
                        else
                            range.durationMinutes / 1440f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                totalHeight *
                                    heightFraction
                            )
                            .offset(
                                y = totalHeight *
                                    startFraction
                            )
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
                                fontSize =
                                    if (
                                        heightFraction >=
                                        0.45f
                                    )
                                        9.sp
                                    else
                                        7.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                textAlign =
                                    TextAlign.Center,
                                maxLines = 2,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 2.dp
                                    )
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
            .heightIn(max = 150.dp),
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


@Composable
private fun V11PaintPalette(
    workplaces: List<Workplace>,
    selectedWorkplace: Workplace?,
    onWorkplace: (Workplace) -> Unit,
    onShiftKind: (ShiftKind) -> Unit,
    onClose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 190.dp, max = 245.dp)
                .padding(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (selectedWorkplace == null) {
                Text(
                    "مكان العمل",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(
                        horizontal = 6.dp,
                        vertical = 3.dp
                    )
                )

                workplaces.forEach { place ->
                    V11PaletteWorkplaceRow(
                        workplace = place,
                        onClick = {
                            onWorkplace(place)
                        }
                    )
                }

                V11PaletteAddRow(
                    text = "إضافة مكان"
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 5.dp,
                            vertical = 4.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClose,
                        contentPadding =
                            PaddingValues(
                                horizontal = 5.dp,
                                vertical = 0.dp
                            )
                    ) {
                        Text(
                            "إلغاء",
                            fontSize = 10.sp
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    Text(
                        selectedWorkplace.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.width(7.dp))

                    Box(
                        Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(
                                v11Color(
                                    selectedWorkplace.colorHex
                                )
                            )
                    )
                }

                HorizontalDivider(
                    modifier =
                        Modifier.padding(vertical = 3.dp)
                )

                listOf(
                    ShiftKind.FULL_DAY,
                    ShiftKind.LONG,
                    ShiftKind.NIGHT,
                    ShiftKind.MORNING,
                    ShiftKind.EVENING
                ).forEach { kind ->
                    V11PaletteShiftRow(
                        label = V11Labels.shift(kind),
                        colorHex =
                            selectedWorkplace.colorHex,
                        onClick = {
                            onShiftKind(kind)
                        }
                    )
                }

                V11PaletteAddRow(
                    text = "إضافة نوع شيفت"
                )

                HorizontalDivider(
                    modifier =
                        Modifier.padding(vertical = 3.dp)
                )

                V11PaletteShiftRow(
                    label = V11Labels.shift(
                        ShiftKind.OFF
                    ),
                    colorHex = "#ECEFF1",
                    onClick = {
                        onShiftKind(
                            ShiftKind.OFF
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun V11PaletteWorkplaceRow(
    workplace: Workplace,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 7.dp,
                vertical = 7.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(CircleShape)
                .background(
                    v11Color(workplace.colorHex)
                )
        )

        Spacer(Modifier.width(9.dp))

        Text(
            workplace.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun V11PaletteShiftRow(
    label: String,
    colorHex: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(
                horizontal = 7.dp,
                vertical = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(25.dp)
                .clip(CircleShape)
                .background(v11Color(colorHex)),
            contentAlignment = Alignment.Center
        ) {
            // Workplace colour indicator.
        }

        Spacer(Modifier.width(9.dp))

        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun V11PaletteAddRow(
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 7.dp,
                vertical = 6.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(
                    1.dp,
                    Color(0xFF9CA3AF),
                    CircleShape
                ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = Color(0xFF4B5563)
            )
        }

        Spacer(Modifier.width(9.dp))

        Text(
            text,
            fontSize = 12.sp,
            color = Color(0xFF4B5563)
        )
    }
}

@Composable
private fun V11PaintStatusBar(
    workplace: Workplace?,
    kind: ShiftKind?,
    count: Int,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = Color.White,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                contentPadding = PaddingValues(
                    horizontal = 7.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    "إلغاء",
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    buildString {
                        append(
                            kind?.let {
                                V11Labels.shift(it)
                            } ?: ""
                        )

                        if (
                            kind != ShiftKind.OFF &&
                            workplace != null
                        ) {
                            append(" • ")
                            append(workplace.name)
                        }
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (count == 0)
                        "حدد الأيام ثم اضغط ✓"
                    else
                        "تم تحديد $count يوم",
                    fontSize = 10.sp,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (
                            kind == ShiftKind.OFF ||
                            workplace == null
                        )
                            Color(0xFFECEFF1)
                        else
                            v11Color(
                                workplace.colorHex
                            )
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V11CalendarManagerSheet(
    db: V11Database,
    activeCalendarId: String?,
    onActiveCalendarChange: (String) -> Unit,
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

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {
                            Text(
                                calendar.name,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            TextButton(
                                onClick = {
                                    onActiveCalendarChange(
                                        calendar.id
                                    )
                                },
                                contentPadding =
                                    PaddingValues(
                                        horizontal = 4.dp,
                                        vertical = 0.dp
                                    )
                            ) {
                                Text(
                                    if (
                                        calendar.id ==
                                        activeCalendarId
                                    )
                                        "✓ التقويم النشط"
                                    else
                                        "تعيين كنشط",
                                    fontSize = 10.sp,
                                    color =
                                        if (
                                            calendar.id ==
                                            activeCalendarId
                                        )
                                            Color(0xFF2E7D32)
                                        else
                                            Color(0xFF4F709C)
                                )
                            }
                        }

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
