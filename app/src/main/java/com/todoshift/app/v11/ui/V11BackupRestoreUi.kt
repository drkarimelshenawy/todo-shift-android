package com.todoshift.app.v11.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoshift.app.backup.*
import com.todoshift.app.v11.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Classic compatibility entry; shares the bounded preview, safety archive and restore coordinator.
 */
@Composable
fun V11BackupRestoreDialog(
    context: Context,
    db: V11Database,
    onDatabaseRestored: (V11Database) -> Unit,
    onDismiss: () -> Unit,
    financeAuthorized: Boolean = false,
    security: V11SecuritySettings? = null,
    securityStore: V11SecurityStore? = null,
    onUnlocked: () -> Unit = {},
) {
    val app = context.applicationContext
    val coordinator = remember(app) { Neo4LocalBackup(app) }
    val adopt = LocalNeoRestoreCommitted.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var legacyExport by rememberSaveable { mutableStateOf(false) }
    var prepared by remember { mutableStateOf<Neo4PreparedRestore?>(null) }
    var expected by remember { mutableStateOf<V11Database?>(null) }

    val latestAuthorized by rememberUpdatedState(financeAuthorized)
    val rootAccess = LocalNeoFinanceAccess.current
    fun canAccess() = latestAuthorized && (rootAccess?.invoke() ?: true)
    if (!canAccess()) {
        if (security != null && securityStore != null) {
            V11FinancialVerificationDialog(
                security,
                securityStore,
                onDismiss = { if (!busy) onDismiss() },
                onVerified = onUnlocked,
            )
        } else
            AlertDialog(
                onDismissRequest = { if (!busy) onDismiss() },
                title = {
                    Text(neoLiteral("يلزم التحقق المالي", "Financial verification required"))
                },
                text = {
                    Text(
                        neoLiteral(
                            "تحتوي النسخ على بيانات مالية؛ تحقق من هويتك أولًا.",
                            "Backups contain financial data; verify your identity first.",
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = onDismiss, enabled = !busy) {
                        Text(neoLiteral("إغلاق", "Close"))
                    }
                },
            )
        return
    }

    fun export(uri: Uri) {
        if (busy || !canAccess()) return
        busy = true
        message = null
        val legacy = legacyExport
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    synchronized(Neo4DataGate.monitor) {
                        check(canAccess()) { "Financial verification required" }
                        check(V11Runtime(app).start() == db) {
                            "Data changed; reopen before exporting"
                        }
                        val raw = if (legacy) V11Storage(app).exportJson(db) else null
                        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                            if (raw != null) output.write(raw.toByteArray(Charsets.UTF_8))
                            else coordinator.export(db, output)
                        } ?: error("Unable to open backup destination")
                    }
                }
                message = neoLiteral("اكتمل إنشاء النسخة", "Backup completed")
            } catch (failure: Exception) {
                message = failure.message ?: neoLiteral("فشل التصدير", "Export failed")
            } finally {
                busy = false
            }
        }
    }

    fun inspect(uri: Uri) {
        if (busy || !canAccess()) return
        busy = true
        message = null
        prepared = null
        scope.launch {
            try {
                val preview =
                    withContext(Dispatchers.IO) {
                        check(canAccess()) { "Financial verification required" }
                        context.contentResolver.openInputStream(uri)?.use(coordinator::preview)
                            ?: error("Unable to read backup file")
                    }
                expected = db
                prepared = preview
            } catch (failure: Exception) {
                message = failure.message ?: neoLiteral("النسخة غير صالحة", "Invalid backup")
            } finally {
                busy = false
            }
        }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) {
            if (it != null) export(it)
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it != null) inspect(it)
        }
    fun startExport(legacy: Boolean) {
        legacyExport = legacy
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
        exportLauncher.launch("ToDo-Shift-$stamp.${if (legacy) "json" else "zip"}")
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(neoLiteral("النسخ والاستعادة", "Backup and restore")) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    neoLiteral(
                        "ZIP: البيانات والإعدادات والمرفقات. JSON القديم: قاعدة البيانات فقط. الملفات غير مشفرة.",
                        "ZIP: data, settings and attachments. Legacy JSON: database only. Files are not encrypted.",
                    )
                )
                OutlinedButton(
                    onClick = { startExport(false) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(neoLiteral("تصدير ZIP كامل", "Export full ZIP"))
                }
                OutlinedButton(
                    onClick = { startExport(true) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(neoLiteral("تصدير JSON القديم", "Export legacy JSON"))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(neoLiteral("فحص نسخة قبل الاستعادة", "Preview a backup"))
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(neoLiteral("إغلاق", "Close")) }
        },
    )
    prepared?.let { preview ->
        AlertDialog(
            onDismissRequest = { if (!busy) prepared = null },
            title = { Text(neoLiteral("مراجعة الاستعادة", "Review restore")) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val p = preview.preview
                    Text(
                        neoLiteral(
                            "تقاويم ${p.calendars}، أيام ${p.days}، مهام ${p.tasks}، دفعات ${p.payments}",
                            "Calendars ${p.calendars}, days ${p.days}, tasks ${p.tasks}, payments ${p.payments}",
                        )
                    )
                    if (p.legacyJson)
                        Text(
                            neoLiteral(
                                "JSON قديم دون checksums أو مرفقات مضمّنة.",
                                "Legacy JSON has no embedded checksums or attachments.",
                            )
                        )
                    Text(
                        neoLiteral(
                            "ستستبدل البيانات الحالية. تُحفظ نسخة أمان متحققة أولًا. تبقى الحماية والتوقيع محليين.",
                            "Current data will be replaced. A validated safety archive is saved first. Security credentials and signing stay local.",
                        )
                    )
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    message?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                val restored =
                                    withContext(Dispatchers.IO) {
                                        synchronized(Neo4DataGate.monitor) {
                                            check(canAccess()) { "Financial verification required" }
                                            val current = V11Runtime(app).start()
                                            check(current == expected) {
                                                "Data changed since preview; review again"
                                            }
                                            createNeoSafetyCopy(app, current)
                                            coordinator.restore(preview, preview.token)
                                        }
                                    }
                                if (adopt != null) adopt(restored)
                                else onDatabaseRestored(restored.database)
                                prepared = null
                                message =
                                    neoLiteral("اكتملت الاستعادة", "Restore committed") +
                                        restored.warnings.joinToString(
                                            prefix = if (restored.warnings.isEmpty()) "" else "\n",
                                            separator = "\n",
                                        )
                            } catch (failure: Exception) {
                                message =
                                    failure.message
                                        ?: neoLiteral("لم تتأكد الاستعادة", "Restore not confirmed")
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) {
                    Text(neoLiteral("تأكيد الاستعادة", "Confirm restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { prepared = null }, enabled = !busy) {
                    Text(neoLiteral("إلغاء", "Cancel"))
                }
            },
        )
    }
}
