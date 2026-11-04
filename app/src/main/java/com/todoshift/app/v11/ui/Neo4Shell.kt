package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun Neo4CompactHeader(title: String, calendar: Boolean, showMoney: Boolean, moneyVisible: Boolean,
    onMoney: () -> Unit, onToday: () -> Unit, onAdd: () -> Unit, onManage: () -> Unit,
    overview: Boolean, onOverview: () -> Unit) {
    val p = LocalNeoAppearance.current
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = p.cardOpacity)) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 48.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (p.showBrand) Text("ToDo Shift", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (calendar) {
                IconButton(onClick = onToday) { Icon(Icons.Rounded.Today, neoLabel("اليوم", "Today")) }
                IconButton(onClick = onAdd) { Icon(Icons.Rounded.Add, neoLabel("إضافة", "Add")) }
                IconButton(onClick = onManage) { Icon(Icons.Rounded.MoreVert, neoLabel("إدارة التقاويم", "Manage calendars")) }
            }
            if (overview) IconButton(onClick = onOverview) { Icon(Icons.Rounded.Dashboard, neoLabel("المعلومات الاختيارية", "Optional overview")) }
            if (showMoney) IconButton(onClick = onMoney) { Icon(if (moneyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, neoLabel("إظهار أو إخفاء المبالغ", "Show or hide amounts")) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Neo4OverviewSheet(db: V11Database, workplaces: List<Workplace>, authorized: Boolean, moneyVisible: Boolean,
    onAction: (NeoQuickAction) -> Unit, onDismiss: () -> Unit) {
    val p = LocalNeoAppearance.current; val now = neoNow()
    val ids = remember(db.calendars) { db.calendars.filter { it.visible }.map { it.id }.toSet() }
    val shifts = remember(db.days, ids) { neoShiftOccurrences(db, ids) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (p.showGreeting) Text(when(now.hour) { in 5..11 -> neoLabel("صباح الخير", "Good morning"); in 12..17 -> neoLabel("نهارك سعيد", "Good afternoon"); else -> neoLabel("مساء الخير", "Good evening") }, style = MaterialTheme.typography.titleMedium)
            p.dashboardOrder.filter { p.cardVisible(it) }.forEach { card ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(neoSpacing()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(neoCardLabel(card), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        when(card) {
                            NeoDashboardCard.CURRENT_SHIFT -> {
                                val current = shifts.filter { now >= it.start && now < it.end }
                                if (current.isEmpty()) Text(neoLabel("لا يوجد شيفت جارٍ", "No current shift")) else current.forEach { NeoShiftSummary(it, workplaces) }
                            }
                            NeoDashboardCard.NEXT_SHIFT -> shifts.firstOrNull { it.start > now }?.let { NeoShiftSummary(it, workplaces) } ?: Text(neoLabel("لا يوجد شيفت قادم", "No upcoming shift"))
                            NeoDashboardCard.SUMMARY -> Text(neoLabel("شيفتات الشهر: ", "Monthly shifts: ") + shifts.count { it.entry.date.startsWith(YearMonth.from(now).toString()) })
                            NeoDashboardCard.TASKS -> Text(neoLabel("مهام اليوم: ", "Today's tasks: ") + db.tasks.count { it.date == now.toLocalDate().toString() && it.status != TaskStatusV11.COMPLETED })
                            NeoDashboardCard.NEXT_EVENT -> {
                                val task = db.tasks.filter { it.date >= now.toLocalDate().toString() && it.status != TaskStatusV11.COMPLETED }.minByOrNull { it.date + (it.time ?: "99:99") }
                                Text(task?.let { it.title + " • " + it.date } ?: neoLabel("لا توجد مهمة قادمة", "No upcoming task"))
                            }
                        }
                    }
                }
            }
            if (p.showFinanceSummary) {
                if (authorized) NeoFinanceOverview(V11FinanceEngine.month(db, YearMonth.from(now)), moneyVisible)
                else Text(neoLabel("افتح الحسابات للتحقق من هويتك قبل عرض المبالغ", "Unlock finances before viewing protected amounts"))
            }
            if (p.showOptionalInfo) p.quickActions.forEach { action -> OutlinedButton(onClick = { onDismiss(); onAction(action) }, modifier = Modifier.fillMaxWidth()) { Text(neoActionLabel(action)) } }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
}
