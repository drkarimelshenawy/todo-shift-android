package com.todoshift.cashbook.ui
import com.todoshift.cashbook.core.Ledger
import org.json.JSONArray
import org.json.JSONObject

internal fun encodeExtras(items: List<Ledger.Item>, bills: List<Ledger.Bill>): String = JSONObject().apply {
    put("items", JSONArray().apply { items.forEach { put(JSONObject().put("name", it.name).put("quantity", it.quantity).put("unit", it.unitMinor.toString())) } })
    put("bills", JSONArray().apply { bills.forEach { put(JSONObject().put("name", it.name).put("uri", it.uri)) } })
}.toString()
internal fun draftItems(json: String): List<Ledger.Item> = JSONObject(json).getJSONArray("items").let { a ->
    (0 until a.length()).map { a.getJSONObject(it).let { o -> Ledger.Item(o.getString("name"), o.getInt("quantity"), o.getString("unit").toLong()) } }
}
internal fun draftBills(json: String): List<Ledger.Bill> = JSONObject(json).getJSONArray("bills").let { a ->
    (0 until a.length()).map { a.getJSONObject(it).let { o -> Ledger.Bill(o.getString("name"), o.getString("uri")) } }
}
internal fun itemTotal(items: List<Ledger.Item>): Long = items.fold(0L) { n, item -> Math.addExact(n, item.totalMinor()) }
