package com.todoshift.app.v11.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.shadow
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
        val dateLayouts = (1..31).map { measure.measure(it.toString(), dateStyle, maxLines = 1) }
        // Include paragraph intrinsics: fallback fonts can be wider than the rounded layout size.
        val dateHeight = dateLayouts.maxOf { it.size.height }
        val dateWidth = dateLayouts.maxOf { maxOf(it.size.width.toFloat(), it.multiParagraph.maxIntrinsicWidth) }
        val caption =
            listOf("Morning", "صباحية", "×99", "☑99", "◷99", "📌").maxOf {
                measure.measure(it, captionStyle, maxLines = 1).size.height
            }
        with(density) { NeoCellMetrics(dateHeight.toDp() + 4.dp, caption.toDp(), dateWidth.toDp()) }
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
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onDatabaseChange: (V11Database) -> Unit = {},
    creationCalendarId: String? = null,
) {
    val focus = LocalNeoFocus.current || LocalNeoRuntimeReduced.current
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityStore = remember(context) { V11SecurityStore(context.applicationContext) }
    val includeTaskContent = securityStore.load().let {!it.protectTasks&&!it.protectTasksOnly}
    val includeNoteContent = securityStore.load().let {!it.protectTasks&&!it.protectNotes}
    val cellMetrics = neoCellMetrics()
    val locale = if (LocalNeoEnglish.current) Locale.ENGLISH else Locale.forLanguageTag("ar")
    val presentation =
        remember(db.days, db.tasks, db.dayItems, visibleCalendarIds, includeTaskContent, includeNoteContent) {
            neoVisibleDays(db, visibleCalendarIds, includeTaskContent, includeNoteContent)
        }
    val entries =
        remember(db.days, visibleCalendarIds) {
            db.days.filter { it.calendarId in visibleCalendarIds }.groupBy { it.date }
        }
    val tasks =
        remember(db.tasks, visibleCalendarIds, includeTaskContent, includeNoteContent) {
            db.tasks
                .filter {
                    includeTaskContent && (includeNoteContent || it.linkedNoteId==null) && it.status != TaskStatusV11.COMPLETED &&
                        (it.calendarId == null || it.calendarId in visibleCalendarIds)
                }
                .groupBy { it.date }
        }
    val events =
        remember(db.dayItems, visibleCalendarIds, includeTaskContent, includeNoteContent) {
            db.dayItems
                .filter {
                    !it.completed && (when(it.type) {DayItemTypeV11.NOTE -> includeNoteContent;DayItemTypeV11.TASK -> includeTaskContent;else -> true}) && (it.type != DayItemTypeV11.NOTE || (includeNoteContent && it.noteCalendarLinked && it.noteVisible && !it.notePrivate)) && (it.calendarId == null || it.calendarId in visibleCalendarIds)
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
                            commit(db.copy(dayItems = db.dayItems.map { if (it.id == note.id) it.copy(noteTrashed = true) else it })) {
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
    var selectedShiftId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }
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
            .shadow(8.dp, RoundedCornerShape(28.dp), clip = false)
            .border(1.dp, MaterialTheme.colorScheme.surface.copy(alpha = .9f), RoundedCornerShape(28.dp))
            .then(
                if (premiumCashBook) Modifier.background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                    ),
                    RoundedCornerShape(24.dp),
                ) else Modifier
            )
            .padding(if (premiumCashBook) 5.dp else 0.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .background(if (premiumCashBook) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(24.dp))
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(onClick = { move(-1) }, modifier = Modifier.shadow(3.dp, CircleShape), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        neoLabel("السابق", "Previous"),
                    )
                }
                Text(
                    neoMonthTitle(month, LocalNeoEnglish.current),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                FilledTonalIconButton(onClick = { move(1) }, modifier = Modifier.shadow(3.dp, CircleShape), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, neoLabel("التالي", "Next"))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
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
                            modifier = Modifier.weight(1f).shadow(3.dp, RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(if (view == item) 1.5.dp else 1.dp, if (view == item) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            label = {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(
                                    neoEnumLabel(item),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                ) }
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
        if (view == NeoCalendarView.WEEK && !paintMode) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { update(p.copy(weeklyRows = !p.weeklyRows)) }) {
                    Text(if (p.weeklyRows) neoLabel("عرض ٧ أعمدة", "Show 7 columns") else neoLabel("عرض ٧ صفوف", "Show 7 rows"))
                }
            }
        }
        val swipeThreshold =
            with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
        // Month navigation must remain available at every supported screen width.
        val allowSwipe = true
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
            val weeklyRows = shownView == NeoCalendarView.WEEK && p.weeklyRows
            val columns = if (shownView == NeoCalendarView.DAY || weeklyRows) 1 else 7
            val rows = if(shownView == NeoCalendarView.MONTH) NeoMonthGrid.dates(shownMonth,p.firstWeekday).size/7 else if (weeklyRows) 7 else 1
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
                                    horizontalArrangement = Arrangement.spacedBy(if (premiumCashBook) 1.dp else 0.dp),
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
                                                    .padding(if (premiumCashBook) 1.dp else 0.dp)
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
                                                weekdayLabel = if (weeklyRows) cellDate.format(DateTimeFormatter.ofPattern("EEEE", locale)) else null,
                                                detailMode = if(shownView==NeoCalendarView.DAY) 2 else if(shownView==NeoCalendarView.WEEK) 1 else 0,
                                                weekRowMode = weeklyRows,
                                                onOpenNote = if (paintMode) null else { note ->
                                                    onSelectedDate(cellDate)
                                                    editingNoteId = note.id
                                                    noteDate = cellDate.toString()
                                                },
                                                onOpenShift = if(paintMode) null else {segment->selectedShiftId=segment.id},
                                                onOpenTasks = if(paintMode) null else { {val items=tasks[key].orEmpty();if(items.size==1) noteNavigator.open("TASK_DETAIL",items.first().id) else {onSelectedDate(cellDate);sheetDate=key}} },
                                                onOpenEvents = if(paintMode) null else { {val items=events[key].orEmpty();if(items.size==1) selectedEventId=items.first().id else {onSelectedDate(cellDate);sheetDate=key}} },
                                                onLongClick = {
                                                    if (!paintMode) {
                                                        onSelectedDate(cellDate)
                                                        sheetDate = key
                                                    }
                                                },
                                            ) {
                                                onSelectedDate(cellDate)
                                                if (!paintMode) {
                                                    val secondTap = tappedDate == key
                                                    tappedDate = key
                                                    if (YearMonth.from(cellDate) != month) {
                                                        tappedDate = null
                                                        onMonthChange(YearMonth.from(cellDate))
                                                    }
                                                    if (secondTap) onOpenDay(cellDate)
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
    selectedShiftId?.let { id ->
        val shift = neoShiftOccurrences(db, visibleCalendarIds).firstOrNull { it.segment.id == id }
        if (shift != null) AlertDialog(
            onDismissRequest = { selectedShiftId = null },
            title = { Text(neoShiftLabel(shift.segment.kind)) },
            text = { Column {
                NeoShiftSummary(shift, workplaces)
                if (shift.segment.note.isNotBlank()) Text(shift.segment.note)
            } },
            confirmButton = { TextButton(onClick = {
                selectedShiftId = null
                sheetDate = null
                onSelectedDate(LocalDate.parse(shift.entry.date))
                onOpenDay(LocalDate.parse(shift.entry.date))
            }) { Text(neoLabel("تعديل", "Edit")) } },
            dismissButton = { TextButton(onClick = { selectedShiftId = null }) { Text(neoLabel("إغلاق", "Close")) } },
        )
    }
    selectedEventId?.let { id ->
        val event = db.dayItems.firstOrNull { it.id == id && it.type != DayItemTypeV11.NOTE && (includeTaskContent || it.type != DayItemTypeV11.TASK) }
        if (event != null) AlertDialog(
            onDismissRequest = { selectedEventId = null },
            title = { Text(event.title) },
            text = { Text(listOf(event.date, event.details).filter { it.isNotBlank() }.joinToString("\n")) },
            confirmButton = { TextButton(enabled = !LocalNeoEditorWriteState.current.pending, onClick = {
                commit(V11DayOrganizerController.toggle(db, event.id)) { selectedEventId = null }
            }) { Text(if (event.completed) neoLabel("إعادة فتح", "Reopen") else neoLabel("إكمال", "Complete")) } },
            dismissButton = { TextButton(onClick = { selectedEventId = null }) { Text(neoLabel("إغلاق", "Close")) } },
        )
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
                    shifts.forEach { shift ->
                        Column(Modifier.fillMaxWidth().clickable { selectedShiftId = shift.segment.id }.padding(vertical = 6.dp)) {
                            NeoShiftSummary(shift, workplaces)
                        }
                        HorizontalDivider()
                    }
                    dayEntries
                        .flatMap { it.segments }
                        .filter { it.kind == ShiftKind.OFF }
                        .forEach { Text(neoShiftLabel(it.kind)) }
                    dayTasks.forEach { task ->
                        TextButton(onClick = { sheetDate = null; noteNavigator.open("TASK_DETAIL", task.id) }) {
                            Text(neoLabel("مهمة: ", "Task: ") + task.title)
                        }
                    }
                    dayEvents.filter { it.type != DayItemTypeV11.NOTE }.forEach { event ->
                        TextButton(onClick = { selectedEventId = event.id }) { Text(event.title) }
                    }
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
                            onOpenDay(date)
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
    weekdayLabel: String? = null,
    detailMode: Int = 0,
    weekRowMode: Boolean = false,
    onOpenNote: ((DayItemV11) -> Unit)? = null,
    onOpenShift: ((ShiftSegmentV11)->Unit)? = null,
    onOpenTasks: (()->Unit)? = null,
    onOpenEvents: (()->Unit)? = null,
    onClick: () -> Unit,
) {
    var pickedShifts by remember {mutableStateOf(emptyList<ShiftSegmentV11>())}
    if(pickedShifts.isNotEmpty()) AlertDialog(onDismissRequest={pickedShifts=emptyList()},title={Text(neoLabel("الورديات","Shifts"))},text={Column {pickedShifts.forEach {shift->TextButton(onClick={pickedShifts=emptyList();onOpenShift?.invoke(shift)}) {Text(neoShiftLabel(shift.kind)+" • "+workplaces.firstOrNull {it.id==shift.workplaceId}?.name.orEmpty())}}}},confirmButton={TextButton(onClick={pickedShifts=emptyList()}){Text(neoLabel("إغلاق","Close"))}})
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
    if(detailMode>0) {
        val roomy = detailMode==2
        Surface(
            modifier.alpha(if(muted) .38f else 1f)
                .shadow(if(today||chosen) 3.dp else 1.dp,RoundedCornerShape(9.dp))
                .combinedClickable(onClick=onClick,onLongClick=onLongClick)
                .semantics { contentDescription=description;selected=chosen },
            color=c.surface.copy(alpha=p.cardOpacity),
            shape=RoundedCornerShape(9.dp),
            border=BorderStroke(if(chosen||today||painting) 2.dp else .5.dp,
                if(chosen) c.secondary else if(today) neoHexColor(p.todayColor) else c.outlineVariant)
        ) {
            Column(Modifier.fillMaxSize().padding(if(roomy) 12.dp else 3.dp),
                verticalArrangement=Arrangement.spacedBy(if(roomy) 10.dp else 3.dp)) {
                Text(listOfNotNull(weekdayLabel,date.dayOfMonth.toString()).joinToString(" • "),
                    style=if(roomy) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                    fontWeight=FontWeight.Bold,maxLines=1)
                val cardText=if(roomy) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.labelSmall
                val shiftCards: @Composable () -> Unit = {
                    segments.take(if(roomy) 8 else 3).forEach {segment ->
                    val place=workplaces.firstOrNull {it.id==segment.workplaceId}
                    val shiftHex=place?.shiftColors?.get(segment.kind.name) ?: place?.colorHex ?: "#8BBACB"
                    val shade=neoHexColor(shiftHex)
                    Surface(shape=RoundedCornerShape(6.dp),color=shade,
                        modifier=(if(weekRowMode) Modifier.width(when(segment.kind) { ShiftKind.FULL_DAY -> 180.dp; ShiftKind.LONG, ShiftKind.NIGHT -> 145.dp; ShiftKind.MORNING, ShiftKind.EVENING -> 110.dp; else -> 100.dp }) else Modifier.fillMaxWidth()).then(if(onOpenShift!=null) Modifier.clickable {onOpenShift(segment)} else Modifier)) {
                        Text(neoShiftLabel(segment.kind)+(if(roomy && place!=null) " • "+place.name else ""),
                            modifier=Modifier.padding(if(roomy) 12.dp else 4.dp),
                            style=cardText,maxLines=2,overflow=TextOverflow.Ellipsis,
                            color=neoInkOn(shade))
                    }
                    }
                }
                if(weekRowMode) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {shiftCards()} else shiftCards()
                val noteCards: @Composable () -> Unit = {
                    notes.take(if(roomy) 8 else 2).forEach {note ->
                    Surface(shape=RoundedCornerShape(6.dp),color=c.secondaryContainer,
                        modifier=(if(weekRowMode) Modifier.widthIn(min=110.dp,max=220.dp) else Modifier.fillMaxWidth()).then(if(onOpenNote!=null) Modifier.clickable {onOpenNote(note)} else Modifier)) {
                        Text(note.title.ifBlank {neoLabel("ملاحظة","Note")},Modifier.padding(if(roomy) 12.dp else 4.dp),
                            style=cardText,maxLines=if(roomy) 3 else 2,overflow=TextOverflow.Ellipsis)
                    }
                    }
                }
                if(weekRowMode) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {noteCards()} else noteCards()
                if(tasks>0) Surface(shape=RoundedCornerShape(6.dp),color=c.tertiaryContainer,
                    modifier=Modifier.fillMaxWidth().then(if(onOpenTasks!=null) Modifier.clickable {onOpenTasks()} else Modifier)) {
                    Text(neoLabel("المهام: $tasks","Tasks: $tasks"),Modifier.padding(if(roomy) 12.dp else 4.dp),style=cardText)
                }
                if(events>0) Surface(shape=RoundedCornerShape(6.dp),color=c.surfaceVariant,
                    modifier=Modifier.fillMaxWidth().then(if(onOpenEvents!=null) Modifier.clickable {onOpenEvents()} else Modifier)) {
                    Text(neoLabel("المواعيد: $events","Events: $events"),Modifier.padding(if(roomy) 12.dp else 4.dp),style=cardText)
                }
            }
        }
        return
    }
    Surface(
        modifier
            .alpha(if (muted) .38f else 1f)
            .shadow(if (today || chosen) 3.dp else 1.dp, RoundedCornerShape(9.dp), clip = false)
            .clip(RoundedCornerShape(9.dp))
            .semantics {
                contentDescription = description
                selected = chosen
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = (if (muted) c.surfaceContainerLowest else c.surface).copy(alpha = p.cardOpacity),
        shape = RoundedCornerShape(9.dp),
        border =
            BorderStroke(
                if (today || chosen || painting) 2.dp else 0.5.dp,
                when {
                    painting -> c.tertiary
                    chosen -> c.secondary
                    today -> neoHexColor(p.todayColor)
                    else -> c.outlineVariant
                },
            ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(c.surface, androidx.compose.ui.graphics.lerp(c.surface, c.background, .4f)))
        )) {
            // Keep two-digit dates legible in a fixed seven-column month even at 200% text.
            // Full-size text remains available in day details; cells never grow the grid.
            val calendarFontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            // Fallback fonts can lay out two-digit dates wider than the metrics
            // returned by TextMeasurer under large accessibility font scaling.
            // Reserve a safety factor in 320dp / 7-column calendars, without
            // changing the fixed cell or grid height.
            val dateScale = minOf(
                (maxWidth - 6.dp).coerceAtLeast(1.dp) / measured.dateWidth.coerceAtLeast(1.dp),
                (maxHeight - 6.dp).coerceAtLeast(1.dp) / measured.dateHeight.coerceAtLeast(1.dp),
                if (calendarFontScale >= 1.8f && maxWidth < 48.dp) .72f else 1f,
            ).coerceAtLeast(.25f)
            val compactCell = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 2f &&
                maxWidth < 48.dp && maxHeight < measured.dateHeight + measured.captionHeight + 12.dp
            Neo4ShiftFill(
                segments,
                workplaces,
                Modifier.fillMaxSize(),
                appearance = if (p.showShiftPaint) p else p.copy(shiftPaints = emptyMap()),
                onSegments=if(onOpenShift==null) null else {items->if(items.size==1) onOpenShift(items.first()) else pickedShifts=items},onEmpty=onClick,
            )
            NeoTodayDecoration(today, date == LocalDate.now().plusDays(1), p)
            Text(
                listOfNotNull(weekdayLabel, date.dayOfMonth.toString()).joinToString(" • "),
                modifier =
                    Modifier.align(Alignment.TopStart).clickable(onClick=onClick)
                        // Give the Arabic and fallback font the full measured
                        // cell width. wrapContentWidth previously compressed
                        // the layout to 20px while intrinsic glyph width was
                        // 37px, even after scaling, clipping two-digit dates.
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .then(if (segments.any { it.kind != ShiftKind.OFF })
                            Modifier.background(c.surface.copy(alpha = .94f), RoundedCornerShape(3.dp))
                            else Modifier)
                        .padding(vertical = 1.dp)
                        .semantics { this[NeoDateFitInfo] = "cell=$maxWidth/$maxHeight metric=${measured.dateWidth}/${measured.dateHeight} scale=$dateScale" },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize * dateScale,
                    lineHeight = 16.sp * dateScale,
                ),
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Bold,
                color = c.onSurface,
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
                                            .then(if (onOpenNote != null) Modifier.clickable { onOpenNote(note) } else Modifier)
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
                    if(p.showShiftNames) segments.filter {it.kind!=ShiftKind.OFF}.take(2).forEach {segment ->
                        Text(workplaces.firstOrNull {it.id==segment.workplaceId}?.shiftNames?.get(segment.kind.name) ?: neoShiftLabel(segment.kind),maxLines=1,overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.labelSmall,color=c.onSurface,
                            modifier=Modifier.background(c.surface.copy(alpha=.9f),RoundedCornerShape(3.dp)).then(if(onOpenShift!=null) Modifier.clickable {onOpenShift(segment)} else Modifier))
                    }

                }
            if (compactCell && notes.isNotEmpty() && (p.showNotes || p.showNoteIcons))
                Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(4.dp).background(c.secondary, CircleShape))
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
                                Modifier.then(if(onOpenTasks!=null) Modifier.clickable {onOpenTasks()} else Modifier).background(
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
                                Modifier.then(if(onOpenEvents!=null) Modifier.clickable {onOpenEvents()} else Modifier).background(
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


