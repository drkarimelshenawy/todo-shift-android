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
    var noteBackground by remember {mutableStateOf(store.defaultNoteBackground)}
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
    NeoChoice(neoLabel("خلفية الملاحظات الافتراضية","Default note background"),listOf("","#FFF4B8","#D9EFDA","#D6E9FF","#F9DCE9","#E8DDF5","#FFE1C3","#E3E6EB"),noteBackground,
        {when(it){""->neoLabel("حسب المظهر","Follow theme");"#FFF4B8"->neoLabel("أصفر","Yellow");"#D9EFDA"->neoLabel("أخضر","Green");"#D6E9FF"->neoLabel("أزرق","Blue");"#F9DCE9"->neoLabel("وردي","Pink");"#E8DDF5"->neoLabel("بنفسجي","Purple");"#FFE1C3"->neoLabel("برتقالي","Orange");else->neoLabel("رمادي","Gray")}},
        {value->runCatching {store.defaultNoteBackground=value}.onSuccess {noteBackground=value;failure=false}.onFailure {failure=true}})
    if (failure) Text(neoLabel("تعذر حفظ التفضيل؛ أعد المحاولة", "Preference could not be saved; retry"))
}
