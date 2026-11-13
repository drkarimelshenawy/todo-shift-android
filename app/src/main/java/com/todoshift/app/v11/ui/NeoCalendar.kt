package com.todoshift.app.v11.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Immutable
internal data class NeoCellMetrics(val dateHeight: Dp, val captionHeight: Dp, val dateWidth: Dp = 1.dp)

internal val NeoDateFitInfo = androidx.compose.ui.semantics.SemanticsPropertyKey<String>("CalendarDateFit")

@Composable
internal fun neoCellMetrics(): NeoCellMetrics {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val measure = rememberTextMeasurer()
    val dateStyle =
        MaterialTheme.typography.labelMedium.copy(lineHeight = 16.sp, fontWeight = FontWeight.Bold)
    val captionStyle = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp)
    return remember(measure, dateStyle, captionStyle, density) {
        val dateSizes = (1..31).map { measure.measure(it.toString(), dateStyle, maxLines = 1).size }
        val date = dateSizes.maxBy { it.width }
        val caption =
            listOf("Morning", "صباحية", "×99", "☑99", "◷99", "📌").maxOf {
                measure.measure(it, captionStyle, maxLines = 1).size.height
            }
        with(density) { NeoCellMetrics(date.height.toDp() + 4.dp, caption.toDp(), date.width.toDp()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val cellMetrics = neoCellMetrics()
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
    val commit = rememberNeoCommit(onDatabaseChange)
    val reject = LocalNeoRejectWrite.current
    var deletingNote by remember { mutableStateOf<DayItemV11?>(null) }
    deletingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text(neoLabel("حذف الملاحظة؟", "Delete note?")) },
            text = {
                Text(
                    "${note.date} • ${note.title}\n" +
                        neoLabel(
                            "تبقى الورديات والملاحظات الأخرى وملفات المرفقات دون تغيير.",
                            "Shifts, other notes and attachment files are kept.",
                        )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !LocalNeoEditorWriteState.current.pending,
                    onClick = {
                        if (db.dayItems.firstOrNull { it.id == note.id } != note)
                            reject("Note changed; review again before deleting")
                        else
                            commit(db.copy(dayItems = db.dayItems.filterNot { it.id == note.id })) {
                                deletingNote = null
                            }
                    },
                ) {
                    Text(neoLabel("حذف", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingNote = null }) { Text(neoLabel("إلغاء", "Cancel")) }
            },
        )
    }
    var sheetDate by rememberSaveable { mutableStateOf<String?>(null) }
    val view = if (paintMode) NeoCalendarView.MONTH else p.calendarView
    var tappedDate by rememberSaveable(month.toString(), view) { mutableStateOf<String?>(null) }
    fun move(step: Long) {
        tappedDate = null
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
    var noteDate by rememberSaveable { mutableStateOf<String?>(null) }
    var editingNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingNote = db.dayItems.firstOrNull { it.id == editingNoteId }
    val flip = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(month, view, p.transition, p.motionMillis) {
        if (p.transition == NeoTransition.FLIP && p.motionMillis > 0 && !focus) {
            flip.snapTo(85f)
            flip.animateTo(0f, tween(p.motionMillis))
        } else flip.snapTo(0f)
    }
    val inheritedDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val gridDirection = neoDirection(p.gridDirection, inheritedDirection)
    val premiumCashBook = true
    Column(
        modifier.fillMaxSize()
            .then(
                if (premiumCashBook) Modifier.background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                    ),
                    RoundedCornerShape(24.dp),
                ) else Modifier
            )
            .padding(if (premiumCashBook) 6.dp else 0.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .background(if (premiumCashBook) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(24.dp))
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = { move(-1) }) {
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
                FilledTonalIconButton(onClick = { move(1) }) {
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
                            shape = RoundedCornerShape(24.dp),
                            border = if (premiumCashBook && view == item) BorderStroke(1.5.dp, Color(0xFFD5AD60)) else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF173B65),
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
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
            val rows = if(shownView == NeoCalendarView.MONTH) NeoMonthGrid.dates(shownMonth,p.firstWeekday).size/7 else 1
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val fontScale =
                    androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
                val headerHeight = (24 * fontScale).dp
                // Month fit is a hard layout constraint: labels and text preferences must
                // never enlarge the month grid or introduce calendar scrolling.
                // Text is ellipsized/clipped inside a cell; full details remain tappable.
                val rowHeight = fittedCalendarRowHeight(maxHeight, headerHeight, rows)
                Column(Modifier.fillMaxSize().clipToBounds()) {
                    Column(Modifier.fillMaxSize()) {
                        CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides
                                neoDirection(p.weekdayDirection, gridDirection)
                        ) {
                            Row(Modifier.fillMaxWidth().height(headerHeight)
                                    .background(if (premiumCashBook) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, RoundedCornerShape(12.dp))) {
                                repeat(columns) { col ->
                                    val weekday = start.plusDays(col.toLong())
                                    val fullLabel =
                                        weekday.format(DateTimeFormatter.ofPattern("EEEE", locale))
                                    Text(
                                        weekday
                                            .format(
                                                DateTimeFormatter.ofPattern(
                                                    if (columns == 1) "EEEE"
                                                    else if (LocalNeoEnglish.current) "EEE"
                                                    else "EEEEE",
                                                    locale,
                                                )
                                            )
                                            .let {
                                                if (
                                                    columns > 1 &&
                                                        LocalNeoEnglish.current &&
                                                        fontScale > 1.3f
                                                )
                                                    it.take(2)
                                                else it
                                            },
                                        modifier =
                                            Modifier.weight(1f).semantics {
                                                contentDescription = fullLabel
                                            },
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
                                    horizontalArrangement = Arrangement.spacedBy(if (premiumCashBook) 4.dp else 0.dp),
                                ) {
                                    repeat(columns) { col ->
                                        val cellDate =
                                            start.plusDays((row * columns + col).toLong())
                                        val key = cellDate.toString()
                                        val monthEdges = NeoMonthGrid.edges(cellDate, shownMonth, p.firstWeekday)
                                        val monthInk = MaterialTheme.colorScheme.primary
                                        val gridRtl = gridDirection == androidx.compose.ui.unit.LayoutDirection.Rtl
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
                                                    .padding(if (premiumCashBook) 1.5.dp else 0.dp)
                                                    .drawWithContent {
                                                        drawContent()
                                                        if (shownView == NeoCalendarView.MONTH && !premiumCashBook) {
                                                            val stroke = 1.5.dp.toPx()
                                                            val inset = stroke / 2
                                                            fun edge(x1: Float,y1: Float,x2: Float,y2: Float) = drawLine(monthInk, androidx.compose.ui.geometry.Offset(x1,y1), androidx.compose.ui.geometry.Offset(x2,y2), stroke)
                                                            if (monthEdges.top) edge(0f,inset,size.width,inset)
                                                            if (monthEdges.bottom) edge(0f,size.height-inset,size.width,size.height-inset)
                                                            if (if(gridRtl) monthEdges.end else monthEdges.start) edge(inset,0f,inset,size.height)
                                                            if (if(gridRtl) monthEdges.start else monthEdges.end) edge(size.width-inset,0f,size.width-inset,size.height)
                                                        }
                                                    },
                                                metrics = cellMetrics,
                                                onLongClick = {
                                                    if (!paintMode) {
                                                        onSelectedDate(cellDate)
                                                        sheetDate = key
                                                    }
                                                },
                                            ) {
                                                val open = !paintMode && tappedDate == key && cellDate == selectedDate && YearMonth.from(cellDate) == month
                                                onSelectedDate(cellDate)
                                                if (!paintMode) {
                                                    tappedDate = key
                                                    if (YearMonth.from(cellDate) != month) {
                                                        tappedDate = null
                                                        onMonthChange(YearMonth.from(cellDate))
                                                    } else if (open) onOpenDay()
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
            LocalDate.parse(date),
            editingNote,
            expectedDatabase = db,
            calendarId = creationCalendarId,
            onDismiss = {
                noteDate?.let { sheetDate = it }
                noteDate = null
                editingNoteId = null
            },
        ) { note ->
            noteRevision++
            noteDate?.let { sheetDate = it }
            noteDate = null
            editingNoteId = null
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
                                    sheetDate = null
                                    editingNoteId = note.id
                                    noteDate = date.toString()
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
                            TextButton(onClick = { deletingNote = note }) {
                                Text(neoLabel("حذف", "Delete"))
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            sheetDate = null
                            editingNoteId = null
                            noteDate = date.toString()
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun NeoCalendarCell(
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
    metrics: NeoCellMetrics? = null,
    onClick: () -> Unit,
) {
    val p = LocalNeoAppearance.current
    val c = MaterialTheme.colorScheme
    val measured = metrics ?: neoCellMetrics()
    // neoCellMetrics already measures text with the scaled LocalDensity.
    // Multiplying by fontScale again incorrectly pushes captions outside a
    // fixed-height calendar cell (and outside the appearance preview).
    val scaledDateHeight = measured.dateHeight + 6.dp
    val today = date == LocalDate.now() && p.todayStyle != NeoTodayStyle.NONE
    val segments = remember(entries) { entries.flatMap { it.segments } }
    val names = segments.map { neoShiftLabel(it.kind) }.distinct().joinToString(" / ")
    val fillColor = segments.firstOrNull()?.let { segment ->
        val custom = p.shiftPaints[segment.kind]?.takeIf { p.showShiftPaint && it.mode != NeoPaintMode.INHERIT }?.first
        neoHexColor(custom ?: workplaces.firstOrNull { it.id == segment.workplaceId }?.colorHex ?: "#8BBACB")
    }
    val dateInk = fillColor?.let { if(it.luminance() > .179f) Color.Black else Color.White } ?: c.onSurface
    val description =
        date.toString() +
            neoLabel(
                "، ${segments.size} شيفت، $tasks مهمة، $events موعد",
                ", ${segments.size} shifts, $tasks tasks, $events events",
            ) +
            " • " +
            names +
            " • " +
            notes.joinToString { it.title }
    Surface(
        modifier
            .alpha(if (muted) .20f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .semantics {
                contentDescription = description
                selected = chosen
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = (if (muted) c.surfaceContainerLowest else c.surface).copy(alpha = p.cardOpacity),
        shape = RoundedCornerShape(12.dp),
        border =
            BorderStroke(
                if (today || chosen || painting) 2.dp else if (true) 0.5.dp else 0.5.dp,
                when {
                    painting -> c.tertiary
                    chosen -> c.secondary
                    today -> if (true) Color(0xFFD8AD55) else c.primary
                    else -> c.outlineVariant
                },
            ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // Keep two-digit dates legible in a fixed seven-column month even at 200% text.
            // Full-size text remains available in day details; cells never grow the grid.
            val dateScale = minOf(
                (maxWidth - 6.dp).coerceAtLeast(1.dp) / measured.dateWidth.coerceAtLeast(1.dp),
                (maxHeight - 6.dp).coerceAtLeast(1.dp) / measured.dateHeight.coerceAtLeast(1.dp),
                1f,
            ).coerceAtLeast(.25f)
            val compactCell = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 2f &&
                maxWidth < 48.dp && maxHeight < measured.dateHeight + measured.captionHeight + 12.dp
            Neo4ShiftFill(
                segments,
                workplaces,
                Modifier.fillMaxSize(),
                appearance = if (p.showShiftPaint) p else p.copy(shiftPaints = emptyMap()),
            )
            NeoTodayDecoration(today, date == LocalDate.now().plusDays(1), p)
            Text(
                date.dayOfMonth.toString(),
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(2.dp)
                        .padding(vertical = 1.dp)
                        .semantics { this[NeoDateFitInfo] = "cell=$maxWidth/$maxHeight metric=${measured.dateWidth}/${measured.dateHeight} scale=$dateScale" },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize * dateScale,
                    lineHeight = 16.sp * dateScale,
                ),
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Bold,
                color =
                    dateInk,
            )
            val marker =
                if (p.showNoteIcons)
                    notes.firstOrNull()?.title?.let { title ->
                        listOf("📌", "★", "⚠").firstOrNull { title.startsWith(it) }
                    }
                else null
            val markerLine =
                marker != null && androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
            if (marker != null && !compactCell)
                Text(
                    marker,
                    modifier =
                        if (markerLine)
                            Modifier.align(Alignment.TopStart)
                                .padding(start = 2.dp, top = scaledDateHeight)
                        else Modifier.align(Alignment.TopEnd).padding(2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                )
            if (!compactCell && ((p.showNotes && notes.isNotEmpty()) || ((p.showShiftNames || p.showWorkplaceNames) && segments.isNotEmpty())))
                Column(
                    Modifier.align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(
                            start = 2.dp,
                            end = 2.dp,
                            top =
                                scaledDateHeight +
                                    if (markerLine) measured.captionHeight else 0.dp,
                            bottom =
                                if (p.showTaskIndicators && (tasks > 0 || events > 0))
                                    measured.captionHeight + 8.dp
                                else 2.dp,
                        )
                ) {
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
                                    style =
                                        MaterialTheme.typography.labelSmall.copy(
                                            lineHeight = 14.sp
                                        ),
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
                    if (p.showWorkplaceNames && segments.isNotEmpty()) {
                        val placeNames = segments.mapNotNull { segment -> workplaces.firstOrNull { it.id == segment.workplaceId }?.name }.distinct().joinToString(" / ").ifEmpty { if(segments.all {it.kind==ShiftKind.OFF}) neoLabel("راحة","Off") else "" }
                        if (placeNames.isNotEmpty()) Text(placeNames, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp), color = c.onSurface,
                            modifier = Modifier.background(c.surface.copy(alpha = .85f), RoundedCornerShape(3.dp)))
                    }
                    if (p.showShiftNames && segments.any { it.kind != ShiftKind.OFF })
                        Text(
                            names,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                            color = c.onSurface,
                            modifier =
                                Modifier.background(
                                    c.surface.copy(alpha = .9f),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                }
            if (compactCell && notes.isNotEmpty() && (p.showNotes || p.showNoteIcons))
                Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(4.dp).background(NeoPremium.Gold, CircleShape))
            if (compactCell && p.showTaskIndicators && (tasks > 0 || events > 0))
                Box(Modifier.align(Alignment.BottomStart).padding(3.dp).size(4.dp).background(c.primary, CircleShape))
            if (!compactCell && p.showTaskIndicators && (tasks > 0 || events > 0))
                FlowRow(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    maxItemsInEachRow =
                        if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) 1
                        else Int.MAX_VALUE,
                ) {
                    if (p.showTaskIndicators && tasks > 0)
                        Text(
                            "☑${if(tasks>99) "99+" else tasks}",
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                            color = c.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier.background(
                                    c.surface.copy(alpha = .95f),
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    if (p.showTaskIndicators && events > 0)
                        Text(
                            "◷${if(events>99) "99+" else events}",
                            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                            color = c.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier.background(
                                    c.surface.copy(alpha = .95f),
                                    RoundedCornerShape(3.dp),
                                ),
                        )

                }
        }
    }
}

fun neoHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF80BFFF))

