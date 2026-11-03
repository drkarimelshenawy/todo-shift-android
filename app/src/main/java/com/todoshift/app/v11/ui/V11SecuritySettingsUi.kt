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
    onChanged:
        (V11SecuritySettings) -> Unit
) {
    val context =
        LocalContext.current

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

    var currentPatternVerified by remember {
        mutableStateOf(false)
    }

    var newPattern by remember {
        mutableStateOf<List<Int>?>(
            null
        )
    }

    var confirmPattern by remember {
        mutableStateOf<List<Int>?>(
            null
        )
    }

    var message by remember {
        mutableStateOf<String?>(null)
    }

    val hadPin =
        remember(current) {
            store.hasPin()
        }

    val hadPattern =
        remember(current) {
            store.hasPattern()
        }

    val biometricAvailable =
        remember {
            val allowed =
                BiometricManager
                    .Authenticators
                    .BIOMETRIC_STRONG or
                    BiometricManager
                        .Authenticators
                        .DEVICE_CREDENTIAL

            BiometricManager
                .from(context)
                .canAuthenticate(
                    allowed
                ) ==
                BiometricManager
                    .BIOMETRIC_SUCCESS
        }

    val protectionAvailable =
        settings.mode !=
            V11LockMode.NONE ||
            settings.biometricEnabled

    AlertDialog(
        onDismissRequest =
            onDismiss,

        title = {
            Column {
                Text(
                    "الأمان والخصوصية",
                    fontWeight =
                        FontWeight.Black
                )

                Text(
                    "اختر طريقة حماية قسم الحسابات",
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
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
                    "طريقة الحماية الأساسية"
                )

                SecurityModeCard(
                    title =
                        "بدون قفل",
                    subtitle =
                        "بدون PIN أو نمط",
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
                    title = "النمط",
                    subtitle =
                        "نمط حماية 3 × 3",
                    selected =
                        settings.mode ==
                            V11LockMode.PATTERN,
                    enabled = true,
                    onClick = {
                        settings =
                            settings.copy(
                                mode =
                                    V11LockMode.PATTERN
                            )
                        message = null
                    }
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                SecuritySectionTitle(
                    "البصمة"
                )

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default
                                .Fingerprint,
                            null
                        )
                    },
                    title =
                        "استخدام البصمة",
                    subtitle =
                        if (
                            biometricAvailable
                        )
                            "اختيار إضافي مستقل لفتح الحسابات"
                        else
                            "البصمة أو قفل الجهاز غير متاح حالياً",
                    checked =
                        settings
                            .biometricEnabled,
                    enabled =
                        biometricAvailable,
                    onCheckedChange = {
                        settings =
                            settings.copy(
                                biometricEnabled =
                                    it
                            )

                        message = null
                    }
                )

                // =================================================
                // Verify current primary protection when changing it.
                // =================================================

                if (
                    current.mode ==
                        V11LockMode.PIN &&
                    hadPin &&
                    (
                        settings.mode !=
                            V11LockMode.PIN ||
                            newPin.isNotEmpty()
                        )
                ) {
                    Spacer(
                        Modifier.height(12.dp)
                    )

                    SecuritySectionTitle(
                        "التحقق من الحماية الحالية"
                    )

                    SecurePinField(
                        value =
                            currentPin,
                        label =
                            "PIN الحالي",
                        onValueChange = {
                            currentPin = it
                            message = null
                        }
                    )
                }

                if (
                    current.mode ==
                        V11LockMode.PATTERN &&
                    hadPattern &&
                    settings.mode !=
                        V11LockMode.PATTERN
                ) {
                    Spacer(
                        Modifier.height(12.dp)
                    )

                    SecuritySectionTitle(
                        "ارسم النمط الحالي"
                    )

                    V11PatternInput(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = 220.dp
                                ),
                        onPatternComplete = {
                            currentPatternVerified =
                                store.verifyPattern(
                                    it
                                )

                            message =
                                if (
                                    currentPatternVerified
                                )
                                    null
                                else
                                    "النمط الحالي غير صحيح"
                        }
                    )

                    if (
                        currentPatternVerified
                    ) {
                        Text(
                            "تم التحقق من النمط الحالي",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                // =================================================
                // PIN creation/change
                // =================================================

                if (
                    settings.mode ==
                        V11LockMode.PIN
                ) {
                    Spacer(
                        Modifier.height(12.dp)
                    )

                    SecuritySectionTitle(
                        if (
                            current.mode ==
                                V11LockMode.PIN &&
                            hadPin
                        )
                            "تغيير PIN"
                        else
                            "إنشاء PIN"
                    )

                    SecurePinField(
                        value = newPin,
                        label = "PIN الجديد",
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
                        label =
                            "تأكيد PIN",
                        onValueChange = {
                            confirmPin = it
                            message = null
                        }
                    )
                }

                // =================================================
                // Pattern creation/change
                // =================================================

                if (
                    settings.mode ==
                        V11LockMode.PATTERN
                ) {
                    Spacer(
                        Modifier.height(12.dp)
                    )

                    if (
                        current.mode ==
                            V11LockMode.PATTERN &&
                        hadPattern
                    ) {
                        SecuritySectionTitle(
                            "ارسم النمط الحالي"
                        )

                        V11PatternInput(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        max = 210.dp
                                    ),
                            onPatternComplete = {
                                currentPatternVerified =
                                    store
                                        .verifyPattern(
                                            it
                                        )

                                message =
                                    if (
                                        currentPatternVerified
                                    )
                                        null
                                    else
                                        "النمط الحالي غير صحيح"
                            }
                        )

                        if (
                            currentPatternVerified
                        ) {
                            Text(
                                "تم التحقق",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )
                        }

                        Spacer(
                            Modifier.height(10.dp)
                        )
                    }

                    SecuritySectionTitle(
                        if (
                            current.mode ==
                                V11LockMode.PATTERN
                        )
                            "النمط الجديد"
                        else
                            "إنشاء النمط"
                    )

                    Text(
                        "اربط 4 نقاط على الأقل",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    V11PatternInput(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = 210.dp
                                ),
                        onPatternComplete = {
                            newPattern = it
                            message = null
                        }
                    )

                    Text(
                        if (
                            newPattern != null
                        )
                            "تم تسجيل النمط"
                        else
                            "ارسم النمط",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Spacer(
                        Modifier.height(10.dp)
                    )

                    SecuritySectionTitle(
                        "تأكيد النمط"
                    )

                    V11PatternInput(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = 210.dp
                                ),
                        onPatternComplete = {
                            confirmPattern = it
                            message = null
                        }
                    )

                    Text(
                        if (
                            confirmPattern != null
                        )
                            "تم تسجيل التأكيد"
                        else
                            "أعد رسم النمط",
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
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
                    title =
                        "قفل قسم الحسابات",
                    subtitle =
                        "اطلب التحقق قبل عرض الحسابات",
                    checked =
                        settings.protectAccounts,
                    enabled =
                        protectionAvailable,
                    onCheckedChange = {
                        settings =
                            settings.copy(
                                protectAccounts =
                                    it
                            )
                    }
                )

                SecuritySwitchCard(
                    icon = {
                        Icon(
                            Icons.Default
                                .VisibilityOff,
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
                                hideAmounts =
                                    it
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
                        Arrangement.spacedBy(
                            6.dp
                        )
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

                message?.let {
                    Spacer(
                        Modifier.height(10.dp)
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
        },

        confirmButton = {
            Button(
                onClick = {
                    message = null

                    // ---------------------------------------------
                    // Verify old PIN when replacing/removing it.
                    // ---------------------------------------------

                    if (
                        current.mode ==
                            V11LockMode.PIN &&
                        hadPin &&
                        (
                            settings.mode !=
                                V11LockMode.PIN ||
                                newPin.isNotEmpty()
                            ) &&
                        !store.verifyPin(
                            currentPin
                        )
                    ) {
                        message =
                            "PIN الحالي غير صحيح"
                        return@Button
                    }

                    // ---------------------------------------------
                    // Verify old Pattern when replacing/removing.
                    // ---------------------------------------------

                    if (
                        current.mode ==
                            V11LockMode.PATTERN &&
                        hadPattern &&
                        (
                            settings.mode !=
                                V11LockMode.PATTERN ||
                                newPattern != null
                            ) &&
                        !currentPatternVerified
                    ) {
                        message =
                            "يجب التحقق من النمط الحالي أولاً"
                        return@Button
                    }

                    // ---------------------------------------------
                    // PIN
                    // ---------------------------------------------

                    if (
                        settings.mode ==
                            V11LockMode.PIN
                    ) {
                        val needsNewPin =
                            current.mode !=
                                V11LockMode.PIN ||
                                !hadPin ||
                                newPin.isNotEmpty()

                        if (needsNewPin) {
                            if (
                                newPin.length
                                    !in 4..12 ||
                                !newPin.all(
                                    Char::isDigit
                                )
                            ) {
                                message =
                                    "PIN يجب أن يكون من 4 إلى 12 رقماً"
                                return@Button
                            }

                            if (
                                newPin !=
                                    confirmPin
                            ) {
                                message =
                                    "تأكيد PIN غير مطابق"
                                return@Button
                            }

                            if (
                                !store.setPin(
                                    newPin
                                )
                            ) {
                                message =
                                    "تعذر حفظ PIN"
                                return@Button
                            }
                        }
                    }

                    // ---------------------------------------------
                    // Pattern
                    // ---------------------------------------------

                    if (
                        settings.mode ==
                            V11LockMode.PATTERN
                    ) {
                        val needsPattern =
                            current.mode !=
                                V11LockMode.PATTERN ||
                                !hadPattern ||
                                newPattern != null

                        if (needsPattern) {
                            val first =
                                newPattern

                            val second =
                                confirmPattern

                            if (
                                first == null ||
                                first.size < 4
                            ) {
                                message =
                                    "ارسم نمطاً من 4 نقاط على الأقل"
                                return@Button
                            }

                            if (
                                second == null ||
                                first != second
                            ) {
                                message =
                                    "تأكيد النمط غير مطابق"
                                return@Button
                            }

                            if (
                                !store.setPattern(
                                    first
                                )
                            ) {
                                message =
                                    "تعذر حفظ النمط"
                                return@Button
                            }
                        }
                    }

                    var finalSettings =
                        settings

                    val securityActive =
                        finalSettings.mode !=
                            V11LockMode.NONE ||
                            finalSettings
                                .biometricEnabled

                    if (!securityActive) {
                        finalSettings =
                            finalSettings.copy(
                                protectAccounts =
                                    false
                            )
                    } else if (
                        !finalSettings
                            .protectAccounts
                    ) {
                        finalSettings =
                            finalSettings.copy(
                                protectAccounts =
                                    true
                            )
                    }

                    // Remove credentials that are no longer
                    // the selected primary method.
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
