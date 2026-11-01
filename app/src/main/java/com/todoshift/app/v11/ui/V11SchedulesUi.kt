package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun V11SchedulesScreen(
    month: YearMonth,
    db: V11Database,
    workplaces: List<Workplace>,
    onDatabaseChange: (V11Database) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "جداولي",
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "الشيفتات المسجلة خلال ${month}"
        )

        Spacer(Modifier.height(14.dp))

        val days = db.days
            .filter {
                it.date.startsWith(
                    month.toString()
                )
            }
            .sortedBy { it.date }

        if (days.isEmpty()) {
            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text(
                    "لا توجد شيفتات مسجلة لهذا الشهر.",
                    modifier =
                        Modifier.padding(16.dp)
                )
            }
        }

        days.forEach { day ->
            val calendar =
                db.calendars.firstOrNull {
                    it.id == day.calendarId
                }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(
                    modifier =
                        Modifier.padding(12.dp)
                ) {
                    Text(
                        day.date,
                        fontWeight =
                            FontWeight.Black
                    )

                    Text(
                        calendar?.name
                            ?: "تقويم",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    day.segments.forEach {
                        segment ->

                        val place =
                            workplaces
                                .firstOrNull {
                                    it.id ==
                                        segment
                                            .workplaceId
                                }

                        Text(
                            "• ${
                                V11Labels.shift(
                                    segment.kind
                                )
                            }${
                                place?.name
                                    ?.let {
                                        " - $it"
                                    } ?: ""
                            }"
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "أدوات الجدولة المتقدمة",
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "التكرار الأسبوعي والدورات والقوالب القديمة محفوظة في المشروع، وسيتم دمج واجهتها مع نموذج Segments الجديد بدون إعادة إنشاء باقات Long/Night القديمة."
        )

        Spacer(Modifier.height(30.dp))
    }
}
