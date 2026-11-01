package com.todoshift.app.v11

import java.util.UUID

enum class ShiftConflictTypeV11 {
    NONE,
    EXACT_DUPLICATE,
    PARTIAL_OVERLAP,
    FULL_COVERAGE,
    EXISTING_COVERS_NEW
}

enum class ShiftConflictActionV11 {
    ADD,
    REPLACE,
    SPLIT_EXISTING,
    TRIM_EXISTING_BEFORE,
    TRIM_EXISTING_AFTER,
    REJECT
}

data class ShiftConflictV11(
    val existing: ShiftSegmentV11,
    val incoming: ShiftSegmentV11,
    val type: ShiftConflictTypeV11,
    val overlapStartMinute: Int,
    val overlapEndMinute: Int
)

data class ShiftConflictOptionV11(
    val id: String,
    val action: ShiftConflictActionV11,
    val title: String,
    val description: String,
    val resultingSegments: List<ShiftSegmentV11>
)

data class ShiftConflictAnalysisV11(
    val canAddDirectly: Boolean,
    val conflicts: List<ShiftConflictV11>,
    val options: List<ShiftConflictOptionV11>
)

object V11ShiftConflictEngine {

    fun analyse(
        existingSegments: List<ShiftSegmentV11>,
        incoming: ShiftSegmentV11
    ): ShiftConflictAnalysisV11 {

        if (incoming.kind == ShiftKind.OFF) {
            return analyseOff(
                existingSegments = existingSegments,
                incoming = incoming
            )
        }

        val incomingRange = incoming.timeRange()
            ?: return ShiftConflictAnalysisV11(
                canAddDirectly = true,
                conflicts = emptyList(),
                options = listOf(
                    directAddOption(
                        existingSegments,
                        incoming
                    )
                )
            )

        val timedExisting =
            existingSegments.filter {
                it.kind != ShiftKind.OFF &&
                    it.timeRange() != null
            }

        val conflicts =
            timedExisting.mapNotNull { existing ->

                val existingRange =
                    existing.timeRange()
                        ?: return@mapNotNull null

                if (!existingRange.overlaps(incomingRange)) {
                    return@mapNotNull null
                }

                val overlapStart =
                    maxOf(
                        existingRange.startMinute,
                        incomingRange.startMinute
                    )

                val overlapEnd =
                    minOf(
                        existingRange.endMinute,
                        incomingRange.endMinute
                    )

                val type =
                    when {
                        existingRange.startMinute ==
                            incomingRange.startMinute &&
                            existingRange.endMinute ==
                            incomingRange.endMinute ->
                            ShiftConflictTypeV11.EXACT_DUPLICATE

                        incomingRange.startMinute <=
                            existingRange.startMinute &&
                            incomingRange.endMinute >=
                            existingRange.endMinute ->
                            ShiftConflictTypeV11.FULL_COVERAGE

                        existingRange.startMinute <=
                            incomingRange.startMinute &&
                            existingRange.endMinute >=
                            incomingRange.endMinute ->
                            ShiftConflictTypeV11.EXISTING_COVERS_NEW

                        else ->
                            ShiftConflictTypeV11.PARTIAL_OVERLAP
                    }

                ShiftConflictV11(
                    existing = existing,
                    incoming = incoming,
                    type = type,
                    overlapStartMinute = overlapStart,
                    overlapEndMinute = overlapEnd
                )
            }

        if (conflicts.isEmpty()) {
            return ShiftConflictAnalysisV11(
                canAddDirectly = true,
                conflicts = emptyList(),
                options = listOf(
                    directAddOption(
                        existingSegments,
                        incoming
                    )
                )
            )
        }

        val options =
            buildSmartOptions(
                existingSegments = existingSegments,
                incoming = incoming,
                conflicts = conflicts
            )

        return ShiftConflictAnalysisV11(
            canAddDirectly = false,
            conflicts = conflicts,
            options = options
        )
    }

    private fun analyseOff(
        existingSegments: List<ShiftSegmentV11>,
        incoming: ShiftSegmentV11
    ): ShiftConflictAnalysisV11 {

        if (existingSegments.isEmpty()) {
            return ShiftConflictAnalysisV11(
                canAddDirectly = true,
                conflicts = emptyList(),
                options = listOf(
                    ShiftConflictOptionV11(
                        id = UUID.randomUUID().toString(),
                        action = ShiftConflictActionV11.ADD,
                        title = "إضافة إجازة",
                        description =
                            "اليوم فارغ ويمكن تسجيله كإجازة.",
                        resultingSegments =
                            listOf(incoming)
                    )
                )
            )
        }

        return ShiftConflictAnalysisV11(
            canAddDirectly = false,
            conflicts = emptyList(),
            options = listOf(
                ShiftConflictOptionV11(
                    id = UUID.randomUUID().toString(),
                    action = ShiftConflictActionV11.REPLACE,
                    title = "تحويل اليوم إلى إجازة",
                    description =
                        "سيتم حذف شيفتات اليوم الحالية وتسجيل اليوم كإجازة.",
                    resultingSegments =
                        listOf(incoming)
                ),
                ShiftConflictOptionV11(
                    id = UUID.randomUUID().toString(),
                    action = ShiftConflictActionV11.REJECT,
                    title = "إلغاء",
                    description =
                        "الاحتفاظ بالشيفتات الحالية بدون تغيير.",
                    resultingSegments =
                        existingSegments
                )
            )
        )
    }

