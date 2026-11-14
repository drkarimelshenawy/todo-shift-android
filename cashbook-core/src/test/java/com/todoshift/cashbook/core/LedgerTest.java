package com.todoshift.cashbook.core;
import java.io.*;
import java.util.*;
public final class LedgerTest {
 static int checks;
 static void equal(Object a,Object b) { checks++; if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b); }
 interface Action {void run() throws Exception;}
 static void fails(Action a) throws Exception {checks++;try {a.run();}catch(Exception expected){return;}throw new AssertionError("Expected failure");}
 static class Memory implements Ledger.Store {byte[] bytes; boolean badRead,badWrite; public byte[] read() throws IOException {if(badRead)throw new IOException("disk read failed");return bytes;}public void write(byte[] b)throws IOException{if(badWrite)throw new IOException("disk full");bytes=b.clone();}}
 public static void main(String[] args)throws Exception {
  equal(Ledger.parseMoney("١٢٫٥٠",false),1250L);equal(Ledger.parseMoney("۱۲.۵۰",false),1250L);
  equal(Ledger.parseMoney("0.01",false),1L);equal(Ledger.parseMoney("-12.50",true),-1250L);
  for(String s:new String[]{"","1,000","1,5","١٬٠٠٠","1.001","NaN","1e3","-1","1.2.3","92233720368547758.08"}) fails(()->Ledger.parseMoney(s,false));
  Memory m=new Memory();Ledger l=new Ledger(m);fails(()->l.addAccount("cash","EGP",0));l.load();
  String a=l.addAccount("نقدي","EGP",10000),b=l.addAccount("بنك","EGP",0);
  String income=l.post(Ledger.Kind.INCOME,a,null,2000,"راتب","2026-11-11");
  l.post(Ledger.Kind.EXPENSE,a,null,1000,"مواصلات","2026-11-11");
  String t=l.post(Ledger.Kind.TRANSFER,a,b,3000,"تحويل","2026-11-11");
  equal(l.snapshot().balance(a),8000L);equal(l.snapshot().balance(b),3000L);
  equal(l.snapshot().total(Ledger.Kind.INCOME),2000L);equal(l.snapshot().total(Ledger.Kind.EXPENSE),1000L);
  l.setDeleted(t,true);equal(l.snapshot().balance(a),11000L);equal(l.snapshot().balance(b),0L);
  l.setDeleted(t,false);equal(l.snapshot().balance(b),3000L);
  fails(()->l.post(Ledger.Kind.TRANSFER,a,a,1,"","2026-11-11"));
  fails(()->l.post(Ledger.Kind.TRANSFER,a,"missing",1,"","2026-11-11"));
  fails(()->l.post(Ledger.Kind.INCOME,a,null,0,"","2026-11-11"));
  fails(()->l.post(Ledger.Kind.INCOME,a,null,1,"","2026-02-30"));
  fails(()->l.addAccount(" ","EGP",0));fails(()->l.addAccount("USD","USD",0));
  byte[] prior=l.backup();m.badWrite=true;fails(()->l.post(Ledger.Kind.INCOME,a,null,200,"draft","2026-11-11"));
  equal(l.snapshot().entries.size(),3);equal(Arrays.equals(prior,m.bytes),true);m.badWrite=false;
  fails(()->l.restore(new byte[]{1,2,3}));equal(Arrays.equals(prior,l.backup()),true);
  l.setDeleted(income,true);l.restore(prior);equal(l.snapshot().total(Ledger.Kind.INCOME),2000L);
  Ledger reloaded=new Ledger(m);reloaded.load();equal(reloaded.snapshot().balance(a),8000L);
  m.badRead=true;fails(reloaded::load);fails(()->reloaded.addAccount("unsafe","EGP",0));equal(Arrays.equals(prior,m.bytes),true);
  m.badRead=false;reloaded.load();equal(reloaded.snapshot().accounts.size(),2);
  Memory overflow=new Memory();Ledger o=new Ledger(overflow);o.load();String id=o.addAccount("limit","EGP",Long.MAX_VALUE);
  fails(()->o.post(Ledger.Kind.INCOME,id,null,1,"","2026-11-11"));equal(o.snapshot().entries.size(),0);
  byte[] junk=Arrays.copyOf(prior,prior.length+1);fails(()->l.restore(junk));
  byte[] corrupt=prior.clone();corrupt[20]^=1;fails(()->l.restore(corrupt));
  equal(l.snapshot().balance(a)+l.snapshot().balance(b),11000L);
  System.out.println("PASS: "+checks+" financial integrity assertions");
 }
}
