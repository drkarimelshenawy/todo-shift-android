package com.todoshift.app.v11.ui

import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.app.v11.*

val LocalNeoAppearance = staticCompositionLocalOf { NeoAppearance() }
val LocalNeoOnChange = staticCompositionLocalOf<(NeoAppearance) -> Unit> { {} }
val LocalNeoEnglish = staticCompositionLocalOf { false }

@Composable fun neoLabel(ar: String, en: String): String = if (LocalNeoEnglish.current) en else ar

fun neoAccent(palette: NeoPalette): Color = Color(neo4AccentArgb(palette))

/** Bounded contrast against neutral surfaces, also for user-defined black/white accents. */
fun neoContrastAccent(raw: Color, dark: Boolean): Color {
    var color = raw
    repeat(24) {
        if (dark && color.luminance() < 0.32f) color = lerp(color, Color.White, 0.08f)
        else if (!dark && color.luminance() > 0.13f) color = lerp(color, Color.Black, 0.08f)
    }
    return color
}

fun neoColorScheme(p: NeoAppearance, systemDark: Boolean): ColorScheme {
    val dark =
        when (p.brightness) {
            NeoBrightness.SYSTEM -> systemDark
            NeoBrightness.LIGHT -> false
            else -> true
        }
    val raw =
        runCatching { Color(android.graphics.Color.parseColor(p.accentHex)) }
            .getOrElse { if (p.palette in listOf(NeoPalette.OCEAN_DEPTH, NeoPalette.CASHBOOK_BLUE)) NeoPremium.Navy else neoAccent(p.palette) }
    // Bound custom accent contrast against both backgrounds; on-accent is computed separately.
    val accent = neoContrastAccent(raw, dark)
    val bg =
        when {
            !dark -> Color(0xFFE4EAF3)
            p.brightness == NeoBrightness.AMOLED -> Color.Black
            p.palette == NeoPalette.EMERALD_LUXURY -> Color(0xFF071310)
            p.palette == NeoPalette.SUNSET_COPPER -> Color(0xFF12100F)
            p.palette == NeoPalette.ROYAL_VIOLET -> Color(0xFF0E0A17)
            p.palette in listOf(NeoPalette.MONOCHROME, NeoPalette.TITANIUM_SILVER) ->
                Color(0xFF101216)
            else -> Color(0xFF08233D)
        }
    val surface = if (dark) lerp(bg, Color.White, 0.045f) else Color(0xFFF4F6FB)
    val variant = lerp(surface, accent, if (dark) 0.10f else 0.05f)
    val container = lerp(surface, accent, 0.12f + p.intensity * 0.12f)
    val ink = if (dark) Color(0xFFF2F5FC) else Color(0xFF08233D)
    val muted = if (dark) Color(0xFFB8C4D6) else Color(0xFF526779)
    val onAccent = if (accent.luminance() > 0.18f) Color(0xFF07101A) else Color.White
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = container,
        onPrimaryContainer = ink,
        secondary = NeoPremium.Gold,
        onSecondary = NeoPremium.Navy,
        secondaryContainer = variant,
        onSecondaryContainer = ink,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = container,
        onTertiaryContainer = ink,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = variant,
        onSurfaceVariant = muted,
        surfaceTint = accent,
        surfaceDim = bg,
        surfaceBright = variant,
        surfaceContainerLowest = bg,
        surfaceContainerLow = surface,
        surfaceContainer = lerp(surface, accent, 0.025f),
        surfaceContainerHigh = variant,
        surfaceContainerHighest = lerp(variant, accent, 0.025f),
        outline = if (dark) Color(0xFF59687D) else Color(0xFF8A9BA9),
        outlineVariant = lerp(surface, ink, if (dark) 0.18f else 0.09f),
        error = if (dark) Color(0xFFFFABB1) else Color(0xFFAA2039),
        onError = if (dark) Color(0xFF450015) else Color.White,
        errorContainer = if (dark) Color(0xFF451724) else Color(0xFFFFDCE2),
        onErrorContainer = if (dark) Color(0xFFFFDCE2) else Color(0xFF451724),
    )
}

@Composable
fun neoTypography(p: NeoAppearance): Typography {
    val en = LocalNeoEnglish.current
    // Android platform families: licensed by the OS, no remote fonts or bundled unlicensed assets.
    val body =
        androidx.compose.runtime.remember(en, p.typeface) {
            if (en && p.typeface == NeoTypeface.GEOMETRIC)
                FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
            else FontFamily.SansSerif
        }
    val display = if (en && p.typeface == NeoTypeface.EDITORIAL) FontFamily.Serif else body
    fun style(
        size: Int,
        height: Int,
        weight: FontWeight = FontWeight.Normal,
        heading: Boolean = false,
    ) =
        TextStyle(
            fontFamily = if (heading) display else body,
            fontWeight = weight,
            fontSize = size.sp,
            lineHeight = height.sp,
        )
    return Typography(
        displayLarge = style(48, 58, FontWeight.Bold, true),
        displayMedium = style(40, 50, FontWeight.Bold, true),
        displaySmall = style(34, 44, FontWeight.Bold, true),
        headlineLarge = style(30, 40, FontWeight.Bold, true),
        headlineMedium = style(26, 36, FontWeight.Bold, true),
        headlineSmall = style(23, 32, FontWeight.Bold, true),
        titleLarge = style(20, 30, FontWeight.SemiBold, true),
        titleMedium = style(16, 26, FontWeight.SemiBold),
        titleSmall = style(14, 24, FontWeight.SemiBold),
        bodyLarge = style(16, 27),
        bodyMedium = style(14, 24),
        bodySmall = style(12, 21),
        labelLarge = style(14, 23, FontWeight.Medium),
        labelMedium = style(12, 21, FontWeight.Medium),
        labelSmall = style(11, 19, FontWeight.Medium),
    )
}

