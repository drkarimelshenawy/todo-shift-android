package com.todoshift.app.v11.ui

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
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

val neoSettingsGroups =
    listOf(
        Triple("APPEARANCE", "المظهر والتصميم", "Appearance & Design"),
        Triple("LANGUAGE", "اللغة والاتجاه", "Language & Direction"),
        Triple("CALENDAR", "التقويم والورديات", "Calendar & Shifts"),
        Triple("NOTIFICATIONS", "التنبيهات والويدجت", "Notifications & Widgets"),
        Triple("PRIVACY", "الخصوصية والبيانات", "Privacy & Data"),
        Triple("ADVANCED", "الخيارات المتقدمة", "Advanced"),
    )

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
    onNeoRestored: (com.todoshift.app.backup.Neo4RestoredState) -> Unit,
    initialPage: String = "HOME",
    onBack: () -> Unit = {},
) {
    var page by rememberSaveable(initialPage) { mutableStateOf(initialPage) }
    var query by rememberSaveable { mutableStateOf("") }
    val nav = LocalNeoFeatures.current
    val p = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    fun back() {
        if (initialPage == "HOME" && page != "HOME") page = "HOME" else onBack()
    }
    BackHandler(page != "HOME") { back() }
    if (page in neoLanguageRoutes) {
        NeoLanguageSettings(page) { back() }
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
            NeoPremiumBanner(neoLabel("الإعدادات والتخصيص", "Settings & customization"))
            OutlinedTextField(
                query,
                { query = it },
                label = { Text(neoLabel("بحث في الإعدادات", "Search settings")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (query.isBlank()) {
                val icons = listOf(Icons.Rounded.Palette, Icons.Rounded.Language, Icons.Rounded.CalendarMonth,
                    Icons.Rounded.Notifications, Icons.Rounded.Shield, Icons.Rounded.Tune)
                val summaries = listOf(
                    neoLabel("الألوان، الخطوط، كثافة العرض والحركة", "Colors, typography, density and motion"),
                    neoLabel("لغة التطبيق واتجاه الواجهة والتقويم", "Language, layout and calendar direction"),
                    neoLabel("أماكن العمل، الورديات وخلايا التقويم", "Workplaces, shifts and calendar cells"),
                    neoLabel("التذكيرات والاختصارات والويدجت", "Reminders, shortcuts and widgets"),
                    neoLabel("النسخ الاحتياطي، الاستعادة وقفل الحسابات", "Backup, restore and finance lock"),
                    neoLabel("الأداء والتشخيص والخيارات المحفوظة", "Performance, diagnostics and saved preferences"))
                NeoPremiumSettingTile(neoLabel("النسخ الاحتياطي والاستعادة", "Backup & restore"),
                    neoLabel("حفظ بياناتك واستعادتها", "Save and restore your data"), Icons.Rounded.CloudUpload) { nav.open("BACKUP") }
                neoSettingsGroups.forEachIndexed { index, (id, ar, en) ->
                    NeoPremiumSettingTile(neoLabel(ar, en), summaries[index], icons[index]) {
                        if (id == "APPEARANCE") nav.open("APPEARANCE") else page = id + "_GROUP"
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
                    val group = neoSettingsGroups.firstOrNull { it.first == result.group }
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { nav.open(result.destination) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            neoLabel(result.ar, result.en),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            group?.let { neoLabel(it.second, it.third) }.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
            }
        } else {
            val groupId = page.removeSuffix("_GROUP")
            val group = neoSettingsGroups.firstOrNull { it.first == groupId }
            val feature = NeoFeatureCatalog.get(page)
            NeoPageHeader(
                if (page.endsWith("_GROUP")) group?.let { neoLabel(it.second, it.third) }.orEmpty()
                else feature?.let { neoLabel(it.ar, it.en) }.orEmpty(),
                { back() },
            )
            if (page.endsWith("_GROUP")) {
                NeoFeatureCatalog.entries
                    .filter { it.group == groupId }
                    .forEach { row ->
                        NeoSettingsRow(neoLabel(row.ar, row.en)) { nav.open(row.id) }
                    }
            } else
                when (page) {
                    "CALENDAR_DISPLAY" -> {
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
                    }
                    "TODAY_STYLE" -> NeoTodayPreferences(p, update)
                    "PAINT_STYLE" -> Neo4PaintStudio(p, update)
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
                        NeoSwitch(
                            neoLabel("ملاحظات الورديات في الجداول", "Shift notes in schedules"),
                            p.showShiftNotes,
                        ) {
                            update(p.copy(showShiftNotes = it))
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
                            "ToDo Shift ${com.todoshift.app.BuildConfig.VERSION_NAME} (${com.todoshift.app.BuildConfig.VERSION_CODE})"
                        )
                        Text(
                            "${com.todoshift.app.BuildConfig.APPLICATION_ID} • Schema ${db.schemaVersion}"
                        )
                        NeoSettingsRow(neoLabel("سلامة البيانات", "Data health")) {
                            nav.open("HEALTH")
                        }
                    }
                }
        }
    }
}
