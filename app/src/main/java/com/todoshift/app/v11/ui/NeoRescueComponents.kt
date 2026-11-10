package com.todoshift.app.v11.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
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
            NeoFeature("FINANCE_DISPLAY", "عرض الحسابات", "Finance display", "Finances", "FINANCE"),
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
            NeoFeature("SHIFT_TEMPLATE", "قوالب الورديات", "Shift templates", "Schedules"),
            NeoFeature("COMPARE", "مقارنة شهرين", "Compare months", "Schedules"),
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
                "theme colors fonts density motion profiles ألوان خط حركة",
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
                "LANGUAGE",
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
                "NOTIFICATIONS",
            ),
            NeoFeature(
                "SECURITY",
                "قفل الحسابات والخصوصية",
                "Finance lock and privacy",
                "Settings",
                "PRIVACY",
            ),
            NeoFeature(
                "BACKUP",
                "النسخ والاستعادة والحساب",
                "Backup, restore and account",
                "Settings",
                "PRIVACY",
            ),
            NeoFeature("HEALTH", "سلامة البيانات", "Data health", "Settings", "PRIVACY"),
            NeoFeature("STORAGE", "مساحة المرفقات", "Attachment storage", "Settings", "PRIVACY"),
            NeoFeature(
                "LEGAL",
                "الخصوصية والسياسات",
                "Privacy and policies",
                "Settings",
                "PRIVACY",
            ),
            NeoFeature("PERFORMANCE", "وضع الأداء", "Performance mode", "Settings", "ADVANCED"),
            NeoFeature("FOCUS", "التركيز المؤقت", "Temporary focus", "Settings", "ADVANCED"),
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
                "ADVANCED",
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

data class NeoUndoAction(val enabled: Boolean = false, val perform: () -> Unit = {})

val LocalNeoUndo = staticCompositionLocalOf { NeoUndoAction() }

class NeoFeatureNavigator(val navigate: (String, String?) -> Unit) {
    fun open(id: String, owner: String? = null) = navigate(id, owner)
}

val LocalNeoFeatures = staticCompositionLocalOf { NeoFeatureNavigator { _, _ -> } }

@Composable
fun NeoPageHeader(title: String, onBack: () -> Unit) {
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

@Composable
fun NeoContextActions(destination: String) {
    val nav = LocalNeoFeatures.current
    val undo = LocalNeoUndo.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, neoLabel("خيارات $destination", "$destination actions"))
        }
        DropdownMenu(open, { open = false }) {
            if (destination != "Settings")
                DropdownMenuItem(
                    text = { Text(neoLabel("تراجع عن آخر تعديل", "Undo last change")) },
                    enabled = undo.enabled,
                    onClick = {
                        open = false
                        undo.perform()
                    },
                )
            NeoFeatureCatalog.entries
                .filter { it.destination == destination }
                .forEach { feature ->
                    DropdownMenuItem(
                        text = { Text(neoLabel(feature.ar, feature.en)) },
                        onClick = {
                            open = false
                            nav.open(feature.id)
                        },
                    )
                }
        }
    }
}

/** Shared guard for inherited editors; the current editor remains mounted while confirming. */
@Composable
fun rememberNeoEditorExit(values: List<Any?>, onDismiss: () -> Unit): () -> Unit {
    val pending = LocalNeoEditorWriteState.current.pending
    val initial = rememberSaveable { values.toList() }
    var confirm by remember { mutableStateOf(false) }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(neoLabel("تجاهل التعديلات؟", "Discard changes?")) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(neoLabel("تجاهل", "Discard")) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    return {
        if (!pending) {
            if (values != initial) confirm = true else onDismiss()
        }
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
