package com.todoshift.app.v11

object V11ShiftConflictChecks {

    fun run(): List<String> {

        val errors =
            mutableListOf<String>()

        checkFullDayToLong(errors)
        checkNoOverlap(errors)
        checkMorningEveningNight(errors)
        checkFreeRanges(errors)

        return errors
    }

    private fun checkFullDayToLong(
        errors: MutableList<String>
    ) {

        val old =
            ShiftSegmentV11(
                id = "old-full",
                kind = ShiftKind.FULL_DAY,
                workplaceId = "work-a"
            )

        val incoming =
            ShiftSegmentV11(
                id = "new-long",
                kind = ShiftKind.LONG,
                workplaceId = "work-b"
            )

        val analysis =
            V11ShiftConflictEngine.analyse(
                listOf(old),
                incoming
            )

        if (analysis.canAddDirectly) {
            errors +=
                "FULL_DAY + LONG must conflict"
            return
        }

        val smart =
            analysis.options.firstOrNull {
                it.action !=
                    ShiftConflictActionV11.REJECT
            }

        if (smart == null) {
            errors +=
                "FULL_DAY + LONG has no smart option"
            return
        }

        if (
            !V11ShiftConflictEngine.isValidTimeline(
                smart.resultingSegments
            )
        ) {
            errors +=
                "FULL_DAY + LONG smart result overlaps"
        }

        val ranges =
            smart.resultingSegments
                .mapNotNull {
                    it.timeRange()
                }
                .sortedBy {
                    it.startMinute
                }

        if (
            ranges.size != 2 ||
            ranges[0].startMinute != 0 ||
            ranges[0].endMinute != 720 ||
            ranges[1].startMinute != 720 ||
            ranges[1].endMinute != 1440
        ) {
            errors +=
                "FULL_DAY + LONG must become 00-12 and 12-24"
        }
    }

    private fun checkNoOverlap(
        errors: MutableList<String>
    ) {

        val morning =
            ShiftSegmentV11(
                id = "morning",
                kind = ShiftKind.MORNING
            )

        val long =
            ShiftSegmentV11(
                id = "long",
                kind = ShiftKind.LONG
            )

        val analysis =
            V11ShiftConflictEngine.analyse(
                listOf(morning),
                long
            )

        if (analysis.canAddDirectly) {
            errors +=
                "MORNING + LONG overlap was not detected"
        }
    }

    private fun checkMorningEveningNight(
        errors: MutableList<String>
    ) {

        val segments =
            listOf(
                ShiftSegmentV11(
                    id = "m",
                    kind = ShiftKind.MORNING
                ),
                ShiftSegmentV11(
                    id = "e",
                    kind = ShiftKind.EVENING
                ),
                ShiftSegmentV11(
                    id = "n",
                    kind = ShiftKind.NIGHT
                )
            )

        if (
            !V11ShiftConflictEngine
                .isValidTimeline(segments)
        ) {
            errors +=
                "Morning/Evening/Night default timeline overlaps"
        }
    }

    private fun checkFreeRanges(
        errors: MutableList<String>
    ) {

        val long =
            ShiftSegmentV11(
                id = "long",
                kind = ShiftKind.LONG
            )

        val free =
            V11ShiftConflictEngine.freeRanges(
                listOf(long)
            )

        if (
            free.size != 1 ||
            free.first().startMinute != 720 ||
            free.first().endMinute != 1440
        ) {
            errors +=
                "LONG free range must be 12:00-24:00"
        }
    }
}
