package com.todoshift.cashbook.core;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
public class CompactAccountsTest {
 static int checks;
 static void ok(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 interface Action{void run()throws Exception;}
 static void rejects(Action a)throws Exception{try{a.run();throw new AssertionError("accepted");}catch(IOException|IllegalArgumentException e){checks++;}}
 static class Memory implements Ledger.Store{byte[] data;boolean fail;public byte[] read(){return data;}public void write(byte[] b)throws IOException{if(fail)throw new IOException("disk");data=b;}}
 static Ledger.Account account(Ledger l,String id){return l.snapshot().accounts.stream().filter(a->a.id.equals(id)).findFirst().get();}
 static byte[] legacy()throws Exception{
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
  d.writeInt(0x43424B31);d.writeInt(4);d.writeInt(1);d.writeUTF("old");d.writeUTF("قديم");d.writeUTF("EGP");d.writeLong(-1000);d.writeUTF("2026-01-01");d.writeUTF("قبض");d.writeUTF("دفع");d.writeUTF("💳");d.writeInt(0);d.writeUTF("دخل");d.writeUTF("خرج");d.flush();byte[] p=b.toByteArray();b.write(MessageDigest.getInstance("SHA-256").digest(p));return b.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  Memory m=new Memory();m.data=legacy();Ledger l=new Ledger(m);l.load();
  ok(l.snapshot().balance("old")==-1000);ok(account(l,"old").icon.equals("💳"));ok(account(l,"old").incomingLabel.equals("قبض"));ok(account(l,"old").createdDate.isEmpty());ok(!account(l,"old").archived);ok(l.snapshot().preferences.presets.isEmpty());
  String a=l.addAccount("راتب","EGP",-25000,"2026-02-01","وارد","صادر","💼","2026-11-11");
  ok(account(l,a).openingMinor==-25000);ok(account(l,a).createdDate.equals("2026-11-11"));ok(account(l,a).openingDate.equals("2026-02-01"));
  l.savePreset("مرتب","فواتير");l.savePreset("مرتب","فواتير");ok(l.snapshot().preferences.presets.size()==1);
  l.useIcon("💳");l.useIcon("💼");l.useIcon("💳");ok(l.snapshot().preferences.recent.equals(Arrays.asList("💳","💼")));
  for(int i=0;i<14;i++)l.useIcon("★"+i);ok(l.snapshot().preferences.recent.size()==12);ok(l.snapshot().preferences.recent.get(0).equals("★13"));
  l.favoriteIcon("💼");ok(l.snapshot().preferences.favorites.contains("💼"));l.favoriteIcon("💼");ok(l.snapshot().preferences.favorites.isEmpty());l.favoriteIcon("💼");
  String entry=l.post(Ledger.Kind.INCOME,a,null,5000,"راتب","2026-11-11");
  l.archiveAccount(a,true);ok(account(l,a).archived);ok(l.snapshot().balance(a)==-20000);ok(l.relatedEntries(a)==1);rejects(()->l.deleteAccount(a));rejects(()->l.post(Ledger.Kind.EXPENSE,a,null,10,"","2026-11-11"));
  l.setDeleted(entry,true);rejects(()->l.deleteAccount(a));ok(l.relatedEntries(a)==1);l.setDeleted(entry,false);
  l.renameAccount(a,"اسمي");ok(account(l,a).archived);ok(account(l,a).createdDate.equals("2026-11-11"));
  l.customizeAccount(a,"معدل","إضافة","خصم","⭐");ok(account(l,a).archived);ok(account(l,a).openingDate.equals("2026-02-01"));
  l.editAccount(a,"معدل",-30000,"2026-03-01","2026-10-01","إضافة","خصم","⭐");ok(l.snapshot().balance(a)==-25000);ok(account(l,a).archived);ok(account(l,a).createdDate.equals("2026-10-01"));
  l.archiveAccount(a,false);ok(!account(l,a).archived);
  String b=l.addAccount("ثان","EGP",0);l.post(Ledger.Kind.TRANSFER,a,b,1000,"تحويل","2026-11-11");ok(l.relatedEntries(b)==1);rejects(()->l.deleteAccount(b));
  l.reorderAccounts(Arrays.asList(b,a,"old"));ok(l.snapshot().accounts.get(0).id.equals(b));ok(l.snapshot().preferences.presets.size()==1);ok(l.snapshot().preferences.favorites.contains("💼"));
  byte[] backup=l.backup();Ledger copy=new Ledger(new Memory());copy.load();copy.restore(backup);ok(copy.snapshot().accounts.get(0).id.equals(b));ok(copy.snapshot().preferences.recent.size()==12);ok(copy.snapshot().preferences.presets.get(0).incoming.equals("مرتب"));ok(copy.snapshot().preferences.favorites.contains("💼"));ok(copy.snapshot().balance(a)==-26000);
  Ledger reload=new Ledger(m);reload.load();ok(reload.snapshot().balance(a)==-26000);ok(account(reload,a).createdDate.equals("2026-10-01"));ok(reload.snapshot().preferences.presets.size()==1);
  byte[] before=m.data;m.fail=true;rejects(()->l.archiveAccount(a,true));ok(!account(l,a).archived);ok(Arrays.equals(before,m.data));rejects(()->l.savePreset("س","ص"));ok(l.snapshot().preferences.presets.size()==1);rejects(()->l.favoriteIcon("⭐"));ok(!l.snapshot().preferences.favorites.contains("⭐"));m.fail=false;
  rejects(()->l.editAccount(a,"معدل",0,"bad","2026-01-01","أ","ب",""));ok(account(l,a).openingMinor==-30000);
  String empty=l.addAccount("فارغ","EGP",-999);l.deleteAccount(empty);ok(l.snapshot().accounts.size()==3);
  l.removePreset(0);ok(l.snapshot().preferences.presets.isEmpty());rejects(()->l.savePreset("","ص"));rejects(()->l.useIcon("x\n"));
  System.out.println("PASS: "+checks+" compact accounts/archive/presets/preferences/v4 migration assertions");
 }
}
