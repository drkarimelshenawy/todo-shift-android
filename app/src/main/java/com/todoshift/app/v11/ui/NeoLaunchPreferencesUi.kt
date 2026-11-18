package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import com.todoshift.app.v11.NeoLaunchPreferences

@Composable
fun NeoLaunchPreferencesUi() {
    val context = LocalContext.current
    val store = remember { NeoLaunchPreferences(context) }
    var noteBackground by remember {mutableStateOf(store.defaultNoteBackground)}
    var ruled by remember {mutableStateOf(store.defaultNoteRuled)}
    var arabicFont by remember {mutableStateOf(store.defaultArabicNoteFont)}
    var englishFont by remember {mutableStateOf(store.defaultEnglishNoteFont)}
    var notes by remember { mutableStateOf(store.openNotes) }
    var failure by remember { mutableStateOf(false) }
    var trashDays by remember {mutableIntStateOf(store.noteTrashRetentionDays)}
    NeoChoice(neoLabel("القسم الافتراضي داخل المهام والملاحظات", "Default Tasks & Notes section"),
        listOf(false,true), notes,
        { if(it) neoLabel("الملاحظات", "Notes") else neoLabel("المهام", "Tasks") },
        { value -> runCatching { store.openNotes = value }.onSuccess { notes=value;failure=false }.onFailure { failure=true } })
    NeoChoice(neoLabel("خلفية الملاحظات الافتراضية","Default note background"),listOf("","#FFF4B8","#D9EFDA","#D6E9FF","#F9DCE9","#E8DDF5","#FFE1C3","#E3E6EB"),noteBackground,
        {when(it){""->neoLabel("حسب المظهر","Follow theme");"#FFF4B8"->neoLabel("أصفر","Yellow");"#D9EFDA"->neoLabel("أخضر","Green");"#D6E9FF"->neoLabel("أزرق","Blue");"#F9DCE9"->neoLabel("وردي","Pink");"#E8DDF5"->neoLabel("بنفسجي","Purple");"#FFE1C3"->neoLabel("برتقالي","Orange");else->neoLabel("رمادي","Gray")}},
        {value->runCatching {store.defaultNoteBackground=value}.onSuccess {noteBackground=value;failure=false}.onFailure {failure=true}})
    NeoSwitch(
        neoLabel("ورق مسطّر افتراضيًا للملاحظات الجديدة",
            "Ruled paper by default for notes"),ruled
    ) {value ->
        runCatching {store.defaultNoteRuled=value}
            .onSuccess {ruled=value;failure=false}.onFailure {failure=true}
    }
    val arabicOptions=NeoNoteTypefaces.arabicChoices+
        listOf(arabicFont).filter{it !in NeoNoteTypefaces.arabicChoices}
    val englishOptions=NeoNoteTypefaces.englishChoices+
        listOf(englishFont).filter{it !in NeoNoteTypefaces.englishChoices}
    NeoChoice(neoLabel("الخط العربي الافتراضي","Default Arabic font"),
        arabicOptions,arabicFont,{key ->
            when(key) {
                "amiri" -> "Amiri — "+neoLabel("نسخي كلاسيكي","Classical")
                "tajawal" -> "Tajawal — "+neoLabel("حديث وواضح","Modern")
                "reem" -> "Reem Kufi — "+neoLabel("كوفي مميز","Kufi")
                "sans" -> neoLabel("Sans القديم","Legacy Sans")
                "serif" -> neoLabel("Serif القديم","Legacy Serif")
                "mono" -> neoLabel("Mono القديم","Legacy Mono")
                else -> neoLabel("الخط الأساسي","Default")
            }
        },
        {value ->
            runCatching {store.defaultArabicNoteFont=value}
                .onSuccess {arabicFont=value;failure=false}.onFailure {failure=true}
        })
    NeoChoice(neoLabel("الخط الإنجليزي الافتراضي","Default English font"),
        englishOptions,englishFont,{key ->
            when(key) {
                "montserrat" -> "Montserrat — "+neoLabel("حديث","Modern")
                "lora" -> "Lora — "+neoLabel("كلاسيكي","Literary")
                "oswald" -> "Oswald — "+neoLabel("للعناوين","Display")
                "sans" -> neoLabel("Sans القديم","Legacy Sans")
                "serif" -> neoLabel("Serif القديم","Legacy Serif")
                "mono" -> neoLabel("Mono القديم","Legacy Mono")
                else -> neoLabel("الخط الأساسي","Default")
            }
        },
        {value ->
            runCatching {store.defaultEnglishNoteFont=value}
                .onSuccess {englishFont=value;failure=false}.onFailure {failure=true}
        })
    NeoChoice(neoLabel("الحذف التلقائي من سلة مهملات الملاحظات", "Auto-delete notes in Trash"),
        listOf(7,15,30,60,90,0),trashDays,
        { days -> if(days==0) neoLabel("بدون حذف تلقائي", "Never delete automatically") else neoLabel("$days يوم", "$days days") },
        {days->runCatching {store.noteTrashRetentionDays=days}.onSuccess {trashDays=days;failure=false}.onFailure {failure=true}})
    Text(neoLabel("الحذف النهائي لا يمكن التراجع عنه. تُحسب المدة من تاريخ النقل للمهملات.",
        "Permanent deletion cannot be undone. Retention begins when the note is moved to Trash."),
        style=androidx.compose.material3.MaterialTheme.typography.bodySmall)
    Text(neoLabel(
        "الخطوط الستة الجديدة مرفقة داخل التطبيق، وتعمل دون إنترنت.",
        "Six distinctive typefaces are bundled in the app and work offline."),
        style=androidx.compose.material3.MaterialTheme.typography.bodySmall)
    if (failure) Text(neoLabel("تعذر حفظ التفضيل؛ أعد المحاولة", "Preference could not be saved; retry"))
}

/** Start destination belongs to general app Settings, not Notes settings. */
@Composable
fun NeoLandingPreferencesUi() {
    val context=LocalContext.current
    val store=remember {NeoLaunchPreferences(context)}
    var landing by remember {mutableIntStateOf(store.landingTab)}
    var failure by remember {mutableStateOf(false)}
    NeoChoice(neoLabel("قسم بداية التطبيق","Default app start section"),listOf(0,4,2,1,3),landing,
        {when(it){0->neoLabel("التقويم","Calendar");4->neoLabel("المهام والملاحظات","Tasks & Notes");
            2->neoLabel("مستحقات العمل","Salary");1->neoLabel("الإدارة المالية","Cash Book");
            else->neoLabel("الإعدادات","Settings")}},
        {value->runCatching{store.landingTab=value}.onSuccess{landing=value;failure=false}.onFailure{failure=true}})
    if(failure) Text(neoLabel("تعذر حفظ التفضيل؛ أعد المحاولة","Preference could not be saved; retry"))
}
