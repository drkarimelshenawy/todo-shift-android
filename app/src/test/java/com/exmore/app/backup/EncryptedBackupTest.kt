package com.exmore.app.backup

import org.junit.Assert.*
import org.junit.Test

class EncryptedBackupTest {
    private val password get() = "separate backup phrase 44".toCharArray()
    @Test fun authenticatedRoundTripUsesFreshSaltAndIv() {
        val plain = "ملاحظات خاصة • private notes".toByteArray()
        val first = EncryptedBackup.encrypt(plain, password)
        val second = EncryptedBackup.encrypt(plain, password)
        assertTrue(EncryptedBackup.isEncrypted(first))
        assertFalse(first.contentEquals(second))
        assertArrayEquals(plain, EncryptedBackup.decrypt(first, password))
        assertFalse(String(first, Charsets.ISO_8859_1).contains("private notes"))
    }
    @Test fun wrongPasswordAndChangedCiphertextAreRejected() {
        val envelope = EncryptedBackup.encrypt("private financial data".toByteArray(), password)
        fun rejected(bytes: ByteArray, secret: CharArray) {
            try { EncryptedBackup.decrypt(bytes, secret); fail("Authentication must reject invalid data") }
            catch (_: IllegalArgumentException) { }
        }
        rejected(envelope, "another backup phrase 44".toCharArray())
        val damaged = envelope.clone(); damaged[damaged.lastIndex] = (damaged.last().toInt() xor 1).toByte()
        rejected(damaged, password)
        val changedSalt = envelope.clone(); changedSalt[10] = (changedSalt[10].toInt() xor 1).toByte()
        rejected(changedSalt, password)
    }
    @Test fun weakExportPasswordsAreRejected() {
        assertFalse(EncryptedBackup.validPassword("123456".toCharArray()))
        assertFalse(EncryptedBackup.validPassword("aaaaaaaaaaaaaaaa".toCharArray()))
        assertTrue(EncryptedBackup.validPassword(password))
    }
}
