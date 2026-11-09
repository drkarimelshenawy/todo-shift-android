package com.todoshift.app.data

import android.content.Context
import android.content.SharedPreferences
import com.todoshift.app.storage.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppStorage internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("todo_shift_data", Context.MODE_PRIVATE))

    companion object {
        private const val KEY_DATABASE = "database"
        private const val SCHEMA_VERSION = 9
    }

    fun read(): StorageRead<AppData> = try {
        if (!prefs.contains(KEY_DATABASE)) StorageRead.Missing
        else StorageRead.Loaded(fromJson(JSONObject(requireNotNull(prefs.getString(KEY_DATABASE, null)))))
    } catch (failure: Exception) {
        StorageRead.Failed(failure)
    }

    fun load(): AppData = read().requireLoaded()

    /** Runtime must first verify that neither V11 data nor its migration marker exists. */
    internal fun initializeFirstInstall(): AppData {
        check(prefs.all.isEmpty()) { "Legacy preferences exist without a database" }
        val fresh = defaultData()
        check(prefs.edit().putString(KEY_DATABASE, toJson(fresh).toString()).commit()) {
            "Unable to persist initial workplace IDs"
        }
        return fresh
    }

    fun save(data: AppData) {
        read().requireLoaded()
        writeValidated(data)
    }

    private fun writeValidated(data: AppData) {
        val raw = toJson(data).toString()
        fromJson(JSONObject(raw))
        check(prefs.edit().putString(KEY_DATABASE, raw).commit()) { "Unable to persist legacy database" }
    }

    fun exportJson(data: AppData): String =
        toJson(data).toString(2)

    fun parseImport(raw: String): AppData = fromJson(JSONObject(raw))

    fun importJson(raw: String): AppData {
        val data = fromJson(JSONObject(raw))
        // Explicit, validated user import may repair a failed store; ordinary saves may not.
        writeValidated(data)
        return data
    }

    private fun defaultData(): AppData {
        return AppData(
            workplaces = listOf(
                Workplace(
                    id = UUID.randomUUID().toString(),
                    name = "مطامير",
                    colorHex = "#9DD8AE"
                ),
                Workplace(
                    id = UUID.randomUUID().toString(),
                    name = "الشاملة",
                    colorHex = "#FFE28A"
                ),
                Workplace(
                    id = UUID.randomUUID().toString(),
                    name = "البنا",
                    colorHex = "#D7ACE8"
                ),
                Workplace(
                    id = UUID.randomUUID().toString(),
                    name = "الماستر",
                    colorHex = "#91C1EE"
                )
            ),
            shifts = emptyList(),
            payments = emptyList(),
            dayDetails = emptyList(),
            templates = emptyList()
        )
    }

    private fun toJson(data: AppData): JSONObject {
        return JSONObject().apply {
            put("schemaVersion", SCHEMA_VERSION)

            put("workplaces", JSONArray().apply {
                data.workplaces.forEach { place ->
                    put(JSONObject().apply {
                        put("id", place.id)
                        put("name", place.name)
                        put("colorHex", place.colorHex)
                        put("fullDayRate", place.fullDayRate)
                        put("longRate", place.longRate)
                        put("nightRate", place.nightRate)
                        put("morningRate", place.morningRate)
                        put("eveningRate", place.eveningRate)
                    })
                }
            })

            put("shifts", JSONArray().apply {
                data.shifts.forEach { shift ->
                    put(JSONObject().apply {
                        put("date", shift.date)
                        put("type", shift.type.name)

                        put("workplaceIds", JSONArray().apply {
                            shift.workplaceIds.forEach { id ->
                                if (id == null) {
                                    put(JSONObject.NULL)
                                } else {
                                    put(id)
                                }
                            }
                        })

                        put("note", shift.note)

                        if (shift.customAmount == null) {
                            put("customAmount", JSONObject.NULL)
                        } else {
                            put("customAmount", shift.customAmount)
                        }
                    })
                }
            })

            put("payments", JSONArray().apply {
                data.payments.forEach { payment ->
                    put(JSONObject().apply {
                        put("id", payment.id)
                        put("date", payment.date)
                        put("amount", payment.amount)
                        put("note", payment.note)
                    })
                }
            })

            put("dayDetails", JSONArray().apply {
                data.dayDetails.forEach { details ->
                    put(JSONObject().apply {
                        put("date", details.date)
                        put("note", details.note)
                        put("extraAmount", details.extraAmount)

                        put("tasks", JSONArray().apply {
                            details.tasks.forEach { task ->
                                put(JSONObject().apply {
                                    put("id", task.id)
                                    put("date", task.date)
                                    put("title", task.title)
                                    put("completed", task.completed)
                                    put("createdAt", task.createdAt)
                                })
                            }
                        })
                    })
                }
            })

            put("templates", JSONArray().apply {
                data.templates.forEach { template ->
                    put(JSONObject().apply {
                        put("id", template.id)
                        put("name", template.name)
                        put("type", template.type.name)

                        put("workplaceIds", JSONArray().apply {
                            template.workplaceIds.forEach { id ->
                                if (id == null) {
                                    put(JSONObject.NULL)
                                } else {
                                    put(id)
                                }
                            }
                        })

                        if (template.customAmount == null) {
                            put("customAmount", JSONObject.NULL)
                        } else {
                            put("customAmount", template.customAmount)
                        }
                    })
                }
            })
        }
    }

    private fun validateShift(shift: JSONObject) {
        ShiftType.valueOf(shift.nonBlankString("type"))
        val ids = shift.get("workplaceIds") as? JSONArray ?: error("Invalid workplaceIds")
        for (i in 0 until ids.length()) require(ids.isNull(i) || ids.get(i) is String) { "Invalid workplace ID" }
    }

    private fun fromJson(root: JSONObject): AppData {
        val version = if (root.has("schemaVersion")) root.schemaVersion(setOf(9)) else null
        root.objectArray("workplaces").forEach { it.nonBlankString("id") }
        root.objectArray("payments").forEach { it.nonBlankString("id") }
        root.objectArray("dayDetails", version == 9).forEach { details ->
            details.objectArray("tasks").forEach { it.nonBlankString("id") }
        }
        root.objectArray("templates", version == 9).forEach { it.nonBlankString("id"); validateShift(it) }
        root.objectArray("shifts").forEach { shift ->
            java.time.LocalDate.parse(shift.nonBlankString("date"))
            validateShift(shift)
        }


        val workplaces = mutableListOf<Workplace>()
        val workplaceArray = root.optJSONArray("workplaces") ?: JSONArray()

        for (i in 0 until workplaceArray.length()) {
            val obj = workplaceArray.getJSONObject(i)

            workplaces += Workplace(
                id = obj.optString(
                    "id",
                    UUID.randomUUID().toString()
                ),
                name = obj.optString("name", "مكان عمل"),
                colorHex = obj.optString(
                    "colorHex",
                    "#9DD8AE"
                ),
                fullDayRate = obj.optDouble(
                    "fullDayRate",
                    0.0
                ),
                longRate = obj.optDouble(
                    "longRate",
                    0.0
                ),
                nightRate = obj.optDouble(
                    "nightRate",
                    0.0
                ),
                morningRate = obj.optDouble(
                    "morningRate",
                    0.0
                ),
                eveningRate = obj.optDouble(
                    "eveningRate",
                    0.0
                )
            )
        }

        val shifts = mutableListOf<ShiftEntry>()
        val shiftArray = root.optJSONArray("shifts") ?: JSONArray()

        for (i in 0 until shiftArray.length()) {
            val obj = shiftArray.getJSONObject(i)

            val ids = mutableListOf<String?>()
            val idsArray =
                obj.optJSONArray("workplaceIds") ?: JSONArray()

            for (j in 0 until idsArray.length()) {
                ids += if (idsArray.isNull(j)) {
                    null
                } else {
                    idsArray.optString(j, null)
                }
            }

            val type = try {
                ShiftType.valueOf(
                    obj.optString(
                        "type",
                        ShiftType.OFF.name
                    )
                )
            } catch (_: Exception) {
                ShiftType.OFF
            }

            shifts += ShiftEntry(
                date = obj.optString("date"),
                type = type,
                workplaceIds = ids,
                note = obj.optString("note", ""),
                customAmount =
                    if (
                        obj.has("customAmount") &&
                        !obj.isNull("customAmount")
                    ) {
                        obj.optDouble("customAmount")
                    } else {
                        null
                    }
            )
        }

        val payments = mutableListOf<Payment>()
        val paymentArray =
            root.optJSONArray("payments") ?: JSONArray()

        for (i in 0 until paymentArray.length()) {
            val obj = paymentArray.getJSONObject(i)

            payments += Payment(
                id = obj.optString(
                    "id",
                    UUID.randomUUID().toString()
                ),
                date = obj.optString("date"),
                amount = obj.optDouble("amount", 0.0),
                note = obj.optString("note", "")
            )
        }

        val dayDetails = mutableListOf<DayDetails>()
        val detailsArray =
            root.optJSONArray("dayDetails") ?: JSONArray()

        for (i in 0 until detailsArray.length()) {
            val obj = detailsArray.getJSONObject(i)
            val date = obj.optString("date")
            val tasks = mutableListOf<DayTask>()

            val taskArray =
                obj.optJSONArray("tasks") ?: JSONArray()

            for (j in 0 until taskArray.length()) {
                val taskObj = taskArray.getJSONObject(j)

                tasks += DayTask(
                    id = taskObj.optString(
                        "id",
                        UUID.randomUUID().toString()
                    ),
                    date = taskObj.optString(
                        "date",
                        date
                    ),
                    title = taskObj.optString(
                        "title",
                        ""
                    ),
                    completed =
                        taskObj.optBoolean(
                            "completed",
                            false
                        ),
                    createdAt =
                        taskObj.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                )
            }

            dayDetails += DayDetails(
                date = date,
                note = obj.optString("note", ""),
                tasks = tasks,
                extraAmount =
                    obj.optDouble(
                        "extraAmount",
                        0.0
                    )
            )
        }

        val templates = mutableListOf<ShiftTemplate>()
        val templateArray =
            root.optJSONArray("templates") ?: JSONArray()

        for (i in 0 until templateArray.length()) {
            val obj = templateArray.getJSONObject(i)

            val ids = mutableListOf<String?>()
            val idsArray =
                obj.optJSONArray("workplaceIds") ?: JSONArray()

            for (j in 0 until idsArray.length()) {
                ids += if (idsArray.isNull(j)) {
                    null
                } else {
                    idsArray.optString(j, null)
                }
            }

            val type = try {
                ShiftType.valueOf(
                    obj.optString(
                        "type",
                        ShiftType.FULL_DAY.name
                    )
                )
            } catch (_: Exception) {
                ShiftType.FULL_DAY
            }

            templates += ShiftTemplate(
                id = obj.optString(
                    "id",
                    UUID.randomUUID().toString()
                ),
                name = obj.optString(
                    "name",
                    "قالب"
                ),
                type = type,
                workplaceIds = ids,
                customAmount =
                    if (
                        obj.has("customAmount") &&
                        !obj.isNull("customAmount")
                    ) {
                        obj.optDouble("customAmount")
                    } else {
                        null
                    }
            )
        }

        return AppData(
            workplaces = workplaces,
            shifts = shifts,
            payments = payments,
            dayDetails = dayDetails,
            templates = templates
        )
    }
}
