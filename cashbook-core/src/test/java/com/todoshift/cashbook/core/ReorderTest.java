package com.todoshift.cashbook.core;
import java.io.*;
import java.util.*;
public final class ReorderTest {
 static int n;static void eq(Object a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 interface Run{void run()throws Exception;}static void fail(Run r)throws Exception{n++;try{r.run();}catch(Exception ok){return;}throw new AssertionError("Expected failure");}
 static class Store implements Ledger.Store {byte[] bytes;boolean bad,readBad;int writes;public byte[] read()throws IOException{if(readBad)throw new IOException("read");return bytes;}public void write(byte[] b)throws IOException{if(bad)throw new IOException("full");bytes=b;writes++;}}
 static List<String> ids(Ledger l){List<String> ids=new ArrayList<>();for(Ledger.Account a:l.snapshot().accounts)ids.add(a.id);return ids;}
 public static void main(String[]args)throws Exception {
  Store s=new Store();Ledger l=new Ledger(s);fail(()->l.reorderAccounts(Collections.emptyList()));l.load();
  l.reorderAccounts(Collections.emptyList());eq(s.writes,0);
  String a=l.addAccount("same","EGP",10000,"2026-11-01"),b=l.addAccount("same","EGP",500),c=l.addAccount("ثالث","EGP",0);
  String entry=l.post(Ledger.Kind.TRANSFER,a,b,2500,"transfer","2026-11-11","10:00");l.post(Ledger.Kind.EXPENSE,b,null,100,"bill","2026-11-11");
  List<String> original=Arrays.asList(a,b,c), reordered=Arrays.asList(c,a,b);long balanceA=l.snapshot().balance(a),balanceB=l.snapshot().balance(b);
  l.reorderAccounts(reordered);eq(ids(l),reordered);eq(l.snapshot().balance(a),balanceA);eq(l.snapshot().balance(b),balanceB);eq(l.snapshot().entries.get(0).id,entry);eq(l.snapshot().entries.size(),2);
  eq(l.snapshot().accounts.get(1).openingDate,"2026-11-01");eq(l.snapshot().accounts.get(1).openingMinor,10000L);
  Ledger restarted=new Ledger(s);restarted.load();eq(ids(restarted),reordered);eq(restarted.snapshot().balance(b),balanceB);
  byte[] backup=l.backup();l.reorderAccounts(original);l.restore(backup);eq(ids(l),reordered);eq(l.snapshot().entries.get(0).id,entry);
  byte[] before=s.bytes.clone();s.bad=true;fail(()->l.reorderAccounts(original));eq(ids(l),reordered);eq(Arrays.equals(before,s.bytes),true);eq(l.snapshot().balance(a),balanceA);s.bad=false;
  fail(()->l.reorderAccounts(Arrays.asList(a,b)));fail(()->l.reorderAccounts(Arrays.asList(a,b,b)));fail(()->l.reorderAccounts(Arrays.asList(a,b,"missing")));fail(()->l.reorderAccounts(null));fail(()->l.reorderAccounts(Arrays.asList(a,b,null)));eq(ids(l),reordered);eq(Arrays.equals(before,s.bytes),true);
  int writes=s.writes;l.reorderAccounts(reordered);eq(s.writes,writes);
  l.renameAccount(a,"renamed");eq(ids(l),reordered);eq(l.snapshot().accounts.get(1).name,"renamed");
  String d=l.addAccount("new","EGP",0);eq(ids(l),Arrays.asList(c,a,b,d));l.reorderAccounts(Arrays.asList(d,b,a,c));l.load();eq(ids(l),Arrays.asList(d,b,a,c));
  s.readBad=true;fail(()->l.load());fail(()->l.reorderAccounts(Arrays.asList(c,a,b,d)));s.readBad=false;l.load();eq(ids(l),Arrays.asList(d,b,a,c));
  Store single=new Store();Ledger one=new Ledger(single);one.load();String x=one.addAccount("one","EGP",0);int oldWrites=single.writes;one.reorderAccounts(Collections.singletonList(x));eq(single.writes,oldWrites);
  System.out.println("PASS: "+n+" account order/persistence/rollback assertions");
 }
}
