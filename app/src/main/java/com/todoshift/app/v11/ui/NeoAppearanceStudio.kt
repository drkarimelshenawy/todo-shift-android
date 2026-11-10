package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

enum class NeoStudioSection {
    THEMES,
    BACKGROUND,
    TYPE,
    LAYOUT,
    MOTION,
    CALENDAR,
    PAINT,
    DASHBOARD,
    PROFILES,
    PREFERENCES,
}

@Composable
fun NeoAppearanceStudio(
    onBack: () -> Unit,
    onClassic: () -> Unit,
    initialSection: NeoStudioSection = NeoStudioSection.THEMES,
) {
    val saved = LocalNeoAppearance.current
    val update = LocalNeoOnChange.current
    var draft by
        rememberSaveable(
            saved,
            stateSaver =
                androidx.compose.runtime.saveable.Saver<NeoAppearance, String>(
                    save = { Neo4AppearanceCodec.encode(it).toString() },
                    restore = { Neo4AppearanceCodec.decode(org.json.JSONObject(it)) },
                ),
        ) {
            mutableStateOf(saved)
        }
    var section by rememberSaveable { mutableStateOf<NeoStudioSection?>(null) }
    var accent by rememberSaveable(saved.accentHex) { mutableStateOf(saved.accentHex) }
    var discard by remember { mutableStateOf(false) }
    val valid = accent.isEmpty() || accent.matches(Regex("#[0-9a-fA-F]{6}"))
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    val commitAppearance = LocalNeoAppearanceCommit.current
    val dirty = draft != saved || accent != saved.accentHex
    fun requestLeave(next: () -> Unit) {
        if (dirty) {
            pendingLeave = next
            discard = true
        } else next()
    }
    fun leave() = requestLeave(onBack)
    neoRegisterLeaveGuard { requestLeave(it) }
    fun apply(next: () -> Unit = {}) {
        if (!valid) return
        if (commitAppearance != null) commitAppearance(draft, next)
        else {
            update(draft)
            next()
        }
    }
    BackHandler { leave() }
    if (discard)
        AlertDialog(
            onDismissRequest = {
                discard = false
                pendingLeave = null
            },
            title = { Text(neoLabel("تجاهل المعاينة؟", "Discard preview changes?")) },
            confirmButton = {
                Column {
                    TextButton(
                        onClick = {
                            draft = saved
                            accent = saved.accentHex
                            discard = false
                            (pendingLeave ?: onBack).invoke()
                            pendingLeave = null
                        }
                    ) {
                        Text(neoLabel("تجاهل", "Discard"))
                    }
                    TextButton(
                        enabled = valid,
                        onClick = {
                            apply {
                                discard = false
                                (pendingLeave ?: onBack).invoke()
                                pendingLeave = null
                            }
                        },
                    ) {
                        Text(neoLabel("تطبيق ومغادرة", "Apply and leave"))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        discard = false
                        pendingLeave = null
                    }
                ) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    Column(Modifier.fillMaxSize()) {
        NeoPageHeader(neoLabel("استوديو المظهر", "Appearance Studio"), { leave() })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Neo4ThemePreview(draft, saved.fontScale)
            listOf(
                    NeoStudioSection.THEMES to neoLabel("الهوية والإضاءة", "Theme and identity"),
                    NeoStudioSection.BACKGROUND to
                        neoLabel("الألوان والخلفية", "Colors and background"),
                    NeoStudioSection.TYPE to neoLabel("الخط والكثافة", "Fonts and density"),
                    NeoStudioSection.MOTION to
                        neoLabel("الحركة والانتقالات", "Motion and transitions"),
                    NeoStudioSection.PROFILES to
                        neoLabel("المظاهر المحفوظة", "Saved appearance profiles"),
                )
                .forEach { (id, title) ->
                    NeoSettingsRow(title) { section = if (section == id) null else id }
                    if (section == id)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (id) {
                                NeoStudioSection.THEMES -> {
                                    NeoChoice(
                                        neoLabel("الهوية", "Identity"),
                                        listOf("NEO", "MINIMAL", "DARK", "AMOLED"),
                                        when {
                                            draft.brightness == NeoBrightness.AMOLED -> "AMOLED"
                                            draft.brightness == NeoBrightness.DARK -> "DARK"
                                            draft.palette == NeoPalette.ARCTIC_WHITE -> "MINIMAL"
                                            else -> "NEO"
                                        },
                                        {
                                            when (it) {
                                                "NEO" -> "Neo Luxury"
                                                "MINIMAL" -> "Minimal Light"
                                                "DARK" -> neoLabel("داكن", "Dark")
                                                else -> "AMOLED"
                                            }
                                        },
                                    ) { identity ->
                                        draft =
                                            draft.copy(
                                                palette =
                                                    if (identity == "MINIMAL")
                                                        NeoPalette.ARCTIC_WHITE
                                                    else NeoPalette.CHAMPAGNE_GOLD,
                                                brightness =
                                                    when (identity) {
                                                        "MINIMAL" -> NeoBrightness.LIGHT
                                                        "DARK" -> NeoBrightness.DARK
                                                        "AMOLED" -> NeoBrightness.AMOLED
                                                        else -> NeoBrightness.SYSTEM
                                                    },
                                                background = NeoBackground.CLASSIC,
                                                glass = false,
                                                glow = false,
                                                cardStyle = NeoCardStyle.FLAT,
                                                accentHex = "",
                                            )
                                        accent = ""
                                    }
                                    NeoChoice(
                                        neoLabel("الإضاءة", "Brightness"),
                                        NeoBrightness.entries,
                                        draft.brightness,
                                        { neoEnumLabel(it) },
                                        { draft = draft.copy(brightness = it) },
                                    )
                                }
                                NeoStudioSection.BACKGROUND -> {
                                    NeoChoice(
                                        neoLabel("مجموعة الألوان", "Color profile"),
                                        NeoPalette.entries,
                                        draft.palette,
                                        { neoEnumLabel(it) },
                                        {
                                            draft = draft.copy(palette = it, accentHex = "")
                                            accent = ""
                                        },
                                    )
                                    OutlinedTextField(
                                        accent,
                                        {
                                            accent = it.take(7)
                                            if (
                                                accent.isEmpty() ||
                                                    accent.matches(Regex("#[0-9a-fA-F]{6}"))
                                            )
                                                draft = draft.copy(accentHex = accent)
                                        },
                                        label = {
                                            Text(
                                                neoLabel(
                                                    "لون مخصص #RRGGBB",
                                                    "Custom accent #RRGGBB",
                                                )
                                            )
                                        },
                                        isError = !valid,
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    NeoSlider(
                                        neoLabel("كثافة اللون", "Color intensity"),
                                        draft.intensity,
                                        0f..1f,
                                    ) {
                                        draft = draft.copy(intensity = it)
                                    }
                                    Neo4BackgroundControls(draft) { draft = it }
                                }
                                NeoStudioSection.TYPE -> {
                                    NeoChoice(
                                        neoLabel("الخط", "Typeface"),
                                        NeoTypeface.entries,
                                        draft.typeface,
                                        { neoEnumLabel(it) },
                                        { draft = draft.copy(typeface = it) },
                                    )
                                    NeoSlider(
                                        neoLabel("حجم الخط", "Text size"),
                                        draft.fontScale,
                                        .85f..1.3f,
                                    ) {
                                        draft = draft.copy(fontScale = it)
                                    }
                                    NeoChoice(
                                        neoLabel("الكثافة", "Density"),
                                        NeoDensity.entries,
                                        draft.density,
                                        { neoEnumLabel(it) },
                                        { draft = draft.copy(density = it) },
                                    )
                                    NeoChoice(
                                        neoLabel("الأسطح", "Surfaces"),
                                        NeoCardStyle.entries,
                                        draft.cardStyle,
                                        { neoEnumLabel(it) },
                                        { draft = draft.copy(cardStyle = it) },
                                    )
                                    NeoSlider(
                                        neoLabel("استدارة الزوايا", "Corner radius"),
                                        draft.cornerRadius.toFloat(),
                                        8f..24f,
                                    ) {
                                        draft = draft.copy(cornerRadius = it.toInt())
                                    }
                                    NeoChoice(
                                        neoLabel("الأيقونات", "Icons"),
                                        NeoIconStyle.entries,
                                        draft.iconStyle,
                                        { neoEnumLabel(it) },
                                        { draft = draft.copy(iconStyle = it) },
                                    )
                                    NeoSwitch(
                                        neoLabel("مؤثرات اختيارية", "Optional visual effects"),
                                        draft.showEffects,
                                    ) {
                                        draft = draft.copy(showEffects = it)
                                    }
                                    NeoSwitch(
                                        neoLabel("أسطح شفافة", "Translucent surfaces"),
                                        draft.glass,
                                    ) {
                                        draft = draft.copy(glass = it)
                                    }
                                    NeoSwitch(neoLabel("ظلال خفيفة", "Soft shadows"), draft.glow) {
                                        draft = draft.copy(glow = it)
                                    }
                                    NeoSwitch(
                                        neoLabel("تفاصيل توضيحية", "Descriptions"),
                                        draft.showDescriptions,
                                    ) {
                                        draft = draft.copy(showDescriptions = it)
                                    }
                                    NeoSwitch(
                                        neoLabel("هوية الرأس", "Header branding"),
                                        draft.showBrand,
                                    ) {
                                        draft = draft.copy(showBrand = it)
                                    }
                                }
                                NeoStudioSection.MOTION -> {
                                    NeoChoice(
                                        neoLabel("الانتقال", "Transition"),
                                        NeoTransition.entries,
                                        draft.transition,
                                        { neoEnumLabel(it) },
                                        {
                                            draft =
                                                draft.copy(
                                                    transition = it,
                                                    optionalTransitions = true,
                                                )
                                        },
                                    )
                                    NeoChoice(
                                        neoLabel("السرعة", "Speed"),
                                        NeoSpeed.entries,
                                        draft.transitionSpeed,
                                        { neoEnumLabel(it) },
                                        { draft = draft.copy(transitionSpeed = it) },
                                    )
                                    NeoSwitch(
                                        neoLabel("تفعيل الانتقالات", "Enable transitions"),
                                        draft.optionalTransitions,
                                    ) {
                                        draft = draft.copy(optionalTransitions = it)
                                    }
                                    NeoChoice(
                                        neoLabel("مستوى الحركة", "Motion level"),
                                        listOf(0, 1, 2),
                                        draft.motion,
                                        {
                                            when (it) {
                                                0 -> neoLabel("بدون", "Off")
                                                1 -> neoLabel("خفيف", "Subtle")
                                                else -> neoLabel("سلس", "Expressive")
                                            }
                                        },
                                        { draft = draft.copy(motion = it) },
                                    )
                                    NeoSwitch(
                                        neoLabel("تقليل الحركة", "Reduce Motion"),
                                        draft.reduceMotion,
                                    ) {
                                        draft = draft.copy(reduceMotion = it)
                                    }
                                    NeoMotionPreview(draft)
                                }
                                NeoStudioSection.PROFILES ->
                                    Neo4ProfilesStudio(draft) { draft = it }
                                else -> Unit
                            }
                        }
                }
            TextButton(
                onClick = {
                    draft = NeoAppearance().preservingLocaleOf(saved)
                    accent = ""
                }
            ) {
                Text(neoLabel("معاينة المظهر الافتراضي", "Preview default appearance"))
            }
        }
        Surface {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        draft = saved
                        accent = saved.accentHex
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(neoLabel("تجاهل", "Discard"))
                }
                Button(
                    enabled = valid && draft != saved,
                    onClick = { apply() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(neoLabel("تطبيق", "Apply"))
                }
            }
        }
    }
}

@Composable
fun <T> NeoChoice(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(Modifier.widthIn(max = 180.dp)) {
            TextButton(onClick = { expanded = true }) {
                Text(label(selected), maxLines = 2)
                Icon(Icons.Rounded.KeyboardArrowDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(label(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun NeoSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = androidx.compose.ui.semantics.Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun NeoSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun neoEnumLabel(value: Any): String =
    when (value) {
        NeoDirection.AUTO -> neoLabel("تلقائي", "Auto")
        NeoDirection.RTL -> neoLabel("يمين إلى يسار", "Right to left")
        NeoDirection.LTR -> neoLabel("يسار إلى يمين", "Left to right")
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
        NeoBrightness.SYSTEM,
        NeoLanguage.SYSTEM -> neoLabel("حسب النظام", "Follow system")
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
        is NeoPalette ->
            value.name.lowercase().split('_').joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
        else -> value.toString()
    }
