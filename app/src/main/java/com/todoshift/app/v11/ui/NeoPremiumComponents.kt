package com.todoshift.app.v11.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The shell's identity is independent of the selected accent palette. */
object NeoPremium {
    val Navy = Color(0xFF0B2949)
    val NavyLight = Color(0xFF224E79)
    val Gold = Color(0xFFD8AD55)
    val Cream = Color(0xFFFFFAF1)
    val Green = Color(0xFF237344)
}

@Composable
fun neoPositiveColor(): Color = if (MaterialTheme.colorScheme.surface.luminance() < .3f) Color(0xFF99D9AC) else NeoPremium.Green

@Composable
fun NeoPremiumBanner(title: String, subtitle: String = "", modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = NeoPremium.Navy, contentColor = NeoPremium.Cream,
        border = BorderStroke(1.dp, NeoPremium.Gold), shadowElevation = 3.dp) {
        Column(Modifier.background(Brush.linearGradient(listOf(NeoPremium.NavyLight, NeoPremium.Navy)))
            .padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFEAD8B2))
        }
    }
}

@Composable
fun NeoPremiumTabs(labels: List<String>, selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            FilterChip(selected = selected == index, onClick = { onSelected(index) },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (selected == index) NeoPremium.Gold else MaterialTheme.colorScheme.outlineVariant),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeoPremium.Navy,
                    selectedLabelColor = NeoPremium.Cream, containerColor = MaterialTheme.colorScheme.surface),
                label = { Text(label, fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium) })
        }
    }
}

@Composable
fun NeoPremiumMonth(title: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = NeoPremium.Navy,
        contentColor = NeoPremium.Cream, border = BorderStroke(1.dp, NeoPremium.Gold)) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, neoLabel("الشهر السابق", "Previous month")) }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, neoLabel("الشهر التالي", "Next month")) }
        }
    }
}

@Composable
fun NeoPremiumSettingTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(icon, null, Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
        }
    }
}
