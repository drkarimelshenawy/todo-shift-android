package com.todoshift.app.v11

class NoteCipherTestApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        val key=javax.crypto.KeyGenerator.getInstance("AES").apply {init(256)}.generateKey()
        NoteContentCipher.keyProvider={key}
        NeoNoteDraftStore.keyProvider={key}
    }
}
