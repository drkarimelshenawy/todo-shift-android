package com.todoshift.app.v11.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*

@Composable
fun BoxScope.NeoTodayDecoration(today: Boolean, tomorrow: Boolean, p: NeoAppearance) {
    val color = neoHexColor(p.todayColor)
    if (today) {
        val animate = neoTodayPulseEnabled(p, LocalNeoRuntimeReduced.current, LocalNeoFocus.current)
        val alpha =
            if (animate)
                rememberInfiniteTransition(label = "todayPulse")
                    .animateFloat(
                        .25f,
                        .85f,
                        infiniteRepeatable(
                            tween(
                                when (p.todaySpeed) {
                                    NeoSpeed.SLOW -> 1800
                                    NeoSpeed.NORMAL -> 1100
                                    NeoSpeed.FAST -> 700
                                }
                            ),
                            RepeatMode.Reverse,
                        ),
                        label = "pulseAlpha",
                    )
                    .value
            else .65f
        when (p.todayStyle) {
            NeoTodayStyle.NONE -> Unit
            NeoTodayStyle.CIRCLE ->
                Box(
                    Modifier.align(Alignment.TopStart)
                        .padding(1.dp)
                        .size(28.dp)
                        .border(2.dp, color, CircleShape)
                )
            NeoTodayStyle.GLOW,
            NeoTodayStyle.PULSE ->
                Box(
                    Modifier.fillMaxSize()
                        .border(
                            if (p.todayStyle == NeoTodayStyle.GLOW) 3.dp else 2.dp,
                            color.copy(alpha = alpha * p.todayIntensity),
                        )
                )
            NeoTodayStyle.BACKGROUND ->
                Box(
                    Modifier.fillMaxSize()
                        .background(color.copy(alpha = .05f + p.todayIntensity * .2f))
                )
            NeoTodayStyle.UNDERLINE ->
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(color)
                )
            NeoTodayStyle.DOTS -> Text("•••", Modifier.align(Alignment.TopEnd), color = color)
            NeoTodayStyle.LABEL ->
                Text(
                    neoLabel("اليوم", "Today"),
                    Modifier.align(Alignment.TopEnd),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
        }
    } else if (tomorrow && p.showTomorrow)
        Text(
            "+1",
            Modifier.align(Alignment.TopEnd),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
}
