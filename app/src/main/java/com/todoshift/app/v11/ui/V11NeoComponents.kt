package com.todoshift.app.v11.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val LocalV11NeoEnabled = compositionLocalOf { false }

/** Decorative premium layer only: no persistence, finance or scheduling changes. */
@Composable
fun V11NeoSectionHero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    NeoPremiumBanner(title, if (LocalNeoAppearance.current.showDescriptions) subtitle else "", modifier.padding(vertical = 8.dp))
}
