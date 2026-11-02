package com.todoshift.app.v11.ui

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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

    val hadPin = remember {
        store.hasPin()
    }

    val biometricAvailable = remember {
        val allowed =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        BiometricManager.from(context)
            .canAuthenticate(allowed) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    val securityWasEnabled =
        current.mode != V11LockMode.NONE

    val pinNeedsVerification =
        hadPin &&
            (
                settings.mode == V11LockMode.NONE ||
                    (
                        settings.mode == V11LockMode.PIN &&
                            (
                                newPin.isNotEmpty() ||
                                    current.mode != V11LockMode.PIN
                                )
                        )
                )

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
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                SecuritySectionTitle(
                    "طريقة الحماية"
                )

                SecurityModeCard(
                    title = "بدون قفل",
                    subtitle =
                        "فتح الحسابات مباشرة",
                    selected =
                        settings.mode ==
                            V11LockMode.NONE,
                    enabled = true,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode.NONE
                            )
                        message = null
                    }
                )

                SecurityModeCard(
                    title = "PIN",
                    subtitle =
                        "رقم سري من 4 إلى 12 رقماً",
                    selected =
                        settings.mode ==
                            V11LockMode.PIN,
                    enabled = true,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode.PIN
                            )
                        message = null
                    }
                )

                SecurityModeCard(
                    title =
                        "البصمة / قفل الجهاز",
                    subtitle =
                        if (biometricAvailable)
                            "استخدام حماية الجهاز"
                        else
                            "غير متاح على الجهاز حالياً",
                    selected =
                        settings.mode ==
                            V11LockMode.BIOMETRIC,
                    enabled =
                        biometricAvailable,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode.BIOMETRIC
                            )
                        message = null
                    }
                )

                if (
                    settings.mode ==
                    V11LockMode.PIN
                ) {
                    Spacer(
                        Modifier.height(10.dp)
                    )

                    SecuritySectionTitle(
                        if (hadPin)
                            "تغيير PIN"
                        else
                            "إنشاء PIN"
                    )

                    if (hadPin) {
                        SecurePinField(
                            value = currentPin,
                            label = "PIN الحالي",
                            onValueChange = {
                                currentPin = it
                                message = null
                            }
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )
                    }

                    SecurePinField(
                        value = newPin,
                        label =
                            if (hadPin)
                                "PIN الجديد"
                            else
                                "PIN",
                        onValueChange = {
                            newPin = it
                            message = null
                        }
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    SecurePinField(
                        value = confirmPin,
                        label = "تأكيد PIN",
                        onValueChange = {
                            confirmPin = it
                            message = null
                        }
                    )
                } else if (
                    settings.mode ==
                        V11LockMode.NONE &&
                    securityWasEnabled &&
                    hadPin
                ) {
                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        "لتعطيل الحماية أدخل PIN الحالي.",
                        style =
                            MaterialTheme.typography
                                .bodySmall
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    SecurePinField(
                        value = currentPin,
                        label = "PIN الحالي",
                        onValueChange = {
                            currentPin = it
                            message = null
                        }
                    )
                }

                Spacer(
                    Modifier.height(14.dp)
                )

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
                    checked =
                        settings.protectAccounts,
                    enabled =
                        settings.mode !=
                            V11LockMode.NONE,
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
                        "يمكن إظهارها من شاشة الحسابات",
                    checked =
                        settings.hideAmounts,
                    enabled = true,
                    onCheckedChange = {
                        settings =
                            settings.copy(
                                hideAmounts = it
                            )
                    }
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                SecuritySectionTitle(
                    "القفل التلقائي"
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "فوري",
                        1 to "1 د",
                        5 to "5 د",
                        15 to "15 د"
                    ).forEach {
                            (minutes, label) ->

                        FilterChip(
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
                            },
                            label = {
                                Text(label)
                            }
                        )
                    }
                }

                if (biometricAvailable) {
                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            null,
                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.width(6.dp)
                        )

                        Text(
                            "البصمة وقفل الجهاز متاحان.",
                            style =
                                MaterialTheme.typography
                                    .bodySmall
                        )
                    }
                }

                message?.let {
                    Spacer(
                        Modifier.height(10.dp)
                    )

                    Text(
                        it,
                        color =
                            MaterialTheme
                                .colorScheme.error,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    message = null

                    if (
                        pinNeedsVerification &&
                        !store.verifyPin(
                            currentPin
                        )
                    ) {
                        message =
                            "PIN الحالي غير صحيح"
                        return@Button
                    }

                    if (
                        settings.mode ==
                        V11LockMode.PIN
                    ) {
                        if (
                            newPin.length < 4 ||
                            newPin.length > 12
                        ) {
                            message =
                                "PIN يجب أن يكون من 4 إلى 12 رقماً"
                            return@Button
                        }

                        if (
                            newPin != confirmPin
                        ) {
                            message =
                                "تأكيد PIN غير مطابق"
                            return@Button
                        }

                        if (
                            !store.setPin(newPin)
                        ) {
                            message =
                                "تعذر حفظ PIN"
                            return@Button
                        }
                    }

                    var finalSettings =
                        settings

                    if (
                        finalSettings.mode ==
                        V11LockMode.NONE
                    ) {
                        finalSettings =
                            finalSettings.copy(
                                protectAccounts =
                                    false
                            )

                        store.clearPin()
                    }

                    if (
                        finalSettings.mode !=
                        V11LockMode.NONE &&
                        !finalSettings
                            .protectAccounts
                    ) {
                        finalSettings =
                            finalSettings.copy(
                                protectAccounts =
                                    true
                            )
                    }

                    store.save(
                        finalSettings
                    )

                    onChanged(
                        finalSettings
                    )

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
private fun SecurePinField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
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
        fontWeight = FontWeight.Black,
        style =
            MaterialTheme.typography
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
            .padding(vertical = 3.dp),
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
                        MaterialTheme.typography
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
            .padding(vertical = 3.dp)
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
                        MaterialTheme.typography
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
