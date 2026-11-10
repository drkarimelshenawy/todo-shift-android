package com.todoshift.app.v11.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.LayoutDirection
import com.todoshift.app.v11.*

fun neoDirection(value: NeoDirection, inherited: LayoutDirection): LayoutDirection =
    when (value) {
        NeoDirection.AUTO -> inherited
        NeoDirection.RTL -> LayoutDirection.Rtl
        NeoDirection.LTR -> LayoutDirection.Ltr
    }

fun neoTransition(p: NeoAppearance, forward: Boolean = true): ContentTransform {
    val d = p.motionMillis
    if (d == 0) return fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    val sign = if (forward) 1 else -1
    return when (p.transition) {
        NeoTransition.FADE -> fadeIn(tween(d)) togetherWith fadeOut(tween(d))
        NeoTransition.SLIDE ->
            (slideInHorizontally(tween(d)) { it * sign } + fadeIn(tween(d))) togetherWith
                (slideOutHorizontally(tween(d)) { -it * sign } + fadeOut(tween(d)))
        NeoTransition.FLIP ->
            (scaleIn(tween(d), initialScale = .05f) + fadeIn(tween(d))) togetherWith
                (scaleOut(tween(d), targetScale = .05f) + fadeOut(tween(d)))
        NeoTransition.ZOOM ->
            (scaleIn(tween(d), initialScale = .8f) + fadeIn(tween(d))) togetherWith
                (scaleOut(tween(d), targetScale = 1.2f) + fadeOut(tween(d)))
        NeoTransition.STACK ->
            (slideInHorizontally(tween(d)) { it / 3 * sign } +
                scaleIn(tween(d), initialScale = .9f)) togetherWith
                (scaleOut(tween(d), targetScale = .9f) + fadeOut(tween(d)))
        NeoTransition.VERTICAL ->
            (slideInVertically(tween(d)) { it * sign } + fadeIn(tween(d))) togetherWith
                (slideOutVertically(tween(d)) { -it * sign } + fadeOut(tween(d)))
    }
}

data class NeoNavigationFrame(
    val alpha: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotationY: Float = 0f,
)

fun neoNavigationFrame(
    p: NeoAppearance,
    progress: Float,
    rtl: Boolean,
    runtimeReduced: Boolean,
): NeoNavigationFrame {
    if (p.motionMillis == 0 || runtimeReduced) return NeoNavigationFrame()
    val t = progress.coerceIn(0f, 1f)
    val left = 1f - t
    val sign = if (rtl) -1f else 1f
    return when (p.transition) {
        NeoTransition.FADE -> NeoNavigationFrame(alpha = t)
        NeoTransition.SLIDE -> NeoNavigationFrame(alpha = .7f + .3f * t, x = left * sign)
        NeoTransition.FLIP ->
            NeoNavigationFrame(alpha = .7f + .3f * t, rotationY = 85f * left * sign)
        NeoTransition.ZOOM -> NeoNavigationFrame(alpha = .7f + .3f * t, scale = .9f + .1f * t)
        NeoTransition.STACK ->
            NeoNavigationFrame(
                alpha = .7f + .3f * t,
                x = .25f * left * sign,
                scale = .95f + .05f * t,
            )
        NeoTransition.VERTICAL -> NeoNavigationFrame(alpha = .7f + .3f * t, y = left)
    }
}
