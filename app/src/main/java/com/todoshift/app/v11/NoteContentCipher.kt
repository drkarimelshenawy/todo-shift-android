package com.todoshift.app.v11

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/** Device-only authenticated encryption. Portable exports use the backup password envelope. */
internal class NoteContentCipher {
    companion object {
        private const val ALIAS = "todoshift-note-content-v45"
        internal var keyProvider: () -> SecretKey = {
            synchronized(NoteContentCipher::class.java) {
                val store=KeyStore.getInstance("AndroidKeyStore").apply {load(null)}
                (store.getKey(ALIAS,null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
                    init(KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
                }.generateKey()
            }
        }
    }
    fun seal(value:String, identity:String):String {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,keyProvider())
        cipher.updateAAD(identity.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv+cipher.doFinal(value.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
    }
    fun open(value:String, identity:String):String {
        val bytes=Base64.decode(value,Base64.NO_WRAP)
        require(bytes.size>=28) {"Invalid protected content"}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,keyProvider(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
        cipher.updateAAD(identity.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
    }
    fun encode(raw:String):String {
        val root=JSONObject(raw)
        val protected=mutableSetOf<String>()
        val notes=root.optJSONArray("dayItems")
        if(notes!=null) for(i in 0 until notes.length()) {
            val note=notes.getJSONObject(i)
            if(note.optBoolean("noteLocked")) {
                protected+=note.getString("id")
                sealRecord(note,listOf("title","details","noteTags"),"note")
            }
        }
        val tasks=root.optJSONArray("tasks")
        if(tasks!=null) for(i in 0 until tasks.length()) {
            val task=tasks.getJSONObject(i)
            if(task.optString("linkedNoteId") in protected) sealRecord(task,listOf("title","details"),"task")
        }
        return root.toString()
    }
    private fun sealRecord(record:JSONObject, fields:List<String>,kind:String) {
        val content=JSONObject()
        fields.forEach {content.put(it,record.optString(it));record.put(it,"")}
        record.put("sealedContentV45",seal(content.toString(),"$kind:${record.getString("id")}"))
    }
    fun decode(raw:String):String {
        val root=JSONObject(raw)
        listOf("dayItems" to "note","tasks" to "task").forEach {(collection,kind) ->
            val records=root.optJSONArray(collection)
            if(records!=null) for(i in 0 until records.length()) {
                val record=records.getJSONObject(i)
                if(record.has("sealedContentV45")) {
                    val content=JSONObject(open(record.getString("sealedContentV45"),"$kind:${record.getString("id")}"))
                    content.keys().forEach {record.put(it,content.get(it))}
                    record.remove("sealedContentV45")
                }
            }
        }
        return root.toString()
    }
}
