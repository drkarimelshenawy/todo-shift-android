package com.todoshift.app.v11.ui

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

private enum class SecurityCredentialDialog {
    NONE,
    PIN,
    PATTERN,
    VERIFY_PIN_FOR_MODE_CHANGE,
    VERIFY_PATTERN_FOR_MODE_CHANGE
}

private enum class PatternSetupStep {
    VERIFY_CURRENT,
    DRAW_NEW,
    CONFIRM_NEW
}

@Composable
fun V11SecuritySettingsDialog(
    store: V11SecurityStore,
    current: V11SecuritySettings,
    onDismiss: () -> Unit,
    onChanged: (V11SecuritySettings) -> Unit
) {
    val context = LocalContext.current

    var settings by remember(current) {
        mutableStateOf(current)
    }

    var credentialDialog by remember {
        mutableStateOf(SecurityCredentialDialog.NONE)
    }

    var pendingMode by remember {
        mutableStateOf<V11LockMode?>(null)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val hadPin = remember(current) {
        store.hasPin()
    }

    val hadPattern = remember(current) {
        store.hasPattern()
    }

    val biometricAvailable = remember {
        val allowed =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        BiometricManager.from(context)
            .canAuthenticate(allowed) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    val protectionAvailable =
        settings.mode != V11LockMode.NONE ||
            settings.biometricEnabled

    fun requestModeChange(mode: V11LockMode) {
        message = null

        if (mode == settings.mode) return

        when {
            current.mode == V11LockMode.PIN &&
                hadPin &&
                mode != V11LockMode.PIN -> {
                pendingMode = mode
                credentialDialog =
                    SecurityCredentialDialog.VERIFY_PIN_FOR_MODE_CHANGE
            }

            current.mode == V11LockMode.PATTERN &&
                hadPattern &&
                mode != V11LockMode.PATTERN -> {
                pendingMode = mode
                credentialDialog =
                    SecurityCredentialDialog.VERIFY_PATTERN_FOR_MODE_CHANGE
            }

            mode == V11LockMode.PIN -> {
                credentialDialog =
                    SecurityCredentialDialog.PIN
            }

            mode == V11LockMode.PATTERN -> {
                credentialDialog =
                    SecurityCredentialDialog.PATTERN
            }

            else -> {
                settings = settings.copy(mode = mode)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Column {
                Text(
                    "الأمان والخصوصية",
                    fontWeight = FontWeight.Black
                )

                Text(
                    "اختر طريقة حماية قسم الحسابات",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },

        text = {
            Column(
                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    )
            ) {
                SecuritySectionTitle(
                    "طريقة الحماية الأساسية"
                )

                SecurityModeCardV3(
                    title = "بدون قفل",
                    subtitle = "بدون PIN أو نمط",
                    selected =
                        settings.mode == V11LockMode.NONE,
                    actionText = null,
                    onSelect = {
                        requestModeChange(
                            V11LockMode.NONE
                        )
                    },
                    onAction = {}
                )

                SecurityModeCardV3(
                    title = "PIN",
                    subtitle =
                        if (
                            settings.mode == V11LockMode.PIN &&
                            store.hasPin()
                        )
                            "PIN مُعيّن حالياً"
                        else
                            "رقم سري من 4 إلى 12 رقماً",
                    selected =
                        settings.mode == V11LockMode.PIN,
                    actionText =
                        if (
                            settings.mode == V11LockMode.PIN &&
                            store.hasPin()
                        )
                            "تغيير PIN"
                        else
                            "تحديد PIN",
                    onSelect = {
                        if (
                            settings.mode == V11LockMode.PIN &&
                            store.hasPin()
                        ) {
                            credentialDialog =
                                SecurityCredentialDialog.PIN
                        } else {
                            requestModeChange(
                                V11LockMode.PIN
                            )
                        }
                    },
                    onAction = {
                        if (
                            settings.mode == V11LockMode.PIN &&
                            store.hasPin()
                        ) {
                            credentialDialog =
                                SecurityCredentialDialog.PIN
                        } else {
                            requestModeChange(
                                V11LockMode.PIN
                            )
                        }
                    }
                )

                SecurityModeCardV3(
                    title = "النمط",
                    subtitle =
                        if (
                            settings.mode == V11LockMode.PATTERN &&
                            store.hasPattern()
                        )
                            "نمط حماية مُعيّن حالياً"
                        else
                            "نمط حماية 3 × 3",
                    selected =
                        settings.mode == V11LockMode.PATTERN,
                    actionText =
                        if (
                            settings.mode == V11LockMode.PATTERN &&
                            store.hasPattern()
                        )
                            "تغيير النمط"
                        else
                            "تحديد النمط",
                    onSelect = {
                        if (
                            settings.mode == V11LockMode.PATTERN &&
                            store.hasPattern()
                        ) {
                            credentialDialog =
                                SecurityCredentialDialog.PATTERN
                        } else {
                            requestModeChange(
                                V11LockMode.PATTERN
                            )
                        }
                    },
                    onAction = {
                        if (
                            settings.mode == V11LockMode.PATTERN &&
                            store.hasPattern()
                        ) {
                            credentialDialog =
                                SecurityCredentialDialog.PATTERN
                        } else {
                            requestModeChange(
                                V11LockMode.PATTERN
                            )
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                SecuritySectionTitle("البصمة")

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default.Fingerprint,
                            null
                        )
                    },
                    title = "استخدام البصمة",
                    subtitle =
                        if (biometricAvailable)
                            "اختيار إضافي مستقل لفتح الحسابات"
                        else
                            "البصمة أو قفل الجهاز غير متاح حالياً",
                    checked = settings.biometricEnabled,
                    enabled = biometricAvailable,
                    onCheckedChange = {
                        settings =
                            settings.copy(
                                biometricEnabled = it
                            )
                        message = null
                    }
                )

                Spacer(Modifier.height(14.dp))

                SecuritySectionTitle(
                    "خيارات الحسابات"
                )

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default.Lock,
                            null
                        )
                    },
                    title = "قفل قسم الحسابات",
                    subtitle =
                        "اطلب التحقق قبل عرض الحسابات",
                    checked = settings.protectAccounts,
                    enabled = protectionAvailable,
                    onCheckedChange = {
                        settings =
                            settings.copy(
                                protectAccounts = it
                            )
                    }
                )

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default.VisibilityOff,
                            null
                        )
                    },
                    title =
                        "إخفاء المبالغ افتراضياً",
                    subtitle =
                        "يمكن إظهارها من أيقونة العين في الحسابات",
                    checked = settings.hideAmounts,
                    enabled = true,
                    onCheckedChange = {
                        settings =
                            settings.copy(
                                hideAmounts = it
                            )
                    }
                )

                Spacer(Modifier.height(14.dp))

                SecuritySectionTitle(
                    "القفل التلقائي"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "فوري",
                        1 to "1 د",
                        5 to "5 د",
                        15 to "15 د"
                    ).forEach { (minutes, label) ->
                        FilterChip(
                            selected =
                                settings.autoLockMinutes ==
                                    minutes,
                            onClick = {
                                settings =
                                    settings.copy(
                                        autoLockMinutes =
                                            minutes
                                    )
                            },
                            label = {
                                Text(label)
                            }
                        )
                    }
                }

                message?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    var finalSettings = settings

                    val securityActive =
                        finalSettings.mode !=
                            V11LockMode.NONE ||
                            finalSettings.biometricEnabled

                    if (!securityActive) {
                        finalSettings =
                            finalSettings.copy(
                                protectAccounts = false
                            )
                    } else if (
                        !finalSettings.protectAccounts
                    ) {
                        finalSettings =
                            finalSettings.copy(
                                protectAccounts = true
                            )
                    }

                    if (
                        finalSettings.mode ==
                            V11LockMode.PIN &&
                        !store.hasPin()
                    ) {
                        message =
                            "حدد PIN أولاً"
                        credentialDialog =
                            SecurityCredentialDialog.PIN
                        return@Button
                    }

                    if (
                        finalSettings.mode ==
                            V11LockMode.PATTERN &&
                        !store.hasPattern()
                    ) {
                        message =
                            "حدد النمط أولاً"
                        credentialDialog =
                            SecurityCredentialDialog.PATTERN
                        return@Button
                    }

                    if (
                        finalSettings.mode !=
                            V11LockMode.PIN
                    ) {
                        store.clearPin()
                    }

                    if (
                        finalSettings.mode !=
                            V11LockMode.PATTERN
                    ) {
                        store.clearPattern()
                    }

                    store.save(finalSettings)
                    onChanged(finalSettings)
                    onDismiss()
                }
            ) {
                Text("حفظ")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    when (credentialDialog) {
        SecurityCredentialDialog.PIN -> {
            PinSetupDialogV3(
                store = store,
                changingExisting =
                    store.hasPin(),
                onDismiss = {
                    credentialDialog =
                        SecurityCredentialDialog.NONE
                },
                onSaved = {
                    settings =
                        settings.copy(
                            mode = V11LockMode.PIN
                        )
                    pendingMode = null
                    message = null
                    credentialDialog =
                        SecurityCredentialDialog.NONE
                }
            )
        }

        SecurityCredentialDialog.PATTERN -> {
            PatternSetupDialogV3(
                store = store,
                changingExisting =
                    store.hasPattern(),
                onDismiss = {
                    credentialDialog =
                        SecurityCredentialDialog.NONE
                },
                onSaved = {
                    settings =
                        settings.copy(
                            mode = V11LockMode.PATTERN
                        )
                    pendingMode = null
                    message = null
                    credentialDialog =
                        SecurityCredentialDialog.NONE
                }
            )
        }

        SecurityCredentialDialog.VERIFY_PIN_FOR_MODE_CHANGE -> {
            VerifyPinDialogV3(
                store = store,
                onDismiss = {
                    pendingMode = null
                    credentialDialog =
                        SecurityCredentialDialog.NONE
                },
                onVerified = {
                    val target = pendingMode
                    pendingMode = null

                    when (target) {
                        V11LockMode.PIN -> {
                            settings =
                                settings.copy(
                                    mode = V11LockMode.PIN
                                )
                        }

                        V11LockMode.PATTERN -> {
                            credentialDialog =
                                SecurityCredentialDialog.PATTERN
                            return@VerifyPinDialogV3
                        }

                        V11LockMode.NONE -> {
                            settings =
                                settings.copy(
                                    mode = V11LockMode.NONE
                                )
                        }

                        null -> Unit
                    }

                    if (
                        credentialDialog ==
                            SecurityCredentialDialog.VERIFY_PIN_FOR_MODE_CHANGE
                    ) {
                        credentialDialog =
                            SecurityCredentialDialog.NONE
                    }
                }
            )
        }

        SecurityCredentialDialog.VERIFY_PATTERN_FOR_MODE_CHANGE -> {
            VerifyPatternDialogV3(
                store = store,
                onDismiss = {
                    pendingMode = null
                    credentialDialog =
                        SecurityCredentialDialog.NONE
                },
                onVerified = {
                    val target = pendingMode
                    pendingMode = null

                    when (target) {
                        V11LockMode.PATTERN -> {
                            settings =
                                settings.copy(
                                    mode =
                                        V11LockMode.PATTERN
                                )
                        }

                        V11LockMode.PIN -> {
                            credentialDialog =
                                SecurityCredentialDialog.PIN
                            return@VerifyPatternDialogV3
                        }

                        V11LockMode.NONE -> {
                            settings =
                                settings.copy(
                                    mode =
                                        V11LockMode.NONE
                                )
                        }

                        null -> Unit
                    }

                    if (
                        credentialDialog ==
                            SecurityCredentialDialog.VERIFY_PATTERN_FOR_MODE_CHANGE
                    ) {
                        credentialDialog =
                            SecurityCredentialDialog.NONE
                    }
                }
            )
        }

        SecurityCredentialDialog.NONE -> Unit
    }
}

