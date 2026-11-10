package com.todoshift.app.backup

import android.content.Context
import android.content.SharedPreferences
import android.util.AtomicFile
import com.todoshift.app.data.AppStorage
import com.todoshift.app.reminders.ToDoShiftReminder
import com.todoshift.app.storage.requireLoaded
import com.todoshift.app.v11.*
import com.todoshift.app.widget.Neo4WidgetRefresh
import java.io.*
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

/**
 * All methods are blocking IO APIs. UI and future workers must invoke them on Dispatchers.IO.
 * Transaction journal covers preference writes; PINs, biometrics, OAuth tokens and keys are
 * excluded.
 */
class Neo4LocalBackup(context: Context) :
    BackupExport, BackupImport, BackupValidation, RestoreCoordinator {
    private val app = context.applicationContext
    private val storage = V11Storage(app)
    private val legacy = AppStorage(app)
    private val uiPreferences =
        listOf(
            "todo_shift_v11_ui",
            "todo_shift_v11_ui_state",
            "todo_shift_neo3_appearance",
            "todo_shift_neo4_profiles",
            "todo_shift_neo4_widgets",
            "todo_shift_cloud_readiness",
            NeoSmartStore.NAME,
        )
    internal var restoreCheckpoint: (String) -> Unit = {}
    private val journal = AtomicFile(File(app.filesDir, "neo4-restore-journal.json"))

    private fun prefs(name: String) = app.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun bytes(text: String) = text.toByteArray(Charsets.UTF_8)

    private fun json(data: ByteArray) = JSONObject(String(data, Charsets.UTF_8))

    override fun export(database: V11Database, output: OutputStream) =
        synchronized(Neo4DataGate.monitor) {
            recoverIfNeeded()
            val confirmedDatabase = storage.load()
            check(confirmedDatabase == database) { "Data changed; reopen before exporting" }
            val sections = linkedMapOf<String, ByteArray>()
            sections["data/v11.json"] = bytes(storage.exportJson(database))
            sections["data/legacy.json"] = bytes(legacy.exportJson(legacy.load()))
            uiPreferences.forEach {
                sections["prefs/$it.json"] = bytes(capture(prefs(it)).toString())
            }
            val appearances =
                listOf(NeoAppearanceStore(app).load()) +
                    Neo4Profiles(app).list().map { it.appearance }
            appearances
                .map { it.customImageFile }
                .filter { it.matches(Regex("neo4-theme-[a-zA-Z0-9-]+[.]jpg")) }
                .distinct()
                .forEach { name ->
                    val file = File(app.filesDir, name)
                    if (file.isFile)
                        sections["themes/$name"] =
                            file.inputStream().use { readBounded(it, 4 * 1024 * 1024) }
                }
            val validOwners = database.tasks.map { it.id }.toSet() + database.dayItems.map { it.id }
            NeoAttachmentStore(app)
                .list()
                .filter { it.owner in validOwners }
                .forEach { attachment ->
                    val file = File(app.filesDir, "attachments/${attachment.name}")
                    require(file.isFile) { "Missing referenced attachment: ${attachment.name}" }
                    sections["attachments/${attachment.name}"] =
                        file.inputStream().use { readBounded(it, NeoAttachmentStore.MAX_FILE) }
                }
            require(sections.values.sumOf { it.size.toLong() } <= Neo4BackupFormat.MAX_BYTES) {
                "Backup exceeds 32 MiB"
            }
            val manifest =
                JSONObject()
                    .put("formatVersion", Neo4BackupFormat.FORMAT)
                    .put("applicationId", "com.todoshift.app")
                    .put(
                        "appVersion",
                        app.packageManager.getPackageInfo(app.packageName, 0).versionName
                            ?: "unknown",
                    )
                    .put("neoEdition", "4.1")
                    .put("createdAt", Instant.now().toString())
                    .put("databaseSchema", 13)
                    .put(
                        "compatibility",
                        JSONObject().put("minReaderFormat", 2).put("legacySchema", 9),
                    )
                    .put("encrypted", false)
                    .put("securityCredentialsIncluded", false)
                    .put(
                        "dataManifest",
                        JSONObject()
                            .put("calendars", database.calendars.size)
                            .put("days", database.days.size)
                            .put("tasks", database.tasks.size)
                            .put("payments", database.payments.size),
                    )
                    .put(
                        "sections",
                        JSONArray().apply {
                            sections.forEach { (name, data) ->
                                put(
                                    JSONObject()
                                        .put("name", name)
                                        .put("bytes", data.size)
                                        .put("sha256", Neo4BackupFormat.sha256(data))
                                )
                            }
                        },
                    )
            ZipOutputStream(output).use { zip ->
                (linkedMapOf("manifest.json" to bytes(manifest.toString(2))) + sections).forEach {
                    (name, data) ->
                    require(data.size <= Neo4BackupFormat.MAX_SECTION)
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
        }

    override fun preview(input: InputStream): Neo4PreparedRestore {
        val raw = readBounded(input, Neo4BackupFormat.MAX_BYTES)
        val parsed = parse(raw)
        return Neo4PreparedRestore(Neo4BackupFormat.sha256(raw), parsed.preview, raw)
    }

    override fun validate(prepared: Neo4PreparedRestore): Neo4RestorePreview {
        require(Neo4BackupFormat.sha256(prepared.bytes) == prepared.token) {
            "Backup changed after preview"
        }
        return parse(prepared.bytes).preview
    }

    private data class Parsed(
        val sections: Map<String, ByteArray>,
        val database: V11Database,
        val preview: Neo4RestorePreview,
    )

    private fun parse(raw: ByteArray): Parsed {
        if (raw.firstOrNull { it.toInt().toChar() !in " \r\n\t" }?.toInt()?.toChar() == '{') {
            val database = validateDatabase(raw)
            return Parsed(
                mapOf("data/v11.json" to raw),
                database,
                Neo4RestorePreview(
                    "Legacy JSON",
                    "unknown",
                    database.calendars.size,
                    database.days.size,
                    database.tasks.size,
                    database.payments.size,
                    0,
                    true,
                ),
            )
        }
        val all = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(raw)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                require(!entry.isDirectory && all.size < 70 && !all.containsKey(name)) {
                    "Duplicate or invalid archive entry"
                }
                require(
                    name == "manifest.json" ||
                        name == "data/v11.json" ||
                        name == "data/legacy.json" ||
                        uiPreferences.any { name == "prefs/$it.json" } ||
                        (Neo4BackupFormat.safeAsset(name) || NeoAttachmentStore.safePath(name))
                ) {
                    "Unsupported archive path"
                }
                val data =
                    readBounded(
                        zip,
                        if (name == "manifest.json") 128 * 1024
                        else if (
                            Neo4BackupFormat.safeAsset(name) || NeoAttachmentStore.safePath(name)
                        )
                            4 * 1024 * 1024
                        else Neo4BackupFormat.MAX_SECTION,
                    )
                total += data.size
                require(total <= Neo4BackupFormat.MAX_BYTES) { "Expanded backup exceeds 32 MiB" }
                all[name] = data
                zip.closeEntry()
            }
        }
        val manifest = json(all.remove("manifest.json") ?: error("Missing manifest"))
        require(
            manifest.getInt("formatVersion") == Neo4BackupFormat.FORMAT &&
                manifest.getInt("databaseSchema") == 13
        ) {
            "Unsupported backup version"
        }
        require(manifest.getString("applicationId") == "com.todoshift.app") {
            "Backup belongs to another app"
        }
        require(!manifest.optBoolean("encrypted", false)) {
            "Encrypted backups are not supported by this version"
        }
        val sections = manifest.getJSONArray("sections")
        val expected = mutableSetOf<String>()
        for (i in 0 until sections.length()) {
            val item = sections.getJSONObject(i)
            val name = item.getString("name")
            require(expected.add(name)) { "Duplicate manifest entry" }
            val data = all[name] ?: error("Missing data section: $name")
            require(
                item.getLong("bytes") == data.size.toLong() &&
                    item.getString("sha256") == Neo4BackupFormat.sha256(data)
            ) {
                "Checksum mismatch: $name"
            }
        }
        require(
            all.keys == expected &&
                "data/v11.json" in all &&
                "data/legacy.json" in all &&
                uiPreferences.filterNot { it == NeoSmartStore.NAME }.all { "prefs/$it.json" in all }
        ) {
            "Incomplete data manifest"
        }
        require(
            manifest.getJSONObject("compatibility").getInt("minReaderFormat") <=
                Neo4BackupFormat.FORMAT
        )
        Instant.parse(manifest.getString("createdAt"))
        all.filterKeys { Neo4BackupFormat.safeAsset(it) }
            .forEach { (_, data) ->
                val bounds =
                    android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                require(bounds.outWidth in 1..1600 && bounds.outHeight in 1..1600) {
                    "Invalid or oversized theme image"
                }
            }
        val database = validateDatabase(all.getValue("data/v11.json"))
        val old = json(all.getValue("data/legacy.json"))
        require(
            old.optInt("schemaVersion") == 9 &&
                listOf("workplaces", "shifts", "payments", "dayDetails", "templates").all {
                    old.optJSONArray(it) != null
                }
        ) {
            "Invalid legacy data section"
        }
        listOf("workplaces", "payments", "templates").forEach { key ->
            val array = old.getJSONArray(key)
            val ids = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val id = array.getJSONObject(i).getString("id")
                require(id.isNotBlank() && ids.add(id)) { "Invalid legacy record ID" }
            }
        }
        val legacyData =
            legacy.parseImport(
                old.toString()
            ) // Pure parse only; never imports or writes before confirmation.
        val legacyAmounts =
            legacyData.payments.map { it.amount } +
                legacyData.dayDetails.map { it.extraAmount } +
                legacyData.shifts.mapNotNull { it.customAmount } +
                legacyData.templates.mapNotNull { it.customAmount } +
                legacyData.workplaces.flatMap {
                    listOf(
                        it.fullDayRate,
                        it.longRate,
                        it.nightRate,
                        it.morningRate,
                        it.eveningRate,
                    )
                }
        require(legacyAmounts.all { it.isFinite() }) { "Non-finite legacy financial value" }
        uiPreferences.forEach { name ->
            all["prefs/$name.json"]?.let { validateUiPreferences(name, json(it)) }
        }
        all["prefs/${NeoSmartStore.NAME}.json"]?.let { section ->
            val saved = json(section).optJSONObject("data")
            if (saved != null) {
                require(saved.getString("type") == "string")
                val smart = JSONObject(saved.getString("value"))
                NeoSmartStore.validate(smart)
                smart.optJSONArray("attachments")?.let { attachments ->
                    for (index in 0 until attachments.length()) {
                        val attachment = attachments.getJSONObject(index)
                        val name = attachment.getString("name")
                        require(NeoAttachmentStore.safeName(name))
                        if (
                            attachment.getString("owner") in
                                (database.tasks.map { it.id } + database.dayItems.map { it.id })
                        )
                            require("attachments/$name" in all) { "Missing attachment section" }
                    }
                }
            }
        }
        val appearancePrefs = json(all.getValue("prefs/todo_shift_neo3_appearance.json"))
        appearancePrefs.optJSONObject("neo4Appearance")?.optString("value")?.let {
            Neo4AppearanceCodec.decode(JSONObject(it))
        }
        return Parsed(
            all,
            database,
            Neo4RestorePreview(
                manifest.getString("createdAt"),
                manifest.getString("appVersion"),
                database.calendars.size,
                database.days.size,
                database.tasks.size,
                database.payments.size,
                all.keys.count { Neo4BackupFormat.safeAsset(it) },
                false,
            ),
        )
    }

    private fun validateDatabase(data: ByteArray): V11Database {
        val o = json(data)
        o.getJSONArray("days").let { days ->
            for (i in 0 until days.length()) {
                val segments = days.getJSONObject(i).getJSONArray("segments")
                for (j in 0 until segments.length()) ShiftKind.valueOf(
                    segments.getJSONObject(j).getString("kind")
                )
            }
        }
        val database = storage.importJson(o.toString()) // V11 importJson is a pure parser.
        require(database.schemaVersion == 13)
        listOf(
                "calendars",
                "days",
                "rates",
                "incomeSources",
                "paymentMethods",
                "payments",
                "settlements",
                "monthSnapshots",
                "auditLog",
                "dayItems",
                "tasks",
                "obligations",
                "obligationPayments",
            )
            .forEach { key ->
                val array = o.getJSONArray(key)
                val ids = mutableSetOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = item.getString("id")
                    require(id.isNotBlank() && ids.add(id)) {
                        "Invalid or duplicate record ID in $key"
                    }
                }
            }
        database.days.forEach { day ->
            LocalDate.parse(day.date)
            val ids = mutableSetOf<String>()
            day.segments.forEach { s ->
                require(s.id.isNotBlank() && ids.add(s.id)) { "Duplicate shift ID" }
                if (s.startMinute != null || s.endMinute != null)
                    require(
                        s.startMinute != null &&
                            s.endMinute != null &&
                            s.startMinute in 0..1439 &&
                            s.endMinute in 1..2879 &&
                            s.endMinute > s.startMinute
                    ) {
                        "Invalid shift time"
                    }
            }
        }
        database.tasks.forEach { LocalDate.parse(it.date) }
        val amounts =
            database.rates.map { it.amount } +
                database.payments.map { it.amount } +
                database.settlements.map { it.amount } +
                database.obligationPayments.map { it.amount } +
                database.obligations.flatMap {
                    listOfNotNull(it.totalAmount, it.installmentAmount)
                } +
                database.days.flatMap { it.segments }.mapNotNull { it.exceptionalAmount } +
                database.payments.flatMap { it.allocations }.map { it.amount } +
                database.incomeSources.mapNotNull { it.fixedMonthlyAmount } +
                database.monthSnapshots.flatMap {
                    listOf(
                        it.expectedAmount,
                        it.receivedAmount,
                        it.settlementAmount,
                        it.finalDifference,
                    )
                }
        require(amounts.all { it.isFinite() }) { "Non-finite financial value" }
        return database
    }

    override fun restore(prepared: Neo4PreparedRestore, confirmedToken: String): Neo4RestoredState =
        restoreChecked(prepared, confirmedToken, false)

    /**
     * Only the authenticated boot recovery UI may explicitly request replacement of a failed store.
     */
    fun recoverFromBackup(
        prepared: Neo4PreparedRestore,
        confirmedToken: String,
    ): Neo4RestoredState = restoreChecked(prepared, confirmedToken, true)

    private fun restoreChecked(
        prepared: Neo4PreparedRestore,
        confirmedToken: String,
        recovery: Boolean,
    ): Neo4RestoredState =
        synchronized(Neo4DataGate.monitor) {
            require(confirmedToken == prepared.token) {
                "Restore confirmation does not match preview"
            }
            validate(prepared)
            recoverIfNeeded()
            val parsed = parse(prepared.bytes)
            val currentRead = storage.read()
            val oldDatabase =
                if (recovery) (currentRead as? com.todoshift.app.storage.StorageRead.Loaded)?.data
                else currentRead.requireLoaded()
            if (recovery && parsed.preview.legacyJson) legacy.load()
            val targets = linkedMapOf<String, JSONObject>()
            targets["todo_shift_v11"] =
                capture(prefs("todo_shift_v11"))
                    .put(
                        "database",
                        typed(String(parsed.sections.getValue("data/v11.json"), Charsets.UTF_8)),
                    )
                    .put("migrated_from_rc1", typed(true))
            parsed.sections["data/legacy.json"]?.let {
                targets["todo_shift_data"] =
                    capture(prefs("todo_shift_data"))
                        .put("database", typed(String(it, Charsets.UTF_8)))
            }
            uiPreferences.forEach { name ->
                parsed.sections["prefs/$name.json"]?.let { targets[name] = json(it) }
            }
            val previous = JSONObject()
            targets.keys.forEach { name -> previous.put(name, capture(prefs(name))) }
            val createdAssets = JSONArray()
            parsed.sections
                .filterKeys { Neo4BackupFormat.safeAsset(it) || NeoAttachmentStore.safePath(it) }
                .forEach { (path, data) ->
                    val file =
                        if (NeoAttachmentStore.safePath(path)) File(app.filesDir, path)
                        else File(app.filesDir, path.substringAfter('/'))
                    if (!file.exists())
                        createdAssets.put(
                            JSONObject()
                                .put("path", path)
                                .put("sha256", Neo4BackupFormat.sha256(data))
                        )
                }
            val journalBytes =
                bytes(
                    JSONObject()
                        .put("format", 1)
                        .put("previous", previous)
                        .put("createdAssets", createdAssets)
                        .toString()
                )
            require(journalBytes.size <= 48 * 1024 * 1024) {
                "Rollback snapshot exceeds recovery limit"
            }
            if (recovery) {
                // Preserve exact raw preference values, including malformed payloads; never
                // auto-delete.
                val quarantine =
                    AtomicFile(
                        File(
                            app.filesDir,
                            "neo41-before-recovery-${java.util.UUID.randomUUID()}.json",
                        )
                    )
                writeAtomic(quarantine, journalBytes)
            }
            NeoCommittedEffects(app).prepare(oldDatabase ?: V11Database())
            writeAtomic(journal, journalBytes)
            try {
                restoreCheckpoint("journal")
                // Stage image files before changing data. Existing files with different content are
                // rejected.
                parsed.sections
                    .filterKeys {
                        Neo4BackupFormat.safeAsset(it) || NeoAttachmentStore.safePath(it)
                    }
                    .forEach { (path, data) ->
                        val file =
                            if (NeoAttachmentStore.safePath(path)) File(app.filesDir, path)
                            else File(app.filesDir, path.substringAfter('/'))
                        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
                        if (file.exists())
                            require(
                                Neo4BackupFormat.sha256(file.readBytes()) ==
                                    Neo4BackupFormat.sha256(data)
                            ) {
                                "Image filename collision"
                            }
                        else writeAtomic(AtomicFile(file), data)
                    }
                restoreCheckpoint("assets")
                targets.forEach { (name, data) ->
                    writePreferences(prefs(name), data)
                    restoreCheckpoint("prefs:$name")
                }
                // Success becomes durable only after every preference commit has completed.
                journal.delete()
                check(!journal.baseFile.exists()) { "Unable to finalize restore journal" }
            } catch (failure: Exception) {
                try {
                    recoverIfNeeded()
                } catch (rollback: Exception) {
                    failure.addSuppressed(rollback)
                }
                throw failure
            }
            val warnings = mutableListOf<String>()
            runCatching {
                    oldDatabase?.tasks?.forEach { ToDoShiftReminder.cancel(app, it.id) }
                    oldDatabase?.dayItems?.forEach { ToDoShiftReminder.cancel(app, it.id) }
                    ToDoShiftReminder.rescheduleAll(app, parsed.database)
                }
                .onFailure { warnings += "Reminder scheduling requires review in Android settings" }
            runCatching { Neo4WidgetRefresh.schedule(app) }
            Neo4RestoredState(
                parsed.database,
                legacy.load(),
                V11UiSettingsStore(app).load(),
                NeoAppearanceStore(app).load(),
                V11ActiveCalendarStore(app).load(),
                warnings,
            )
        }

    fun updateWorkplaces(
        expectedLegacy: com.todoshift.app.data.AppData,
        expected: V11Database,
        workplaces: List<com.todoshift.app.data.Workplace>,
    ): Pair<com.todoshift.app.data.AppData, V11Database> =
        synchronized(Neo4DataGate.monitor) {
            recoverIfNeeded()
            check(legacy.load() == expectedLegacy && storage.load() == expected) {
                "Data changed; reopen before editing workplaces"
            }
            val updatedLegacy = expectedLegacy.copy(workplaces = workplaces)
            val newPlaces =
                workplaces.filter { place -> expectedLegacy.workplaces.none { it.id == place.id } }
            val updated =
                NeoWorkplaceSources.ensure(NeoRatePolicy.reconcile(expected, workplaces), newPlaces)
            val previous =
                JSONObject()
                    .put("todo_shift_data", capture(prefs("todo_shift_data")))
                    .put("todo_shift_v11", capture(prefs("todo_shift_v11")))
            val snapshot = bytes(JSONObject().put("format", 1).put("previous", previous).toString())
            require(snapshot.size <= 48 * 1024 * 1024)
            writeAtomic(journal, snapshot)
            try {
                legacy.save(updatedLegacy)
                storage.save(updated)
                journal.delete()
                check(!journal.baseFile.exists()) { "Unable to finalize workplace update" }
            } catch (failure: Exception) {
                try {
                    previous.keys().forEach {
                        writePreferences(prefs(it), previous.getJSONObject(it))
                    }
                    journal.delete()
                } catch (rollback: Exception) {
                    failure.addSuppressed(rollback)
                }
                throw failure
            }
            runCatching { Neo4WidgetRefresh.schedule(app) }
            updatedLegacy to updated
        }

    /** Text and decorative metadata share the existing rollback journal and restore gate. */
    internal fun saveNote(
        expected: V11Database,
        expectedMetadata: String,
        note: DayItemV11,
        color: String,
        border: Boolean,
        checkpoint: (String) -> Unit = {},
    ): V11Database =
        synchronized(Neo4DataGate.monitor) {
            recoverIfNeeded()
            require(note.type == DayItemTypeV11.NOTE)
            require(color.matches(Regex("#[0-9a-fA-F]{6}")))
            val smart = NeoSmartStore(app)
            check(storage.load() == expected && smart.snapshot().toString() == expectedMetadata) {
                "Data changed; reopen before saving the note. Draft retained."
            }
            val next =
                expected.copy(dayItems = expected.dayItems.filterNot { it.id == note.id } + note)
            val previous =
                JSONObject()
                    .put("todo_shift_v11", capture(prefs("todo_shift_v11")))
                    .put(NeoSmartStore.NAME, capture(prefs(NeoSmartStore.NAME)))
            val snapshot = bytes(JSONObject().put("format", 1).put("previous", previous).toString())
            require(snapshot.size <= 48 * 1024 * 1024)
            NeoCommittedEffects(app).prepare(expected)
            writeAtomic(journal, snapshot)
            try {
                checkpoint("journal")
                storage.save(next)
                checkpoint("text")
                smart.update { o ->
                    val colors = o.optJSONObject("noteColors") ?: JSONObject()
                    colors.put(note.id, color)
                    o.put("noteColors", colors)
                    val borders = o.optJSONObject("noteBorders") ?: JSONObject()
                    borders.put(note.id, border)
                    o.put("noteBorders", borders)
                }
                checkpoint("style")
                journal.delete()
                check(!journal.baseFile.exists()) { "Unable to finalize note transaction" }
            } catch (failure: Exception) {
                try {
                    recoverIfNeeded()
                } catch (rollback: Exception) {
                    failure.addSuppressed(rollback)
                }
                throw failure
            }
            runCatching { Neo4WidgetRefresh.schedule(app) }
            next
        }

    fun recoverIfNeeded() =
        synchronized(Neo4DataGate.monitor) {
            val raw =
                try {
                    journal.openRead().use { readBounded(it, 48 * 1024 * 1024) }
                } catch (_: FileNotFoundException) {
                    return@synchronized
                }
            val o = json(raw)
            require(o.getInt("format") == 1) { "Unsupported restore recovery journal" }
            val previous = o.getJSONObject("previous")
            require(
                previous.keys().asSequence().all {
                    it in uiPreferences || it == "todo_shift_v11" || it == "todo_shift_data"
                }
            )
            val created = o.optJSONArray("createdAssets") ?: JSONArray()
            for (i in 0 until created.length()) {
                val asset = created.getJSONObject(i)
                val path = asset.getString("path")
                require(Neo4BackupFormat.safeAsset(path) || NeoAttachmentStore.safePath(path))
                require(asset.getString("sha256").matches(Regex("[0-9a-f]{64}")))
            }
            previous.keys().forEach { name ->
                writePreferences(prefs(name), previous.getJSONObject(name))
            }
            for (i in 0 until created.length()) {
                val asset = created.getJSONObject(i)
                val path = asset.getString("path")
                val file =
                    if (NeoAttachmentStore.safePath(path)) File(app.filesDir, path)
                    else File(app.filesDir, path.substringAfter('/'))
                // Keep anything referenced by the restored snapshot, preexisting, or modified.
                if (
                    !previous.toString().contains(file.name) &&
                        file.isFile &&
                        Neo4BackupFormat.sha256(file.readBytes()) == asset.getString("sha256")
                ) {
                    check(file.delete()) { "Unable to remove interrupted staged asset" }
                }
            }
            journal.delete()
        }

    private fun capture(preferences: SharedPreferences): JSONObject =
        JSONObject().apply {
            preferences.all.forEach { (key, value) -> if (value != null) put(key, typed(value)) }
        }

    private fun typed(value: Any): JSONObject =
        JSONObject().apply {
            when (value) {
                is String -> {
                    put("type", "string")
                    put("value", value)
                }
                is Boolean -> {
                    put("type", "boolean")
                    put("value", value)
                }
                is Int -> {
                    put("type", "int")
                    put("value", value)
                }
                is Long -> {
                    put("type", "long")
                    put("value", value)
                }
                is Float -> {
                    require(value.isFinite())
                    put("type", "float")
                    put("value", value)
                }
                is Set<*> -> {
                    require(value.all { it is String })
                    put("type", "strings")
                    put("value", JSONArray(value.toList()))
                }
                else -> error("Unsupported preference type")
            }
        }

    /** Known preference keys must retain the type consumed by existing readers. */
    private fun validateUiPreferences(name: String, o: JSONObject) {
        validatePreferences(o)
        val expected =
            when (name) {
                "todo_shift_v11_ui" ->
                    mapOf("showWorkplaceNames" to "boolean").withDefault { "string" }
                NeoSmartStore.NAME -> mapOf("data" to "string")
                "todo_shift_v11_ui_state" -> mapOf("active_calendar_id" to "string")
                "todo_shift_neo3_appearance" ->
                    mapOf(
                        "intensity" to "float",
                        "fontScale" to "float",
                        "cornerRadius" to "int",
                        "motion" to "int",
                        "glass" to "boolean",
                        "glow" to "boolean",
                        "reduceMotion" to "boolean",
                        "dashboardExpanded" to "boolean",
                        "showShiftNotes" to "boolean",
                        "showFinanceChart" to "boolean",
                        "hiddenCards" to "strings",
                        "quickActions" to "strings",
                    )
                "todo_shift_neo4_profiles" -> mapOf("profiles" to "string")
                "todo_shift_neo4_widgets" ->
                    com.todoshift.app.widget.Neo4WidgetKind.entries.associate {
                        it.name to "string"
                    }
                "todo_shift_cloud_readiness" ->
                    mapOf("frequency" to "string", "wifiOnly" to "boolean")
                else -> emptyMap()
            }
        o.keys().forEach { key ->
            val type =
                when (name) {
                    "todo_shift_v11_ui" ->
                        if (key == "showWorkplaceNames") "boolean"
                        else if (
                            key in
                                listOf(
                                    "todayHighlightStyle",
                                    "todayHighlightColor",
                                    "fabSide",
                                    "appTheme",
                                    "calendarStyle",
                                    "widgetTheme",
                                    "experience",
                                    "premiumBrightness",
                                )
                        )
                            "string"
                        else null
                    "todo_shift_neo3_appearance" ->
                        expected[key]
                            ?: if (
                                key in
                                    listOf(
                                        "neo4Appearance",
                                        "brightness",
                                        "palette",
                                        "accentHex",
                                        "cardStyle",
                                        "density",
                                        "typeface",
                                        "iconStyle",
                                        "calendarView",
                                        "calendarDensity",
                                        "language",
                                        "dashboardOrder",
                                        "timelineFilter",
                                    )
                            )
                                "string"
                            else null
                    else -> expected[key]
                }
            if (type != null)
                require(o.getJSONObject(key).getString("type") == type) {
                    "Invalid preference type in $name/$key"
                }
        }
    }

    private fun validatePreferences(o: JSONObject) {
        require(o.length() <= 256)
        o.keys().forEach { key ->
            require(key.length <= 160)
            val v = o.getJSONObject(key)
            when (v.getString("type")) {
                "string" -> v.getString("value")
                "boolean" -> v.getBoolean("value")
                "int" -> v.getInt("value")
                "long" -> v.getLong("value")
                "float" ->
                    require(
                        v.getDouble("value").isFinite() && v.getDouble("value").toFloat().isFinite()
                    )
                "strings" -> {
                    val a = v.getJSONArray("value")
                    require(a.length() <= 256)
                    for (i in 0 until a.length()) a.getString(i)
                }
                else -> error("Unsupported preference type")
            }
        }
    }

    private fun writePreferences(preferences: SharedPreferences, o: JSONObject) {
        validatePreferences(o)
        val editor = preferences.edit().clear()
        o.keys().forEach { key ->
            val v = o.getJSONObject(key)
            when (v.getString("type")) {
                "string" -> editor.putString(key, v.getString("value"))
                "boolean" -> editor.putBoolean(key, v.getBoolean("value"))
                "int" -> editor.putInt(key, v.getInt("value"))
                "long" -> editor.putLong(key, v.getLong("value"))
                "float" -> editor.putFloat(key, v.getDouble("value").toFloat())
                "strings" -> {
                    val a = v.getJSONArray("value")
                    editor.putStringSet(key, (0 until a.length()).map { a.getString(it) }.toSet())
                }
            }
        }
        check(editor.commit()) { "Unable to persist restored data" }
    }

    private fun writeAtomic(file: AtomicFile, data: ByteArray) {
        val output = file.startWrite()
        try {
            output.write(data)
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }

    private fun readBounded(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= limit) { "Backup section exceeds limit" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
