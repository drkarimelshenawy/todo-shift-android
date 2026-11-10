package com.todoshift.app.v11.ui

import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.test.platform.app.InstrumentationRegistry
import org.robolectric.Shadows
import org.robolectric.util.ReflectionHelpers

/**
 * Direct semantics driver for actual Activity windows, including Compose dialogs. Avoids Espresso's
 * Robolectric modal-window idling loop; no production replacement/content stub.
 */
class ActivitySemantics(private val frame: () -> Unit) {
    private fun nodes(): List<SemanticsNode> {
        val all = mutableListOf<SemanticsNode>()
        fun visit(n: SemanticsNode) {
            all.add(n)
            n.children.forEach(::visit)
        }
        fun view(v: View) {
            if (v.javaClass.simpleName == "AndroidComposeView") {
                val owner = v.javaClass.getMethod("getSemanticsOwner").invoke(v) as SemanticsOwner
                visit(owner.rootSemanticsNode)
            } else if (v is ViewGroup) for (i in 0 until v.childCount) view(v.getChildAt(i))
        }
        val global =
            ReflectionHelpers.callStaticMethod<Any>(
                Class.forName("android.view.WindowManagerGlobal"),
                "getInstance",
            )
        ReflectionHelpers.getField<List<View>>(global, "mViews").forEach(::view)
        return all
    }

    private fun matches(n: SemanticsNode, text: String) =
        n.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == text } ||
            n.config.getOrNull(SemanticsProperties.EditableText)?.text == text ||
            text in n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

    fun has(text: String): Boolean {
        var found = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            found = nodes().any { matches(it, text) }
        }
        return found
    }

    fun await(text: String) = until { has(text) }

    fun until(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Activity condition timed out" }
            frame()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32))
            Thread.sleep(10)
        }
    }

    fun click(text: String) {
        await(text)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val n =
                nodes().lastOrNull {
                    matches(it, text) &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action != null
                } ?: error("No clickable Activity node: $text")
            check(n.config[SemanticsActions.OnClick].action!!.invoke())
        }
        frame()
    }

    fun input(label: String, raw: String) {
        await(label)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val n =
                nodes().lastOrNull {
                    matches(it, label) &&
                        it.config.getOrNull(SemanticsActions.SetText)?.action != null
                } ?: error("No editable Activity node: $label")
            check(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(raw)))
        }
        frame()
    }
}
