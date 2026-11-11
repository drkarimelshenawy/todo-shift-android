package com.todoshift.app.v11.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.todoshift.app.v11.V11LockMode
import com.todoshift.app.v11.V11SecuritySettings
import com.todoshift.app.v11.V11SecurityStore

@Composable
fun V11AccountsUnlockScreen(
    settings: V11SecuritySettings,
    securityStore: V11SecurityStore,
    onUnlocked: () -> Unit
) {
    val verifier = rememberNeoSecurityVerifier()
    val context =
        LocalContext.current

    var pin by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf<String?>(
            null
        )
    }

    fun biometric() {
        val activity =
            context as?
                FragmentActivity

        if (activity == null) {
            error =
                neoLiteral("تعذر تشغيل البصمة على هذه الشاشة", "Cannot start biometrics on this screen")
            return
        }

        val allowed =
            BiometricManager
                .Authenticators
                .BIOMETRIC_STRONG or
                BiometricManager
                    .Authenticators
                    .DEVICE_CREDENTIAL

        if (
            BiometricManager
                .from(context)
                .canAuthenticate(
                    allowed
                ) !=
            BiometricManager
                .BIOMETRIC_SUCCESS
        ) {
            error =
                neoLiteral("البصمة أو قفل الجهاز غير متاح", "Biometrics or device lock unavailable")
            return
        }

        val executor =
            androidx.core.content
                .ContextCompat
                .getMainExecutor(
                    context
                )

        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object :
                    BiometricPrompt
                        .AuthenticationCallback() {

                    override fun
                        onAuthenticationSucceeded(
                        result:
                            BiometricPrompt
                                .AuthenticationResult
                    ) {
                        super
                            .onAuthenticationSucceeded(
                                result
                            )

                        error = null
                        onUnlocked()
                    }

                    override fun
                        onAuthenticationError(
                        errorCode: Int,
                        errString:
                            CharSequence
                    ) {
                        super
                            .onAuthenticationError(
                                errorCode,
                                errString
                            )

                        if (
                            errorCode !=
                                BiometricPrompt
                                    .ERROR_USER_CANCELED &&
                            errorCode !=
                                BiometricPrompt
                                    .ERROR_NEGATIVE_BUTTON
                        ) {
                            error =
                                errString
                                    .toString()
                        }
                    }
                }
            )

        val info =
            BiometricPrompt
                .PromptInfo
                .Builder()
                .setTitle(
                    neoLiteral("فتح الحسابات", "Unlock finances")
                )
                .setSubtitle(
                    neoLiteral("استخدم البصمة أو قفل الجهاز", "Use biometrics or device lock")
                )
                .setAllowedAuthenticators(
                    allowed
                )
                .build()

        prompt.authenticate(info)
    }

    Box(
        modifier =
            Modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(20.dp)
                        .verticalScroll(
                            rememberScrollState()
                        ),
                horizontalAlignment =
                    Alignment
                        .CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Lock,
                    null,
                    modifier =
                        Modifier.size(
                            42.dp
                        )
                )

                Spacer(
                    Modifier.height(
                        12.dp
                    )
                )

                Text(
                    neoLiteral("الحسابات محمية", "Finances are protected"),
                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Black
                )

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                when (settings.mode) {
                    V11LockMode.PIN -> {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                pin =
                                    it.filter(
                                        Char::isDigit
                                    ).take(12)

                                error = null
                            },
                            label = {
                                Text(
                                    neoLiteral("الرقم السري", "PIN")
                                )
                            },
                            singleLine = true,
                            visualTransformation =
                                PasswordVisualTransformation(),
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType
                                            .NumberPassword
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        )

                        Spacer(
                            Modifier.height(
                                10.dp
                            )
                        )

                        Button(
                            onClick = {
                                val submittedSecret = pin
                                verifier.submit({ securityStore.verifyPin(submittedSecret) }) { verified ->
                                if (verified) {
                                    pin = ""
                                    error = null
                                    onUnlocked()
                                } else {
                                    error =
                                        neoSecurityFailure(securityStore, neoLiteral("الرقم السري غير صحيح", "Incorrect PIN"))
                                }
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text(if (verifier.busy) neoLiteral("جارٍ التحقق…", "Verifying…") else neoLiteral("فتح", "Open"))
                        }
                    }

                    V11LockMode.PATTERN -> {
                        Text(
                            neoLiteral("ارسم نمط الفتح", "Draw unlock pattern"),
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(
                                6.dp
                            )
                        )

                        V11PatternInput(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        max =
                                            260.dp
                                    ),
                            onPatternComplete = {
                                val submittedSecret = it.toList()
                                verifier.submit({ securityStore.verifyPattern(submittedSecret) }) { verified ->
                                if (verified) {
                                    error = null
                                    onUnlocked()
                                } else {
                                    error =
                                        neoSecurityFailure(securityStore, neoLiteral("النمط غير صحيح", "Incorrect pattern"))
                                }
                                }
                            }
                        )
                    }

                    V11LockMode.NONE -> {
                        if (
                            !settings
                                .biometricEnabled
                        ) {
                            LaunchedEffect(
                                Unit
                            ) {
                                onUnlocked()
                            }
                        } else {
                            Text(
                                neoLiteral("استخدم البصمة لفتح الحسابات", "Use biometrics to unlock finances"),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium
                            )
                        }
                    }
                }

                if (
                    settings
                        .biometricEnabled
                ) {
                    if (
                        settings.mode !=
                            V11LockMode.NONE
                    ) {
                        Spacer(
                            Modifier.height(
                                14.dp
                            )
                        )

                        HorizontalDivider()

                        Spacer(
                            Modifier.height(
                                14.dp
                            )
                        )

                        Text(
                            neoLiteral("أو", "or"),
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall
                        )

                        Spacer(
                            Modifier.height(
                                8.dp
                            )
                        )
                    } else {
                        Spacer(
                            Modifier.height(
                                14.dp
                            )
                        )
                    }

                    Button(
                        onClick = {
                            biometric()
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default
                                .Fingerprint,
                            null
                        )

                        Spacer(
                            Modifier.width(
                                8.dp
                            )
                        )

                        Text(
                            neoLiteral("فتح بالبصمة", "Unlock with biometrics")
                        )
                    }
                }

                error?.let {
                    Spacer(
                        Modifier.height(
                            10.dp
                        )
                    )

                    Text(
                        it,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}



@Composable
private fun V11BiometricVerificationEffect(
    onResult: (Boolean) -> Unit
) {
    val context = LocalContext.current

    val activity =
        remember(context) {
            context as? FragmentActivity
        }

    LaunchedEffect(Unit) {
        if (activity == null) {
            onResult(false)
            return@LaunchedEffect
        }

        val allowed =
            BiometricManager
                .Authenticators
                .BIOMETRIC_STRONG or
                BiometricManager
                    .Authenticators
                    .DEVICE_CREDENTIAL

        if (
            BiometricManager
                .from(context)
                .canAuthenticate(
                    allowed
                ) !=
            BiometricManager
                .BIOMETRIC_SUCCESS
        ) {
            onResult(false)
            return@LaunchedEffect
        }

        val executor =
            androidx.core.content
                .ContextCompat
                .getMainExecutor(
                    context
                )

        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object :
                    BiometricPrompt
                        .AuthenticationCallback() {

                    override fun
                        onAuthenticationSucceeded(
                        result:
                            BiometricPrompt
                                .AuthenticationResult
                    ) {
                        super
                            .onAuthenticationSucceeded(
                                result
                            )

                        onResult(true)
                    }

                    override fun
                        onAuthenticationError(
                        errorCode: Int,
                        errString:
                            CharSequence
                    ) {
                        super
                            .onAuthenticationError(
                                errorCode,
                                errString
                            )

                        onResult(false)
                    }

                    override fun
                        onAuthenticationFailed() {
                        super
                            .onAuthenticationFailed()
                    }
                }
            )

        val info =
            BiometricPrompt
                .PromptInfo
                .Builder()
                .setTitle(
                    neoLiteral("تأكيد التعديل المالي", "Confirm financial change")
                )
                .setSubtitle(
                    neoLiteral("استخدم البصمة أو قفل الجهاز", "Use biometrics or device lock")
                )
                .setAllowedAuthenticators(
                    allowed
                )
                .build()

        prompt.authenticate(info)
    }
}

