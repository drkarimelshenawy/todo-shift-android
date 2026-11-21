package com.todoshift.app.v11.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import com.todoshift.cashbook.ui.CashBookSectionPreference
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Shared content spacing; 48dp actions remain accessible even in compact density. */
object NeoSpace {
    val page = 12.dp
    val gap = 8.dp
    val row = 48.dp
    val radius = 12.dp
}

data class NeoFeature(
    val id: String,
    val ar: String,
    val en: String,
    val destination: String,
    val group: String = "",
    val keywords: String = "",
)

object NeoFeatureCatalog {
    val entries =
        listOf(
            NeoFeature("FINANCE_DISPLAY", "عرض الحسابات", "Finance display", "Settings", "CASHBOOK"),
            NeoFeature("WORK_SUMMARY", "ملخص العمل التشغيلي", "Operational work summary", "Calendar"),
            NeoFeature("COPY", "نسخ الورديات", "Copy shifts", "Calendar"),
            NeoFeature(
                "PAINT_SHORTCUTS",
                "المفضلة واقتراح الوردية",
                "Favorites and next shift",
                "Calendar",
            ),
            NeoFeature(
                "CALENDAR_DISPLAY",
                "عرض التقويم والملاحظات",
                "Calendar and note display",
                "Settings",
                "CALENDAR",
                "خلايا notes density outside",
            ),
            NeoFeature(
                "TODAY_STYLE",
                "تمييز اليوم",
                "Today highlight",
                "Settings",
                "CALENDAR",
                "circle pulse glow today",
            ),
            NeoFeature("PAINT_STYLE", "ألوان الورديات", "Shift colors", "Settings", "CALENDAR"),
            NeoFeature(
                "CALENDAR_MANAGER",
                "إدارة التقاويم",
                "Manage calendars",
                "Settings",
                "CALENDAR",
            ),
            NeoFeature("WORKPLACES", "أماكن العمل", "Workplaces", "Settings", "CALENDAR"),
            NeoFeature("SHIFT_TEMPLATE", "قوالب الورديات", "Shift templates", "Calendar"),
            NeoFeature("COMPARE", "مقارنة شهرين", "Compare months", "Calendar"),
            NeoFeature("TASK_TEMPLATES", "قوالب المهام", "Task templates", "Tasks"),
            NeoFeature("ATTACHMENTS", "المرفقات", "Attachments", "Tasks"),
            NeoFeature("RATES", "أسعار الورديات المؤرخة", "Effective-dated rates", "Finances"),
            NeoFeature("PAYMENTS", "توزيع الدفعات على الشهور", "Allocate receipts", "Finances"),
            NeoFeature("STATEMENT", "كشف مكان العمل وتصديره", "Workplace statements", "Finances"),
            NeoFeature("MONTH_REVIEW", "مراجعة وإقفال الشهر", "Review and close month", "Finances"),
            NeoFeature(
                "APPEARANCE",
                "استوديو المظهر",
                "Appearance Studio",
                "Settings",
                "APPEARANCE",
                "theme colors fonts density motion profiles Nile Jade ثيم ثيمات يشم النيل ألوان خط حركة",
            ),
            NeoFeature(
                "LANGUAGE",
                "لغة التطبيق",
                "App language",
                "Settings",
                "LANGUAGE",
                "العربية English",
            ),
            NeoFeature(
                "UI_DIRECTION",
                "اتجاه الواجهة",
                "UI direction",
                "Settings",
                "LANGUAGE",
                "RTL LTR",
            ),
            NeoFeature(
                "NAV_DIRECTION",
                "اتجاه التنقل",
                "Navigation direction",
                "Settings",
                "LANGUAGE",
                "RTL LTR",
            ),
            NeoFeature(
                "GRID_DIRECTION",
                "اتجاه التقويم",
                "Calendar grid direction",
                "Settings",
                "LANGUAGE",
                "RTL LTR",
            ),
            NeoFeature(
                "WEEKDAY_DIRECTION",
                "اتجاه أيام الأسبوع",
                "Weekday direction",
                "Settings",
                "LANGUAGE",
                "RTL LTR",
            ),
            NeoFeature(
                "FIRST_WEEKDAY",
                "أول يوم في الأسبوع",
                "First weekday",
                "Settings",
                "CALENDAR",
                "السبت الأحد الاثنين Saturday Sunday Monday",
            ),
            NeoFeature(
                "NOTIFICATIONS",
                "مركز التنبيهات",
                "Notification center",
                "Settings",
                "NOTIFICATIONS",
                "quiet reminders تذكير هدوء",
            ),
            NeoFeature(
                "WIDGETS",
                "الويدجت والاختصارات",
                "Widgets and shortcuts",
                "Settings",
                "WIDGETS",
            ),
            NeoFeature(
                "SECURITY",
                "قفل الحسابات والخصوصية",
                "Finance lock and privacy",
                "Settings",
                "SECURITY",
            ),
            NeoFeature(
                "BACKUP",
                "النسخ والاستعادة والحساب",
                "Backup, restore and account",
                "Settings",
                "DATA",
            ),
            NeoFeature("HEALTH", "سلامة البيانات", "Data health", "Settings", "DATA"),
            NeoFeature("STORAGE", "مساحة المرفقات", "Attachment storage", "Settings", "DATA"),
            NeoFeature(
                "LEGAL",
                "الخصوصية والسياسات",
                "Privacy and policies",
                "Settings",
                "ADVANCED",
            ),
            NeoFeature("LAUNCH", "شاشة البداية الافتراضية", "Default start screen", "Settings", "APPEARANCE"),
            NeoFeature("PERFORMANCE", "وضع الأداء", "Performance mode", "Settings", "ADVANCED"),
            NeoFeature("FOCUS", "التركيز المؤقت", "Temporary focus", "Tasks"),
            NeoFeature(
                "USAGE",
                "إحصاءات استخدام محلية",
                "Local usage statistics",
                "Settings",
                "ADVANCED",
            ),
            NeoFeature(
                "ABOUT",
                "حول التطبيق والتشخيص",
                "About and diagnostics",
                "Settings",
                "ADVANCED",
            ),
            NeoFeature(
                "TASK_OVERVIEW",
                "ملخص المهام وترتيبه",
                "Task overview and ordering",
                "Settings",
                "TASKS",
            ),
            NeoFeature(
                "NOTES_PREFERENCES",
                "تفضيلات الملاحظات",
                "Notes preferences",
                "Settings",
                "NOTES",
                "الخطوط الخلفيات الورق المسطر حذف سلة المهملات notes fonts background trash",
            ),
            NeoFeature(
                "LEGACY",
                "المظهر الكلاسيكي المحفوظ",
                "Saved Classic preferences",
                "Settings",
                "ADVANCED",
            ),
        )

