package com.todoshift.cashbook.core;
import java.io.*;import java.util.*;import java.time.*;import java.security.*;
public class QueryTest {
 static int n;static void eq(Object a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 interface Run{void run()throws Exception;}static void fail(Run r)throws Exception{n++;try{r.run();}catch(Exception ok){return;}throw new AssertionError("Expected failure");}
 static class Store implements Ledger.Store{byte[] data;boolean bad;public byte[] read(){return data;}public void write(byte[] bytes)throws IOException{if(bad)throw new IOException("full");data=bytes;}}
 static LedgerQuery.Filter all(boolean descending){return new LedgerQuery.Filter(null,null,"",null,false,descending);}
 public static void main(String[]args)throws Exception{
  Store store=new Store();Ledger l=new Ledger(store);l.load();String a=l.addAccount("cash","EGP",10000),b=l.addAccount("bank","EGP",0);
  String late=l.post(Ledger.Kind.EXPENSE,a,null,2000,"lunch","2026-11-11","15:30");
  String early=l.post(Ledger.Kind.INCOME,a,null,5000,"salary","2026-11-10","09:00");
  String t=l.post(Ledger.Kind.TRANSFER,a,b,3000,"transfer","2026-11-11","10:00");
  LedgerQuery.View v=LedgerQuery.account(l.snapshot(),a,all(false));
  eq(v.rows.get(0).entry.id,early);eq(v.rows.get(0).runningMinor,15000L);eq(v.rows.get(1).entry.id,t);eq(v.rows.get(1).runningMinor,12000L);eq(v.rows.get(2).runningMinor,10000L);
  eq(v.incomeMinor,5000L);eq(v.expenseMinor,2000L);eq(v.transferOutMinor,3000L);eq(v.netMinor(),3000L);
  LedgerQuery.Filter daily=LedgerQuery.Filter.period(LedgerQuery.Period.DAILY,LocalDate.parse("2026-11-11"),DayOfWeek.SATURDAY,"",null,false,true);
  v=LedgerQuery.account(l.snapshot(),a,daily);eq(v.rows.size(),2);eq(v.incomeMinor,0L);eq(v.currentBalanceMinor,10000L);eq(v.rows.get(0).entry.id,late);
  v=LedgerQuery.account(l.snapshot(),a,new LedgerQuery.Filter(null,null,"LUNCH",null,false,true));eq(v.rows.size(),1);eq(v.rows.get(0).runningMinor,10000L);
  v=LedgerQuery.account(l.snapshot(),b,all(true));eq(v.transferInMinor,3000L);eq(v.incomeMinor,0L);
  LedgerQuery.Filter week=LedgerQuery.Filter.period(LedgerQuery.Period.WEEKLY,LocalDate.parse("2026-11-11"),DayOfWeek.SATURDAY,"",null,false,true);
  eq(week.from,LocalDate.parse("2026-11-07"));eq(week.to,LocalDate.parse("2026-11-13"));
  LedgerQuery.Filter leap=LedgerQuery.Filter.period(LedgerQuery.Period.MONTHLY,LocalDate.parse("2024-02-10"),DayOfWeek.MONDAY,"",null,false,true);eq(leap.to,LocalDate.parse("2024-02-29"));
  fail(()->new LedgerQuery.Filter(LocalDate.parse("2026-12-01"),LocalDate.parse("2026-11-01"),"",null,false,true));
  l.edit(late,Ledger.Kind.EXPENSE,a,null,1000,"edited","2026-11-11","16:00");eq(l.snapshot().balance(a),11000L);eq(l.snapshot().entries.size(),3);
  store.bad=true;fail(()->l.edit(late,Ledger.Kind.EXPENSE,a,null,500,"bad","2026-11-11","16:00"));eq(l.snapshot().balance(a),11000L);store.bad=false;
  fail(()->l.post(Ledger.Kind.INCOME,a,null,10,"","2026-11-11","25:00"));
  l.setDeleted(t,true);v=LedgerQuery.account(l.snapshot(),a,new LedgerQuery.Filter(null,null,"",null,true,true));eq(v.rows.size(),1);eq(v.rows.get(0).runningMinor,null);eq(v.transferOutMinor,0L);
  fail(()->l.edit(t,Ledger.Kind.EXPENSE,a,null,10,"","2026-11-11","12:00"));
  byte[] backup=l.backup();l.restore(backup);eq(l.snapshot().entries.get(0).time,"16:00");
  // Real v1 binary layout and checksum: old entries acquire no fabricated time.
  ByteArrayOutputStream buf=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(buf);
  out.writeInt(0x43424B31);out.writeInt(1);out.writeInt(1);out.writeUTF("a");out.writeUTF("legacy");out.writeUTF("EGP");out.writeLong(100);
  out.writeInt(1);out.writeUTF("e");out.writeUTF("INCOME");out.writeUTF("a");out.writeUTF("");out.writeLong(10);out.writeUTF("old");out.writeUTF("2026-11-01");out.writeBoolean(false);out.flush();
  byte[] payload=buf.toByteArray();buf.write(MessageDigest.getInstance("SHA-256").digest(payload));
  l.restore(buf.toByteArray());eq(l.snapshot().entries.get(0).time,"");eq(l.snapshot().balance("a"),110L);l.load();eq(l.snapshot().balance("a"),110L);
  l.renameAccount("a","بنك");eq(l.snapshot().accounts.get(0).name,"بنك");eq(l.snapshot().balance("a"),110L);
  store.bad=true;fail(()->l.renameAccount("a","failed"));eq(l.snapshot().accounts.get(0).name,"بنك");store.bad=false;
  fail(()->l.renameAccount("a"," "));fail(()->l.renameAccount("missing","bad"));
  System.out.println("PASS: "+n+" query/edit/v1 migration assertions");
 }
}
