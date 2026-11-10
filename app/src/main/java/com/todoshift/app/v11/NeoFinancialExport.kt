package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4DataGate
import java.io.OutputStream

/** Recheck live authorization after a provider returns, before exporting any financial bytes. */
internal object NeoFinancialExport {
    fun write(authorized: () -> Boolean, open: () -> OutputStream?, csv: String) =
        synchronized(Neo4DataGate.monitor) {
            check(authorized()) { "Financial verification required" }
            val stream = open() ?: error("Cannot open statement destination")
            stream.bufferedWriter(Charsets.UTF_8).use { writer ->
                check(authorized()) { "Financial verification required" }
                writer.write("\uFEFF")
                writer.write(csv)
            }
        }
}