    fun search(query: String) =
        entries.filter {
            query.isNotBlank() && "${it.ar} ${it.en} ${it.keywords}".contains(query, true)
        }

    fun get(id: String) = entries.firstOrNull { it.id == id }
}


/**
 * Single information architecture for Premium settings. A setting has one canonical owner;
 * shortcuts use the same destination instead of duplicating preferences or storage.
 */
/**
 * Feature catalog groups are navigation taxonomy, NOT screen types.
 * Salary, Tasks and Schedules entries are functional tools, while
 * Settings pages have dedicated owners. Never send a tool to the
 * settings-only route just because it has a non-empty group.
 */
object NeoFeatureRoutePolicy {
    private val dedicatedSettings = setOf(
        "APPEARANCE", "LANGUAGE", "UI_DIRECTION", "NAV_DIRECTION",
        "GRID_DIRECTION", "WEEKDAY_DIRECTION", "WIDGETS",
        "SECURITY", "BACKUP", "LEGAL",
    )
    fun isSettingsPage(id: String): Boolean =
        id in dedicatedSettings || id in NeoSettingsHierarchy.inlinePages
}

object NeoSettingsHierarchy {
    val rootGroups = listOf(
        "APPEARANCE", "LANGUAGE", "SECTIONS", "NOTIFICATIONS", "WIDGETS",
        "SECURITY", "DATA", "ADVANCED",
    )
    val sectionGroups = listOf("CALENDAR", "TASKS", "NOTES", "SALARY", "CASHBOOK")
    val inlinePages = setOf(
        "CALENDAR_DISPLAY", "FIRST_WEEKDAY", "TODAY_STYLE", "PAINT_STYLE",
        "TASK_OVERVIEW", "NOTES_PREFERENCES", "FINANCE_DISPLAY",
        "LAUNCH", "PERFORMANCE", "LEGACY", "ABOUT",
    )

    fun children(group: String): List<String> = when (group) {
        "APPEARANCE" -> listOf("APPEARANCE", "LAUNCH")
        "CALENDAR" -> listOf("CALENDAR_DISPLAY", "FIRST_WEEKDAY", "TODAY_STYLE", "PAINT_STYLE")
        "TASKS" -> listOf("TASK_OVERVIEW")
        "NOTES" -> listOf("NOTES_PREFERENCES")
        "SALARY" -> listOf("RATES", "STATEMENT")
        "CASHBOOK" -> listOf("FINANCE_DISPLAY")
        "DATA" -> listOf("BACKUP", "HEALTH", "STORAGE")
        "ADVANCED" -> listOf("ABOUT", "LEGAL", "PERFORMANCE", "USAGE", "LEGACY")
        else -> emptyList()
    }

