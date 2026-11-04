package com.todoshift.app.v11.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Neo4LegalHub(onBack: () -> Unit) {
    val context = LocalContext.current; val en = LocalNeoEnglish.current
    var selected by remember { mutableStateOf<String?>(null) }
    BackHandler { if (selected == null) onBack() else selected = null }
    val document by produceState<String?>(null, selected, en) {
        value = null
        selected?.let { slug -> value = withContext(Dispatchers.IO) { runCatching { context.assets.open("legal/$slug.${if (en) "en" else "ar"}.txt").bufferedReader().use { it.readText() } }.getOrDefault(if (en) "Document unavailable" else "المستند غير متاح") } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { if (selected == null) onBack() else selected = null }) { Text(neoLabel("رجوع", "Back")) }
        if (selected == null) {
            Text(neoLabel("الخصوصية والسياسات", "Privacy & Legal"), style = MaterialTheme.typography.titleLarge)
            Text(neoLabel("نصوص مراجعة؛ بيانات المطور وروابط النشر لم تُستكمل.", "Review drafts; developer details and publication URLs are incomplete."))
            listOf("privacy" to neoLabel("سياسة الخصوصية", "Privacy Policy"), "terms" to neoLabel("شروط الاستخدام", "Terms of Use"),
                "backup-sync" to neoLabel("سياسة النسخ والمزامنة", "Backup & Sync Policy"), "google-data" to neoLabel("استخدام بيانات Google", "Google data usage")).forEach { (slug, title) ->
                OutlinedButton(onClick = { selected = slug }, modifier = Modifier.fillMaxWidth()) { Text(title) }
            }
        } else if (document == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else Text(document.orEmpty(), style = MaterialTheme.typography.bodyMedium)
    }
}
