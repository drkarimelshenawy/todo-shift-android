package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

@Composable
fun V11MoreScreen(
    db: V11Database,
    uiSettings: V11UiSettings,
    moneyVisible: Boolean,
    securitySettings:
        V11SecuritySettings,
    securityStore:
        V11SecurityStore,
    onSecuritySettingsChange:
        (V11SecuritySettings) -> Unit,
    onUiSettingsChange:
        (V11UiSettings) -> Unit,
    onToggleMoney: () -> Unit,
    onOpenWorkplaces: () -> Unit
) {
    var securityOpen by remember {
        mutableStateOf(false)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(14.dp)
    ) {
        Text(
            "الإعدادات",
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(16.dp))

        SettingsTitle(
            "التقويم والعمل"
        )

        SettingsAction(
            title =
                "أماكن العمل والأسعار",
            subtitle =
                "إدارة الأماكن والألوان وأسعار الشيفتات",
            onClick =
                onOpenWorkplaces
        )

        SettingsInfo(
            title = "التقاويم",
            subtitle =
                "${db.calendars.size} تقويم • ${
                    db.calendars.count {
                        it.visible
                    }
                } ظاهر"
        )

        Spacer(Modifier.height(16.dp))

        SettingsTitle(
            "الحسابات والالتزامات"
        )

        SettingsSwitch(
            title = "إظهار المبالغ",
            subtitle =
                "إخفاء أو إظهار القيم المالية داخل الحسابات",
            checked = moneyVisible,
            onCheckedChange = {
                if (it != moneyVisible)
                    onToggleMoney()
            }
        )

        SettingsInfo(
            title = "مصادر الدخل",
            subtitle =
                "${db.incomeSources.size} مصدر"
        )

        SettingsInfo(
            title = "طرق الدفع",
            subtitle =
                "${db.paymentMethods.size} طريقة"
        )

        SettingsInfo(
            title = "الالتزامات",
            subtitle =
                "${db.obligations.count {
                    it.status ==
                        ObligationStatusV11.ACTIVE
                }} التزام نشط"
        )

        Spacer(Modifier.height(16.dp))

        SettingsTitle(
            "المظهر والتخصيص"
        )

        SettingsSwitch(
            title =
                "أسماء أماكن العمل",
            subtitle =
                "إظهار اسم مكان العمل داخل الشيفت في التقويم",
            checked =
                uiSettings.showWorkplaceNames,
            onCheckedChange = {
                onUiSettingsChange(
                    uiSettings.copy(
                        showWorkplaceNames = it
                    )
                )
            }
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {
                Text(
                    "مكان زر الإضافة",
                    fontWeight =
                        FontWeight.Bold
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {
                    FilterChip(
                        selected =
                            uiSettings.fabSide ==
                                FabSide.RIGHT,
                        onClick = {
                            onUiSettingsChange(
                                uiSettings.copy(
                                    fabSide =
                                        FabSide.RIGHT
                                )
                            )
                        },
                        label = {
                            Text("يمين")
                        }
                    )

                    FilterChip(
                        selected =
                            uiSettings.fabSide ==
                                FabSide.LEFT,
                        onClick = {
                            onUiSettingsChange(
                                uiSettings.copy(
                                    fabSide =
                                        FabSide.LEFT
                                )
                            )
                        },
                        label = {
                            Text("يسار")
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {
                Text(
                    "تمييز اليوم الحالي",
                    fontWeight =
                        FontWeight.Bold
                )

                listOf(
                    TodayHighlightStyle.BORDER,
                    TodayHighlightStyle.TINT,
                    TodayHighlightStyle.RING,
                    TodayHighlightStyle.BORDER_TINT,
                    TodayHighlightStyle.NONE
                ).forEach { style ->
                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment
                                .CenterVertically
                    ) {
                        RadioButton(
                            selected =
                                uiSettings
                                    .todayHighlightStyle ==
                                    style,
                            onClick = {
                                onUiSettingsChange(
                                    uiSettings.copy(
                                        todayHighlightStyle =
                                            style
                                    )
                                )
                            }
                        )

                        Text(
                            highlightLabel(
                                style
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsTitle(
            "الخصوصية والبيانات"
        )

        SettingsAction(
            title =
                "حماية الحسابات",
            subtitle =
                when {
                    !securitySettings
                        .protectAccounts ->
                        "غير مفعلة"

                    securitySettings.mode ==
                        V11LockMode.PIN ->
                        "PIN • قفل تلقائي"

                    securitySettings.mode ==
                        V11LockMode.BIOMETRIC ->
                        "بصمة / قفل الجهاز • قفل تلقائي"

                    else ->
                        "بدون قفل"
                },
            onClick = {
                securityOpen = true
            }
        )

        SettingsInfo(
            title =
                "النسخ الاحتياطي المحلي",
            subtitle =
                "Schema ${db.schemaVersion} • سيتم ربط التصدير والاستعادة النهائية في RC7E"
        )

        Spacer(Modifier.height(16.dp))

        SettingsTitle(
            "الحساب والمزامنة"
        )

        SettingsInfo(
            title = "Google Sync",
            subtitle =
                "غير مفعّل حاليا — لن يظهر كمفتاح وهمي قبل تنفيذ المزامنة الحقيقية"
        )

        Spacer(Modifier.height(16.dp))

        SettingsTitle(
            "حول التطبيق"
        )

        SettingsInfo(
            title = "ToDo Shift",
            subtitle =
                "V1.1 • Schema ${db.schemaVersion}"
        )

        Spacer(Modifier.height(30.dp))
    }
    if (securityOpen) {
        V11SecuritySettingsDialog(
            store = securityStore,
            current = securitySettings,
            onDismiss = {
                securityOpen = false
            },
            onChanged =
                onSecuritySettingsChange
        )
    }
}

@Composable
private fun SettingsTitle(
    text: String
) {
    Text(
        text,
        fontWeight = FontWeight.Black,
        style =
            MaterialTheme.typography
                .titleMedium,
        modifier =
            Modifier.padding(
                vertical = 6.dp
            )
    )
}

@Composable
private fun SettingsInfo(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {
            Text(
                title,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                subtitle,
                style =
                    MaterialTheme.typography
                        .bodySmall
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ChevronRight,
                null
            )

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    subtitle,
                    style =
                        MaterialTheme.typography
                            .bodySmall
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
    onCheckedChange:
        (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Switch(
                checked = checked,
                onCheckedChange =
                    onCheckedChange
            )

            Spacer(Modifier.weight(1f))

            Column(
                horizontalAlignment =
                    Alignment.End
            ) {
                Text(
                    title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    subtitle,
                    style =
                        MaterialTheme.typography
                            .bodySmall
                )
            }
        }
    }
}

private fun highlightLabel(
    style: TodayHighlightStyle
): String =
    when (style) {
        TodayHighlightStyle.BORDER ->
            "إطار"
        TodayHighlightStyle.TINT ->
            "تلوين خفيف"
        TodayHighlightStyle.RING ->
            "حلقة حول الرقم"
        TodayHighlightStyle.BORDER_TINT ->
            "إطار + تلوين"
        TodayHighlightStyle.NONE ->
            "بدون"
        TodayHighlightStyle.PULSE ->
            "نبض"
    }