    fun parent(page: String): String = when (page) {
        "CALENDAR_GROUP", "TASKS_GROUP", "NOTES_GROUP", "SALARY_GROUP", "CASHBOOK_GROUP" -> "SECTIONS_GROUP"
        "CALENDAR_DISPLAY", "FIRST_WEEKDAY", "TODAY_STYLE", "PAINT_STYLE" -> "CALENDAR_GROUP"
        "TASK_OVERVIEW" -> "TASKS_GROUP"
        "TASK_NOTES", "NOTES_PREFERENCES" -> "NOTES_GROUP"
        "FINANCE_DISPLAY" -> "CASHBOOK_GROUP"
        "LAUNCH" -> "APPEARANCE_GROUP"
        "PERFORMANCE", "LEGACY", "ABOUT" -> "ADVANCED_GROUP"
        else -> "HOME"
    }
}

data class NeoUndoAction(val enabled: Boolean = false, val perform: () -> Unit = {})

val LocalNeoUndo = staticCompositionLocalOf { NeoUndoAction() }

class NeoFeatureNavigator(val navigate: (String, String?) -> Unit) {
    fun open(id: String, owner: String? = null) = navigate(id, owner)
}

val LocalNeoFeatures = staticCompositionLocalOf { NeoFeatureNavigator { _, _ -> } }

// The root shell owns the title and guarded Back on focused feature routes.
val LocalNeoShellPageHeader = staticCompositionLocalOf { false }

@Composable
fun NeoPageHeader(title: String, onBack: () -> Unit) {
    if (LocalNeoShellPageHeader.current) return
    Row(
        Modifier.fillMaxWidth().heightIn(min = NeoSpace.row),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, neoLabel("رجوع", "Back"))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun NeoSettingsRow(title: String, summary: String = "", onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = NeoSpace.row)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (summary.isNotBlank())
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
}

/** Shared locale-aware dropdown used by settings, notes and contextual actions. */
@Composable
fun NeoLocalizedMenu(expanded: Boolean, onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides
        if (LocalNeoEnglish.current) LayoutDirection.Ltr else LayoutDirection.Rtl) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, content = content)
    }
}

@Composable
fun NeoContextActions(destination: String) {
    val nav = LocalNeoFeatures.current
    val undo = LocalNeoUndo.current
    var open by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, neoLabel("خيارات $destination", "$destination actions"))
        }
        NeoLocalizedMenu(open, { open = false }) {
            if (destination == "Calendar") {
                val appearance = LocalNeoAppearance.current
                val update = LocalNeoOnChange.current
                DropdownMenuItem(
                    text = { Text(neoLabel("إدارة أماكن العمل", "Manage workplaces")) },
                    leadingIcon = { Icon(Icons.Rounded.Business, null) },
                    onClick = { open = false; nav.open("WORKPLACES") },
                )
                HorizontalDivider()
                Text(neoLabel("العرض الافتراضي", "Default calendar view"),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                listOf(
                    Triple(com.todoshift.app.v11.NeoCalendarView.MONTH, neoLabel("شهر", "Month"), Icons.Rounded.CalendarMonth),
                    Triple(com.todoshift.app.v11.NeoCalendarView.WEEK, neoLabel("أسبوع", "Week"), Icons.Rounded.ViewWeek),
                    Triple(com.todoshift.app.v11.NeoCalendarView.DAY, neoLabel("يوم", "Day"), Icons.Rounded.Today),
                ).forEach { (view, label, icon) ->
                    DropdownMenuItem(text = { Text(label) },
                        leadingIcon = { Icon(icon, null) },
                        trailingIcon = if (appearance.calendarView == view) {
                            { Icon(Icons.Rounded.Check, neoLabel("محدد", "Selected")) }
                        } else null,
                        onClick = { open = false; update(appearance.copy(calendarView = view)) })
                }
                if (appearance.calendarView == com.todoshift.app.v11.NeoCalendarView.WEEK)
                    DropdownMenuItem(
                        text = { Text(if (appearance.weeklyRows) neoLabel("الأسبوع: ٧ أعمدة", "Week: 7 columns")
                            else neoLabel("الأسبوع: ٧ صفوف", "Week: 7 rows")) },
                        leadingIcon = { Icon(Icons.Rounded.ViewModule, null) },
                        onClick = { open = false; update(appearance.copy(weeklyRows = !appearance.weeklyRows)) })
            } else if (destination == "Cash Book") {
                val selected = CashBookSectionPreference.load(context)
                Text(neoLabel("القسم الافتراضي", "Default finance section"),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                listOf(
                    Triple(CashBookSectionPreference.ACCOUNTS, neoLabel("الحسابات", "Accounts"), Icons.Rounded.AccountBalanceWallet),
                    Triple(CashBookSectionPreference.DEBTS, neoLabel("المديونيات", "Debts"), Icons.Rounded.ReceiptLong),
                    Triple(CashBookSectionPreference.SALARY, neoLabel("مستحقات العمل", "Work entitlements"), Icons.Rounded.Payments),
                ).forEach { (section, label, icon) ->
                    DropdownMenuItem(text = { Text(label) }, leadingIcon = { Icon(icon, null) },
                        trailingIcon = if (selected == section) { { Icon(Icons.Rounded.Check, null) } } else null,
                        onClick = { if (CashBookSectionPreference.save(context, section)) open = false })
                }
            }
            NeoFeatureCatalog.entries
                .filter { it.destination == destination && destination != "Calendar" }
                .forEach { feature ->
                    val icon: ImageVector = when (feature.id) {
                        "TASK_TEMPLATES", "SHIFT_TEMPLATE" -> Icons.Rounded.ContentCopy
                        "STATEMENT", "WORK_SUMMARY" -> Icons.Rounded.Description
                        "PAYMENTS", "RATES" -> Icons.Rounded.Payments
                        "ATTACHMENTS" -> Icons.Rounded.AttachFile
                        "COMPARE", "MONTH_REVIEW" -> Icons.Rounded.Assessment
                        else -> Icons.Rounded.Tune
                    }
                    DropdownMenuItem(text = { Text(neoLabel(feature.ar, feature.en)) },
                        leadingIcon = { Icon(icon, null) },
                        onClick = { open = false; nav.open(feature.id) })
                }
        }

    }
}

