package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.R
import com.todoshift.app.v11.*

private val LocalNeoSettingsQuery = staticCompositionLocalOf { "" }

private enum class V11SettingsPage {
    HOME,
    CALENDAR_WORK,
    FINANCE,
    APPEARANCE,
    PRIVACY,
    BACKUP,
    SYNC,
    ABOUT,
    WIDGETS,
    LEGAL,
    LANGUAGE,
}

@Composable
fun V11MoreScreen(
    db: V11Database,
    workplaces: List<com.todoshift.app.data.Workplace> = emptyList(),
    uiSettings: V11UiSettings,
    moneyVisible: Boolean,
    securitySettings: V11SecuritySettings,
    securityStore: V11SecurityStore,
    onSecuritySettingsChange: (V11SecuritySettings) -> Unit,
    onDatabaseRestored: (V11Database) -> Unit,
    onUiSettingsChange: (V11UiSettings) -> Unit,
    onToggleMoney: () -> Unit,
    onOpenWorkplaces: () -> Unit,
    financeAuthorized: Boolean = false,
    onAccountsUnlocked: () -> Unit = {},
    onNeoRestored: (com.todoshift.app.backup.Neo4RestoredState) -> Unit = {},
) {
    if (LocalV11NeoEnabled.current) {
        NeoRescueSettings(
            db,
            uiSettings,
            securitySettings,
            securityStore,
            financeAuthorized,
            onAccountsUnlocked,
            onSecuritySettingsChange,
            onUiSettingsChange,
            onNeoRestored,
        )
        return
    }
    val navigateDate = LocalNeoNavigateDate.current
    var toolsOpen by remember { mutableStateOf(false) }
    if (toolsOpen) {
        BackHandler { toolsOpen = false }
        NeoModernHub(
            db,
            workplaces,
            financeAuthorized,
            moneyVisible,
            onDatabaseRestored,
            { date ->
                toolsOpen = false
                navigateDate(date)
            },
            { toolsOpen = false },
        )
        return
    }
    var page by remember { mutableStateOf(V11SettingsPage.HOME) }
    var securityOpen by remember { mutableStateOf(false) }
    var studioSection by remember { mutableStateOf(NeoStudioSection.THEMES) }
    var backupOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    BackHandler(enabled = page != V11SettingsPage.HOME && !securityOpen && !backupOpen) {
        page = V11SettingsPage.HOME
    }

    if (uiSettings.experience == V11Experience.PREMIUM && page == V11SettingsPage.APPEARANCE) {
        NeoAppearanceStudio(
            initialSection = studioSection,
            onBack = { page = V11SettingsPage.HOME },
            onClassic = {
                onUiSettingsChange(uiSettings.copy(experience = V11Experience.CLASSIC))
                page = V11SettingsPage.HOME
            },
        )
        return
    }

    if (
        page == V11SettingsPage.BACKUP ||
            (uiSettings.experience == V11Experience.PREMIUM && page == V11SettingsPage.SYNC)
    ) {
        Neo4AccountBackupPage(
            db,
            financeAuthorized,
            securitySettings,
            securityStore,
            onAccountsUnlocked,
            onNeoRestored,
            onBack = { page = V11SettingsPage.HOME },
            onPolicy = { page = V11SettingsPage.LEGAL },
        )
        return
    }
    if (page == V11SettingsPage.WIDGETS) {
        Neo4WidgetStudio { page = V11SettingsPage.HOME }
        return
    }
    if (page == V11SettingsPage.LEGAL) {
        Neo4LegalHub { page = V11SettingsPage.HOME }
        return
    }

    when (page) {
        V11SettingsPage.WIDGETS,
        V11SettingsPage.LEGAL -> Unit
        V11SettingsPage.LANGUAGE -> {
            SettingsSubPage(
                title = neoLiteral("اللغة والاتجاه", "Language and direction"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                val p = LocalNeoAppearance.current
                val update = LocalNeoOnChange.current
                NeoChoice(
                    neoLabel("اللغة", "Language"),
                    NeoLanguage.entries,
                    p.language,
                    { neoEnumLabel(it) },
                    { update(p.copy(language = it)) },
                )
                NeoDirectionPreferences(p, update)
            }
        }
        V11SettingsPage.HOME -> {
            SettingsPageContainer(title = neoLiteral("الإعدادات", "Settings")) {
                if (uiSettings.experience == V11Experience.PREMIUM) {
                    V11NeoSectionHero(
                        neoLabel(neoLiteral("تخصيص تجربتك", "Make it yours"), "Make it yours"),
                        neoLabel(
                            "Neo Luxury 4.1 Ultimate • المظهر والخصوصية",
                            "Neo Luxury 4.1 Ultimate • appearance and privacy",
                        ),
                    )
                    SettingsAction(
                        neoLabel(
                            neoLiteral("استوديو المظهر", "Appearance Studio"),
                            "Appearance Studio",
                        ),
                        neoLabel(
                            "الثيمات • الخطوط • التخطيط • الحركة • اللغة",
                            "Themes • typography • layout • motion • language",
                        ),
                    ) {
                        studioSection = NeoStudioSection.THEMES
                        page = V11SettingsPage.APPEARANCE
                    }
                    SettingsAction(
                        neoLabel("استوديو الويدجت", "Widget Studio"),
                        neoLabel(
                            "ألوان وخصوصية وأحجام الشاشة الرئيسية",
                            "Launcher appearance and privacy",
                        ),
                    ) {
                        page = V11SettingsPage.WIDGETS
                    }
                    SettingsAction(
                        neoLabel("الخصوصية والسياسات", "Privacy & Legal"),
                        neoLabel(
                            "الخصوصية والاستخدام والنسخ وبيانات Google",
                            "Privacy, terms, backup and Google data",
                        ),
                    ) {
                        page = V11SettingsPage.LEGAL
                    }
                    SettingsAction(
                        neoLabel(neoLiteral("الإشعارات", "Notifications"), "Notifications"),
                        neoLabel(
                            "إدارة إشعارات ToDo Shift من إعدادات الهاتف",
                            "Manage supported notifications in Android settings",
                        ),
                    ) {
                        context.startActivity(
                            android.content
                                .Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(
                                    android.provider.Settings.EXTRA_APP_PACKAGE,
                                    context.packageName,
                                )
                        )
                    }
                }
                SettingsAction(
                    neoLiteral("اللغة والاتجاه", "Language and direction"),
                    neoLiteral(
                        "لغة مستقلة عن اتجاه الواجهة والتقويم",
                        "Language independent from UI and calendar directions",
                    ),
                ) {
                    page = V11SettingsPage.LANGUAGE
                }
                SettingsAction(
                    neoLiteral("الأدوات الذكية والبحث", "Smart tools and search"),
                    neoLiteral(
                        "نسخ وقوائم وأسعار وكشوف ومرفقات",
                        "Copy, templates, rates, statements and attachments",
                    ),
                ) {
                    toolsOpen = true
                }
                Text(
                    neoLiteral("التنظيم والعمل", "Organization and work"),
                    style = MaterialTheme.typography.titleSmall,
                )
                SettingsAction(
                    neoLiteral("التقويم والعمل", "Calendar and work"),
                    neoLiteral("أماكن العمل والأسعار والتقاويم", "Workplaces, rates and calendars"),
                ) {
                    page = V11SettingsPage.CALENDAR_WORK
                }

                SettingsAction(
                    neoLiteral("الحسابات والالتزامات", "Finance and obligations"),
                    neoLiteral(
                        "المبالغ ومصادر الدخل وطرق الدفع والالتزامات",
                        "Amounts, income sources, payment methods and obligations",
                    ),
                ) {
                    page = V11SettingsPage.FINANCE
                }

                if (uiSettings.experience == V11Experience.CLASSIC) {
                    SettingsAction(
                        neoLiteral("المظهر والتخصيص", "Appearance and customization"),
                        neoLiteral(
                            "طريقة عرض التقويم وزر الإضافة وتمييز اليوم",
                            "Calendar layout, add button and today's highlight",
                        ),
                    ) {
                        page = V11SettingsPage.APPEARANCE
                    }
                }
                Text(
                    neoLiteral("البيانات والخصوصية", "Data and privacy"),
                    style = MaterialTheme.typography.titleSmall,
                )
                SettingsAction(
                    neoLiteral("الأمان والخصوصية", "Privacy and security"),
                    securitySummary(securitySettings),
                ) {
                    page = V11SettingsPage.PRIVACY
                }

                SettingsAction(
                    neoLiteral("النسخ الاحتياطي والاستعادة", "Backup and restore"),
                    neoLiteral(
                        "تصدير واستعادة بيانات ToDo Shift",
                        "Export and restore ToDo Shift data",
                    ),
                ) {
                    page = V11SettingsPage.BACKUP
                }

                SettingsAction(neoLiteral("الحساب والمزامنة", "Account and sync"), "Google Sync") {
                    page = V11SettingsPage.SYNC
                }

                SettingsAction(
                    neoLiteral("حول التطبيق", "About"),
                    "ToDo Shift ${com.todoshift.app.BuildConfig.VERSION_NAME} (${com.todoshift.app.BuildConfig.VERSION_CODE})",
                ) {
                    page = V11SettingsPage.ABOUT
                }
            }
        }

        V11SettingsPage.CALENDAR_WORK -> {
            SettingsSubPage(
                title = neoLiteral("التقويم والعمل", "Calendar and work"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                SettingsAction(
                    neoLiteral("أماكن العمل والأسعار", "Workplaces and rates"),
                    neoLiteral(
                        "إدارة الأماكن والألوان وأسعار الشيفتات",
                        "Manage workplaces, colors and shift rates",
                    ),
                    onOpenWorkplaces,
                )

                if (uiSettings.experience == V11Experience.PREMIUM) {
                    SettingsAction(
                        neoLabel("تفضيلات التقويم", "Calendar preferences"),
                        neoLabel(
                            "شهر وأسبوع ويوم وكثافة الخلايا",
                            "Month, week, day and cell density",
                        ),
                    ) {
                        studioSection = NeoStudioSection.CALENDAR
                        page = V11SettingsPage.APPEARANCE
                    }
                }

                SettingsInfo(
                    neoLiteral("التقاويم", "Calendars"),
                    neoLiteral(
                        "${db.calendars.size} تقويم • ${db.calendars.count { it.visible }} ظاهر",
                        "${db.calendars.size} calendars • ${db.calendars.count { it.visible }} visible",
                    ),
                )
            }
        }

        V11SettingsPage.FINANCE -> {
            SettingsSubPage(
                title = neoLiteral("الحسابات والالتزامات", "Finance and obligations"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                if (uiSettings.experience == V11Experience.PREMIUM) {
                    val appearance = LocalNeoAppearance.current
                    val update = LocalNeoOnChange.current
                    NeoSwitch(
                        neoLabel("عرض المؤشر المالي", "Show financial chart"),
                        appearance.showFinanceChart,
                    ) {
                        update(appearance.copy(showFinanceChart = it))
                    }
                }

                SettingsInfo(
                    neoLiteral("مصادر الدخل", "Income sources"),
                    neoLiteral("${db.incomeSources.size} مصدر", "${db.incomeSources.size} sources"),
                )

                SettingsInfo(
                    neoLiteral("طرق الدفع", "Payment methods"),
                    neoLiteral(
                        "${db.paymentMethods.size} طريقة",
                        "${db.paymentMethods.size} methods",
                    ),
                )

                SettingsInfo(
                    neoLiteral("الالتزامات", "Obligations"),
                    neoLiteral(
                        "${db.obligations.count { it.status == ObligationStatusV11.ACTIVE }} التزام نشط",
                        "${db.obligations.count { it.status == ObligationStatusV11.ACTIVE }} active obligations",
                    ),
                )
            }
        }

        V11SettingsPage.APPEARANCE -> {
            SettingsSubPage(
                title = neoLiteral("المظهر والتخصيص", "Appearance and customization"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                SettingsChoiceCard(
                    title = stringResource(R.string.premium_settings_experience),
                    options =
                        listOf(
                            V11Experience.CLASSIC to
                                stringResource(R.string.premium_settings_classic),
                            V11Experience.PREMIUM to
                                stringResource(R.string.premium_settings_premium),
                        ),
                    selected = uiSettings.experience,
                    onSelected = { onUiSettingsChange(uiSettings.copy(experience = it)) },
                )

                if (uiSettings.experience == V11Experience.PREMIUM) {
                    SettingsChoiceCard(
                        title = stringResource(R.string.premium_settings_brightness),
                        options =
                            listOf(
                                V11PremiumBrightness.SYSTEM to
                                    stringResource(R.string.premium_settings_system),
                                V11PremiumBrightness.LIGHT to
                                    stringResource(R.string.premium_settings_light),
                                V11PremiumBrightness.DARK to
                                    stringResource(R.string.premium_settings_dark),
                            ),
                        selected = uiSettings.premiumBrightness,
                        onSelected = { onUiSettingsChange(uiSettings.copy(premiumBrightness = it)) },
                    )
                }

                SettingsChoiceCard(
                    title = neoLiteral("ثيم التطبيق", "App theme"),
                    options =
                        listOf(
                            V11AppTheme.TODO_SHIFT to "ToDo Shift",
                            V11AppTheme.MIDNIGHT to "Midnight",
                            V11AppTheme.OCEAN to "Ocean",
                            V11AppTheme.FOREST to "Forest",
                            V11AppTheme.SAND to "Sand",
                            V11AppTheme.MONO to "Mono",
                        ),
                    selected = uiSettings.appTheme,
                    onSelected = { onUiSettingsChange(uiSettings.copy(appTheme = it)) },
                )

                SettingsChoiceCard(
                    title = neoLiteral("شكل التقويم", "Calendar style"),
                    options =
                        listOf(
                            V11CalendarStyle.CLASSIC to "Classic",
                            V11CalendarStyle.COMPACT to "Compact",
                            V11CalendarStyle.CARDS to "Cards",
                        ),
                    selected = uiSettings.calendarStyle,
                    onSelected = { onUiSettingsChange(uiSettings.copy(calendarStyle = it)) },
                )

                SettingsChoiceCard(
                    title = neoLiteral("ثيم الويدجت", "Widget theme"),
                    options =
                        listOf(
                            V11WidgetTheme.FOLLOW_APP to neoLiteral("يتبع التطبيق", "Follow app"),
                            V11WidgetTheme.LIGHT to neoLiteral("فاتح", "Light"),
                            V11WidgetTheme.DARK to neoLiteral("داكن", "Dark"),
                            V11WidgetTheme.TRANSPARENT to neoLiteral("شفاف", "Transparent"),
                        ),
                    selected = uiSettings.widgetTheme,
                    onSelected = { onUiSettingsChange(uiSettings.copy(widgetTheme = it)) },
                )

                SettingsSwitch(
                    title = neoLiteral("أسماء أماكن العمل", "Workplace names"),
                    subtitle =
                        neoLiteral(
                            "إظهار اسم مكان العمل داخل الشيفت في التقويم",
                            "Show workplace names inside calendar shifts",
                        ),
                    checked = uiSettings.showWorkplaceNames,
                    onCheckedChange = {
                        onUiSettingsChange(uiSettings.copy(showWorkplaceNames = it))
                    },
                )

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            neoLiteral("مكان زر الإضافة", "Add button position"),
                            fontWeight = FontWeight.Bold,
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = uiSettings.fabSide == FabSide.RIGHT,
                                onClick = {
                                    onUiSettingsChange(uiSettings.copy(fabSide = FabSide.RIGHT))
                                },
                                label = { Text(neoLiteral("يمين", "Right")) },
                            )

                            FilterChip(
                                selected = uiSettings.fabSide == FabSide.LEFT,
                                onClick = {
                                    onUiSettingsChange(uiSettings.copy(fabSide = FabSide.LEFT))
                                },
                                label = { Text(neoLiteral("يسار", "Left")) },
                            )
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            neoLiteral("تمييز اليوم الحالي", "Highlight today"),
                            fontWeight = FontWeight.Bold,
                        )

                        listOf(
                                TodayHighlightStyle.BORDER,
                                TodayHighlightStyle.TINT,
                                TodayHighlightStyle.RING,
                                TodayHighlightStyle.BORDER_TINT,
                                TodayHighlightStyle.NONE,
                            )
                            .forEach { style ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = uiSettings.todayHighlightStyle == style,
                                        onClick = {
                                            onUiSettingsChange(
                                                uiSettings.copy(todayHighlightStyle = style)
                                            )
                                        },
                                    )

                                    Text(highlightLabel(style))
                                }
                            }
                    }
                }
            }
        }

        V11SettingsPage.PRIVACY -> {
            SettingsSubPage(
                title = neoLiteral("الأمان والخصوصية", "Privacy and security"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                SettingsAction(
                    neoLiteral("إعدادات الحماية", "Security settings"),
                    securitySummary(securitySettings),
                ) {
                    securityOpen = true
                }
            }
        }

        V11SettingsPage.BACKUP -> {
            SettingsSubPage(
                title = neoLiteral("النسخ الاحتياطي والاستعادة", "Backup and restore"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                SettingsAction(
                    neoLiteral("فتح النسخ الاحتياطي والاستعادة", "Open backup and restore"),
                    "Schema ${db.schemaVersion}",
                ) {
                    backupOpen = true
                }
            }
        }

        V11SettingsPage.SYNC -> {
            SettingsSubPage(
                title = neoLiteral("الحساب والمزامنة", "Account and sync"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                SettingsInfo(
                    "Google Sync",
                    neoLiteral(
                        "غير مفعّل حاليا — لن يظهر كمفتاح وهمي قبل تنفيذ المزامنة الحقيقية",
                        "Not currently available. Sync will appear when implemented.",
                    ),
                )
            }
        }

        V11SettingsPage.ABOUT -> {
            SettingsSubPage(
                title = neoLiteral("حول التطبيق", "About"),
                onBack = { page = V11SettingsPage.HOME },
            ) {
                SettingsInfo(
                    "ToDo Shift",
                    "${com.todoshift.app.BuildConfig.VERSION_NAME} (${com.todoshift.app.BuildConfig.VERSION_CODE}) • Schema ${db.schemaVersion}\n${com.todoshift.app.BuildConfig.APPLICATION_ID}",
                )
            }
        }
    }

    if (backupOpen) {
        V11BackupRestoreDialog(
            context = context,
            db = db,
            onDatabaseRestored = onDatabaseRestored,
            onDismiss = { backupOpen = false },
        )
    }

    if (securityOpen) {
        V11SecuritySettingsDialog(
            store = securityStore,
            current = securitySettings,
            onDismiss = { securityOpen = false },
            onChanged = onSecuritySettingsChange,
        )
    }
}

@Composable
private fun SettingsPageContainer(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // Main section title is rendered once by V11GlobalHeader.
        // Keep this container focused on the settings content.
        if (LocalV11NeoEnabled.current) {
            var query by remember { mutableStateOf("") }
            OutlinedTextField(
                query,
                { query = it },
                label = { Text(neoLabel("ابحث في الإعدادات", "Search settings")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            CompositionLocalProvider(LocalNeoSettingsQuery provides query) { content() }
        } else content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = neoLiteral("رجوع", "Back"),
                )
            }

            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }

        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun <T> SettingsChoiceCard(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)

            options.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selected == option.first,
                        onClick = { onSelected(option.first) },
                    )

                    Text(option.second, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun securitySummary(securitySettings: V11SecuritySettings): String =
    when {
        !securitySettings.protectAccounts -> neoLiteral("بدون قفل", "No lock")

        securitySettings.mode == V11LockMode.PIN && securitySettings.biometricEnabled ->
            neoLiteral("PIN + بصمة • قفل تلقائي", "PIN + biometrics • auto-lock")

        securitySettings.mode == V11LockMode.PATTERN && securitySettings.biometricEnabled ->
            neoLiteral("نمط + بصمة • قفل تلقائي", "Pattern + biometrics • auto-lock")

        securitySettings.mode == V11LockMode.PIN ->
            neoLiteral("PIN • قفل تلقائي", "PIN • auto-lock")

        securitySettings.mode == V11LockMode.PATTERN ->
            neoLiteral("نمط • قفل تلقائي", "Pattern • auto-lock")

        securitySettings.mode == V11LockMode.NONE && securitySettings.biometricEnabled ->
            neoLiteral("بصمة / قفل الجهاز • قفل تلقائي", "Biometrics / device lock • auto-lock")

        else -> neoLiteral("بدون قفل", "No lock")
    }

@Composable
private fun SettingsInfo(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(2.dp))

            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsAction(title: String, subtitle: String, onClick: () -> Unit) {
    val query = LocalNeoSettingsQuery.current
    if (
        LocalV11NeoEnabled.current &&
            query.isNotBlank() &&
            !title.contains(query, ignoreCase = true) &&
            !subtitle.contains(query, ignoreCase = true)
    )
        return
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, fontWeight = FontWeight.Bold)

                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Switch(checked = checked, onCheckedChange = onCheckedChange)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(title, fontWeight = FontWeight.Bold)

                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun highlightLabel(style: TodayHighlightStyle): String =
    when (style) {
        TodayHighlightStyle.BORDER -> neoLiteral("إطار", "Border")
        TodayHighlightStyle.TINT -> neoLiteral("تظليل", "Tint")
        TodayHighlightStyle.RING -> neoLiteral("دائرة", "Ring")
        TodayHighlightStyle.BORDER_TINT -> neoLiteral("إطار وتظليل", "Border and tint")
        TodayHighlightStyle.NONE -> neoLiteral("بدون", "None")
        else -> neoLiteral("نبض", "Pulse")
    }
