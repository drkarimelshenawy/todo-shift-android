package com.todoshift.app.v11

import android.content.Context
import org.json.JSONObject
import java.util.UUID

data class Neo4LayoutProfile(val id: String, val name: String, val appearance: NeoAppearance)
class Neo4Profiles(context: Context) {
    private val prefs = context.getSharedPreferences("todo_shift_neo4_profiles", Context.MODE_PRIVATE)
    fun list(): List<Neo4LayoutProfile> = runCatching {
        val o = JSONObject(prefs.getString("profiles", "{}") ?: "{}")
        o.keys().asSequence().mapNotNull { id -> runCatching { val v = o.getJSONObject(id)
            Neo4LayoutProfile(id, v.getString("name"), Neo4AppearanceCodec.decode(v.getJSONObject("appearance"))) }.getOrNull() }.toList()
    }.getOrDefault(emptyList())
    fun save(name: String, appearance: NeoAppearance) {
        require(name.isNotBlank() && name.length <= 60)
        val all = list(); require(all.size < 12) { "Maximum 12 profiles" }
        val o = JSONObject(prefs.getString("profiles", "{}") ?: "{}")
        o.put(UUID.randomUUID().toString(), JSONObject().put("name", name.trim()).put("appearance", Neo4AppearanceCodec.encode(appearance)))
        prefs.edit().putString("profiles", o.toString()).apply()
    }
    fun delete(id: String) { val o = JSONObject(prefs.getString("profiles", "{}") ?: "{}"); o.remove(id); prefs.edit().putString("profiles", o.toString()).apply() }
}
