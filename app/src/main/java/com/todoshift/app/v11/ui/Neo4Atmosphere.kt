package com.todoshift.app.v11.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.todoshift.app.R
import com.todoshift.app.v11.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun neo4GradientColors(palette: NeoPalette, dark: Boolean): List<Color> {
    val pair = when (palette) {
        NeoPalette.MINT_AURA -> 0xFFE1F4E9 to 0xFFB8D8E7
        NeoPalette.OCEAN_DEPTH -> 0xFFD7ECF6 to 0xFF9DB9DA
        NeoPalette.LAVENDER_GLOW -> 0xFFEDE4F7 to 0xFFC3CDE8
        NeoPalette.PEACH_BLOOM -> 0xFFFFE9D8 to 0xFFEACADA
        NeoPalette.EMERALD_NIGHT -> 0xFFC7E3D6 to 0xFF81B6AE
        NeoPalette.ARCTIC_BLUE -> 0xFFE8F4FA to 0xFFB2D3E9
        NeoPalette.GOLDEN_HOUR -> 0xFFF6ECCC to 0xFFE6C2A1
        NeoPalette.ROSE_MIST -> 0xFFF7E4EB to 0xFFDCC2DE
        NeoPalette.MIDNIGHT_TEAL -> 0xFFBDDCE0 to 0xFF90B5CE
        NeoPalette.SUNSET_CORAL -> 0xFFFFE3D6 to 0xFFEAB8C7
        NeoPalette.FOREST_SAGE -> 0xFFE3ECDA to 0xFFB7D3C7
        NeoPalette.GRAPHITE_BLUE -> 0xFFE1E8EF to 0xFFAEBFD0
        else -> 0xFFE8EDF6 to 0xFFC8DCE7
    }
    val colors = listOf(Color(pair.first), Color(pair.second))
    return if (dark) colors.map { lerp(Color(0xFF07121D), it, 0.15f) } else colors
}

@Composable
fun Neo4Atmosphere(modifier: Modifier = Modifier, appearance: NeoAppearance = LocalNeoAppearance.current) {
    val c = MaterialTheme.colorScheme
    val dark = c.background.luminance() < 0.3f
    val context = LocalContext.current
    var personal by remember(appearance.customImageFile, appearance.background, appearance.imageTheme) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(appearance.customImageFile, appearance.background, appearance.imageTheme) {
        if (appearance.background == NeoBackground.IMAGE && appearance.imageTheme == NeoImageTheme.PERSONAL && appearance.customImageFile.matches(Regex("neo4-theme-[a-zA-Z0-9-]+[.]jpg"))) {
            personal = withContext(Dispatchers.IO) { runCatching { Neo4ImageStore.load(context.applicationContext, appearance.customImageFile) }.getOrNull() }
        }
    }
    Box(modifier.fillMaxSize().background(c.background)) {
        if (appearance.brightness != NeoBrightness.AMOLED && appearance.background != NeoBackground.CLASSIC) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(neo4GradientColors(appearance.palette, dark))))
            if (appearance.showEffects) Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(c.primary.copy(alpha = if (dark) 0.08f else 0.09f), Color.Transparent))))
            if (appearance.background == NeoBackground.IMAGE) {
                val alignment = object : Alignment {
                    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): androidx.compose.ui.unit.IntOffset =
                        androidx.compose.ui.unit.IntOffset(((space.width - size.width) * appearance.imagePositionX).toInt(), ((space.height - size.height) * appearance.imagePositionY).toInt())
                }
                if (appearance.imageTheme == NeoImageTheme.PERSONAL) {
                    personal?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = alignment) }
                } else {
                    val resource = when (appearance.imageTheme) {
                        NeoImageTheme.SUNSET_BEACH -> R.drawable.neo4_sunset_beach
                        NeoImageTheme.CLOUD_PARADISE -> R.drawable.neo4_cloud_paradise
                        NeoImageTheme.FOREST_MIST -> R.drawable.neo4_forest_mist
                        NeoImageTheme.AURORA_NIGHT -> R.drawable.neo4_aurora_night
                        NeoImageTheme.LAVENDER_SKY -> R.drawable.neo4_lavender_sky
                        else -> R.drawable.neo4_golden_sunset
                    }
                    Image(painterResource(resource), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = alignment)
                }
                Box(Modifier.fillMaxSize().background((if (dark) Color(0xFF07111E) else Color.White).copy(alpha = 0.58f + appearance.imageDim * 0.32f)))
            }
        }
    }
}

@Composable
fun Neo4ShiftFill(segments: List<ShiftSegmentV11>, workplaces: List<com.todoshift.app.data.Workplace>, modifier: Modifier = Modifier,
    appearance: NeoAppearance = LocalNeoAppearance.current) {
    val bands = remember(segments) { neo4PaintBands(segments.map { it.kind }) }
    val brushes = remember(segments, workplaces, appearance.shiftPaints) { segments.map { segment ->
        val style = appearance.shiftPaints[segment.kind] ?: NeoShiftPaint()
        val old = workplaces.firstOrNull { it.id == segment.workplaceId }?.colorHex ?: "#8BBACB"
        val first = neoHexColor(if (style.mode == NeoPaintMode.INHERIT) old else style.first)
        val rawSecond = neoHexColor(style.second)
        val second = lerp(first, rawSecond, when(style.strength) { NeoGradientStrength.SOFT -> 0.35f; NeoGradientStrength.MEDIUM -> 0.7f; NeoGradientStrength.STRONG -> 1f })
        if (style.mode != NeoPaintMode.GRADIENT) Brush.linearGradient(listOf(first, first)) else when (style.direction) {
            NeoGradientDirection.VERTICAL -> Brush.verticalGradient(listOf(first, second))
            NeoGradientDirection.HORIZONTAL -> Brush.horizontalGradient(listOf(first, second))
            NeoGradientDirection.DIAGONAL -> Brush.linearGradient(listOf(first, second))
        }
    } }
    androidx.compose.foundation.Canvas(modifier) {
        bands.forEach { band ->
            val top = band.top * size.height; val bottom = band.bottom * size.height
            if (band.owners.size == 1) drawRect(brushes[band.owners[0]], Offset(0f, top), androidx.compose.ui.geometry.Size(size.width, bottom - top))
            else {
                val stripeHeight = minOf(4f * density, (bottom - top) / band.owners.size).coerceAtLeast(0.001f)
                var y = top; var i = 0
                while (y < bottom) {
                    drawRect(brushes[band.owners[i % band.owners.size]], Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, minOf(stripeHeight, bottom - y)))
                    y += stripeHeight; i++
                }
            }
        }
    }
}
