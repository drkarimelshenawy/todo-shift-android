package com.todoshift.app.v11

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/** All mutations return a candidate snapshot. UI must review it and use saveIfCurrent. */
object ModernOperations {
    fun canUndo(current: V11Database, previous: V11Database, financeAuthorized: Boolean): Boolean {
        if (current.monthSnapshots != previous.monthSnapshots) return false
        return financeAuthorized ||
            (current.rates == previous.rates &&
                current.incomeSources == previous.incomeSources &&
                current.paymentMethods == previous.paymentMethods &&
                current.payments == previous.payments &&
                current.settlements == previous.settlements &&
                current.obligations == previous.obligations &&
                current.obligationPayments == previous.obligationPayments)
    }

    fun distributePayment(
        db: V11Database,
        paymentId: String,
        months: List<YearMonth>,
    ): V11Database {
        require(months.isNotEmpty() && months.distinct().size == months.size)
        val payment = db.payments.single { it.id == paymentId }
        val cents = LocalizedNumbers.cents(payment.amount)
        val base = cents / months.size
        val remainder = cents % months.size
        val allocations =
            months.mapIndexed { index, month ->
                PaymentAllocationV11(
                    UUID.randomUUID().toString(),
                    month.toString(),
                    payment.sourceId,
                    LocalizedNumbers.amount(base + if (index < remainder) 1 else 0),
                )
            }
        return db.copy(
            payments =
                db.payments.map {
                    if (it.id == paymentId) it.copy(allocations = allocations) else it
                },
            auditLog =
                db.auditLog +
                    AuditEntryV11(
                        UUID.randomUUID().toString(),
                        "PAYMENT_ALLOCATE",
                        "payment",
                        paymentId,
                        "Allocated across ${months.joinToString()}; total unchanged",
                    ),
        )
    }

    fun resizedAllocations(
        allocations: List<PaymentAllocationV11>,
        amount: Double,
    ): List<PaymentAllocationV11> {
        if (allocations.isEmpty()) return allocations
        val weights = allocations.map { LocalizedNumbers.cents(it.amount) }
        require(weights.all { it >= 0 } && weights.sum() > 0)
        val total = weights.sum().toBigInteger()
        val newTotal = LocalizedNumbers.cents(amount).toBigInteger()
        val pairs = weights.map { newTotal.multiply(it.toBigInteger()).divideAndRemainder(total) }
        val values =
            pairs.map { it[0].also { v -> require(v.bitLength() <= 63) }.toLong() }.toMutableList()
        val remainder = newTotal.toLong() - values.sum()
        pairs.indices
            .sortedByDescending { pairs[it][1] }
            .take(remainder.toInt())
            .forEach { values[it]++ }
        return allocations.mapIndexed { index, a ->
            a.copy(amount = LocalizedNumbers.amount(values[index]))
        }
    }

    data class CopyPreview(
        val candidate: V11Database,
        val conflicts: List<String>,
        val copied: Int,
    )

    fun previewCopy(
        db: V11Database,
        from: LocalDate,
        to: LocalDate,
        destination: LocalDate,
    ): CopyPreview {
        require(to >= from && java.time.temporal.ChronoUnit.DAYS.between(from, to) <= 366)
        val delta = java.time.temporal.ChronoUnit.DAYS.between(from, destination)
        require(delta != 0L) { "Choose a different destination" }
        var next = db
        val conflicts = mutableListOf<String>()
        var count = 0
        db.days
            .filter { LocalDate.parse(it.date) in from..to }
            .forEach { day ->
                val date = LocalDate.parse(day.date).plusDays(delta).toString()
                require(
                    db.monthSnapshots.none { it.month == date.take(7) && it.reopenedAt == null }
                ) {
                    "Destination month is closed"
                }
                day.segments.forEach { old ->
                    val target =
                        next.days.firstOrNull { it.date == date && it.calendarId == day.calendarId }
                    val fresh = old.copy(id = UUID.randomUUID().toString())
                    val collision =
                        target?.segments.orEmpty().any {
                            it.timeRange()?.let { a -> fresh.timeRange()?.overlaps(a) } == true
                        }
                    if (collision) conflicts += "$date • ${old.kind}"
                    else {
                        next =
                            next.copy(
                                days =
                                    if (target == null)
                                        next.days +
                                            DayEntryV11(
                                                UUID.randomUUID().toString(),
                                                date,
                                                day.calendarId,
                                                listOf(fresh),
                                            )
                                    else
                                        next.days.map {
                                            if (it.id == target.id)
                                                it.copy(segments = it.segments + fresh)
                                            else it
                                        }
                            )
                        count++
                    }
                }
            }
        return CopyPreview(next, conflicts, count)
    }

    data class Stats(val shifts: Int, val workedDays: Int, val off: Int, val earnings: Double)

    fun stats(db: V11Database, month: YearMonth): Stats {
        val days = db.days.filter { it.date.startsWith(month.toString()) }
        return Stats(
            days.sumOf { d -> d.segments.count { it.kind != ShiftKind.OFF } },
            days
                .filter { d -> d.segments.any { it.kind != ShiftKind.OFF } }
                .map { it.date }
                .distinct()
                .size,
            days.sumOf { d -> d.segments.count { it.kind == ShiftKind.OFF } },
            V11FinanceEngine.month(db, month).expected,
        )
    }

