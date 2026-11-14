package com.todoshift.app.backup

import org.json.JSONArray
import org.json.JSONObject

enum class BackupSection { CALENDAR, TASKS, SALARY, CASHBOOK, SETTINGS }
enum class RestoreMode { MERGE, REPLACE }
data class RestoreInspection(
    val sections: Set<BackupSection>, val incoming: Int, val additions: Int,
    val duplicates: Int, val conflicts: Int, val replacements: Int,
    val dependencies: Int,
    val cashAccounts: Int = 0, val cashTransactions: Int = 0, val cashDebts: Int = 0,
)

/** Stable IDs are the merge key. Partial replacement never replaces shared dependencies. */
internal object SelectiveRestore {
    private fun canonical(value: Any?): String = when (value) {
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(prefix = "{", postfix = "}") { JSONObject.quote(it) + ":" + canonical(value.opt(it)) }
        is JSONArray -> (0 until value.length()).joinToString(prefix = "[", postfix = "]") { canonical(value.opt(it)) }
        else -> JSONArray().put(value).toString()
    }
    private fun same(first: JSONObject, second: JSONObject) = canonical(first) == canonical(second)
    private val owned = mapOf(
        BackupSection.CALENDAR to setOf("days", "dayItems"),
        BackupSection.TASKS to setOf("tasks", "dayItems"),
        BackupSection.SALARY to setOf("payments", "settlements", "monthSnapshots"),
        BackupSection.CASHBOOK to setOf("obligations", "obligationPayments"),
    )
    private val shared = setOf("calendars", "rates", "incomeSources", "paymentMethods")
    private val arrays = shared + owned.values.flatten() + "auditLog"
    fun selected(key: String, scope: Set<BackupSection>): Boolean = when (key) {
        "calendars" -> scope.any { it != BackupSection.SETTINGS }
        "days" -> scope.any { it in setOf(BackupSection.CALENDAR, BackupSection.SALARY, BackupSection.TASKS) }
        "rates", "incomeSources" -> BackupSection.CALENDAR in scope || BackupSection.SALARY in scope
        "paymentMethods" -> BackupSection.SALARY in scope || BackupSection.CASHBOOK in scope
        "auditLog" -> scope.any { it != BackupSection.SETTINGS }
        else -> scope.any { key in owned[it].orEmpty() }
    }

    private fun selectedItem(item: JSONObject, scope: Set<BackupSection>): Boolean = when (item.optString("type")) {
        "NOTE", "TASK" -> BackupSection.TASKS in scope
        else -> BackupSection.CALENDAR in scope
    }

    fun filtered(source: JSONObject, scope: Set<BackupSection>): JSONObject =
        JSONObject(source.toString()).apply {
            arrays.filterNot { selected(it, scope) }.forEach { put(it, JSONArray()) }
            if (BackupSection.CALENDAR !in scope && BackupSection.SALARY !in scope && BackupSection.TASKS in scope) {
                val tasks = source.getJSONArray("tasks")
                val linked = (0 until tasks.length()).mapNotNull { tasks.getJSONObject(it).optString("linkedShiftId").takeIf { id -> id.isNotBlank() && id != "null" } }.toSet()
                val days = source.getJSONArray("days")
                put("days", JSONArray((0 until days.length()).map { days.getJSONObject(it) }.filter { day ->
                    val segments = day.getJSONArray("segments")
                    (0 until segments.length()).any { segments.getJSONObject(it).getString("id") in linked }
                }))
            }
            val items = optJSONArray("dayItems") ?: JSONArray()
            put("dayItems", JSONArray((0 until items.length()).map { items.getJSONObject(it) }.filter { selectedItem(it, scope) }))
            if (scope != BackupSection.entries.toSet()) {
                val audit = optJSONArray("auditLog") ?: JSONArray()
                put("auditLog", JSONArray((0 until audit.length()).map { audit.getJSONObject(it) }.filter { item ->
                    when (item.optString("entityType").uppercase()) {
                        "PAYMENT", "SETTLEMENT", "MONTH", "INCOME_SOURCE" -> BackupSection.SALARY in scope
                        "OBLIGATION" -> BackupSection.CASHBOOK in scope
                        "TASK", "NOTE", "DAY_ITEM" -> BackupSection.TASKS in scope
                        "SHIFT_SEGMENT", "CALENDAR", "RATE", "WORKPLACE" -> BackupSection.CALENDAR in scope
                        else -> false
                    }
                }))
            }
        }

    fun plan(current: JSONObject, incoming: JSONObject, scope: Set<BackupSection>, mode: RestoreMode, wholeReplacement: Boolean = false): Pair<JSONObject, RestoreInspection> {
        require(scope.isNotEmpty()) { "Select at least one section" }
        val next = JSONObject(current.toString())
        val scopedIncoming = filtered(incoming, scope)
        var total = 0; var additions = 0; var duplicates = 0; var conflicts = 0; var replacements = 0; var dependencies = 0
        arrays.filter { selected(it, scope) }.forEach { key ->
            fun records(o: JSONObject): List<JSONObject> = o.getJSONArray(key).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
            val old = records(current).associateBy { it.getString("id") }
            val fresh = records(scopedIncoming)
            val sharedDependency = !wholeReplacement && (key in shared || key == "auditLog" ||
                (key == "days" && (BackupSection.SALARY !in scope || BackupSection.CALENDAR !in scope || current.getJSONArray("tasks").let { tasks -> (0 until tasks.length()).any { tasks.getJSONObject(it).optString("linkedShiftId").let { id -> id.isNotBlank() && id != "null" } } })))
            val retained = if (key == "dayItems") old.filterValues { !selectedItem(it, scope) } else emptyMap()
            val merged = if (mode == RestoreMode.MERGE || sharedDependency) LinkedHashMap(old) else LinkedHashMap(retained)
            fresh.forEach { record ->
                val id = record.getString("id"); val previous = old[id]
                if (key == "dayItems" && previous != null && previous.optString("type") != record.optString("type") && !selectedItem(previous, scope))
                    error("Shared day-item identity conflicts with an unrelated section: $id")
                total++
                if (sharedDependency) dependencies++
                if (previous == null) additions++
                else if (same(previous, record)) duplicates++
                else conflicts++
                // Existing shared identities always win: restoring tasks cannot change historical compensation.
                if (previous == null || (!sharedDependency && mode == RestoreMode.REPLACE)) merged[id] = record

            }
            if (mode == RestoreMode.REPLACE && !sharedDependency) replacements += old.size - retained.size
            next.put(key, JSONArray(merged.values.toList()))
        }
        return next to RestoreInspection(scope, total, additions, duplicates, conflicts, replacements, dependencies)
    }
}
