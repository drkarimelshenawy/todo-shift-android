package com.exmore.app.appcore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An explicit pilot control, not global notifications or automatic telemetry consent. */
@Composable
fun AppCorePilotSettings(arabic: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext
    fun label(ar: String, en: String) = if (arabic) ar else en
    var open by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var paired by remember { mutableStateOf(AppCorePilot.isPaired(app)) }
    var state by remember { mutableStateOf(AppCorePilot.status) }
    var message by remember { mutableStateOf("") }
    var fcmBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var releaseBusy by remember { mutableStateOf(false) }
    fun subscribe(enable: Boolean) {
        fcmBusy = true
        message = ""
        try {
            check(FirebaseApp.getInstance().options.projectId == "appcore-2026")
            val messaging = FirebaseMessaging.getInstance()
            val task = if (enable) messaging.subscribeToTopic("exmore_test") else messaging.unsubscribeFromTopic("exmore_test")
            task.addOnCompleteListener { result ->
                fcmBusy = false
                message = if (result.isSuccessful) {
                    if (enable) label("تم الاشتراك في إشعارات الاختبار فقط", "Subscribed to test notifications only")
                    else label("تم إلغاء اشتراك الاختبار", "Test subscription removed")
                } else label("لم يكتمل الطلب؛ حاول مرة أخرى", "Request failed; retry")
            }
        } catch (_: Exception) {
            fcmBusy = false
            message = label("خدمة الإشعارات غير متاحة", "Messaging unavailable")
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) subscribe(true) else message = label("لم تمنح إذن الإشعارات", "Notification permission denied")
    }
    LaunchedEffect(paired) {
        while (paired) {
            AppCorePilot.heartbeat(app)
            state = AppCorePilot.status
            delay(5000)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label("AppCore — ربط تجريبي", "AppCore — pilot connection"), style = MaterialTheme.typography.titleSmall)
        Text(if (!paired) label("غير مرتبط", "Not paired") else when (state) {
            "accepted" -> label("قبل الخادم تسجيل النسخة", "Server accepted version heartbeat")
            "retry" -> label("لم يؤكد الخادم الربط؛ ستعاد المحاولة", "Not confirmed; will retry")
            else -> label("رمز محفوظ؛ في انتظار تأكيد الخادم", "Token saved; awaiting server")
        })
        Text(label("يرسل رقم الإصدار ووقت الاتصال فقط. لا يرسل مهامك أو ملاحظاتك أو بياناتك المالية.", "Sends only the version and connection timestamp, not tasks, notes or financial records."))
        OutlinedButton(onClick = { input = ""; open = true }) { Text(label("إدخال رمز الربط", "Enter pairing token")) }
        if (paired) OutlinedButton(onClick = {
            runCatching { AppCorePilot.setToken(app, "") }.onSuccess { paired = false; state = "idle" }.onFailure { message = label("تعذر إلغاء الربط", "Could not disconnect") }
        }) { Text(label("إلغاء الربط محليًا", "Disconnect locally")) }
        if (paired) OutlinedButton(enabled = !releaseBusy, onClick = {
            releaseBusy = true
            scope.launch {
                try {
                    val release = withContext(Dispatchers.IO) { AppCoreReleaseClient.check(com.exmore.app.BuildConfig.VERSION_CODE) }
                    message = if (release == null) label("لا يوجد إصدار أحدث منشور", "No newer published release")
                    else label("إصدار اختياري متاح: ", "Optional release available: ") + release.version + " (" + release.code + ")"
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    message = label("تعذر التحقق من الإصدار؛ التطبيق المحلي يعمل", "Release check unavailable; local app still works")
                } finally { releaseBusy = false }
            }
        }) { Text(label("اختبار التحقق من التحديث", "Test release lookup")) }
        Text(label("إشعارات الاختبار اختيارية ومستقلة عن ربط الجهاز؛ لا تستخدمها لبيانات خاصة.", "Test notifications are optional and independent of pairing; do not use for private information."))
        OutlinedButton(enabled = !fcmBusy, onClick = {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else subscribe(true)
        }) { Text(label("تفعيل إشعارات الاختبار", "Enable test notifications")) }
        OutlinedButton(enabled = !fcmBusy, onClick = { subscribe(false) }) { Text(label("إيقاف إشعارات الاختبار", "Disable test notifications")) }
        Text(label("التشخيص الاختياري", "Optional diagnostics"), style = MaterialTheme.typography.titleSmall)
        Text(label("بموافقتك تُرسل إحصائيات الاستخدام وتقارير الأعطال وقياسات الأداء إلى Firebase. لا يضيف AppCore محتوى المهام أو الملاحظات أو المال إلى التقارير. قد تتضمن تقارير SDK معلومات تقنية عن الجهاز والأخطاء.", "With consent, usage statistics, crash reports and performance measurements are sent to Firebase. AppCore does not attach task, note or financial content; SDK reports may contain technical device and error details."))
        var diagnosticsEnabled by remember { mutableStateOf(AppCoreDiagnostics.enabled(app)) }
        OutlinedButton(onClick = {
            val enable = !diagnosticsEnabled
            runCatching { AppCoreDiagnostics.setEnabled(app, enable) }.onSuccess {
                diagnosticsEnabled = enable
                message = if (enable) label("تم طلب تفعيل التشخيص", "Diagnostics enabled") else label("تم طلب إيقاف التشخيص؛ قد يلزم إعادة التشغيل", "Diagnostics disable requested; restart may be required")
            }.onFailure {
                diagnosticsEnabled = AppCoreDiagnostics.enabled(app)
                message = label("تعذر تغيير التشخيص بالكامل", "Could not change all diagnostic settings")
            }
        }) { Text(if (diagnosticsEnabled) label("إيقاف التشخيص", "Disable diagnostics") else label("أوافق على تفعيل التشخيص", "Consent and enable diagnostics")) }
        Text(label("تسجيل التثبيت الاختياري", "Optional installation registration"), style = MaterialTheme.typography.titleSmall)
        Text(label("يرسل معرف Firebase عشوائيًا ورقم الإصدار ووقت الاتصال؛ لا يرسل محتوى التطبيق. يمكنك طلب حذف التسجيل.", "Uses an anonymous Firebase ID, version and last-seen time; no app content. You may request deletion."))
        var registered by remember { mutableStateOf(AppCoreRegistry.enabled(app)) }
        var pendingDeletion by remember { mutableStateOf(AppCoreRegistry.deletionPending(app)) }
        var registryBusy by remember { mutableStateOf(false) }
        OutlinedButton(enabled = !registryBusy, onClick = {
            registryBusy = true
            AppCoreRegistry.consent(app, !registered && !pendingDeletion, com.exmore.app.BuildConfig.VERSION_CODE) { result ->
                registryBusy = false
                registered = AppCoreRegistry.enabled(app)
                pendingDeletion = AppCoreRegistry.deletionPending(app)
                message = when(result) {
                    "accepted" -> label("أكد الخادم التسجيل", "Server accepted registration")
                    "removed" -> label("تم حذف التسجيل", "Registration removed")
                    "delete_pending" -> label("توقف التسجيل؛ أعد طلب الحذف عند الاتصال", "Registration stopped; retry deletion online")
                    else -> label("العملية لم تتأكد؛ راجع الاتصال وإعداد Firebase", "Not confirmed; check connection and Firebase settings")
                }
            }
        }) { Text(if(registered || pendingDeletion) label("إيقاف وحذف التسجيل", "Stop and delete registration") else label("أوافق على التسجيل الاختياري", "Consent to registration")) }
        if (message.isNotEmpty()) Text(message)
    }
    if (open) AlertDialog(onDismissRequest = { input = ""; open = false },
        title = { Text(label("رمز الربط من مدير AppCore", "Owner-issued pairing token")) },
        text = { OutlinedTextField(value = input, onValueChange = { input = it.take(128) }, singleLine = true, visualTransformation = PasswordVisualTransformation()) },
        confirmButton = { TextButton(enabled = input.trim().matches(Regex("[a-f0-9]{64}")), onClick = {
            runCatching { AppCorePilot.setToken(app, input.trim()) }.onSuccess { paired = true; state = AppCorePilot.status; open = false; input = "" }.onFailure { message = label("تعذر حفظ الرمز", "Could not save token"); input = ""; open = false }
        }) { Text(label("ربط", "Pair")) } },
        dismissButton = { TextButton(onClick = { input = ""; open = false }) { Text(label("إلغاء", "Cancel")) } })
}