    data class SearchResult(val type: String, val id: String, val date: String?, val text: String)

    fun search(db: V11Database, query: String, finance: Boolean): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val q = query.trim()
        return (db.tasks.map { SearchResult("TASK", it.id, it.date, it.title + " " + it.details) } +
                db.dayItems.map {
                    SearchResult(
                        if (it.type == DayItemTypeV11.NOTE) "NOTE" else "EVENT",
                        it.id,
                        it.date,
                        it.title + " " + it.details,
                    )
                } +
                db.days.flatMap { d ->
                    d.segments.map {
                        SearchResult(
                            "SHIFT",
                            it.id,
                            d.date,
                            "${it.kind} ${it.workplaceId.orEmpty()} ${it.note}",
                        )
                    }
                } +
                if (finance)
                    db.payments.map {
                        SearchResult("PAYMENT", it.id, it.receivedDate, "${it.note} ${it.amount}")
                    }
                else emptyList())
            .filter { it.text.contains(q, true) || it.date?.contains(q) == true }
    }

    fun health(db: V11Database): List<String> = buildList {
        val calendarIds = db.calendars.map { it.id }.toSet()
        db.days
            .filter { it.calendarId !in calendarIds }
            .forEach { add("Unknown calendar on ${it.date}; no automatic reassignment") }
        db.days.forEach { day ->
            day.segments.forEach { s ->
                if (s.kind != ShiftKind.OFF && s.workplaceId.isNullOrBlank())
                    add("Missing workplace: ${day.date} / ${s.id}")
                if (s.exceptionalAmount?.isFinite() == false) add("Invalid amount: ${s.id}")
            }
        }
        db.payments
            .filter { p ->
                p.allocations.isNotEmpty() &&
                    p.allocations.sumOf { LocalizedNumbers.cents(it.amount) } !=
                        LocalizedNumbers.cents(p.amount)
            }
            .forEach { add("Allocation mismatch: ${it.id}; review manually") }
        val occurrences = neoShiftOccurrences(db)
        occurrences
            .filter { neoHasConflict(it, occurrences) }
            .forEach { add("Overlap: ${it.entry.date} / ${it.segment.id}") }
    }

    fun repairPaidStatuses(db: V11Database): V11Database {
        val corrected =
            db.obligations
                .filter {
                    it.status == ObligationStatusV11.ACTIVE &&
                        it.totalAmount != null &&
                        it.totalAmount > 0 &&
                        db.obligationPayments
                            .filter { p -> p.obligationId == it.id }
                            .sumOf { p -> LocalizedNumbers.cents(p.amount) } >=
                            LocalizedNumbers.cents(it.totalAmount)
                }
                .map { it.id }
                .toSet()
        if (corrected.isEmpty()) return db
        return db.copy(
            obligations =
                db.obligations.map {
                    if (it.id in corrected) it.copy(status = ObligationStatusV11.PAID) else it
                },
            auditLog =
                db.auditLog +
                    AuditEntryV11(
                        UUID.randomUUID().toString(),
                        "HEALTH_REPAIR",
                        "OBLIGATION",
                        corrected.joinToString(),
                        "User-approved status repair; amounts unchanged",
                    ),
        )
    }

    fun statementCsv(db: V11Database, workplace: String, from: LocalDate, to: LocalDate): String {
        require(to >= from)
        val rows =
            db.days
                .filter { LocalDate.parse(it.date) in from..to }
                .flatMap { day ->
                    day.segments
                        .filter { it.workplaceId == workplace }
                        .map { s ->
                            listOf(
                                day.date,
                                s.id,
                                s.kind.name,
                                s.timeRange()?.startMinute?.toString().orEmpty(),
                                s.timeRange()?.endMinute?.toString().orEmpty(),
                                V11RateEngine.segmentAmount(s, day.date, db.rates)
                                    .toBigDecimal()
                                    .setScale(2, java.math.RoundingMode.HALF_UP)
                                    .toPlainString(),
                            )
                        }
                }
        val paymentRows =
            db.payments
                .filter { LocalDate.parse(it.receivedDate) in from..to }
                .mapNotNull { payment ->
                    val cents =
                        if (payment.allocations.isEmpty()) {
                            if (
                                db.incomeSources
                                    .firstOrNull { it.id == payment.sourceId }
                                    ?.workplaceId == workplace
                            )
                                LocalizedNumbers.cents(payment.amount)
                            else 0L
                        } else
                            payment.allocations
                                .filter { a ->
                                    db.incomeSources
                                        .firstOrNull { it.id == a.sourceId }
                                        ?.workplaceId == workplace
                                }
                                .sumOf { LocalizedNumbers.cents(it.amount) }
                    if (cents == 0L) null
                    else
                        listOf(
                            "RECEIPT",
                            payment.receivedDate,
                            payment.id,
                            "",
                            "",
                            "",
                            "",
                            LocalizedNumbers.amount(cents).toString(),
                        )
                }
        val csvRows = rows.map { listOf("SHIFT") + it + listOf("") } + paymentRows
        return "record,date,id,type,start_minute,end_minute,earned,received\r\n" +
            csvRows.joinToString("\r\n") { row ->
                row.joinToString(",") { value ->
                    val safe =
                        if (value.firstOrNull() in listOf('=', '+', '-', '@')) "'" + value
                        else value
                    "\"" + safe.replace("\"", "\"\"") + "\""
                }
            }
    }
}
