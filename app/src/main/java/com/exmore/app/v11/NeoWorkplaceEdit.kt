package com.exmore.app.v11

import com.exmore.app.data.Workplace
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal object NeoWorkplaceEdit {
    fun apply(db:V11Database, original:Workplace?, place:Workplace, monthly:Double?, effectiveDate:String):V11Database {
        val date=LocalDate.parse(effectiveDate);val month=YearMonth.from(date).toString()
        require(monthly==null || monthly.isFinite()&&monthly>=0)
        var next=NeoWorkplaceSources.ensure(db,listOf(place))
        val candidates=next.incomeSources.filter {it.workplaceId==place.id}
        require(candidates.size==1) {"Multiple income sources are linked to this workplace; review them before changing compensation"}
        val source=candidates.single()
        if(source.fixedMonthlyAmount!=monthly) {
            require(date.dayOfMonth==1 || YearMonth.from(date)==YearMonth.now()) {"Monthly compensation periods start at a month boundary"}
            // An empty past history means shift-based earnings, never a retroactive fixed salary.
            val history=source.compensationHistory.ifEmpty { listOf(FixedSalaryPeriodV11("0001-01",source.fixedMonthlyAmount ?: 0.0,source.fixedMonthlyAmount==null)) }
            next=V11AccountsController.updateIncomeSource(next,source.copy(type=if(monthly==null) IncomeSourceTypeV11.SHIFT_BASED else IncomeSourceTypeV11.FIXED_SALARY,fixedMonthlyAmount=monthly,compensationHistory=history),month)
            // Controller owns history; explicitly retain the legacy shift-based sentinel.
            next=next.copy(incomeSources=next.incomeSources.map {if(it.id==source.id) it.copy(compensationHistory=(history.filterNot {h->h.effectiveMonth==month}+FixedSalaryPeriodV11(month,monthly ?: 0.0,monthly==null)).sortedBy {h->h.effectiveMonth}) else it})
        }
        fun rate(p:Workplace,k:ShiftKind)=when(k){ShiftKind.FULL_DAY->p.fullDayRate;ShiftKind.LONG->p.longRate;ShiftKind.NIGHT->p.nightRate;ShiftKind.MORNING->p.morningRate;ShiftKind.EVENING->p.eveningRate;ShiftKind.OFF->0.0}
        // Monthly salary and shift-rate history are independent. Never write hidden rates.
        if (monthly == null) ShiftKind.entries.filter {it!=ShiftKind.OFF}.forEach {kind ->
            val amount=rate(place,kind);require(amount.isFinite()&&amount>=0)
            val current=next.rates.filter {it.workplaceId==place.id&&it.shiftKind==kind&&it.effectiveFrom<=effectiveDate}.maxByOrNull {it.effectiveFrom}
            if(original==null || current==null || current.amount!=amount) {
                require(next.monthSnapshots.none {it.reopenedAt==null&&it.month>=month}) {"Reopen the closed month before correcting historical rates"}
                val same=next.rates.firstOrNull {it.workplaceId==place.id&&it.shiftKind==kind&&it.effectiveFrom==effectiveDate}
                val entry=RateHistoryEntryV11(same?.id ?: UUID.randomUUID().toString(),place.id,kind,effectiveDate,amount)
                next=next.copy(rates=next.rates.filterNot {it.id==entry.id}+entry,auditLog=next.auditLog+AuditEntryV11(UUID.randomUUID().toString(),"DATED_RATE_ADD","WORKPLACE_KIND","${place.id}|${kind.name}","Explicit workplace rate effective $effectiveDate; prior periods retained"))
            }
        }
        return next
    }
}
