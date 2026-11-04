package com.todoshift.app.v11

/** Visual geometry only; never consults/changes timeRange, duration, rates or stored dates. */
data class Neo4Coverage(val top: Float, val bottom: Float)
fun neo4Coverage(kind: ShiftKind): Neo4Coverage? = when (kind) {
    ShiftKind.FULL_DAY -> Neo4Coverage(0f, 1f)
    ShiftKind.LONG -> Neo4Coverage(0f, 0.5f)
    ShiftKind.NIGHT -> Neo4Coverage(0.5f, 1f)
    ShiftKind.MORNING -> Neo4Coverage(0f, 1f / 3f)
    ShiftKind.EVENING -> Neo4Coverage(1f / 3f, 2f / 3f)
    ShiftKind.OFF -> null
}
data class Neo4PaintBand(val top: Float, val bottom: Float, val owners: List<Int>)
/** All overlaps remain visible as alternating full-width bands, in stored segment order.
 * The data conflict resolver and its precedence are untouched; no entry is merged or replaced.
 */
fun neo4PaintBands(kinds: List<ShiftKind>): List<Neo4PaintBand> {
    val ranges = kinds.map(::neo4Coverage)
    val cuts = (listOf(0f, 1f) + ranges.filterNotNull().flatMap { listOf(it.top, it.bottom) }).distinct().sorted()
    return cuts.zipWithNext().mapNotNull { (top, bottom) ->
        val mid = (top + bottom) / 2f
        val owners = ranges.indices.filter { ranges[it]?.let { r -> mid >= r.top && mid < r.bottom } == true }
        if (owners.isEmpty()) null else Neo4PaintBand(top, bottom, owners)
    }
}
val neo4PaintPresets: List<Pair<String, String>> = listOf(
    "#64D5B0" to "#459ACF", "#72C7E8" to "#4168BB", "#B7A2EB" to "#7D83C7", "#F0B99C" to "#D8899B",
    "#51B797" to "#225B63", "#A3D9EE" to "#6F9BD3", "#E8CB81" to "#C58F6B", "#E3ADC5" to "#B785AC",
    "#66B4BA" to "#314F78", "#EEA58D" to "#D4778E", "#A8BE9A" to "#6D998B", "#8BA5BA" to "#4D6078",
    "#B9DCD0" to "#74AEAF", "#D9BFEA" to "#A296CF", "#E9D0B2" to "#C3988D", "#B7D3E6" to "#8B9CC5"
)
