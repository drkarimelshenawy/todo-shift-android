package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.data.AppData
import org.json.JSONArray
import org.json.JSONObject

class V11Storage(context: Context) {

    private val prefs = context.getSharedPreferences(
        "todo_shift_v11",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_DATABASE = "database"
        private const val KEY_MIGRATED = "migrated_from_rc1"
    }

    fun hasDatabase(): Boolean =
        prefs.contains(KEY_DATABASE)

    fun migrateIfNeeded(
        oldData: AppData,
        effectiveFrom: String
    ): V11Database {
        if (hasDatabase()) return load()

        val migrated = V11MigrationEngine.migrate(
            old = oldData,
            effectiveFrom = effectiveFrom
        )

        save(migrated)

        prefs.edit()
            .putBoolean(KEY_MIGRATED, true)
            .apply()

        return migrated
    }

    fun save(db: V11Database) {
        prefs.edit()
            .putString(KEY_DATABASE, toJson(db).toString())
            .apply()
    }

    fun load(): V11Database {
        val raw = prefs.getString(KEY_DATABASE, null)
            ?: return V11Database()

        return runCatching {
            fromJson(JSONObject(raw))
        }.getOrElse {
            V11Database()
        }
    }

    fun exportJson(db: V11Database): String =
        toJson(db).toString(2)

    fun importJson(raw: String): V11Database {
        val db = fromJson(JSONObject(raw))
        save(db)
        return db
    }

