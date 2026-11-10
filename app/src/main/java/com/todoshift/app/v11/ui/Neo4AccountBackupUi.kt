package com.todoshift.app.v11.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todoshift.app.backup.*
import com.todoshift.app.cloud.*
import com.todoshift.app.v11.*
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun Neo4AccountBackupPage(
    db: V11Database,
    financeAuthorized: Boolean,
    security: V11SecuritySettings,
    securityStore: V11SecurityStore,
    onUnlocked: () -> Unit,
    onRestored: (Neo4RestoredState) -> Unit,
    onBack: () -> Unit,
    onPolicy: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext
    val backup = remember { Neo4LocalBackup(app) }
    val cloud = remember { CloudReadinessStore(app) }
    val scope = rememberCoroutineScope()
    var schedule by remember { mutableStateOf(cloud.schedule()) }
    var pendingExport by remember { mutableStateOf<Uri?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var prepared by remember { mutableStateOf<Neo4PreparedRestore?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var lastLocal by remember { mutableLongStateOf(cloud.lastLocalSuccess()) }
    var lastBytes by remember { mutableLongStateOf(cloud.lastLocalBytes()) }
    val exportPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) {
            pendingExport = it
        }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            pendingImport = it
        }
    BackHandler(
        enabled = busy
    ) { /* Restore/export cannot be abandoned halfway through a transaction. */
    }
    LaunchedEffect(financeAuthorized, pendingExport, pendingImport) {
        if (financeAuthorized && !busy && (pendingExport != null || pendingImport != null)) {
            val destination = pendingExport
            val source = pendingImport
            pendingExport = null
            pendingImport = null
            busy = true
            message = null
            failed = false
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        if (destination != null) {
                            var count = 0L
                            context.contentResolver.openOutputStream(destination, "wt")?.use { raw
                                ->
                                val counted =
                                    object : java.io.OutputStream() {
                                        override fun write(value: Int) {
                                            raw.write(value)
                                            count++
                                        }

                                        override fun write(
                                            buffer: ByteArray,
                                            offset: Int,
                                            length: Int,
                                        ) {
                                            raw.write(buffer, offset, length)
                                            count += length
                                        }

                                        override fun flush() = raw.flush()

                                        override fun close() = raw.close()
                                    }
                                backup.export(db, counted)
                            } ?: error("Cannot open backup destination")
                            cloud.localSuccess(count)
                            null
                        } else
                            context.contentResolver.openInputStream(source!!)?.use {
                                backup.preview(it)
                            } ?: error("Cannot read backup file")
                    }
                }
                result
                    .onSuccess { value ->
                        if (value == null) {
                            lastLocal = cloud.lastLocalSuccess()
                            lastBytes = cloud.lastLocalBytes()
                            message = neoLiteral("تم إنشاء النسخة المحلية", "Local backup created")
                        } else prepared = value
                    }
                    .onFailure {
                        failed = true
                        message =
                            neoLiteral("تعذر إتمام العملية: ", "Operation failed: ") +
                                (it.message ?: it.javaClass.simpleName)
                    }
                busy = false
            }
        }
    }
    if (busy)
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        ) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircularProgressIndicator()
                    Text(neoLabel("جارٍ التحقق أو حفظ النسخة…", "Validating or writing backup…"))
                }
            }
        }
    if (!financeAuthorized) {
        Column(Modifier.fillMaxSize()) {
            if (!LocalNeoShellPageHeader.current)
                TextButton(onClick = onBack) { Text(neoLabel("رجوع", "Back")) }
            Text(
                neoLabel(
                    "تحتوي النسخ على بيانات مالية؛ تحقق من هويتك أولًا",
                    "Backups contain financial data. Verify your identity first.",
                ),
                Modifier.padding(16.dp),
            )
            V11AccountsUnlockScreen(security, securityStore, onUnlocked)
        }
        return
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!LocalNeoShellPageHeader.current)
            TextButton(onClick = onBack) { Text(neoLabel("رجوع", "Back")) }
        if (!LocalNeoShellPageHeader.current)
            Text(
                neoLabel("الحساب والنسخ الاحتياطي", "Account & Backup"),
                style = MaterialTheme.typography.titleLarge,
            )
        Text(
            neoLabel("نسخ محلي يعمل فعليًا", "Local backup is available"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            neoLabel(
                "يشمل البيانات والإعدادات والتخطيطات والصور الشخصية المستخدمة. لا يشمل PIN أو البصمة أو مفاتيح الجهاز. الملف غير مشفر.",
                "Includes data, settings, profiles and referenced personal images. Excludes PINs, biometrics and device keys. The archive is not encrypted.",
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            enabled = !busy,
            onClick = { exportPicker.launch("ToDoShift-${java.time.LocalDate.now()}.zip") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(neoLabel("تصدير نسخة محلية", "Export local backup"))
        }
        OutlinedButton(
            enabled = !busy,
            onClick = {
                importPicker.launch(
                    arrayOf("application/zip", "application/json", "application/octet-stream")
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(neoLabel("فحص نسخة للاستعادة", "Inspect backup for restore"))
        }
        Text(
            if (lastLocal > 0)
                neoLabel("آخر نسخة محلية: ", "Last local backup: ") +
                    Instant.ofEpochMilli(lastLocal) +
                    " • $lastBytes bytes"
            else neoLabel("لا توجد نسخة محلية ناجحة مسجلة", "No successful local backup recorded"),
            style = MaterialTheme.typography.bodySmall,
        )
        message?.let {
            Text(
                it,
                color =
                    if (failed) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
            )
        }
        val safetyCopies = remember {
            java.io
                .File(app.filesDir, "safety-copies")
                .listFiles()
                ?.filter { it.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
        }
        if (safetyCopies.isNotEmpty()) {
            Text(
                neoLabel("نسخ أمان قبل التعديلات الكبرى", "Safety copies before major changes"),
                style = MaterialTheme.typography.titleSmall,
            )
            safetyCopies.take(5).forEach { file ->
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                prepared =
                                    withContext(Dispatchers.IO) {
                                        file.inputStream().use { backup.preview(it) }
                                    }
                            } catch (e: Exception) {
                                failed = true
                                message = e.message
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) {
                    Text(
                        neoLabel("فحص نسخة ", "Inspect copy ") +
                            java.time.Instant.ofEpochMilli(file.lastModified())
                    )
                }
            }
        }
        TextButton(onClick = onPolicy) {
            Text(neoLabel("سياسة النسخ والمزامنة", "Backup & Sync Policy"))
        }
        HorizontalDivider()
        var cloudExpanded by remember { mutableStateOf(false) }
        TextButton(onClick = { cloudExpanded = !cloudExpanded }) {
            Text(
                neoLabel(
                    "خدمات Google الاختيارية (غير متاحة)",
                    "Optional Google services (unavailable)",
                )
            )
        }
        if (cloudExpanded) {
            Text(
                neoLabel(
                    "غير متصل: النسخ اليدوي متاح، والنسخ السحابي والجدولة غير مفعّلين.",
                    "Not connected: manual backup is available; cloud backup and scheduling are inactive.",
                )
            )
            Text(
                neoLabel("تفضيلات محفوظة دون تشغيل: ", "Saved inactive preferences: ") +
                    schedule.frequency.name +
                    " • Wi-Fi: " +
                    schedule.wifiOnly,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    prepared?.let { pending ->
        AlertDialog(
            onDismissRequest = { if (!busy) prepared = null },
            title = { Text(neoLabel("مراجعة قبل الاستعادة", "Review before restoring")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${pending.preview.createdAt} • ${pending.preview.appVersion}")
                    Text(
                        neoLabel(
                            "تقاويم: ${pending.preview.calendars} • أيام: ${pending.preview.days} • مهام: ${pending.preview.tasks} • دفعات: ${pending.preview.payments} • صور: ${pending.preview.images}",
                            "Calendars: ${pending.preview.calendars} • days: ${pending.preview.days} • tasks: ${pending.preview.tasks} • payments: ${pending.preview.payments} • images: ${pending.preview.images}",
                        )
                    )
                    Text(
                        neoLabel(
                            "ستُستبدل البيانات الحالية بعد تأكيدك. يُستحسن تصدير نسخة منها أولًا. لا تتغير وسائل الحماية الحالية.",
                            "Confirmation replaces current data. Export a backup of current data first. Existing authentication is kept.",
                        )
                    )
                    if (pending.preview.legacyJson)
                        Text(
                            neoLabel(
                                "هذه صيغة JSON قديمة: تستعيد V11 فقط وتحتفظ بأماكن العمل والإعدادات الحالية.",
                                "Legacy JSON restores V11 only, preserving current workplaces and settings.",
                            )
                        )
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching {
                                    withContext(Dispatchers.IO) {
                                        backup.restore(pending, pending.token)
                                    }
                                }
                                .onSuccess { value ->
                                    prepared = null
                                    onRestored(value)
                                    failed = false
                                    message =
                                        neoLiteral(
                                            "تمت الاستعادة المحلية",
                                            "Local restore completed",
                                        ) +
                                            if (value.warnings.isEmpty()) ""
                                            else " • " + value.warnings.joinToString()
                                }
                                .onFailure {
                                    failed = true
                                    message =
                                        neoLiteral("تعذرت الاستعادة: ", "Restore failed: ") +
                                            it.message
                                }
                            busy = false
                        }
                    },
                ) {
                    Text(neoLabel("تأكيد الاستعادة", "Confirm restore"))
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { prepared = null }) {
                    Text(neoLabel("إلغاء", "Cancel"))
                }
            },
        )
    }
}
