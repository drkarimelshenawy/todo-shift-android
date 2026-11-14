package com.todoshift.cashbook.ui

import android.content.Context
import android.util.AtomicFile
import com.todoshift.cashbook.core.Ledger
import java.io.File
import java.io.IOException

/** One financial writer; background readers share the AtomicFile lock with that writer. */
class AndroidLedgerStore(context: Context) : Ledger.Store {
    private val file = AtomicFile(File(context.noBackupFilesDir, "cashbook-v1.bin"))
    companion object { private val fileLock=Any() }
    override fun read(): ByteArray? = synchronized(fileLock) {
        // AtomicFile can recover a .bak even if its base file is absent.
        val base = file.baseFile
        if (!base.exists() && !File(base.path + ".bak").exists()) return@synchronized null
        file.openRead().use {
            val bytes = it.readBytesLimited(16 * 1024 * 1024 + 32)
            if (bytes.isEmpty()) throw IOException("Empty ledger")
            bytes
        }
    }
    override fun write(snapshot: ByteArray) = synchronized(fileLock) {
        val stream = file.startWrite()
        try {
            stream.write(snapshot)
            stream.fd.sync()
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
}
internal fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val result = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer)
        if (n == -1) break
        if (result.size() + n > limit) throw IOException("File too large")
        result.write(buffer, 0, n)
    }
    return result.toByteArray()
}
