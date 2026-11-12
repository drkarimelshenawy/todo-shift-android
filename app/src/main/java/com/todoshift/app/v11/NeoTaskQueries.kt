package com.todoshift.app.v11

import org.json.JSONArray
import org.json.JSONObject

enum class NeoTaskPeriod { TODAY, UPCOMING, OVERDUE, ALL_DATES }
enum class NeoTaskStatus { ALL, ACTIVE, COMPLETED }
enum class NeoTaskSort { DATE, PRIORITY, MANUAL }
data class NeoTaskListOptions(val compact: Boolean = true, val hideCompleted: Boolean = false,
    val sort: NeoTaskSort = NeoTaskSort.DATE, val order: List<String> = emptyList()) {
    fun json() = JSONObject().put("compact",compact).put("hideCompleted",hideCompleted)
        .put("sort",sort.name).put("order",JSONArray(order))
    companion object {
        fun read(root: JSONObject): NeoTaskListOptions {
            if(!root.has("taskListOptions")) return NeoTaskListOptions()
            val o=root.getJSONObject("taskListOptions")
            listOf("compact","hideCompleted").forEach { if(o.has(it)) require(o.get(it) is Boolean) }
            val a=o.optJSONArray("order") ?: JSONArray()
            return NeoTaskListOptions(o.optBoolean("compact",true),o.optBoolean("hideCompleted",false),
                NeoTaskSort.valueOf(o.optString("sort","DATE")),List(a.length()) { a.getString(it) }.distinct())
        }
    }
}
object NeoTaskQueries {
    fun select(tasks: List<TaskV11>, today: String, period: NeoTaskPeriod, status: NeoTaskStatus,
        search: String = "", priority: TaskPriorityV11? = null, calendar: String? = null,
        options: NeoTaskListOptions = NeoTaskListOptions()): List<TaskV11> {
        val found=tasks.filter { task ->
            (when(period) { NeoTaskPeriod.TODAY -> task.date==today; NeoTaskPeriod.UPCOMING -> task.date>today
                NeoTaskPeriod.OVERDUE -> task.date<today; NeoTaskPeriod.ALL_DATES -> true }) &&
            (when(status) { NeoTaskStatus.ALL -> !options.hideCompleted || task.status!=TaskStatusV11.COMPLETED
                NeoTaskStatus.ACTIVE -> task.status!=TaskStatusV11.COMPLETED; NeoTaskStatus.COMPLETED -> task.status==TaskStatusV11.COMPLETED }) &&
            (search.isBlank() || task.title.contains(search,true) || task.details.contains(search,true)) &&
            (priority==null || task.priority==priority) && (calendar==null || task.calendarId==calendar)
        }
        return when(options.sort) {
            NeoTaskSort.DATE -> found.sortedWith(compareBy<TaskV11> { it.date }.thenBy { it.time ?: "99:99" }.thenByDescending { it.priority.ordinal }.thenBy { it.id })
            NeoTaskSort.PRIORITY -> found.sortedWith(compareByDescending<TaskV11> { it.priority.ordinal }.thenBy { it.date }.thenBy { it.id })
            NeoTaskSort.MANUAL -> found.sortedWith(compareBy<TaskV11> { options.order.indexOf(it.id).takeIf { it>=0 } ?: Int.MAX_VALUE }.thenBy { it.createdAt }.thenBy { it.id })
        }
    }
    fun move(all: List<TaskV11>, order: List<String>, visible: List<TaskV11>, id: String, delta: Int): List<String> {
        val index=visible.indexOfFirst { it.id==id };val target=visible.getOrNull(index+delta) ?: return order
        require(index>=0 && delta in listOf(-1,1))
        val result=(order.filter { saved -> all.any { it.id==saved } }+all.map { it.id }).distinct().toMutableList()
        val a=result.indexOf(id);val b=result.indexOf(target.id)
        result[a]=target.id;result[b]=id;return result
    }
}