@Composable
fun neoSpacing() =
    when (LocalNeoAppearance.current.density) {
        NeoDensity.COMPACT -> 8.dp
        NeoDensity.BALANCED -> 12.dp
        NeoDensity.COMFORTABLE -> 16.dp
    }

@Composable
private fun neoCardShape() = RoundedCornerShape(LocalNeoAppearance.current.cornerRadius.dp)

/** Package-scoped Card adapters keep all legacy call sites and Classic behavior intact. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (LocalV11NeoEnabled.current) LocalNeoAppearance.current.cardOpacity else 1f)),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!LocalV11NeoEnabled.current) {
        androidx.compose.material3.Card(modifier, shape, colors, elevation, border, content)
    } else {
        val p = LocalNeoAppearance.current
        val c = MaterialTheme.colorScheme
        androidx.compose.material3.Card(
            modifier = modifier,
            shape = neoCardShape(),
            colors = colors,
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        if (
                            p.showEffects &&
                                !LocalNeoRuntimeReduced.current &&
                                p.glow &&
                                p.cardStyle == NeoCardStyle.LAYERED
                        )
                            3.dp
                        else 1.dp
                ),
            border = border ?:
                if (p.cardStyle == NeoCardStyle.FLAT)
                    BorderStroke(0.5.dp, c.outlineVariant.copy(alpha = 0.5f))
                else
                    BorderStroke(
                        1.dp,
                        if (p.showEffects && !LocalNeoRuntimeReduced.current && p.glow)
                            c.primary.copy(alpha = 0.18f + 0.12f * p.intensity)
                        else c.outlineVariant,
                    ),
        ) {
            Column(
                Modifier.then(
                    if (p.showEffects && p.glass && p.cardStyle != NeoCardStyle.FLAT)
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    c.primary.copy(alpha = p.glassStrength * 0.2f),
                                    Color.Transparent,
                                )
                            )
                        )
                    else Modifier
                ),
                content = content,
            )
        }
    }
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (LocalV11NeoEnabled.current) LocalNeoAppearance.current.cardOpacity else 1f)),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val neo = LocalV11NeoEnabled.current
    val p = LocalNeoAppearance.current
    val source =
        interactionSource
            ?: remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by
        animateFloatAsState(
            if (neo && pressed && p.motionMillis > 0) 0.985f else 1f,
            animationSpec = tween(if (neo) p.motionMillis else 0),
            label = "neoCardPress",
        )
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier =
            if (neo)
                modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            else modifier,
        enabled = enabled,
        shape = if (neo) neoCardShape() else shape,
        colors = colors,
        elevation =
            if (neo)
                CardDefaults.cardElevation(
                    defaultElevation =
                        if (p.showEffects && !LocalNeoRuntimeReduced.current && p.glow) 1.dp
                        else 0.dp,
                    pressedElevation = if (p.reduceMotion) 0.dp else 1.dp,
                )
            else elevation,
        border = border ?: if (neo) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        interactionSource = if (neo) source else interactionSource,
        content = content,
    )
}

@Composable
fun NeoEmptyState(title: String, detail: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(neoSpacing()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (!LocalV11NeoEnabled.current || LocalNeoAppearance.current.showDescriptions)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }
}

@Composable
fun NeoEnvironment(
    appearance: NeoAppearance,
    enabled: Boolean,
    onChange: (NeoAppearance) -> Unit,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val systemEnglish = configuration.locales[0]?.language != "ar"
    val english =
        when (appearance.language) {
            NeoLanguage.SYSTEM -> systemEnglish
            NeoLanguage.ENGLISH -> true
            NeoLanguage.ARABIC -> false
        }
    val inheritedDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val direction = neoDirection(appearance.uiDirection, inheritedDirection)
    // This UI-only bridge has no Compose state; the scoped language local invalidates content.
    NeoLegacyLocale.english = english
    NeoLegacyLocale.premium = enabled
    val activityManager =
        LocalContext.current.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
    val runtimeReduced =
        appearance.performance == NeoPerformance.LIGHTWEIGHT ||
            (appearance.performance == NeoPerformance.AUTO && activityManager.isLowRamDevice)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity =
        if (enabled)
            androidx.compose.ui.unit.Density(
                density.density,
                density.fontScale * appearance.fontScale,
            )
        else density
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides scaledDensity,
        LocalNeoAppearance provides appearance,
        LocalNeoRuntimeReduced provides runtimeReduced,
        LocalNeoOnChange provides onChange,
        LocalNeoEnglish provides english,
        androidx.compose.ui.platform.LocalLayoutDirection provides direction,
    ) {
        content()
    }
}

