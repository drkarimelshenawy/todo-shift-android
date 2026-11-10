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
    private fun nodes(actions: Boolean = false): List<SemanticsNode> {
        val all = mutableListOf<SemanticsNode>()
        fun visit(n: SemanticsNode) {
            all.add(n)
            n.children.forEach(::visit)
        }
        val resumed =
            androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                .toSet()
        fun activity(context: android.content.Context): android.app.Activity? =
            when (context) {
                is android.app.Activity -> context
                is android.content.ContextWrapper -> activity(context.baseContext)
                else -> null
            }
        fun view(v: View) {
            val host = activity(v.context)
            if (resumed.isNotEmpty() && host != null && host !in resumed) return
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
        val modal =
            org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull {
                it.isShowing && (resumed.isEmpty() || activity(it.context) in resumed)
            }
        if (actions && modal != null) view(modal.window!!.decorView)
        else ReflectionHelpers.getField<List<View>>(global, "mViews").forEach(::view)
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

    fun selected(text: String): Boolean {
        var result = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result =
                nodes().any {
                    matches(it, text) && it.config.getOrNull(SemanticsProperties.Selected) == true
                }
        }
        return result
    }

    fun await(text: String) = until { has(text) }

    fun until(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 15_000_000_000L
        while (!condition()) {
            if (System.nanoTime() >= deadline) {
                var dump = ""
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    dump =
                        nodes().joinToString(" | ") {
                            it.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString {
                                it.text
                            } + ":selected=" + it.config.getOrNull(SemanticsProperties.Selected)
                        }
                }
                error("Activity condition timed out: $dump")
            }
            frame()
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32))
            Thread.sleep(10)
        }
    }

    fun click(text: String) {
        await(text)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val n =
                nodes(actions = true).lastOrNull {
                    matches(it, text) &&
                        it.config.getOrNull(SemanticsActions.OnClick)?.action != null
                } ?: error("No clickable Activity node: $text")
            check(n.config[SemanticsActions.OnClick].action!!.invoke())
        }
        frame()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(64))
        frame()
    }

    fun input(label: String, raw: String) {
        await(label)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val n =
                nodes(actions = true).lastOrNull {
                    matches(it, label) &&
                        it.config.getOrNull(SemanticsActions.SetText)?.action != null
                } ?: error("No editable Activity node: $label")
            check(n.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(raw)))
        }
        frame()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(64))
        frame()
    }
}
