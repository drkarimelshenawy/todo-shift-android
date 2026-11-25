package com.exmore.app.v11.ui

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
import com.exmore.app.data.Workplace
import com.exmore.app.v11.*
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
            listOf("Morning", "مورننج", "×99", "☑99", "◷99", "📌").maxOf {
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
    val frameColors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(24.dp), clip = false)
            .border(1.dp, frameColors.outlineVariant, RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(frameColors.surfaceContainerLow, frameColors.surfaceContainer)
                ),
                RoundedCornerShape(24.dp),
            )
            .padding(5.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // Month controls keep the previous month at the visual right in Arabic,
            // and at the visual left in English, regardless of the calendar grid.
            val monthBarDirection = if (LocalNeoEnglish.current)
                androidx.compose.ui.unit.LayoutDirection.Ltr
            else androidx.compose.ui.unit.LayoutDirection.Rtl
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides monthBarDirection
            ) {
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
                // Grid spacing comes out of the fixed height. No extra scrolling.
                val gridGap = if (shownView == NeoCalendarView.MONTH) 3.dp else 2.dp
                val rowHeight = fittedCalendarRowHeight(
                    (maxHeight - gridGap * rows).coerceAtLeast(0.dp),
                    headerHeight,
                    rows,
                )
                Column(Modifier.fillMaxSize().clipToBounds()) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(gridGap),
                    ) {
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
                                    horizontalArrangement = Arrangement.spacedBy(if (premiumCashBook) 2.dp else 0.dp),
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
                                                    .padding(if (premiumCashBook) .5.dp else 0.dp)
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
                                                // Only the cell handles taps. A first tap selects; the
                                                // next tap opens details. Child badges must not intercept
                                                // that shared contract across month/week/day modes.
                                                onOpenNote = null,
                                                onOpenShift = null,
                                                onOpenTasks = null,
                                                onOpenEvents = null,
                                                onLongClick = {
                                                    tappedDate = null
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
                                                    if (secondTap) {
                                                        tappedDate = null
                                                        onOpenDay(cellDate)
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
                            dayEvents.isEmpty() &&
                            presentation[key]?.notes.orEmpty().isEmpty()
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

/**
 * Glyph-only day and note overlays choose ink for the shift band below.
 * Never paint a special date header: 24h/12h/6h coverage remains unchanged.
 */
internal fun neoCalendarOverlayInk(
    segments: List<ShiftSegmentV11>,
    workplaces: List<Workplace>,
    appearance: NeoAppearance,
    fraction: Float,
    fallback: Color,
): Color {
    val active = segments.filter { segment ->
        neo4Coverage(segment.kind)?.let { fraction >= it.top && fraction < it.bottom } == true
    }
    if (active.isEmpty()) return fallback
    val segment = active.first()
    val place = workplaces.firstOrNull { it.id == segment.workplaceId }
    val own = place?.shiftColors?.get(segment.kind.name)
    val style = appearance.shiftPaints[segment.kind] ?: NeoShiftPaint()
    val color = neoHexColor(
        if (own != null || style.mode == NeoPaintMode.INHERIT)
            own ?: place?.colorHex ?: "#8BBACB" else style.first
    )
    return neoInkOn(color)
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
            modifier.alpha(if (muted) {
                if (c.background.luminance() < .12f) .72f else .5f
            } else 1f)
                .shadow(if(today||chosen) 3.dp else 1.dp,RoundedCornerShape(9.dp))
                .combinedClickable(onClick=onClick,onLongClick=onLongClick)
                .semantics { contentDescription=description;selected=chosen },
            color=c.surface.copy(alpha=if (c.background.luminance()<.12f)
                p.cardOpacity.coerceAtLeast(.94f) else p.cardOpacity),
            shape=RoundedCornerShape(9.dp),
            border=BorderStroke(if(chosen||today||painting) 2.dp else .5.dp,
                if(chosen) c.secondary else if(today) neoHexColor(p.todayColor) else c.outlineVariant)
        ) {
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if(roomy) 12.dp else 3.dp),
                verticalArrangement=Arrangement.spacedBy(if(roomy) 10.dp else 3.dp)
            ) {
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
                            style=cardText,maxLines=if(weekRowMode||roomy) 2 else 1,overflow=TextOverflow.Ellipsis,
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
                            style=cardText,maxLines=if(roomy) 3 else if(weekRowMode) 2 else 1,overflow=TextOverflow.Ellipsis)
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
            .alpha(if (muted) {
                if (c.background.luminance() < .12f) .72f else .5f
            } else 1f)
            .shadow(if (today || chosen) 3.dp else 1.dp, RoundedCornerShape(9.dp), clip = false)
            .clip(RoundedCornerShape(9.dp))
            .semantics {
                contentDescription = description
                selected = chosen
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = (if (muted) c.surfaceContainerLowest else c.surface).copy(
            alpha = if (c.background.luminance() < .12f)
                p.cardOpacity.coerceAtLeast(.94f) else p.cardOpacity
        ),
        shape = RoundedCornerShape(9.dp),
        border =
            BorderStroke(
                if (today || chosen || painting) 2.dp else 0.85.dp,
                when {
                    painting -> c.tertiary
                    chosen -> c.secondary
                    today -> neoHexColor(p.todayColor)
                    else -> c.outlineVariant
                },
            ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(c.surfaceContainerLow, c.surfaceContainer))
        )) {
            // Keep two-digit dates legible in a fixed seven-column month even at 200% text.
            // Full-size text remains available in day details; cells never grow the grid.
            val calendarFontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            // Fallback fonts can lay out two-digit dates wider than the metrics
            // returned by TextMeasurer under large accessibility font scaling.
            // Reserve a safety factor in 320dp / 7-column calendars, without
            // changing the fixed cell or grid height.
            // Divide tiny fixed-height month cells into independent date,
            // caption and indicator slots. On a 62dp cell the old 45dp date
            // + 40dp badge reservations left literally zero space for notes.
            // On narrow 5/6-week month grids, preserve a minimum caption
            // slot when notes exist. Date remains legible, but must not consume
            // the only line available for the note title.
            val hasVisibleNote = p.showNotes && notes.isNotEmpty()
            val dateSlot = minOf(
                scaledDateHeight,
                maxHeight * if (hasVisibleNote) .34f else .44f,
            ).coerceAtLeast(12.dp)
            val dateScale = minOf(
                (maxWidth - 6.dp).coerceAtLeast(1.dp) / measured.dateWidth.coerceAtLeast(1.dp),
                (dateSlot - 6.dp).coerceAtLeast(1.dp) / measured.dateHeight.coerceAtLeast(1.dp),
                if (calendarFontScale >= 1.8f && maxWidth < 48.dp) .72f else 1f,
            ).coerceAtLeast(.25f)
            val compactCell = androidx.compose.ui.platform.LocalDensity.current.fontScale >= 2f &&
                maxWidth < 48.dp && maxHeight < measured.dateHeight + measured.captionHeight + 12.dp
            // Draw text directly over the shift bands, with a glyph-only halo for contrast.
            // The day number, notes and badges must never reserve a painted date strip.
            val paintAppearance = if (p.showShiftPaint) p else p.copy(shiftPaints = emptyMap())
            val dateInk = neoCalendarOverlayInk(
                segments, workplaces, paintAppearance, .04f, c.onSurface
            )
            val firstNoteMarker = if (p.showNoteIcons)
                notes.firstOrNull()?.title?.let { title ->
                    listOf("📌", "★", "⚠").firstOrNull { title.startsWith(it) }
                } else null
            // Pinned marker has its own measured row BELOW the day number.
            // Corner placement overlapped the day date at large accessibility sizes.
            val markerRowHeight = if (firstNoteMarker != null && !compactCell)
                minOf(measured.captionHeight * .60f, maxHeight * .12f)
                    .coerceAtLeast(6.dp) else 0.dp
            val captionsStart = dateSlot + markerRowHeight + 1.dp
            val badgeReserved = if (p.showTaskIndicators && (tasks > 0 || events > 0))
                minOf(
                    measured.captionHeight + 2.dp,
                    maxHeight * if (hasVisibleNote) .18f else .22f,
                ) else 0.dp
            val badgeScale = if (badgeReserved > 0.dp)
                minOf(1f, badgeReserved / measured.captionHeight.coerceAtLeast(1.dp))
                    .coerceAtLeast(.35f)
            else 1f
            val captionBudget = (maxHeight - captionsStart - badgeReserved - 2.dp)
                .coerceAtLeast(0.dp)
            // Preserve a compact caption at large accessibility scales instead
            // of hiding it when it misses the nominal line height by a few dp.
            val baselineCaption = measured.captionHeight.coerceAtLeast(1.dp)
            // A month cell is a compact overview. Keep a single actual line
            // whenever at least 8dp remain; size it for that exact slot.
            val captionLines = if (captionBudget >= 7.dp)
                (captionBudget / (baselineCaption * .85f)).toInt()
                    .coerceIn(1, 3)
            else 0
            // Each caption is bounded to a distinct slot inside the fixed cell.
            val captionRowHeight = if (captionLines > 0)
                ((captionBudget - (captionLines - 1).dp) / captionLines)
                    .coerceAtLeast(1.dp) else 0.dp
            val captionScale = if (captionLines > 0)
                minOf(1f, (captionRowHeight - 2.dp).coerceAtLeast(1.dp) /
                    baselineCaption).coerceAtLeast(.28f)
            else 1f
            val captionPosition = ((captionsStart + measured.captionHeight / 2) /
                maxHeight.coerceAtLeast(1.dp)).coerceIn(0f, .99f)
            val captionInk = neoCalendarOverlayInk(
                segments, workplaces, paintAppearance, captionPosition, c.onSurface
            )
            fun shadowFor(ink: Color) = androidx.compose.ui.graphics.Shadow(
                color = if (ink.luminance() > .5f)
                    Color.Black.copy(alpha = .9f) else Color.White.copy(alpha = .85f),
                offset = androidx.compose.ui.geometry.Offset.Zero,
                blurRadius = 1.6f,
            )
            val overlayShadow = shadowFor(dateInk)
            val captionShadow = shadowFor(captionInk)
            Neo4ShiftFill(
                segments,
                workplaces,
                Modifier.fillMaxSize(),
                appearance = paintAppearance,
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
                        .fillMaxWidth().height(dateSlot).clipToBounds()
                        .padding(horizontal = 1.dp)
                        // Date remains transparent; shift paint stays full-height.
                        .semantics {
                            this[NeoDateFitInfo] = "cell=$maxWidth/$maxHeight date=${measured.dateHeight} slot=$dateSlot badge=$badgeReserved caption=${measured.captionHeight} budget=$captionBudget rows=$captionLines scale=$dateScale"
                        },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize * dateScale,
                    lineHeight = 16.sp * dateScale,
                    shadow = overlayShadow,
                ),
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Bold,
                color = dateInk,
            )
            val marker = firstNoteMarker
            if (marker != null && !compactCell)
                Text(
                    marker,
                    modifier = Modifier.align(Alignment.TopEnd)
                        .offset(y = dateSlot).padding(end = 2.dp)
                        .height(markerRowHeight).clipToBounds(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = MaterialTheme.typography.labelSmall.fontSize *
                            minOf(1f, markerRowHeight / baselineCaption),
                        lineHeight = 14.sp * minOf(1f, markerRowHeight / baselineCaption),
                        shadow = overlayShadow,
                    ),
                    color = dateInk,
                    maxLines = 1, softWrap = false,
                )
            if (!compactCell && captionLines > 0 &&
                ((p.showNotes && notes.isNotEmpty()) ||
                    ((p.showShiftNames || p.showWorkplaceNames) && segments.isNotEmpty())))
                Column(
                    Modifier.align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(start = 3.dp, end = 3.dp, top = captionsStart)
                        .height(captionBudget)
                        .clipToBounds(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    var usedLines = 0
                    if (p.showNotes && notes.isNotEmpty() && usedLines < captionLines) {
                        val note = notes.first()
                        val noteColor = neoHexColor(
                            noteMetadata.optJSONObject("noteColors")
                                ?.optString(note.id, "#D8AD55") ?: "#D8AD55"
                        )
                        val noteBorder = noteMetadata.optJSONObject("noteBorders")
                            ?.optBoolean(note.id, false) == true
                        Row(
                            Modifier.fillMaxWidth().height(captionRowHeight).clipToBounds(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (!noteBorder)
                                Box(Modifier.size(4.dp).background(noteColor, CircleShape))
                            Text(
                                note.title.removePrefix("📌 ").removePrefix("★ ")
                                    .removePrefix("⚠ "),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = 14.sp * captionScale,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * captionScale,
                                    shadow = captionShadow
                                ),
                                color = captionInk,
                                modifier = Modifier.weight(1f).height(captionRowHeight)
                                    .clipToBounds()
                                    .then(if (onOpenNote != null) Modifier.clickable {
                                        onOpenNote(note)
                                    } else Modifier)
                                    .semantics { contentDescription = note.title }
                                    .then(if (noteBorder) Modifier.border(
                                        1.dp, noteColor, RoundedCornerShape(3.dp)
                                    ) else Modifier),
                            )
                            if (notes.size > 1) Text(
                                "+${notes.size - 1}",
                                color = captionInk,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * captionScale,
                                    lineHeight = 14.sp * captionScale,
                                    shadow = captionShadow
                                ),
                                maxLines = 1,
                            )
                        }
                        usedLines++
                    }
                    if (p.showWorkplaceNames && segments.isNotEmpty() &&
                        usedLines < captionLines) {
                        val names = segments.mapNotNull { segment ->
                            workplaces.firstOrNull { it.id == segment.workplaceId }?.name
                        }.distinct().joinToString(" / ").ifEmpty {
                            if (segments.all { it.kind == ShiftKind.OFF })
                                neoLabel("راحة", "Off") else ""
                        }
                        if (names.isNotEmpty()) {
                            Text(
                                names, maxLines = 1, softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = 14.sp * captionScale,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * captionScale,
                                    shadow = captionShadow),
                                color = captionInk,
                                modifier = Modifier.height(captionRowHeight).clipToBounds(),
                            )
                            usedLines++
                        }
                    }
                    if (p.showShiftNames && usedLines < captionLines) {
                        segments.firstOrNull { it.kind != ShiftKind.OFF }?.let { segment ->
                            Text(
                                workplaces.firstOrNull { it.id == segment.workplaceId }
                                    ?.shiftNames?.get(segment.kind.name) ?: neoShiftLabel(segment.kind),
                                maxLines = 1, softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = 14.sp * captionScale,
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * captionScale,
                                    shadow = captionShadow),
                                color = captionInk,
                                modifier = Modifier.height(captionRowHeight).clipToBounds()
                                    .then(if (onOpenShift != null)
                                    Modifier.clickable { onOpenShift(segment) } else Modifier),
                            )
                        }
                    }
                }
            if (compactCell && notes.isNotEmpty() && (p.showNotes || p.showNoteIcons))
                Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(4.dp).background(c.secondary, CircleShape))
            if (compactCell && p.showTaskIndicators && (tasks > 0 || events > 0))
                Box(Modifier.align(Alignment.BottomStart).padding(3.dp).size(4.dp).background(c.primary, CircleShape))
            if (!compactCell && p.showTaskIndicators && (tasks > 0 || events > 0))
                Row(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth()
                        .height(badgeReserved).clipToBounds()
                        .padding(horizontal = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (tasks > 0) Text(
                        "☑${if (tasks > 99) "99+" else tasks}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * badgeScale,
                            lineHeight = 14.sp * badgeScale, shadow = captionShadow),
                        color = neoCalendarOverlayInk(segments, workplaces,
                            paintAppearance, .97f, c.onSurface),
                        maxLines = 1, softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).then(
                            if (onOpenTasks != null) Modifier.clickable { onOpenTasks() }
                            else Modifier
                        ),
                    )
                    if (events > 0) Text(
                        "◷${if (events > 99) "99+" else events}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = MaterialTheme.typography.labelSmall.fontSize * badgeScale,
                            lineHeight = 14.sp * badgeScale, shadow = captionShadow),
                        color = neoCalendarOverlayInk(segments, workplaces,
                            paintAppearance, .97f, c.onSurface),
                        maxLines = 1, softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).then(
                            if (onOpenEvents != null) Modifier.clickable { onOpenEvents() }
                            else Modifier
                        ),
                    )
                }
        }
    }
}

fun neoHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF80BFFF))


