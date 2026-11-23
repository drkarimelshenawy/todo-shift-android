package com.todoshift.app.v11.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.todoshift.app.widget.Neo4WidgetKind
import com.todoshift.app.widget.Neo4WidgetSettings
import androidx.compose.ui.graphics.luminance
import com.todoshift.app.v11.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NeoThemeReviewTest {
    private fun contrast(a: Color, b: Color): Float =
        (maxOf(a.luminance(), b.luminance()) + .05f) / (minOf(a.luminance(), b.luminance()) + .05f)

    @Test fun themeTextHasContrastInEveryPaletteAndBrightnessMode() {
        for (palette in NeoPalette.entries) for (mode in NeoBrightness.entries) {
            for (night in listOf(false, true)) for (accent in listOf("", "#FFFFFF", "#000000")) {
                val theme = neoColorScheme(NeoAppearance(palette = palette, brightness = mode, accentHex = accent), night)
                val pairs = listOf(
                    theme.onPrimary to theme.primary, theme.onSecondary to theme.secondary,
                    theme.primary to theme.surface, theme.secondary to theme.surface,
                    theme.onPrimaryContainer to theme.primaryContainer,
                    theme.onSecondaryContainer to theme.secondaryContainer,
                    theme.onSurface to theme.surface, theme.onSurfaceVariant to theme.surfaceVariant,
                    theme.onBackground to theme.background, theme.onError to theme.error
                )
                pairs.forEach { (fg, bg) ->
                    assertTrue("$palette/$mode/$night/$accent: $fg on $bg", contrast(fg, bg) >= 4.5f)
                }
                if (mode == NeoBrightness.AMOLED) assertEquals(Color.Black, theme.background)
            }
        }
    }

    @Test fun amoledRetainsBlackCanvasButReadableOpaquePanelsAcrossPalettes() {
        for (palette in NeoPalette.entries) for (accent in listOf("", "#000000", "#FFFFFF")) {
            val p = NeoAppearance(
                palette = palette,
                brightness = NeoBrightness.AMOLED,
                accentHex = accent,
            )
            val scheme = neoColorScheme(p, systemDark = false)
            assertEquals(Color.Black, scheme.background)
            assertEquals(Color.Black, scheme.surfaceDim)
            assertTrue("$palette/$accent card should differ from black",
                contrast(scheme.surface, scheme.background) >= 1.20f)
            assertTrue("$palette/$accent variant must be distinguishable",
                contrast(scheme.surfaceVariant, scheme.surface) >= 1.20f)
            assertTrue(contrast(scheme.onSurface, scheme.surface) >= 7.0f)
            assertTrue(contrast(scheme.onSurfaceVariant, scheme.surfaceVariant) >= 4.5f)
            assertTrue(contrast(scheme.onPrimaryContainer, scheme.primaryContainer) >= 4.5f)
            assertTrue(contrast(scheme.onSecondaryContainer, scheme.secondaryContainer) >= 4.5f)
        }
    }

    @Test fun jadeSurvivesPreferencesAndPortableProfileRoundTrip() {
        val store = NeoAppearanceStore(RuntimeEnvironment.getApplication())
        val selected = NeoAppearance(palette = NeoPalette.NILE_JADE, language = NeoLanguage.ARABIC,
            brightness = NeoBrightness.AMOLED, background = NeoBackground.GRADIENT, gridDirection = NeoDirection.LTR)
        store.save(selected)
        assertEquals(selected, store.load())
        assertEquals(selected, Neo4AppearanceCodec.decode(Neo4AppearanceCodec.encode(selected)))
        val skin = Neo4WidgetSettings(RuntimeEnvironment.getApplication()).skin(Neo4WidgetKind.CALENDAR)
        val scheme = neoColorScheme(selected, false)
        assertEquals(scheme.surface.toArgb(), skin.panelColor)
        assertEquals(scheme.primary.toArgb(), skin.accent)
        for (term in listOf("يشم النيل", "Nile Jade")) {
            assertTrue(NeoSettingsSearch.search(term).any { it.destination == "APPEARANCE" })
        }
    }

    @Test fun customNoteAndShiftFillsKeepReadableInk() {
        for (channel in 0..255) {
            val fill = Color(channel, channel, channel)
            assertTrue(contrast(neoInkOn(fill), fill) >= 4.5f)
        }
    }
}
