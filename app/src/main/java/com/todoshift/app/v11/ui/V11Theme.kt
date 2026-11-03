package com.todoshift.app.v11.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import com.todoshift.app.v11.V11Experience
import com.todoshift.app.v11.V11PremiumBrightness
import androidx.compose.ui.graphics.Color
import com.todoshift.app.v11.V11AppTheme

@Composable
fun V11AppThemeProvider(
    theme: V11AppTheme,
    experience: V11Experience = V11Experience.CLASSIC,
    premiumBrightness: V11PremiumBrightness = V11PremiumBrightness.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (premiumBrightness) {
        V11PremiumBrightness.SYSTEM -> isSystemInDarkTheme()
        V11PremiumBrightness.LIGHT -> false
        V11PremiumBrightness.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (experience == V11Experience.PREMIUM)
            v11PremiumColorScheme(dark) else v11ColorScheme(theme),
        content = content
    )
}

fun v11ColorScheme(
    theme: V11AppTheme
): ColorScheme =
    when (theme) {
        V11AppTheme.TODO_SHIFT ->
            lightColorScheme(
                primary = Color(0xFF213555),
                secondary = Color(0xFF4F709C),
                background = Color(0xFFF7F8FA),
                surface = Color.White,
                surfaceVariant =
                    Color(0xFFF1F3F8),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onBackground =
                    Color(0xFF172033),
                onSurface =
                    Color(0xFF172033),
                onSurfaceVariant =
                    Color(0xFF64748B),
                outline =
                    Color(0xFFD7DCE5)
            )

        V11AppTheme.MIDNIGHT ->
            darkColorScheme(
                primary = Color(0xFF9BB7E0),
                secondary =
                    Color(0xFF819CC5),
                background =
                    Color(0xFF0D1117),
                surface =
                    Color(0xFF161B22),
                surfaceVariant =
                    Color(0xFF202733),
                onPrimary =
                    Color(0xFF08111F),
                onSecondary =
                    Color(0xFF08111F),
                onBackground =
                    Color(0xFFE6EDF3),
                onSurface =
                    Color(0xFFE6EDF3),
                onSurfaceVariant =
                    Color(0xFFAAB7C4),
                outline =
                    Color(0xFF394451)
            )

        V11AppTheme.OCEAN ->
            lightColorScheme(
                primary =
                    Color(0xFF075985),
                secondary =
                    Color(0xFF0284C7),
                background =
                    Color(0xFFF0F9FF),
                surface =
                    Color(0xFFFFFFFF),
                surfaceVariant =
                    Color(0xFFE0F2FE),
                onPrimary =
                    Color.White,
                onSecondary =
                    Color.White,
                onBackground =
                    Color(0xFF082F49),
                onSurface =
                    Color(0xFF082F49),
                onSurfaceVariant =
                    Color(0xFF476678),
                outline =
                    Color(0xFFBAE6FD)
            )

        V11AppTheme.FOREST ->
            lightColorScheme(
                primary =
                    Color(0xFF285943),
                secondary =
                    Color(0xFF4D7C62),
                background =
                    Color(0xFFF4F8F4),
                surface =
                    Color.White,
                surfaceVariant =
                    Color(0xFFE5EFE8),
                onPrimary =
                    Color.White,
                onSecondary =
                    Color.White,
                onBackground =
                    Color(0xFF172D23),
                onSurface =
                    Color(0xFF172D23),
                onSurfaceVariant =
                    Color(0xFF5C7065),
                outline =
                    Color(0xFFC9D8CE)
            )

        V11AppTheme.SAND ->
            lightColorScheme(
                primary =
                    Color(0xFF70543B),
                secondary =
                    Color(0xFF987454),
                background =
                    Color(0xFFFAF6EF),
                surface =
                    Color(0xFFFFFCF7),
                surfaceVariant =
                    Color(0xFFF1E7D8),
                onPrimary =
                    Color.White,
                onSecondary =
                    Color.White,
                onBackground =
                    Color(0xFF382B21),
                onSurface =
                    Color(0xFF382B21),
                onSurfaceVariant =
                    Color(0xFF766454),
                outline =
                    Color(0xFFDDCDBA)
            )

        V11AppTheme.MONO ->
            lightColorScheme(
                primary =
                    Color(0xFF181818),
                secondary =
                    Color(0xFF555555),
                background =
                    Color(0xFFF5F5F5),
                surface =
                    Color.White,
                surfaceVariant =
                    Color(0xFFEAEAEA),
                onPrimary =
                    Color.White,
                onSecondary =
                    Color.White,
                onBackground =
                    Color(0xFF111111),
                onSurface =
                    Color(0xFF111111),
                onSurfaceVariant =
                    Color(0xFF666666),
                outline =
                    Color(0xFFD0D0D0)
            )
    }


fun v11PremiumColorScheme(dark: Boolean): ColorScheme =
    if (dark) darkColorScheme(
        primary = Color(0xFFD8C49B), secondary = Color(0xFF9BB9B0),
        background = Color(0xFF10151B), surface = Color(0xFF1A222B),
        surfaceVariant = Color(0xFF27313B), onPrimary = Color(0xFF171C21),
        onBackground = Color(0xFFF2F0EB), onSurface = Color(0xFFF2F0EB),
        outline = Color(0xFF52606A)
    ) else lightColorScheme(
        primary = Color(0xFF283E52), secondary = Color(0xFF6D857C),
        background = Color(0xFFF6F5F1), surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFEBEEE9), onPrimary = Color.White,
        onBackground = Color(0xFF172631), onSurface = Color(0xFF172631),
        outline = Color(0xFFD2DAD7)
    )
