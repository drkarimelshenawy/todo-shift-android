package com.todoshift.app.v11.ui

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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

private fun v11Color(hex: String): Color =
    runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color(0xFF90CAF9))

private fun monthName(month: Int): String =
    listOf(
            neoLiteral("يناير", "January"),
            neoLiteral("فبراير", "February"),
            neoLiteral("مارس", "March"),
            neoLiteral("أبريل", "April"),
            neoLiteral("مايو", "May"),
            neoLiteral("يونيو", "June"),
            neoLiteral("يوليو", "July"),
            neoLiteral("أغسطس", "August"),
            neoLiteral("سبتمبر", "September"),
            neoLiteral("أكتوبر", "October"),
            neoLiteral("نوفمبر", "November"),
            neoLiteral("ديسمبر", "December"),
        )
        .getOrElse(month - 1) { "" }

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
    onUiSettingsChange: (V11UiSettings) -> Unit,
    quickAction: NeoQuickAction? = null,
    quickActionId: Int = 0,
    financeAuthorized: Boolean = false,
    financeAmountsVisible: Boolean = false,
    onOpenWorkplaces: () -> Unit = {},
    managerRequest: Int = 0,
    onManagerHandled: () -> Unit = {},
    onManagerDismiss: () -> Unit = {},
) {
    val commit = rememberNeoCommit(onDatabaseChange)
    var selectedDate by
        rememberSaveable(
            initialDate,
            stateSaver =
                androidx.compose.runtime.saveable.Saver<LocalDate, String>(
                    save = { it.toString() },
                    restore = { LocalDate.parse(it) },
                ),
        ) {
            mutableStateOf(initialDate)
        }
    var paletteOpen by remember { mutableStateOf(false) }
    var selectedWorkplace by remember { mutableStateOf<Workplace?>(null) }
    var selectedPaintKind by remember { mutableStateOf<ShiftKind?>(null) }
    var paintMode by rememberSaveable { mutableStateOf(false) }
    var painterRequestedWorkplace by rememberSaveable { mutableStateOf<String?>(null) }
    var modernPaintRequest by remember { mutableIntStateOf(0) }
    var paintPreview by remember { mutableStateOf<V11Database?>(null) }
    var bulkPaint by rememberSaveable { mutableStateOf(false) }
    var paintDates by
        rememberSaveable(
            stateSaver =
                androidx.compose.runtime.saveable.listSaver<Set<LocalDate>, String>(
                    save = { it.map(LocalDate::toString) },
                    restore = { it.map(LocalDate::parse).toSet() },
                )
        ) {
            mutableStateOf(setOf<LocalDate>())
        }
    var openDay by
        rememberSaveable(
            stateSaver =
                androidx.compose.runtime.saveable.Saver<LocalDate?, String>(
                    save = { it?.toString() },
                    restore = { LocalDate.parse(it) },
                )
        ) {
            mutableStateOf<LocalDate?>(null)
        }
    var showCalendarManager by rememberSaveable { mutableStateOf(false) }
    var quickAddOpen by remember { mutableStateOf(false) }
    LaunchedEffect(managerRequest) {
        if (managerRequest > 0) {
            showCalendarManager = true
            onManagerHandled()
        }
    }
    LaunchedEffect(quickActionId) {
        if (quickActionId > 0)
            when (quickAction) {
                NeoQuickAction.ADD_SHIFT -> {
                    paintMode = true
                    paletteOpen = false
                    paintDates = emptySet()
                    modernPaintRequest = 0
                }
                NeoQuickAction.TODAY -> {
                    selectedDate = LocalDate.now()
                    onMonthChange(YearMonth.from(selectedDate))
                }
                else -> Unit
            }
    }

    BackHandler(enabled = openDay != null) { openDay = null }

    var pendingConflict by remember { mutableStateOf<V11PendingPaintConflict?>(null) }

    var pendingPaintQueue by remember { mutableStateOf<List<LocalDate>>(emptyList()) }

    var pendingPaintDb by remember { mutableStateOf<V11Database?>(null) }

    val visibleCalendarIds =
        remember(db.calendars) { db.calendars.filter { it.visible }.map { it.id }.toSet() }

    val activeCalendar =
        remember(db.calendars, activeCalendarId) {
            db.calendars.firstOrNull { it.id == activeCalendarId }
                ?: db.calendars.sortedBy { it.order }.firstOrNull()
        }

    Scaffold(
        containerColor =
            if (LocalV11NeoEnabled.current) androidx.compose.ui.graphics.Color.Transparent
            else MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButtonPosition =
            if (uiSettings.fabSide == FabSide.LEFT) FabPosition.Start else FabPosition.End,
        floatingActionButton = {
            if (uiSettings.experience == V11Experience.CLASSIC) {
                Column(
                    horizontalAlignment =
                        if (uiSettings.fabSide == FabSide.LEFT) Alignment.Start else Alignment.End
                ) {
                    if (paletteOpen && !paintMode) {
                        V11PaintPalette(
                            workplaces = workplaces,
                            selectedWorkplace = selectedWorkplace,
                            onWorkplace = { selectedWorkplace = it },
                            onShiftKind = { kind ->
                                selectedPaintKind = kind
                                paintDates = emptySet()
                                paintMode = true
                                paletteOpen = false
                            },
                            onAddWorkplace =
                                if (uiSettings.experience == V11Experience.PREMIUM)
                                    ({
                                        paletteOpen = false
                                        onOpenWorkplaces()
                                    })
                                else null,
                            onCustomTimes =
                                if (uiSettings.experience == V11Experience.PREMIUM)
                                    ({
                                        paletteOpen = false
                                        openDay = selectedDate
                                    })
                                else null,
                            onClose = {
                                paletteOpen = false
                                selectedWorkplace = null
                            },
                        )

                        Spacer(Modifier.height(8.dp))
                    }

                    SmallFloatingActionButton(
                        onClick = {
                            if (paintMode) {
                                val kind = selectedPaintKind
                                val calendarId =
                                    db.calendars.firstOrNull { it.id == activeCalendarId }?.id
                                        ?: db.calendars.sortedBy { it.order }.firstOrNull()?.id

                                if (kind != null && calendarId != null && paintDates.isNotEmpty()) {
                                    pendingPaintDb = db
                                    pendingPaintQueue = paintDates.sorted()

                                    fun continueQueue(startDb: V11Database) {
                                        var updated = startDb

                                        val queue = pendingPaintQueue.toMutableList()

                                        while (queue.isNotEmpty()) {
                                            val date = queue.removeAt(0)

                                            val pending =
                                                V11PaintConflictResolver.analyse(
                                                    db = updated,
                                                    date = date.toString(),
                                                    calendarId = calendarId,
                                                    kind = kind,
                                                    workplaceId =
                                                        if (kind == ShiftKind.OFF) null
                                                        else selectedWorkplace?.id,
                                                )

                                            if (pending.analysis.canAddDirectly) {
                                                updated =
                                                    V11PaintConflictResolver.applyDirect(
                                                        updated,
                                                        pending,
                                                    )

                                                pendingPaintDb = updated

                                                continue
                                            }

                                            pendingPaintDb = updated

                                            pendingPaintQueue = queue

                                            pendingConflict = pending

                                            return
                                        }

                                        commit(updated) {
                                            pendingPaintDb = null
                                            pendingPaintQueue = emptyList()
                                            paintMode = false
                                            paintDates = emptySet()
                                            selectedPaintKind = null
                                            selectedWorkplace = null
                                            paletteOpen = false
                                        }
                                    }

                                    continueQueue(db)
                                }
                            } else if (uiSettings.experience == V11Experience.PREMIUM) {
                                if (paletteOpen) {
                                    paletteOpen = false
                                    selectedWorkplace = null
                                } else quickAddOpen = !quickAddOpen
                            } else {
                                paletteOpen = !paletteOpen

                                if (!paletteOpen) {
                                    selectedWorkplace = null
                                }
                            }
                        },
                        shape = CircleShape,
                        containerColor =
                            if (paintMode) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        contentColor =
                            if (LocalV11NeoEnabled.current && !paintMode)
                                MaterialTheme.colorScheme.onPrimary
                            else Color.White,
                        modifier = Modifier.padding(bottom = 0.dp).size(48.dp),
                    ) {
                        Icon(
                            if (paintMode) Icons.Default.Check else Icons.Default.Add,
                            contentDescription =
                                if (paintMode) neoLiteral("حفظ", "Save")
                                else neoLiteral("إضافة", "Add"),
                            modifier = Modifier.size(25.dp),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = if (LocalV11NeoEnabled.current) neoSpacing() else 10.dp)
        ) {
            if (uiSettings.experience == V11Experience.CLASSIC) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        activeCalendar?.name ?: neoLiteral("التقويم", "Calendar"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.weight(1f))

                    TextButton(
                        onClick = { showCalendarManager = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(neoLiteral("إدارة التقاويم", "Manage calendars"))
                    }
                }
            }
            if (uiSettings.experience == V11Experience.PREMIUM) {
                NeoCalendarArea(
                    month = month,
                    db = paintPreview ?: db,
                    workplaces = workplaces,
                    visibleCalendarIds = visibleCalendarIds,
                    selectedDate = selectedDate,
                    paintMode = paintMode,
                    paintDates = paintDates,
                    onSelectedDate = { date ->
                        selectedDate = date
                        if (paintMode) {
                            if (bulkPaint)
                                paintDates =
                                    if (date in paintDates) paintDates - date else paintDates + date
                            else modernPaintRequest++
                        }
                    },
                    onMonthChange = onMonthChange,
                    onToday = {
                        selectedDate = LocalDate.now()
                        onMonthChange(YearMonth.from(selectedDate))
                    },
                    creationCalendarId = activeCalendarId,
                    onOpenDay = { openDay = selectedDate },
                    modifier = Modifier.weight(1f),
                    onDatabaseChange = onDatabaseChange,
                )
            } else
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
                                if (date in paintDates) paintDates - date else paintDates + date
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
                    modifier = Modifier.weight(1f),
                )

            // This slot is ALWAYS reserved, even while the workplace painter is closed.
            // Opening the bar cannot resize the calendar or cover its final week.
            if (uiSettings.experience == V11Experience.PREMIUM) {
                Box(
                    Modifier.fillMaxWidth().height(if (bulkPaint) 116.dp else 68.dp)
                        .padding(top = 2.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (paintMode || paletteOpen) {
                        LaunchedEffect(Unit) {
                            paintMode = true
                            paletteOpen = false
                        }
                        Column(Modifier.fillMaxSize()) {
                            NeoPainterBar(
                    db,
                    workplaces,
                    activeCalendar?.id,
                    paintDates,
                    selectedDate,
                    modernPaintRequest,
                    bulkPaint,
                    { bulkPaint = it },
                    { paintDates = emptySet() },
                    onDatabaseChange,
                    {
                        paintMode = false
                        paletteOpen = false
                        paintDates = emptySet()
                        modernPaintRequest = 0
                        bulkPaint = false
                        paintPreview = null
                        painterRequestedWorkplace = null
                    },
                    onPreview = { paintPreview = it },
                    initialWorkplaceId = painterRequestedWorkplace,
                            )
                        }
                    } else {
                        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(neoLabel("أماكن العمل", "Workplaces"),style=MaterialTheme.typography.labelMedium)
                                Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
                                    workplaces.forEach { place ->
                                        AssistChip(onClick = { painterRequestedWorkplace = place.id; paintMode = true }, label = { Text(place.name) }, leadingIcon = {
                                            Box(Modifier.size(10.dp).background(neoHexColor(place.colorHex), androidx.compose.foundation.shape.CircleShape))
                                        })
                                    }
                                    if (workplaces.isEmpty()) TextButton(onClick = { showCalendarManager = true }) { Text(neoLabel("إدارة التقاويم", "Manage calendars")) }
                                }
                                FilledTonalIconButton(onClick={paintDates=emptySet();bulkPaint=false;paintMode=true;paletteOpen=false},
                                    modifier=Modifier.size(48.dp).testTag("calendar-paint-fab")) {
                                    Icon(Icons.Default.Add,neoLabel("رسم الورديات","Paint shifts"))
                                }
                            }
                        }
                    }
                }
            } else if (paintMode) {
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
                    },
                )
            }
        }
    }

    if (quickAddOpen) {
        AlertDialog(
            onDismissRequest = { quickAddOpen = false },
            title = { Text(neoLabel(neoLiteral("إضافة سريعة", "Quick add"), "Quick add")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            quickAddOpen = false
                            openDay = selectedDate
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            neoLabel(
                                neoLiteral(
                                    "شيفت أو مهمة أو موعد لليوم المختار",
                                    "Shift, task or event for selected day",
                                ),
                                "Shift, task or event for selected day",
                            )
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            quickAddOpen = false
                            paletteOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            neoLabel(
                                neoLiteral("رسم الشيفتات على التقويم", "Paint shifts on calendar"),
                                "Paint shifts on calendar",
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { quickAddOpen = false }) {
                    Text(neoLabel(neoLiteral("إغلاق", "Close"), "Close"))
                }
            },
        )
    }

    openDay?.let { day ->
        V11DayDetailsScreen(
            financeAuthorized = financeAuthorized,
            financeAmountsVisible = financeAmountsVisible,
            initialDate = day,
            db = db,
            workplaces = workplaces,
            activeCalendarId = activeCalendarId,
            onBack = { openDay = null },
            onDatabaseChange = onDatabaseChange,
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
            title = { Text(neoLiteral("تعارض في الشيفت", "Shift conflict")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        neoLiteral(
                            "اليوم ${conflict.date} يحتوي على شيفت يتداخل زمنياً مع الشيفت الجديد.",
                            "Day ${conflict.date} has a shift overlapping the new shift.",
                        )
                    )

                    Text(
                        neoLiteral(
                            "اختر طريقة حل التعارض. لن يتم إنشاء شيفتين متداخلين.",
                            "Choose a conflict resolution. Overlapping shifts will not be created.",
                        )
                    )

                    conflict.analysis.options
                        .filter { it.action != ShiftConflictActionV11.REJECT }
                        .forEachIndexed { index, option ->
                            OutlinedButton(
                                onClick = {
                                    val base = pendingPaintDb ?: db

                                    var updated =
                                        V11PaintConflictResolver.applyOption(
                                            db = base,
                                            pending = conflict,
                                            option = option,
                                        )

                                    pendingConflict = null

                                    val remaining = pendingPaintQueue

                                    val kind = selectedPaintKind

                                    val calendar = conflict.calendarId

                                    if (kind == null || remaining.isEmpty()) {
                                        commit(updated) {
                                            pendingPaintDb = null
                                            pendingPaintQueue = emptyList()
                                            paintMode = false
                                            paintDates = emptySet()
                                            selectedPaintKind = null
                                            selectedWorkplace = null
                                            paletteOpen = false
                                        }
                                    } else {
                                        val queue = remaining.toMutableList()

                                        while (queue.isNotEmpty()) {
                                            val date = queue.removeAt(0)

                                            val next =
                                                V11PaintConflictResolver.analyse(
                                                    db = updated,
                                                    date = date.toString(),
                                                    calendarId = calendar,
                                                    kind = kind,
                                                    workplaceId =
                                                        if (kind == ShiftKind.OFF) null
                                                        else selectedWorkplace?.id,
                                                )

                                            if (next.analysis.canAddDirectly) {
                                                updated =
                                                    V11PaintConflictResolver.applyDirect(
                                                        updated,
                                                        next,
                                                    )

                                                continue
                                            }

                                            pendingPaintDb = updated

                                            pendingPaintQueue = queue

                                            pendingConflict = next

                                            return@OutlinedButton
                                        }

                                        pendingPaintDb = null

                                        pendingPaintQueue = emptyList()

                                        commit(updated) {
                                            paintMode = false
                                            paintDates = emptySet()
                                            selectedPaintKind = null
                                            selectedWorkplace = null
                                            paletteOpen = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    when (option.action) {
                                        ShiftConflictActionV11.REPLACE ->
                                            neoLiteral(
                                                "استبدال الشيفت القديم",
                                                "Replace existing shift",
                                            )

                                        ShiftConflictActionV11.SPLIT_EXISTING ->
                                            neoLiteral(
                                                "تقسيم الشيفت القديم وحفظ الوقت المتبقي",
                                                "Split existing shift and keep remaining time",
                                            )

                                        ShiftConflictActionV11.TRIM_EXISTING_BEFORE ->
                                            neoLiteral(
                                                "تقصير الشيفت القديم قبل الجديد",
                                                "Trim existing shift before the new one",
                                            )

                                        ShiftConflictActionV11.TRIM_EXISTING_AFTER ->
                                            neoLiteral(
                                                "تقصير الشيفت القديم بعد الجديد",
                                                "Trim existing shift after the new one",
                                            )

                                        ShiftConflictActionV11.ADD -> neoLiteral("إضافة", "Add")

                                        else ->
                                            neoLiteral("الحل ${index + 1}", "Option ${index + 1}")
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
                    Text(neoLiteral("إلغاء بدون تغيير", "Cancel without changes"))
                }
            },
        )
    }

    if (showCalendarManager) {
        V11CalendarManagerSheet(
            db = db,
            activeCalendarId = activeCalendarId,
            onActiveCalendarChange = onActiveCalendarChange,
            onDismiss = {
                showCalendarManager = false
                onManagerDismiss()
            },
            onDatabaseChange = onDatabaseChange,
        )
    }
}

@Composable
private fun V11CompactHeader(activeCalendarName: String, onManageCalendars: () -> Unit) {
    val neo = LocalV11NeoEnabled.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(if (neo) 64.dp else 52.dp)
                .then(
                    if (neo)
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp)
                    else Modifier
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.neo41_brand_emblem),
            contentDescription = "ToDo Shift",
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "ToDo Shift",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (neo) colors.onSurface else Color(0xFF162A46),
                maxLines = 1,
            )

            Text(
                activeCalendarName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (neo) colors.onSurfaceVariant else Color(0xFF4F709C),
                maxLines = 1,
            )
        }

        TextButton(
            onClick = onManageCalendars,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text(
                neoLiteral("إدارة التقاويم", "Manage calendars"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun V11MonthSelector(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val neo = LocalV11NeoEnabled.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(if (neo) 68.dp else 58.dp)
                .then(
                    if (neo)
                        Modifier.clip(RoundedCornerShape(22.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.outlineVariant, RoundedCornerShape(22.dp))
                            .padding(horizontal = 10.dp)
                    else Modifier
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(38.dp)) {
            Text("‹", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                "${monthName(month.monthValue)} ${month.year}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (neo) colors.onSurface else Color(0xFF16181D),
            )

            TextButton(
                onClick = onToday,
                modifier = Modifier.height(25.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            ) {
                Text(
                    neoLiteral("انتقل لتاريخ اليوم", "Go to today"),
                    fontSize = 9.sp,
                    color = if (neo) colors.primary else Color(0xFF4F709C),
                )
            }
        }

        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(38.dp)) {
            Text("›", fontSize = 27.sp, fontWeight = FontWeight.Bold)
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
    modifier: Modifier = Modifier,
) {
    var previousMonth by remember { mutableStateOf(month) }

    val direction =
        remember(month) {
            when {
                month > previousMonth -> 1
                month < previousMonth -> -1
                else -> 0
            }
        }

    LaunchedEffect(month) { previousMonth = month }

    AnimatedContent(
        modifier = modifier.fillMaxSize(),
        targetState = month,
        transitionSpec = {
            val forward = targetState > initialState

            ContentTransform(
                targetContentEnter =
                    slideInHorizontally(
                        animationSpec = tween(280),
                        initialOffsetX = { width -> if (forward) width else -width },
                    ) + fadeIn(animationSpec = tween(220)),
                initialContentExit =
                    slideOutHorizontally(
                        animationSpec = tween(280),
                        targetOffsetX = { width -> if (forward) -width else width },
                    ) + fadeOut(animationSpec = tween(180)),
            )
        },
        label = "monthTransition",
    ) { shownMonth ->
        Column(modifier = Modifier.fillMaxSize()) {
            V11MonthSelector(
                month = shownMonth,
                onPrevious = { onMonthChange(shownMonth.minusMonths(1)) },
                onNext = { onMonthChange(shownMonth.plusMonths(1)) },
                onToday = onToday,
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
                onPrevious = { onMonthChange(shownMonth.minusMonths(1)) },
                onNext = { onMonthChange(shownMonth.plusMonths(1)) },
                modifier = Modifier.weight(1f),
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
    modifier: Modifier = Modifier,
) {
    val first = month.atDay(1)

    // Saturday = first column.
    val firstIndex = (first.dayOfWeek.value + 1) % 7

    val naturalRows = (firstIndex + month.lengthOfMonth() + 6) / 7

    val rows = maxOf(5, naturalRows)

    val gridStart = first.minusDays(firstIndex.toLong())

    var drag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier =
            modifier.fillMaxSize().pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { _, amount -> drag += amount },
                    onDragEnd = {
                        when {
                            drag > 90f -> onPrevious()

                            drag < -90f -> onNext()
                        }

                        drag = 0f
                    },
                )
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(
                    neoLiteral("السبت", "Saturday"),
                    neoLiteral("الأحد", "Sunday"),
                    neoLiteral("الاثنين", "Monday"),
                    neoLiteral("الثلاثاء", "Tuesday"),
                    neoLiteral("الأربعاء", "Wednesday"),
                    neoLiteral("الخميس", "Thursday"),
                    neoLiteral("الجمعة", "Friday"),
                )
                .forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
        }

        Spacer(Modifier.height(4.dp))

        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            repeat(rows) { row ->
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    repeat(7) { col ->
                        val cell = row * 7 + col

                        val date = gridStart.plusDays(cell.toLong())

                        val isCurrentMonth = YearMonth.from(date) == month

                        val dayEntries =
                            db.days.filter {
                                it.date == date.toString() && it.calendarId in visibleCalendarIds
                            }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(1.5.dp)) {
                            V11DayCell(
                                date = date,
                                entries = dayEntries,
                                workplaces = workplaces,
                                uiSettings = uiSettings,
                                selected = date == selectedDate,
                                paintSelected = paintMode && date in paintDates,
                                muted = !isCurrentMonth,
                                onClick = { onSelectedDate(date) },
                                cellModifier = Modifier.fillMaxSize(),
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
    cellModifier: Modifier = Modifier,
) {
    val today = date == LocalDate.now()

    val segments = entries.flatMap { it.segments }

    val shape =
        when (uiSettings.calendarStyle) {
            V11CalendarStyle.CLASSIC -> RoundedCornerShape(9.dp)

            V11CalendarStyle.COMPACT -> RoundedCornerShape(5.dp)

            V11CalendarStyle.CARDS -> RoundedCornerShape(14.dp)
        }

    val cellPadding =
        when (uiSettings.calendarStyle) {
            V11CalendarStyle.CLASSIC -> 0.dp

            V11CalendarStyle.COMPACT -> 1.dp

            V11CalendarStyle.CARDS -> 2.dp
        }

    var modifier =
        cellModifier
            .padding(cellPadding)
            .fillMaxSize()
            .alpha(if (muted) 0.30f else 1f)
            .clip(shape)
            .background(
                when {
                    today &&
                        uiSettings.todayHighlightStyle in
                            listOf(TodayHighlightStyle.TINT, TodayHighlightStyle.BORDER_TINT) ->
                        v11Color(uiSettings.todayHighlightColor).copy(alpha = 0.10f)

                    else -> MaterialTheme.colorScheme.surface
                }
            )

    if (paintSelected) {
        modifier = modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
    } else if (
        today &&
            uiSettings.todayHighlightStyle in
                listOf(TodayHighlightStyle.BORDER, TodayHighlightStyle.BORDER_TINT)
    ) {
        modifier = modifier.border(2.dp, v11Color(uiSettings.todayHighlightColor), shape)
    } else if (selected) {
        modifier = modifier.border(2.dp, MaterialTheme.colorScheme.secondary, shape)
    } else {
        modifier =
            modifier.border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(
                    alpha = if (uiSettings.calendarStyle == V11CalendarStyle.CARDS) 0.85f else 0.55f
                ),
                shape,
            )
    }

    Box(modifier = modifier.clickable(onClick = onClick)) {
        if (segments.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val totalHeight = maxHeight

                segments.forEach { segment ->
                    val place = workplaces.firstOrNull { it.id == segment.workplaceId }

                    val bg =
                        if (segment.kind == ShiftKind.OFF) Color(0xFFECEFF1)
                        else place?.let { v11Color(it.colorHex) } ?: Color(0xFFE5E7EB)

                    val range = segment.timeRange()

                    val startFraction = if (range == null) 0f else range.startMinute / 1440f

                    val heightFraction = if (range == null) 1f else range.durationMinutes / 1440f

                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(totalHeight * heightFraction)
                                .offset(y = totalHeight * startFraction)
                                .background(bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (uiSettings.showWorkplaceNames) {
                            Text(
                                if (segment.kind == ShiftKind.OFF) neoLiteral("إجازة", "Off")
                                else place?.name ?: "",
                                fontSize = if (heightFraction >= 0.45f) 9.sp else 7.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                modifier = Modifier.padding(horizontal = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        if (today && uiSettings.todayHighlightStyle == TodayHighlightStyle.RING) {
            Box(
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(3.dp)
                        .size(24.dp)
                        .border(2.dp, v11Color(uiSettings.todayHighlightColor), CircleShape)
            )
        }

        Text(
            date.dayOfMonth.toString(),
            modifier =
                Modifier.align(Alignment.TopStart)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.90f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF111827),
        )

        if (segments.size > 4) {
            Text(
                "+${segments.size - 4}",
                modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
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
    onDeleteSegment: (String) -> Unit,
) {
    val entries =
        db.days.filter { it.date == date.toString() && it.calendarId in visibleCalendarIds }

    val neo = LocalV11NeoEnabled.current
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = if (neo) 190.dp else 150.dp),
        shape = if (neo) RoundedCornerShape(22.dp) else CardDefaults.shape,
        border =
            if (neo)
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                )
            else null,
        colors =
            CardDefaults.cardColors(
                containerColor = if (neo) MaterialTheme.colorScheme.surface else Color.White
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FilledTonalButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(neoLiteral("إضافة شيفت", "Add shift"))
                }

                Text(
                    "${date.dayOfMonth} ${
                        monthName(date.monthValue)
                    }",
                    fontWeight = FontWeight.Black,
                )
            }

            if (entries.isEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    neoLiteral("لا توجد شيفتات في هذا اليوم", "No shifts on this day"),
                    color =
                        if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                        else Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }

            entries.forEach { entry ->
                val calendar = db.calendars.firstOrNull { it.id == entry.calendarId }

                entry.segments.forEach { segment ->
                    Spacer(Modifier.height(7.dp))

                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (LocalV11NeoEnabled.current)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else Color(0xFFF6F7F9)
                                )
                                .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { onDeleteSegment(segment.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                neoLiteral("حذف تقويم فارغ فقط", "Delete an empty calendar only"),
                                tint = Color(0xFFB3261E),
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Column(horizontalAlignment = Alignment.End) {
                            Text(neoShiftText(segment.kind), fontWeight = FontWeight.Bold)

                            val place = workplaces.firstOrNull { it.id == segment.workplaceId }

                            Text(
                                listOfNotNull(place?.name, calendar?.name).joinToString(" • "),
                                fontSize = 11.sp,
                                color =
                                    if (LocalV11NeoEnabled.current)
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    else Color.Gray,
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            Modifier.size(13.dp)
                                .clip(CircleShape)
                                .background(
                                    workplaces
                                        .firstOrNull { it.id == segment.workplaceId }
                                        ?.let { v11Color(it.colorHex) } ?: Color.LightGray
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
    onClose: () -> Unit,
    onAddWorkplace: (() -> Unit)? = null,
    onCustomTimes: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.surface
                    else Color.White
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier =
                Modifier.widthIn(
                        min = 190.dp,
                        max = if (LocalV11NeoEnabled.current) 280.dp else 245.dp,
                    )
                    .then(
                        if (LocalV11NeoEnabled.current)
                            Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())
                        else Modifier
                    )
                    .padding(10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            if (selectedWorkplace == null) {
                Text(
                    neoLiteral("مكان العمل", "Workplace"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                        else Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )

                workplaces.forEach { place ->
                    V11PaletteWorkplaceRow(workplace = place, onClick = { onWorkplace(place) })
                }

                V11PaletteAddRow(
                    text = neoLiteral("إضافة مكان", "Add workplace"),
                    onClick = onAddWorkplace,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                    ) {
                        Text(neoLiteral("إلغاء", "Cancel"), fontSize = 10.sp)
                    }

                    Spacer(Modifier.weight(1f))

                    Text(selectedWorkplace.name, fontWeight = FontWeight.Black, fontSize = 13.sp)

                    Spacer(Modifier.width(7.dp))

                    Box(
                        Modifier.size(19.dp)
                            .clip(CircleShape)
                            .background(v11Color(selectedWorkplace.colorHex))
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp))

                listOf(
                        ShiftKind.FULL_DAY,
                        ShiftKind.LONG,
                        ShiftKind.NIGHT,
                        ShiftKind.MORNING,
                        ShiftKind.EVENING,
                    )
                    .forEach { kind ->
                        V11PaletteShiftRow(
                            label = neoShiftText(kind),
                            colorHex = selectedWorkplace.colorHex,
                            onClick = { onShiftKind(kind) },
                        )
                    }

                V11PaletteAddRow(
                    text =
                        if (LocalV11NeoEnabled.current)
                            neoLabel("إدارة أوقات اليوم", "Edit day times")
                        else neoLiteral("إضافة نوع شيفت", "Add shift type"),
                    onClick = onCustomTimes,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 3.dp))

                V11PaletteShiftRow(
                    label = neoShiftText(ShiftKind.OFF),
                    colorHex = "#ECEFF1",
                    onClick = { onShiftKind(ShiftKind.OFF) },
                )
            }
        }
    }
}

@Composable
private fun V11PaletteWorkplaceRow(workplace: Workplace, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .then(if (LocalV11NeoEnabled.current) Modifier.heightIn(min = 48.dp) else Modifier)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(31.dp).clip(CircleShape).background(v11Color(workplace.colorHex))
        )

        Spacer(Modifier.width(9.dp))

        Text(workplace.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V11PaletteShiftRow(label: String, colorHex: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .then(if (LocalV11NeoEnabled.current) Modifier.heightIn(min = 48.dp) else Modifier)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(25.dp).clip(CircleShape).background(v11Color(colorHex)),
            contentAlignment = Alignment.Center,
        ) {
            // Workplace colour indicator.
        }

        Spacer(Modifier.width(9.dp))

        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V11PaletteAddRow(text: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.heightIn(min = 48.dp).clickable(onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = 7.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(28.dp).clip(CircleShape).border(1.dp, Color(0xFF9CA3AF), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint =
                    if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                    else Color(0xFF4B5563),
            )
        }

        Spacer(Modifier.width(9.dp))

        Text(
            text,
            fontSize = 12.sp,
            color =
                if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                else Color(0xFF4B5563),
        )
    }
}

@Composable
private fun V11PaintStatusBar(
    workplace: Workplace?,
    kind: ShiftKind?,
    count: Int,
    onCancel: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.surface else Color.White,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
            ) {
                Text(neoLiteral("إلغاء", "Cancel"), fontSize = 11.sp)
            }

            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    buildString {
                        append(kind?.let { neoShiftText(it) } ?: "")

                        if (kind != ShiftKind.OFF && workplace != null) {
                            append(" • ")
                            append(workplace.name)
                        }
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    if (count == 0) neoLiteral("حدد الأيام ثم اضغط ✓", "Select days, then tap ✓")
                    else neoLiteral("تم تحديد $count يوم", "$count days selected"),
                    fontSize = 12.sp,
                    color =
                        if (LocalV11NeoEnabled.current) MaterialTheme.colorScheme.onSurfaceVariant
                        else Color(0xFF6B7280),
                )
            }

            Spacer(Modifier.width(8.dp))

            Box(
                Modifier.size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (kind == ShiftKind.OFF || workplace == null) Color(0xFFECEFF1)
                        else v11Color(workplace.colorHex)
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
    onDatabaseChange: (V11Database) -> Unit,
) {
    val commit = rememberNeoCommit(onDatabaseChange)
    if (LocalV11NeoEnabled.current) {
        NeoCalendarManagerDialog(
            db,
            activeCalendarId,
            onActiveCalendarChange,
            onDatabaseChange,
            onDismiss,
        )
        return
    }
    var newName by remember { mutableStateOf("") }

    var selectedColor by remember { mutableStateOf("#4F709C") }

    val colors =
        listOf(
            "#213555",
            "#4F709C",
            "#9DD8AE",
            "#FFE28A",
            "#D7ACE8",
            "#91C1EE",
            "#FFAB91",
            "#80CBC4",
        )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, neoLiteral("إغلاق", "Close"))
                }

                Text(
                    neoLiteral("إدارة التقاويم", "Manage calendars"),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                )
            }

            Spacer(Modifier.height(10.dp))

            db.calendars
                .sortedBy { it.order }
                .forEach { calendar ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = {
                                onDatabaseChange(
                                    V11CalendarController.deleteCalendar(db, calendar.id)
                                )
                            },
                            enabled =
                                db.calendars.size > 1 &&
                                    db.days.none { it.calendarId == calendar.id } &&
                                    db.tasks.none { it.calendarId == calendar.id } &&
                                    db.dayItems.none { it.calendarId == calendar.id } &&
                                    db.incomeSources.none { it.calendarId == calendar.id } &&
                                    db.settlements.none { it.calendarId == calendar.id },
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                neoLiteral("حذف تقويم فارغ فقط", "Delete an empty calendar only"),
                            )
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
                            Text(calendar.name, fontWeight = FontWeight.Bold)

                            TextButton(
                                onClick = { onActiveCalendarChange(calendar.id) },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    if (calendar.id == activeCalendarId)
                                        neoLiteral("✓ الأساسي للإضافة", "✓ Primary for new entries")
                                    else neoLiteral("تعيين كأساسي", "Set as primary"),
                                    fontSize = 12.sp,
                                    color =
                                        if (calendar.id == activeCalendarId)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            Modifier.size(18.dp)
                                .clip(CircleShape)
                                .background(v11Color(calendar.colorHex))
                        )
                    }
                }

            HorizontalDivider()

            Spacer(Modifier.height(14.dp))

            Text(neoLiteral("تقويم جديد", "New calendar"), fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(neoLiteral("اسم التقويم", "Calendar name")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                colors.forEach { color ->
                    Box(
                        modifier =
                            Modifier.size(31.dp)
                                .clip(CircleShape)
                                .background(v11Color(color))
                                .then(
                                    if (selectedColor == color)
                                        Modifier.border(3.dp, Color.Black, CircleShape)
                                    else Modifier
                                )
                                .clickable { selectedColor = color }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    if (newName.isBlank()) return@Button

                    commit(V11CalendarController.addCalendar(db, newName, selectedColor)) {
                        newName = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(5.dp))
                Text(neoLiteral("إنشاء التقويم", "Create calendar"))
            }
        }
    }
}


