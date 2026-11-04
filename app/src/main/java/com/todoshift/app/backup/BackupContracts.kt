package com.todoshift.app.backup

import com.todoshift.app.v11.*
import com.todoshift.app.data.AppData
import java.io.InputStream
import java.io.OutputStream

data class Neo4RestorePreview(val createdAt: String, val appVersion: String, val calendars: Int, val days: Int, val tasks: Int, val payments: Int, val images: Int, val legacyJson: Boolean)
class Neo4PreparedRestore internal constructor(val token: String, val preview: Neo4RestorePreview, internal val bytes: ByteArray)
data class Neo4RestoredState(val database: V11Database, val legacy: AppData, val ui: V11UiSettings, val appearance: NeoAppearance, val activeCalendarId: String?, val warnings: List<String>)
interface BackupExport { fun export(database: V11Database, output: OutputStream) }
interface BackupImport { fun preview(input: InputStream): Neo4PreparedRestore }
interface BackupValidation { fun validate(prepared: Neo4PreparedRestore): Neo4RestorePreview }
interface RestoreCoordinator { fun restore(prepared: Neo4PreparedRestore, confirmedToken: String): Neo4RestoredState }
object Neo4DataGate { val monitor = Any() }
object Neo4BackupFormat {
    const val FORMAT = 2
    const val MAX_BYTES = 32 * 1024 * 1024
    const val MAX_SECTION = 12 * 1024 * 1024
    fun sha256(bytes: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
    fun safeAsset(name: String): Boolean = name.matches(Regex("themes/neo4-theme-[a-zA-Z0-9-]+[.]jpg"))
}

