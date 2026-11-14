package com.todoshift.cashbook.core;

import java.io.*;
import java.math.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Isolated, synchronous ledger. Call from an IO dispatcher; never shares host data. */
public final class Ledger {
    public interface Store {
        /** null means genuinely absent, errors MUST throw; never return null for a failed read. */
        byte[] read() throws IOException;
        /** Must atomically replace the entire snapshot, or throw without replacing it. */
        void write(byte[] snapshot) throws IOException;
    }
    public enum Kind { INCOME, EXPENSE, TRANSFER, SETTLEMENT }
    public static final class Account {
        public final String id, name, currency, openingDate, incomingLabel, outgoingLabel, icon, createdDate;
        public final boolean archived;
        public final long openingMinor;
        public Account(String id, String name, String currency, long openingMinor) {
            this(id,name,currency,openingMinor,"");
        }
        public Account(String id,String name,String currency,long openingMinor,String openingDate) {
            this(id,name,currency,openingMinor,openingDate,"إيراد","مصروف","");
        }
        public Account(String id,String name,String currency,long openingMinor,String openingDate,String incomingLabel,String outgoingLabel,String icon) {
            this(id,name,currency,openingMinor,openingDate,incomingLabel,outgoingLabel,icon,"",false);
        }
        public Account(String id,String name,String currency,long openingMinor,String openingDate,String incomingLabel,String outgoingLabel,String icon,String createdDate,boolean archived) {
            this.createdDate=createdDate;this.archived=archived;
            this.id=id; this.name=name; this.currency=currency; this.openingMinor=openingMinor; this.openingDate=openingDate;
            this.incomingLabel=incomingLabel;this.outgoingLabel=outgoingLabel;this.icon=icon;
        }
    }
    public static final class Item {
        public final String name; public final int quantity; public final long unitMinor;
        public Item(String name,int quantity,long unitMinor) {this.name=name;this.quantity=quantity;this.unitMinor=unitMinor;}
        public long totalMinor() {return Math.multiplyExact((long)quantity,unitMinor);}
    }
    /** Persisted document reference. File bytes/permissions belong to the Android host. */
    public static final class Bill {
        public final String name,uri;
        public Bill(String name,String uri) {this.name=name;this.uri=uri;}
    }
    public static final class Entry {
        public final String id, accountId, targetId, note, date, time;
        public final Kind kind;
        public final long minor;
        public final boolean deleted;
        public final int installmentIndex;
        public final boolean earlyCredit;
        public final List<Item> items;
        public final List<Bill> bills;
        public Entry(String id, Kind kind, String accountId, String targetId, long minor,
                     String note, String date, boolean deleted) {
            this(id,kind,accountId,targetId,minor,note,date,"",deleted);
        }
        public Entry(String id, Kind kind, String accountId, String targetId, long minor,
                     String note, String date, String time, boolean deleted) {
            this(id,kind,accountId,targetId,minor,note,date,time,deleted,Collections.emptyList(),Collections.emptyList());
        }
        public Entry(String id,Kind kind,String accountId,String targetId,long minor,String note,String date,String time,boolean deleted,List<Item> items,List<Bill> bills) {
            this(id,kind,accountId,targetId,minor,note,date,time,deleted,items,bills,-1,false);
        }
        public Entry(String id,Kind kind,String accountId,String targetId,long minor,String note,String date,String time,boolean deleted,List<Item> items,List<Bill> bills,int installmentIndex,boolean earlyCredit) {
            this.installmentIndex=installmentIndex;this.earlyCredit=earlyCredit;
            this.id=id; this.kind=kind; this.accountId=accountId; this.targetId=targetId;
            this.minor=minor; this.note=note; this.date=date; this.time=time; this.deleted=deleted;
            this.items=Collections.unmodifiableList(new ArrayList<>(items));
            this.bills=Collections.unmodifiableList(new ArrayList<>(bills));
        }
        Entry deleted(boolean value) { return new Entry(id,kind,accountId,targetId,minor,note,date,time,value,items,bills,installmentIndex,earlyCredit); }
    }
    public static final class ButtonLabels {
        public final String incoming,outgoing;
        public ButtonLabels(String incoming,String outgoing) {this.incoming=incoming;this.outgoing=outgoing;}
    }
    public static final class Preferences {
        public final List<ButtonLabels> presets;
        public final List<String> recent, favorites;
        public Preferences(List<ButtonLabels> presets,List<String> recent,List<String> favorites) {
            this.presets=Collections.unmodifiableList(new ArrayList<>(presets));
            this.recent=Collections.unmodifiableList(new ArrayList<>(recent));
            this.favorites=Collections.unmodifiableList(new ArrayList<>(favorites));
        }
        public Preferences() {this(Collections.emptyList(),Collections.emptyList(),Collections.emptyList());}
    }
    public static final class Snapshot {
        public final List<Account> accounts;
        public final List<Entry> entries;
        public final ButtonLabels defaults;
        public final Preferences preferences;
        public final List<Debt> debts;
        Snapshot(List<Account> accounts, List<Entry> entries) {this(accounts,entries,new ButtonLabels("إيراد","مصروف"));}
        Snapshot(List<Account> accounts,List<Entry> entries,ButtonLabels defaults) {
            this(accounts,entries,defaults,new Preferences());
        }
        Snapshot(List<Account> accounts,List<Entry> entries,ButtonLabels defaults,Preferences preferences) {
            this(accounts,entries,defaults,preferences,Collections.emptyList());
        }
        Snapshot(List<Account> accounts,List<Entry> entries,ButtonLabels defaults,Preferences preferences,List<Debt> debts){
            this.debts=Collections.unmodifiableList(new ArrayList<>(debts));this.preferences=preferences;this.defaults=defaults;
            this.accounts=Collections.unmodifiableList(new ArrayList<>(accounts));
            this.entries=Collections.unmodifiableList(new ArrayList<>(entries));
        }
        public long balance(String accountId) {
            Account a=account(this,accountId);
            long n=a.openingMinor;
            for(Entry e:entries) if(!e.deleted) {
                if(e.accountId.equals(accountId)) {boolean add=e.kind==Kind.INCOME || e.kind==Kind.SETTLEMENT&&debts.stream().anyMatch(d->d.accountId.equals(accountId)&&d.direction==Debt.Direction.PAYABLE);n=Math.addExact(n,add?e.minor:-e.minor);}
                if(e.kind==Kind.TRANSFER && e.targetId.equals(accountId)) n=Math.addExact(n,e.minor);
            }
            return n;
        }
        public long total(Kind kind) {
            if(kind==Kind.TRANSFER||kind==Kind.SETTLEMENT) throw new IllegalArgumentException("Transfers are not income/expense");
            long n=0;
            for(Entry e:entries) if(!e.deleted && e.kind==kind) n=Math.addExact(n,e.minor);
            return n;
        }
    }
    private final Store store;
    private Snapshot current;
    private boolean loaded;
    public Ledger(Store store) { this.store=Objects.requireNonNull(store); }
    public synchronized Snapshot load() throws IOException {
        // A failed read makes all mutations unavailable until successful explicit reload.
        loaded=false;
        byte[] bytes=store.read();
        Snapshot next=bytes==null?new Snapshot(Collections.emptyList(),Collections.emptyList()):decode(bytes);
        validate(next); current=next; loaded=true; return current;
    }
    public synchronized Snapshot snapshot() { ready(); return current; }
    private void ready() { if(!loaded) throw new IllegalStateException("Load must succeed before reading or writing"); }
    private void commit(Snapshot next) throws IOException {
        ready(); validate(next); byte[] bytes=encode(next); store.write(bytes); current=next;
    }
    public synchronized String addAccount(String name, String currency, long openingMinor) throws IOException {
        return addAccount(name,currency,openingMinor,"");
    }
    public synchronized String addAccount(String name,String currency,long openingMinor,String openingDate) throws IOException {
        ready(); return addAccount(name,currency,openingMinor,openingDate,current.defaults.incoming,current.defaults.outgoing,"");
    }
    public synchronized String addAccount(String name,String currency,long openingMinor,String openingDate,String incoming,String outgoing,String icon) throws IOException {
        return addAccount(name,currency,openingMinor,openingDate,incoming,outgoing,icon,java.time.LocalDate.now().toString());
    }
    public synchronized String addAccount(String name,String currency,long openingMinor,String openingDate,String incoming,String outgoing,String icon,String createdDate) throws IOException {
        ready(); String id=UUID.randomUUID().toString();
        List<Account> list=new ArrayList<>(current.accounts);
        list.add(new Account(id,name.trim(),currency,openingMinor,openingDate,incoming.trim(),outgoing.trim(),icon.trim(),createdDate,false));
        commit(new Snapshot(list,current.entries,current.defaults,current.preferences,current.debts)); return id;
    }
    public synchronized void renameAccount(String id,String name) throws IOException {
        ready();List<Account> list=new ArrayList<>();boolean found=false;
        for(Account a:current.accounts) {
            if(a.id.equals(id)) {list.add(new Account(a.id,name.trim(),a.currency,a.openingMinor,a.openingDate,a.incomingLabel,a.outgoingLabel,a.icon,a.createdDate,a.archived));found=true;}
            else list.add(a);
        }
        if(!found)throw new IllegalArgumentException("Unknown account");
        commit(new Snapshot(list,current.entries,current.defaults,current.preferences,current.debts));
    }
    public synchronized void customizeAccount(String id,String name,String incoming,String outgoing,String icon) throws IOException {
        ready();List<Account> list=new ArrayList<>();boolean found=false;
        for(Account a:current.accounts) {
            if(a.id.equals(id)) {list.add(new Account(a.id,name.trim(),a.currency,a.openingMinor,a.openingDate,incoming.trim(),outgoing.trim(),icon.trim(),a.createdDate,a.archived));found=true;}
            else list.add(a);
        }
        if(!found)throw new IllegalArgumentException("Unknown account");
        commit(new Snapshot(list,current.entries,current.defaults,current.preferences,current.debts));
    }
    public synchronized void editAccount(String id,String name,long opening,String openingDate,String createdDate,String incoming,String outgoing,String icon) throws IOException {
        ready();Account old=account(current,id);if(current.debts.stream().anyMatch(d->d.accountId.equals(id)) && (old.openingMinor!=opening || !old.openingDate.equals(openingDate)))throw new IllegalArgumentException("Use debt settings to change principal or start");List<Account> list=new ArrayList<>();
        for(Account a:current.accounts)list.add(a.id.equals(id)?new Account(id,name.trim(),a.currency,opening,openingDate,incoming.trim(),outgoing.trim(),icon.trim(),createdDate,a.archived):a);
        commit(new Snapshot(list,current.entries,current.defaults,current.preferences,current.debts));
    }
    public synchronized long relatedEntries(String id) {ready();account(current,id);return current.entries.stream().filter(e->e.accountId.equals(id)||e.targetId.equals(id)).count();}
    public synchronized void archiveAccount(String id,boolean archived) throws IOException {
        ready();account(current,id);List<Account> list=new ArrayList<>();
        for(Account a:current.accounts)list.add(a.id.equals(id)?new Account(a.id,a.name,a.currency,a.openingMinor,a.openingDate,a.incomingLabel,a.outgoingLabel,a.icon,a.createdDate,archived):a);
        commit(new Snapshot(list,current.entries,current.defaults,current.preferences,current.debts));
    }
    public synchronized void deleteAccount(String id) throws IOException {
        ready();if(relatedEntries(id)>0)throw new IllegalArgumentException("Account has related entries; archive instead");
        List<Account> list=new ArrayList<>(current.accounts);list.removeIf(a->a.id.equals(id));
        List<Debt> debts=new ArrayList<>(current.debts);debts.removeIf(d->d.accountId.equals(id));
        commit(new Snapshot(list,current.entries,current.defaults,current.preferences,debts));
    }
    public synchronized void savePreset(String incoming,String outgoing) throws IOException {
        ready();ButtonLabels pair=new ButtonLabels(incoming.trim(),outgoing.trim());List<ButtonLabels> list=new ArrayList<>(current.preferences.presets);
        if(list.stream().anyMatch(p->p.incoming.equals(pair.incoming)&&p.outgoing.equals(pair.outgoing)))return;
        list.add(pair);commit(new Snapshot(current.accounts,current.entries,current.defaults,new Preferences(list,current.preferences.recent,current.preferences.favorites),current.debts));
    }
    public synchronized void removePreset(int index) throws IOException {
        ready();List<ButtonLabels> list=new ArrayList<>(current.preferences.presets);list.remove(index);
        commit(new Snapshot(current.accounts,current.entries,current.defaults,new Preferences(list,current.preferences.recent,current.preferences.favorites),current.debts));
    }
    public synchronized void useIcon(String icon) throws IOException {
        ready();if(icon.trim().isEmpty())return;List<String> list=new ArrayList<>(current.preferences.recent);list.remove(icon);list.add(0,icon);if(list.size()>12)list.remove(list.size()-1);
        commit(new Snapshot(current.accounts,current.entries,current.defaults,new Preferences(current.preferences.presets,list,current.preferences.favorites),current.debts));
    }
    public synchronized void favoriteIcon(String icon) throws IOException {
        ready();List<String> list=new ArrayList<>(current.preferences.favorites);if(!list.remove(icon))list.add(icon);
        commit(new Snapshot(current.accounts,current.entries,current.defaults,new Preferences(current.preferences.presets,current.preferences.recent,list),current.debts));
    }
    public synchronized String addDebt(String name,long principal,String start,String icon,Debt spec) throws IOException {
        ready();if(principal<=0)throw new IllegalArgumentException("Principal must be positive");String id=UUID.randomUUID().toString();
        List<Account> accounts=new ArrayList<>(current.accounts);accounts.add(new Account(id,name.trim(),"EGP",spec.direction==Debt.Direction.PAYABLE?-principal:principal,start,"سداد","تحصيل",icon,java.time.LocalDate.now().toString(),false));
        List<Debt> debts=new ArrayList<>(current.debts);debts.add(spec.withId(id));commit(new Snapshot(accounts,current.entries,current.defaults,current.preferences,debts));return id;
    }
    public synchronized void configureDebt(String id,long principal,String start,Debt spec) throws IOException {
        ready();Debt old=current.debts.stream().filter(d->d.accountId.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Missing debt"));
        if(old.direction!=spec.direction)throw new IllegalArgumentException("Direction is immutable");
        if(!old.changes.isEmpty() && (!old.firstDue.equals(spec.firstDue)||old.frequency!=spec.frequency||old.installmentMinor!=spec.installmentMinor||!old.customDates.equals(spec.customDates)))throw new IllegalArgumentException("Keep postponed schedule; edit individual due dates");
        List<Account> accounts=new ArrayList<>();for(Account a:current.accounts)accounts.add(a.id.equals(id)?new Account(a.id,a.name,a.currency,old.direction==Debt.Direction.PAYABLE?-principal:principal,start,a.incomingLabel,a.outgoingLabel,a.icon,a.createdDate,a.archived):a);
        List<Debt> debts=new ArrayList<>();for(Debt d:current.debts)debts.add(d.accountId.equals(id)?new Debt(id,spec.direction,spec.frequency,spec.installmentMinor,spec.firstDue,spec.customDates,spec.reminder,spec.beforeDays,spec.reminderTime,old.changes):d);
        commit(new Snapshot(accounts,current.entries,current.defaults,current.preferences,debts));
    }
    public synchronized void postpone(String id,int index,String date) throws IOException {
        ready();Debt old=current.debts.stream().filter(d->d.accountId.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Missing debt"));
        Debt.Due due=old.schedule(current).stream().filter(d->d.index==index).findFirst().orElseThrow(()->new IllegalArgumentException("Missing installment"));
        if(due.paidMinor>0)throw new IllegalArgumentException("Cannot postpone paid installment");
        if(date.compareTo(due.date)<=0)throw new IllegalArgumentException("New due must be later");
        List<Debt.Change> changes=new ArrayList<>(old.changes);changes.add(new Debt.Change(index,due.date,date,java.time.LocalDateTime.now().toString()));
        Debt next=new Debt(id,old.direction,old.frequency,old.installmentMinor,old.firstDue,old.customDates,old.reminder,old.beforeDays,old.reminderTime,changes);
        List<Debt> debts=new ArrayList<>();for(Debt d:current.debts)debts.add(d.accountId.equals(id)?next:d);commit(new Snapshot(current.accounts,current.entries,current.defaults,current.preferences,debts));
    }
    public synchronized String settle(String id,String moneyAccount,long amount,String date,String time,String note,String editId) throws IOException {
        return settle(id,moneyAccount,amount,date,time,note,editId,-1,false);
    }
    public synchronized String settle(String id,String moneyAccount,long amount,String date,String time,String note,String editId,int installmentIndex,boolean earlyCredit) throws IOException {
        ready();Debt d=current.debts.stream().filter(x->x.accountId.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Missing debt"));
        if(account(current,id).archived)throw new IllegalArgumentException("Restore debt first");
        boolean linked=moneyAccount!=null&&!moneyAccount.isEmpty();
        if(linked&&(account(current,moneyAccount).archived||current.debts.stream().anyMatch(x->x.accountId.equals(moneyAccount))))throw new IllegalArgumentException("Choose an active financial account");
        String source=!linked?id:d.direction==Debt.Direction.PAYABLE?moneyAccount:id,target=!linked?"":d.direction==Debt.Direction.PAYABLE?id:moneyAccount;
        Kind kind=linked?Kind.TRANSFER:Kind.SETTLEMENT;String entryId=editId==null?UUID.randomUUID().toString():editId;
        List<Entry> list=new ArrayList<>(current.entries);
        if(editId!=null){Entry old=list.stream().filter(e->e.id.equals(editId)).findFirst().orElseThrow(()->new IllegalArgumentException("Missing settlement"));if(old.deleted||!old.accountId.equals(id)&&!old.targetId.equals(id))throw new IllegalArgumentException("Wrong or deleted settlement");list.remove(old);}
        Entry next=new Entry(entryId,kind,source,target,amount,note.trim(),date,time,false,Collections.emptyList(),Collections.emptyList(),installmentIndex,earlyCredit);
        if(editId==null)list.add(next);else list.add(current.entries.indexOf(current.entries.stream().filter(e->e.id.equals(editId)).findFirst().get()),next);
        commit(new Snapshot(current.accounts,list,current.defaults,current.preferences,current.debts));return entryId;
    }
    public synchronized void distributeCredit(String id) throws IOException {
        ready();if(current.debts.stream().noneMatch(d->d.accountId.equals(id)))throw new IllegalArgumentException("Missing debt");List<Entry> list=new ArrayList<>();
        for(Entry e:current.entries)list.add(!e.deleted&&e.earlyCredit&&(e.accountId.equals(id)||e.targetId.equals(id))?new Entry(e.id,e.kind,e.accountId,e.targetId,e.minor,e.note,e.date,e.time,false,e.items,e.bills,e.installmentIndex,false):e);
        commit(new Snapshot(current.accounts,list,current.defaults,current.preferences,current.debts));
    }
    public synchronized boolean duplicateSettlement(String id,String moneyAccount,long amount,String date,String editId){
        ready();return current.entries.stream().anyMatch(e->!e.deleted&&!e.id.equals(editId)&&(e.accountId.equals(id)||e.targetId.equals(id))&&e.minor==amount&&e.date.equals(date)&&Objects.equals(e.kind==Kind.SETTLEMENT?"":e.accountId.equals(id)?e.targetId:e.accountId,moneyAccount==null?"":moneyAccount));
    }
    /** Copy-on-create defaults only: never rewrites existing account labels. */
    public synchronized void setDefaultLabels(String incoming,String outgoing) throws IOException {
        ready();commit(new Snapshot(current.accounts,current.entries,new ButtonLabels(incoming.trim(),outgoing.trim()),current.preferences,current.debts));
    }
    /** Reorders the complete account list; identity, entries and binary schema stay unchanged. */
    public synchronized void reorderAccounts(List<String> orderedIds) throws IOException {
        ready();
        if(orderedIds==null || orderedIds.size()!=current.accounts.size())
            throw new IllegalArgumentException("Expected every account exactly once");
        Map<String,Account> remaining=new HashMap<>();
        List<String> oldIds=new ArrayList<>();
        for(Account a:current.accounts) {remaining.put(a.id,a);oldIds.add(a.id);}
        List<Account> next=new ArrayList<>();
        for(String id:orderedIds) {
            Account a=remaining.remove(id);
            if(a==null)throw new IllegalArgumentException("Unknown or duplicate account");
            next.add(a);
        }
        if(!remaining.isEmpty())throw new IllegalArgumentException("Missing account");
        if(oldIds.equals(orderedIds))return;
        commit(new Snapshot(next,current.entries,current.defaults,current.preferences,current.debts));
    }
    public synchronized String post(Kind kind,String source,String target,long minor,String note,String date) throws IOException {
        return post(kind,source,target,minor,note,date,"");
    }
    public synchronized String post(Kind kind,String source,String target,long minor,String note,String date,String time) throws IOException {
        return post(kind,source,target,minor,note,date,time,Collections.emptyList(),Collections.emptyList());
    }
    public synchronized String post(Kind kind,String source,String target,long minor,String note,String date,String time,List<Item> items,List<Bill> bills) throws IOException {
        if(isSalaryMarker(note))throw new IllegalArgumentException("Reserved salary receipt marker");
        ready();if(account(current,source).archived || (kind==Kind.TRANSFER && account(current,target).archived))throw new IllegalArgumentException("Restore archived account before posting"); String id=UUID.randomUUID().toString();
        List<Entry> list=new ArrayList<>(current.entries);
        list.add(new Entry(id,kind,source,target==null?"":target,minor,note.trim(),date,time,false,items,bills));
        commit(new Snapshot(current.accounts,list,current.defaults,current.preferences,current.debts)); return id;
    }
    /**
     * Durable, idempotent salary receipt income. The stable external key is
     * encoded in the persisted entry note, so the key and income are committed
     * in one AtomicFile snapshot rather than two independent files.
     *
     * All writers MUST share the same process-wide lock and reload before
     * checking. Cross-process posting is not supported.
     */
    public String postSalaryIncomeOnce(String paymentId,String accountId,long minor,String date) throws IOException {
        if(paymentId==null || !paymentId.matches("[A-Za-z0-9_-]{1,120}"))
            throw new IllegalArgumentException("Invalid payment ID");
        if(accountId==null || accountId.isEmpty() || minor<=0)
            throw new IllegalArgumentException("Invalid salary receipt");
        final String marker="[salary-receipt:"+paymentId+"]";
        synchronized(Ledger.class) {
            load();
            Entry found=null;
            for(Entry e:current.entries) {
                if(e.note.equals(marker)) {
                    if(found!=null)throw new IllegalStateException("Duplicate salary receipt key");
                    found=e;
                }
            }
            if(found!=null) {
                if(found.deleted || found.kind!=Kind.INCOME || !found.accountId.equals(accountId)
                    || found.minor!=minor || !found.date.equals(date))
                    throw new IllegalStateException("Salary receipt conflicts with existing entry");
                return found.id;
            }
            ready();
            if(account(current,accountId).archived)throw new IllegalArgumentException("Archived salary account");
            String id=UUID.randomUUID().toString();
            List<Entry> list=new ArrayList<>(current.entries);
            list.add(new Entry(id,Kind.INCOME,accountId,"",minor,marker,date,"",false));
            commit(new Snapshot(current.accounts,list,current.defaults,current.preferences,current.debts));
            return id;
        }
    }
    public synchronized void edit(String id,Kind kind,String source,String target,long minor,String note,String date,String time) throws IOException {
        ready();Entry old=null;for(Entry e:current.entries)if(e.id.equals(id))old=e;
        if(old==null)throw new IllegalArgumentException("Entry not found");
        edit(id,kind,source,target,minor,note,date,time,old.items,old.bills);
    }
    public synchronized void edit(String id,Kind kind,String source,String target,long minor,String note,String date,String time,List<Item> items,List<Bill> bills) throws IOException {
        ready();List<Entry> list=new ArrayList<>();boolean found=false;
        for(Entry e:current.entries) {
            if(e.id.equals(id)) {
                if(isSalaryMarker(e.note))throw new IllegalStateException("Salary receipt entries cannot be edited directly");
                if(isSalaryMarker(note))throw new IllegalArgumentException("Reserved salary receipt marker");
                if(e.deleted)throw new IllegalArgumentException("Restore before editing");
                list.add(new Entry(id,kind,source,target==null?"":target,minor,note.trim(),date,time,false,items,bills,e.installmentIndex,e.earlyCredit));found=true;
            } else list.add(e);
        }
        if(!found)throw new IllegalArgumentException("Entry not found");
        commit(new Snapshot(current.accounts,list,current.defaults,current.preferences,current.debts));
    }
    public synchronized void setDeleted(String id,boolean deleted) throws IOException {
        ready(); List<Entry> list=new ArrayList<>(); boolean found=false;
        for(Entry e:current.entries) { if(e.id.equals(id)) { if(isSalaryMarker(e.note))throw new IllegalStateException("Salary receipt entries cannot be deleted directly"); list.add(e.deleted(deleted)); found=true; } else list.add(e); }
        if(!found) throw new IllegalArgumentException("Entry not found");
        commit(new Snapshot(current.accounts,list,current.defaults,current.preferences,current.debts));
    }
    public synchronized byte[] backup() throws IOException { ready(); return encode(current); }
    public synchronized void restore(byte[] bytes) throws IOException {
        ready(); Snapshot next=decode(bytes);
        // A backup cannot silently erase a previously recorded salary receipt.
        for(Entry old:current.entries)if(isSalaryMarker(old.note)) {
            boolean same=false;
            for(Entry candidate:next.entries)if(candidate.note.equals(old.note)) {
                same=salaryEquivalent(old,candidate);
                break;
            }
            if(!same)throw new IllegalStateException("Backup would remove or change a salary receipt");
        }
        commit(next);
    }
    /** Decimal input only: Arabic and Persian digits supported, grouping is deliberately rejected. */
    public static long parseMoney(String input,boolean allowNegative) {
        StringBuilder b=new StringBuilder();
        for(char c:input.trim().toCharArray()) {
            if(c>='٠'&&c<='٩') c=(char)('0'+c-'٠');
            if(c>='۰'&&c<='۹') c=(char)('0'+c-'۰');
            if(c=='٫') c='.';
            b.append(c);
        }
        String s=b.toString();
        if(!s.matches(allowNegative?"-?[0-9]+(\\.[0-9]{1,2})?":"[0-9]+(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Use digits and a decimal point; maximum two decimal places");
        return new BigDecimal(s).movePointRight(2).longValueExact();
    }
    public static String money(long minor) { return BigDecimal.valueOf(minor,2).toPlainString(); }
    private static boolean isSalaryMarker(String note) {
        return note!=null && note.matches("\\[salary-receipt:[A-Za-z0-9_-]{1,120}\\]");
    }
    private static boolean salaryEquivalent(Entry a,Entry b) {
        return a.id.equals(b.id) && a.kind==b.kind && a.accountId.equals(b.accountId)
            && a.minor==b.minor && a.date.equals(b.date) && a.note.equals(b.note)
            && a.deleted==b.deleted && !a.deleted;
    }
    private static Account account(Snapshot s,String id) {
        for(Account a:s.accounts) if(a.id.equals(id)) return a;
        throw new IllegalArgumentException("Unknown account");
    }
    private static void text(String s,int max) { if(s==null || s.length()>max) throw new IllegalArgumentException("Invalid text length"); }
    private static void label(String value) {
        text(value,30);if(value.trim().isEmpty())throw new IllegalArgumentException("Empty operation label");
        for(int i=0;i<value.length();i++)if(Character.isISOControl(value.charAt(i)))throw new IllegalArgumentException("Invalid operation label");
    }
    private static void validate(Snapshot s) {
        if(s.defaults==null)throw new IllegalArgumentException("Missing defaults");
        label(s.defaults.incoming);label(s.defaults.outgoing);
        if(s.accounts.size()>2000 || s.entries.size()>100000) throw new IllegalArgumentException("Ledger exceeds supported size");
        if(s.preferences.presets.size()>100||s.preferences.recent.size()>12||s.preferences.favorites.size()>120)throw new IllegalArgumentException("Too many preferences");
        for(ButtonLabels p:s.preferences.presets){label(p.incoming);label(p.outgoing);}
        for(List<String> icons:Arrays.asList(s.preferences.recent,s.preferences.favorites))for(String icon:icons){text(icon,16);if(icon.trim().isEmpty())throw new IllegalArgumentException("Empty icon");for(int i=0;i<icon.length();i++)if(Character.isISOControl(icon.charAt(i)))throw new IllegalArgumentException("Invalid icon");}
        Set<String> ids=new HashSet<>(); String currency=null;
        for(Account a:s.accounts) {
            text(a.id,80); text(a.name,100); text(a.openingDate,10);text(a.createdDate,10);
            if(!a.createdDate.isEmpty()){try{java.time.LocalDate.parse(a.createdDate);}catch(RuntimeException ex){throw new IllegalArgumentException("Invalid creation date",ex);}}
            label(a.incomingLabel);label(a.outgoingLabel);text(a.icon,16);
            for(int i=0;i<a.icon.length();i++)if(Character.isISOControl(a.icon.charAt(i)))throw new IllegalArgumentException("Invalid icon");
            if(!a.openingDate.isEmpty()) {try { java.time.LocalDate.parse(a.openingDate); } catch(RuntimeException ex) {throw new IllegalArgumentException("Invalid opening date");}}
            if(a.id.isEmpty() || a.name.trim().isEmpty() || !ids.add(a.id)) throw new IllegalArgumentException("Invalid account");
            // v0.1 is explicitly EGP only; future currencies require scale and reporting policy.
            if(!"EGP".equals(a.currency)) throw new IllegalArgumentException("v0.1 supports EGP only");
            if(currency!=null&&!currency.equals(a.currency)) throw new IllegalArgumentException("Mixed currencies unsupported");
            currency=a.currency;
        }
        ids.clear();
        Set<String> salaryKeys=new HashSet<>();
        for(Entry e:s.entries) {
            if(isSalaryMarker(e.note)) {
                if(!salaryKeys.add(e.note) || e.deleted || e.kind!=Kind.INCOME || !e.targetId.isEmpty())
                    throw new IllegalArgumentException("Invalid or duplicate salary receipt");
            }
            text(e.id,80); text(e.note,2000); text(e.date,10); text(e.time,5);
            if(!e.time.isEmpty() && !e.time.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) throw new IllegalArgumentException("Invalid time");
            if(e.id.isEmpty() || !ids.add(e.id) || e.kind==null || e.minor<=0) throw new IllegalArgumentException("Invalid entry");
            try { java.time.LocalDate.parse(e.date); } catch(RuntimeException ex) { throw new IllegalArgumentException("Invalid date"); }
            if(e.items.size()>100 || e.bills.size()>10)throw new IllegalArgumentException("Too many extras");
            long itemTotal=0;
            for(Item item:e.items) {
                text(item.name,100);
                if(item.name.trim().isEmpty()||item.quantity<1||item.quantity>1000000||item.unitMinor<0)throw new IllegalArgumentException("Invalid item");
                itemTotal=Math.addExact(itemTotal,item.totalMinor());
            }
            if(!e.items.isEmpty() && itemTotal!=e.minor)throw new IllegalArgumentException("Item total must match entry");
            for(Bill bill:e.bills) {
                text(bill.name,200);text(bill.uri,2048);
                if(bill.name.trim().isEmpty()||!bill.uri.startsWith("content://"))throw new IllegalArgumentException("Invalid bill reference");
            }
            Account source=account(s,e.accountId);
            if(e.installmentIndex < -1 || e.installmentIndex>599)throw new IllegalArgumentException("Invalid installment target");
            boolean debtEntry=s.debts.stream().anyMatch(d->d.accountId.equals(e.accountId)||d.accountId.equals(e.targetId));
            if((e.kind==Kind.SETTLEMENT||e.installmentIndex!=-1||e.earlyCredit)&&!debtEntry)throw new IllegalArgumentException("Debt metadata on ordinary entry");
            if(e.kind==Kind.TRANSFER) {
                Account target=account(s,e.targetId);
                if(source.id.equals(target.id)||!source.currency.equals(target.currency)) throw new IllegalArgumentException("Invalid transfer");
            } else if(!e.targetId.isEmpty()) throw new IllegalArgumentException("Unexpected target");
        }
        Set<String> debtIds=new HashSet<>();for(Debt d:s.debts){if(!debtIds.add(d.accountId))throw new IllegalArgumentException("Duplicate debt");try{d.validate(s);}catch(java.time.DateTimeException ex){throw new IllegalArgumentException("Invalid debt date",ex);}}
        for(Account a:s.accounts) s.balance(a.id); // Reject overflow before committing any bytes.
        s.total(Kind.INCOME); s.total(Kind.EXPENSE);
    }
    private static final int MAGIC=0x43424B31, VERSION=7, MAX_BYTES=16*1024*1024;
    private static byte[] encode(Snapshot s) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(s.accounts.size());
        for(Account a:s.accounts) { out.writeUTF(a.id);out.writeUTF(a.name);out.writeUTF(a.currency);out.writeLong(a.openingMinor);out.writeUTF(a.openingDate);out.writeUTF(a.incomingLabel);out.writeUTF(a.outgoingLabel);out.writeUTF(a.icon);out.writeUTF(a.createdDate);out.writeBoolean(a.archived); }
        out.writeInt(s.entries.size());
        for(Entry e:s.entries) { out.writeUTF(e.id);out.writeUTF(e.kind.name());out.writeUTF(e.accountId);out.writeUTF(e.targetId);out.writeLong(e.minor);out.writeUTF(e.note);out.writeUTF(e.date);out.writeUTF(e.time);out.writeBoolean(e.deleted);out.writeInt(e.installmentIndex);out.writeBoolean(e.earlyCredit);
            out.writeInt(e.items.size());for(Item item:e.items){out.writeUTF(item.name);out.writeInt(item.quantity);out.writeLong(item.unitMinor);}
            out.writeInt(e.bills.size());for(Bill bill:e.bills){out.writeUTF(bill.name);out.writeUTF(bill.uri);}
        }
        out.writeUTF(s.defaults.incoming);out.writeUTF(s.defaults.outgoing);
        out.writeInt(s.preferences.presets.size());for(ButtonLabels p:s.preferences.presets){out.writeUTF(p.incoming);out.writeUTF(p.outgoing);}
        for(List<String> icons:Arrays.asList(s.preferences.recent,s.preferences.favorites)){out.writeInt(icons.size());for(String icon:icons)out.writeUTF(icon);}
        out.writeInt(s.debts.size());for(Debt d:s.debts){out.writeUTF(d.accountId);out.writeUTF(d.direction.name());out.writeUTF(d.frequency.name());out.writeLong(d.installmentMinor);out.writeUTF(d.firstDue);out.writeBoolean(d.reminder);out.writeInt(d.beforeDays);out.writeUTF(d.reminderTime);out.writeInt(d.customDates.size());for(String day:d.customDates)out.writeUTF(day);out.writeInt(d.changes.size());for(Debt.Change c:d.changes){out.writeInt(c.index);out.writeUTF(c.oldDate);out.writeUTF(c.newDate);out.writeUTF(c.changedAt);}}
        out.flush(); byte[] result=bytes.toByteArray();
        if(result.length>MAX_BYTES) throw new IOException("Snapshot too large");
        ByteArrayOutputStream envelope=new ByteArrayOutputStream();
        envelope.write(result); envelope.write(digest(result));
        return envelope.toByteArray();
    }
    private static byte[] digest(byte[] data) {
        try { return MessageDigest.getInstance("SHA-256").digest(data); }
        catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static Snapshot decode(byte[] bytes) throws IOException {
        if(bytes==null || bytes.length<32 || bytes.length>MAX_BYTES+32) throw new IOException("Invalid backup size");
        try {
            byte[] payload=Arrays.copyOf(bytes,bytes.length-32);
            if(!MessageDigest.isEqual(digest(payload),Arrays.copyOfRange(bytes,bytes.length-32,bytes.length)))
                throw new IOException("Backup checksum mismatch");
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload));
            if(in.readInt()!=MAGIC)throw new IOException("Unsupported backup format");
            int version=in.readInt();if(version<1||version>VERSION)throw new IOException("Unsupported backup version");
            int count=in.readInt(); if(count<0||count>2000) throw new IOException("Invalid account count");
            List<Account> accounts=new ArrayList<>(); List<Entry> entries=new ArrayList<>();
            for(int i=0;i<count;i++) {
                String id=in.readUTF(),name=in.readUTF(),currency=in.readUTF();long opening=in.readLong();
                String openingDate=version>=3?in.readUTF():"";
                String incoming=version>=4?in.readUTF():"إيراد",outgoing=version>=4?in.readUTF():"مصروف",icon=version>=4?in.readUTF():"";
                String created=version>=5?in.readUTF():"";boolean archived=version>=5&&in.readBoolean();
                accounts.add(new Account(id,name,currency,opening,openingDate,incoming,outgoing,icon,created,archived));
            }
            count=in.readInt(); if(count<0||count>100000) throw new IOException("Invalid entry count");
            for(int i=0;i<count;i++) {
                String id=in.readUTF();Kind kind=Kind.valueOf(in.readUTF());String source=in.readUTF(),target=in.readUTF();
                long minor=in.readLong();String note=in.readUTF(),date=in.readUTF(),time=version>=2?in.readUTF():"";
                boolean deleted=in.readBoolean();int installmentIndex=version>=7?in.readInt():-1;boolean earlyCredit=version>=7&&in.readBoolean();List<Item> items=new ArrayList<>();List<Bill> bills=new ArrayList<>();
                if(version>=3) {
                    int n=in.readInt();if(n<0||n>100)throw new IOException("Invalid item count");
                    for(int j=0;j<n;j++)items.add(new Item(in.readUTF(),in.readInt(),in.readLong()));
                    n=in.readInt();if(n<0||n>10)throw new IOException("Invalid bill count");
                    for(int j=0;j<n;j++)bills.add(new Bill(in.readUTF(),in.readUTF()));
                }
                entries.add(new Entry(id,kind,source,target,minor,note,date,time,deleted,items,bills,installmentIndex,earlyCredit));
            }
            ButtonLabels defaults=version>=4?new ButtonLabels(in.readUTF(),in.readUTF()):new ButtonLabels("إيراد","مصروف");
            Preferences preferences=new Preferences();
            if(version>=5){
                int n=in.readInt();if(n<0||n>100)throw new IOException("Invalid preset count");List<ButtonLabels> presets=new ArrayList<>();
                for(int i=0;i<n;i++)presets.add(new ButtonLabels(in.readUTF(),in.readUTF()));
                List<List<String>> icons=new ArrayList<>();for(int max: new int[]{12,120}){n=in.readInt();if(n<0||n>max)throw new IOException("Invalid icon count");List<String> list=new ArrayList<>();for(int i=0;i<n;i++)list.add(in.readUTF());icons.add(list);}
                preferences=new Preferences(presets,icons.get(0),icons.get(1));
            }
            List<Debt> debts=new ArrayList<>();if(version>=6){
                int n=in.readInt();if(n<0||n>2000)throw new IOException("Invalid debt count");
                for(int i=0;i<n;i++){String id=in.readUTF();Debt.Direction direction=Debt.Direction.valueOf(in.readUTF());Debt.Frequency frequency=Debt.Frequency.valueOf(in.readUTF());long installment=in.readLong();String first=in.readUTF();boolean reminder=in.readBoolean();int before=in.readInt();String time=in.readUTF();int dates=in.readInt();if(dates<0||dates>600)throw new IOException("Invalid dates");List<String> days=new ArrayList<>();for(int j=0;j<dates;j++)days.add(in.readUTF());int countChanges=in.readInt();if(countChanges<0||countChanges>5000)throw new IOException("Invalid changes");List<Debt.Change> changes=new ArrayList<>();for(int j=0;j<countChanges;j++)changes.add(new Debt.Change(in.readInt(),in.readUTF(),in.readUTF(),in.readUTF()));debts.add(new Debt(id,direction,frequency,installment,first,days,reminder,before,time,changes));}
            }
            if(in.read()!=-1) throw new IOException("Trailing bytes");
            Snapshot s=new Snapshot(accounts,entries,defaults,preferences,debts); validate(s); return s;
        } catch(IllegalArgumentException | ArithmeticException ex) { throw new IOException("Invalid backup content",ex); }
    }
}
