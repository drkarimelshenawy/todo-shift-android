package com.exmore.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.exmore.app.v11.*

val neoSettingsGroups =
    listOf(
        Triple("APPEARANCE", "المظهر والتصميم", "Appearance & Design"),
        Triple("LANGUAGE", "اللغة والاتجاه", "Language & Direction"),
        Triple("SECTIONS", "إعدادات الأقسام", "Section Settings"),
        Triple("NOTIFICATIONS", "التنبيهات والتذكيرات", "Notifications & Reminders"),
        Triple("WIDGETS", "الويدجت", "Widgets"),
        Triple("SHORTCUTS", "اختصارات المشغّل", "Launcher shortcuts"),
        Triple("SECURITY", "الأمان والخصوصية", "Security & Privacy"),
        Triple("DATA", "البيانات والنسخ الاحتياطي", "Data & Backup"),
        Triple("ADVANCED", "حول التطبيق والمتقدم", "About & Advanced"),
    )

private fun neoSettingsGroupNames(group: String): Pair<String, String> = when (group) {
    "CALENDAR" -> "التقويم وإدارة العمل" to "Calendar & Work"
    "TASKS" -> "المهام" to "Tasks"
    "NOTES" -> "الملاحظات" to "Notes"
    "SALARY" -> "مستحقات العمل" to "Work Entitlements"
    "CASHBOOK" -> "الإدارة المالية" to "Cash Book"
    else -> neoSettingsGroups.firstOrNull { it.first == group }
        ?.let { it.second to it.third }
        ?: ("الإعدادات" to "Settings")
}

