package com.todoshift.app.v11

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class NeoAttachmentStore(private val context: Context) {
    data class Attachment(val owner: String, val name: String, val mime: String)

    private val metadata = NeoSmartStore(context)

    fun list(): List<Attachment> =
        metadata
            .snapshot()
            .optJSONArray("attachments")
            ?.let { a ->
                (0 until a.length()).map {
                    a.getJSONObject(it).let { o ->
                        val name = o.getString("name")
                        require(safeName(name))
                        Attachment(o.getString("owner"), name, o.getString("mime"))
                    }
                }
            }
            .orEmpty()

    fun import(owner: String, uri: Uri): Attachment =
        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
            require(owner.isNotBlank())
            val declaredMime =
                context.contentResolver
                    .getType(uri)
                    ?.substringBefore(';')
                    ?.lowercase(java.util.Locale.ROOT)
            val data =
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var n = input.read(buffer)
                    while (n >= 0) {
                        if (n > 0) out.write(buffer, 0, n)
                        require(out.size() <= MAX_FILE) { "Attachment exceeds 4 MiB" }
                        n = input.read(buffer)
                    }
                    out.toByteArray()
                } ?: error("Cannot read attachment")
            require(data.isNotEmpty())
            // Some document providers report octet-stream. Recognize bounded supported content,
            // never infer an executable type from a user-controlled filename.
            val mime =
                when {
                    data.size >= 5 && data.copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()) ->
                        "application/pdf"
                    data.size >= 8 &&
                        data
                            .copyOfRange(0, 8)
                            .contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)) ->
                        "image/png"
                    data.size >= 3 &&
                        data[0] == (-1).toByte() &&
                        data[1] == (-40).toByte() &&
                        data[2] == (-1).toByte() -> "image/jpeg"
                    declaredMime == "text/plain" && data.none { it == 0.toByte() } -> {
                        Charsets.UTF_8.newDecoder()
                            .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                            .decode(java.nio.ByteBuffer.wrap(data))
                        "text/plain"
                    }
                    else -> error("Choose a valid PNG, JPEG, PDF or UTF-8 text file")
                }
            val ext =
                when (mime) {
                    "image/jpeg" -> "jpg"
                    "image/png" -> "png"
                    "application/pdf" -> "pdf"
                    else -> "txt"
                }
            require(
                list().sumOf { File(context.filesDir, "attachments/${it.name}").length() } +
                    data.size <= 24 * 1024 * 1024
            ) {
                "Attachment backup allowance is full"
            }
            val name = "neo-attachment-${UUID.randomUUID()}.$ext"
            val folder = File(context.filesDir, "attachments")
            check(folder.isDirectory || folder.mkdirs())
            val file = AtomicFile(File(folder, name))
            val stream = file.startWrite()
            try {
                stream.write(data)
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
            metadata.update { o ->
                val a = o.optJSONArray("attachments") ?: JSONArray()
                require(a.length() < 24)
                a.put(JSONObject().put("owner", owner).put("name", name).put("mime", mime))
                o.put("attachments", a)
            }
            Attachment(owner, name, mime)
        }

    fun uri(name: String): Uri {
        require(safeName(name))
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.attachments",
            File(context.filesDir, "attachments/$name"),
        )
    }

    fun detach(name: String) {
        metadata.update { o ->
            val a = o.optJSONArray("attachments") ?: JSONArray()
            o.put(
                "attachments",
                JSONArray(
                    (0 until a.length()).map(a::getJSONObject).filter {
                        it.getString("name") != name
                    }
                ),
            )
        }
    }

    /** Inventory includes detached private files; nothing is removed automatically. */
    fun detachedNames(): Set<String> =
        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
            val referenced = list().map { it.name }.toSet()
            File(context.filesDir, "attachments")
                .listFiles()
                .orEmpty()
                .filter { it.isFile && safeName(it.name) && it.name !in referenced }
                .map { it.name }
                .toSet()
        }

    fun storedBytes(): Long =
        File(context.filesDir, "attachments")
            .listFiles()
            .orEmpty()
            .filter { it.isFile && safeName(it.name) }
            .sumOf { it.length() }

    /** Caller must preview and explicitly confirm this exact unreferenced set. */
    fun removeDetached(expected: Set<String>) =
        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
            check(expected == detachedNames()) { "Storage changed; review again" }
            expected.forEach { name ->
                check(safeName(name))
                check(File(context.filesDir, "attachments/$name").delete()) {
                    "Unable to remove detached file"
                }
            }
        }

    companion object {
        const val MAX_FILE = 4 * 1024 * 1024

        fun safeName(name: String) =
            name.matches(Regex("neo-attachment-[a-zA-Z0-9-]+[.](png|jpg|pdf|txt)"))

        fun safePath(path: String) =
            path.startsWith("attachments/") && safeName(path.removePrefix("attachments/"))
    }
}
