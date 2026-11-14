package com.todoshift.app.v11

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Interrupted note edits are encrypted locally and excluded from Android/section backups. */
internal class NeoNoteDraftStore(context: Context) {
    private val directory = File(context.noBackupFilesDir, "note-drafts-v44")
    private fun file(id: String): AtomicFile {
        val name=java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it)}
        check(directory.exists() || directory.mkdirs()) { "Unable to open note drafts" }
        return AtomicFile(File(directory, "$name.draft"))
    }
    private fun key(): SecretKey = synchronized(NeoNoteDraftStore::class.java) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("todoshift-note-drafts-v44", null) as? SecretKey) ?: run {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("todoshift-note-drafts-v44", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }
    }
    fun read(id: String): JSONObject? {
        val data = try { file(id).openRead().use { it.readBytes() } } catch (_: FileNotFoundException) { return null }
        require(data.size >= 29) { "Invalid note draft" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        return JSONObject(String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8))
    }
    fun save(id: String, value: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        val data = cipher.iv + cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        val target = file(id); val stream = target.startWrite()
        try { stream.write(data); target.finishWrite(stream) } catch (failure: Exception) { target.failWrite(stream); throw failure }
    }
    fun remove(id: String) { file(id).delete() }
}
