package com.todoshift.app.backup

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import com.todoshift.app.v11.V11Database
import com.todoshift.app.v11.NoteContentCipher
import java.io.File
import java.util.UUID
import org.json.JSONObject

/** Local snapshots use a device key; portable exports keep their independent password envelope. */
private fun sealSafety(bytes:ByteArray,identity:String):ByteArray {
    val cipher=NoteContentCipher()
    val sealed=cipher.seal(Base64.encodeToString(bytes,Base64.NO_WRAP),identity)
    check(Base64.decode(cipher.open(sealed,identity),Base64.NO_WRAP).contentEquals(bytes)) {"Safety encryption verification failed"}
    return JSONObject().put("format","todoshift-device-safety-v45").put("identity",identity).put("ciphertext",sealed).toString().toByteArray(Charsets.UTF_8)
}
fun openNeoSafetyCopy(file:File):java.io.InputStream {
    val bytes=file.readBytes()
    if(bytes.size>=2 && bytes[0]==0x50.toByte() && bytes[1]==0x4b.toByte()) return bytes.inputStream()
    val envelope=JSONObject(String(bytes,Charsets.UTF_8))
    require(envelope.getString("format")=="todoshift-device-safety-v45")
    return Base64.decode(NoteContentCipher().open(envelope.getString("ciphertext"),envelope.getString("identity")),Base64.NO_WRAP).inputStream()
}
fun protectNeoSafetyCopies(context:Context) {
    File(context.filesDir,"safety-copies").listFiles().orEmpty().filter {it.extension=="zip"}.forEach {file ->
        val legacy=file.inputStream().use {it.read()==0x50 && it.read()==0x4b}
        if(legacy) {
            val data=sealSafety(file.readBytes(),file.name)
            val target=AtomicFile(file);val stream=target.startWrite()
            try {stream.write(data);target.finishWrite(stream)} catch(failure:Exception) {target.failWrite(stream);throw failure}
        }
    }
}

/** A failed snapshot blocks the major change; no old safety copy is silently deleted. */
fun createNeoSafetyCopy(context: Context, expected: V11Database): String {
    val dir = File(context.filesDir, "safety-copies")
    check(dir.isDirectory || dir.mkdirs())
    val file = AtomicFile(File(dir, "before-${UUID.randomUUID()}.sealed"))
    val buffer=java.io.ByteArrayOutputStream()
    Neo4LocalBackup(context).export(expected,buffer)
    val data=sealSafety(buffer.toByteArray(),file.baseFile.name)
    val stream=file.startWrite()
    try {stream.write(data);file.finishWrite(stream)} catch(failure:Exception) {file.failWrite(stream);throw failure}
    return file.baseFile.name
}
