package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

enum class NeoStudioSection { THEMES, BACKGROUND, TYPE, LAYOUT, MOTION, CALENDAR, PAINT, DASHBOARD, PROFILES, PREFERENCES }

@Composable
fun NeoAppearanceStudio(onBack: () -> Unit, onClassic: () -> Unit, initialSection: NeoStudioSection = NeoStudioSection.THEMES) {
    val saved = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    var draft by remember(saved) { mutableStateOf(saved) }
    var section by remember { mutableStateOf(initialSection) }
    var accentInput by remember(saved.accentHex) { mutableStateOf(saved.accentHex) }
    val validAccent = accentInput.isEmpty() || accentInput.matches(Regex("#[0-9a-fA-F]{6}"))
    var confirmExit by remember { mutableStateOf(false) }
    fun leave() { if (draft != saved) confirmExit = true else onBack() }
    BackHandler { leave() }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false },
        title = { Text(neoLabel("تجاهل المعاينة؟", "Discard preview changes?")) },
        text = { Text(neoLabel("لم تُحفظ هذه التعديلات. بياناتك لا تتغير.", "These appearance changes have not been saved.")) },
        confirmButton = { TextButton(onClick = onBack) { Text(neoLabel("تجاهل", "Discard")) } },
        dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(neoLabel("متابعة التعديل", "Keep editing")) } })
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { leave() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, neoLabel("رجوع", "Back")) }
            Text(neoLabel("استوديو المظهر", "Appearance Studio"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeoStudioSection.entries.forEach { item ->
                FilterChip(selected = section == item, onClick = { section = item }, label = { Text(when (item) {
                    NeoStudioSection.THEMES -> neoLabel("الثيمات", "Themes")
                    NeoStudioSection.BACKGROUND -> neoLabel("الخلفيات", "Backgrounds")
                    NeoStudioSection.PAINT -> neoLabel("تلوين الشيفتات", "Shift paint")
                    NeoStudioSection.PROFILES -> neoLabel("التخطيطات المحفوظة", "Profiles")
                    NeoStudioSection.TYPE -> neoLabel("الخطوط", "Typography")
                    NeoStudioSection.LAYOUT -> neoLabel("التخطيط", "Layout")
                    NeoStudioSection.MOTION -> neoLabel("الحركات", "Motion")
                    NeoStudioSection.CALENDAR -> neoLabel("التقويم", "Calendar")
                    NeoStudioSection.DASHBOARD -> neoLabel("لوحة اليوم", "Dashboard")
                    NeoStudioSection.PREFERENCES -> neoLabel("التفضيلات", "Preferences")
                }) })
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Neo4ThemePreview(draft, saved.fontScale) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    when (section) {
                        NeoStudioSection.THEMES -> {
                            NeoChoice(neoLabel("الإضاءة", "Brightness"), NeoBrightness.entries, draft.brightness,
                                { neoEnumLabel(it) }, { draft = draft.copy(brightness = it) })
                            Text(neoLabel("الهوية اللونية", "Color collection"), style = MaterialTheme.typography.titleMedium)
                            NeoPalette.entries.chunked(2).forEach { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    row.forEach { palette ->
                                        Card(onClick = { draft = draft.copy(palette = palette, accentHex = "", brightness = if (palette == NeoPalette.ARCTIC_WHITE) NeoBrightness.LIGHT else draft.brightness); accentInput = "" }, modifier = Modifier.weight(1f)) {
                                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(Modifier.size(30.dp).clip(CircleShape).background(neoAccent(palette)))
                                                Text(neoEnumLabel(palette), style = MaterialTheme.typography.labelLarge)
                                                if (draft.palette == palette) Text(neoLabel("مختار", "Selected"), color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(value = accentInput, onValueChange = { input ->
                                accentInput = input.take(7)
                                if (accentInput.isEmpty() || accentInput.matches(Regex("#[0-9a-fA-F]{6}"))) draft = draft.copy(accentHex = accentInput)
                            }, label = { Text(neoLabel("لون مخصص #RRGGBB", "Custom accent #RRGGBB")) },
                                supportingText = { Text(neoLabel("اتركه فارغًا لاستخدام لون الثيم. يضبط التباين تلقائيًا.", "Leave empty to use the palette. Contrast is adjusted automatically.")) },
                                isError = !validAccent, singleLine = true, modifier = Modifier.fillMaxWidth())
                            NeoSlider(neoLabel("كثافة اللون", "Color intensity"), draft.intensity, 0f..1f) { draft = draft.copy(intensity = it) }
                        }
                        NeoStudioSection.BACKGROUND -> Neo4BackgroundControls(draft) { draft = it }
                        NeoStudioSection.PAINT -> Neo4PaintStudio(draft) { draft = it }
                        NeoStudioSection.PROFILES -> Neo4ProfilesStudio(draft) { draft = it }
                        NeoStudioSection.TYPE -> {
                            NeoChoice(neoLabel("نمط الكتابة", "Type style"), NeoTypeface.entries, draft.typeface,
                                { neoEnumLabel(it) }, { draft = draft.copy(typeface = it) })
                            Text(neoLabel("خطوط النظام: Sans للنصوص العربية، Condensed للإنجليزية الهندسية، وSerif للعناوين الإنجليزية المميزة.",
                                "System fonts: Sans for Arabic, Condensed for geometric English, Serif for editorial English headings."), style = MaterialTheme.typography.bodySmall)
                            NeoSlider(neoLabel("حجم الخط", "Text size") + " ${(draft.fontScale * 100).toInt()}%", draft.fontScale, 0.85f..1.3f) { draft = draft.copy(fontScale = it) }
                            Text(neoLabel("يُضاف هذا الحجم إلى تكبير الخط من إعدادات الهاتف.", "This scale combines with the phone's font size."), style = MaterialTheme.typography.bodySmall)
                        }
                        NeoStudioSection.LAYOUT -> {
                            Neo4VisibilityControls(draft) { draft = it }
                            NeoChoice(neoLabel("كثافة المعلومات", "Information density"), NeoDensity.entries, draft.density,
                                { neoEnumLabel(it) }, { draft = draft.copy(density = it) })
                            NeoChoice(neoLabel("شكل البطاقات", "Card style"), NeoCardStyle.entries, draft.cardStyle,
                                { neoEnumLabel(it) }, { draft = draft.copy(cardStyle = it) })
                            NeoSlider(neoLabel("استدارة الزوايا", "Corner radius") + " ${draft.cornerRadius}", draft.cornerRadius.toFloat(), 8f..32f) { draft = draft.copy(cornerRadius = it.toInt()) }
                            NeoSwitch(neoLabel("أسطح زجاجية خفيفة", "Subtle glass surfaces"), draft.glass) { draft = draft.copy(glass = it) }
                            NeoSwitch(neoLabel("حدود مضيئة وظلال خفيفة", "Accent edges and soft shadows"), draft.glow) { draft = draft.copy(glow = it) }
                            NeoChoice(neoLabel("الأيقونات", "Icons"), NeoIconStyle.entries, draft.iconStyle,
                                { neoEnumLabel(it) }, { draft = draft.copy(iconStyle = it) })
                        }
                        NeoStudioSection.MOTION -> {
                            NeoChoice(neoLabel("الانتقال", "Transition"), NeoTransition.entries, draft.transition, { neoEnumLabel(it) }, { draft = draft.copy(transition = it, optionalTransitions = true) })
                            NeoChoice(neoLabel("السرعة", "Speed"), NeoSpeed.entries, draft.transitionSpeed, { neoEnumLabel(it) }, { draft = draft.copy(transitionSpeed = it) })
                            NeoChoice(neoLabel("الأداء", "Performance"), NeoPerformance.entries, draft.performance, { neoEnumLabel(it) }, { draft = draft.copy(performance = it) })
                            NeoSwitch(neoLabel("تفعيل الانتقالات", "Enable transitions"), draft.optionalTransitions) { draft = draft.copy(optionalTransitions = it) }
                            NeoMotionPreview(draft)
                            NeoChoice(neoLabel("مستوى الحركة", "Motion level"), listOf(0, 1, 2), draft.motion,
                                { when(it) { 0 -> neoLabel("بدون", "Off"); 1 -> neoLabel("خفيفة", "Subtle"); else -> neoLabel("سلسة", "Expressive") } },
                                { draft = draft.copy(motion = it) })
                            NeoSwitch(neoLabel("تقليل الحركة", "Reduce motion"), draft.reduceMotion) { draft = draft.copy(reduceMotion = it) }
                        }
                        NeoStudioSection.CALENDAR -> {
                            NeoCalendarPreferences(draft) { draft = it }
                            NeoChoice(neoLabel("طريقة العرض الافتراضية", "Default view"), NeoCalendarView.entries, draft.calendarView,
                                { neoEnumLabel(it) }, { draft = draft.copy(calendarView = it) })
                            Text(neoLabel("الشيفتات الليلية و24 ساعة تظهر في تاريخ البداية فقط.", "Overnight and 24-hour shifts appear only on their start date."), style = MaterialTheme.typography.bodySmall)
                        }
                        NeoStudioSection.DASHBOARD -> {
                            NeoSwitch(neoLabel("فتح لوحة اليوم افتراضيًا", "Expand dashboard"), draft.dashboardExpanded) { draft = draft.copy(dashboardExpanded = it) }
                            draft.dashboardOrder.forEachIndexed { index, card ->
                                Card(Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Switch(checked = draft.cardVisible(card), onCheckedChange = { show ->
                                            draft = draft.withDashboardCard(card, show)
                                        })
                                        Text(neoCardLabel(card), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                        IconButton(enabled = index > 0, onClick = {
                                            val order = draft.dashboardOrder.toMutableList(); java.util.Collections.swap(order, index, index - 1)
                                            draft = draft.copy(dashboardOrder = order)
                                        }) { Icon(Icons.Rounded.KeyboardArrowUp, neoLabel("تحريك لأعلى", "Move up")) }
                                        IconButton(enabled = index < draft.dashboardOrder.lastIndex, onClick = {
                                            val order = draft.dashboardOrder.toMutableList(); java.util.Collections.swap(order, index, index + 1)
                                            draft = draft.copy(dashboardOrder = order)
                                        }) { Icon(Icons.Rounded.KeyboardArrowDown, neoLabel("تحريك لأسفل", "Move down")) }
                                    }
                                }
                            }
                            Text(neoLabel("الاختصارات السريعة", "Quick actions"), style = MaterialTheme.typography.titleMedium)
                            NeoQuickAction.entries.forEach { action ->
                                NeoSwitch(neoActionLabel(action), action in draft.quickActions) { show ->
                                    draft = draft.copy(quickActions = if (show) draft.quickActions + action else draft.quickActions - action)
                                }
                            }
                        }
                        NeoStudioSection.PREFERENCES -> {
                            Text(neoLabel("اللغة والاتجاه من الإعدادات ← اللغة والاتجاه", "Language and direction: Settings → Language and direction"),style=MaterialTheme.typography.bodySmall)
                            NeoSwitch(neoLabel("ملاحظات الشيفتات في الخط الزمني", "Shift notes in timeline"), draft.showShiftNotes) { draft = draft.copy(showShiftNotes = it) }
                            NeoChoice(neoLabel("فلتر الشيفتات الافتراضي", "Default shift filter"), NeoTimelineFilter.entries, draft.timelineFilter,
                                { neoEnumLabel(it) }, { draft = draft.copy(timelineFilter = it) })
                            NeoSwitch(neoLabel("المؤشر المالي", "Financial chart"), draft.showFinanceChart) { draft = draft.copy(showFinanceChart = it) }
                            OutlinedButton(onClick = onClassic, modifier = Modifier.fillMaxWidth()) { Text(neoLabel("العودة إلى Classic", "Switch to Classic")) }
                        }
                    }
                    TextButton(onClick = { draft = NeoAppearance(); accentInput = "" }) {
                        Text(neoLabel("استعادة المظهر الافتراضي في المعاينة", "Preview default appearance"))
                    }
                }
            }
        }
        Surface(tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { draft = saved; accentInput = saved.accentHex }, modifier = Modifier.weight(1f)) { Text(neoLabel("إلغاء التعديلات", "Discard")) }
                Button(enabled = validAccent && draft != saved, onClick = { update(draft) }, modifier = Modifier.weight(1f)) {
                    Text(neoLabel("تطبيق وحفظ", "Apply & save"))
                }
            }
        }
    }
}

