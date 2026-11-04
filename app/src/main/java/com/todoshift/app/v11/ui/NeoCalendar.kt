package com.todoshift.app.v11.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoCalendarArea(
    month: YearMonth, db: V11Database, workplaces: List<Workplace>, visibleCalendarIds: Set<String>,
    selectedDate: LocalDate, paintMode: Boolean, paintDates: Set<LocalDate>,
    onSelectedDate: (LocalDate) -> Unit, onMonthChange: (YearMonth) -> Unit, onToday: () -> Unit,
    onOpenDay: () -> Unit, modifier: Modifier = Modifier
) {
    val p = LocalNeoAppearance.current; val update = LocalNeoOnChange.current
    val locale = if (LocalNeoEnglish.current) Locale.ENGLISH else Locale.forLanguageTag("ar")
    val entries = remember(db.days, visibleCalendarIds) { db.days.filter { it.calendarId in visibleCalendarIds }.groupBy { it.date } }
    val tasks = remember(db.tasks, visibleCalendarIds) { db.tasks.filter { it.status != TaskStatusV11.COMPLETED && (it.calendarId == null || it.calendarId in visibleCalendarIds) }.groupBy { it.date } }
    val events = remember(db.dayItems, visibleCalendarIds) { db.dayItems.filter { !it.completed && (it.calendarId == null || it.calendarId in visibleCalendarIds) }.groupBy { it.date } }
    var sheetDate by rememberSaveable { mutableStateOf<String?>(null) }
    val view = if (paintMode) NeoCalendarView.MONTH else p.calendarView
    fun move(step: Long) {
        val date = when (view) {
            NeoCalendarView.MONTH -> month.plusMonths(step).let { it.atDay(minOf(selectedDate.dayOfMonth, it.lengthOfMonth())) }
            NeoCalendarView.WEEK -> selectedDate.plusDays(step * 7)
            NeoCalendarView.DAY -> selectedDate.plusDays(step)
        }
        onMonthChange(YearMonth.from(date)); if (!paintMode) onSelectedDate(date)
    }
    Column(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val availableWidth = maxWidth
            val toolbarWidth = maxOf(availableWidth, 240.dp + 100.dp * fontScale)
            Row(Modifier.horizontalScroll(rememberScrollState()).width(toolbarWidth).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { move(-1) }, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, neoLabel("السابق", "Previous")) }
                Text(month.atDay(1).format(DateTimeFormatter.ofPattern(if (availableWidth < 420.dp) "MM/yyyy" else "MMMM yyyy", locale)),
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                IconButton(onClick = { move(1) }, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, neoLabel("التالي", "Next")) }
                NeoCalendarView.entries.forEach { item ->
                    TextButton(enabled = !paintMode, onClick = { update(p.copy(calendarView = item)) },
                        contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.widthIn(min = 36.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = if (view == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                        Text(neoEnumLabel(item), style = MaterialTheme.typography.labelMedium, fontWeight = if (view == item) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    }
                }
            }
        }
        var distance by remember { mutableFloatStateOf(0f) }
        val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
        AnimatedContent(targetState = Triple(month, view, if (view == NeoCalendarView.MONTH) month.atDay(1) else selectedDate),
            modifier = Modifier.weight(1f).pointerInput(month, view, selectedDate, paintMode, rtl) {
                detectHorizontalDragGestures(onDragStart = { distance = 0f }, onHorizontalDrag = { _, amount -> distance += amount },
                    onDragEnd = { if (kotlin.math.abs(distance) > 90f) move(if ((distance > 0f) == rtl) 1 else -1); distance = 0f }, onDragCancel = { distance = 0f })
            }, transitionSpec = { fadeIn(tween(p.motionMillis)) togetherWith fadeOut(tween(p.motionMillis)) }, label = "neo4Calendar") { state ->
            val shownMonth = state.first; val shownView = state.second; val date = state.third
            val start = if (shownView == NeoCalendarView.MONTH) shownMonth.atDay(1).let { it.minusDays(((it.dayOfWeek.value + 1) % 7).toLong()) }
                else if (shownView == NeoCalendarView.WEEK) date.minusDays(((date.dayOfWeek.value + 1) % 7).toLong()) else date
            val columns = if (shownView == NeoCalendarView.DAY) 1 else 7
            val rows = if (shownView != NeoCalendarView.MONTH) 1 else maxOf(5, (((shownMonth.atDay(1).dayOfWeek.value + 1) % 7 + shownMonth.lengthOfMonth() + 6) / 7))
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val headerHeight = 26.dp
                val rowHeight = ((maxHeight - headerHeight) / rows).coerceAtLeast(48.dp)
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                val contentWidth = if (columns == 1) maxWidth else maxOf(maxWidth, (20.dp * fontScale + 8.dp) * columns + 18.dp)
                Column(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                Column(Modifier.width(contentWidth).verticalScroll(rememberScrollState())) {
                    Row(Modifier.fillMaxWidth().height(headerHeight)) {
                        repeat(columns) { col -> Text(start.plusDays(col.toLong()).format(DateTimeFormatter.ofPattern(if (columns == 1) "EEEE" else "EEEEE", locale)),
                            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) }
                    }
                    repeat(rows) { row ->
                        Row(Modifier.fillMaxWidth().height(rowHeight), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            repeat(columns) { col ->
                                val cellDate = start.plusDays((row * columns + col).toLong()); val key = cellDate.toString()
                                NeoCalendarCell(cellDate, entries[key].orEmpty(), tasks[key].orEmpty().size, events[key].orEmpty().size,
                                    workplaces, cellDate == selectedDate, cellDate in paintDates,
                                    YearMonth.from(cellDate) != shownMonth, Modifier.weight(1f).fillMaxHeight().padding(vertical = 1.5.dp)) {
                                    onSelectedDate(cellDate)
                                    if (!paintMode) { sheetDate = key; if (YearMonth.from(cellDate) != month) onMonthChange(YearMonth.from(cellDate)) }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
    sheetDate?.let { key ->
        val date = runCatching { LocalDate.parse(key) }.getOrNull()
        if (date != null && !paintMode) ModalBottomSheet(onDismissRequest = { sheetDate = null }) {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), style = MaterialTheme.typography.titleMedium)
                val dayEntries = entries[key].orEmpty(); val dayTasks = tasks[key].orEmpty(); val dayEvents = events[key].orEmpty()
                val shifts = remember(db.days, key) { neoShiftOccurrences(db, visibleCalendarIds).filter { it.entry.date == key } }
                if (dayEntries.all { it.segments.isEmpty() } && dayTasks.isEmpty() && dayEvents.isEmpty()) Text(neoLabel("لا توجد عناصر في هذا اليوم", "Nothing scheduled for this day"))
                shifts.forEach { NeoShiftSummary(it, workplaces); HorizontalDivider() }
                dayEntries.flatMap { it.segments }.filter { it.kind == ShiftKind.OFF }.forEach { Text(neoShiftLabel(it.kind)) }
                dayTasks.forEach { Text(neoLabel("مهمة: ", "Task: ") + it.title) }
                dayEvents.forEach { Text(it.title) }
                if (p.showCalendarLegend) db.calendars.filter { it.id in visibleCalendarIds }.forEach { calendar ->
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(neoHexColor(calendar.colorHex), CircleShape)); Spacer(Modifier.width(7.dp)); Text(calendar.name, style = MaterialTheme.typography.labelSmall) }
                }
                Button(onClick = { sheetDate = null; onOpenDay() }, modifier = Modifier.fillMaxWidth()) { Text(neoLabel("إضافة أو تعديل", "Add or edit")) }
                Spacer(Modifier.navigationBarsPadding().height(12.dp))
            }
        }
    }
}

@Composable
private fun NeoCalendarCell(date: LocalDate, entries: List<DayEntryV11>, tasks: Int, events: Int, workplaces: List<Workplace>,
    chosen: Boolean, painting: Boolean, muted: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNeoAppearance.current; val c = MaterialTheme.colorScheme
    val today = date == LocalDate.now(); val segments = remember(entries) { entries.flatMap { it.segments } }
    val description = date.toString() + neoLabel("، ${segments.size} شيفت، $tasks مهمة، $events موعد", ", ${segments.size} shifts, $tasks tasks, $events events")
    Surface(modifier.clip(RoundedCornerShape((p.cornerRadius / 2).dp)).semantics { contentDescription = description; selected = chosen }.clickable(onClick = onClick),
        color = c.surface.copy(alpha = p.cardOpacity), shape = RoundedCornerShape((p.cornerRadius / 2).dp),
        border = BorderStroke(if (today || chosen || painting) 2.dp else 0.5.dp, if (today || chosen || painting) c.primary else c.outlineVariant)) {
        Box(Modifier.fillMaxSize()) {
            if (p.showShiftPaint) Neo4ShiftFill(segments, workplaces, Modifier.fillMaxSize())
            Text(date.dayOfMonth.toString(), modifier = Modifier.align(Alignment.TopStart).padding(2.dp)
                .background(c.surface.copy(alpha = 0.90f), RoundedCornerShape(4.dp)).padding(horizontal = 2.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                color = (if (today) c.primary else c.onSurface).copy(alpha = if (muted) 0.6f else 1f))
            if (p.showSecondaryIndicators) Row(Modifier.align(Alignment.BottomStart).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (tasks > 0) Box(Modifier.size(5.dp).background(c.onSurface, CircleShape))
                if (events > 0) Box(Modifier.size(5.dp).background(c.secondary, CircleShape))
                if (segments.size > 1) Text("${segments.size}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.background(c.surface.copy(alpha = 0.9f)))
            }
        }
    }
}

fun neoHexColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF80BFFF))
