package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
            OutlinedTextField(
                query,
                { query = it },
                label = { Text(neoLabel("بحث في الإعدادات", "Search settings")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (query.isBlank())
                neoSettingsGroups.forEach { (id, ar, en) ->
                    NeoSettingsRow(neoLabel(ar, en)) {
                        if (id == "APPEARANCE") nav.open("APPEARANCE") else page = id + "_GROUP"
                    }
                }
            else
                NeoFeatureCatalog.search(query).forEach { feature ->
                    NeoSettingsRow(neoLabel(feature.ar, feature.en)) { nav.open(feature.id) }
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
                    "LANGUAGE" ->
                        NeoChoice(
                            neoLabel("لغة التطبيق", "App language"),
                            NeoLanguage.entries,
                            p.language,
                            { neoEnumLabel(it) },
                            { update(p.copy(language = it)) },
                        )
                    "UI_DIRECTION",
                    "NAV_DIRECTION",
                    "GRID_DIRECTION",
                    "WEEKDAY_DIRECTION" -> {
                        val value =
                            when (page) {
                                "UI_DIRECTION" -> p.uiDirection
                                "NAV_DIRECTION" -> p.navigationDirection
                                "GRID_DIRECTION" -> p.gridDirection
                                else -> p.weekdayDirection
                            }
                        NeoChoice(
                            feature?.let { neoLabel(it.ar, it.en) }.orEmpty(),
                            NeoDirection.entries,
                            value,
                            { neoEnumLabel(it) },
                        ) { direction ->
                            update(
                                when (page) {
                                    "UI_DIRECTION" -> p.copy(uiDirection = direction)
                                    "NAV_DIRECTION" -> p.copy(navigationDirection = direction)
                                    "GRID_DIRECTION" -> p.copy(gridDirection = direction)
                                    else -> p.copy(weekdayDirection = direction)
                                }
                            )
                        }
                    }
                    "FIRST_WEEKDAY" ->
                        NeoChoice(
                            neoLabel("أول يوم", "First weekday"),
                            listOf(6, 7, 1),
                            p.firstWeekday,
                            {
                                when (it) {
                                    6 -> neoLabel("السبت", "Saturday")
                                    7 -> neoLabel("الأحد", "Sunday")
                                    else -> neoLabel("الاثنين", "Monday")
                                }
                            },
                            { update(p.copy(firstWeekday = it)) },
                        )
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
