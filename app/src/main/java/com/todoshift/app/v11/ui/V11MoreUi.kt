package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

private enum class V11SettingsPage {
    HOME,
    CALENDAR_WORK,
    FINANCE,
    APPEARANCE,
    PRIVACY,
    BACKUP,
    SYNC,
    ABOUT
}

@Composable
fun V11MoreScreen(
    db: V11Database,
    uiSettings: V11UiSettings,
    moneyVisible: Boolean,
    securitySettings: V11SecuritySettings,
    securityStore: V11SecurityStore,
    onSecuritySettingsChange: (V11SecuritySettings) -> Unit,
    onDatabaseRestored: (V11Database) -> Unit,
    onUiSettingsChange: (V11UiSettings) -> Unit,
    onToggleMoney: () -> Unit,
    onOpenWorkplaces: () -> Unit
) {
    var page by remember { mutableStateOf(V11SettingsPage.HOME) }
    var securityOpen by remember { mutableStateOf(false) }
    var backupOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    when (page) {
        V11SettingsPage.HOME -> {
            SettingsPageContainer(title = "الإعدادات") {
                SettingsAction(
                    "التقويم والعمل",
                    "أماكن العمل والأسعار والتقاويم"
                ) { page = V11SettingsPage.CALENDAR_WORK }

                SettingsAction(
                    "الحسابات والالتزامات",
                    "المبالغ ومصادر الدخل وطرق الدفع والالتزامات"
                ) { page = V11SettingsPage.FINANCE }

                SettingsAction(
                    "المظهر والتخصيص",
                    "طريقة عرض التقويم وزر الإضافة وتمييز اليوم"
                ) { page = V11SettingsPage.APPEARANCE }

                SettingsAction(
                    "الأمان والخصوصية",
                    securitySummary(securitySettings)
                ) { page = V11SettingsPage.PRIVACY }

                SettingsAction(
                    "النسخ الاحتياطي والاستعادة",
                    "تصدير واستعادة بيانات ToDo Shift"
                ) { page = V11SettingsPage.BACKUP }

                SettingsAction(
                    "الحساب والمزامنة",
                    "Google Sync"
                ) { page = V11SettingsPage.SYNC }

                SettingsAction(
                    "حول التطبيق",
                    "ToDo Shift V1.1"
                ) { page = V11SettingsPage.ABOUT }
            }
        }

        V11SettingsPage.CALENDAR_WORK -> {
            SettingsSubPage(
                title = "التقويم والعمل",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsAction(
                    "أماكن العمل والأسعار",
                    "إدارة الأماكن والألوان وأسعار الشيفتات",
                    onOpenWorkplaces
                )

                SettingsInfo(
                    "التقاويم",
                    "${db.calendars.size} تقويم • ${db.calendars.count { it.visible }} ظاهر"
                )
            }
        }

        V11SettingsPage.FINANCE -> {
            SettingsSubPage(
                title = "الحسابات والالتزامات",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsSwitch(
                    title = "إظهار المبالغ",
                    subtitle = "إخفاء أو إظهار القيم المالية داخل الحسابات",
                    checked = moneyVisible,
                    onCheckedChange = {
                        if (it != moneyVisible) onToggleMoney()
                    }
                )

                SettingsInfo(
                    "مصادر الدخل",
                    "${db.incomeSources.size} مصدر"
                )

                SettingsInfo(
                    "طرق الدفع",
                    "${db.paymentMethods.size} طريقة"
                )

                SettingsInfo(
                    "الالتزامات",
                    "${db.obligations.count { it.status == ObligationStatusV11.ACTIVE }} التزام نشط"
                )
            }
        }

        V11SettingsPage.APPEARANCE -> {
            SettingsSubPage(
                title = "المظهر والتخصيص",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsSwitch(
                    title = "أسماء أماكن العمل",
                    subtitle = "إظهار اسم مكان العمل داخل الشيفت في التقويم",
                    checked = uiSettings.showWorkplaceNames,
                    onCheckedChange = {
                        onUiSettingsChange(
                            uiSettings.copy(showWorkplaceNames = it)
                        )
                    }
                )

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "مكان زر الإضافة",
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected =
                                    uiSettings.fabSide == FabSide.RIGHT,
                                onClick = {
                                    onUiSettingsChange(
                                        uiSettings.copy(
                                            fabSide = FabSide.RIGHT
                                        )
                                    )
                                },
                                label = { Text("يمين") }
                            )

                            FilterChip(
                                selected =
                                    uiSettings.fabSide == FabSide.LEFT,
                                onClick = {
                                    onUiSettingsChange(
                                        uiSettings.copy(
                                            fabSide = FabSide.LEFT
                                        )
                                    )
                                },
                                label = { Text("يسار") }
                            )
                        }
                    }
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "تمييز اليوم الحالي",
                            fontWeight = FontWeight.Bold
                        )

                        listOf(
                            TodayHighlightStyle.BORDER,
                            TodayHighlightStyle.TINT,
                            TodayHighlightStyle.RING,
                            TodayHighlightStyle.BORDER_TINT,
                            TodayHighlightStyle.NONE
                        ).forEach { style ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected =
                                        uiSettings.todayHighlightStyle == style,
                                    onClick = {
                                        onUiSettingsChange(
                                            uiSettings.copy(
                                                todayHighlightStyle = style
                                            )
                                        )
                                    }
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
                title = "الأمان والخصوصية",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsAction(
                    "إعدادات الحماية",
                    securitySummary(securitySettings)
                ) {
                    securityOpen = true
                }
            }
        }

        V11SettingsPage.BACKUP -> {
            SettingsSubPage(
                title = "النسخ الاحتياطي والاستعادة",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsAction(
                    "فتح النسخ الاحتياطي والاستعادة",
                    "Schema ${db.schemaVersion}"
                ) {
                    backupOpen = true
                }
            }
        }

        V11SettingsPage.SYNC -> {
            SettingsSubPage(
                title = "الحساب والمزامنة",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsInfo(
                    "Google Sync",
                    "غير مفعّل حاليا — لن يظهر كمفتاح وهمي قبل تنفيذ المزامنة الحقيقية"
                )
            }
        }

        V11SettingsPage.ABOUT -> {
            SettingsSubPage(
                title = "حول التطبيق",
                onBack = { page = V11SettingsPage.HOME }
            ) {
                SettingsInfo(
                    "ToDo Shift",
                    "V1.1 • Schema ${db.schemaVersion}"
                )
            }
        }
    }

    if (backupOpen) {
        V11BackupRestoreDialog(
            context = context,
            db = db,
            onDatabaseRestored = onDatabaseRestored,
            onDismiss = { backupOpen = false }
        )
    }

    if (securityOpen) {
        V11SecuritySettingsDialog(
            store = securityStore,
            current = securitySettings,
            onDismiss = { securityOpen = false },
            onChanged = onSecuritySettingsChange
        )
    }
}

