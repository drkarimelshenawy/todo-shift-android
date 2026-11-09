package com.todoshift.app.v11

import android.content.Context
import java.time.LocalDate
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** Additive operational metadata: existing databases are never reset or reinterpreted. */
class NeoSmartStore(context: Context) {
    private val prefs = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    private fun read(): JSONObject =
        prefs.getString("data", null)?.let { JSONObject(it).also(::validate) }
            ?: JSONObject().put("version", 1)

    fun snapshot(): JSONObject =
        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) { read() }

    fun update(change: (JSONObject) -> Unit) =
        synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
            val next = read()
            change(next)
            validate(next)
            check(prefs.edit().putString("data", next.toString()).commit()) {
                "Metadata could not be saved"
            }
        }

    data class TaskTemplate(
        val id: String,
        val name: String,
        val titles: List<String>,
        val workplaceId: String?,
        val calendarId: String?,
        val shiftKind: ShiftKind? = null,
    )

    fun templates(): List<TaskTemplate> =
        snapshot()
            .optJSONArray("taskTemplates")
            ?.let { a ->
                (0 until a.length()).map { i ->
                    a.getJSONObject(i).let { o ->
                        TaskTemplate(
                            o.getString("id"),
                            o.getString("name"),
                            o.getJSONArray("titles").let { t ->
                                (0 until t.length()).map(t::getString)
                            },
                            o.optString("workplace").ifBlank { null },
                            o.optString("calendar").ifBlank { null },
                            runCatching { ShiftKind.valueOf(o.optString("shiftKind")) }.getOrNull(),
                        )
                    }
                }
            }
            .orEmpty()

    fun saveTemplate(
        name: String,
        titles: List<String>,
        workplace: String?,
        calendar: String?,
        shiftKind: ShiftKind? = null,
        id: String? = null,
    ) {
        require(
            name.isNotBlank() &&
                titles.isNotEmpty() &&
                titles.size <= 50 &&
                titles.all { it.isNotBlank() }
        )
        update { o ->
            val previous = o.optJSONArray("taskTemplates") ?: JSONArray()
            val a =
                JSONArray(
                    (0 until previous.length()).map(previous::getJSONObject).filter {
                        it.getString("id") != id
                    }
                )
            require(a.length() < 30)
            a.put(
                JSONObject()
                    .put("id", id ?: UUID.randomUUID().toString())
                    .put("name", name.take(100))
                    .put("titles", JSONArray(titles.map { it.take(300) }))
                    .put("workplace", workplace.orEmpty())
                    .put("calendar", calendar.orEmpty())
                    .put("shiftKind", shiftKind?.name.orEmpty())
            )
            o.put("taskTemplates", a)
        }
    }

    fun instantiate(template: TaskTemplate, date: LocalDate, db: V11Database): V11Database {
        val prefix = "template-${template.id}-$date-"
        if (db.tasks.any { it.id.startsWith(prefix) }) return db
        val tasks =
            template.titles.mapIndexed { index, title ->
                TaskV11(
                    prefix + index,
                    title,
                    date = date.toString(),
                    workplaceId = template.workplaceId,
                    calendarId =
                        template.calendarId?.takeIf { id -> db.calendars.any { it.id == id } },
                    linkedShiftId =
                        if (template.shiftKind == null) null
                        else
                            db.days
                                .filter { it.date == date.toString() && (template.calendarId == null || it.calendarId == template.calendarId) }
                                .flatMap { it.segments }
                                .firstOrNull {
                                    it.kind == template.shiftKind &&
                                        (template.workplaceId == null ||
                                            it.workplaceId == template.workplaceId)
                                }
                                ?.id,
                )
            }
        return db.copy(tasks = db.tasks + tasks)
    }

    fun deleteTemplate(id: String) {
        update { o ->
            val a = o.optJSONArray("taskTemplates") ?: JSONArray()
            o.put(
                "taskTemplates",
                JSONArray(
                    (0 until a.length()).map(a::getJSONObject).filter { it.getString("id") != id }
                ),
            )
        }
    }

    fun noteColor(id: String): String =
        snapshot().optJSONObject("noteColors")?.optString(id)?.takeIf {
            it.matches(Regex("#[0-9A-Fa-f]{6}"))
        } ?: "#D8AD55"

    fun setNoteColor(id: String, color: String) {
        require(color.matches(Regex("#[0-9A-Fa-f]{6}")))
        update { o ->
            val colors = o.optJSONObject("noteColors") ?: JSONObject()
            colors.put(id, color)
            o.put("noteColors", colors)
        }
    }

    companion object {
        const val NAME = "todo_shift_neo41_smart"

        fun validate(o: JSONObject) {
            require(o.getInt("version") == 1) { "Unsupported smart metadata version" }
            o.optJSONArray("taskTemplates")?.let { a ->
                require(a.length() <= 30)
                val ids = mutableSetOf<String>()
                for (i in 0 until a.length()) {
                    val t = a.getJSONObject(i)
                    require(t.getString("id").isNotBlank() && ids.add(t.getString("id")))
                    require(t.getString("name").isNotBlank())
                    val titles = t.getJSONArray("titles")
                    require(titles.length() in 1..50)
                    for (j in 0 until titles.length()) require(
                        titles.getString(j).isNotBlank() && titles.getString(j).length <= 300
                    )
                }
            }
            o.optJSONArray("shiftTemplates")?.let { a ->
                require(a.length() <= 30)
                val ids = mutableSetOf<String>()
                for (i in 0 until a.length()) {
                    val t = a.getJSONObject(i)
                    require(t.getString("id").isNotBlank() && ids.add(t.getString("id")))
                    require(t.getString("name").isNotBlank() && t.getString("name").length <= 100)
                    ShiftKind.valueOf(t.getString("kind"))
                    require(t.has("workplace"))
                }
            }
            o.optJSONArray("attachments")?.let { a ->
                require(a.length() <= 24)
                for (i in 0 until a.length()) {
                    val t = a.getJSONObject(i)
                    require(t.getString("owner").isNotBlank())
                    require(NeoAttachmentStore.safeName(t.getString("name")))
                    require(
                        t.getString("mime") in
                            listOf("image/jpeg", "image/png", "application/pdf", "text/plain")
                    )
                }
            }
            listOf("notify_tasks", "notify_events", "notify_shifts", "quietHours", "usageEnabled")
                .filter { o.has(it) }
                .forEach { require(o.get(it) is Boolean) }
        }
    }
}
