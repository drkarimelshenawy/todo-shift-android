package com.todoshift.app.v11.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import com.todoshift.app.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Neo4ThemePreview(draft: NeoAppearance, rootFontScale: Float) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val english =
        when (draft.language) {
            NeoLanguage.SYSTEM ->
                androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language != "ar"
            NeoLanguage.ENGLISH -> true
            NeoLanguage.ARABIC -> false
        }
    CompositionLocalProvider(
        LocalNeoAppearance provides draft,
        LocalNeoEnglish provides english,
        androidx.compose.ui.platform.LocalLayoutDirection provides
            neoDirection(
                draft.uiDirection,
                androidx.compose.ui.platform.LocalLayoutDirection.current,
            ),
        androidx.compose.ui.platform.LocalDensity provides
            androidx.compose.ui.unit.Density(
                density.density,
                density.fontScale / rootFontScale * draft.fontScale,
            ),
    ) {
        V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = androidx.compose.ui.graphics.Color.Transparent,
            ) {
                Box {
                    Neo4Atmosphere(Modifier.matchParentSize(), draft)
                    Column(
                        Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            neoLabel("معاينة حية", "Live preview"),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            listOf(
                                    ShiftKind.FULL_DAY,
                                    ShiftKind.LONG,
                                    ShiftKind.NIGHT,
                                    ShiftKind.OFF,
                                )
                                .forEachIndexed { index, kind ->
                                    Surface(
                                        modifier = Modifier.weight(1f).height(64.dp),
                                        shape = MaterialTheme.shapes.small,
                                        border =
                                            if (index == 1)
                                                androidx.compose.foundation.BorderStroke(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                )
                                            else null,
                                    ) {
                                        Box {
                                            Neo4ShiftFill(
                                                listOf(
                                                    ShiftSegmentV11(
                                                        "preview-$index",
                                                        kind,
                                                        "preview",
                                                    )
                                                ),
                                                listOf(
                                                    com.todoshift.app.data.Workplace(
                                                        "preview",
                                                        "Work",
                                                        "#00A4B8",
                                                    )
                                                ),
                                                Modifier.fillMaxSize(),
                                                draft,
                                            )
                                            Text(
                                                "${index+12}",
                                                Modifier.align(
                                                        androidx.compose.ui.Alignment.TopStart
                                                    )
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .padding(horizontal = 3.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            Text(
                                                if (kind == ShiftKind.OFF) neoLabel("راحة", "Off")
                                                else neoShiftLabel(kind),
                                                Modifier.align(
                                                        androidx.compose.ui.Alignment.BottomStart
                                                    )
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .padding(horizontal = 3.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                            )
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Neo4BackgroundControls(p: NeoAppearance, update: (NeoAppearance) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null)
                scope.launch {
                    busy = true
                    error = null
                    runCatching {
                            withContext(Dispatchers.IO) {
                                Neo4ImageStore.importPhoto(context.applicationContext, uri)
                            }
                        }
                        .onSuccess {
                            update(
                                p.copy(
                                    customImageFile = it,
                                    imageTheme = NeoImageTheme.PERSONAL,
                                    background = NeoBackground.IMAGE,
                                )
                            )
                        }
                        .onFailure { error = it.message }
                    busy = false
                }
        }
    NeoChoice(
        neoLabel("الخلفية", "Background"),
        NeoBackground.entries,
        p.background,
        { neo4Label(it) },
        { update(p.copy(background = it)) },
    )
    if (p.background == NeoBackground.IMAGE) {
        NeoChoice(
            neoLabel("الثيم المصور", "Illustration theme"),
            NeoImageTheme.entries,
            p.imageTheme,
            { neo4Label(it) },
            { update(p.copy(imageTheme = it)) },
        )
        Text(
            neoLabel(
                "الصور الست رسومات أصلية خفيفة. الصورة الشخصية تبقى داخل الهاتف.",
                "Six original lightweight illustrations. Personal images remain on this phone.",
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            enabled = !busy,
            onClick = {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(neoLabel("اختيار صورة من الهاتف", "Choose personal photo"))
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Neo4Slider(neoLabel("موضع أفقي", "Horizontal crop"), p.imagePositionX) {
            update(p.copy(imagePositionX = it))
        }
        Neo4Slider(neoLabel("موضع رأسي", "Vertical crop"), p.imagePositionY) {
            update(p.copy(imagePositionY = it))
        }
        Neo4Slider(neoLabel("طبقة القراءة", "Reading overlay"), p.imageDim) {
            update(p.copy(imageDim = it))
        }
    }
    Neo4Slider(neoLabel("شفافية البطاقات", "Card opacity"), p.cardOpacity, 0.65f..1f) {
        update(p.copy(cardOpacity = it))
    }
    Neo4Slider(neoLabel("قوة السطح الزجاجي", "Glass strength"), p.glassStrength, 0f..0.6f) {
        update(p.copy(glassStrength = it))
    }
}

@Composable
fun Neo4VisibilityControls(p: NeoAppearance, update: (NeoAppearance) -> Unit) {
    NeoChoice(
        neoLabel("نمط التخطيط", "Layout preset"),
        NeoLayoutPreset.entries,
        p.layoutPreset,
        { neo4Label(it) },
        { update(p.withPreset(it)) },
    )
    Text(
        neoLabel(
            "كل المعلومات الثانوية تظهر عند الطلب، ولا يوضع أي كارت فوق التقويم أو تحته.",
            "Secondary information opens on demand. No cards are placed above or below the calendar.",
        ),
        style = MaterialTheme.typography.bodySmall,
    )
    NeoSwitch(neoLabel("رسالة الترحيب", "Greeting"), p.showGreeting) {
        update(p.copy(showGreeting = it))
    }
    NeoSwitch(neoLabel("ملخص اليوم", "Day summary"), p.showDailySummary) {
        update(
            p.withDashboardCard(NeoDashboardCard.CURRENT_SHIFT, it)
                .withDashboardCard(NeoDashboardCard.SUMMARY, it)
        )
    }
    NeoSwitch(neoLabel("الشيفت القادم", "Next shift"), p.showNextShift) {
        update(p.withDashboardCard(NeoDashboardCard.NEXT_SHIFT, it))
    }
    NeoSwitch(neoLabel("ملخص المهام", "Task summary"), p.showTaskSummary) {
        update(p.withDashboardCard(NeoDashboardCard.TASKS, it))
    }
    NeoSwitch(neoLabel("الملخص المالي", "Finance summary"), p.showFinanceSummary) {
        update(p.copy(showFinanceSummary = it))
    }
    NeoSwitch(neoLabel("البطاقات الثانوية", "Secondary cards"), p.showSecondaryCards) {
        update(p.copy(showSecondaryCards = it))
    }
    NeoSwitch(neoLabel("المعلومات الاختيارية", "Optional information"), p.showOptionalInfo) {
        update(p.withDashboardCard(NeoDashboardCard.NEXT_EVENT, it))
    }
    NeoSwitch(neoLabel("النصوص الوصفية", "Descriptive text"), p.showDescriptions) {
        update(p.copy(showDescriptions = it))
    }
    NeoSwitch(neoLabel("دليل ألوان التقاويم", "Calendar legend"), p.showCalendarLegend) {
        update(p.copy(showCalendarLegend = it))
    }
    NeoSwitch(neoLabel("اسم التطبيق في الهيدر", "App name in header"), p.showBrand) {
        update(p.copy(showBrand = it))
    }
    NeoSwitch(neoLabel("المؤثرات البصرية", "Visual effects"), p.showEffects) {
        update(p.copy(showEffects = it))
    }
    NeoSwitch(
        neoLabel(
            "ألوان شيفتات مخصصة بدل ألوان أماكن العمل",
            "Custom shift paint instead of workplace colors",
        ),
        p.showShiftPaint,
    ) {
        update(p.copy(showShiftPaint = it))
    }
}

@Composable
fun Neo4PaintStudio(p: NeoAppearance, update: (NeoAppearance) -> Unit) {
    var selected by remember { mutableStateOf(ShiftKind.FULL_DAY) }
    val paint = p.shiftPaints[selected] ?: NeoShiftPaint()
    fun set(value: NeoShiftPaint) =
        update(p.copy(shiftPaints = p.shiftPaints + (selected to value)))
    NeoChoice(
        neoLabel("نوع الشيفت", "Shift"),
        ShiftKind.entries.filter { it != ShiftKind.OFF },
        selected,
        { neoShiftLabel(it) },
        { selected = it },
    )
    Box(Modifier.fillMaxWidth().height(72.dp).background(MaterialTheme.colorScheme.surface)) {
        Neo4ShiftFill(
            listOf(ShiftSegmentV11("preview", selected)),
            emptyList(),
            Modifier.fillMaxSize(),
            p,
        )
        Text(
            neoLabel("معاينة • 17", "Preview • 17"),
            Modifier.padding(12.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(5.dp),
            fontWeight = FontWeight.Bold,
        )
    }
    NeoChoice(
        neoLabel("نوع اللون", "Paint type"),
        NeoPaintMode.entries,
        paint.mode,
        { neo4Label(it) },
        { set(paint.copy(mode = it)) },
    )
    if (paint.mode != NeoPaintMode.INHERIT) {
        var first by remember(selected, paint.first) { mutableStateOf(paint.first) }
        var second by remember(selected, paint.second) { mutableStateOf(paint.second) }
        fun valid(s: String) = s.matches(Regex("#[0-9a-fA-F]{6}"))
        OutlinedTextField(
            first,
            {
                first = it.take(7)
                if (valid(first)) set(paint.copy(first = first))
            },
            label = { Text(neoLabel("اللون الأول", "First color")) },
            isError = !valid(first),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (paint.mode == NeoPaintMode.GRADIENT) {
            OutlinedTextField(
                second,
                {
                    second = it.take(7)
                    if (valid(second)) set(paint.copy(second = second))
                },
                label = { Text(neoLabel("اللون الثاني", "Second color")) },
                isError = !valid(second),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            NeoChoice(
                neoLabel("اتجاه التدرج", "Gradient direction"),
                NeoGradientDirection.entries,
                paint.direction,
                { neo4Label(it) },
                { set(paint.copy(direction = it)) },
            )
            NeoChoice(
                neoLabel("شدة التدرج", "Gradient strength"),
                NeoGradientStrength.entries,
                paint.strength,
                { neo4Label(it) },
                { set(paint.copy(strength = it)) },
            )
        }
    }
    neo4PaintPresets.chunked(4).forEachIndexed { row, values ->
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            values.forEachIndexed { index, pair ->
                OutlinedButton(
                    onClick = {
                        set(
                            paint.copy(
                                mode = NeoPaintMode.GRADIENT,
                                first = pair.first,
                                second = pair.second,
                            )
                        )
                    },
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                    modifier =
                        Modifier.weight(1f)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(neoHexColor(pair.first), neoHexColor(pair.second))
                                ),
                                MaterialTheme.shapes.medium,
                            ),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    Text(
                        "${row * 4 + index + 1}",
                        Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .padding(2.dp),
                    )
                }
            }
        }
    }
    TextButton(onClick = { update(p.copy(shiftPaints = p.shiftPaints - selected)) }) {
        Text(neoLabel("استعادة لون مكان العمل الأصلي", "Restore original workplace color"))
    }
    Text(
        neoLabel(
            "التداخل يُعرض كمساحات أفقية متناوبة بعرض الخلية، وفق ترتيب الشيفتات المخزن. لا تتغير الأوقات أو قواعد حل التعارض.",
            "Overlaps alternate full-width horizontal areas in stored order. Times and conflict rules remain unchanged.",
        ),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
fun Neo4ProfilesStudio(draft: NeoAppearance, update: (NeoAppearance) -> Unit) {
    val context = LocalContext.current
    val store = remember { Neo4Profiles(context.applicationContext) }
    var profiles by remember { mutableStateOf(store.list()) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    OutlinedTextField(
        name,
        { name = it.take(60) },
        label = { Text(neoLabel("اسم التخطيط", "Profile name")) },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        enabled = name.isNotBlank() && profiles.size < 12,
        onClick = {
            runCatching { store.save(name, draft) }
                .onSuccess {
                    profiles = store.list()
                    name = ""
                }
                .onFailure { error = it.message }
        },
    ) {
        Text(neoLabel("حفظ المعاينة كتخطيط", "Save preview as profile"))
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    profiles.forEach { profile ->
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { update(profile.appearance.preservingLocaleOf(draft)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(profile.name)
            }
            TextButton(
                onClick = {
                    runCatching { store.delete(profile.id) }
                        .onSuccess { profiles = store.list() }
                        .onFailure { error = it.message }
                }
            ) {
                Text(neoLabel("حذف", "Delete"))
            }
        }
    }
}

@Composable
private fun Neo4Slider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onChange: (Float) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.labelLarge)
    Slider(value, onChange, valueRange = range)
}

@Composable
fun neo4Label(value: Any): String =
    when (value) {
        Neo4WidgetKind.CALENDAR -> neoLabel("التقويم", "Calendar")
        Neo4WidgetKind.TODAY -> neoLabel("شيفت اليوم", "Today's shift")
        Neo4WidgetKind.TASKS -> neoLabel("المهام", "Tasks")
        Neo4WidgetKind.FINANCE -> neoLabel("الملخص المالي", "Finance summary")
        Neo4WidgetTheme.FOLLOW_APP -> neoLabel("يتبع التطبيق", "Follow app")
        Neo4WidgetTheme.LIGHT -> neoLabel("فاتح", "Light")
        Neo4WidgetTheme.DARK -> neoLabel("داكن", "Dark")
        Neo4WidgetTheme.TRANSPARENT -> neoLabel("شفاف", "Transparent")
        NeoBackground.GRADIENT -> neoLabel("تدرج", "Gradient")
        NeoBackground.IMAGE -> neoLabel("صورة", "Image")
        NeoBackground.CLASSIC -> "Classic"
        NeoLayoutPreset.MINIMAL -> "Minimal"
        NeoLayoutPreset.BALANCED -> "Balanced"
        NeoLayoutPreset.RICH -> "Rich"
        NeoPaintMode.INHERIT -> neoLabel("لون مكان العمل الأصلي", "Original workplace color")
        NeoPaintMode.SOLID -> neoLabel("لون واحد", "Solid")
        NeoPaintMode.GRADIENT -> neoLabel("تدرج لوني", "Gradient")
        NeoGradientDirection.VERTICAL -> neoLabel("رأسي", "Vertical")
        NeoGradientDirection.HORIZONTAL -> neoLabel("أفقي", "Horizontal")
        NeoGradientDirection.DIAGONAL -> neoLabel("قطري", "Diagonal")
        NeoGradientStrength.SOFT -> neoLabel("ناعم", "Soft")
        NeoGradientStrength.MEDIUM -> neoLabel("متوسط", "Medium")
        NeoGradientStrength.STRONG -> neoLabel("قوي", "Strong")
        else ->
            value.toString().lowercase().split('_').joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
    }
