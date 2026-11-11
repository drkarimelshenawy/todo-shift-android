package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.backup.Neo4DataGate
import org.json.JSONObject
import java.util.UUID

data class Neo4LayoutProfile(val id: String, val name: String, val appearance: NeoAppearance)
class Neo4Profiles(context: Context) {
    private val prefs = context.getSharedPreferences("todo_shift_neo4_profiles", Context.MODE_PRIVATE)

    /** Corruption is reported, never converted to an empty or partial successful read. */
    fun list(): List<Neo4LayoutProfile> = synchronized(Neo4DataGate.monitor) {
        try {
            val root = JSONObject(prefs.getString("profiles", "{}") ?: "{}")
            root.keys().asSequence().map { id ->
                val item = root.getJSONObject(id)
                val name = item.getString("name")
                require(id.isNotBlank() && name.isNotBlank())
                Neo4LayoutProfile(id, name, Neo4AppearanceCodec.decode(item.getJSONObject("appearance")))
            }.toList()
        } catch (failure: Exception) {
            throw IllegalStateException("Unable to read layout profiles; stored data was preserved", failure)
        }
    }

    fun save(name: String, appearance: NeoAppearance) = synchronized(Neo4DataGate.monitor) {
        require(name.isNotBlank() && name.length <= 60)
        require(list().size < 12) { "Maximum 12 profiles" }
        val root = JSONObject(prefs.getString("profiles", "{}") ?: "{}")
        root.put(UUID.randomUUID().toString(), JSONObject().put("name", name.trim())
            .put("appearance", Neo4AppearanceCodec.encode(appearance)))
        check(prefs.edit().putString("profiles", root.toString()).commit()) { "Unable to save layout profiles" }
    }

    fun delete(id: String) = synchronized(Neo4DataGate.monitor) {
        list() // Refuse to mutate a partially corrupt collection.
        val root = JSONObject(prefs.getString("profiles", "{}") ?: "{}")
        root.remove(id)
        check(prefs.edit().putString("profiles", root.toString()).commit()) { "Unable to save layout profiles" }
    }
}
