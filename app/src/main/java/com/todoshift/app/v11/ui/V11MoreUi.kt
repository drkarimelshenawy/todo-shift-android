package com.todoshift.app.v11.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onUiSettingsChange:
        (V11UiSettings) -> Unit,
    onToggleMoney: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {
        Text(
            "المزيد",
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(14.dp))

        MoreSection(
            "أماكن العمل والشيفتات",
            "إدارة الأماكن، أنواع الشيفتات والأسعار"
        )

        MoreSection(
            "التقويم والجداول",
            "${db.calendars.size} تقويم"
        )

        MoreSection(
            "المالية والحسابات",
            if (moneyVisible)
                "المبالغ ظاهرة حاليا"
            else
                "المبالغ مخفية حاليا",
            onClick = onToggleMoney
        )

        MoreSection(
            "التقارير والإحصائيات",
            "تقارير الشيفتات والمالية"
        )

        MoreSection(
            "البحث",
            "بحث بالتاريخ والتقويم ومكان العمل ونوع الشيفت"
        )

        MoreSection(
            "النسخ الاحتياطي والاستعادة",
            "بيانات V1.1 قابلة للتصدير والاستعادة"
        )

        MoreSection(
            "الإشعارات",
            "تذكيرات الشيفتات"
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "المظهر والتخصيص",
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
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

                Row {
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

                    Spacer(
                        Modifier.width(8.dp)
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

                Spacer(
                    Modifier.height(12.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {
                    Switch(
                        checked =
                            uiSettings
                                .showWorkplaceNames,
                        onCheckedChange = {
                            onUiSettingsChange(
                                uiSettings.copy(
                                    showWorkplaceNames =
                                        it
                                )
                            )
                        }
                    )

                    Text(
                        "إظهار أسماء الأماكن داخل التقويم"
                    )
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    "تمييز اليوم الحالي",
                    fontWeight =
                        FontWeight.Bold
                )

                TodayHighlightStyle.entries
                    .forEach { style ->

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUiSettingsChange(
                                            uiSettings
                                                .copy(
                                                    todayHighlightStyle =
                                                        style
                                                )
                                        )
                                    },
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
                                        uiSettings
                                            .copy(
                                                todayHighlightStyle =
                                                    style
                                            )
                                    )
                                }
                            )

                            Text(
                                when (style) {
                                    TodayHighlightStyle.BORDER ->
                                        "إطار"
                                    TodayHighlightStyle.TINT ->
                                        "تلوين خفيف"
                                    TodayHighlightStyle.RING ->
                                        "حلقة حول الرقم"
                                    TodayHighlightStyle.PULSE ->
                                        "نبض"
                                    TodayHighlightStyle.BORDER_TINT ->
                                        "إطار + تلوين"
                                    TodayHighlightStyle.NONE ->
                                        "بدون"
                                }
                            )
                        }
                    }
            }
        }

        Spacer(Modifier.height(14.dp))

        MoreSection(
            "حول التطبيق وتشخيص البيانات",
            "ToDo Shift V1.1 • Schema ${db.schemaVersion}"
        )

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun MoreSection(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier =
                Modifier.padding(14.dp)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style =
                    MaterialTheme.typography
                        .bodySmall
            )
        }
    }
}
