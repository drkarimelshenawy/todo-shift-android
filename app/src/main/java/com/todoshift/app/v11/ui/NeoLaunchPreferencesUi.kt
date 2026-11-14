package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.v11.NeoLaunchPreferences

@Composable
fun NeoLaunchPreferencesUi() {
    val context = LocalContext.current
    val store = remember { NeoLaunchPreferences(context) }
    var landing by remember { mutableIntStateOf(store.landingTab) }
    var notes by remember { mutableStateOf(store.openNotes) }
    var failure by remember { mutableStateOf(false) }
    NeoChoice(neoLabel("قسم البداية", "Default landing section"), listOf(0, 4, 2, 1, 3), landing,
        { when (it) {
            0 -> neoLabel("التقويم", "Calendar")
            4 -> neoLabel("المهام والملاحظات", "Tasks & Notes")
            2 -> neoLabel("مستحقات العمل", "Salary")
            1 -> neoLabel("الإدارة المالية", "Cash Book")
            else -> neoLabel("الإعدادات", "Settings")
        } },
        { value -> runCatching { store.landingTab = value }.onSuccess { landing = value; failure = false }.onFailure { failure = true } })
    NeoSwitch(neoLabel("افتح الملاحظات داخل قسم المهام", "Open Notes in Tasks section"), notes) { value ->
        runCatching { store.openNotes = value }.onSuccess { notes = value; failure = false }.onFailure { failure = true }
    }
    if (failure) Text(neoLabel("تعذر حفظ التفضيل؛ أعد المحاولة", "Preference could not be saved; retry"))
}
