package com.todoshift.app.v11.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

@Composable
fun NeoDirectionPreferences(p: NeoAppearance, update: (NeoAppearance) -> Unit) {
    listOf(
            Triple(neoLabel("اتجاه الواجهة", "UI direction"), p.uiDirection, 0),
            Triple(neoLabel("اتجاه التنقل", "Navigation direction"), p.navigationDirection, 1),
            Triple(neoLabel("اتجاه التقويم", "Grid direction"), p.gridDirection, 2),
            Triple(neoLabel("اتجاه أيام الأسبوع", "Weekday direction"), p.weekdayDirection, 3),
        )
        .forEach { (label, value, index) ->
            NeoChoice(
                label,
                NeoDirection.entries,
                value,
                {
                    when (it) {
                        NeoDirection.AUTO -> neoLabel("تلقائي", "Auto")
                        NeoDirection.RTL -> neoLabel("يمين إلى يسار", "Right to left")
                        NeoDirection.LTR -> neoLabel("يسار إلى يمين", "Left to right")
                    }
                },
            ) {
                update(
                    when (index) {
                        0 -> p.copy(uiDirection = it)
                        1 -> p.copy(navigationDirection = it)
                        2 -> p.copy(gridDirection = it)
                        else -> p.copy(weekdayDirection = it)
                    }
                )
            }
        }
}

@Composable
fun NeoCalendarPreferences(p: NeoAppearance, update: (NeoAppearance) -> Unit) {
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
    NeoChoice(
        neoLabel("حجم الخلايا", "Cell size"),
        NeoCellSize.entries,
        p.cellSize,
        { neoEnumLabel(it) },
        { update(p.copy(cellSize = it)) },
    )
    NeoSwitch(neoLabel("التواريخ خارج الشهر", "Outside-month dates"), p.showOutsideDates) {
        update(p.copy(showOutsideDates = it))
    }
    NeoSwitch(neoLabel("تمييز الغد", "Tomorrow marker"), p.showTomorrow) {
        update(p.copy(showTomorrow = it))
    }
    NeoSwitch(neoLabel("نص الملاحظات", "Note text"), p.showNotes) { update(p.copy(showNotes = it)) }
    NeoSwitch(neoLabel("رموز الملاحظات", "Note icons"), p.showNoteIcons) {
        update(p.copy(showNoteIcons = it))
    }
    NeoSwitch(neoLabel("أسماء الشيفتات", "Shift names"), p.showShiftNames) {
        update(p.copy(showShiftNames = it))
    }
    NeoSwitch(neoLabel("مؤشرات المهام", "Task indicators"), p.showTaskIndicators) {
        update(p.copy(showTaskIndicators = it))
    }
    NeoChoice(
        neoLabel("تمييز اليوم", "Today highlight"),
        NeoTodayStyle.entries,
        p.todayStyle,
        { neoEnumLabel(it) },
        { update(p.copy(todayStyle = it)) },
    )
    var color by remember(p.todayColor) { mutableStateOf(p.todayColor) }
    OutlinedTextField(
        color,
        {
            color = it
            if (it.matches(Regex("#[0-9a-fA-F]{6}"))) update(p.copy(todayColor = it))
        },
        label = { Text(neoLabel("لون اليوم #RRGGBB", "Today color #RRGGBB")) },
        isError = !color.matches(Regex("#[0-9a-fA-F]{6}")),
    )
    Text(neoLabel("قوة التمييز", "Highlight intensity"))
    Slider(p.todayIntensity, { update(p.copy(todayIntensity = it)) })
    NeoChoice(
        neoLabel("سرعة النبض", "Pulse speed"),
        NeoSpeed.entries,
        p.todaySpeed,
        { neoEnumLabel(it) },
        { update(p.copy(todaySpeed = it)) },
    )
}

@Composable
fun NeoMotionPreview(p: NeoAppearance) {
    var alternate by remember { mutableStateOf(false) }
    TextButton(onClick = { alternate = !alternate }) {
        Text(neoLabel("معاينة الحركة", "Preview animation"))
    }
    AnimatedContent(alternate, transitionSpec = { neoTransition(p) }, label = "motionPreview") {
        value ->
        Surface(
            color =
                if (value) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                if (value) "ToDo Shift" else neoLabel("المعاينة", "Preview"),
                Modifier.fillMaxWidth().padding(20.dp),
            )
        }
    }
}
