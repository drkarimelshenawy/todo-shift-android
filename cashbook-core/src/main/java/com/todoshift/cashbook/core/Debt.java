package com.todoshift.cashbook.core;

import java.time.*;
import java.util.*;

/** A debt is an account with a signed principal; settlements are single transfers. */
public final class Debt {
 public enum Direction { PAYABLE, RECEIVABLE }
 public enum Frequency { OPEN, WEEKLY, MONTHLY, CUSTOM }
 public final String accountId, firstDue, reminderTime;
 public final Direction direction;
 public final Frequency frequency;
 public final long installmentMinor;
 public final boolean reminder;
 public final int beforeDays;
 public final List<String> customDates;
 public final List<Change> changes;
 public Debt(String accountId,Direction direction,Frequency frequency,long installmentMinor,String firstDue,List<String> customDates,boolean reminder,int beforeDays,String reminderTime,List<Change> changes) {
  this.accountId=accountId;this.direction=direction;this.frequency=frequency;this.installmentMinor=installmentMinor;this.firstDue=firstDue;this.reminder=reminder;this.beforeDays=beforeDays;this.reminderTime=reminderTime;
  this.customDates=Collections.unmodifiableList(new ArrayList<>(customDates));this.changes=Collections.unmodifiableList(new ArrayList<>(changes));
 }
 public Debt withId(String id){return new Debt(id,direction,frequency,installmentMinor,firstDue,customDates,reminder,beforeDays,reminderTime,changes);}
 public static final class Change {
  public final int index;public final String oldDate,newDate,changedAt;
  public Change(int index,String oldDate,String newDate,String changedAt){this.index=index;this.oldDate=oldDate;this.newDate=newDate;this.changedAt=changedAt;}
 }
 public static final class Due {
  public final int index;public final String date;public final long amountMinor,paidMinor,creditMinor;
  Due(int index,String date,long amount,long paid){this(index,date,amount,paid,0);}
  Due(int index,String date,long amount,long paid,long credit){this.index=index;this.date=date;this.amountMinor=amount;this.paidMinor=paid;this.creditMinor=credit;}
  public long remaining(){return amountMinor-paidMinor-creditMinor;}
  public String status(LocalDate today){return remaining()==0?(creditMinor>0?"CREDIT":"PAID"):LocalDate.parse(date).isBefore(today)?"OVERDUE":paidMinor>0||creditMinor>0?"PARTIAL":"UPCOMING";}
 }
 public long principal(Ledger.Snapshot s){long n=account(s).openingMinor;if(n==Long.MIN_VALUE)throw new IllegalArgumentException("Principal overflow");return Math.abs(n);}
 public Ledger.Account account(Ledger.Snapshot s){return s.accounts.stream().filter(a->a.id.equals(accountId)).findFirst().orElseThrow(()->new IllegalArgumentException("Missing debt account"));}
 public long remaining(Ledger.Snapshot s){return direction==Direction.PAYABLE?Math.negateExact(s.balance(accountId)):s.balance(accountId);}
 public long paid(Ledger.Snapshot s){return Math.subtractExact(principal(s),remaining(s));}
 public List<Due> schedule(Ledger.Snapshot s){
  long total=principal(s);int count=frequency==Frequency.OPEN?(firstDue.isEmpty()?0:1):(int)(1+(total-1)/installmentMinor);
  if(count>600)throw new IllegalArgumentException("Maximum 600 installments");
  List<Due> rows=new ArrayList<>();long left=total;
  for(int i=0;i<count;i++){
   String date=frequency==Frequency.CUSTOM?customDates.get(i):frequency==Frequency.MONTHLY?LocalDate.parse(firstDue).plusMonths(i).toString():frequency==Frequency.WEEKLY?LocalDate.parse(firstDue).plusWeeks(i).toString():firstDue;
   for(Change c:changes)if(c.index==i)date=c.newDate;
   long amount=frequency==Frequency.OPEN?left:Math.min(left,installmentMinor);rows.add(new Due(i,date,amount,0));left-=amount;
  }
  rows.sort(Comparator.comparing((Due d)->d.date).thenComparingInt(d->d.index));
  long[] assigned=new long[count];long held=0;
  List<Ledger.Entry> entries=new ArrayList<>(s.entries);entries.sort(Comparator.comparing((Ledger.Entry e)->e.date).thenComparing(e->e.time));
  for(Ledger.Entry e:entries)if(!e.deleted&&(e.accountId.equals(accountId)||e.targetId.equals(accountId))){
   long value=e.minor;int target=e.installmentIndex;
   if(target>=0){Due row=rows.stream().filter(d->d.index==target).findFirst().orElseThrow(()->new IllegalArgumentException("Invalid target installment"));long part=Math.min(value,row.amountMinor-assigned[target]);assigned[target]+=part;value-=part;}
   if(e.earlyCredit){held=Math.addExact(held,value);continue;}
   for(Due row:rows){long part=Math.min(value,row.amountMinor-assigned[row.index]);assigned[row.index]+=part;value-=part;if(value==0)break;}
   if(count>0&&value>0)throw new IllegalArgumentException("Allocation exceeds schedule");
  }
  List<Due> result=new ArrayList<>();
  for(Due row:rows){long credit=Math.min(held,row.amountMinor-assigned[row.index]);held-=credit;result.add(new Due(row.index,row.date,row.amountMinor,assigned[row.index],credit));}
  return Collections.unmodifiableList(result);
 }
 public long advanceCredit(Ledger.Snapshot s){if(frequency==Frequency.OPEN&&firstDue.isEmpty())return 0;return paid(s)-schedule(s).stream().mapToLong(d->d.paidMinor).sum();}
 public void validate(Ledger.Snapshot s){
  if(direction==null||frequency==null||accountId==null)throw new IllegalArgumentException("Invalid debt type");
  Ledger.Account a=account(s);long total=principal(s);
  if(total<=0 || direction==Direction.PAYABLE&&a.openingMinor>=0 || direction==Direction.RECEIVABLE&&a.openingMinor<=0)throw new IllegalArgumentException("Invalid debt principal");
  if(!firstDue.isEmpty())LocalDate.parse(firstDue);
  if(!a.openingDate.isEmpty()&&!firstDue.isEmpty()&&firstDue.compareTo(a.openingDate)<0)throw new IllegalArgumentException("Due before start");
  if(frequency!=Frequency.OPEN&&(installmentMinor<=0||firstDue.isEmpty()))throw new IllegalArgumentException("Installment and first due required");
  int count=frequency==Frequency.OPEN?(firstDue.isEmpty()?0:1):(int)(1+(total-1)/installmentMinor);
  if(frequency!=Frequency.OPEN && 1+(total-1)/installmentMinor>600)throw new IllegalArgumentException("Too many installments");
  if(frequency==Frequency.CUSTOM&&customDates.size()!=count || frequency!=Frequency.CUSTOM&&!customDates.isEmpty())throw new IllegalArgumentException("Custom date count mismatch");
  String previous="";for(String date:customDates){LocalDate.parse(date);if(date.compareTo(firstDue)<0||date.compareTo(previous)<=0)throw new IllegalArgumentException("Dates must increase");previous=date;}
  if(beforeDays<0||beforeDays>30||!reminderTime.matches("([01][0-9]|2[0-3]):[0-5][0-9]"))throw new IllegalArgumentException("Invalid reminder");
  if(reminder&&count==0)throw new IllegalArgumentException("Reminder requires target date");
  if(changes.size()>5000)throw new IllegalArgumentException("Too many changes");
  for(Change c:changes){if(c.index<0||c.index>=count)throw new IllegalArgumentException("Invalid installment");LocalDate.parse(c.oldDate);LocalDate.parse(c.newDate);LocalDateTime.parse(c.changedAt);if(!a.openingDate.isEmpty()&&c.newDate.compareTo(a.openingDate)<0)throw new IllegalArgumentException("Due before start");}
  long remaining=remaining(s);if(remaining<0||remaining>total)throw new IllegalArgumentException("Debt overpaid or wrong direction");
  for(Ledger.Entry e:s.entries)if(e.accountId.equals(accountId)||e.targetId.equals(accountId)){
   if(e.kind!=Ledger.Kind.TRANSFER&&e.kind!=Ledger.Kind.SETTLEMENT)throw new IllegalArgumentException("Debt supports linked settlements only");
   if(e.kind==Ledger.Kind.TRANSFER&&(direction==Direction.PAYABLE&&!e.targetId.equals(accountId)||direction==Direction.RECEIVABLE&&!e.accountId.equals(accountId)))throw new IllegalArgumentException("Wrong settlement direction");
   String other=e.accountId.equals(accountId)?e.targetId:e.accountId;
   if(e.kind==Ledger.Kind.TRANSFER&&s.debts.stream().anyMatch(d->d.accountId.equals(other)))throw new IllegalArgumentException("Debt-to-debt settlement unsupported");
   if(!a.openingDate.isEmpty()&&e.date.compareTo(a.openingDate)<0)throw new IllegalArgumentException("Settlement before debt start");
  }
  schedule(s);
 }
}
