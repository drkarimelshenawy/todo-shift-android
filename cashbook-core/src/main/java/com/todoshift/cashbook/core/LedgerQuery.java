package com.todoshift.cashbook.core;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Read-only period/search views. Row balances always include all live history, not search matches. */
public final class LedgerQuery {
    public enum Period { ALL, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
    public static final class Filter {
        public final LocalDate from, to;
        public final String search;
        public final Ledger.Kind kind;
        public final boolean deleted, descending;
        public Filter(LocalDate from, LocalDate to, String search, Ledger.Kind kind, boolean deleted, boolean descending) {
            if(from!=null && to!=null && from.isAfter(to)) throw new IllegalArgumentException("Invalid range");
            this.from=from;this.to=to;this.search=search==null?"":search.trim().toLowerCase(Locale.ROOT);
            this.kind=kind;this.deleted=deleted;this.descending=descending;
        }
        public static Filter period(Period period,LocalDate anchor,DayOfWeek firstDay,String search,Ledger.Kind kind,boolean deleted,boolean descending) {
            LocalDate from=null,to=null;
            switch(period) {
                case DAILY: from=anchor;to=anchor;break;
                case WEEKLY: from=anchor.with(TemporalAdjusters.previousOrSame(firstDay));to=from.plusDays(6);break;
                case MONTHLY: from=anchor.withDayOfMonth(1);to=anchor.withDayOfMonth(anchor.lengthOfMonth());break;
                case YEARLY: from=anchor.withDayOfYear(1);to=anchor.withDayOfYear(anchor.lengthOfYear());break;
                case CUSTOM: throw new IllegalArgumentException("Custom range requires explicit dates");
                default: break;
            }
            return new Filter(from,to,search,kind,deleted,descending);
        }
        boolean matches(Ledger.Entry e) {
            LocalDate day=LocalDate.parse(e.date);
            return e.deleted==deleted && (from==null||!day.isBefore(from)) && (to==null||!day.isAfter(to))
                && (kind==null||e.kind==kind) && (search.isEmpty()||e.note.toLowerCase(Locale.ROOT).contains(search));
        }
    }
    public static final class Row {
        public final Ledger.Entry entry;
        /** null for deleted entries; a cancelled operation has no live running balance. */
        public final Long runningMinor;
        Row(Ledger.Entry entry,Long runningMinor) {this.entry=entry;this.runningMinor=runningMinor;}
    }
    public static final class View {
        public final List<Row> rows;
        public final long incomeMinor,expenseMinor,transferInMinor,transferOutMinor,currentBalanceMinor;
        View(List<Row> rows,long income,long expense,long tin,long tout,long balance) {
            this.rows=Collections.unmodifiableList(rows);incomeMinor=income;expenseMinor=expense;
            transferInMinor=tin;transferOutMinor=tout;currentBalanceMinor=balance;
        }
        public long netMinor() {return Math.subtractExact(incomeMinor,expenseMinor);}
    }
    public static View account(Ledger.Snapshot s,String accountId,Filter filter) {
        Ledger.Account account=null;
        for(Ledger.Account a:s.accounts) if(a.id.equals(accountId))account=a;
        if(account==null)throw new IllegalArgumentException("Unknown account");
        List<Ledger.Entry> ordered=new ArrayList<>(s.entries);
        // TimSort is stable: equal date/time uses original insertion order.
        ordered.sort(Comparator.comparing((Ledger.Entry e)->e.date).thenComparing(e->e.time));
        long running=account.openingDate.isEmpty()?account.openingMinor:0,income=0,expense=0,tin=0,tout=0;
        boolean openingApplied=account.openingDate.isEmpty();
        List<Row> rows=new ArrayList<>();
        for(Ledger.Entry e:ordered) {
            boolean source=e.accountId.equals(accountId),target=e.kind==Ledger.Kind.TRANSFER&&e.targetId.equals(accountId);
            if(!source&&!target)continue;
            if(!openingApplied && e.date.compareTo(account.openingDate)>=0) {running=Math.addExact(running,account.openingMinor);openingApplied=true;}
            if(!e.deleted)running=Math.addExact(running,source?(e.kind==Ledger.Kind.INCOME || e.kind==Ledger.Kind.SETTLEMENT&&s.debts.stream().anyMatch(d->d.accountId.equals(accountId)&&d.direction==Debt.Direction.PAYABLE)?e.minor:-e.minor):e.minor);
            if(!filter.matches(e))continue;
            rows.add(new Row(e,e.deleted?null:running));
            // Trash view intentionally does not contribute cancelled amounts to live totals.
            if(!e.deleted) {
                if(e.kind==Ledger.Kind.INCOME)income=Math.addExact(income,e.minor);
                else if(e.kind==Ledger.Kind.EXPENSE)expense=Math.addExact(expense,e.minor);
                else if(source)tout=Math.addExact(tout,e.minor);else tin=Math.addExact(tin,e.minor);
            }
        }
        if(filter.descending)Collections.reverse(rows);
        return new View(rows,income,expense,tin,tout,s.balance(accountId));
    }
    private LedgerQuery(){}
}
