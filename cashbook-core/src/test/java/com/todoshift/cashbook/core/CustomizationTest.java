package com.todoshift.cashbook.core;
import java.io.*;import java.util.*;import java.security.*;
public final class CustomizationTest {
 static int n;static void eq(Object a,Object b){n++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 interface Run{void run()throws Exception;}static void fail(Run r)throws Exception{n++;try{r.run();}catch(Exception ok){return;}throw new AssertionError("Expected failure");}
 static class Store implements Ledger.Store{byte[] bytes;boolean bad,readBad;public byte[] read()throws IOException{if(readBad)throw new IOException("read");return bytes;}public void write(byte[] b)throws IOException{if(bad)throw new IOException("full");bytes=b;}}
 static Ledger.Account account(Ledger l,String id){return l.snapshot().accounts.stream().filter(a->a.id.equals(id)).findFirst().get();}
 static byte[] legacyV3()throws Exception{
  ByteArrayOutputStream buf=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(buf);
  o.writeInt(0x43424B31);o.writeInt(3);o.writeInt(1);o.writeUTF("legacy");o.writeUTF("حساب قديم");o.writeUTF("EGP");o.writeLong(100000);o.writeUTF("2026-11-01");
  o.writeInt(1);o.writeUTF("old-entry");o.writeUTF("INCOME");o.writeUTF("legacy");o.writeUTF("");o.writeLong(23450);o.writeUTF("عملية قديمة");o.writeUTF("2026-11-10");o.writeUTF("10:00");o.writeBoolean(false);o.writeInt(0);o.writeInt(0);o.flush();
  byte[] p=buf.toByteArray();buf.write(MessageDigest.getInstance("SHA-256").digest(p));return buf.toByteArray();
 }
 public static void main(String[]args)throws Exception{
  Store s=new Store();Ledger l=new Ledger(s);l.load();eq(l.snapshot().defaults.incoming,"إيراد");eq(l.snapshot().defaults.outgoing,"مصروف");
  String old=l.addAccount("old","EGP",10000,"2026-11-01");l.post(Ledger.Kind.EXPENSE,old,null,100,"original","2026-11-11");
  l.setDefaultLabels("قبض","دفع");eq(account(l,old).incomingLabel,"إيراد");eq(account(l,old).outgoingLabel,"مصروف");
  String a=l.addAccount("next","EGP",0);eq(account(l,a).incomingLabel,"قبض");eq(account(l,a).outgoingLabel,"دفع");eq(account(l,a).icon,"");
  String b=l.addAccount("custom","EGP",0,"2026-11-01","إيداع","سحب","💳");eq(account(l,b).incomingLabel,"إيداع");eq(account(l,b).outgoingLabel,"سحب");eq(account(l,b).icon,"💳");
  l.customizeAccount(old,"renamed","مرتب","فواتير","🏠");eq(account(l,old).name,"renamed");eq(account(l,old).incomingLabel,"مرتب");eq(account(l,old).outgoingLabel,"فواتير");eq(account(l,old).icon,"🏠");eq(account(l,old).openingMinor,10000L);eq(account(l,old).openingDate,"2026-11-01");eq(l.snapshot().balance(old),9900L);eq(l.snapshot().entries.get(0).note,"original");eq(l.snapshot().entries.get(0).kind,Ledger.Kind.EXPENSE);eq(account(l,a).incomingLabel,"قبض");
  l.renameAccount(old,"name only");eq(account(l,old).incomingLabel,"مرتب");eq(account(l,old).icon,"🏠");
  l.reorderAccounts(Arrays.asList(b,old,a));eq(l.snapshot().accounts.get(0).id,b);eq(account(l,b).icon,"💳");eq(l.snapshot().defaults.incoming,"قبض");
  // Labels never change the actual operation kind, including a label spelling "transfer".
  l.customizeAccount(b,"custom","استلام","تحويل","★");String e=l.post(Ledger.Kind.EXPENSE,b,null,10,"label only","2026-11-11");eq(l.snapshot().balance(b),-10L);eq(l.snapshot().entries.get(1).kind,Ledger.Kind.EXPENSE);
  l.post(Ledger.Kind.TRANSFER,old,b,50,"real transfer","2026-11-11");eq(l.snapshot().balance(old),9850L);eq(l.snapshot().balance(b),40L);eq(l.snapshot().defaults.outgoing,"دفع");
  l.edit(e,Ledger.Kind.EXPENSE,b,null,20,"edited","2026-11-11","11:00");l.setDeleted(e,true);eq(l.snapshot().defaults.incoming,"قبض");eq(account(l,b).outgoingLabel,"تحويل");
  Ledger restart=new Ledger(s);restart.load();eq(account(restart,old).icon,"🏠");eq(restart.snapshot().defaults.outgoing,"دفع");eq(restart.snapshot().accounts.get(0).id,b);
  byte[] backup=l.backup();l.setDefaultLabels("دخل","إنفاق");eq(account(l,a).incomingLabel,"قبض");l.restore(backup);eq(l.snapshot().defaults.incoming,"قبض");eq(account(l,b).icon,"★");
  byte[] before=s.bytes.clone();s.bad=true;fail(()->l.customizeAccount(old,"bad","x","y","💼"));eq(account(l,old).name,"name only");eq(account(l,old).icon,"🏠");eq(Arrays.equals(before,s.bytes),true);fail(()->l.setDefaultLabels("x","y"));eq(l.snapshot().defaults.incoming,"قبض");s.bad=false;
  fail(()->l.customizeAccount(old,"ok"," ","x",""));fail(()->l.customizeAccount(old,"ok","x","\n",""));fail(()->l.customizeAccount(old,"ok",String.join("",Collections.nCopies(31,"x")),"y",""));fail(()->l.customizeAccount(old,"ok","x","y",String.join("",Collections.nCopies(17,"x"))));fail(()->l.setDefaultLabels("","y"));fail(()->l.customizeAccount("missing","ok","x","y",""));eq(Arrays.equals(before,s.bytes),true);
  s.readBad=true;fail(()->l.load());fail(()->l.setDefaultLabels("x","y"));s.readBad=false;l.load();eq(l.snapshot().defaults.incoming,"قبض");
  Store legacy=new Store();legacy.bytes=legacyV3();Ledger migrated=new Ledger(legacy);migrated.load();eq(migrated.snapshot().balance("legacy"),123450L);eq(migrated.snapshot().accounts.get(0).icon,"");eq(migrated.snapshot().accounts.get(0).incomingLabel,"إيراد");eq(migrated.snapshot().entries.get(0).id,"old-entry");eq(migrated.snapshot().defaults.outgoing,"مصروف");
  migrated.customizeAccount("legacy","حساب قديم","وارد","صادر","💼");migrated.load();eq(migrated.snapshot().balance("legacy"),123450L);eq(migrated.snapshot().accounts.get(0).icon,"💼");
  if(args.length>0)try(FileOutputStream f=new FileOutputStream(args[0])){f.write(legacyV3());}
  System.out.println("PASS: "+n+" customization/defaults/emoji/v3 migration assertions");
 }
}
