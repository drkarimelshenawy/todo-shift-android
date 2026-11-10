package com.todoshift.app.v11.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun neoNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(30_000L)
        }
    }
    return now
}

@Composable
fun NeoDashboard(
    db: V11Database,
    workplaces: List<Workplace>,
    onQuickAction: (NeoQuickAction) -> Unit,
    financeAuthorized: Boolean = false,
    moneyVisible: Boolean = false,
) {
    if (LocalNeoFocus.current) return
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    val now = neoNow()
    val en = LocalNeoEnglish.current
    val locale = if (en) Locale.ENGLISH else Locale.forLanguageTag("ar")
    val visibleIds =
        remember(db.calendars) { db.calendars.filter { it.visible }.map { it.id }.toSet() }
    val shifts = remember(db.days, visibleIds) { neoShiftOccurrences(db, visibleIds) }
    val current = shifts.filter { now >= it.start && now < it.end }
    val next = shifts.firstOrNull { it.start > now }
    val today = now.toLocalDate().toString()
    val pendingTasks =
        db.tasks.filter {
            it.status != TaskStatusV11.COMPLETED &&
                (it.calendarId == null || it.calendarId in visibleIds)
        }
    val completed =
        db.tasks.count {
            it.status == TaskStatusV11.COMPLETED &&
                (it.calendarId == null || it.calendarId in visibleIds)
        }
    val events =
        db.dayItems
            .filter {
                !it.completed &&
                    it.date >= today &&
                    (it.calendarId == null || it.calendarId in visibleIds)
            }
            .sortedBy { it.date }
    val greeting =
        when (now.hour) {
            in 5..11 -> neoLabel("صباح الخير", "Good morning")
            in 12..17 -> neoLabel("نهارك سعيد", "Good afternoon")
            else -> neoLabel("مساء الخير", "Good evening")
        }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (p.showGreeting) greeting else neoLabel("اليوم", "Today"),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { update(p.copy(dashboardExpanded = !p.dashboardExpanded)) }) {
                Icon(
                    if (p.dashboardExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    neoLabel("إظهار أو إخفاء لوحة اليوم", "Expand or collapse today's dashboard"),
                )
            }
        }
        AnimatedVisibility(
            visible = p.dashboardExpanded,
            enter = fadeIn(tween(p.motionMillis)),
            exit = fadeOut(tween(p.motionMillis)),
        ) {
            Box {
                val screenHeight =
                    androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
                Column(
                    Modifier.heightIn(max = screenHeight * 0.38f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    p.dashboardOrder
                        .filter { p.cardVisible(it) }
                        .forEach { card ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(neoSpacing()),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        neoCardLabel(card),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    when (card) {
                                        NeoDashboardCard.CURRENT_SHIFT ->
                                            if (current.isEmpty()) {
                                                Text(
                                                    neoLabel(
                                                        "لا يوجد شيفت جارٍ الآن",
                                                        "No shift in progress",
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                )
                                            } else
                                                current.forEach { NeoShiftSummary(it, workplaces) }
                                        NeoDashboardCard.NEXT_SHIFT ->
                                            if (next == null) {
                                                Text(
                                                    neoLabel(
                                                        "لا توجد شيفتات قادمة",
                                                        "No upcoming shifts",
                                                    )
                                                )
                                            } else NeoShiftSummary(next, workplaces)
                                        NeoDashboardCard.SUMMARY -> {
                                            val count =
                                                shifts.count {
                                                    it.entry.date.startsWith(
                                                        now.toLocalDate().toString().take(7)
                                                    )
                                                }
                                            Text(
                                                neoLabel(
                                                    "شيفتات الشهر: $count",
                                                    "Shifts this month: $count",
                                                ),
                                                style = MaterialTheme.typography.titleLarge,
                                            )
                                            Text(
                                                neoLabel(
                                                    "التقاويم الظاهرة: ${visibleIds.size}",
                                                    "Visible calendars: ${visibleIds.size}",
                                                )
                                            )
                                        }
                                        NeoDashboardCard.TASKS -> {
                                            Text(
                                                neoLabel(
                                                    "مهام اليوم: ${pendingTasks.count { it.date == today }} • متأخرة: ${pendingTasks.count { it.date < today }}",
                                                    "Today: ${pendingTasks.count { it.date == today }} • Overdue: ${pendingTasks.count { it.date < today }}",
                                                )
                                            )
                                            val total = pendingTasks.size + completed
                                            if (total > 0) {
                                                LinearProgressIndicator(
                                                    progress = { completed.toFloat() / total },
                                                    modifier = Modifier.fillMaxWidth(),
                                                )
                                                Text(
                                                    neoLabel(
                                                        "مكتملة: $completed من $total",
                                                        "Completed: $completed of $total",
                                                    ),
                                                    style = MaterialTheme.typography.labelMedium,
                                                )
                                            }
                                        }
                                        NeoDashboardCard.NEXT_EVENT -> {
                                            val task =
                                                pendingTasks
                                                    .filter { it.date >= today }
                                                    .minByOrNull { it.date + (it.time ?: "99:99") }
                                            val event = events.firstOrNull()
                                            when {
                                                task != null &&
                                                    (event == null || task.date <= event.date) -> {
                                                    Text(
                                                        task.title,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    Text(
                                                        listOfNotNull(task.date, task.time)
                                                            .joinToString(" • "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                                event != null -> {
                                                    Text(event.title)
                                                    Text(
                                                        event.date,
                                                        style = MaterialTheme.typography.bodySmall,
                                                    )
                                                }
                                                else ->
                                                    Text(
                                                        neoLabel(
                                                            "لا توجد التزامات قادمة",
                                                            "No upcoming commitments",
                                                        )
                                                    )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    if (p.showFinanceSummary) {
                        if (financeAuthorized)
                            NeoFinanceOverview(
                                V11FinanceEngine.month(db, java.time.YearMonth.from(now)),
                                moneyVisible,
                            )
                        else
                            Text(
                                neoLabel(
                                    "افتح الحسابات للتحقق قبل عرض المبالغ",
                                    "Unlock Finance before viewing protected amounts",
                                )
                            )
                    }
                    if (p.quickActions.isNotEmpty())
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            p.quickActions.forEach { action ->
                                FilledTonalButton(onClick = { onQuickAction(action) }) {
                                    Text(neoActionLabel(action))
                                }
                            }
                        }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun NeoShiftSummary(item: NeoShiftOccurrence, workplaces: List<Workplace>) {
    val en = LocalNeoEnglish.current
    val locale = if (en) Locale.ENGLISH else Locale.forLanguageTag("ar")
    Text(
        listOfNotNull(
                neoShiftLabel(item.segment.kind),
                workplaces.firstOrNull { it.id == item.segment.workplaceId }?.name,
            )
            .joinToString(" • "),
        style = MaterialTheme.typography.titleMedium,
    )
    val recorded = item.segment.startMinute != null && item.segment.endMinute != null
    val time =
        if (recorded) "\u2066" + neoSegmentClock(item.segment, en) + "\u2069"
        else neoLabel("أوقات الساعة غير مسجلة", "Clock times not recorded")
    Text(item.entry.date + " • " + time, style = MaterialTheme.typography.bodySmall)
    if (recorded && item.end.toLocalDate() != item.start.toLocalDate())
        Text(
            neoLabel(
                "ينتهي ${item.end.toLocalDate()} • مسجل بيوم البداية",
                "Ends ${item.end.toLocalDate()} • filed on start day",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
}

@Composable
fun neoShiftLabel(kind: ShiftKind): String =
    when (kind) {
        ShiftKind.FULL_DAY -> "24h"
        ShiftKind.LONG -> neoLabel("طويلة", "Long")
        ShiftKind.NIGHT -> neoLabel("ليلية", "Night")
        ShiftKind.MORNING -> neoLabel("صباحية", "Morning")
        ShiftKind.EVENING -> neoLabel("مسائية", "Evening")
        ShiftKind.OFF -> neoLabel("إجازة", "Off")
    }

@Composable
fun neoCardLabel(card: NeoDashboardCard): String =
    when (card) {
        NeoDashboardCard.CURRENT_SHIFT -> neoLabel("الشيفت الحالي", "Current shift")
        NeoDashboardCard.NEXT_SHIFT -> neoLabel("الشيفت القادم", "Next shift")
        NeoDashboardCard.SUMMARY -> neoLabel("ملخص الشهر", "Monthly overview")
        NeoDashboardCard.TASKS -> neoLabel("تقدم المهام", "Task progress")
        NeoDashboardCard.NEXT_EVENT -> neoLabel("أقرب التزام", "Next commitment")
    }

@Composable
fun neoActionLabel(action: NeoQuickAction): String =
    when (action) {
        NeoQuickAction.ADD_SHIFT -> neoLabel("إضافة شيفت", "Add shift")
        NeoQuickAction.ADD_TASK -> neoLabel("إضافة مهمة", "Add task")
        NeoQuickAction.TODAY -> neoLabel("اليوم", "Today")
    }
