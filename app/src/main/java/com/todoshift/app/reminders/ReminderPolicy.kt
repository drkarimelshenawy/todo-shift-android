package com.todoshift.app.reminders

import android.content.Context
import com.todoshift.app.v11.NeoSmartStore
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/** Durable scheduling metadata; never creates copies of tasks or rewrites due dates. */
class ReminderPolicy(context:Context) {
    private val store=NeoSmartStore(context)
    fun snooze(id:String,at:Long) {store.update {config ->
        val snoozes=config.optJSONObject("reminderSnoozes") ?: JSONObject()
        snoozes.put(id,at);config.put("reminderSnoozes",snoozes)
    }}
    fun configure(id:String,kind:String,days:Int=1) {
        require(kind in kinds && days in 1..3660)
        store.update {config ->
            val rules=config.optJSONObject("reminderRecurrence") ?: JSONObject()
            rules.put(id,JSONObject().put("kind",kind).put("days",days));config.put("reminderRecurrence",rules)
        }
    }
    fun rule(id:String):Pair<String,Int> = store.snapshot().optJSONObject("reminderRecurrence")?.optJSONObject(id)
        ?.let {it.optString("kind","NONE") to it.optInt("days",1)} ?: ("NONE" to 1)

    fun next(id:String,original:Long,fallback:String="NONE",now:Long=System.currentTimeMillis()):Long? {
        val config=store.snapshot()
        val snooze=config.optJSONObject("reminderSnoozes")?.optLong(id,0) ?: 0L
        if(snooze>now) return snooze
        val rule=config.optJSONObject("reminderRecurrence")?.optJSONObject(id)
        val kind=rule?.optString("kind",fallback) ?: fallback
        if(original>now) return original
        if(kind=="NONE") return null
        val zone=ZoneId.systemDefault()
        val anchor=Instant.ofEpochMilli(original).atZone(zone)
        val current=Instant.ofEpochMilli(now).atZone(zone)
        val days=(rule?.optInt("days",1) ?: 1).coerceIn(1,3660)
        // Compute from the original date, retaining Jan 31/Feb 29 anchors rather than drifting.
        var n=when(kind) {
            "DAILY" -> java.time.temporal.ChronoUnit.DAYS.between(anchor.toLocalDate(),current.toLocalDate())
            "WEEKLY" -> java.time.temporal.ChronoUnit.DAYS.between(anchor.toLocalDate(),current.toLocalDate())/7
            "MONTHLY" -> java.time.temporal.ChronoUnit.MONTHS.between(java.time.YearMonth.from(anchor),java.time.YearMonth.from(current))
            "YEARLY" -> (current.year-anchor.year).toLong()
            "CUSTOM" -> java.time.temporal.ChronoUnit.DAYS.between(anchor.toLocalDate(),current.toLocalDate())/days
            else -> return null
        }.coerceAtLeast(0)
        fun occurrence(index:Long)=when(kind) {"DAILY"->anchor.plusDays(index);"WEEKLY"->anchor.plusWeeks(index);"MONTHLY"->anchor.plusMonths(index);"YEARLY"->anchor.plusYears(index);else->anchor.plusDays(index*days)}.toInstant().toEpochMilli()
        var result=occurrence(n)
        while(result<=now) {n++;result=occurrence(n)}
        return result
    }
    companion object {val kinds=listOf("NONE","DAILY","WEEKLY","MONTHLY","YEARLY","CUSTOM")}
}