@Composable
private fun SettingsPageContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(2.dp))
        content()
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "رجوع"
                )
            }

            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        }

        content()
        Spacer(Modifier.height(24.dp))
    }
}

private fun securitySummary(
    securitySettings: V11SecuritySettings
): String =
    when {
        !securitySettings.protectAccounts ->
            "بدون قفل"

        securitySettings.mode == V11LockMode.PIN &&
            securitySettings.biometricEnabled ->
            "PIN + بصمة • قفل تلقائي"

        securitySettings.mode == V11LockMode.PATTERN &&
            securitySettings.biometricEnabled ->
            "نمط + بصمة • قفل تلقائي"

        securitySettings.mode == V11LockMode.PIN ->
            "PIN • قفل تلقائي"

        securitySettings.mode == V11LockMode.PATTERN ->
            "نمط • قفل تلقائي"

        securitySettings.mode == V11LockMode.NONE &&
            securitySettings.biometricEnabled ->
            "بصمة / قفل الجهاز • قفل تلقائي"

        else ->
            "بدون قفل"
    }

@Composable
private fun SettingsInfo(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(2.dp))

            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingsAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun highlightLabel(
    style: TodayHighlightStyle
): String = when (style) {
    TodayHighlightStyle.BORDER -> "إطار"
    TodayHighlightStyle.TINT -> "تظليل"
    TodayHighlightStyle.RING -> "دائرة"
    TodayHighlightStyle.BORDER_TINT -> "إطار وتظليل"
    TodayHighlightStyle.NONE -> "بدون"
    else -> "نبض"
}
