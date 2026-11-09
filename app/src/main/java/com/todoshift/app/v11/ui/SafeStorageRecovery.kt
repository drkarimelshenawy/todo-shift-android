package com.todoshift.app.v11.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todoshift.app.backup.*
import com.todoshift.app.v11.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Available on failed boot; never initializes a database or bypasses existing authentication. */
@Composable
fun SafeStorageRecovery(onDismiss: () -> Unit, onRecovered: () -> Unit) {
    val context = LocalContext.current
    val securityStore = remember { V11SecurityStore(context.applicationContext) }
    val security = remember { runCatching { securityStore.load() }.getOrNull() }
    var unlocked by remember { mutableStateOf(security != null && !security.protectAccounts && security.mode == V11LockMode.NONE && !security.biometricEnabled) }
    val backup = remember { Neo4LocalBackup(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var prepared by remember { mutableStateOf<Neo4PreparedRestore?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && unlocked && !busy) {
            busy = true
            scope.launch {
                try { prepared = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { backup.preview(it) } ?: error("Cannot open backup") } }
                catch (failure: Exception) { message = failure.message ?: "Backup could not be validated" }
                finally { busy = false }
            }
        }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("استعادة آمنة / Safe recovery", style = MaterialTheme.typography.titleLarge)
                Text("لا يتم حذف البيانات تلقائيًا. ستُحفظ القيم الحالية قبل الاستبدال. / Current raw preferences are preserved privately before replacement.")
                when {
                    security == null -> Text("تعذر التحقق من الحماية؛ الاستعادة محظورة. / Security settings cannot be read; recovery is blocked.")
                    !unlocked -> V11AccountsUnlockScreen(security, securityStore) { unlocked = true }
                    busy -> CircularProgressIndicator()
                    else -> {
                        OutlinedButton(onClick = { picker.launch(arrayOf("application/zip", "application/json", "application/octet-stream")) }) { Text("فحص نسخة / Inspect backup") }
                        prepared?.let { p ->
                            Text("${p.preview.calendars} calendars • ${p.preview.days} days • ${p.preview.tasks} tasks • ${p.preview.payments} payments")
                            Text("سيستبدل التأكيد البيانات الحالية بالنسخة المختارة. لا يُغيّر PIN أو البصمة. / Confirmation replaces current data with this backup. PIN and biometric settings remain unchanged.")
                            Button(onClick = {
                                busy = true
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { backup.recoverFromBackup(p, p.token) }
                                        onRecovered()
                                    } catch (failure: Exception) { message = failure.message ?: "Recovery failed" }
                                    finally { busy = false }
                                }
                            }) { Text("تأكيد الاستبدال / Confirm replacement") }
                        }
                    }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(enabled = !busy, onClick = onDismiss) { Text("إلغاء / Cancel") }
            }
        }
    }
}
