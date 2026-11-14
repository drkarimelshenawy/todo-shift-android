package com.todoshift.app.v11.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
    var encryptedInput by remember { mutableStateOf<ByteArray?>(null) }
    var exportPassword by remember { mutableStateOf<CharArray?>(null) }
    var requestExportPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var exportScope by remember { mutableStateOf(BackupSection.entries.toSet()) }
    var restoreScope by remember { mutableStateOf(BackupSection.entries.toSet()) }
    var restoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var inspection by remember { mutableStateOf<RestoreInspection?>(null) }
    var inspectionError by remember { mutableStateOf<String?>(null) }
    var backupAuthorized by remember(security) { mutableStateOf(security.mode == V11LockMode.NONE && !security.biometricEnabled) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner, security) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && (security.mode != V11LockMode.NONE || security.biometricEnabled)) backupAuthorized = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(prepared, restoreScope, restoreMode) {
        inspection = null
        inspectionError = null
        prepared?.let { value ->
            runCatching { withContext(Dispatchers.IO) { backup.inspect(value, restoreScope, restoreMode) } }
                .onSuccess { inspection = it }.onFailure { inspectionError = it.message }
        }
    }
    var lastLocal by remember { mutableLongStateOf(cloud.lastLocalSuccess()) }
    var lastBytes by remember { mutableLongStateOf(cloud.lastLocalBytes()) }
    val exportPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) {
            pendingExport = it
            if (it == null) { exportPassword?.fill('\u0000'); exportPassword = null }
        }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            pendingImport = it
        }
    BackHandler(
        enabled = busy
    ) { /* Restore/export cannot be abandoned halfway through a transaction. */
    }
    LaunchedEffect(backupAuthorized, pendingExport, pendingImport) {
        if (backupAuthorized && !busy && (pendingExport != null || pendingImport != null)) {
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
                                val secret = exportPassword ?: error("Backup password required")
                                try { backup.exportEncrypted(db, counted, exportScope, secret) }
                                finally { secret.fill('\u0000'); exportPassword = null }
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
                        } else { restoreScope = value.preview.availableSections; prepared = value }
                    }
                    .onFailure {
                        if (it is BackupPasswordRequired) encryptedInput = it.envelope
                        failed = it !is BackupPasswordRequired
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
    if (!backupAuthorized) {
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
            V11AccountsUnlockScreen(security, securityStore) { backupAuthorized = true; onUnlocked() }
        }
        return
    }
    if (requestExportPassword) BackupExportPasswordDialog(onDismiss = { requestExportPassword = false }) { secret ->
        exportPassword?.fill('\u0000'); exportPassword = secret; requestExportPassword = false
        exportPicker.launch("ToDoShift-${java.time.LocalDate.now()}.tdsb")
    }
    encryptedInput?.let { envelope -> BackupImportPasswordDialog(envelope, backup, onDismiss = { encryptedInput = null }) { value ->
        restoreScope = value.preview.availableSections; prepared = value; encryptedInput = null; message = null; failed = false
    } }
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
                "تُشفر النسخة بكلمة مرور مستقلة. تشمل البيانات والإعدادات والتخطيطات، دون PIN أو البصمة أو مفاتيح الجهاز. تُقبل النسخ القديمة أيضًا.",
                "Backups are encrypted with a separate password. Includes data, settings and profiles; excludes PINs, biometrics and device keys. Older ZIP and JSON backups can still be imported.",
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        BackupScopeControls(exportScope) { exportScope = it }
        Button(
            enabled = !busy && exportScope.isNotEmpty(),
            onClick = { requestExportPassword = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(neoLabel("تصدير نسخة محلية مشفرة", "Export encrypted local backup"))
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
                ?.filter { it.extension in setOf("zip","sealed") }
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
                                        openNeoSafetyCopy(file).use { backup.preview(it) }
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
                    "Google Drive — غير متاح حاليًا",
                    "Google Drive — currently unavailable",
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
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackupScopeControls(restoreScope, pending.preview.availableSections) { restoreScope = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RestoreMode.entries.forEach { mode -> FilterChip(selected = restoreMode == mode, onClick = { restoreMode = mode }, label = { Text(if (mode == RestoreMode.MERGE) neoLabel("دمج", "Merge") else neoLabel("استبدال", "Replace")) }) }
                    }
                    Text("${pending.preview.createdAt} • ${pending.preview.appVersion}")
                    Text(neoLabel("ملاحظات: ${pending.preview.notes} • ورديات: ${pending.preview.shifts} • أحداث: ${pending.preview.events}","Notes: ${pending.preview.notes} • shifts: ${pending.preview.shifts} • events: ${pending.preview.events}"))
                    Text(
                        neoLabel(
                            "تقاويم: ${pending.preview.calendars} • أيام: ${pending.preview.days} • مهام: ${pending.preview.tasks} • دفعات: ${pending.preview.payments} • صور: ${pending.preview.images}",
                            "Calendars: ${pending.preview.calendars} • days: ${pending.preview.days} • tasks: ${pending.preview.tasks} • payments: ${pending.preview.payments} • images: ${pending.preview.images}",
                        )
                    )
                    Text(
                        neoLabel(
                            "تُنشأ نسخة أمان تلقائيًا. الاستبدال يخص الأقسام المحددة فقط، وتُحفظ التبعيات المشتركة الحالية.",
                            "A safety snapshot is created automatically. Replace affects selected sections; current shared dependencies are preserved.",
                        )
                    )
                    inspection?.let { plan ->
                        if(plan.details.isNotEmpty()) {
                            Text(neoLabel("تفاصيل الفحص (أول ٢٠٠ سجل من ${plan.details.size})","Inspection details (first 200 of ${plan.details.size} records)"),style=MaterialTheme.typography.titleSmall)
                            plan.details.take(200).forEach {Text(it,style=MaterialTheme.typography.labelSmall)}
                        }
                        Text(neoLabel("إضافات: ${plan.additions} • متطابق: ${plan.duplicates} • تعارضات: ${plan.conflicts} • استبدال: ${plan.replacements} • تبعيات مشتركة: ${plan.dependencies}", "Additions: ${plan.additions} • duplicates: ${plan.duplicates} • conflicts: ${plan.conflicts} • replacements: ${plan.replacements} • shared dependencies: ${plan.dependencies}"))
                        if (BackupSection.CASHBOOK in restoreScope) Text(neoLabel("حسابات: ${plan.cashAccounts} • قيود: ${plan.cashTransactions} • مديونيات: ${plan.cashDebts}", "Accounts: ${plan.cashAccounts} • transactions: ${plan.cashTransactions} • debts: ${plan.cashDebts}"))
                        if (BackupSection.CASHBOOK in restoreScope) Text(neoLabel("تُفحص قيود كاش بوك قبل الحفظ؛ يُرفض تعارض المعرفات والدفعات المرتبطة.", "Cash Book identifiers and linked receipts are validated before writing; conflicts are rejected."))
                    }
                    inspectionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (restoreMode == RestoreMode.MERGE && (inspection?.conflicts ?: 0) > 0) Text(neoLabel("توجد تعارضات: راجع النسخة واختر الاستبدال إن كان مقصودًا.", "Conflicting records: review the backup and choose Replace if intended."), color = MaterialTheme.colorScheme.error)
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
                    enabled = !busy && inspection != null && restoreScope.isNotEmpty() && !(restoreMode == RestoreMode.MERGE && inspection!!.conflicts > 0),
                    onClick = {
                        scope.launch {
                            busy = true
                            runCatching {
                                    withContext(Dispatchers.IO) {
                                        backup.restore(pending, pending.token, restoreScope, restoreMode, inspection!!.currentFingerprint)
                                    }
                                }
                                .onSuccess { value ->
                                    prepared = null
                                    onRestored(value)
                                    failed = false
                                    message =
                                        neoLiteral(
                                            "تمت الاستعادة المحلية والتحقق من الحفظ. إضافات: ${inspection?.additions ?: 0} • استبدالات: ${inspection?.replacements ?: 0} • متطابق: ${inspection?.duplicates ?: 0}",
                                            "Restore committed and validated. Additions: ${inspection?.additions ?: 0} • replacements: ${inspection?.replacements ?: 0} • duplicates: ${inspection?.duplicates ?: 0}",
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

@Composable
private fun BackupScopeControls(selected: Set<BackupSection>, available: Set<BackupSection> = BackupSection.entries.toSet(), onChange: (Set<BackupSection>) -> Unit) {
    val presets = listOf(
        neoLabel("الكل", "Full application") to available,
        neoLabel("التقويم", "Calendar only") to setOf(BackupSection.CALENDAR),
        neoLabel("التقويم والمستحقات", "Calendar + Salary") to setOf(BackupSection.CALENDAR, BackupSection.SALARY),
        neoLabel("كاش بوك كامل", "Complete Cash Book") to setOf(BackupSection.CASHBOOK, BackupSection.SALARY),
        neoLabel("المهام والملاحظات", "Notes + Tasks") to setOf(BackupSection.TASKS),
        neoLabel("الإعدادات", "Settings only") to setOf(BackupSection.SETTINGS),
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        presets.filter { (_, scope) -> available.containsAll(scope) }.forEach { (label, scope) ->
            FilterChip(selected == scope, onClick = { onChange(scope) }, label = { Text(label) })
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        available.forEach { section -> FilterChip(section in selected, onClick = { onChange(if (section in selected) selected - section else selected + section) }, label = { Text(when (section) {
            BackupSection.CALENDAR -> neoLabel("التقويم", "Calendar")
            BackupSection.TASKS -> neoLabel("المهام والملاحظات", "Tasks + Notes")
            BackupSection.SALARY -> neoLabel("المستحقات", "Salary")
            BackupSection.CASHBOOK -> neoLabel("الإدارة المالية", "Cash Book")
            BackupSection.SETTINGS -> neoLabel("الإعدادات", "Settings")
        }) }) }
    }
}
