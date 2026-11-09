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
