package com.todoshift.app.v11.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.hypot

@Composable
fun V11PatternInput(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPatternComplete:
        (List<Int>) -> Unit
) {
    var path by remember {
        mutableStateOf<List<Int>>(
            emptyList()
        )
    }

    var pointer by remember {
        mutableStateOf<Offset?>(null)
    }

    val primary =
        MaterialTheme.colorScheme.primary

    val idle =
        MaterialTheme.colorScheme
            .outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) {
                    return@pointerInput
                }

                fun center(
                    index: Int
                ): Offset {
                    val row =
                        index / 3

                    val col =
                        index % 3

                    val stepX =
                        size.width / 4f

                    val stepY =
                        size.height / 4f

                    return Offset(
                        x =
                            stepX *
                                (col + 1),
                        y =
                            stepY *
                                (row + 1)
                    )
                }

                fun nodeAt(
                    position: Offset
                ): Int? {
                    val radius =
                        minOf(
                            size.width,
                            size.height
                        ) / 8f

                    var best:
                        Pair<Int, Float>? =
                        null

                    for (index in 0..8) {
                        val c =
                            center(index)

                        val distance =
                            hypot(
                                position.x -
                                    c.x,
                                position.y -
                                    c.y
                            )

                        if (
                            distance <= radius &&
                            (
                                best == null ||
                                    distance <
                                    best!!.second
                                )
                        ) {
                            best =
                                index to distance
                        }
                    }

                    return best?.first
                }

                fun middleNode(
                    from: Int,
                    to: Int
                ): Int? {
                    val aRow = from / 3
                    val aCol = from % 3
                    val bRow = to / 3
                    val bCol = to % 3

                    val rowDiff =
                        abs(aRow - bRow)

                    val colDiff =
                        abs(aCol - bCol)

                    val hasMiddle =
                        (
                            rowDiff == 2 &&
                                colDiff == 0
                            ) ||
                            (
                                rowDiff == 0 &&
                                    colDiff == 2
                                ) ||
                            (
                                rowDiff == 2 &&
                                    colDiff == 2
                                )

                    if (!hasMiddle) {
                        return null
                    }

                    val middleRow =
                        (aRow + bRow) / 2

                    val middleCol =
                        (aCol + bCol) / 2

                    return (
                        middleRow * 3 +
                            middleCol
                        )
                }

                fun addNode(
                    node: Int
                ) {
                    if (node in path) {
                        return
                    }

                    val previous =
                        path.lastOrNull()

                    if (previous != null) {
                        val middle =
                            middleNode(
                                previous,
                                node
                            )

                        if (
                            middle != null &&
                            middle !in path
                        ) {
                            path =
                                path + middle
                        }
                    }

                    path =
                        path + node
                }

                detectDragGestures(
                    onDragStart = {
                            position ->

                        path =
                            emptyList()

                        pointer =
                            position

                        nodeAt(position)
                            ?.let {
                                addNode(it)
                            }
                    },

                    onDrag = {
                            change,
                            _ ->

                        pointer =
                            change.position

                        nodeAt(
                            change.position
                        )?.let {
                            addNode(it)
                        }
                    },

                    onDragEnd = {
                        val completed =
                            path

                        pointer = null

                        if (
                            completed.size >= 4
                        ) {
                            onPatternComplete(
                                completed
                            )
                        }
                    },

                    onDragCancel = {
                        pointer = null
                    }
                )
            }
    ) {
        fun center(
            index: Int
        ): Offset {
            val row =
                index / 3

            val col =
                index % 3

            return Offset(
                x =
                    size.width /
                        4f *
                        (col + 1),

                y =
                    size.height /
                        4f *
                        (row + 1)
            )
        }

        if (path.size >= 2) {
            for (
                index in
                0 until
                    path.lastIndex
            ) {
                drawLine(
                    color = primary,
                    start =
                        center(
                            path[index]
                        ),
                    end =
                        center(
                            path[index + 1]
                        ),
                    strokeWidth = 8f
                )
            }
        }

        val last =
            path.lastOrNull()

        val currentPointer =
            pointer

        if (
            last != null &&
            currentPointer != null
        ) {
            drawLine(
                color =
                    primary.copy(
                        alpha = 0.55f
                    ),
                start =
                    center(last),
                end =
                    currentPointer,
                strokeWidth = 6f
            )
        }

        for (index in 0..8) {
            val selected =
                index in path

            drawCircle(
                color =
                    if (selected)
                        primary
                    else
                        idle,
                radius =
                    if (selected)
                        18f
                    else
                        14f,
                center =
                    center(index)
            )

            if (selected) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center =
                        center(index)
                )
            }
        }
    }
}
