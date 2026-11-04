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
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.background(
                Brush.horizontalGradient(listOf(colors.primaryContainer, colors.surface))
            ).padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(color = colors.primary.copy(alpha = 0.65f), thickness = 2.dp,
                modifier = Modifier.width(52.dp))
        }
    }
}
