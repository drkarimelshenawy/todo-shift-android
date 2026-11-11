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
    val saveRunner = rememberNeoSecurityVerifier()
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
                    neoLiteral("الأمان والخصوصية", "Privacy and security"),
                    fontWeight = FontWeight.Black
                )

                Text(
                    neoLiteral("اختر طريقة حماية قسم الحسابات", "Choose how to protect your finances"),
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
                    neoLiteral("طريقة الحماية الأساسية", "Primary authentication method")
                )

                SecurityModeCardV3(
                    title = neoLiteral("بدون قفل", "No lock"),
                    subtitle = neoLiteral("بدون PIN أو نمط", "Without PIN or pattern"),
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
                            neoLiteral("PIN مُعيّن حالياً", "PIN is configured")
                        else
                            neoLiteral("رقم سري من 4 إلى 12 رقماً", "PIN of 4 to 12 digits"),
                    selected =
                        settings.mode == V11LockMode.PIN,
                    actionText =
                        if (
                            settings.mode == V11LockMode.PIN &&
                            store.hasPin()
                        )
                            neoLiteral("تغيير PIN", "Change PIN")
                        else
                            neoLiteral("تحديد PIN", "Set PIN"),
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
                    title = neoLiteral("النمط", "Pattern"),
                    subtitle =
                        if (
                            settings.mode == V11LockMode.PATTERN &&
                            store.hasPattern()
                        )
                            neoLiteral("نمط حماية مُعيّن حالياً", "Security pattern is configured")
                        else
                            neoLiteral("نمط حماية 3 × 3", "3 × 3 security pattern"),
                    selected =
                        settings.mode == V11LockMode.PATTERN,
                    actionText =
                        if (
                            settings.mode == V11LockMode.PATTERN &&
                            store.hasPattern()
                        )
                            neoLiteral("تغيير النمط", "Change pattern")
                        else
                            neoLiteral("تحديد النمط", "Set pattern"),
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

                SecuritySectionTitle(neoLiteral("البصمة", "Biometrics"))

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default.Fingerprint,
                            null
                        )
                    },
                    title = neoLiteral("استخدام البصمة", "Use biometrics"),
                    subtitle =
                        if (biometricAvailable)
                            neoLiteral("اختيار إضافي مستقل لفتح الحسابات", "An additional method to unlock finances")
                        else
                            neoLiteral("البصمة أو قفل الجهاز غير متاح حالياً", "Biometrics or device lock currently unavailable"),
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
                    neoLiteral("خيارات الحسابات", "Finance options")
                )

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default.Lock,
                            null
                        )
                    },
                    title = neoLiteral("قفل قسم الحسابات", "Protect finances"),
                    subtitle =
                        neoLiteral("اطلب التحقق قبل عرض الحسابات", "Require authentication before showing finances"),
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
                        neoLiteral("إخفاء المبالغ افتراضياً", "Hide amounts by default"),
                    subtitle =
                        neoLiteral("يمكن إظهارها من أيقونة العين في الحسابات", "Use the eye icon in Finance to reveal amounts"),
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
                    neoLiteral("القفل التلقائي", "Auto-lock")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to neoLiteral("فوري", "Immediately"),
                        1 to neoLiteral("1 د", "1 min"),
                        5 to neoLiteral("5 د", "5 min"),
                        15 to neoLiteral("15 د", "15 min")
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
                enabled = !saveRunner.busy,
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
                            neoLiteral("حدد PIN أولاً", "Set a PIN first")
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
                            neoLiteral("حدد النمط أولاً", "Set a pattern first")
                        credentialDialog =
                            SecurityCredentialDialog.PATTERN
                        return@Button
                    }

                    val submitted = finalSettings
                    saveRunner.execute({ store.save(submitted) }) { result ->
                        result.onSuccess {
                            onChanged(submitted)
                            onDismiss()
                        }.onFailure { failure ->
                            message = failure.message ?: neoLiteral("تعذر حفظ إعدادات الحماية", "Cannot save security settings")
                        }
                    }
                }
            ) {
                Text(neoLiteral("حفظ", "Save"))
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(neoLiteral("إلغاء", "Cancel"))
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
internal fun PinSetupDialogV3(
    store: V11SecurityStore,
    changingExisting: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val verifier = rememberNeoSecurityVerifier()
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
                    neoLiteral("تغيير PIN", "Change PIN")
                else
                    neoLiteral("تحديد PIN", "Set PIN")
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
                        label = neoLiteral("PIN الحالي", "Current PIN"),
                        onValueChange = {
                            currentPin = it
                            message = null
                        }
                    )
                }

                SecurePinField(
                    value = newPin,
                    label = neoLiteral("PIN الجديد", "New PIN"),
                    onValueChange = {
                        newPin = it
                        message = null
                    }
                )

                SecurePinField(
                    value = confirmPin,
                    label = neoLiteral("تأكيد PIN", "Confirm PIN"),
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
                enabled = !verifier.busy,
                onClick = {
                    if (newPin.length !in 4..12 || !newPin.all(Char::isDigit)) {
                        message = neoLiteral("PIN يجب أن يكون من 4 إلى 12 رقماً", "PIN must contain 4 to 12 digits")
                    } else if (newPin != confirmPin) {
                        message = neoLiteral("تأكيد PIN غير مطابق", "PINs do not match")
                    } else {
                        val old = currentPin
                        val next = newPin
                        verifier.execute({
                            if (changingExisting && !store.verifyPin(old))
                                neoSecurityFailure(store, neoLiteral("PIN الحالي غير صحيح", "Incorrect current PIN"))
                            else if (!store.setPin(next)) neoLiteral("تعذر حفظ PIN", "Cannot save PIN")
                            else null
                        }) { result ->
                            message = result.getOrElse { it.message ?: neoLiteral("تعذر حفظ PIN", "Cannot save PIN") }
                            if (result.isSuccess && message == null) onSaved()
                        }
                    }
                }
            ) {
                Text(if (verifier.busy) neoLiteral("جارٍ الحفظ…", "Saving…") else neoLiteral("حفظ", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(neoLiteral("إلغاء", "Cancel"))
            }
        }
    )
}

