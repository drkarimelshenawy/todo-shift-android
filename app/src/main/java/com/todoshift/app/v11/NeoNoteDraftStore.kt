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

/** Interrupted edits remain encrypted. Writers and versioned cleanup share one process lock. */
internal class NeoNoteDraftStore(context: Context) {
    companion object {
        private val fileLock=Any()
        internal var keyProvider:()->SecretKey = {deviceKey()}
        private fun deviceKey():SecretKey = synchronized(NeoNoteDraftStore::class.java) {
            val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
            (store.getKey("todoshift-note-drafts-v44",null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("todoshift-note-drafts-v44",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }
    }
    private val directory=File(context.noBackupFilesDir,"note-drafts-v44")
    private fun file(id:String):AtomicFile {
        val name=java.security.MessageDigest.getInstance("SHA-256").digest(id.toByteArray(Charsets.UTF_8)).joinToString("") {"%02x".format(it)}
        check(directory.exists()||directory.mkdirs()) {"Unable to open note drafts"}
        return AtomicFile(File(directory,"$name.draft"))
    }
    fun read(id:String):JSONObject? = synchronized(fileLock) {
        val data=try {file(id).openRead().use {it.readBytes()}} catch(_:FileNotFoundException) {return@synchronized null}
        require(data.size>=29) {"Invalid note draft"}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,keyProvider(),GCMParameterSpec(128,data.copyOfRange(0,12)))
        cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        JSONObject(String(cipher.doFinal(data.copyOfRange(12,data.size)),Charsets.UTF_8))
    }
    fun save(id:String,value:JSONObject) = synchronized(fileLock) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,keyProvider());cipher.updateAAD(id.toByteArray(Charsets.UTF_8))
        val data=cipher.iv+cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        val target=file(id);val stream=target.startWrite()
        try {stream.write(data);stream.fd.sync();target.finishWrite(stream)} catch(failure:Exception) {target.failWrite(stream);throw failure}
    }
    fun remove(id:String) = synchronized(fileLock) {file(id).delete()}
    /** An older save finishing after a newer editor must never delete the newer draft. */
    fun removeIfVersion(id:String,version:String) = synchronized(fileLock) {
        if(read(id)?.optString("editVersion")==version) file(id).delete()
    }
}
