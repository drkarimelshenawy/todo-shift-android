package com.todoshift.app.v11.ui

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

@Composable
fun V11SecuritySettingsDialog(
    store: V11SecurityStore,
    current: V11SecuritySettings,
    onDismiss: () -> Unit,
    onChanged:
        (V11SecuritySettings) -> Unit
) {
    val context = LocalContext.current

    var settings by remember(current) {
        mutableStateOf(current)
    }

    var pin by remember {
        mutableStateOf("")
    }

    var confirmPin by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val biometricAvailable =
        remember {
            val allowed =
                BiometricManager.Authenticators
                    .BIOMETRIC_STRONG or
                    BiometricManager.Authenticators
                        .DEVICE_CREDENTIAL

            BiometricManager
                .from(context)
                .canAuthenticate(allowed) ==
                BiometricManager
                    .BIOMETRIC_SUCCESS
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("حماية الحسابات")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Text(
                    "طريقة الحماية",
                    fontWeight =
                        FontWeight.Black
                )

                SecurityModeRow(
                    label = "بدون قفل",
                    selected =
                        settings.mode ==
                            V11LockMode.NONE,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode.NONE
                            )
                        message = null
                    }
                )

                SecurityModeRow(
                    label = "PIN",
                    selected =
                        settings.mode ==
                            V11LockMode.PIN,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode.PIN
                            )
                        message = null
                    }
                )

                SecurityModeRow(
                    label =
                        if (biometricAvailable)
                            "بصمة / قفل الجهاز"
                        else
                            "بصمة / قفل الجهاز — غير متاح",
                    selected =
                        settings.mode ==
                            V11LockMode.BIOMETRIC,
                    enabled =
                        biometricAvailable,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode
                                        .BIOMETRIC
                            )
                        message = null
                    }
                )

                if (
                    settings.mode ==
                    V11LockMode.PIN
                ) {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin =
                                it.filter(
                                    Char::isDigit
                                ).take(12)
                        },
                        label = {
                            Text(
                                "PIN جديد"
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
                            )
                    )

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            confirmPin =
                                it.filter(
                                    Char::isDigit
                                ).take(12)
                        },
                        label = {
                            Text(
                                "تأكيد PIN"
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
                            )
                    )
                }

                Spacer(
                    Modifier.height(12.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween
                ) {
                    Switch(
                        checked =
                            settings
                                .protectAccounts,
                        onCheckedChange = {
                            settings =
                                settings.copy(
                                    protectAccounts =
                                        it
                                )
                        }
                    )

                    Text(
                        "قفل قسم الحسابات"
                    )
                }

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween
                ) {
                    Switch(
                        checked =
                            settings.hideAmounts,
                        onCheckedChange = {
                            settings =
                                settings.copy(
                                    hideAmounts =
                                        it
                                )
                        }
                    )

                    Text(
                        "إخفاء المبالغ افتراضياً"
                    )
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "القفل التلقائي",
                    fontWeight =
                        FontWeight.Bold
                )

                listOf(
                    0 to "فوري",
                    1 to "بعد دقيقة",
                    5 to "بعد 5 دقائق",
                    15 to "بعد 15 دقيقة"
                ).forEach {
                    (minutes, label) ->

                    Row {
                        RadioButton(
                            selected =
                                settings
                                    .autoLockMinutes ==
                                    minutes,
                            onClick = {
                                settings =
                                    settings.copy(
                                        autoLockMinutes =
                                            minutes
                                    )
                            }
                        )

                        Text(
                            label,
                            modifier =
                                Modifier.padding(
                                    top = 12.dp
                                )
                        )
                    }
                }

                message?.let {
                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        it,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (
                        settings.mode ==
                        V11LockMode.PIN
                    ) {
                        if (pin.length < 4) {
                            message =
                                "PIN يجب ألا يقل عن 4 أرقام"
                            return@Button
                        }

                        if (pin != confirmPin) {
                            message =
                                "تأكيد PIN غير مطابق"
                            return@Button
                        }

                        if (!store.setPin(pin)) {
                            message =
                                "تعذر حفظ PIN"
                            return@Button
                        }
                    }

                    if (
                        settings.mode ==
                        V11LockMode.NONE
                    ) {
                        store.clearPin()
                    }

                    store.save(settings)
                    onChanged(settings)
                    onDismiss()
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun SecurityModeRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled
        )

        Text(
            label,
            modifier =
                Modifier.padding(
                    top = 12.dp
                ),
            color =
                if (enabled)
                    LocalContentColor.current
                else
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
        )
    }
}