@Composable
internal fun PatternSetupDialogV3(
    store: V11SecurityStore,
    changingExisting: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val verifier = rememberNeoSecurityVerifier()
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
                neoLiteral("تحقق من النمط الحالي", "Verify current pattern")

            PatternSetupStep.DRAW_NEW ->
                if (changingExisting)
                    neoLiteral("ارسم النمط الجديد", "Draw a new pattern")
                else
                    neoLiteral("ارسم النمط", "Draw pattern")

            PatternSetupStep.CONFIRM_NEW ->
                neoLiteral("تأكيد النمط", "Confirm pattern")
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
                            neoLiteral("ارسم النمط الحالي للمتابعة", "Draw your current pattern to continue")

                        PatternSetupStep.DRAW_NEW ->
                            neoLiteral("اربط 4 نقاط على الأقل", "Connect at least 4 dots")

                        PatternSetupStep.CONFIRM_NEW ->
                            neoLiteral("أعد رسم نفس النمط للتأكيد", "Draw the same pattern to confirm")
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
                            if (!verifier.busy) when (step) {
                                PatternSetupStep.VERIFY_CURRENT -> {
                                    val submitted = pattern.toList()
                                    verifier.submit({ store.verifyPattern(submitted) }) { verified ->
                                        if (verified) {
                                            message = null
                                            step = PatternSetupStep.DRAW_NEW
                                        } else {
                                            message = neoSecurityFailure(store, neoLiteral("النمط الحالي غير صحيح", "Incorrect current pattern"))
                                        }
                                        inputGeneration++
                                    }
                                }

                                PatternSetupStep.DRAW_NEW -> {
                                    if (pattern.size < 4) {
                                        message =
                                            neoLiteral("النمط يجب أن يحتوي على 4 نقاط على الأقل", "Pattern must contain at least 4 dots")
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
                                            neoLiteral("النمط غير مطابق، أعد التأكيد", "Patterns do not match. Try again.")
                                        inputGeneration++
                                    } else {
                                        val submitted = pattern.toList()
                                        verifier.submit({ store.setPattern(submitted) }) { saved ->
                                            if (saved) onSaved()
                                            else {
                                                message = neoLiteral("تعذر حفظ النمط", "Cannot save pattern")
                                                inputGeneration++
                                            }
                                        }
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
                Text(neoLiteral("إلغاء", "Cancel"))
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
    val verifier = rememberNeoSecurityVerifier()
    var pin by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(neoLiteral("التحقق من PIN الحالي", "Verify current PIN"))
        },
        text = {
            Column {
                SecurePinField(
                    value = pin,
                    label = neoLiteral("PIN الحالي", "Current PIN"),
                    onValueChange = {
                        pin = it
                        error = false
                    }
                )

                if (error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        neoSecurityFailure(store, neoLiteral("PIN الحالي غير صحيح", "Incorrect current PIN")),
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val submittedSecret = pin
                                verifier.submit({ store.verifyPin(submittedSecret) }) { verified ->
                                if (verified) {
                        onVerified()
                    } else {
                        error = true
                    }
                                }
                }
            ) {
                Text(if (verifier.busy) neoLiteral("جارٍ التحقق…", "Verifying…") else neoLiteral("تحقق", "Verify"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(neoLiteral("إلغاء", "Cancel"))
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
    val verifier = rememberNeoSecurityVerifier()
    var generation by remember {
        mutableIntStateOf(0)
    }

    var error by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(neoLiteral("التحقق من النمط الحالي", "Verify current pattern"))
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
                            val submittedSecret = it.toList()
                                verifier.submit({ store.verifyPattern(submittedSecret) }) { verified ->
                                if (verified) {
                                onVerified()
                            } else {
                                error = true
                                generation++
                            }
                                }
                        }
                    )
                }

                if (error) {
                    Text(
                        neoSecurityFailure(store, neoLiteral("النمط الحالي غير صحيح", "Incorrect current pattern")),
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(neoLiteral("إلغاء", "Cancel"))
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