/** Shared guard for inherited editors; the current editor remains mounted while confirming. */
@Composable
fun rememberNeoEditorExit(
    values: List<Any?>,
    onDismiss: () -> Unit,
    busy: Boolean = false,
): NeoEditorExit {
    val gate = LocalNeoLeaveGate.current
    val controller = remember { NeoEditorExit() }
    val pending = LocalNeoEditorWriteState.current.pending || busy
    val initial = rememberSaveable { values.toList() }
    var confirm by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun request(next: () -> Unit) {
        if (pending) return
        if (values != initial) {
            destination = next
            confirm = true
        } else next()
    }
    neoRegisterLeaveGuard { next ->
        request {
            onDismiss()
            next()
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = {
                if (!pending) {
                    confirm = false
                    destination = null
                }
            },
            title = { Text(neoLabel("تجاهل التعديلات؟", "Discard changes?")) },
            confirmButton = {
                Column {
                    TextButton(
                        enabled = !pending && controller.save != null,
                        onClick = {
                            val next = destination ?: return@TextButton
                            gate?.awaitSavedExit(next)
                            try {
                                controller.save?.invoke()
                            } finally {
                                // Drop a request rejected by validation or an exception before
                                // submission.
                                gate?.takeSavedExit()
                            }
                        },
                    ) {
                        Text(neoLabel("حفظ ومغادرة", "Save and leave"))
                    }
                    TextButton(
                        enabled = !pending,
                        onClick = {
                            confirm = false
                            destination?.invoke()
                            destination = null
                        },
                    ) {
                        Text(neoLabel("تجاهل", "Discard"))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !pending,
                    onClick = {
                        confirm = false
                        destination = null
                    },
                ) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    controller.dismiss = { request(onDismiss) }
    return controller
}

class NeoEditorExit : () -> Unit {
    internal var save: (() -> Unit)? by mutableStateOf(null)
    internal var dismiss: () -> Unit = {}

    override fun invoke() = dismiss()

    @Composable
    fun SaveButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        content: @Composable RowScope.() -> Unit,
    ) {
        SideEffect { save = if (enabled) onClick else null }
        Button(onClick = onClick, enabled = enabled, modifier = modifier, content = content)
    }
}

@Composable
fun neoSearchCategory(type: String): String =
    when (type) {
        "TASK" -> neoLabel("مهام", "Tasks")
        "NOTE" -> neoLabel("ملاحظات", "Notes")
        "EVENT" -> neoLabel("مواعيد", "Events")
        "SHIFT" -> neoLabel("ورديات", "Shifts")
        "PAYMENT" -> neoLabel("دفعات", "Payments")
        "WORKPLACE" -> neoLabel("أماكن العمل", "Workplaces")
        else -> type
    }
