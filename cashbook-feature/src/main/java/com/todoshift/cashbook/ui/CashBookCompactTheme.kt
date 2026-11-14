package com.todoshift.cashbook.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact visuals, scoped to this feature. Keeps Android density/fontScale and touch targets. */
internal object CashBookCompact {
    const val TextScale = 0.85f
    val HeaderHeight = 48.dp
    val RowVerticalPadding = 9.dp
    val RowMetaGap = 12.dp
    val ContentPadding = 10.dp
    val EditorGap = 13.dp
    val SummaryPadding = 13.dp
    val SummaryTitleGap = 15.dp
    val TotalsPadding = 8.dp
}

private fun TextStyle.compact(): TextStyle {
    val size = when {
        fontSize == TextUnit.Unspecified -> fontSize
        fontSize.type == TextUnitType.Sp -> (fontSize.value * CashBookCompact.TextScale).coerceAtLeast(11f).sp
        else -> fontSize * CashBookCompact.TextScale
    }
    return copy(fontSize = size, lineHeight = if (lineHeight == TextUnit.Unspecified) lineHeight else lineHeight * CashBookCompact.TextScale)
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun CashBookCompactTheme(content: @Composable () -> Unit) {
    val host = MaterialTheme.typography
    val compact = remember(host) {
        host.copy(
            displayLarge = host.displayLarge.compact(), displayMedium = host.displayMedium.compact(), displaySmall = host.displaySmall.compact(),
            headlineLarge = host.headlineLarge.compact(), headlineMedium = host.headlineMedium.compact(), headlineSmall = host.headlineSmall.compact(),
            titleLarge = host.titleLarge.compact(), titleMedium = host.titleMedium.compact(), titleSmall = host.titleSmall.compact(),
            bodyLarge = host.bodyLarge.compact(), bodyMedium = host.bodyMedium.compact(), bodySmall = host.bodySmall.compact(),
            labelLarge = host.labelLarge.compact(), labelMedium = host.labelMedium.compact(), labelSmall = host.labelSmall.compact()
        )
    }
    MaterialTheme(typography = compact, content = content)
}