    private fun directAddOption(
        existingSegments: List<ShiftSegmentV11>,
        incoming: ShiftSegmentV11
    ): ShiftConflictOptionV11 {

        return ShiftConflictOptionV11(
            id = UUID.randomUUID().toString(),
            action = ShiftConflictActionV11.ADD,
            title = "إضافة الشيفت",
            description =
                "لا يوجد تعارض زمني مع شيفتات اليوم.",
            resultingSegments =
                sortSegments(
                    existingSegments
                        .filter { it.kind != ShiftKind.OFF } +
                        incoming
                )
        )
    }

    private fun buildSmartOptions(
        existingSegments: List<ShiftSegmentV11>,
        incoming: ShiftSegmentV11,
        conflicts: List<ShiftConflictV11>
    ): List<ShiftConflictOptionV11> {

        val options =
            mutableListOf<ShiftConflictOptionV11>()

        /*
         * Smart option:
         * preserve every non-overlapping minute of existing
         * shifts and give the requested interval to incoming.
         *
         * Example:
         * existing FULL_DAY 00:00-24:00
         * incoming LONG 00:00-12:00
         *
         * result:
         * LONG incoming 00:00-12:00
         * existing becomes 12:00-24:00
         */
        val preserved =
            mutableListOf<ShiftSegmentV11>()

        existingSegments.forEach { existing ->

            if (existing.kind == ShiftKind.OFF) {
                return@forEach
            }

            val range = existing.timeRange()

            if (range == null) {
                preserved += existing
                return@forEach
            }

            val matchingConflict =
                conflicts.firstOrNull {
                    it.existing.id == existing.id
                }

            if (matchingConflict == null) {
                preserved += existing
                return@forEach
            }

            val incomingRange =
                incoming.timeRange()
                    ?: return@forEach

            if (
                range.startMinute <
                incomingRange.startMinute
            ) {
                preserved +=
                    createRemainder(
                        source = existing,
                        startMinute =
                            range.startMinute,
                        endMinute =
                            minOf(
                                range.endMinute,
                                incomingRange.startMinute
                            )
                    )
            }

            if (
                range.endMinute >
                incomingRange.endMinute
            ) {
                preserved +=
                    createRemainder(
                        source = existing,
                        startMinute =
                            maxOf(
                                range.startMinute,
                                incomingRange.endMinute
                            ),
                        endMinute =
                            range.endMinute
                    )
            }
        }

        val smartResult =
            sortSegments(
                preserved + incoming
            )

        if (isValidTimeline(smartResult)) {

            val action =
                when {
                    conflicts.any {
                        val range =
                            it.existing.timeRange()

                        range != null &&
                            range.startMinute <
                            it.overlapStartMinute &&
                            range.endMinute >
                            it.overlapEndMinute
                    } ->
                        ShiftConflictActionV11.SPLIT_EXISTING

                    conflicts.any {
                        val range =
                            it.existing.timeRange()

                        range != null &&
                            range.startMinute <
                            it.overlapStartMinute
                    } ->
                        ShiftConflictActionV11.TRIM_EXISTING_AFTER

                    conflicts.any {
                        val range =
                            it.existing.timeRange()

                        range != null &&
                            range.endMinute >
                            it.overlapEndMinute
                    } ->
                        ShiftConflictActionV11.TRIM_EXISTING_BEFORE

                    else ->
                        ShiftConflictActionV11.REPLACE
                }

            options +=
                ShiftConflictOptionV11(
                    id = UUID.randomUUID().toString(),
                    action = action,
                    title = "حل التعارض تلقائيًا",
                    description =
                        buildSmartDescription(
                            conflicts,
                            incoming
                        ),
                    resultingSegments =
                        smartResult
                )
        }

        /*
         * Full replacement remains available,
         * but never happens silently.
         */
        options +=
            ShiftConflictOptionV11(
                id = UUID.randomUUID().toString(),
                action = ShiftConflictActionV11.REPLACE,
                title = "استبدال الشيفتات المتعارضة",
                description =
                    "سيتم حذف الجزء المتعارض وإضافة الشيفت الجديد مع الاحتفاظ بالأجزاء غير المتعارضة.",
                resultingSegments =
                    smartResult
            )

        options +=
            ShiftConflictOptionV11(
                id = UUID.randomUUID().toString(),
                action = ShiftConflictActionV11.REJECT,
                title = "إلغاء الإضافة",
                description =
                    "لن يتم تغيير شيفتات اليوم الحالية.",
                resultingSegments =
                    existingSegments
            )

        return options.distinctBy {
            Pair(
                it.action,
                timelineSignature(
                    it.resultingSegments
                )
            )
        }
    }

