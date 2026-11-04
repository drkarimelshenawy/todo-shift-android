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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NeoCalendarArea(
    month: YearMonth, db: V11Database, workplaces: List<Workplace>, visibleCalendarIds: Set<String>,
    selectedDate: LocalDate, paintMode: Boolean, paintDates: Set<LocalDate>,
    onSelectedDate: (LocalDate) -> Unit, onMonthChange: (YearMonth) -> Unit, onToday: () -> Unit,
    onOpenDay: () -> Unit, modifier: Modifier = Modifier
) {
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    val en = LocalNeoEnglish.current
    val locale = if (en) Locale.ENGLISH else Locale.forLanguageTag("ar")
    val entries = remember(db.days, visibleCalendarIds) { db.days.filter { it.calendarId in visibleCalendarIds }.groupBy { it.date } }
    val tasks = remember(db.tasks, visibleCalendarIds) { db.tasks.filter { it.status != TaskStatusV11.COMPLETED && (it.calendarId == null || it.calendarId in visibleCalendarIds) }.groupBy { it.date } }
    val events = remember(db.dayItems, visibleCalendarIds) { db.dayItems.filter { !it.completed && (it.calendarId == null || it.calendarId in visibleCalendarIds) }.groupBy { it.date } }
    val shownView = if (paintMode) NeoCalendarView.MONTH else p.calendarView
    fun move(direction: Long) {
        when (shownView) {
            NeoCalendarView.MONTH -> {
                val next = month.plusMonths(direction)
                onMonthChange(next)
                if (!paintMode) onSelectedDate(next.atDay(minOf(selectedDate.dayOfMonth, next.lengthOfMonth())))
            }
            else -> {
                val date = selectedDate.plusDays(if (shownView == NeoCalendarView.WEEK) direction * 7 else direction)
                onMonthChange(YearMonth.from(date)); onSelectedDate(date)
            }
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { move(-1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, neoLabel("السابق", "Previous")) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(month.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), style = MaterialTheme.typography.titleLarge)
            }
            IconButton(onClick = { move(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, neoLabel("التالي", "Next")) }
            FilledTonalButton(onClick = onToday) { Text(neoLabel("اليوم", "Today")) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoCalendarView.entries.forEach { view ->
                FilterChip(selected = shownView == view, enabled = !paintMode, onClick = { update(p.copy(calendarView = view)) }, label = { Text(neoEnumLabel(view)) })
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            db.calendars.filter { it.id in visibleCalendarIds }.sortedBy { it.order }.forEach { calendar ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(neoHexColor(calendar.colorHex)))
                    Text(calendar.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        var swipeDistance by remember { mutableFloatStateOf(0f) }
        val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
        AnimatedContent(modifier = Modifier.pointerInput(month, shownView, selectedDate, paintMode, rtl) {
            detectHorizontalDragGestures(onDragStart = { swipeDistance = 0f }, onHorizontalDrag = { _, amount -> swipeDistance += amount },
                onDragEnd = {
                    if (kotlin.math.abs(swipeDistance) > 90f) move(if ((swipeDistance > 0f) == rtl) 1 else -1)
                    swipeDistance = 0f
                }, onDragCancel = { swipeDistance = 0f })
        }, targetState = Triple(month, shownView, if (shownView == NeoCalendarView.MONTH) month.atDay(1) else selectedDate),
            transitionSpec = { fadeIn(tween(p.motionMillis)) togetherWith fadeOut(tween(p.motionMillis)) }, label = "neoCalendar") { target ->
            val targetMonth = target.first
            val view = target.second
            val date = target.third
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (view != NeoCalendarView.DAY) {
                    val start = if (view == NeoCalendarView.MONTH) targetMonth.atDay(1).let { it.minusDays(((it.dayOfWeek.value + 1) % 7).toLong()) }
                        else date.minusDays(((date.dayOfWeek.value + 1) % 7).toLong())
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { index ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(start.plusDays(index.toLong()).format(DateTimeFormatter.ofPattern("EEEEE", locale)),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    val rows = if (view == NeoCalendarView.WEEK) 1 else (((targetMonth.atDay(1).dayOfWeek.value + 1) % 7 + targetMonth.lengthOfMonth() + 6) / 7)
                    repeat(rows) { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(7) { col ->
                                val cellDate = start.plusDays((row * 7 + col).toLong())
                                NeoCalendarCell(cellDate, entries[cellDate.toString()].orEmpty(),
                                    taskCount = tasks[cellDate.toString()].orEmpty().size,
                                    eventCount = events[cellDate.toString()].orEmpty().size,
                                    db = db, workplaces = workplaces, chosen = cellDate == selectedDate,
                                    painting = cellDate in paintDates, muted = YearMonth.from(cellDate) != targetMonth,
                                    modifier = Modifier.weight(1f), onClick = {
                                        onSelectedDate(cellDate)
                                        if (!paintMode && YearMonth.from(cellDate) != month) onMonthChange(YearMonth.from(cellDate))
                                    })
                            }
                        }
                    }
                }
            }
        }
        if (!paintMode) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(neoSpacing()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)), style = MaterialTheme.typography.titleMedium)
                    val dayEntries = entries[selectedDate.toString()].orEmpty()
                    val dayTasks = tasks[selectedDate.toString()].orEmpty()
                    val dayEvents = events[selectedDate.toString()].orEmpty()
                    if (dayEntries.all { it.segments.isEmpty() } && dayTasks.isEmpty() && dayEvents.isEmpty()) {
                        Text(neoLabel("يوم هادئ، أضف ما يهمك", "A clear day. Add what matters."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val occurrences = remember(db.days, visibleCalendarIds) { neoShiftOccurrences(db, visibleCalendarIds) }
                    occurrences.filter { it.entry.date == selectedDate.toString() }.forEach { item ->
                        NeoShiftSummary(item, workplaces)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    dayEntries.flatMap { it.segments }.filter { it.kind == ShiftKind.OFF }.forEach { Text(neoShiftLabel(it.kind)) }
                    dayTasks.forEach { task -> Text(neoLabel("مهمة: ", "Task: ") + task.title, style = MaterialTheme.typography.bodyMedium) }
                    dayEvents.forEach { event -> Text(event.title, style = MaterialTheme.typography.bodyMedium) }
                    FilledTonalButton(onClick = onOpenDay, modifier = Modifier.fillMaxWidth()) { Text(neoLabel("إضافة وتعديل تفاصيل اليوم", "Add or edit day details")) }
                }
            }
        }
    }
}

@Composable
private fun NeoCalendarCell(date: LocalDate, entries: List<DayEntryV11>, taskCount: Int, eventCount: Int,
    db: V11Database, workplaces: List<Workplace>, chosen: Boolean, painting: Boolean, muted: Boolean,
    modifier: Modifier, onClick: () -> Unit) {
    val p = LocalNeoAppearance.current
    val c = MaterialTheme.colorScheme
    val today = date == LocalDate.now()
    val segments = entries.flatMap { it.segments }
    val description = date.toString() + neoLabel("، ${segments.size} شيفت، $taskCount مهمة، $eventCount موعد", ", ${segments.size} shifts, $taskCount tasks, $eventCount events")
    val height = when(p.calendarDensity) { NeoDensity.COMPACT -> 58; NeoDensity.BALANCED -> 72; NeoDensity.COMFORTABLE -> 88 }
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    Surface(modifier.height((height * fontScale).dp).semantics { contentDescription = description; selected = chosen }
        .clickable(onClick = onClick),
        shape = RoundedCornerShape((p.cornerRadius / 2).dp),
        color = if (chosen || painting) c.primaryContainer else if (today) c.surfaceVariant else c.surface,
        border = BorderStroke(if (chosen || today || painting) 2.dp else 1.dp, if (chosen || today || painting) c.primary else c.outlineVariant)) {
        Column(Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelLarge,
                color = if (today) c.primary else c.onSurface.copy(alpha = if (muted) 0.55f else 1f), fontWeight = FontWeight.Bold)
            segments.take(if (p.calendarDensity == NeoDensity.COMPACT) 2 else 3).forEach { segment ->
                val color = if (segment.kind == ShiftKind.OFF) c.outline else workplaces.firstOrNull { it.id == segment.workplaceId }?.let { neoHexColor(it.colorHex) } ?: c.primary
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(color))
            }
            val maxBars = if (p.calendarDensity == NeoDensity.COMPACT) 2 else 3
            if (segments.size > maxBars) Text("+${segments.size - maxBars}", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (taskCount > 0) Box(Modifier.size(5.dp).clip(CircleShape).background(c.primary))
                if (eventCount > 0) Box(Modifier.size(5.dp).clip(CircleShape).background(c.secondary))
            }
        }
    }
}

fun neoHexColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFF80BFFF))
