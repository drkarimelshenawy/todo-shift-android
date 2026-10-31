package com.todoshift.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AppStorage(context: Context) {

    private val prefs =
        context.getSharedPreferences("todo_shift_data", Context.MODE_PRIVATE)

    fun load(): AppData {
        val raw = prefs.getString("database", null)
            ?: return defaultData()

        return try {
            fromJson(JSONObject(raw))
        } catch (_: Exception) {
            defaultData()
        }
    }

    fun save(data: AppData) {
        prefs.edit()
            .putString("database", toJson(data).toString())
            .apply()
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
            payments = emptyList()
        )
    }

    private fun toJson(data: AppData): JSONObject {
        val root = JSONObject()

        val workplaces = JSONArray()
        data.workplaces.forEach { w ->
            workplaces.put(
                JSONObject()
                    .put("id", w.id)
                    .put("name", w.name)
                    .put("colorHex", w.colorHex)
                    .put("fullDayRate", w.fullDayRate)
                    .put("longRate", w.longRate)
                    .put("nightRate", w.nightRate)
                    .put("morningRate", w.morningRate)
                    .put("eveningRate", w.eveningRate)
            )
        }

        val shifts = JSONArray()
        data.shifts.forEach { s ->
            val ids = JSONArray()
            s.workplaceIds.forEach { id ->
                if (id == null) ids.put(JSONObject.NULL)
                else ids.put(id)
            }

            shifts.put(
                JSONObject()
                    .put("date", s.date)
                    .put("type", s.type.name)
                    .put("workplaceIds", ids)
                    .put("note", s.note)
                    .put(
                        "customAmount",
                        s.customAmount ?: JSONObject.NULL
                    )
            )
        }

        val payments = JSONArray()
        data.payments.forEach { p ->
            payments.put(
                JSONObject()
                    .put("id", p.id)
                    .put("date", p.date)
                    .put("amount", p.amount)
                    .put("note", p.note)
            )
        }

        root.put("workplaces", workplaces)
        root.put("shifts", shifts)
        root.put("payments", payments)

        return root
    }

    private fun fromJson(root: JSONObject): AppData {

        val workplaces = mutableListOf<Workplace>()
        val workplaceArray = root.optJSONArray("workplaces") ?: JSONArray()

        for (i in 0 until workplaceArray.length()) {
            val w = workplaceArray.getJSONObject(i)

            workplaces += Workplace(
                id = w.getString("id"),
                name = w.getString("name"),
                colorHex = w.getString("colorHex"),
                fullDayRate = w.optDouble("fullDayRate", 0.0),
                longRate = w.optDouble("longRate", 0.0),
                nightRate = w.optDouble("nightRate", 0.0),
                morningRate = w.optDouble("morningRate", 0.0),
                eveningRate = w.optDouble("eveningRate", 0.0)
            )
        }

        val shifts = mutableListOf<ShiftEntry>()
        val shiftArray = root.optJSONArray("shifts") ?: JSONArray()

        for (i in 0 until shiftArray.length()) {
            val s = shiftArray.getJSONObject(i)
            val idsArray = s.getJSONArray("workplaceIds")
            val ids = mutableListOf<String?>()

            for (j in 0 until idsArray.length()) {
                ids += if (idsArray.isNull(j)) null
                else idsArray.getString(j)
            }

            shifts += ShiftEntry(
                date = s.getString("date"),
                type = ShiftType.valueOf(s.getString("type")),
                workplaceIds = ids,
                note = s.optString("note", ""),
                customAmount =
                    if (s.isNull("customAmount")) null
                    else s.getDouble("customAmount")
            )
        }

        val payments = mutableListOf<Payment>()
        val paymentArray = root.optJSONArray("payments") ?: JSONArray()

        for (i in 0 until paymentArray.length()) {
            val p = paymentArray.getJSONObject(i)

            payments += Payment(
                id = p.getString("id"),
                date = p.getString("date"),
                amount = p.getDouble("amount"),
                note = p.optString("note", "")
            )
        }

        return AppData(
            workplaces = workplaces,
            shifts = shifts,
            payments = payments
        )
    }
}
