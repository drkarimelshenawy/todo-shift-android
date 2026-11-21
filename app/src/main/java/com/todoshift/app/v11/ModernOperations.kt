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
        require(payment.reversedAt == null) { "Reversed receipts cannot be allocated" }
        val affectedMonths = (months.map { it.toString() } + payment.accrualMonth + payment.allocations.map { it.accrualMonth }).toSet()
        require(db.monthSnapshots.none { it.month in affectedMonths && it.reopenedAt == null }) {
            "Reopen the earning period before reallocating this receipt"
        }
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
        return (db.tasks.filterNot {it.linkedNotePrivateV11(db)}.map { SearchResult("TASK", it.id, it.date, it.title + " " + it.details) } +
                db.dayItems.filter { !it.notePrivate && it.noteVisible }.map {
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

    /**
     * Export a workplace's *earning period*, not the transaction-date cash book.
     * An allocation belongs to exactly one earning month even when received later.
     * Preserve the actual receipt date in a separate column for reconciliation.
     */
    fun statementCsv(db: V11Database, workplace: String, from: LocalDate, to: LocalDate): String {
        require(!to.isBefore(from)) { "Invalid statement period" }
        val months = generateSequence(YearMonth.from(from)) { it.plusMonths(1) }
            .takeWhile { !it.isAfter(YearMonth.from(to)) }.take(121).toList()
        require(months.size <= 120) { "Statement period is too long" }
        val sourceIds = db.incomeSources.filter { it.workplaceId == workplace }.map { it.id }.toSet()
        fun monthlySources(month: YearMonth) = db.incomeSources.filter { source ->
            source.workplaceId == workplace &&
                (source.fixedMonthlyAmount != null || source.compensationHistory.isNotEmpty()) &&
                if (source.compensationHistory.isEmpty()) source.active
                else source.compensationHistory.filter { period ->
                    !YearMonth.parse(period.effectiveMonth).isAfter(month)
                }.maxByOrNull { it.effectiveMonth }?.shiftBased == false
        }
        val salariedByMonth = months.associateWith { monthlySources(it) }
        fun row(
            kind: String, date: String, id: String, type: String,
            start: String = "", end: String = "", earned: String = "",
            received: String = "", adjusted: String = "",
            accrualMonth: String = "", receiptDate: String = "",
        ) = listOf(kind, date, id, type, start, end, earned, received,
            adjusted, accrualMonth, receiptDate)
        fun plainMoney(value: Double) =
            value.toBigDecimal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()

        val shiftRows = db.days.filter { LocalDate.parse(it.date) in from..to }
            .flatMap { day ->
                day.segments.filter { it.workplaceId == workplace && it.kind != ShiftKind.OFF }
                    .map { segment ->
                        val fixedSalary = salariedByMonth[YearMonth.from(LocalDate.parse(day.date))]
                            .orEmpty().isNotEmpty()
                        val amount = if (fixedSalary && segment.exceptionalAmount == null) 0.0
                            else V11RateEngine.segmentAmount(segment, day.date, db.rates)
                        row("SHIFT", day.date, segment.id, segment.kind.name,
                            segment.timeRange()?.startMinute?.toString().orEmpty(),
                            segment.timeRange()?.endMinute?.toString().orEmpty(),
                            earned = plainMoney(amount), accrualMonth = day.date.take(7))
                    }
            }

        // Fixed monthly pay is an accrual on the first day of the earning month.
        // A partial period starting later cannot silently include that accrual.
        val fixedRows = months.filter { it.atDay(1) in from..to }.flatMap { month ->
            salariedByMonth.getValue(month).map { source ->
                row("SALARY", month.atDay(1).toString(), source.id, "FIXED_MONTHLY",
                    earned = plainMoney(V11FinanceEngine.monthlyAmount(source, month)),
                    accrualMonth = month.toString())
            }
        }
        val paymentRows = db.payments.filter { it.reversedAt == null }.flatMap { payment ->
            val amounts = if (payment.allocations.isEmpty()) {
                if (payment.sourceId in sourceIds) listOf(payment.accrualMonth to LocalizedNumbers.cents(payment.amount))
                else emptyList()
            } else payment.allocations.filter { it.sourceId in sourceIds }
                .groupBy { it.accrualMonth }
                .map { (month, allocations) -> month to allocations.sumOf { LocalizedNumbers.cents(it.amount) } }
            amounts.mapNotNull { (monthText, cents) ->
                val month = runCatching { YearMonth.parse(monthText) }.getOrNull()
                if (month == null || month.atDay(1) !in from..to || cents == 0L) null
                else row("RECEIPT", payment.receivedDate, payment.id, "RECEIVED",
                    received = plainMoney(LocalizedNumbers.amount(cents)),
                    accrualMonth = monthText, receiptDate = payment.receivedDate)
            }
        }
        val adjustmentRows = db.settlements
            .filter { settlement ->
                settlement.action in setOf(
                    SettlementActionV11.WRITE_OFF,
                    SettlementActionV11.CANCEL_ENTITLEMENT,
                    SettlementActionV11.ADJUSTMENT,
                ) &&
                    (settlement.workplaceId == workplace || settlement.sourceId in sourceIds) &&
                    runCatching { YearMonth.parse(settlement.accrualMonth).atDay(1) in from..to }
                        .getOrDefault(false)
            }.map { settlement ->
                row("SETTLEMENT", YearMonth.parse(settlement.accrualMonth).atDay(1).toString(),
                    settlement.id, settlement.action.name,
                    adjusted = plainMoney(settlement.amount),
                    accrualMonth = settlement.accrualMonth)
            }

        val header = listOf("record", "date", "id", "type", "start_minute",
            "end_minute", "earned", "received", "adjusted", "accrual_month", "received_date")
        val csvRows = shiftRows + fixedRows + paymentRows + adjustmentRows
        return header.joinToString(",") + "\r\n" +
            csvRows.joinToString("\r\n") { row ->
                row.joinToString(",") { value ->
                    // Guard spreadsheet readers against user-defined formula injection.
                    val safe = if (value.firstOrNull() in listOf('=', '+', '-', '@')) "'" + value
                        else value
                    "\"" + safe.replace("\"", "\"\"") + "\""
                }
            }
    }

}
