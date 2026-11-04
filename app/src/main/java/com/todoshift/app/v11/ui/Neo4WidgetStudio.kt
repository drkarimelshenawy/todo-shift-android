package com.todoshift.app.v11.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.todoshift.app.widget.*

@Composable
fun Neo4WidgetStudio(onBack: () -> Unit) {
    val context = LocalContext.current; val store = remember { Neo4WidgetSettings(context.applicationContext) }
    var kind by remember { mutableStateOf(Neo4WidgetKind.CALENDAR) }; var value by remember(kind) { mutableStateOf(store.load(kind)) }
    fun save(v: Neo4WidgetOptions) { value = v; store.save(kind, v) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text(neoLabel("رجوع", "Back")) }
        Text(neoLabel("استوديو الويدجت", "Widget Studio"), style = MaterialTheme.typography.titleLarge)
        NeoChoice(neoLabel("الويدجت", "Widget"), Neo4WidgetKind.entries, kind, { neo4Label(it) }, { kind = it })
        val skin = store.skin(kind)
        Column(Modifier.fillMaxWidth().background(Color(skin.background), MaterialTheme.shapes.large).padding(18.dp)) {
            Text(neoLabel("معاينة شكلية فقط", "Visual preview only"), color = Color(skin.text))
            Text(if (kind == Neo4WidgetKind.FINANCE) "••••••" else neo4Label(kind), color = Color(skin.text), style = MaterialTheme.typography.titleLarge)
        }
        NeoChoice(neoLabel("ثيم مستقل", "Independent theme"), Neo4WidgetTheme.entries, value.theme, { neo4Label(it) }, { save(value.copy(theme = it)) })
        var accent by remember(kind, value.accent) { mutableStateOf(value.accent) }
        OutlinedTextField(accent, { accent = it.take(7); if (accent.isEmpty() || accent.matches(Regex("#[0-9a-fA-F]{6}"))) save(value.copy(accent = accent)) }, label = { Text("Accent #RRGGBB") }, modifier = Modifier.fillMaxWidth())
        Text(neoLabel("اترك اللون فارغًا ليتبع لون التطبيق", "Leave accent empty to follow the app"), style = MaterialTheme.typography.bodySmall)
        NeoSwitch(neoLabel("تفاصيل إضافية", "Extra details"), value.showDetails) { save(value.copy(showDetails = it)) }
        NeoSwitch(neoLabel("إخفاء المحتوى", "Hide content"), value.hideContent) { save(value.copy(hideContent = it)) }
        if (kind == Neo4WidgetKind.FINANCE) {
            NeoSwitch(neoLabel("السماح بالمبالغ غير المحمية فقط", "Allow unprotected amounts only"), value.showAmounts) { save(value.copy(showAmounts = it)) }
            Text(neoLabel("الحسابات المحمية تُخفى دائمًا خارج التطبيق، ولا يُحفظ فتح القفل للويدجت.", "Protected finances are always hidden outside the app. Unlock state is never shared with widgets."), style = MaterialTheme.typography.bodySmall)
        }
        Text(neoLabel("تُضاف الويدجت وتُغيّر أحجامها من الشاشة الرئيسية للهاتف. التحديث الدوري نحو 30 دقيقة وقد يؤخره Android لتوفير الطاقة.", "Add and resize widgets from your launcher. Periodic updates are approximately 30 minutes and may be delayed by Android power saving."), style = MaterialTheme.typography.bodySmall)
    }
}