@Composable
fun <T> NeoChoice(title: String, options: List<T>, selected: T, label: @Composable (T) -> String, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(label(selected)) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option -> DropdownMenuItem(text = { Text(label(option)) }, onClick = { onSelected(option); expanded = false }) }
            }
        }
    }
}

@Composable
fun NeoSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = checked,
        role = androidx.compose.ui.semantics.Role.Switch, onValueChange = onCheckedChange).padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NeoSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column { Text(title, style = MaterialTheme.typography.labelLarge); Slider(value = value, onValueChange = onChange, valueRange = range) }
}

@Composable
fun neoEnumLabel(value: Any): String = when (value) {
    NeoTransition.FADE -> neoLabel("تلاشي", "Fade")
    NeoTransition.SLIDE -> neoLabel("انزلاق", "Slide")
    NeoTransition.FLIP -> neoLabel("قلب", "Flip")
    NeoTransition.ZOOM -> neoLabel("تكبير", "Zoom")
    NeoTransition.STACK -> neoLabel("تكديس", "Stack")
    NeoTransition.VERTICAL -> neoLabel("انزلاق رأسي", "Vertical slide")
    NeoSpeed.SLOW -> neoLabel("بطيء", "Slow")
    NeoSpeed.NORMAL -> neoLabel("عادي", "Normal")
    NeoSpeed.FAST -> neoLabel("سريع", "Fast")
    NeoPerformance.AUTO -> neoLabel("تلقائي", "Automatic")
    NeoPerformance.NORMAL -> neoLabel("عادي", "Normal")
    NeoPerformance.LIGHTWEIGHT -> neoLabel("خفيف", "Lightweight")
    NeoTodayStyle.NONE -> neoLabel("بدون", "None")
    NeoTodayStyle.CIRCLE -> neoLabel("دائرة", "Circle")
    NeoTodayStyle.GLOW -> neoLabel("حد مضيء", "Glow outline")
    NeoTodayStyle.PULSE -> neoLabel("نبض", "Pulse")
    NeoTodayStyle.BACKGROUND -> neoLabel("خلفية", "Background")
    NeoTodayStyle.UNDERLINE -> neoLabel("خط سفلي", "Underline")
    NeoTodayStyle.DOTS -> neoLabel("نقاط", "Dots")
    NeoTodayStyle.LABEL -> neoLabel("كلمة اليوم", "Today label")
    NeoCellSize.COMPACT -> neoLabel("صغير", "Compact")
    NeoCellSize.COMFORTABLE -> neoLabel("مريح", "Comfortable")
    NeoCellSize.LARGE -> neoLabel("كبير", "Large")
    NeoBrightness.SYSTEM, NeoLanguage.SYSTEM -> neoLabel("حسب النظام", "Follow system")
    NeoBrightness.DARK -> neoLabel("داكن", "Dark")
    NeoBrightness.LIGHT -> neoLabel("فاتح", "Light")
    NeoBrightness.AMOLED -> "AMOLED Black"
    NeoDensity.COMPACT -> neoLabel("مضغوط", "Compact")
    NeoDensity.BALANCED -> neoLabel("متوازن", "Balanced")
    NeoDensity.COMFORTABLE -> neoLabel("مريح", "Comfortable")
    NeoCardStyle.LAYERED -> neoLabel("متعددة الطبقات", "Layered")
    NeoCardStyle.OUTLINED -> neoLabel("بإطار", "Outlined")
    NeoCardStyle.FLAT -> neoLabel("مسطحة", "Flat")
    NeoTypeface.SYSTEM -> neoLabel("Sans • نظام الهاتف", "System Sans")
    NeoTypeface.GEOMETRIC -> neoLabel("Condensed • هندسي", "Geometric Condensed")
    NeoTypeface.EDITORIAL -> neoLabel("Serif • عناوين مميزة", "Editorial Display")
    NeoIconStyle.ROUNDED -> neoLabel("مستديرة", "Rounded")
    NeoIconStyle.OUTLINED -> neoLabel("مفرغة", "Outlined")
    NeoCalendarView.MONTH -> neoLabel("شهر", "Month")
    NeoCalendarView.WEEK -> neoLabel("أسبوع", "Week")
    NeoCalendarView.DAY -> neoLabel("يوم", "Day")
    NeoLanguage.ARABIC -> "العربية"
    NeoLanguage.ENGLISH -> "English"
    NeoTimelineFilter.ALL -> neoLabel("الكل", "All")
    NeoTimelineFilter.CURRENT -> neoLabel("الجارية", "Current")
    NeoTimelineFilter.UPCOMING -> neoLabel("القادمة", "Upcoming")
    NeoTimelineFilter.PAST -> neoLabel("السابقة", "Past")
    is NeoPalette -> value.name.lowercase().split('_').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    else -> value.toString()
}
