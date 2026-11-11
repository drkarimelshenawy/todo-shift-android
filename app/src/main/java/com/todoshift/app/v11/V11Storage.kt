package com.todoshift.app.v11

import android.content.Context
import android.content.SharedPreferences
import com.todoshift.app.storage.*
import com.todoshift.app.data.AppData
import org.json.JSONArray
import org.json.JSONObject

class V11Storage internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE))

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
        check(!wasMigrated()) { "Previously migrated database is missing" }

        val migrated = V11MigrationEngine.migrate(
            old = oldData,
            effectiveFrom = effectiveFrom
        )

        FinancialDataValidation.validate(migrated)
        val raw = toJson(migrated).toString()
        fromJson(JSONObject(raw))
        check(prefs.edit().putString(KEY_DATABASE, raw).putBoolean(KEY_MIGRATED, true).commit()) {
            "Unable to persist migrated database"
        }
        return migrated
    }

    internal fun isPristine(): Boolean = prefs.all.isEmpty()

    fun wasMigrated(): Boolean = prefs.getBoolean(KEY_MIGRATED, false)

    fun save(db: V11Database) {
        when (val current = read()) {
            is StorageRead.Failed -> current.requireLoaded()
            StorageRead.Missing -> error("Cannot save before explicit initialization or migration")
            is StorageRead.Loaded -> Unit
        }
        FinancialDataValidation.validate(db)
        val raw = toJson(db).toString()
        fromJson(JSONObject(raw))
        check(prefs.edit().putString(KEY_DATABASE, raw).commit()) { "Unable to persist database" }
    }

    fun read(): StorageRead<V11Database> = try {
        if (!prefs.contains(KEY_DATABASE)) StorageRead.Missing
        else StorageRead.Loaded(fromJson(JSONObject(requireNotNull(prefs.getString(KEY_DATABASE, null)))))
    } catch (failure: Exception) {
        StorageRead.Failed(failure)
    }

    fun load(): V11Database = read().requireLoaded()

    fun exportJson(db: V11Database): String =
        toJson(db).toString(2)

    fun importJson(raw: String): V11Database {
        val root = JSONObject(raw)

        require(root.has("schemaVersion")) {
            "ملف النسخة الاحتياطية لا يحتوي على إصدار قاعدة البيانات"
        }
        require(root.getInt("schemaVersion") == 13) {
            "إصدار قاعدة البيانات غير مدعوم"
        }

        val requiredArrays = listOf(
            "calendars", "days", "rates",
            "incomeSources", "paymentMethods",
            "payments", "settlements",
            "monthSnapshots", "auditLog",
            "dayItems", "tasks",
            "obligations", "obligationPayments"
        )

        requiredArrays.forEach { key ->
            require(root.optJSONArray(key) != null) {
                "ملف النسخة الاحتياطية ناقص أو غير صالح: $key"
            }
        }

        // Reject malformed nested entries before invoking the permissive parser.
        requiredArrays.forEach { key ->
            val array = root.getJSONArray(key)
            for (i in 0 until array.length()) {
                require(array.optJSONObject(i) != null) {
                    "عنصر غير صالح في $key عند الفهرس $i"
                }
            }
        }
        val calendarIds = mutableSetOf<String>()
        root.getJSONArray("calendars").let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id")
                require(id.isNotBlank() && calendarIds.add(id)) {
                    "معرف تقويم فارغ أو مكرر"
                }
            }
        }
        root.getJSONArray("days").let { array ->
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                require(item.optString("calendarId") in calendarIds) {
                    "يوم مرتبط بتقويم غير موجود"
                }
                require(item.optJSONArray("segments") != null) {
                    "يوم لا يحتوي على قائمة الشيفتات الصحيحة"
                }
            }
        }
        return fromJson(root)
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

                                    if (segment.startMinute == null)
                                        put("startMinute", JSONObject.NULL)
                                    else
                                        put("startMinute", segment.startMinute)

                                    if (segment.endMinute == null)
                                        put("endMinute", JSONObject.NULL)
                                    else
                                        put("endMinute", segment.endMinute)
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

                        if (source.fixedMonthlyAmount == null)
                            put("fixedMonthlyAmount", JSONObject.NULL)
                        else
                            put(
                                "fixedMonthlyAmount",
                                source.fixedMonthlyAmount
                            )

                        put("order", source.order)
                    })
                }
            })

            put("paymentMethods", JSONArray().apply {
                db.paymentMethods.forEach { method ->
                    put(JSONObject().apply {
                        put("id", method.id)
                        put("name", method.name)
                        put("active", method.active)
                        put("order", method.order)
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

                        put("allocations", JSONArray().apply {
                            payment.allocations.forEach { allocation ->
                                put(JSONObject().apply {
                                    put("id", allocation.id)
                                    put(
                                        "accrualMonth",
                                        allocation.accrualMonth
                                    )

                                    if (allocation.sourceId == null)
                                        put(
                                            "sourceId",
                                            JSONObject.NULL
                                        )
                                    else
                                        put(
                                            "sourceId",
                                            allocation.sourceId
                                        )

                                    put(
                                        "amount",
                                        allocation.amount
                                    )
                                })
                            }
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


            put("dayItems", JSONArray().apply {
                db.dayItems.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("date", item.date)

                        if (item.calendarId == null)
                            put("calendarId", JSONObject.NULL)
                        else
                            put("calendarId", item.calendarId)

                        put("type", item.type.name)
                        put("title", item.title)
                        put("details", item.details)
                        put("completed", item.completed)

                        if (item.reminderAt == null)
                            put("reminderAt", JSONObject.NULL)
                        else
                            put("reminderAt", item.reminderAt)

                        put("createdAt", item.createdAt)
                    })
                }
            })

            put("tasks", JSONArray().apply {
                db.tasks.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("details", item.details)
                        put("date", item.date)

                        if (item.time == null)
                            put("time", JSONObject.NULL)
                        else
                            put("time", item.time)

                        put("priority", item.priority.name)
                        put("status", item.status.name)
                        put("repeat", item.repeat.name)

                        if (item.reminderAt == null)
                            put("reminderAt", JSONObject.NULL)
                        else
                            put("reminderAt", item.reminderAt)

                        if (item.workplaceId == null)
                            put("workplaceId", JSONObject.NULL)
                        else
                            put("workplaceId", item.workplaceId)

                        if (item.calendarId == null)
                            put("calendarId", JSONObject.NULL)
                        else
                            put("calendarId", item.calendarId)

                        if (item.linkedShiftId == null)
                            put("linkedShiftId", JSONObject.NULL)
                        else
                            put("linkedShiftId", item.linkedShiftId)

                        put("createdAt", item.createdAt)
                        put("updatedAt", item.updatedAt)

                        if (item.completedAt == null)
                            put("completedAt", JSONObject.NULL)
                        else
                            put("completedAt", item.completedAt)
                    })
                }
            })

            put("obligations", JSONArray().apply {
                db.obligations.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put("name", item.name)
                        put("type", item.type.name)

                        if (item.totalAmount == null)
                            put("totalAmount", JSONObject.NULL)
                        else
                            put("totalAmount", item.totalAmount)

                        if (item.installmentAmount == null)
                            put(
                                "installmentAmount",
                                JSONObject.NULL
                            )
                        else
                            put(
                                "installmentAmount",
                                item.installmentAmount
                            )

                        put("startDate", item.startDate)

                        if (item.endDate == null)
                            put("endDate", JSONObject.NULL)
                        else
                            put("endDate", item.endDate)

                        if (item.dueDay == null)
                            put("dueDay", JSONObject.NULL)
                        else
                            put("dueDay", item.dueDay)

                        if (item.installmentCount == null)
                            put(
                                "installmentCount",
                                JSONObject.NULL
                            )
                        else
                            put(
                                "installmentCount",
                                item.installmentCount
                            )

                        put(
                            "reminderDaysBefore",
                            item.reminderDaysBefore
                        )
                        put("status", item.status.name)
                        put("note", item.note)
                    })
                }
            })

            put("obligationPayments", JSONArray().apply {
                db.obligationPayments.forEach { item ->
                    put(JSONObject().apply {
                        put("id", item.id)
                        put(
                            "obligationId",
                            item.obligationId
                        )
                        put("dueMonth", item.dueMonth)
                        put("amount", item.amount)
                        put("paidDate", item.paidDate)

                        if (item.paymentMethodId == null)
                            put(
                                "paymentMethodId",
                                JSONObject.NULL
                            )
                        else
                            put(
                                "paymentMethodId",
                                item.paymentMethodId
                            )

                        put("note", item.note)
                    })
                }
            })
        }

    private fun validateStoredStructure(root: JSONObject) {
        val version = root.schemaVersion(setOf(11, 12, 13))
        val base = listOf("calendars", "days", "rates", "incomeSources", "paymentMethods",
            "payments", "settlements", "monthSnapshots", "auditLog")
        val added12 = listOf("dayItems", "obligations", "obligationPayments")
        (base + added12 + "tasks").forEach { key ->
            root.objectArray(key, key in base || (version >= 12 && key in added12) || (version >= 13 && key == "tasks"))
        }
        val calendars = root.objectArray("calendars").map { it.nonBlankString("id") }
        require(calendars.distinct().size == calendars.size) { "Duplicate calendar ID" }
        root.objectArray("days").forEach { day ->
            day.nonBlankString("id")
            java.time.LocalDate.parse(day.nonBlankString("date"))
            require(day.nonBlankString("calendarId") in calendars) { "Unknown shift calendar" }
            day.objectArray("segments").forEach { segment ->
                segment.nonBlankString("id")
                ShiftKind.valueOf(segment.nonBlankString("kind"))
            }
        }
        root.objectArray("payments").forEach { payment ->
            val ids = payment.get("linkedShiftIds") as? JSONArray ?: error("Invalid linkedShiftIds")
            for (i in 0 until ids.length()) require(ids.get(i) is String) { "Invalid linked shift ID" }
            payment.objectArray("allocations", version >= 12)
        }
    }

    private fun fromJson(root: JSONObject): V11Database {
        validateStoredStructure(root)


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
                    note = s.optString("note", ""),
                    startMinute =
                        if (
                            s.has("startMinute") &&
                            !s.isNull("startMinute")
                        ) s.optInt("startMinute")
                        else null,
                    endMinute =
                        if (
                            s.has("endMinute") &&
                            !s.isNull("endMinute")
                        ) s.optInt("endMinute")
                        else null
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
                active = o.optBoolean("active", true),
                fixedMonthlyAmount =
                    if (
                        o.has("fixedMonthlyAmount") &&
                        !o.isNull("fixedMonthlyAmount")
                    ) o.optDouble("fixedMonthlyAmount")
                    else null,
                order = o.optInt("order", i)
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
                active = o.optBoolean("active", true),
                order = o.optInt("order", i)
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

            val allocations =
                mutableListOf<PaymentAllocationV11>()

            val allocationArray =
                o.optJSONArray("allocations") ?: JSONArray()

            for (j in 0 until allocationArray.length()) {
                val a = allocationArray.getJSONObject(j)

                allocations += PaymentAllocationV11(
                    id = a.optString("id"),
                    accrualMonth =
                        a.optString("accrualMonth"),
                    sourceId =
                        nullableString(a, "sourceId"),
                    amount =
                        a.optDouble("amount", 0.0)
                )
            }

            /*
             * Schema 11 payments had only one accrualMonth.
             * We keep that value untouched. New payments may
             * additionally contain explicit allocations.
             */
            payments += PaymentV11(
                id = o.optString("id"),
                amount = o.optDouble("amount", 0.0),
                receivedDate = o.optString("receivedDate"),
                accrualMonth = o.optString("accrualMonth"),
                sourceId = nullableString(o, "sourceId"),
                paymentMethodId =
                    nullableString(o, "paymentMethodId"),
                note = o.optString("note", ""),
                linkedShiftIds = linked,
                allocations = allocations
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

        val dayItems = mutableListOf<DayItemV11>()
        val dayItemArray =
            root.optJSONArray("dayItems") ?: JSONArray()

        for (i in 0 until dayItemArray.length()) {
            val o = dayItemArray.getJSONObject(i)

            val type = runCatching {
                DayItemTypeV11.valueOf(
                    o.optString(
                        "type",
                        DayItemTypeV11.NOTE.name
                    )
                )
            }.getOrDefault(DayItemTypeV11.NOTE)

            dayItems += DayItemV11(
                id = o.optString("id"),
                date = o.optString("date"),
                calendarId =
                    nullableString(o, "calendarId"),
                type = type,
                title = o.optString("title"),
                details = o.optString("details", ""),
                completed =
                    o.optBoolean("completed", false),
                reminderAt =
                    if (
                        o.has("reminderAt") &&
                        !o.isNull("reminderAt")
                    ) o.optLong("reminderAt")
                    else null,
                createdAt =
                    o.optLong(
                        "createdAt",
                        System.currentTimeMillis()
                    )
            )
        }

        val tasks =
            mutableListOf<TaskV11>()

        val taskArray =
            root.optJSONArray("tasks") ?: JSONArray()

        for (i in 0 until taskArray.length()) {
            val o = taskArray.getJSONObject(i)

            val priority = runCatching {
                TaskPriorityV11.valueOf(
                    o.optString(
                        "priority",
                        TaskPriorityV11.NORMAL.name
                    )
                )
            }.getOrDefault(TaskPriorityV11.NORMAL)

            val status = runCatching {
                TaskStatusV11.valueOf(
                    o.optString(
                        "status",
                        TaskStatusV11.TODO.name
                    )
                )
            }.getOrDefault(TaskStatusV11.TODO)

            val repeat = runCatching {
                TaskRepeatV11.valueOf(
                    o.optString(
                        "repeat",
                        TaskRepeatV11.NONE.name
                    )
                )
            }.getOrDefault(TaskRepeatV11.NONE)

            tasks += TaskV11(
                id = o.optString("id"),
                title = o.optString("title"),
                details = o.optString("details", ""),
                date = o.optString("date"),
                time = nullableString(o, "time"),
                priority = priority,
                status = status,
                repeat = repeat,
                reminderAt =
                    if (
                        o.has("reminderAt") &&
                        !o.isNull("reminderAt")
                    ) o.optLong("reminderAt")
                    else null,
                workplaceId =
                    nullableString(o, "workplaceId"),
                calendarId =
                    nullableString(o, "calendarId"),
                linkedShiftId =
                    nullableString(o, "linkedShiftId"),
                createdAt =
                    o.optLong(
                        "createdAt",
                        System.currentTimeMillis()
                    ),
                updatedAt =
                    o.optLong(
                        "updatedAt",
                        System.currentTimeMillis()
                    ),
                completedAt =
                    if (
                        o.has("completedAt") &&
                        !o.isNull("completedAt")
                    ) o.optLong("completedAt")
                    else null
            )
        }

        val obligations =
            mutableListOf<ObligationV11>()

        val obligationArray =
            root.optJSONArray("obligations") ?: JSONArray()

        for (i in 0 until obligationArray.length()) {
            val o = obligationArray.getJSONObject(i)

            val type = runCatching {
                ObligationTypeV11.valueOf(
                    o.optString(
                        "type",
                        ObligationTypeV11.OTHER.name
                    )
                )
            }.getOrDefault(ObligationTypeV11.OTHER)

            val status = runCatching {
                ObligationStatusV11.valueOf(
                    o.optString(
                        "status",
                        ObligationStatusV11.ACTIVE.name
                    )
                )
            }.getOrDefault(
                ObligationStatusV11.ACTIVE
            )

            obligations += ObligationV11(
                id = o.optString("id"),
                name = o.optString("name"),
                type = type,
                totalAmount =
                    if (
                        o.has("totalAmount") &&
                        !o.isNull("totalAmount")
                    ) o.optDouble("totalAmount")
                    else null,
                installmentAmount =
                    if (
                        o.has("installmentAmount") &&
                        !o.isNull("installmentAmount")
                    ) o.optDouble("installmentAmount")
                    else null,
                startDate = o.optString("startDate"),
                endDate =
                    nullableString(o, "endDate"),
                dueDay =
                    if (
                        o.has("dueDay") &&
                        !o.isNull("dueDay")
                    ) o.optInt("dueDay")
                    else null,
                installmentCount =
                    if (
                        o.has("installmentCount") &&
                        !o.isNull("installmentCount")
                    ) o.optInt("installmentCount")
                    else null,
                reminderDaysBefore =
                    o.optInt("reminderDaysBefore", 3),
                status = status,
                note = o.optString("note", "")
            )
        }

        val obligationPayments =
            mutableListOf<ObligationPaymentV11>()

        val obligationPaymentArray =
            root.optJSONArray("obligationPayments")
                ?: JSONArray()

        for (i in 0 until obligationPaymentArray.length()) {
            val o =
                obligationPaymentArray.getJSONObject(i)

            obligationPayments +=
                ObligationPaymentV11(
                    id = o.optString("id"),
                    obligationId =
                        o.optString("obligationId"),
                    dueMonth =
                        o.optString("dueMonth"),
                    amount =
                        o.optDouble("amount", 0.0),
                    paidDate =
                        o.optString("paidDate"),
                    paymentMethodId =
                        nullableString(
                            o,
                            "paymentMethodId"
                        ),
                    note =
                        o.optString("note", "")
                )
        }

        return V11Database(
            schemaVersion = 13,
            calendars = calendars,
            days = days,
            rates = rates,
            incomeSources = sources,
            paymentMethods = methods,
            payments = payments,
            settlements = settlements,
            monthSnapshots = snapshots,
            auditLog = audit,
            dayItems = dayItems,
            tasks = tasks,
            obligations = obligations,
            obligationPayments = obligationPayments
        )
    }
}
