package com.todoshift.app.backup

import android.content.Context
import android.util.AtomicFile
import com.todoshift.app.v11.V11Database
import java.io.File
import java.util.UUID

/** A failed snapshot blocks the major change; no old safety copy is silently deleted. */
fun createNeoSafetyCopy(context: Context, expected: V11Database): String {
    val dir = File(context.filesDir, "safety-copies")
    check(dir.isDirectory || dir.mkdirs())
    val file = AtomicFile(File(dir, "before-${UUID.randomUUID()}.zip"))
    val stream = file.startWrite()
    try {
        // export owns/closes its stream; stage to memory before atomic write finalization.
        val buffer = java.io.ByteArrayOutputStream()
        Neo4LocalBackup(context).export(expected, buffer)
        stream.write(buffer.toByteArray())
        file.finishWrite(stream)
    } catch (e: Exception) {
        file.failWrite(stream)
        throw e
    }
    return file.baseFile.name
}
