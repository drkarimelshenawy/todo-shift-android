package com.todoshift.app.v11.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.V11AppTheme
import com.todoshift.app.v11.V11Experience
import com.todoshift.app.v11.V11PremiumBrightness

@Composable
fun V11AppThemeProvider(
    theme: V11AppTheme,
    experience: V11Experience = V11Experience.CLASSIC,
    premiumBrightness: V11PremiumBrightness = V11PremiumBrightness.SYSTEM,
    content: @Composable () -> Unit,
) {
    if (experience == V11Experience.CLASSIC) {
        MaterialTheme(colorScheme = v11ColorScheme(theme), content = content)
    } else {
        val p = LocalNeoAppearance.current
        val colors = neoColorScheme(p, isSystemInDarkTheme())
        MaterialTheme(
            colorScheme = colors,
            typography = neoTypography(p),
            shapes =
                androidx.compose.material3.Shapes(
                    small =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            (p.cornerRadius / 2).dp
                        ),
                    medium =
                        androidx.compose.foundation.shape.RoundedCornerShape(p.cornerRadius.dp),
                    large = androidx.compose.foundation.shape.RoundedCornerShape(p.cornerRadius.dp),
                    extraLarge =
                        androidx.compose.foundation.shape.RoundedCornerShape(
                            (p.cornerRadius + 4).dp
                        ),
                ),
            content = {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides colors.onSurface
                ) {
                    content()
                }
            },
        )
    }
}

fun v11ColorScheme(theme: V11AppTheme): ColorScheme =
    when (theme) {
        V11AppTheme.TODO_SHIFT ->
            lightColorScheme(
                primary = Color(0xFF213555),
                secondary = Color(0xFF4F709C),
                background = Color(0xFFF7F8FA),
                surface = Color.White,
                surfaceVariant = Color(0xFFF1F3F8),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF172033),
                onSurface = Color(0xFF172033),
                onSurfaceVariant = Color(0xFF64748B),
                outline = Color(0xFFD7DCE5),
            )

        V11AppTheme.MIDNIGHT ->
            darkColorScheme(
                primary = Color(0xFF9BB7E0),
                secondary = Color(0xFF819CC5),
                background = Color(0xFF0D1117),
                surface = Color(0xFF161B22),
                surfaceVariant = Color(0xFF202733),
                onPrimary = Color(0xFF08111F),
                onSecondary = Color(0xFF08111F),
                onBackground = Color(0xFFE6EDF3),
                onSurface = Color(0xFFE6EDF3),
                onSurfaceVariant = Color(0xFFAAB7C4),
                outline = Color(0xFF394451),
            )

        V11AppTheme.OCEAN ->
            lightColorScheme(
                primary = Color(0xFF075985),
                secondary = Color(0xFF0284C7),
                background = Color(0xFFF0F9FF),
                surface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFFE0F2FE),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF082F49),
                onSurface = Color(0xFF082F49),
                onSurfaceVariant = Color(0xFF476678),
                outline = Color(0xFFBAE6FD),
            )

        V11AppTheme.FOREST ->
            lightColorScheme(
                primary = Color(0xFF285943),
                secondary = Color(0xFF4D7C62),
                background = Color(0xFFF4F8F4),
                surface = Color.White,
                surfaceVariant = Color(0xFFE5EFE8),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF172D23),
                onSurface = Color(0xFF172D23),
                onSurfaceVariant = Color(0xFF5C7065),
                outline = Color(0xFFC9D8CE),
            )

        V11AppTheme.SAND ->
            lightColorScheme(
                primary = Color(0xFF70543B),
                secondary = Color(0xFF987454),
                background = Color(0xFFFAF6EF),
                surface = Color(0xFFFFFCF7),
                surfaceVariant = Color(0xFFF1E7D8),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF382B21),
                onSurface = Color(0xFF382B21),
                onSurfaceVariant = Color(0xFF766454),
                outline = Color(0xFFDDCDBA),
            )

        V11AppTheme.MONO ->
            lightColorScheme(
                primary = Color(0xFF181818),
                secondary = Color(0xFF555555),
                background = Color(0xFFF5F5F5),
                surface = Color.White,
                surfaceVariant = Color(0xFFEAEAEA),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground = Color(0xFF111111),
                onSurface = Color(0xFF111111),
                onSurfaceVariant = Color(0xFF666666),
                outline = Color(0xFFD0D0D0),
            )
    }

/**
 * Neo Luxury visual tokens; classic color schemes remain unchanged. Existing appTheme preference
 * selects a Neo accent without database migrations.
 */
fun v11PremiumColorScheme(dark: Boolean, theme: V11AppTheme = V11AppTheme.TODO_SHIFT): ColorScheme {
    val accent =
        when (theme) {
            V11AppTheme.TODO_SHIFT -> Color(0xFF36E4EF)
            V11AppTheme.MIDNIGHT -> Color(0xFFA78BFA)
            V11AppTheme.OCEAN -> Color(0xFF5C9DFF)
            V11AppTheme.FOREST -> Color(0xFF69F0BD)
            V11AppTheme.SAND -> Color(0xFFE6BD83)
            V11AppTheme.MONO -> Color(0xFFCFD8E3)
        }
    val lightAccent =
        when (theme) {
            V11AppTheme.TODO_SHIFT -> Color(0xFF087CAA)
            V11AppTheme.MIDNIGHT -> Color(0xFF7053C4)
            V11AppTheme.OCEAN -> Color(0xFF285BC4)
            V11AppTheme.FOREST -> Color(0xFF087E68)
            V11AppTheme.SAND -> Color(0xFF79532B)
            V11AppTheme.MONO -> Color(0xFF344054)
        }
    return if (dark)
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF08111B),
            primaryContainer = Color(0xFF1C3042),
            onPrimaryContainer = Color(0xFFE8F5FF),
            secondary = Color(0xFFA78BFA),
            onSecondary = Color(0xFF101124),
            background = Color(0xFF080B16),
            onBackground = Color(0xFFF5F7FF),
            surface = Color(0xFF11182B),
            onSurface = Color(0xFFF5F7FF),
            surfaceVariant = Color(0xFF1B2740),
            onSurfaceVariant = Color(0xFFB0BED3),
            surfaceTint = accent,
            outline = Color(0xFF34435F),
            outlineVariant = Color(0xFF26324B),
            error = Color(0xFFFF8F9C),
        )
    else
        lightColorScheme(
            primary = lightAccent,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDCEFF8),
            onPrimaryContainer = Color(0xFF15283A),
            secondary = Color(0xFF7053C4),
            onSecondary = Color.White,
            background = Color(0xFFF4F7FC),
            onBackground = Color(0xFF17243D),
            surface = Color.White,
            onSurface = Color(0xFF17243D),
            surfaceVariant = Color(0xFFE7EDF7),
            onSurfaceVariant = Color(0xFF53657F),
            surfaceTint = lightAccent,
            outline = Color(0xFFCCD8E8),
            outlineVariant = Color(0xFFDEE6F1),
            error = Color(0xFFB32637),
        )
}
