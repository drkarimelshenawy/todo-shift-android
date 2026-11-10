package com.todoshift.app.v11.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NeoCalendarArea(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    visibleCalendarIds: Set<String>,
    selectedDate: LocalDate,
    paintMode: Boolean,
    paintDates: Set<LocalDate>,
    onSelectedDate: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onToday: () -> Unit,
    onOpenDay: () -> Unit,
    modifier: Modifier = Modifier,
    onDatabaseChange: (V11Database) -> Unit = {},
    creationCalendarId: String? = null,
) {
    val focus = LocalNeoFocus.current || LocalNeoRuntimeReduced.current
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    val locale = if (LocalNeoEnglish.current) Locale.ENGLISH else Locale.forLanguageTag("ar")
    val presentation =
        remember(db.days, db.tasks, db.dayItems, visibleCalendarIds) {
            neoVisibleDays(db, visibleCalendarIds)
        }
    val entries =
        remember(db.days, visibleCalendarIds) {
            db.days.filter { it.calendarId in visibleCalendarIds }.groupBy { it.date }
        }
    val tasks =
        remember(db.tasks, visibleCalendarIds) {
            db.tasks
                .filter {
                    it.status != TaskStatusV11.COMPLETED &&
                        (it.calendarId == null || it.calendarId in visibleCalendarIds)
                }
                .groupBy { it.date }
        }
    val events =
        remember(db.dayItems, visibleCalendarIds) {
            db.dayItems
                .filter {
                    !it.completed && (it.calendarId == null || it.calendarId in visibleCalendarIds)
                }
                .groupBy { it.date }
        }
    var sheetDate by rememberSaveable { mutableStateOf<String?>(null) }
    val view = if (paintMode) NeoCalendarView.MONTH else p.calendarView
    fun move(step: Long) {
        val date =
            when (view) {
                NeoCalendarView.MONTH -> neoMoveMonth(month, selectedDate, step)
                NeoCalendarView.WEEK -> selectedDate.plusDays(step * 7)
                NeoCalendarView.DAY -> selectedDate.plusDays(step)
            }
        onMonthChange(YearMonth.from(date))
        if (!paintMode) onSelectedDate(date)
    }
    val noteNavigator = LocalNeoFeatures.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val noteStore = remember { NeoSmartStore(context.applicationContext) }
    var noteRevision by remember { mutableIntStateOf(0) }
    val noteMetadata = remember(db.dayItems, noteRevision) { noteStore.snapshot() }
    var noteDate by remember { mutableStateOf<LocalDate?>(null) }
    var editingNote by remember { mutableStateOf<DayItemV11?>(null) }
    val flip = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(month, view, p.transition, p.motionMillis) {
        if (p.transition == NeoTransition.FLIP && p.motionMillis > 0 && !focus) {
            flip.snapTo(85f)
            flip.animateTo(0f, tween(p.motionMillis))
        } else flip.snapTo(0f)
    }
    val inheritedDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val gridDirection = neoDirection(p.gridDirection, inheritedDirection)
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { move(-1) }) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        neoLabel("السابق", "Previous"),
                    )
                }
                Text(
                    neoMonthTitle(month, LocalNeoEnglish.current),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                IconButton(onClick = { move(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, neoLabel("التالي", "Next"))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val constrained =
                    androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 340 ||
                        androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
                if (constrained)
                    Box(Modifier.weight(1f)) {
                        NeoChoice(
                            neoLabel("العرض", "View"),
                            NeoCalendarView.entries,
                            view,
                            { neoEnumLabel(it) },
                            { if (!paintMode) update(p.copy(calendarView = it)) },
                        )
                    }
                else {
                    NeoCalendarView.entries.forEach { item ->
                        FilterChip(
                            selected = view == item,
                            enabled = !paintMode,
                            onClick = { update(p.copy(calendarView = item)) },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    neoEnumLabel(item),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
                IconButton(onClick = onToday) {
                    Icon(Icons.Rounded.Today, neoLabel("الانتقال لليوم", "Jump to today"))
                }
            }
        }
        var distance by remember { mutableFloatStateOf(0f) }
        val swipeThreshold =
            with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        val allowSwipe =
            androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >=
                ((20 * fontScale + 8) * 7 + 18)
        val rtl =
            androidx.compose.ui.platform.LocalLayoutDirection.current ==
                androidx.compose.ui.unit.LayoutDirection.Rtl
        AnimatedContent(
            targetState =
                Triple(
                    month,
                    view,
                    if (view == NeoCalendarView.MONTH) month.atDay(1) else selectedDate,
                ),
            modifier =
                Modifier.weight(1f)
                    .graphicsLayer {
                        rotationY = flip.value
                        cameraDistance = 12f * density
                    }
                    .pointerInput(
                        month,
                        view,
                        selectedDate,
                        paintMode,
                        rtl,
                        swipeThreshold,
                        allowSwipe,
                    ) {
                        if (!paintMode && allowSwipe)
                            detectHorizontalDragGestures(
                                onDragStart = { distance = 0f },
                                onHorizontalDrag = { _, amount -> distance += amount },
                                onDragEnd = {
                                    neoSwipeStep(distance, swipeThreshold, rtl)?.let { move(it) }
                                    distance = 0f
                                },
                                onDragCancel = { distance = 0f },
                            )
                    },
            transitionSpec = {
                neoTransition(
                    if (focus) p.copy(reduceMotion = true) else p,
                    targetState.first >= initialState.first,
                )
            },
            label = "neo4Calendar",
        ) { state ->
            val shownMonth = state.first
            val shownView = state.second
            val date = state.third
            val start =
                if (shownView == NeoCalendarView.MONTH)
                    neoWeekStart(shownMonth.atDay(1), p.firstWeekday)
                else if (shownView == NeoCalendarView.WEEK) neoWeekStart(date, p.firstWeekday)
                else date
            val columns = if (shownView == NeoCalendarView.DAY) 1 else 7
            val rows =
                if (shownView != NeoCalendarView.MONTH) 1
                else
                    maxOf(
                        5,
                        (((shownMonth.atDay(1).dayOfWeek.value - p.firstWeekday + 7) % 7 +
                            shownMonth.lengthOfMonth() +
                            6) / 7),
                    )
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val headerHeight = 26.dp
                val minimumHeight =
                    when (p.cellSize) {
                        NeoCellSize.COMPACT -> 48.dp
                        NeoCellSize.COMFORTABLE -> 64.dp
                        NeoCellSize.LARGE -> 88.dp
                    }
                val rowHeight =
                    ((maxHeight - headerHeight) / rows).coerceIn(
                        minimumHeight,
                        minimumHeight + 12.dp,
                    )
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                val contentWidth =
                    if (columns == 1) maxWidth
                    else maxOf(maxWidth, (20.dp * fontScale + 8.dp) * columns + 18.dp)
                Column(
                    Modifier.fillMaxSize()
                        .then(
                            if (contentWidth > maxWidth)
                                Modifier.horizontalScroll(rememberScrollState())
                            else Modifier
                        )
                ) {
                    Column(Modifier.width(contentWidth).verticalScroll(rememberScrollState())) {
                        CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides
                                neoDirection(p.weekdayDirection, gridDirection)
                        ) {
                            Row(Modifier.fillMaxWidth().height(headerHeight)) {
                                repeat(columns) { col ->
                                    Text(
                                        start
                                            .plusDays(col.toLong())
                                            .format(
                                                DateTimeFormatter.ofPattern(
                                                    if (columns == 1) "EEEE" else "EEEEE",
                                                    locale,
                                                )
                                            ),
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides gridDirection
                        ) {
                            repeat(rows) { row ->
                                Row(
                                    Modifier.fillMaxWidth().height(rowHeight),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    repeat(columns) { col ->
                                        val cellDate =
                                            start.plusDays((row * columns + col).toLong())
                                        val key = cellDate.toString()
                                        val day =
                                            presentation[key]
                                                ?: NeoDayPresentation(emptyList(), 0, 0)
                                        if (
                                            !p.showOutsideDates &&
                                                YearMonth.from(cellDate) != shownMonth &&
                                                shownView == NeoCalendarView.MONTH
                                        )
                                            Spacer(Modifier.weight(1f))
                                        else
                                            NeoCalendarCell(
                                                cellDate,
                                                day.entries,
                                                day.tasks,
                                                day.events,
                                                day.notes,
                                                workplaces,
                                                noteMetadata,
                                                cellDate == selectedDate,
                                                cellDate in paintDates,
                                                YearMonth.from(cellDate) != shownMonth,
                                                Modifier.weight(1f)
                                                    .fillMaxHeight()
                                                    .padding(vertical = 1.5.dp),
                                                onLongClick = {
                                                    if (!paintMode) {
                                                        onSelectedDate(cellDate)
                                                        sheetDate = key
                                                    }
                                                },
                                            ) {
                                                onSelectedDate(cellDate)
                                                if (!paintMode) {
                                                    if (YearMonth.from(cellDate) != month)
                                                        onMonthChange(YearMonth.from(cellDate))
                                                    onOpenDay()
                                                }
                                            }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    noteDate?.let { date ->
        NeoDayNoteDialog(
            date,
            editingNote,
            expectedDatabase = db,
            calendarId = creationCalendarId,
            onDismiss = {
                noteDate = null
                editingNote = null
            },
        ) { note ->
            noteRevision++
            noteDate = null
            editingNote = null
        }
    }
    sheetDate?.let { key ->
        val date = runCatching { LocalDate.parse(key) }.getOrNull()
        if (date != null && !paintMode)
            ModalBottomSheet(onDismissRequest = { sheetDate = null }) {
                Column(
                    Modifier.fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val dayEntries = entries[key].orEmpty()
                    val dayTasks = tasks[key].orEmpty()
                    val dayEvents = events[key].orEmpty()
                    val shifts =
                        remember(db.days, visibleCalendarIds, key) {
                            neoShiftOccurrences(db, visibleCalendarIds).filter {
                                it.entry.date == key
                            }
                        }
                    if (
                        dayEntries.all { it.segments.isEmpty() } &&
                            dayTasks.isEmpty() &&
                            dayEvents.isEmpty()
                    )
                        Text(
                            neoLabel("لا توجد عناصر في هذا اليوم", "Nothing scheduled for this day")
                        )
                    shifts.forEach {
                        NeoShiftSummary(it, workplaces)
                        HorizontalDivider()
                    }
                    dayEntries
                        .flatMap { it.segments }
                        .filter { it.kind == ShiftKind.OFF }
                        .forEach { Text(neoShiftLabel(it.kind)) }
                    dayTasks.forEach { Text(neoLabel("مهمة: ", "Task: ") + it.title) }
                    dayEvents.filter { it.type != DayItemTypeV11.NOTE }.forEach { Text(it.title) }
                    presentation[key]?.notes.orEmpty().forEach { note ->
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = {
                                    editingNote = note
                                    noteDate = date
                                }
                            ) {
                                Text(note.title)
                            }
                            TextButton(
                                onClick = {
                                    sheetDate = null
                                    noteNavigator.open("ATTACHMENTS", note.id)
                                }
                            ) {
                                Text(neoLabel("مرفق", "Attach"))
                            }
                            TextButton(
                                onClick = {
                                    onDatabaseChange(
                                        db.copy(
                                            dayItems = db.dayItems.filterNot { it.id == note.id }
                                        )
                                    )
                                }
                            ) {
                                Text(neoLabel("حذف", "Delete"))
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            editingNote = null
                            noteDate = date
                        }
                    ) {
                        Text(neoLabel("ملاحظة جديدة", "New note"))
                    }
                    if (p.showCalendarLegend)
                        db.calendars
                            .filter { it.id in visibleCalendarIds }
                            .forEach { calendar ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(8.dp)
                                            .background(neoHexColor(calendar.colorHex), CircleShape)
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text(calendar.name, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                    Button(
                        onClick = {
                            sheetDate = null
                            onOpenDay()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(neoLabel("إضافة أو تعديل", "Add or edit"))
                    }
                    Spacer(Modifier.navigationBarsPadding().height(12.dp))
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeoCalendarCell(
    date: LocalDate,
    entries: List<DayEntryV11>,
    tasks: Int,
    events: Int,
    notes: List<DayItemV11>,
    workplaces: List<Workplace>,
    noteMetadata: org.json.JSONObject,
    chosen: Boolean,
    painting: Boolean,
    muted: Boolean,
    modifier: Modifier,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
) {
    val p = LocalNeoAppearance.current
    val c = MaterialTheme.colorScheme
    val today = date == LocalDate.now() && p.todayStyle != NeoTodayStyle.NONE
    val segments = remember(entries) { entries.flatMap { it.segments } }
    val description =
        date.toString() +
            neoLabel(
                "، ${segments.size} شيفت، $tasks مهمة، $events موعد",
                ", ${segments.size} shifts, $tasks tasks, $events events",
            )
    Surface(
        modifier
            .clip(RoundedCornerShape((p.cornerRadius / 2).dp))
            .semantics {
                contentDescription = description
                selected = chosen
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = (if (muted) c.surfaceContainerLowest else c.surface).copy(alpha = p.cardOpacity),
        shape = RoundedCornerShape((p.cornerRadius / 2).dp),
        border =
            BorderStroke(
                if (today || chosen || painting) 2.dp else 0.5.dp,
                when {
                    painting -> c.tertiary
                    chosen -> c.secondary
                    today -> c.primary
                    else -> c.outlineVariant
                },
            ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Neo4ShiftFill(
                segments,
                workplaces,
                Modifier.fillMaxSize().then(if (muted) Modifier.alpha(0.35f) else Modifier),
                appearance = if (p.showShiftPaint) p else p.copy(shiftPaints = emptyMap()),
            )
            NeoTodayDecoration(today, date == LocalDate.now().plusDays(1), p)
            Text(
                date.dayOfMonth.toString(),
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(2.dp)
                        .background(c.surface.copy(alpha = 0.90f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color =
                    (if (today) c.primary else c.onSurface).copy(alpha = if (muted) 0.6f else 1f),
            )
            if (p.showNoteIcons)
                notes.firstOrNull()?.title?.let { title ->
                    val marker = listOf("📌", "★", "⚠").firstOrNull { title.startsWith(it) }
                    if (marker != null)
                        Text(
                            marker,
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                }
            if ((p.showNotes && notes.isNotEmpty()) || (p.showShiftNames && segments.isNotEmpty()))
                Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 2.dp)) {
                    if (p.showNotes)
                        notes.take(2).forEach { note ->
                            val noteColor =
                                neoHexColor(
                                    noteMetadata
                                        .optJSONObject("noteColors")
                                        ?.optString(note.id, "#D8AD55") ?: "#D8AD55"
                                )
                            val noteBorder =
                                noteMetadata
                                    .optJSONObject("noteBorders")
                                    ?.optBoolean(note.id, false) == true
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!noteBorder)
                                    Box(Modifier.size(4.dp).background(noteColor, CircleShape))
                                Text(
                                    note.title
                                        .removePrefix("📌 ")
                                        .removePrefix("★ ")
                                        .removePrefix("⚠ "),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.onSurface,
                                    modifier =
                                        Modifier.weight(1f)
                                            .semantics { contentDescription = note.title }
                                            .then(
                                                if (noteBorder)
                                                    Modifier.border(
                                                        1.dp,
                                                        noteColor,
                                                        RoundedCornerShape(3.dp),
                                                    )
                                                else Modifier
                                            )
                                            .background(
                                                c.surface.copy(alpha = .85f),
                                                RoundedCornerShape(3.dp),
                                            ),
                                )
                            }
                        }
                    if (p.showShiftNames && segments.any { it.kind != ShiftKind.OFF })
                        Text(
                            segments.joinToString("/") {
                                when (it.kind) {
                                    ShiftKind.FULL_DAY -> "24h"
                                    ShiftKind.LONG -> "Long"
                                    ShiftKind.NIGHT -> neoLiteral("ليل", "Night")
                                    ShiftKind.MORNING -> neoLiteral("ص", "AM")
                                    ShiftKind.EVENING -> neoLiteral("م", "PM")
                                    ShiftKind.OFF -> neoLiteral("راحة", "Off")
                                }
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.onSurface,
                            modifier =
                                Modifier.background(
                                    c.surface.copy(alpha = .9f),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                }
            if (segments.isNotEmpty() || (p.showTaskIndicators && (tasks > 0 || events > 0)))
                Row(
                    Modifier.align(Alignment.BottomStart).padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (p.showTaskIndicators && tasks > 0)
                        Box(Modifier.size(5.dp).background(c.onSurface, CircleShape))
                    if (p.showTaskIndicators && events > 0)
                        Box(Modifier.size(5.dp).background(c.secondary, CircleShape))
                    if (segments.isNotEmpty())
                        Text(
                            if (segments.all { it.kind == ShiftKind.OFF }) neoLabel("راحة", "Off")
                            else "• ${segments.size}",
                            color = c.onSurface,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelSmall,
                            modifier =
                                Modifier.background(c.surface, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 2.dp),
                        )
                }
        }
    }
}

fun neoHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF80BFFF))