    private fun toJson(db: V11Database): JSONObject =
        JSONObject().apply {

            put("schemaVersion", db.schemaVersion)

            put("calendars", JSONArray().apply {
                db.calendars.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("colorHex", item.colorHex)
                        put("visible", item.visible)
                        put("order", item.order)
                        put("financial", item.financial)
                    })
                }
            })

            put("days", JSONArray().apply {
                db.days.forEach { day ->
                    put(JSONObject().apply {
                        put("id", day.id)
                        put("date", day.date)
                        put("calendarId", day.calendarId)
                        put("note", day.note)

                        put("segments", JSONArray().apply {
                            day.segments.forEach { segment ->
                                put(JSONObject().apply {
                                    put("id", segment.id)
                                    put("kind", segment.kind.name)

                                    if (segment.workplaceId == null)
                                        put("workplaceId", JSONObject.NULL)
                                    else
                                        put("workplaceId", segment.workplaceId)

                                    if (segment.exceptionalAmount == null)
                                        put("exceptionalAmount", JSONObject.NULL)
                                    else
                                        put(
                                            "exceptionalAmount",
                                            segment.exceptionalAmount
                                        )

                                    put("note", segment.note)
                                })
                            }
                        })
                    })
                }
            })

            put("rates", JSONArray().apply {
                db.rates.forEach { rate ->
                    put(JSONObject().apply {
                        put("id", rate.id)
                        put("workplaceId", rate.workplaceId)
                        put("shiftKind", rate.shiftKind.name)
                        put("effectiveFrom", rate.effectiveFrom)
                        put("amount", rate.amount)
                    })
                }
            })

            put("incomeSources", JSONArray().apply {
                db.incomeSources.forEach { source ->
                    put(JSONObject().apply {
                        put("id", source.id)
                        put("name", source.name)
                        put("type", source.type.name)

                        if (source.workplaceId == null)
                            put("workplaceId", JSONObject.NULL)
                        else
                            put("workplaceId", source.workplaceId)

                        if (source.calendarId == null)
                            put("calendarId", JSONObject.NULL)
                        else
                            put("calendarId", source.calendarId)

                        put("active", source.active)
                    })
                }
            })

            put("paymentMethods", JSONArray().apply {
                db.paymentMethods.forEach { method ->
                    put(JSONObject().apply {
                        put("id", method.id)
                        put("name", method.name)
                        put("active", method.active)
                    })
                }
            })

            put("payments", JSONArray().apply {
                db.payments.forEach { payment ->
                    put(JSONObject().apply {
                        put("id", payment.id)
                        put("amount", payment.amount)
                        put("receivedDate", payment.receivedDate)
                        put("accrualMonth", payment.accrualMonth)

                        if (payment.sourceId == null)
                            put("sourceId", JSONObject.NULL)
                        else
                            put("sourceId", payment.sourceId)

                        if (payment.paymentMethodId == null)
                            put("paymentMethodId", JSONObject.NULL)
                        else
                            put(
                                "paymentMethodId",
                                payment.paymentMethodId
                            )

                        put("note", payment.note)

                        put("linkedShiftIds", JSONArray().apply {
                            payment.linkedShiftIds.forEach { put(it) }
                        })
                    })
                }
            })

            put("settlements", JSONArray().apply {
                db.settlements.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("accrualMonth", item.accrualMonth)

                        if (item.sourceId == null)
                            put("sourceId", JSONObject.NULL)
                        else
                            put("sourceId", item.sourceId)

                        if (item.workplaceId == null)
                            put("workplaceId", JSONObject.NULL)
                        else
                            put("workplaceId", item.workplaceId)

                        if (item.calendarId == null)
                            put("calendarId", JSONObject.NULL)
                        else
                            put("calendarId", item.calendarId)

                        put("action", item.action.name)
                        put("amount", item.amount)
                        put("reason", item.reason)
                        put("createdAt", item.createdAt)
                    })
                }
            })

            put("monthSnapshots", JSONArray().apply {
                db.monthSnapshots.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("month", item.month)
                        put("expectedAmount", item.expectedAmount)
                        put("receivedAmount", item.receivedAmount)
                        put("settlementAmount", item.settlementAmount)
                        put("finalDifference", item.finalDifference)
                        put("closedAt", item.closedAt)

                        if (item.reopenedAt == null)
                            put("reopenedAt", JSONObject.NULL)
                        else
                            put("reopenedAt", item.reopenedAt)

                        put("note", item.note)
                    })
                }
            })

            put("auditLog", JSONArray().apply {
                db.auditLog.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("action", item.action)
                        put("entityType", item.entityType)

                        if (item.entityId == null)
                            put("entityId", JSONObject.NULL)
                        else
                            put("entityId", item.entityId)

                        put("description", item.description)
                        put("createdAt", item.createdAt)
                    })
                }
            })
        }

    private fun fromJson(root: JSONObject): V11Database {

        fun nullableString(
            obj: JSONObject,
            key: String
        ): String? =
            if (!obj.has(key) || obj.isNull(key))
                null
            else
                obj.optString(key).takeIf { it.isNotBlank() }

        val calendars = mutableListOf<AppCalendarV11>()
        val calendarArray =
            root.optJSONArray("calendars") ?: JSONArray()

        for (i in 0 until calendarArray.length()) {
            val o = calendarArray.getJSONObject(i)

            calendars += AppCalendarV11(
                id = o.optString("id"),
                name = o.optString("name", "تقويم"),
                colorHex = o.optString("colorHex", "#213555"),
                visible = o.optBoolean("visible", true),
                order = o.optInt("order", i),
                financial = o.optBoolean("financial", true)
            )
        }

        val days = mutableListOf<DayEntryV11>()
        val dayArray = root.optJSONArray("days") ?: JSONArray()

        for (i in 0 until dayArray.length()) {
            val o = dayArray.getJSONObject(i)
            val segments = mutableListOf<ShiftSegmentV11>()
            val segmentArray =
                o.optJSONArray("segments") ?: JSONArray()

            for (j in 0 until segmentArray.length()) {
                val s = segmentArray.getJSONObject(j)

                val kind = runCatching {
                    ShiftKind.valueOf(
                        s.optString("kind", ShiftKind.OFF.name)
                    )
                }.getOrDefault(ShiftKind.OFF)

                segments += ShiftSegmentV11(
                    id = s.optString("id"),
                    kind = kind,
                    workplaceId =
                        nullableString(s, "workplaceId"),
                    exceptionalAmount =
                        if (
                            s.has("exceptionalAmount") &&
                            !s.isNull("exceptionalAmount")
                        ) s.optDouble("exceptionalAmount")
                        else null,
                    note = s.optString("note", "")
                )
            }

            days += DayEntryV11(
                id = o.optString("id"),
                date = o.optString("date"),
                calendarId = o.optString("calendarId"),
                segments = segments,
                note = o.optString("note", "")
            )
        }

        val rates = mutableListOf<RateHistoryEntryV11>()
        val rateArray = root.optJSONArray("rates") ?: JSONArray()

        for (i in 0 until rateArray.length()) {
            val o = rateArray.getJSONObject(i)

            val kind = runCatching {
                ShiftKind.valueOf(
                    o.optString(
                        "shiftKind",
                        ShiftKind.FULL_DAY.name
                    )
                )
            }.getOrDefault(ShiftKind.FULL_DAY)

            rates += RateHistoryEntryV11(
                id = o.optString("id"),
                workplaceId = o.optString("workplaceId"),
                shiftKind = kind,
                effectiveFrom = o.optString("effectiveFrom"),
                amount = o.optDouble("amount", 0.0)
            )
        }

        val sources = mutableListOf<IncomeSourceV11>()
        val sourceArray =
            root.optJSONArray("incomeSources") ?: JSONArray()

        for (i in 0 until sourceArray.length()) {
            val o = sourceArray.getJSONObject(i)

            val type = runCatching {
                IncomeSourceTypeV11.valueOf(
                    o.optString(
                        "type",
                        IncomeSourceTypeV11.OTHER.name
                    )
                )
            }.getOrDefault(IncomeSourceTypeV11.OTHER)

            sources += IncomeSourceV11(
                id = o.optString("id"),
                name = o.optString("name"),
                type = type,
                workplaceId =
                    nullableString(o, "workplaceId"),
                calendarId =
                    nullableString(o, "calendarId"),
                active = o.optBoolean("active", true)
            )
        }

        val methods = mutableListOf<PaymentMethodV11>()
        val methodArray =
            root.optJSONArray("paymentMethods") ?: JSONArray()

        for (i in 0 until methodArray.length()) {
            val o = methodArray.getJSONObject(i)

            methods += PaymentMethodV11(
                id = o.optString("id"),
                name = o.optString("name"),
                active = o.optBoolean("active", true)
            )
        }

        val payments = mutableListOf<PaymentV11>()
        val paymentArray =
            root.optJSONArray("payments") ?: JSONArray()

        for (i in 0 until paymentArray.length()) {
            val o = paymentArray.getJSONObject(i)
            val linked = mutableListOf<String>()
            val linkedArray =
                o.optJSONArray("linkedShiftIds") ?: JSONArray()

            for (j in 0 until linkedArray.length()) {
                linked += linkedArray.optString(j)
            }

            payments += PaymentV11(
                id = o.optString("id"),
                amount = o.optDouble("amount", 0.0),
                receivedDate = o.optString("receivedDate"),
                accrualMonth = o.optString("accrualMonth"),
                sourceId = nullableString(o, "sourceId"),
                paymentMethodId =
                    nullableString(o, "paymentMethodId"),
                note = o.optString("note", ""),
                linkedShiftIds = linked
            )
        }

        val settlements = mutableListOf<SettlementV11>()
        val settlementArray =
            root.optJSONArray("settlements") ?: JSONArray()

        for (i in 0 until settlementArray.length()) {
            val o = settlementArray.getJSONObject(i)

            val action = runCatching {
                SettlementActionV11.valueOf(
                    o.optString(
                        "action",
                        SettlementActionV11.ADJUSTMENT.name
                    )
                )
            }.getOrDefault(SettlementActionV11.ADJUSTMENT)

            settlements += SettlementV11(
                id = o.optString("id"),
                accrualMonth = o.optString("accrualMonth"),
                sourceId = nullableString(o, "sourceId"),
                workplaceId =
                    nullableString(o, "workplaceId"),
                calendarId =
                    nullableString(o, "calendarId"),
                action = action,
                amount = o.optDouble("amount", 0.0),
                reason = o.optString("reason", ""),
                createdAt = o.optLong("createdAt", 0L)
            )
        }

        val snapshots = mutableListOf<MonthSnapshotV11>()
        val snapshotArray =
            root.optJSONArray("monthSnapshots") ?: JSONArray()

        for (i in 0 until snapshotArray.length()) {
            val o = snapshotArray.getJSONObject(i)

            snapshots += MonthSnapshotV11(
                id = o.optString("id"),
                month = o.optString("month"),
                expectedAmount =
                    o.optDouble("expectedAmount", 0.0),
                receivedAmount =
                    o.optDouble("receivedAmount", 0.0),
                settlementAmount =
                    o.optDouble("settlementAmount", 0.0),
                finalDifference =
                    o.optDouble("finalDifference", 0.0),
                closedAt = o.optLong("closedAt", 0L),
                reopenedAt =
                    if (
                        o.has("reopenedAt") &&
                        !o.isNull("reopenedAt")
                    ) o.optLong("reopenedAt")
                    else null,
                note = o.optString("note", "")
            )
        }

        val audit = mutableListOf<AuditEntryV11>()
        val auditArray =
            root.optJSONArray("auditLog") ?: JSONArray()

        for (i in 0 until auditArray.length()) {
            val o = auditArray.getJSONObject(i)

            audit += AuditEntryV11(
                id = o.optString("id"),
                action = o.optString("action"),
                entityType = o.optString("entityType"),
                entityId = nullableString(o, "entityId"),
                description = o.optString("description"),
                createdAt = o.optLong("createdAt", 0L)
            )
        }

        return V11Database(
            schemaVersion = root.optInt("schemaVersion", 11),
            calendars = calendars,
            days = days,
            rates = rates,
            incomeSources = sources,
            paymentMethods = methods,
            payments = payments,
            settlements = settlements,
            monthSnapshots = snapshots,
            auditLog = audit
        )
    }
}
