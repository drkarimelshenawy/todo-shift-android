package com.exmore.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.exmore.app.backup.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun BackupExportPasswordDialog(onDismiss: () -> Unit, onReady: (CharArray) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = password.length in 12..256 && password.toSet().size >= 4 && password == confirmation
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text(neoLabel("تشفير النسخة الاحتياطية", "Encrypt backup")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(neoLabel("استخدم عبارة مرور مستقلة لا تقل عن 12 حرفًا. لا يمكن استعادة النسخة إذا نسيتها، ولا تُغير PIN التطبيق.", "Use a separate passphrase of at least 12 characters. A forgotten password cannot be recovered. This does not change the application PIN."))
                OutlinedTextField(password, { password = it.take(256) }, label = { Text(neoLabel("كلمة مرور النسخة", "Backup password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                OutlinedTextField(confirmation, { confirmation = it.take(256) }, label = { Text(neoLabel("تأكيد كلمة المرور", "Confirm password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { val secret = password.toCharArray(); password = ""; confirmation = ""; onReady(secret) }) { Text(neoLabel("متابعة", "Continue")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}

@Composable
internal fun BackupImportPasswordDialog(envelope: ByteArray, backup: Neo4LocalBackup, onDismiss: () -> Unit, onPrepared: (Neo4PreparedRestore) -> Unit) {
    var password by remember(envelope) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn, dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        title = { Text(neoLabel("فتح نسخة مشفرة", "Open encrypted backup")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(neoLabel("أدخل كلمة مرور هذه النسخة قبل عرض محتوياتها. لا يمكن استرداد كلمة مرور منسية، ولا يتغير PIN التطبيق.", "Enter this backup's password before inspecting its contents. A forgotten password cannot be recovered. The application PIN stays unchanged."))
                OutlinedTextField(password, { password = it.take(256) }, enabled = !busy, label = { Text(neoLabel("كلمة مرور النسخة", "Backup password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { Button(enabled = !busy && password.isNotEmpty(), onClick = {
            busy = true
            val secret = password.toCharArray()
            scope.launch {
                try {
                    val prepared = withContext(Dispatchers.IO) { backup.preview(envelope.inputStream(), secret) }
                    password = ""; onPrepared(prepared)
                } catch (_: Exception) { error = neoLiteral("كلمة مرور غير صحيحة أو نسخة مشفرة تالفة", "Incorrect backup password or damaged encrypted backup") }
                finally { secret.fill('\u0000'); busy = false }
            }
        }) { Text(neoLabel("فك التشفير والفحص", "Decrypt and inspect")) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(neoLabel("إلغاء", "Cancel")) } },
    )
}
