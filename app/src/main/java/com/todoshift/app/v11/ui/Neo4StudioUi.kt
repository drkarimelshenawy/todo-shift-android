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
            NeoLanguage.ENGLISH -> true
            NeoLanguage.ARABIC -> false
            else ->
                androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language != "ar"
        }
    var component by
        androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("CALENDAR") }
    CompositionLocalProvider(
        LocalNeoAppearance provides draft,
        LocalNeoEnglish provides english,
        LocalNeoOnChange provides {},
        LocalNeoFeatures provides NeoFeatureNavigator { _, _ -> },
        LocalV11NeoEnabled provides true,
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
            Surface(shape = MaterialTheme.shapes.medium) {
                Box {
                    Neo4Atmosphere(Modifier.matchParentSize(), draft)
                    Column(
                        Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(
                                neoLabel("معاينة حية", "Live preview"),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            NeoChoice(
                                neoLabel("المكوّن", "Component"),
                                listOf("CALENDAR", "HEADER", "NAVIGATION", "TASK"),
                                component,
                                {
                                    when (it) {
                                        "CALENDAR" -> neoLabel("التقويم", "Calendar")
                                        "HEADER" -> neoLabel("الرأس", "Header")
                                        "NAVIGATION" -> neoLabel("التنقل", "Navigation")
                                        else -> neoLabel("مهمة", "Task")
                                    }
                                },
                                { component = it },
                            )
                        }
                        when (component) {
                            "HEADER" ->
                                Neo4CompactHeader(
                                    neoLabel("التقويم", "Calendar"),
                                    true,
                                    false,
                                    false,
                                    {},
                                    {},
                                    {},
                                    {},
                                    false,
                                    {},
                                )
                            "NAVIGATION" -> V11PremiumBottomBar(0, {})
                            "TASK" ->
                                TaskCardV11(
                                    TaskV11(
                                        "preview-task",
                                        neoLabel("مراجعة الجدول", "Review schedule"),
                                        date = java.time.LocalDate.now().toString(),
                                    ),
                                    {},
                                    {},
                                    {},
                                )
                            else -> {
                                val day = java.time.LocalDate.now()
                                val places =
                                    listOf(
                                        com.todoshift.app.data.Workplace(
                                            "preview-a",
                                            neoLabel("العيادة", "Clinic"),
                                            "#00A4B8",
                                        ),
                                        com.todoshift.app.data.Workplace(
                                            "preview-b",
                                            neoLabel("المستشفى", "Hospital"),
                                            "#6C3A9B",
                                        ),
                                    )
                                val metrics = neoCellMetrics()
                                val largeType =
                                    androidx.compose.ui.platform.LocalDensity.current.fontScale >
                                        1.3f
                                val details =
                                    (if (draft.showNotes) 1 else 0) +
                                        (if (draft.showShiftNames) 1 else 0)
                                val icons = if (largeType && draft.showNoteIcons) 1 else 0
                                val badges = if (largeType && draft.showTaskIndicators) 2 else 1
                                val height =
                                    maxOf(
                                        metrics.dateHeight +
                                            metrics.captionHeight * (details + icons + badges) +
                                            12.dp,
                                        72.dp,
                                    )
                                CompositionLocalProvider(
                                    androidx.compose.ui.platform.LocalLayoutDirection provides
                                        neoDirection(
                                            draft.gridDirection,
                                            androidx.compose.ui.platform.LocalLayoutDirection
                                                .current,
                                        )
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().height(height),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        repeat(3) { index ->
                                            val segments =
                                                when (index) {
                                                    0 ->
                                                        listOf(
                                                            ShiftSegmentV11(
                                                                "p24",
                                                                ShiftKind.FULL_DAY,
                                                                "preview-a",
                                                            )
                                                        )
                                                    1 ->
                                                        listOf(
                                                            ShiftSegmentV11(
                                                                "pm",
                                                                ShiftKind.MORNING,
                                                                "preview-a",
                                                            ),
                                                            ShiftSegmentV11(
                                                                "pe",
                                                                ShiftKind.EVENING,
                                                                "preview-b",
                                                            ),
                                                        )
                                                    else ->
                                                        listOf(
                                                            ShiftSegmentV11("poff", ShiftKind.OFF)
                                                        )
                                                }
                                            val date = day.plusDays(index.toLong())
                                            NeoCalendarCell(
                                                date,
                                                listOf(
                                                    DayEntryV11(
                                                        "p$index",
                                                        date.toString(),
                                                        "preview",
                                                        segments,
                                                    )
                                                ),
                                                if (index == 0) 1 else 0,
                                                0,
                                                if (index == 0)
                                                    listOf(
                                                        DayItemV11(
                                                            "preview-note",
                                                            date.toString(),
                                                            type = DayItemTypeV11.NOTE,
                                                            title =
                                                                neoLabel("📌 مراجعة", "📌 Review"),
                                                        )
                                                    )
                                                else emptyList(),
                                                places,
                                                org.json.JSONObject(),
                                                index == 1,
                                                false,
                                                false,
                                                Modifier.weight(1f).fillMaxHeight(),
                                                {},
                                                metrics,
                                                {},
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
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf(emptyList<Neo4LayoutProfile>()) }
    var name by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    LaunchedEffect(store) {
        try {
            profiles = withContext(Dispatchers.IO) {
                synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) { store.list() }
            }
        } finally { busy = false }
    }
    fun write(action: () -> Unit, completed: () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                profiles = withContext(Dispatchers.IO) {
                    synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                        action()
                        store.list()
                    }
                }
                completed()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = failure.message ?: "Profile save failed"
            } finally { busy = false }
        }
    }
    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    OutlinedTextField(
        name,
        { name = it.take(60) },
        label = { Text(neoLabel("اسم التخطيط", "Profile name")) },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        enabled = !busy && name.isNotBlank() && profiles.size < 12,
        onClick = {
            val submittedName = name
            val submittedDraft = draft
            write({ store.save(submittedName, submittedDraft) }) {
                if (name == submittedName) name = ""
            }
        },
    ) {
        Text(neoLabel("حفظ المعاينة كتخطيط", "Save preview as profile"))
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    profiles.forEach { profile ->
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                enabled = !busy,
                onClick = { update(profile.appearance.preservingLocaleOf(draft)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(profile.name)
            }
            TextButton(
                enabled = !busy,
                onClick = {
                    write({ store.delete(profile.id) })
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
        NeoBackground.CLASSIC -> neoLabel("سادة", "Plain")
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
