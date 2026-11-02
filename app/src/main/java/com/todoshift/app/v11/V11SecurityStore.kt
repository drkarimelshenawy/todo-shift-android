package com.todoshift.app.v11

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class V11LockMode {
    NONE,
    PIN,
    BIOMETRIC
}

data class V11SecuritySettings(
    val mode: V11LockMode = V11LockMode.NONE,
    val protectAccounts: Boolean = false,
    val hideAmounts: Boolean = true,
    val autoLockMinutes: Int = 0
)

class V11SecurityStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "todo_shift_security_v11",
            Context.MODE_PRIVATE
        )

    companion object {
        private const val KEY_MODE = "mode"
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

        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
    }

    fun load(): V11SecuritySettings =
        V11SecuritySettings(
            mode = runCatching {
                V11LockMode.valueOf(
                    prefs.getString(
                        KEY_MODE,
                        V11LockMode.NONE.name
                    ) ?: V11LockMode.NONE.name
                )
            }.getOrDefault(V11LockMode.NONE),

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

    fun save(settings: V11SecuritySettings) {
        prefs.edit()
            .putString(
                KEY_MODE,
                settings.mode.name
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
            .apply()
    }

    fun hasPin(): Boolean =
        prefs.contains(KEY_PIN_SALT) &&
            prefs.contains(KEY_PIN_HASH)

    fun setPin(pin: String): Boolean {
        if (
            pin.length < 4 ||
            !pin.all(Char::isDigit)
        ) {
            return false
        }

        val salt =
            ByteArray(16).also {
                SecureRandom().nextBytes(it)
            }

        val hash =
            hashPin(
                pin = pin,
                salt = salt
            )

        prefs.edit()
            .putString(
                KEY_PIN_SALT,
                Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
                )
            )
            .putString(
                KEY_PIN_HASH,
                Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
                )
            )
            .apply()

        return true
    }

    fun verifyPin(pin: String): Boolean {
        val saltRaw =
            prefs.getString(
                KEY_PIN_SALT,
                null
            ) ?: return false

        val hashRaw =
            prefs.getString(
                KEY_PIN_HASH,
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
                hashPin(
                    pin = pin,
                    salt = salt
                )

            constantTimeEquals(
                expected,
                actual
            )
        }.getOrDefault(false)
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .apply()
    }

    private fun hashPin(
        pin: String,
        salt: ByteArray
    ): ByteArray {
        val spec =
            PBEKeySpec(
                pin.toCharArray(),
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
        if (a.size != b.size) return false

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
