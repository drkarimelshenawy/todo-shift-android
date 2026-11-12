package com.todoshift.app.v11

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class V11LockMode {
    NONE,
    PIN,
    PATTERN
}

data class V11SecuritySettings(
    val mode: V11LockMode = V11LockMode.NONE,
    val biometricEnabled: Boolean = false,
    val protectAccounts: Boolean = false,
    val hideAmounts: Boolean = true,
    val autoLockMinutes: Int = 0
)

class V11SecurityStore internal constructor(
    private val prefs: android.content.SharedPreferences,
    private val now: () -> Long,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences("todo_shift_security_v11", Context.MODE_PRIVATE),
        { System.currentTimeMillis() },
    )

    companion object {
        private val verificationGate = Any()
        private const val FAILED_ATTEMPTS = "verification_failures"
        private const val BLOCKED_UNTIL = "verification_blocked_until"
        private const val MAX_DELAY = 300_000L

        private const val KEY_MODE =
            "mode"

        private const val KEY_BIOMETRIC_ENABLED =
            "biometric_enabled"

        private const val KEY_PROTECT_ACCOUNTS =
            "protect_accounts"

        private const val KEY_HIDE_AMOUNTS =
            "hide_amounts"

        private const val KEY_AUTO_LOCK =
            "auto_lock_minutes"

        private const val KEY_PIN_SALT =
            "pin_salt"

        private const val KEY_PIN_HASH =
            "pin_hash"

        private const val KEY_PATTERN_SALT =
            "pattern_salt"

        private const val KEY_PATTERN_HASH =
            "pattern_hash"

        private const val ITERATIONS =
            120_000

        private const val KEY_LENGTH =
            256
    }

    fun load(): V11SecuritySettings {
        val rawMode =
            prefs.getString(
                KEY_MODE,
                V11LockMode.NONE.name
            ) ?: V11LockMode.NONE.name

        /*
         * Migration from Security V1:
         *
         * Old BIOMETRIC mode becomes:
         * primary mode = NONE
         * biometricEnabled = true
         */
        val legacyBiometric =
            rawMode == "BIOMETRIC"

        val mode =
            when (rawMode) {
                V11LockMode.PIN.name ->
                    V11LockMode.PIN

                V11LockMode.PATTERN.name ->
                    V11LockMode.PATTERN

                V11LockMode.NONE.name, "BIOMETRIC" -> V11LockMode.NONE
                else -> error("Unsupported security mode; refusing to disable protection")
            }

        val biometricEnabled =
            if (
                prefs.contains(
                    KEY_BIOMETRIC_ENABLED
                )
            ) {
                prefs.getBoolean(
                    KEY_BIOMETRIC_ENABLED,
                    false
                )
            } else {
                legacyBiometric
            }

        return V11SecuritySettings(
            mode = mode,

            biometricEnabled =
                biometricEnabled,

            protectAccounts =
                prefs.getBoolean(
                    KEY_PROTECT_ACCOUNTS,
                    false
                ),

            hideAmounts =
                prefs.getBoolean(
                    KEY_HIDE_AMOUNTS,
                    true
                ),

            autoLockMinutes =
                prefs.getInt(
                    KEY_AUTO_LOCK,
                    0
                )
        )
    }

    fun save(
        settings: V11SecuritySettings
    ) {
        val editor = prefs.edit()
            .putString(
                KEY_MODE,
                settings.mode.name
            )
            .putBoolean(
                KEY_BIOMETRIC_ENABLED,
                settings.biometricEnabled
            )
            .putBoolean(
                KEY_PROTECT_ACCOUNTS,
                settings.protectAccounts
            )
            .putBoolean(
                KEY_HIDE_AMOUNTS,
                settings.hideAmounts
            )
            .putInt(
                KEY_AUTO_LOCK,
                settings.autoLockMinutes
            )
            .apply {
                if (settings.mode != V11LockMode.PIN) { remove(KEY_PIN_SALT); remove(KEY_PIN_HASH) }
                if (settings.mode != V11LockMode.PATTERN) { remove(KEY_PATTERN_SALT); remove(KEY_PATTERN_HASH) }
            }
        check(editor.commit()) { "Unable to save security settings" }
    }

    // ========================================================
    // PIN
    // ========================================================

    fun hasPin(): Boolean =
        prefs.contains(KEY_PIN_SALT) &&
            prefs.contains(KEY_PIN_HASH)

    fun setPin(
        pin: String
    ): Boolean {
        if (
            pin.length !in 4..12 ||
            !pin.all(Char::isDigit)
        ) {
            return false
        }

        return setSecret(
            value = pin,
            saltKey = KEY_PIN_SALT,
            hashKey = KEY_PIN_HASH
        )
    }

    fun verifyPin(
        pin: String
    ): Boolean =
        verifySecret(
            value = pin,
            saltKey = KEY_PIN_SALT,
            hashKey = KEY_PIN_HASH
        )

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
    }

    // ========================================================
    // PATTERN
    // ========================================================

    fun hasPattern(): Boolean =
        prefs.contains(KEY_PATTERN_SALT) &&
            prefs.contains(KEY_PATTERN_HASH)

    fun setPattern(
        pattern: List<Int>
    ): Boolean {
        if (!validPattern(pattern)) {
            return false
        }

        return setSecret(
            value = serializePattern(pattern),
            saltKey = KEY_PATTERN_SALT,
            hashKey = KEY_PATTERN_HASH
        )
    }

    fun verifyPattern(
        pattern: List<Int>
    ): Boolean {
        return verifySecret(
            value = serializePattern(pattern),
            saltKey = KEY_PATTERN_SALT,
            hashKey = KEY_PATTERN_HASH
        )
    }

    fun clearPattern() {
        prefs.edit()
            .remove(KEY_PATTERN_SALT)
            .remove(KEY_PATTERN_HASH)
            .apply()
    }

    private fun validPattern(
        pattern: List<Int>
    ): Boolean =
        pattern.size >= 4 &&
            pattern.distinct().size ==
                pattern.size &&
            pattern.all {
                it in 0..8
            }

    private fun serializePattern(
        pattern: List<Int>
    ): String =
        pattern.joinToString("-")

    // ========================================================
    // SECURE SECRET STORAGE
    // ========================================================

    private fun setSecret(
        value: String,
        saltKey: String,
        hashKey: String
    ): Boolean = synchronized(verificationGate) {
        val salt =
            ByteArray(16).also {
                SecureRandom()
                    .nextBytes(it)
            }

        val hash =
            hashSecret(
                value = value,
                salt = salt
            )

        prefs.edit()
            .putString(
                saltKey,
                Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
                )
            )
            .putString(
                hashKey,
                Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
                )
            )
            .commit()

    }

    /** Shared across PIN/pattern and store instances; survives screen/process recreation. */
    fun verificationDelayMillis(): Long = synchronized(verificationGate) {
        val current = now()
        val until = prefs.getLong(BLOCKED_UNTIL, 0L)
        if (until <= current) return@synchronized 0L
        // A backwards wall-clock jump must not produce an unbounded lockout.
        val remaining = until - current
        if (remaining > MAX_DELAY) {
            prefs.edit().putLong(BLOCKED_UNTIL, current + MAX_DELAY).commit()
            MAX_DELAY
        } else remaining
    }

    private fun verifySecret(value: String, saltKey: String, hashKey: String): Boolean =
        synchronized(verificationGate) {
            if (verificationDelayMillis() > 0L) return@synchronized false
            val matched = matchesSecret(value, saltKey, hashKey)
            val editor = prefs.edit()
            if (matched) {
                editor.remove(FAILED_ATTEMPTS).remove(BLOCKED_UNTIL)
            } else {
                val failures = (prefs.getInt(FAILED_ATTEMPTS, 0).coerceIn(0, 20) + 1)
                editor.putInt(FAILED_ATTEMPTS, failures)
                if (failures >= 5) {
                    val delay = (30_000L shl (failures - 5).coerceAtMost(4)).coerceAtMost(MAX_DELAY)
                    editor.putLong(BLOCKED_UNTIL, now() + delay)
                }
            }
            // A failed durable acknowledgement never grants access.
            editor.commit() && matched
        }

    private fun matchesSecret(
        value: String,
        saltKey: String,
        hashKey: String
    ): Boolean {
        val saltRaw =
            prefs.getString(
                saltKey,
                null
            ) ?: return false

        val hashRaw =
            prefs.getString(
                hashKey,
                null
            ) ?: return false

        return runCatching {
            val salt =
                Base64.decode(
                    saltRaw,
                    Base64.NO_WRAP
                )

            val expected =
                Base64.decode(
                    hashRaw,
                    Base64.NO_WRAP
                )

            val actual =
                hashSecret(
                    value = value,
                    salt = salt
                )

            constantTimeEquals(
                expected,
                actual
            )
        }.getOrDefault(false)
    }

    private fun hashSecret(
        value: String,
        salt: ByteArray
    ): ByteArray {
        val spec =
            PBEKeySpec(
                value.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
            )

        return try {
            SecretKeyFactory
                .getInstance(
                    "PBKDF2WithHmacSHA256"
                )
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(
        a: ByteArray,
        b: ByteArray
    ): Boolean {
        if (a.size != b.size) {
            return false
        }

        var result = 0

        for (i in a.indices) {
            result =
                result or
                    (
                        a[i].toInt() xor
                            b[i].toInt()
                    )
        }

        return result == 0
    }
}
