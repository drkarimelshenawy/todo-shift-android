package com.todoshift.cashbook.core;
import java.time.*;
import java.util.*;
/** Host-neutral desired reminders. Host replaces its schedule on every snapshot change. */
public final class DebtReminders {
 public interface Host { void sync(Ledger.Snapshot snapshot) throws Exception; }
 public static final class Event {
  public final String key,accountId,title,date;public final long minor;public final boolean overdue;public final LocalDateTime at;
  Event(String key,String accountId,String title,long minor,String date,boolean overdue,LocalDateTime at){this.key=key;this.accountId=accountId;this.title=title;this.minor=minor;this.date=date;this.overdue=overdue;this.at=at;}
 }
 public static List<Event> plan(Ledger.Snapshot s){
  List<Event> events=new ArrayList<>();
  for(Debt d:s.debts)if(d.reminder&&!d.account(s).archived&&d.remaining(s)>0){for(Debt.Due due:d.schedule(s))if(due.remaining()>0){
   String title=d.account(s).name;LocalDate date=LocalDate.parse(due.date);LocalTime time=LocalTime.parse(d.reminderTime);String root=d.accountId+":"+due.index+":"+due.date;
   events.add(new Event(root+":before",d.accountId,title,due.remaining(),due.date,false,date.minusDays(d.beforeDays).atTime(time)));
   events.add(new Event(root+":late",d.accountId,title,due.remaining(),due.date,true,date.plusDays(1).atTime(time)));
  }}
  events.sort(Comparator.comparing(e->e.at));return Collections.unmodifiableList(events);
 }
}
