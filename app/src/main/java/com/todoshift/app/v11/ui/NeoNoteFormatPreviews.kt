package com.todoshift.app.v11.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Presentation only; original list kind IDs and rich note serialization are untouched. */
internal val neoNoteListKinds = listOf(
    "bullet", "circle", "square", "check", "number",
    "numberParen", "letter", "letterParen", "arabic",
)

internal fun neoNoteListPreview(kind: String): Pair<String, String> = when (kind) {
    "bullet" -> "•" to "•"
    "circle" -> "◦" to "◦"
    "square" -> "▪" to "▪"
    "check" -> "☐" to "☑"
    "number" -> "1." to "2."
    "numberParen" -> "1)" to "2)"
    "letter" -> "A." to "B."
    "letterParen" -> "A)" to "B)"
    "arabic" -> "أ." to "ب."
    else -> error("Unknown list kind: $kind")
}

/** Identical style IDs to A4 drawing and Word/PDF export. */
internal val neoNotePageBorderChoices = NeoPageBorders.styles

@Composable
internal fun NeoNoteListStyleTile(
    kind: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    val lineInk = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .48f)
    val markers = neoNoteListPreview(kind)
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 112.dp)
                .padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(markers.first, markers.second).forEachIndexed { i, marker ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            marker,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ink,
                            modifier = Modifier.width(25.dp),
                            textAlign = TextAlign.End,
                            maxLines = 1,
                        )
                        Box(
                            Modifier.width(if (i == 0) 27.dp else 20.dp)
                                .height(2.dp)
                                .background(lineInk, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = ink, maxLines = 2,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp,
            )
            if (selected) Icon(
                Icons.Rounded.CheckCircle, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = ink,
            )
        }
    }
}

@Composable
internal fun NeoNotePageBorderTile(
    style: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = label },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 128.dp)
                .padding(horizontal = 4.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(53.dp, 73.dp)) {
                val unit = 1.dp.toPx()
                val corner = CornerRadius(3f * unit)
                val borderInk = Color(0xFF52657C)
                val paperLine = Color(0xFFD2D8E0)
                drawRoundRect(Color.White, cornerRadius = corner)
                drawRoundRect(paperLine, cornerRadius = corner,
                    style = Stroke(width = .8f * unit))
                // Paper guide lines are not page borders.
                for (i in 0..2) {
                    val y = (25f + i * 9f) * unit
                    drawLine(paperLine, Offset(17f * unit, y),
                        Offset((if (i == 2) 34f else 38f) * unit, y),
                        strokeWidth = 1.5f * unit)
                }
                if (style != NeoPageBorders.NONE) {
                    val fx = when (style) {
                        NeoPageBorders.DASHED -> PathEffect.dashPathEffect(
                            floatArrayOf(6f * unit, 3.5f * unit))
                        NeoPageBorders.DOTTED -> PathEffect.dashPathEffect(
                            floatArrayOf(1.2f * unit, 3f * unit))
                        else -> null
                    }
                    val inset = 6.5f * unit
                    drawRect(
                        borderInk,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2f * inset, size.height - 2f * inset),
                        style = Stroke(width = 1.5f * unit, pathEffect = fx),
                    )
                    if (style == NeoPageBorders.DOUBLE) {
                        val innerInset = 10f * unit
                        drawRect(
                            borderInk,
                            topLeft = Offset(innerInset, innerInset),
                            size = Size(size.width - 2f * innerInset,
                                size.height - 2f * innerInset),
                            style = Stroke(width = .85f * unit),
                        )
                    }
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2, textAlign = TextAlign.Center,
                lineHeight = 15.sp,
            )
            if (selected) Icon(
                Icons.Rounded.CheckCircle, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = primary,
            )
        }
    }
}
