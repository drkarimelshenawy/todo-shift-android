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
        Text(label("إشعارات الاختبار اختيارية ومستقلة عن ربط الجهاز؛ لا تستخدمها لبيانات خاصة.", "Test notifications are optional and independent of pairing; do not use for private information."))
        OutlinedButton(enabled = !fcmBusy, onClick = {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else subscribe(true)
        }) { Text(label("تفعيل إشعارات الاختبار", "Enable test notifications")) }
        OutlinedButton(enabled = !fcmBusy, onClick = { subscribe(false) }) { Text(label("إيقاف إشعارات الاختبار", "Disable test notifications")) }
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
