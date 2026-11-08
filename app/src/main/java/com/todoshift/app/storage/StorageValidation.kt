package com.todoshift.app.storage

import org.json.JSONArray
import org.json.JSONObject

/** Reject broken collection structures before permissive historical scalar defaults are used. */
internal fun JSONObject.objectArray(key: String, required: Boolean = true): List<JSONObject> {
    if (!has(key) && !required) return emptyList()
    val array = get(key) as? JSONArray ?: error("Invalid array: $key")
    return (0 until array.length()).map { index ->
        array.get(index) as? JSONObject ?: error("Invalid object: $key[$index]")
    }
}

internal fun JSONObject.nonBlankString(key: String): String =
    (get(key) as? String)?.takeIf { it.isNotBlank() } ?: error("Invalid string: $key")

internal fun JSONObject.schemaVersion(supported: Set<Int>): Int {
    val value = get("schemaVersion")
    require(value is Number && value.toDouble() == value.toInt().toDouble() && value.toInt() in supported) {
        "Unsupported database schema"
    }
    return value.toInt()
}
