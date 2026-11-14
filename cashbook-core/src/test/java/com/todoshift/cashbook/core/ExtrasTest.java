package com.todoshift.cashbook.core;
import java.io.*;import java.util.*;import java.security.*;
public class ExtrasTest {
 static int n;static void eq(Object a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 interface Action{void run()throws Exception;}static void fail(Action a)throws Exception{n++;try{a.run();}catch(Exception ok){return;}throw new AssertionError("Expected failure");}
 public static void main(String[]args)throws Exception{
  LedgerTest.Memory store=new LedgerTest.Memory();Ledger l=new Ledger(store);l.load();String id=l.addAccount("cash","EGP",10000,"2026-11-10");
  l.post(Ledger.Kind.EXPENSE,id,null,1000,"before opening","2026-11-09","12:00");
  List<Ledger.Item> items=Arrays.asList(new Ledger.Item("paper",3,1250));List<Ledger.Bill> bills=Arrays.asList(new Ledger.Bill("receipt.pdf","content://documents/receipt"));
  String e=l.post(Ledger.Kind.EXPENSE,id,null,3750,"items","2026-11-11","10:00",items,bills);
  eq(l.snapshot().balance(id),5250L);eq(l.snapshot().entries.get(1).items.get(0).totalMinor(),3750L);
  LedgerQuery.View v=LedgerQuery.account(l.snapshot(),id,new LedgerQuery.Filter(null,null,"",null,false,false));eq(v.rows.get(0).runningMinor,-1000L);eq(v.rows.get(1).runningMinor,5250L);
  fail(()->l.post(Ledger.Kind.EXPENSE,id,null,1,"mismatch","2026-11-11","10:00",items,bills));eq(l.snapshot().entries.size(),2);
  fail(()->l.addAccount("bad","EGP",0,"2026-02-30"));
  fail(()->l.post(Ledger.Kind.EXPENSE,id,null,3750,"bad","2026-11-11","10:00",items,Arrays.asList(new Ledger.Bill("bad","file:///secret"))));
  l.load();eq(l.snapshot().accounts.get(0).openingDate,"2026-11-10");eq(l.snapshot().entries.get(1).bills.get(0).uri,"content://documents/receipt");
  l.setDeleted(e,true);l.setDeleted(e,false);eq(l.snapshot().entries.get(1).items.size(),1);eq(l.snapshot().entries.get(1).bills.size(),1);
  store.badWrite=true;fail(()->l.edit(e,Ledger.Kind.EXPENSE,id,null,100,"bad","2026-11-11","12:00",Collections.emptyList(),Collections.emptyList()));eq(l.snapshot().entries.get(1).items.size(),1);store.badWrite=false;
  byte[] backup=l.backup();l.restore(backup);eq(l.snapshot().entries.get(1).items.get(0).name,"paper");
  // Independently constructed v2 record, before opening-date/items/bills existed.
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
  out.writeInt(0x43424B31);out.writeInt(2);out.writeInt(1);out.writeUTF("a");out.writeUTF("legacy");out.writeUTF("EGP");out.writeLong(100);
  out.writeInt(1);out.writeUTF("e");out.writeUTF("INCOME");out.writeUTF("a");out.writeUTF("");out.writeLong(10);out.writeUTF("old");out.writeUTF("2026-11-01");out.writeUTF("15:30");out.writeBoolean(false);out.flush();
  bytes.write(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));l.restore(bytes.toByteArray());
  eq(l.snapshot().accounts.get(0).openingDate,"");eq(l.snapshot().entries.get(0).time,"15:30");eq(l.snapshot().entries.get(0).items.size(),0);eq(l.snapshot().balance("a"),110L);
  eq(MoneyCalculator.calculate("125+75*2"),27500L);eq(MoneyCalculator.calculate("(125+75)*2"),40000L);eq(MoneyCalculator.calculate("١٢٫٥ + ٢٫٥"),1500L);
  eq(MoneyCalculator.calculate("1/3"),33L);eq(MoneyCalculator.calculate("2/3"),67L);eq(MoneyCalculator.calculate("-1.005"),-101L);
  fail(()->MoneyCalculator.calculate("1/0"));fail(()->MoneyCalculator.calculate("alert(1)"));fail(()->MoneyCalculator.calculate("1,5"));fail(()->MoneyCalculator.calculate("92233720368547758.08"));
  System.out.println("PASS: "+n+" items/bills/dated-opening/v2 migration/calculator assertions");
 }
}