    private fun createRemainder(
        source: ShiftSegmentV11,
        startMinute: Int,
        endMinute: Int
    ): ShiftSegmentV11 {

        val kind =
            inferKind(
                startMinute,
                endMinute,
                source.kind
            )

        return source.copy(
            id = UUID.randomUUID().toString(),
            kind = kind,
            startMinute = startMinute,
            endMinute = endMinute
        )
    }

    private fun inferKind(
        startMinute: Int,
        endMinute: Int,
        fallback: ShiftKind
    ): ShiftKind {

        return when {
            startMinute == 0 &&
                endMinute == 1440 ->
                ShiftKind.FULL_DAY

            startMinute == 0 &&
                endMinute == 720 ->
                ShiftKind.LONG

            startMinute == 720 &&
                endMinute == 1440 ->
                ShiftKind.NIGHT

            startMinute == 0 &&
                endMinute <= 480 ->
                ShiftKind.MORNING

            startMinute >= 480 &&
                endMinute <= 720 ->
                ShiftKind.EVENING

            startMinute >= 720 ->
                ShiftKind.NIGHT

            else ->
                fallback
        }
    }

    fun isValidTimeline(
        segments: List<ShiftSegmentV11>
    ): Boolean {

        val timed =
            segments
                .filter {
                    it.kind != ShiftKind.OFF
                }
                .mapNotNull {
                    it.timeRange()
                }
                .sortedBy {
                    it.startMinute
                }

        for (i in 0 until timed.size - 1) {
            if (
                timed[i].endMinute >
                timed[i + 1].startMinute
            ) {
                return false
            }
        }

        return true
    }

    fun occupiedMinutes(
        segments: List<ShiftSegmentV11>
    ): Int {

        return segments
            .filter {
                it.kind != ShiftKind.OFF
            }
            .mapNotNull {
                it.timeRange()
            }
            .sumOf {
                it.durationMinutes
            }
    }

    fun freeRanges(
        segments: List<ShiftSegmentV11>
    ): List<ShiftTimeRangeV11> {

        val timed =
            segments
                .filter {
                    it.kind != ShiftKind.OFF
                }
                .mapNotNull {
                    it.timeRange()
                }
                .sortedBy {
                    it.startMinute
                }

        if (timed.isEmpty()) {
            return listOf(
                ShiftTimeRangeV11(
                    0,
                    1440
                )
            )
        }

        val free =
            mutableListOf<ShiftTimeRangeV11>()

        var cursor = 0

        timed.forEach { range ->

            if (range.startMinute > cursor) {
                free +=
                    ShiftTimeRangeV11(
                        cursor,
                        range.startMinute
                    )
            }

            cursor =
                maxOf(
                    cursor,
                    range.endMinute
                )
        }

        if (cursor < 1440) {
            free +=
                ShiftTimeRangeV11(
                    cursor,
                    1440
                )
        }

        return free
    }

    private fun sortSegments(
        segments: List<ShiftSegmentV11>
    ): List<ShiftSegmentV11> {

        return segments.sortedWith(
            compareBy<ShiftSegmentV11> {
                if (it.kind == ShiftKind.OFF)
                    1441
                else
                    it.timeRange()
                        ?.startMinute
                        ?: 1440
            }.thenBy {
                it.timeRange()
                    ?.endMinute
                    ?: 1440
            }
        )
    }

    private fun timelineSignature(
        segments: List<ShiftSegmentV11>
    ): String {

        return sortSegments(segments)
            .joinToString("|") { segment ->

                val range =
                    segment.timeRange()

                "${segment.kind}:${segment.workplaceId}:${range?.startMinute}:${range?.endMinute}"
            }
    }

    private fun buildSmartDescription(
        conflicts: List<ShiftConflictV11>,
        incoming: ShiftSegmentV11
    ): String {

        val incomingRange =
            incoming.timeRange()

        if (
            conflicts.size == 1 &&
            incomingRange != null
        ) {
            val existing =
                conflicts.first().existing

            val existingRange =
                existing.timeRange()

            /*
             * Important user case:
             * 24h existing + new Long
             * -> Long first 12h
             * -> old workplace keeps Night last 12h.
             */
            if (
                existingRange?.startMinute == 0 &&
                existingRange.endMinute == 1440 &&
                incomingRange.startMinute == 0 &&
                incomingRange.endMinute == 720
            ) {
                return "الشيفت الحالي 24 ساعة يتعارض مع Long الجديد. سيتم وضع Long في أول 12 ساعة وتحويل الجزء المتبقي من الشيفت الحالي إلى Night."
            }
        }

        return "يوجد تعارض زمني. سيحتفظ التطبيق بالأجزاء غير المتعارضة من الشيفتات الحالية ويضع الشيفت الجديد في وقته فقط."
    }
}