@Composable
private fun NeoSettingsCompactTile(title: String, summary: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick)
                .heightIn(min = 58.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NeoRescueSettings(
    db: V11Database,
    uiSettings: V11UiSettings,
    securitySettings: V11SecuritySettings,
    securityStore: V11SecurityStore,
    financeAuthorized: Boolean,
    onAccountsUnlocked: () -> Unit,
    onSecurityChange: (V11SecuritySettings) -> Unit,
    onUiChange: (V11UiSettings) -> Unit,
    onNeoRestored: (com.exmore.app.backup.Neo4RestoredState) -> Unit,
    initialPage: String = "HOME",
    onBack: () -> Unit = {},
) {
    CompositionLocalProvider(LocalLayoutDirection provides
        if (LocalNeoEnglish.current) LayoutDirection.Ltr else LayoutDirection.Rtl) {
        NeoRescueSettingsContent(
            db, uiSettings, securitySettings, securityStore, financeAuthorized,
            onAccountsUnlocked, onSecurityChange, onUiChange, onNeoRestored,
            initialPage, onBack,
        )
    }
}

@Composable
private fun NeoRescueSettingsContent(
    db: V11Database,
    uiSettings: V11UiSettings,
    securitySettings: V11SecuritySettings,
    securityStore: V11SecurityStore,
    financeAuthorized: Boolean,
    onAccountsUnlocked: () -> Unit,
    onSecurityChange: (V11SecuritySettings) -> Unit,
    onUiChange: (V11UiSettings) -> Unit,
    onNeoRestored: (com.exmore.app.backup.Neo4RestoredState) -> Unit,
    initialPage: String = "HOME",
    onBack: () -> Unit = {},
) {
    var page by rememberSaveable(initialPage) { mutableStateOf(initialPage) }
    var query by rememberSaveable { mutableStateOf("") }
    val nav = LocalNeoFeatures.current
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    fun back() {
        if (initialPage == "HOME" && page != "HOME") {
            page = if(page == "SHORTCUTS") "HOME" else NeoSettingsHierarchy.parent(page)
        } else onBack()
    }
    BackHandler(page != "HOME") { back() }
    if (page in neoLanguageRoutes) {
        NeoLanguageSettings(page) { back() }
        return
    }
    if (page == "TASK_NOTES" || page == "NOTES_PREFERENCES") {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(neoSpacing()),
            verticalArrangement = Arrangement.spacedBy(NeoSpace.gap)) {
            NeoPageHeader(neoLabel("تفضيلات الملاحظات", "Notes preferences"), { back() })
            NeoLaunchPreferencesUi()
        }
        return
    }
    when (page) {
        "APPEARANCE" -> {
            NeoAppearanceStudio(
                onBack = { back() },
                onClassic = {
                    onUiChange(uiSettings.copy(experience = V11Experience.CLASSIC))
                    back()
                },
            )
            return
        }
        "SHORTCUTS" -> {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(neoSpacing()), verticalArrangement = Arrangement.spacedBy(NeoSpace.gap)) {
                NeoPageHeader(neoLabel("اختصارات المشغّل", "Launcher shortcuts"), { back() })
                NeoShortcutControls()
            }
            return
        }
        "WIDGETS" -> {
            Neo4WidgetStudio { back() }
            return
        }
        "BACKUP" -> {
            Neo4AccountBackupPage(
                db,
                financeAuthorized,
                securitySettings,
                securityStore,
                onAccountsUnlocked,
                onNeoRestored,
                { back() },
                { nav.open("LEGAL") },
            )
            return
        }
        "LEGAL" -> {
            Neo4LegalHub { back() }
            return
        }
        "SECURITY" -> {
            V11SecuritySettingsDialog(securityStore, securitySettings, { back() }, onSecurityChange)
            return
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(neoSpacing()),
        verticalArrangement = Arrangement.spacedBy(NeoSpace.gap),
    ) {
        if (page == "HOME") {
            OutlinedTextField(
                query,
                { query = it },
                label = { Text(neoLabel("بحث في الإعدادات", "Search settings")) },
                trailingIcon = { if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(Icons.Rounded.Close,neoLabel("مسح البحث","Clear search"))} },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (query.isBlank()) {
                val icons = listOf(
                    Icons.Rounded.Palette, Icons.Rounded.Language, Icons.Rounded.CalendarMonth,
                    Icons.Rounded.Notifications, Icons.Rounded.Widgets, Icons.Rounded.Apps, Icons.Rounded.Shield,
                    Icons.Rounded.CloudUpload, Icons.Rounded.Tune,
                )
                val summaries = listOf(
                    neoLabel("الثيمات والخطوط والخلفيات والحركة وشاشة البداية", "Themes, fonts, backgrounds, motion and start screen"),
                    neoLabel("لغة التطبيق واتجاه الواجهة والتنقل", "Application language, layout and navigation direction"),
                    neoLabel("التقويم والمهام والملاحظات والمستحقات والمالية", "Calendar, tasks, notes, salary and finances"),
                    neoLabel("التذكيرات وإشعارات التطبيق", "Reminders and app notifications"),
                    neoLabel("مظهر الويدجت", "Widget appearance"),
                    neoLabel("إضافة اختصارات الأقسام إلى الشاشة الرئيسية", "Pin section shortcuts to the home screen"),
                    neoLabel("PIN والبصمة وحماية الأقسام وإخفاء المبالغ", "PIN, biometrics, section locks and hidden amounts"),
                    neoLabel("النسخ والاستعادة وسلامة البيانات والتخزين", "Backup, restore, data health and storage"),
                    neoLabel("الإصدار والسياسات والأداء والتشخيص والتفضيلات القديمة", "Version, policies, performance, diagnostics and legacy settings"),
                )
                neoSettingsGroups.forEachIndexed { index, (id, ar, en) ->
                    when (index) {
                        0 -> Text(neoLabel("التخصيص العام", "General customization"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        2 -> Text(neoLabel("الأقسام والوظائف", "Sections and features"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        6 -> Text(neoLabel("الأمان والبيانات", "Security and data"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        8 -> Text(neoLabel("معلومات التطبيق", "App information"),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    NeoSettingsCompactTile(neoLabel(ar, en), summaries[index], icons[index]) {
                        when (id) {
                            "SHORTCUTS" -> page = id
                            "NOTIFICATIONS", "WIDGETS", "SECURITY" -> nav.open(id)
                            else -> page = "${id}_GROUP"
                        }
                    }
                }
            }
            else {
                val results = remember(query) { NeoSettingsSearch.search(query) }
                if (results.isEmpty()) {
                    Text(
                        neoLabel("لا توجد إعدادات مطابقة", "No matching settings"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                results.forEach { result ->
                    val groupNames = neoSettingsGroupNames(result.group)
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { if(result.destination == "SHORTCUTS") page = "SHORTCUTS" else nav.open(result.destination) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            neoLabel(result.ar, result.en),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            neoLabel(groupNames.first, groupNames.second),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        } else {
            val groupId = page.removeSuffix("_GROUP")
            val groupNames = neoSettingsGroupNames(groupId)
            val feature = NeoFeatureCatalog.get(page)
            NeoPageHeader(
                if (page.endsWith("_GROUP")) neoLabel(groupNames.first, groupNames.second)
                else feature?.let { neoLabel(it.ar, it.en) }.orEmpty(),
                { back() },
            )
            if (page == "SECTIONS_GROUP") {
                NeoSettingsHierarchy.sectionGroups.forEach { id ->
                    val names = neoSettingsGroupNames(id)
                    NeoSettingsRow(neoLabel(names.first, names.second)) { page = "${id}_GROUP" }
                }
            } else if (page.endsWith("_GROUP")) {
                if (groupId == "CASHBOOK") Text(
                    neoLabel("رسم الحسابات يُضبط هنا. تفضيلات أزرار الإدخال والقوالب تظل داخل خيارات Cash Book نفسها، دون إنشاء نسخة ثانية من الإعدادات.",
                        "Configure the finance chart here. Entry-button defaults and presets remain in Cash Book options, without duplicating stored settings."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (groupId == "SALARY") Text(
                    neoLabel("إدارة أسعار الورديات وكشوف المستحقات تتم داخل قسم مستحقات العمل. الروابط التالية تفتح الأدوات الأصلية دون تكرار البيانات أو الإعدادات.",
                        "Rates and statements are managed in Work Entitlements. These shortcuts open the original tools without duplicating data or preferences."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (groupId == "ADVANCED") com.exmore.app.appcore.AppCorePilotSettings(arabic = !LocalNeoEnglish.current)
                NeoSettingsHierarchy.children(groupId).forEachIndexed { index, id ->
                    if (groupId == "ADVANCED" && index == 0) Text(
                        neoLabel("حول التطبيق", "About the app"), style = MaterialTheme.typography.titleSmall)
                    if (groupId == "ADVANCED" && index == 2) Text(
                        neoLabel("الخيارات المتقدمة", "Advanced options"), style = MaterialTheme.typography.titleSmall)
                    val row = NeoFeatureCatalog.get(id)
                    if (row != null) NeoSettingsRow(neoLabel(row.ar, row.en)) {
                        if (id in NeoSettingsHierarchy.inlinePages) page = id
                        else nav.open(id)
                    }
                }
            } else
                when (page) {
                    "CALENDAR_DISPLAY" -> {
                        Neo4ThemePreview(p,p.fontScale)
                        NeoChoice(
                            neoLabel("العرض الافتراضي", "Default view"),
                            NeoCalendarView.entries,
                            p.calendarView,
                            { neoEnumLabel(it) },
                            { update(p.copy(calendarView = it)) },
                        )
                        NeoCalendarPreferences(p, update)
                        NeoSwitch(
                            neoLabel("دليل الألوان", "Calendar legend"),
                            p.showCalendarLegend,
                        ) {
                            update(p.copy(showCalendarLegend = it))
                        }
                        NeoSwitch(
                            neoLabel("ملاحظات الورديات في الجداول", "Shift notes in schedules"),
                            p.showShiftNotes,
                        ) {
                            update(p.copy(showShiftNotes = it))
                        }
                    }
                    "FIRST_WEEKDAY" -> NeoChoice(
                        neoLabel("أول يوم في الأسبوع", "First day of the week"),
                        listOf(6, 7, 1), p.firstWeekday,
                        { when (it) { 6 -> neoLabel("السبت", "Saturday"); 7 -> neoLabel("الأحد", "Sunday"); else -> neoLabel("الاثنين", "Monday") } },
                        { update(p.copy(firstWeekday = it)) },
                    )
                    "TODAY_STYLE" -> NeoTodayPreferences(p, update)
                    "PAINT_STYLE" -> Neo4PaintStudio(p, update)
                    "LAUNCH" -> NeoLandingPreferencesUi()
                    "PERFORMANCE" ->
                        NeoChoice(
                            neoLabel("وضع الأداء", "Performance mode"),
                            NeoPerformance.entries,
                            p.performance,
                            { neoEnumLabel(it) },
                            { update(p.copy(performance = it)) },
                        )
                    "FINANCE_DISPLAY" ->
                        NeoSwitch(neoLabel("رسم الحسابات", "Finance chart"), p.showFinanceChart) {
                            update(p.copy(showFinanceChart = it))
                        }
                    "TASK_OVERVIEW" -> {
                        NeoSwitch(
                            neoLabel("الترحيب داخل المهام", "Greeting in Tasks"),
                            p.showGreeting,
                        ) {
                            update(p.copy(showGreeting = it))
                        }
                        NeoSwitch(
                            neoLabel("ملخص الحسابات داخل المهام", "Finance summary in Tasks"),
                            p.showFinanceSummary,
                        ) {
                            update(p.copy(showFinanceSummary = it))
                        }
                        NeoSwitch(
                            neoLabel("فتح الملخص تلقائيًا", "Expand task overview"),
                            p.dashboardExpanded,
                        ) {
                            update(p.copy(dashboardExpanded = it))
                        }
                        NeoSwitch(
                            neoLabel("بطاقات إضافية", "Secondary cards"),
                            p.showSecondaryCards,
                        ) {
                            update(p.copy(showSecondaryCards = it))
                        }
                        p.dashboardOrder.forEach { card ->
                            NeoSwitch(neoCardLabel(card), p.cardVisible(card)) {
                                update(p.withDashboardCard(card, it))
                            }
                        }
                        p.dashboardOrder.forEachIndexed { index, card ->
                            Row(Modifier.fillMaxWidth()) {
                                Text(neoCardLabel(card), modifier = Modifier.weight(1f))
                                TextButton(
                                    enabled = index > 0,
                                    onClick = {
                                        val list = p.dashboardOrder.toMutableList()
                                        java.util.Collections.swap(list, index, index - 1)
                                        update(p.copy(dashboardOrder = list))
                                    },
                                ) {
                                    Text(neoLabel("أعلى", "Up"))
                                }
                            }
                        }
                        NeoQuickAction.entries.forEach { action ->
                            NeoSwitch(neoActionLabel(action), action in p.quickActions) { show ->
                                update(
                                    p.copy(
                                        quickActions =
                                            if (show) p.quickActions + action
                                            else p.quickActions - action
                                    )
                                )
                            }
                        }
                    }
                    "LEGACY" -> {
                        Text(
                            neoLabel(
                                "تُحفظ إعدادات Classic بشكل مستقل",
                                "Classic preferences remain stored independently",
                            )
                        )
                        OutlinedButton(
                            onClick = {
                                onUiChange(uiSettings.copy(experience = V11Experience.CLASSIC))
                                back()
                            }
                        ) {
                            Text(neoLabel("فتح Classic", "Open Classic"))
                        }
                    }
                    "ABOUT" -> {
                        Text(
                            "ExMore — Life Planner • ${com.exmore.app.BuildConfig.VERSION_NAME} (${com.exmore.app.BuildConfig.VERSION_CODE})"
                        )
                        Text(
                            "${com.exmore.app.BuildConfig.APPLICATION_ID} • Schema ${db.schemaVersion}"
                        )
                    }
                }
        }
    }
}
