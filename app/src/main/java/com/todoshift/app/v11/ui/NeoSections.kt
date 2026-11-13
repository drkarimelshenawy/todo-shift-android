package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

@Composable
fun NeoShiftTimeline(
    db: V11Database,
    workplaces: List<Workplace>,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onOpenDay: (LocalDate, String) -> Unit,
) {
    val positive = neoPositiveColor()
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    val now = neoNow()
    var workplace by rememberSaveable { mutableStateOf<String?>(null) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var from by rememberSaveable(month) { mutableStateOf(month.atDay(1).toString()) }
    var to by rememberSaveable(month) { mutableStateOf(month.atEndOfMonth().toString()) }
    var limit by remember(month, p.timelineFilter, workplace, from, to) { mutableIntStateOf(20) }
    val visibleIds =
        remember(db.calendars) { db.calendars.filter { it.visible }.map { it.id }.toSet() }
    val all = remember(db.days, visibleIds) { neoShiftOccurrences(db, visibleIds) }
    var kindFilter by rememberSaveable { mutableStateOf<ShiftKind?>(null) }
    val validDates =
        runCatching { LocalDate.parse(from) <= LocalDate.parse(to) }.getOrDefault(false)
    val filtered =
        remember(all, p.timelineFilter, workplace, kindFilter, from, to, now) {
            all.filter { item ->
                (kindFilter == null || item.segment.kind == kindFilter) &&
                    (workplace == null || item.segment.workplaceId == workplace) &&
                    item.entry.date >= from &&
                    item.entry.date <= to &&
                    neoScheduleMatchesPeriod(item, p.timelineFilter, now)
            }
        }
    val offRows =
        if (validDates && workplace == null && (kindFilter == null || kindFilter == ShiftKind.OFF))
            neoScheduleOffEntries(db, visibleIds, from, to, p.timelineFilter, now.toLocalDate())
        else emptyList()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    neoLabel("الشهر السابق", "Previous month"),
                )
            }
            Text(
                neoMonthTitle(month, LocalNeoEnglish.current),
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    neoLabel("الشهر التالي", "Next month"),
                )
            }
            IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                Icon(Icons.Rounded.FilterList, neoLabel("فلاتر", "Filters"))
            }
        }
        NeoChoice(
            neoLabel("فترة الورديات", "Shift period"),
            NeoTimelineFilter.entries,
            p.timelineFilter,
            { neoEnumLabel(it) },
            { update(p.copy(timelineFilter = it)) },
        )
        val monthRows =
            db.days.filter { it.calendarId in visibleIds && it.date.startsWith(month.toString()) }
        val workedDays =
            monthRows
                .filter { d -> d.segments.any { it.kind != ShiftKind.OFF } }
                .map { it.date }
                .distinct()
                .size
        val shiftCount = monthRows.sumOf { d -> d.segments.count { it.kind != ShiftKind.OFF } }
        val offCount = monthRows.sumOf { d -> d.segments.count { it.kind == ShiftKind.OFF } }
        Text(
            neoLabel(
                "$shiftCount شيفت • $workedDays يوم عمل • $offCount راحة",
                "$shiftCount shifts • $workedDays worked days • $offCount off",
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        if (filtersExpanded) {
            NeoChoice(
                neoLabel("نوع الشيفت", "Shift type"),
                listOf<ShiftKind?>(null) + ShiftKind.entries,
                kindFilter,
                { it?.let { kind -> neoShiftLabel(kind) } ?: neoLabel("الكل", "All") },
                { kindFilter = it },
            )
            NeoChoice(
                neoLabel("مكان العمل", "Workplace"),
                listOf<String?>(null) + workplaces.map { it.id },
                workplace,
                { id ->
                    workplaces.firstOrNull { it.id == id }?.name
                        ?: neoLabel("جميع الأماكن", "All workplaces")
                },
                { workplace = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    from,
                    { from = it.take(10) },
                    label = { Text(neoLabel("من YYYY-MM-DD", "From YYYY-MM-DD")) },
                    singleLine = true,
                    isError = !validDates,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    to,
                    { to = it.take(10) },
                    label = { Text(neoLabel("إلى YYYY-MM-DD", "To YYYY-MM-DD")) },
                    singleLine = true,
                    isError = !validDates,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                onClick = {
                    workplace = null
                    kindFilter = null
                    update(p.copy(timelineFilter = NeoTimelineFilter.ALL))
                    from = month.atDay(1).toString()
                    to = month.atEndOfMonth().toString()
                }
            ) {
                Text(neoLabel("إعادة الفلاتر", "Reset filters"))
            }
        }
        if (!validDates)
            Text(
                neoLabel("أدخل نطاق تاريخ صحيح", "Enter a valid date range"),
                color = MaterialTheme.colorScheme.error,
            )
        else if (filtered.isEmpty() && offRows.isEmpty())
            NeoEmptyState(
                neoLabel("لا توجد شيفتات مطابقة", "No matching shifts"),
                neoLabel(
                    "غيّر الفلتر أو أضف شيفتًا من التقويم",
                    "Change filters or add a shift from the calendar",
                ),
            )
        if (validDates && offRows.isNotEmpty()) {
            Text(
                neoLabel("أيام الراحة", "Off entries"),
                style = MaterialTheme.typography.titleSmall,
            )
            offRows.take(limit).forEach { (day, _) ->
                OutlinedCard(
                    onClick = { onOpenDay(LocalDate.parse(day.date), day.calendarId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ListItem(
                        headlineContent = { Text(day.date + " • " + neoLabel("راحة", "Off")) },
                        supportingContent = {
                            Text(
                                (db.calendars.firstOrNull { it.id == day.calendarId }?.name
                                    ?: day.calendarId) + " • " + neoLabel("بلا مدة", "No duration")
                            )
                        },
                    )
                }
            }
        }
        if (validDates)
            filtered.take(limit).forEach { item ->
                Card(
                    onClick = { onOpenDay(item.start.toLocalDate(), item.entry.calendarId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val recorded = item.segment.startMinute != null && item.segment.endMinute != null
                    val active = recorded && now >= item.start && now < item.end
                    val future = item.start > now
                    val status = if(!recorded) neoLabel("بلا وقت","No time") else if(active) neoLabel("جارٍ الآن","In progress") else if(future) neoLabel("قادم","Upcoming") else neoLabel("انتهى","Finished")
                    val tint = if(active) MaterialTheme.colorScheme.primary else if(future) (if(MaterialTheme.colorScheme.surface.luminance()<.3f) NeoPremium.Gold else Color(0xFF8B641C)) else positive
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.widthIn(min=64.dp,max=90.dp).background(tint.copy(alpha=.13f)).padding(horizontal=10.dp,vertical=18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                            Icon(if(active) Icons.Rounded.Schedule else if(future) Icons.Rounded.Upcoming else Icons.Rounded.CheckCircle,null,tint=tint)
                            Text(status,style=MaterialTheme.typography.labelSmall,color=tint)
                        }
                        Column(Modifier.weight(1f).padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            NeoShiftSummary(item,workplaces)
                            Text(db.calendars.firstOrNull {it.id==item.entry.calendarId}?.name.orEmpty(),style=MaterialTheme.typography.labelSmall)
                            if(neoHasConflict(item,all)) Text(neoLabel("تعارض زمني • راجع التفاصيل","Time conflict • review details"),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                            if(p.showShiftNotes && item.segment.note.isNotBlank()) Text(item.segment.note,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        if (validDates && workplace == null && (kindFilter == null || kindFilter == ShiftKind.OFF))
            db.days
                .filter {
                    it.calendarId in visibleIds &&
                        it.date >= from &&
                        it.date <= to &&
                        it.segments.any { s -> s.kind == ShiftKind.OFF }
                }
                .forEach { day ->
                    TextButton(onClick = { onOpenDay(LocalDate.parse(day.date), day.calendarId) }) {
                        Text(day.date + " • " + neoLabel("راحة", "Off"))
                    }
                }
        if (validDates && (filtered.size > limit || offRows.size > limit))
            TextButton(onClick = { limit += 20 }) { Text(neoLabel("عرض المزيد", "Show more")) }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
    }
}

@Composable
fun NeoFinanceOverview(result: MonthFinanceV11, visible: Boolean) {
    val en = LocalNeoEnglish.current
    val positive = neoPositiveColor()
    val p = LocalNeoAppearance.current
    fun amount(value: Double): String =
        if (visible)
            "\u2066" +
                String.format(Locale.US, "%,.2f", value) +
                "\u2069" +
                (if (en) " EGP" else " ج.م")
        else "••••••"
    val metrics =
        listOf(
            neoLabel("المستحق", "Expected") to result.expected,
            neoLabel("المستلم", "Received") to result.received,
            neoLabel("المتبقي", "Remaining") to result.difference,
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.2f
            @Composable fun Metric(index: Int, modifier: Modifier = Modifier) {
                val metric = metrics[index]
                val tint = if(index == 1) positive else MaterialTheme.colorScheme.primary
                Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if(index == 1) Color(0xFFB1CEAD) else MaterialTheme.colorScheme.outlineVariant), shadowElevation = 2.dp) {
                    Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(metric.first,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(amount(metric.second),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=tint)
                    }
                }
            }
            if(stacked) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {metrics.indices.forEach {Metric(it,Modifier.fillMaxWidth())}}
            else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {metrics.indices.forEach {Metric(it,Modifier.weight(1f))}}
        }
        if (p.showFinanceChart && visible && result.expected > 0.0) {
            val progress=(result.received/result.expected).coerceIn(0.0,1.0).toFloat()
            val track=MaterialTheme.colorScheme.outlineVariant
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(76.dp),contentAlignment=Alignment.Center) {
                        Canvas(Modifier.fillMaxSize().padding(5.dp)) {
                            drawArc(track,-90f,360f,false,style=Stroke(10.dp.toPx()))
                            drawArc(positive,-90f,360f*progress,false,style=Stroke(10.dp.toPx(),cap=androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                        Text("${(progress*100).toInt()}%",style=MaterialTheme.typography.titleMedium)
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text(neoLabel("تقدم التحصيل","Collection progress"),style=MaterialTheme.typography.titleMedium)
                        Text(neoLabel("المستلم من إجمالي المستحق","Received of expected income"),style=MaterialTheme.typography.bodySmall)
                        Text(neoLabel("التسويات: ","Settlements: ")+amount(result.settlements),style=MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun NeoTaskSearch(
    query: String,
    onQueryChange: (String) -> Unit,
    priority: TaskPriorityV11?,
    onPriority: (TaskPriorityV11?) -> Unit,
    calendarId: String?,
    onCalendar: (String?) -> Unit,
    db: V11Database,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            query,
            onQueryChange,
            label = { Text(neoLabel("ابحث في المهام", "Search tasks")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = { filtersOpen = !filtersOpen }) {
                    Text(neoLabel("فلاتر", "Filters"))
                }
            },
        )
        if (filtersOpen) {
            NeoChoice(
                neoLabel("الأولوية", "Priority"),
                listOf<TaskPriorityV11?>(null) + TaskPriorityV11.entries,
                priority,
                { neoPriorityLabel(it) },
                onPriority,
            )
            NeoChoice(
                neoLabel("التصنيف حسب التقويم", "Category by calendar"),
                listOf<String?>(null) + db.calendars.map { it.id },
                calendarId,
                { id ->
                    db.calendars.firstOrNull { it.id == id }?.name
                        ?: neoLabel("كل التقاويم", "All calendars")
                },
                onCalendar,
            )
        }
    }
}

@Composable
fun neoPriorityLabel(priority: TaskPriorityV11?): String =
    when (priority) {
        null -> neoLabel("كل الأولويات", "All priorities")
        TaskPriorityV11.LOW -> neoLabel("منخفضة", "Low")
        TaskPriorityV11.NORMAL -> neoLabel("عادية", "Normal")
        TaskPriorityV11.HIGH -> neoLabel("عالية", "High")
        TaskPriorityV11.URGENT -> neoLabel("عاجلة", "Urgent")
    }
