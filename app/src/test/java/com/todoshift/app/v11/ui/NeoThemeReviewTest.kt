package com.todoshift.app.v11.ui

import androidx.compose.ui.graphics.Color
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

    @Test fun jadeSurvivesPreferencesAndPortableProfileRoundTrip() {
        val store = NeoAppearanceStore(RuntimeEnvironment.getApplication())
        val selected = NeoAppearance(palette = NeoPalette.NILE_JADE, language = NeoLanguage.ARABIC,
            brightness = NeoBrightness.AMOLED, background = NeoBackground.GRADIENT, gridDirection = NeoDirection.LTR)
        store.save(selected)
        assertEquals(selected, store.load())
        assertEquals(selected, Neo4AppearanceCodec.decode(Neo4AppearanceCodec.encode(selected)))
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