@Composable
fun V11FinancialVerificationDialog(
    settings: V11SecuritySettings,
    store: V11SecurityStore,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val verifier = rememberNeoSecurityVerifier()
    var pin by remember {
        mutableStateOf("")
    }

    var patternGeneration by remember {
        mutableIntStateOf(0)
    }

    var errorText by remember {
        mutableStateOf<String?>(null)
    }

    var biometricRequested by remember {
        mutableStateOf(false)
    }

    val hasPin =
        settings.mode == V11LockMode.PIN &&
            store.hasPin()

    val hasPattern =
        settings.mode == V11LockMode.PATTERN &&
            store.hasPattern()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(neoLiteral("تأكيد التعديل", "Confirm change"))
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {
                Text(
                    neoLiteral("تحقق من هويتك قبل حفظ التعديل المالي.", "Verify your identity before saving a financial change."),
                    style =
                        MaterialTheme.typography
                            .bodyMedium
                )

                Spacer(Modifier.height(12.dp))

                if (hasPin) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin =
                                it.filter(
                                    Char::isDigit
                                ).take(12)
                            errorText = null
                        },
                        label = {
                            Text("PIN")
                        },
                        singleLine = true,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.NumberPassword
                            ),
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Button(
                        onClick = {
                            val submittedSecret = pin
                                verifier.submit({ store.verifyPin(submittedSecret) }) { verified ->
                                if (verified) {
                                onVerified()
                            } else {
                                errorText =
                                    neoSecurityFailure(store, neoLiteral("PIN غير صحيح", "Incorrect PIN"))
                            }
                                }
                        },
                        enabled =
                            pin.isNotBlank(),
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(if (verifier.busy) neoLiteral("جارٍ التحقق…", "Verifying…") else neoLiteral("تحقق وحفظ", "Verify and save"))
                    }
                }

                if (hasPattern) {
                    Text(
                        neoLiteral("ارسم نمط الحماية", "Draw security pattern"),
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    key(patternGeneration) {
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
                                    errorText =
                                        neoSecurityFailure(store, neoLiteral("النمط غير صحيح", "Incorrect pattern"))
                                    patternGeneration++
                                }
                                }
                            }
                        )
                    }
                }

                if (
                    settings.biometricEnabled
                ) {
                    if (
                        hasPin ||
                        hasPattern
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 8.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier =
                                    Modifier.weight(1f)
                            )
                            Text(
                                neoLiteral("  أو  ", "  or  "),
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                            HorizontalDivider(
                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            biometricRequested =
                                true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            null
                        )
                        Spacer(
                            Modifier.width(6.dp)
                        )
                        Text(neoLiteral("التحقق بالبصمة", "Biometric authentication"))
                    }
                }

                if (
                    !hasPin &&
                    !hasPattern &&
                    !settings.biometricEnabled
                ) {
                    Text(
                        neoLiteral("لا توجد وسيلة تحقق مفعلة.", "No authentication method is enabled."),
                        color =
                            MaterialTheme.colorScheme
                                .error
                    )
                }

                errorText?.let {
                    Spacer(
                        Modifier.height(8.dp)
                    )
                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme
                                .error,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(neoLiteral("إلغاء", "Cancel"))
            }
        }
    )

    if (biometricRequested) {
        V11BiometricVerificationEffect(
            onResult = { success ->
                biometricRequested = false

                if (success) {
                    onVerified()
                } else {
                    errorText =
                        neoLiteral("تعذر التحقق بالبصمة", "Biometric authentication failed")
                }
            }
        )
    }
}
