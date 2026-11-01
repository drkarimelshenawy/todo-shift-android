package com.todoshift.app.v11

import com.todoshift.app.data.AppData
import com.todoshift.app.data.ShiftType
import java.util.UUID

object V11MigrationEngine {

    private const val DEFAULT_CALENDAR_ID =
        "v11-default-work-calendar"

    fun migrate(
        old: AppData,
        effectiveFrom: String
    ): V11Database {

        val defaultCalendar = AppCalendarV11(
            id = DEFAULT_CALENDAR_ID,
            name = "الشغل",
            colorHex = "#213555",
            visible = true,
            order = 0,
            financial = true
        )

        val days = old.shifts.map { shift ->

            val segments =
                when (shift.type) {

                    ShiftType.FULL_DAY ->
                        listOf(
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.FULL_DAY,
                                workplaceId =
                                    shift.workplaceIds.getOrNull(0),
                                exceptionalAmount =
                                    shift.customAmount,
                                note = shift.note
                            )
                        )

                    ShiftType.LONG_NIGHT -> {
                        val customPart =
                            shift.customAmount?.div(2.0)

                        listOf(
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.LONG,
                                workplaceId =
                                    shift.workplaceIds.getOrNull(0),
                                exceptionalAmount = customPart,
                                note = shift.note
                            ),
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.NIGHT,
                                workplaceId =
                                    shift.workplaceIds.getOrNull(1),
                                exceptionalAmount = customPart,
                                note = shift.note
                            )
                        )
                    }

                    ShiftType.THREE_SHIFTS -> {
                        val customPart =
                            shift.customAmount?.div(3.0)

                        listOf(
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.MORNING,
                                workplaceId =
                                    shift.workplaceIds.getOrNull(0),
                                exceptionalAmount = customPart,
                                note = shift.note
                            ),
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.EVENING,
                                workplaceId =
                                    shift.workplaceIds.getOrNull(1),
                                exceptionalAmount = customPart,
                                note = shift.note
                            ),
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.NIGHT,
                                workplaceId =
                                    shift.workplaceIds.getOrNull(2),
                                exceptionalAmount = customPart,
                                note = shift.note
                            )
                        )
                    }

                    ShiftType.OFF ->
                        listOf(
                            ShiftSegmentV11(
                                id = UUID.randomUUID().toString(),
                                kind = ShiftKind.OFF,
                                workplaceId = null,
                                exceptionalAmount = null,
                                note = shift.note
                            )
                        )
                }

            DayEntryV11(
                id = UUID.randomUUID().toString(),
                date = shift.date,
                calendarId = DEFAULT_CALENDAR_ID,
                segments = segments,
                note = shift.note
            )
        }

        val rates = old.workplaces.flatMap { workplace ->

            listOf(
                ShiftKind.FULL_DAY to workplace.fullDayRate,
                ShiftKind.LONG to workplace.longRate,
                ShiftKind.NIGHT to workplace.nightRate,
                ShiftKind.MORNING to workplace.morningRate,
                ShiftKind.EVENING to workplace.eveningRate
            ).map { (kind, amount) ->
                RateHistoryEntryV11(
                    id = UUID.randomUUID().toString(),
                    workplaceId = workplace.id,
                    shiftKind = kind,
                    effectiveFrom = effectiveFrom,
                    amount = amount
                )
            }
        }

        val incomeSources = old.workplaces.map { workplace ->
            IncomeSourceV11(
                id = "workplace-${workplace.id}",
                name = workplace.name,
                type = IncomeSourceTypeV11.SHIFT_BASED,
                workplaceId = workplace.id,
                calendarId = DEFAULT_CALENDAR_ID
            )
        }

        val methods = listOf(
            PaymentMethodV11(
                id = "cash",
                name = "Cash"
            ),
            PaymentMethodV11(
                id = "visa",
                name = "Visa"
            ),
            PaymentMethodV11(
                id = "post",
                name = "البريد"
            ),
            PaymentMethodV11(
                id = "bank",
                name = "تحويل بنكي"
            ),
            PaymentMethodV11(
                id = "wallet",
                name = "محفظة"
            )
        )

        val payments = old.payments.map { payment ->
            val month =
                if (payment.date.length >= 7) {
                    payment.date.substring(0, 7)
                } else {
                    ""
                }

            PaymentV11(
                id = payment.id,
                amount = payment.amount,
                receivedDate = payment.date,
                accrualMonth = month,
                sourceId = null,
                paymentMethodId = null,
                note = payment.note
            )
        }

        return V11Database(
            calendars = listOf(defaultCalendar),
            days = days,
            rates = rates,
            incomeSources = incomeSources,
            paymentMethods = methods,
            payments = payments,
            auditLog = listOf(
                AuditEntryV11(
                    id = UUID.randomUUID().toString(),
                    action = "MIGRATION",
                    entityType = "DATABASE",
                    description =
                        "Migrated RC1 data to V1.1 core schema"
                )
            )
        )
    }
}