@Composable
private fun PinSetupDialogV3(
    store: V11SecurityStore,
    changingExisting: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var currentPin by remember {
        mutableStateOf("")
    }

    var newPin by remember {
        mutableStateOf("")
    }

    var confirmPin by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (changingExisting)
                    "تغيير PIN"
                else
                    "تحديد PIN"
            )
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                if (changingExisting) {
                    SecurePinField(
                        value = currentPin,
                        label = "PIN الحالي",
                        onValueChange = {
                            currentPin = it
                            message = null
                        }
                    )
                }

                SecurePinField(
                    value = newPin,
                    label = "PIN الجديد",
                    onValueChange = {
                        newPin = it
                        message = null
                    }
                )

                SecurePinField(
                    value = confirmPin,
                    label = "تأكيد PIN",
                    onValueChange = {
                        confirmPin = it
                        message = null
                    }
                )

                message?.let {
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        changingExisting &&
                            !store.verifyPin(
                                currentPin
                            ) -> {
                            message =
                                "PIN الحالي غير صحيح"
                        }

                        newPin.length !in 4..12 ||
                            !newPin.all(
                                Char::isDigit
                            ) -> {
                            message =
                                "PIN يجب أن يكون من 4 إلى 12 رقماً"
                        }

                        newPin != confirmPin -> {
                            message =
                                "تأكيد PIN غير مطابق"
                        }

                        !store.setPin(newPin) -> {
                            message =
                                "تعذر حفظ PIN"
                        }

                        else -> onSaved()
                    }
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun PatternSetupDialogV3(
    store: V11SecurityStore,
    changingExisting: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var step by remember {
        mutableStateOf(
            if (changingExisting)
                PatternSetupStep.VERIFY_CURRENT
            else
                PatternSetupStep.DRAW_NEW
        )
    }

    var firstPattern by remember {
        mutableStateOf<List<Int>?>(null)
    }

    var inputGeneration by remember {
        mutableIntStateOf(0)
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val title =
        when (step) {
            PatternSetupStep.VERIFY_CURRENT ->
                "تحقق من النمط الحالي"

            PatternSetupStep.DRAW_NEW ->
                if (changingExisting)
                    "ارسم النمط الجديد"
                else
                    "ارسم النمط"

            PatternSetupStep.CONFIRM_NEW ->
                "تأكيد النمط"
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    when (step) {
                        PatternSetupStep.VERIFY_CURRENT ->
                            "ارسم النمط الحالي للمتابعة"

                        PatternSetupStep.DRAW_NEW ->
                            "اربط 4 نقاط على الأقل"

                        PatternSetupStep.CONFIRM_NEW ->
                            "أعد رسم نفس النمط للتأكيد"
                    },
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(10.dp))

                key(inputGeneration, step) {
                    V11PatternInput(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 190.dp,
                                    max = 240.dp
                                ),
                        onPatternComplete = { pattern ->
                            when (step) {
                                PatternSetupStep.VERIFY_CURRENT -> {
                                    if (
                                        store.verifyPattern(
                                            pattern
                                        )
                                    ) {
                                        message = null
                                        step =
                                            PatternSetupStep.DRAW_NEW
                                        inputGeneration++
                                    } else {
                                        message =
                                            "النمط الحالي غير صحيح"
                                        inputGeneration++
                                    }
                                }

                                PatternSetupStep.DRAW_NEW -> {
                                    if (pattern.size < 4) {
                                        message =
                                            "النمط يجب أن يحتوي على 4 نقاط على الأقل"
                                        inputGeneration++
                                    } else {
                                        firstPattern = pattern
                                        message = null
                                        step =
                                            PatternSetupStep.CONFIRM_NEW
                                        inputGeneration++
                                    }
                                }

                                PatternSetupStep.CONFIRM_NEW -> {
                                    if (
                                        firstPattern != pattern
                                    ) {
                                        message =
                                            "النمط غير مطابق، أعد التأكيد"
                                        inputGeneration++
                                    } else if (
                                        !store.setPattern(
                                            pattern
                                        )
                                    ) {
                                        message =
                                            "تعذر حفظ النمط"
                                        inputGeneration++
                                    } else {
                                        onSaved()
                                    }
                                }
                            }
                        }
                    )
                }

                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun VerifyPinDialogV3(
    store: V11SecurityStore,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var pin by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("التحقق من PIN الحالي")
        },
        text = {
            Column {
                SecurePinField(
                    value = pin,
                    label = "PIN الحالي",
                    onValueChange = {
                        pin = it
                        error = false
                    }
                )

                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "PIN الحالي غير صحيح",
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (store.verifyPin(pin)) {
                        onVerified()
                    } else {
                        error = true
                    }
                }
            ) {
                Text("تحقق")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun VerifyPatternDialogV3(
    store: V11SecurityStore,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var generation by remember {
        mutableIntStateOf(0)
    }

    var error by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("التحقق من النمط الحالي")
        },
        text = {
            Column(
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                key(generation) {
                    V11PatternInput(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    min = 190.dp,
                                    max = 240.dp
                                ),
                        onPatternComplete = {
                            if (
                                store.verifyPattern(it)
                            ) {
                                onVerified()
                            } else {
                                error = true
                                generation++
                            }
                        }
                    )
                }

                if (error) {
                    Text(
                        "النمط الحالي غير صحيح",
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun SecurityModeCardV3(
    title: String,
    subtitle: String,
    selected: Boolean,
    actionText: String?,
    onSelect: () -> Unit,
    onAction: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected)
                        MaterialTheme.colorScheme
                            .secondaryContainer
                    else
                        MaterialTheme.colorScheme
                            .surfaceVariant
            )
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            if (actionText != null) {
                TextButton(
                    onClick = onAction
                ) {
                    Icon(
                        Icons.Default.Edit,
                        null,
                        modifier = Modifier.size(17.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(actionText)
                }
            }
        }
    }
}


@Composable
private fun SecurePinField(
    value: String,
    label: String,
    onValueChange:
        (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(
                it.filter(
                    Char::isDigit
                ).take(12)
            )
        },
        label = {
            Text(label)
        },
        singleLine = true,
        modifier =
            Modifier.fillMaxWidth(),
        visualTransformation =
            PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    KeyboardType
                        .NumberPassword
            )
    )
}

@Composable
private fun SecuritySectionTitle(
    text: String
) {
    Text(
        text,
        fontWeight =
            FontWeight.Black,
        style =
            MaterialTheme
                .typography
                .titleSmall,
        modifier =
            Modifier.padding(
                bottom = 6.dp
            )
    )
}

@Composable
private fun SecurityModeCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 3.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected)
                        MaterialTheme
                            .colorScheme
                            .secondaryContainer
                    else
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                enabled = enabled
            )

            Spacer(
                Modifier.width(6.dp)
            )

            Column {
                Text(
                    title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    subtitle,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}

@Composable
private fun SecuritySwitchCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange:
        (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 3.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            icon()

            Spacer(
                Modifier.width(8.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    subtitle,
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }

            Switch(
                checked = checked,
                onCheckedChange =
                    onCheckedChange,
                enabled = enabled
            )
        }
    }
}
