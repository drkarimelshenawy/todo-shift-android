package com.todoshift.app.v11.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import com.todoshift.app.widget.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun Neo4WidgetStudio(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Neo4WidgetSettings(context.applicationContext) }
    var kind by rememberSaveable { mutableStateOf(Neo4WidgetKind.CALENDAR) }
    var value by remember(kind) { mutableStateOf(store.load(kind)) }
    val scope=rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var opacity by remember(kind,value.backgroundOpacity) { mutableStateOf(value.backgroundOpacity) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var accent by rememberSaveable(kind, value.accent) { mutableStateOf(value.accent) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var discard by remember { mutableStateOf(false) }
    fun request(next: () -> Unit) {
        if (accent != value.accent || opacity != value.backgroundOpacity) {
            pending = next
            discard = true
        } else next()
    }
    neoRegisterLeaveGuard { request(it) }
    androidx.activity.compose.BackHandler { request(onBack) }
    if (discard)
        AlertDialog(
            onDismissRequest = {
                discard = false
                pending = null
            },
            title = { Text(neoLabel("تجاهل تعديلات الويدجت؟", "Discard unsaved widget changes?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        accent = value.accent
                        opacity = value.backgroundOpacity
                        discard = false
                        pending?.invoke()
                        pending = null
                    }
                ) {
                    Text(neoLabel("تجاهل", "Discard"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        discard = false
                        pending = null
                    }
                ) {
                    Text(neoLabel("متابعة", "Keep editing"))
                }
            },
        )
    fun save(next: Neo4WidgetOptions) {
        if(saving) return
        val target=kind;saving=true
        scope.launch {
            try { withContext(Dispatchers.IO) { store.save(target,next) };if(kind==target) value=next;saveError=null }
            catch(failure: Exception) { saveError=failure.message ?: "Widget settings could not be saved" }
            finally {saving=false}
        }
    }
    val fixture = remember {
        val date = LocalDate.now().toString()
        V11Database(
            calendars = listOf(AppCalendarV11("preview", "Preview", "#00A4B8")),
            days =
                listOf(
                    DayEntryV11(
                        "p",
                        date,
                        "preview",
                        listOf(
                            ShiftSegmentV11(
                                "ps",
                                ShiftKind.MORNING,
                                "preview-a",
                                startMinute = 540,
                                endMinute = 900,
                            )
                        ),
                    )
                ),
            tasks = listOf(TaskV11("pt", "Preview task", date = date, calendarId = "preview")),
        )
    }
    val security = runCatching { V11SecurityStore(context).load() }.getOrNull()
    val content =
        NeoWidgetContentModel.project(
            kind,
            fixture,
            value,
            security,
            Neo4WidgetPrivacy.amountsHidden(context),
            LocalDateTime.now(),
            LocalNeoEnglish.current,
        )
    val skin = store.skin(kind)
    val appearance = LocalNeoAppearance.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeoPageHeader(neoLabel("استوديو الويدجت", "Widget Studio")) { request(onBack) }
        NeoChoice(
            neoLabel("الويدجت", "Widget"),
            Neo4WidgetKind.entries,
            kind,
            { neo4Label(it) },
            { next -> request { kind = next } },
        )
        Column(
            Modifier.fillMaxWidth()
                .background(if(kind==Neo4WidgetKind.CALENDAR) Color(skin.panelColor).copy(alpha=opacity) else Color(skin.background), MaterialTheme.shapes.medium)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                neoLabel("معاينة ببيانات تجريبية", "Synthetic preview"),
                color = Color(skin.secondary),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                content.title,
                color = Color(skin.accent),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                content.value,
                color = Color(skin.text),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (content.detail.isNotEmpty()) Text(content.detail, color = Color(skin.secondary))
            if (kind == Neo4WidgetKind.CALENDAR && !value.hideContent) {
                val segments = NeoWidgetContentModel.segments(fixture, LocalDate.now())
                val colors = mapOf("preview-a" to android.graphics.Color.parseColor("#00A4B8"))
                val bitmap =
                    remember(appearance, segments) {
                        Neo4WidgetPainter.render(segments, colors, appearance)
                    }
                Image(
                    bitmap.asImageBitmap(),
                    neoLabel(
                        "نفس باندات الرسم المستخدمة في ويدجت التقويم",
                        "Calendar widget production paint bands",
                    ),
                    Modifier.size(64.dp),
                )
            }
            content.rows.take(2).forEach { Text(it, color = Color(skin.text)) }
        }
        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        NeoChoice(
            neoLabel("ثيم مستقل", "Independent theme"),
            Neo4WidgetTheme.entries,
            value.theme,
            { neo4Label(it) },
            { theme -> save(value.copy(theme = theme, backgroundOpacity =
                if(theme == Neo4WidgetTheme.TRANSPARENT) 0f
                else if(value.backgroundOpacity == 0f) .85f else value.backgroundOpacity)) },
        )
        val valid = accent.isEmpty() || accent.matches(Regex("#[0-9a-fA-F]{6}"))
        OutlinedTextField(
            accent,
            {
                accent = it.take(7)
                if (accent.isEmpty() || accent.matches(Regex("#[0-9a-fA-F]{6}")))
                    save(value.copy(accent = accent))
            },
            isError = !valid,
            label = { Text("Accent #RRGGBB") },
            modifier = Modifier.fillMaxWidth(),
        )
        if(kind==Neo4WidgetKind.CALENDAR) {
            Text(neoLabel("وضوح خلفية الويدجت", "Background opacity"))
            Slider(value=opacity,onValueChange={opacity=it},valueRange=0f..1f,enabled=!saving,
                onValueChangeFinished={save(value.copy(backgroundOpacity=opacity))})
        }
        if(kind==Neo4WidgetKind.TASKS) NeoSwitch(neoLabel("إظهار المهام المكتملة", "Show completed tasks"),value.showCompleted) {save(value.copy(showCompleted=it))}
        NeoSwitch(neoLabel("تفاصيل إضافية", "Extra details"), value.showDetails) {
            save(value.copy(showDetails = it))
        }
        NeoSwitch(neoLabel("إخفاء المحتوى", "Hide content"), value.hideContent) {
            save(value.copy(hideContent = it))
        }
        if (kind == Neo4WidgetKind.FINANCE) {
            NeoSwitch(
                neoLabel("السماح بالمبالغ غير المحمية فقط", "Allow unprotected amounts only"),
                value.showAmounts,
            ) {
                save(value.copy(showAmounts = it))
            }
            Text(
                neoLabel(
                    "الحسابات المحمية تُخفى دائمًا خارج التطبيق، ولا تُنقل حالة فتح القفل إلى الويدجت.",
                    "Protected finances are always hidden outside the app; unlock state is never shared with widgets.",
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            neoLabel(
                "تُضاف الويدجت وتُغيّر أحجامها من المشغّل. قد يؤخر Android التحديث؛ يلزم اختبار المشغّل الفعلي.",
                "Add and resize widgets in your launcher. Android may defer updates; actual launcher acceptance is required.",
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
