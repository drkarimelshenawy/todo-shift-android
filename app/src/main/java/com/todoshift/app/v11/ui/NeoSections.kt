package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun NeoShiftTimeline(db: V11Database, workplaces: List<Workplace>, month: YearMonth,
    onMonthChange: (YearMonth) -> Unit, onOpenDay: (LocalDate, String) -> Unit) {
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    val now = neoNow()
    var workplace by remember { mutableStateOf<String?>(null) }
    var filtersExpanded by remember { mutableStateOf(false) }
    var from by remember(month) { mutableStateOf(month.atDay(1).toString()) }
    var to by remember(month) { mutableStateOf(month.atEndOfMonth().toString()) }
    var limit by remember(month, p.timelineFilter, workplace, from, to) { mutableIntStateOf(20) }
    val all = remember(db.days) { neoShiftOccurrences(db) }
    val validDates = runCatching { LocalDate.parse(from) <= LocalDate.parse(to) }.getOrDefault(false)
    val filtered = remember(all, p.timelineFilter, workplace, from, to, now) { all.filter { item ->
        (workplace == null || item.segment.workplaceId == workplace) && item.entry.date >= from && item.entry.date <= to &&
            when (p.timelineFilter) {
                NeoTimelineFilter.ALL -> true
                NeoTimelineFilter.CURRENT -> now >= item.start && now < item.end
                NeoTimelineFilter.UPCOMING -> item.start > now
                NeoTimelineFilter.PAST -> item.end <= now
            }
    } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onMonthChange(month.minusMonths(1)) }) { Text(neoLabel("السابق", "Previous")) }
            Text(month.toString(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { onMonthChange(month.plusMonths(1)) }) { Text(neoLabel("التالي", "Next")) }
            TextButton(onClick = { filtersExpanded = !filtersExpanded }) { Text(neoLabel("فلاتر", "Filters")) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoTimelineFilter.entries.forEach { filter ->
                FilterChip(selected = filter == p.timelineFilter, onClick = { update(p.copy(timelineFilter = filter)) }, label = { Text(neoEnumLabel(filter)) })
            }
        }
        if (filtersExpanded) {
        NeoChoice(neoLabel("مكان العمل", "Workplace"), listOf<String?>(null) + workplaces.map { it.id }, workplace,
            { id -> workplaces.firstOrNull { it.id == id }?.name ?: neoLabel("جميع الأماكن", "All workplaces") }, { workplace = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(from, { from = it.take(10) }, label = { Text(neoLabel("من YYYY-MM-DD", "From YYYY-MM-DD")) }, singleLine = true, isError = !validDates, modifier = Modifier.weight(1f))
            OutlinedTextField(to, { to = it.take(10) }, label = { Text(neoLabel("إلى YYYY-MM-DD", "To YYYY-MM-DD")) }, singleLine = true, isError = !validDates, modifier = Modifier.weight(1f))
        }
            TextButton(onClick = { workplace = null; from = month.atDay(1).toString(); to = month.atEndOfMonth().toString() }) { Text(neoLabel("إعادة الفلاتر", "Reset filters")) }
        }
        if (!validDates) Text(neoLabel("أدخل نطاق تاريخ صحيح", "Enter a valid date range"), color = MaterialTheme.colorScheme.error)
        else if (filtered.isEmpty()) NeoEmptyState(neoLabel("لا توجد شيفتات مطابقة", "No matching shifts"), neoLabel("غيّر الفلتر أو أضف شيفتًا من التقويم", "Change filters or add a shift from the calendar"))
        else filtered.take(limit).forEach { item ->
            Card(onClick = { onOpenDay(item.start.toLocalDate(), item.entry.calendarId) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(neoSpacing()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val status = when {
                        now >= item.start && now < item.end -> neoLabel("جارٍ الآن", "In progress")
                        item.start > now -> neoLabel("قادم", "Upcoming")
                        else -> neoLabel("انتهى", "Finished")
                    }
                    Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    NeoShiftSummary(item, workplaces)
                    Text(db.calendars.firstOrNull { it.id == item.entry.calendarId }?.name.orEmpty(), style = MaterialTheme.typography.labelSmall)
                    if (neoHasConflict(item, all)) Text(neoLabel("تعارض زمني • راجع التفاصيل", "Time conflict • review details"), color = MaterialTheme.colorScheme.error)
                    if (p.showDescriptions && p.showShiftNotes && item.segment.note.isNotBlank()) Text(item.segment.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (validDates && filtered.size > limit) TextButton(onClick = { limit += 20 }) { Text(neoLabel("عرض المزيد", "Show more")) }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
    }
}

@Composable
fun NeoFinanceOverview(result: MonthFinanceV11, visible: Boolean) {
    val en = LocalNeoEnglish.current
    val p = LocalNeoAppearance.current
    fun amount(value: Double): String = if (visible) "\u2066" + String.format(Locale.US, "%,.2f", value) + "\u2069" + (if (en) " EGP" else " ج.م") else "••••••"
    val metrics = listOf(neoLabel("المستحق", "Expected") to result.expected,
        neoLabel("المستلم", "Received") to result.received, neoLabel("المتبقي", "Remaining") to result.difference)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.forEachIndexed { index, metric ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(neoSpacing()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(metric.first, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(amount(metric.second), style = if(index == 2) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        color = if (index == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(),
                        // Amounts retain a stable reading direction in RTL.
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                }
            }
        }
        if (p.showFinanceChart && visible && result.expected > 0.0) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(neoSpacing()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(neoLabel("تقدم التحصيل", "Collection progress"), style = MaterialTheme.typography.titleMedium)
                    val progress = (result.received / result.expected).coerceIn(0.0, 1.0).toFloat()
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(neoLabel("تم تحصيل ${(progress * 100).toInt()}% من المستحق", "${(progress * 100).toInt()}% of expected income collected"))
                    Text(neoLabel("التسويات: ", "Settlements: ") + amount(result.settlements), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun NeoTaskSearch(query: String, onQueryChange: (String) -> Unit, priority: TaskPriorityV11?, onPriority: (TaskPriorityV11?) -> Unit,
    calendarId: String?, onCalendar: (String?) -> Unit, db: V11Database) {
    var filtersOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(query, onQueryChange, label = { Text(neoLabel("ابحث في المهام", "Search tasks")) }, singleLine = true,
            modifier = Modifier.fillMaxWidth(), trailingIcon = {
                TextButton(onClick = { filtersOpen = !filtersOpen }) { Text(neoLabel("فلاتر", "Filters")) }
            })
        if (filtersOpen) {
            NeoChoice(neoLabel("الأولوية", "Priority"), listOf<TaskPriorityV11?>(null) + TaskPriorityV11.entries, priority,
                { neoPriorityLabel(it) }, onPriority)
            NeoChoice(neoLabel("التصنيف حسب التقويم", "Category by calendar"), listOf<String?>(null) + db.calendars.map { it.id }, calendarId,
                { id -> db.calendars.firstOrNull { it.id == id }?.name ?: neoLabel("كل التقاويم", "All calendars") }, onCalendar)
        }
    }
}

@Composable
fun neoPriorityLabel(priority: TaskPriorityV11?): String = when(priority) {
    null -> neoLabel("كل الأولويات", "All priorities")
    TaskPriorityV11.LOW -> neoLabel("منخفضة", "Low")
    TaskPriorityV11.NORMAL -> neoLabel("عادية", "Normal")
    TaskPriorityV11.HIGH -> neoLabel("عالية", "High")
    TaskPriorityV11.URGENT -> neoLabel("عاجلة", "Urgent")
}
