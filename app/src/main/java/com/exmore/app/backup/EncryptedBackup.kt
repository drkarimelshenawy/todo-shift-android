package com.exmore.app.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned authenticated envelope. Passwords and derived keys never enter the archive. */
internal object EncryptedBackup {
    private val magic = "TDSB44E1".toByteArray(Charsets.US_ASCII)
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val ITERATIONS = 310_000
    fun isEncrypted(bytes: ByteArray): Boolean = bytes.size >= magic.size && bytes.copyOfRange(0, magic.size).contentEquals(magic)
    fun validPassword(password: CharArray): Boolean = password.size in 12..256 && password.toSet().size >= 4
    private fun key(password: CharArray, salt: ByteArray): ByteArray {
        require(password.size in 1..256) { "Invalid backup password" }
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        require(validPassword(password)) { "Use a backup passphrase of at least 12 characters with at least four distinct characters" }
        require(plain.size <= Neo4BackupFormat.MAX_BYTES) { "Backup exceeds limit" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val header = magic + salt + iv
        val derived = key(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, iv))
            cipher.updateAAD(header)
            header + cipher.doFinal(plain)
        } finally { derived.fill(0) }
    }
    fun decrypt(envelope: ByteArray, password: CharArray): ByteArray {
        val headerSize = magic.size + SALT_BYTES + IV_BYTES
        require(isEncrypted(envelope) && envelope.size >= headerSize + 16 && envelope.size <= Neo4BackupFormat.MAX_BYTES + 128) { "Invalid encrypted backup" }
        val salt = envelope.copyOfRange(magic.size, magic.size + SALT_BYTES)
        val iv = envelope.copyOfRange(magic.size + SALT_BYTES, headerSize)
        val derived = key(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derived, "AES"), GCMParameterSpec(128, iv))
            cipher.updateAAD(envelope.copyOfRange(0, headerSize))
            cipher.doFinal(envelope, headerSize, envelope.size - headerSize)
        } catch (failure: java.security.GeneralSecurityException) {
            throw IllegalArgumentException("Incorrect backup password or damaged encrypted backup", failure)
        } finally { derived.fill(0) }
    }
}

/** Carries only ciphertext while the UI obtains the password, never a partial plaintext preview. */
class BackupPasswordRequired internal constructor(val envelope: ByteArray) : IllegalArgumentException("Enter the backup password to inspect this encrypted file")
