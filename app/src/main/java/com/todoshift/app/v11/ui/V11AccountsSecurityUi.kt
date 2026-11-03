package com.todoshift.app.v11.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
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
                "تعذر تشغيل البصمة على هذه الشاشة"
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
                "البصمة أو قفل الجهاز غير متاح"
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
                    "فتح الحسابات"
                )
                .setSubtitle(
                    "استخدم البصمة أو قفل الجهاز"
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
                    Modifier.padding(
                        20.dp
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
                    "الحسابات محمية",
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
                                    "الرقم السري"
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
                                if (
                                    securityStore
                                        .verifyPin(
                                            pin
                                        )
                                ) {
                                    pin = ""
                                    error = null
                                    onUnlocked()
                                } else {
                                    error =
                                        "الرقم السري غير صحيح"
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                        ) {
                            Text("فتح")
                        }
                    }

                    V11LockMode.PATTERN -> {
                        Text(
                            "ارسم نمط الفتح",
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
                                if (
                                    securityStore
                                        .verifyPattern(
                                            it
                                        )
                                ) {
                                    error = null
                                    onUnlocked()
                                } else {
                                    error =
                                        "النمط غير صحيح"
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
                                "استخدم البصمة لفتح الحسابات",
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
                            "أو",
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
                            "فتح بالبصمة"
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
