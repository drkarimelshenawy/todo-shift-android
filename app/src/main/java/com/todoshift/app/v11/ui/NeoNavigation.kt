package com.todoshift.app.v11.ui

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

internal data class NeoRoute(
    val tab: Int = 0,
    val screen: String = "ROOT",
    val feature: String = "SEARCH",
    val owner: String? = null,
)

/**
 * One saved owner drives body, title and selected destination; history holds IDs, never records.
 */
@Stable
class NeoNavigator(initialTab: Int = 0) {
    internal var route by mutableStateOf(NeoRoute(tab = initialTab.takeIf { it in 0..4 } ?: 0))
        private set

    private var parents by mutableStateOf(emptyList<NeoRoute>())
    val tab
        get() = route.tab

    val featureOpen
        get() = route.screen == "FEATURE"

    val workplacesOpen
        get() = route.screen == "WORKPLACES"

    val feature
        get() = route.feature

    val owner
        get() = route.owner

    val isRoot
        get() = route.screen == "ROOT"

    val canBack
        get() = !isRoot || tab != 0 || parents.isNotEmpty()

    val stateKey
        get() = "${route.tab}:${route.screen}:${route.feature}:${route.owner.orEmpty()}"

    fun selectTab(tab: Int) {
        parents = emptyList()
        route = NeoRoute(tab.takeIf { it in 0..4 } ?: 0)
    }

    private fun open(next: NeoRoute) {
        if (route == next) return
        parents = (parents + route).takeLast(8)
        route = next
    }

    fun openFeature(id: String, owner: String? = null) =
        open(route.copy(screen = "FEATURE", feature = id, owner = owner))

    val hasParent
        get() = parents.isNotEmpty()

    fun openCalendarManager() = open(NeoRoute(tab = 0))

    fun openWorkplaces() = open(route.copy(screen = "WORKPLACES", owner = null))

    fun back() {
        if (parents.isNotEmpty()) {
            route = parents.last()
            parents = parents.dropLast(1)
        } else selectTab(0)
    }

    companion object {
        private fun encode(r: NeoRoute) =
            JSONObject()
                .put("tab", r.tab)
                .put("screen", r.screen)
                .put("feature", r.feature)
                .put("owner", r.owner ?: JSONObject.NULL)

        private fun decode(o: JSONObject) =
            NeoRoute(
                o.getInt("tab").takeIf { it in 0..4 } ?: 0,
                o.getString("screen").takeIf { it in listOf("ROOT", "FEATURE", "WORKPLACES") }
                    ?: "ROOT",
                o.getString("feature"),
                o.optString("owner").takeUnless { it.isEmpty() || it == "null" },
            )

        val saver =
            Saver<NeoNavigator, String>(
                save = {
                    JSONObject()
                        .put("route", encode(it.route))
                        .put(
                            "parents",
                            JSONArray().apply { it.parents.forEach { put(encode(it)) } },
                        )
                        .toString()
                },
                restore = { raw ->
                    runCatching {
                            val o = JSONObject(raw)
                            NeoNavigator().apply {
                                route = decode(o.getJSONObject("route"))
                                val p = o.getJSONArray("parents")
                                parents =
                                    (0 until minOf(p.length(), 8)).map {
                                        decode(p.getJSONObject(it))
                                    }
                            }
                        }
                        .getOrDefault(NeoNavigator())
                },
            )
    }
}

class NeoLeaveGate {
    private val interceptors = linkedMapOf<Any, ((() -> Unit) -> Unit)>()

    fun register(token: Any, action: ((() -> Unit) -> Unit)) {
        interceptors[token] = action
    }

    fun remove(token: Any) {
        interceptors.remove(token)
    }

    fun request(action: () -> Unit) {
        interceptors.values.lastOrNull()?.invoke(action) ?: action()
    }
}

val LocalNeoLeaveGate = staticCompositionLocalOf<NeoLeaveGate?> { null }

@Composable
fun neoRegisterLeaveGuard(guard: ((() -> Unit) -> Unit)) {
    val gate = LocalNeoLeaveGate.current
    val current by rememberUpdatedState(guard)
    val token = remember { Any() }
    DisposableEffect(gate) {
        gate?.register(token) { current(it) }
        onDispose { gate?.remove(token) }
    }
}

val LocalNeoAppearanceCommit =
    staticCompositionLocalOf<((com.todoshift.app.v11.NeoAppearance, () -> Unit) -> Unit)?> { null }

data class NeoEntry(
    val token: Long,
    val tab: Int,
    val date: LocalDate? = null,
    val taskAction: String? = null,
    val taskId: String? = null,
) {
    companion object {
        fun parse(intent: Intent, token: Long): NeoEntry {
            val action =
                intent.getStringExtra("todo_shift_task_action")?.takeIf {
                    it in listOf("add", "open")
                }
            val date =
                intent.getStringExtra("todo_shift_open_date")?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()
                }
            val tab =
                if (action != null) 4
                else if (intent.action == "com.todoshift.app.OPEN_FINANCE") 2
                else intent.getIntExtra("neo4_open_tab", 0).takeIf { it in 0..4 } ?: 0
            return NeoEntry(
                token,
                tab,
                date,
                action,
                intent.getStringExtra("todo_shift_task_open_id")?.take(200),
            )
        }
    }
}
