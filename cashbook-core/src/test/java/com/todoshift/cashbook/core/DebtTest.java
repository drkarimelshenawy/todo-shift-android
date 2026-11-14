package com.todoshift.cashbook.core;
import java.util.*;import java.io.*;import java.time.*;import java.security.MessageDigest;
public final class DebtTest {
 static int checks;static void ok(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 interface Action{void run()throws Exception;}static void no(Action a)throws Exception{try{a.run();throw new AssertionError("accepted check "+checks);}catch(IOException|IllegalArgumentException e){checks++;}}
 static class Store implements Ledger.Store{byte[] bytes;boolean fail;public byte[] read(){return bytes;}public void write(byte[] b)throws IOException{if(fail)throw new IOException("disk");bytes=b;}}
 static Debt spec(Debt.Direction dir,Debt.Frequency freq,long part,String first,boolean remind){return new Debt("",dir,freq,part,first,Collections.emptyList(),remind,1,"09:00",Collections.emptyList());}
 static Debt debt(Ledger l,String id){return l.snapshot().debts.stream().filter(d->d.accountId.equals(id)).findFirst().get();}
 static byte[] v5()throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(0x43424b31);d.writeInt(5);d.writeInt(1);d.writeUTF("old");d.writeUTF("قديم");d.writeUTF("EGP");d.writeLong(12345);d.writeUTF("2026-01-01");d.writeUTF("وارد");d.writeUTF("صادر");d.writeUTF("💳");d.writeUTF("2026-01-02");d.writeBoolean(false);d.writeInt(0);d.writeUTF("إيراد");d.writeUTF("مصروف");d.writeInt(1);d.writeUTF("راتب");d.writeUTF("فواتير");d.writeInt(1);d.writeUTF("💳");d.writeInt(1);d.writeUTF("💳");d.flush();byte[] p=b.toByteArray();b.write(MessageDigest.getInstance("SHA-256").digest(p));return b.toByteArray();}
 public static void main(String[] args)throws Exception{
  Store store=new Store();store.bytes=v5();Ledger l=new Ledger(store);l.load();ok(l.snapshot().debts.isEmpty());ok(l.snapshot().balance("old")==12345);ok(l.snapshot().accounts.get(0).icon.equals("💳"));ok(l.snapshot().preferences.presets.size()==1);ok(l.snapshot().preferences.favorites.contains("💳"));
  String wallet=l.addAccount("محفظة","EGP",50000);String id=l.addDebt("لاب توب",10000,"2026-01-01","i:001",spec(Debt.Direction.PAYABLE,Debt.Frequency.MONTHLY,3000,"2026-01-31",true));
  Debt d=debt(l,id);ok(d.principal(l.snapshot())==10000);ok(d.remaining(l.snapshot())==10000);ok(d.paid(l.snapshot())==0);
  List<Debt.Due> rows=d.schedule(l.snapshot());ok(rows.size()==4);ok(rows.get(1).date.equals("2026-02-28"));ok(rows.get(2).date.equals("2026-03-31"));ok(rows.get(3).amountMinor==1000);ok(rows.get(0).status(LocalDate.of(2026,2,1)).equals("OVERDUE"));
  String pay=l.settle(id,wallet,2500,"2026-01-31","10:00","جزئي",null);ok(l.snapshot().balance(wallet)==47500);ok(d.remaining(l.snapshot())==7500);ok(d.paid(l.snapshot())==2500);ok(l.snapshot().total(Ledger.Kind.INCOME)==0);ok(l.snapshot().total(Ledger.Kind.EXPENSE)==0);ok(d.schedule(l.snapshot()).get(0).remaining()==500);ok(l.snapshot().entries.size()==1);
  no(()->l.post(Ledger.Kind.EXPENSE,id,null,100,"","2026-02-01"));no(()->l.post(Ledger.Kind.INCOME,id,null,100,"","2026-02-01"));no(()->l.post(Ledger.Kind.TRANSFER,id,wallet,100,"","2026-02-01"));no(()->l.settle(id,wallet,8000,"2026-02-01","","",null));ok(d.remaining(l.snapshot())==7500);
  l.settle(id,wallet,3000,"2026-02-01","11:00","تعديل",pay);ok(l.snapshot().balance(wallet)==47000);ok(d.schedule(l.snapshot()).get(0).remaining()==0);ok(l.snapshot().entries.size()==1);
  l.setDeleted(pay,true);ok(d.remaining(l.snapshot())==10000);ok(l.snapshot().balance(wallet)==50000);l.setDeleted(pay,false);ok(d.remaining(l.snapshot())==7000);
  no(()->l.postpone(id,0,"2026-06-01"));l.postpone(id,1,"2026-05-01");d=debt(l,id);ok(d.changes.size()==1);ok(d.changes.get(0).oldDate.equals("2026-02-28"));ok(d.schedule(l.snapshot()).get(1).index==2);ok(d.schedule(l.snapshot()).stream().anyMatch(r->r.date.equals("2026-05-01")));
  String rec=l.addDebt("فلوس لي",7000,"2026-01-01","",spec(Debt.Direction.RECEIVABLE,Debt.Frequency.OPEN,0,"2026-06-01",true));String collect=l.settle(rec,wallet,2000,"2026-02-02","09:00","تحصيل",null);ok(l.snapshot().balance(wallet)==49000);ok(debt(l,rec).remaining(l.snapshot())==5000);ok(l.snapshot().total(Ledger.Kind.INCOME)==0);ok(l.snapshot().total(Ledger.Kind.EXPENSE)==0);
  no(()->l.post(Ledger.Kind.TRANSFER,wallet,rec,100,"","2026-02-02"));no(()->l.settle(id,rec,100,"2026-02-02","","",null));no(()->l.settle(id,wallet,1,"2025-12-31","","",null));
  byte[] before=store.bytes;store.fail=true;no(()->l.settle(id,wallet,10,"2026-02-03","","",null));ok(Arrays.equals(before,store.bytes));ok(debt(l,id).remaining(l.snapshot())==7000);no(()->l.postpone(id,2,"2026-07-01"));ok(debt(l,id).changes.size()==1);store.fail=false;
  l.archiveAccount(id,true);ok(DebtReminders.plan(l.snapshot()).stream().noneMatch(e->e.accountId.equals(id)));no(()->l.settle(id,wallet,1,"2026-02-03","","",null));l.archiveAccount(id,false);
  ok(DebtReminders.plan(l.snapshot()).stream().anyMatch(e->e.accountId.equals(id)&&e.key.contains("2026-05-01")));ok(DebtReminders.plan(l.snapshot()).stream().noneMatch(e->e.key.startsWith(id+":0:")));
  byte[] backup=l.backup();Ledger copy=new Ledger(new Store());copy.load();copy.restore(backup);ok(copy.snapshot().debts.size()==2);ok(debt(copy,id).changes.size()==1);ok(debt(copy,rec).remaining(copy.snapshot())==5000);ok(copy.snapshot().accounts.get(0).icon.equals("💳"));ok(copy.snapshot().preferences.presets.size()==1);
  l.reorderAccounts(Arrays.asList(id,rec,wallet,"old"));l.renameAccount(id,"جديد");l.favoriteIcon("i:002");l.setDefaultLabels("دخل","خرج");ok(l.snapshot().debts.size()==2);ok(debt(l,id).changes.size()==1);
  String full=l.settle(id,wallet,7000,"2026-04-01","09:00","كامل",null);ok(debt(l,id).remaining(l.snapshot())==0);ok(DebtReminders.plan(l.snapshot()).stream().noneMatch(e->e.accountId.equals(id)));no(()->l.settle(id,wallet,1,"2026-04-01","","",null));
  l.setDeleted(full,true);ok(debt(l,id).remaining(l.snapshot())==7000);ok(DebtReminders.plan(l.snapshot()).stream().anyMatch(e->e.accountId.equals(id)));l.setDeleted(full,false);
  no(()->l.deleteAccount(id));
  String free=l.addDebt("مرن",100,"2026-01-01","",spec(Debt.Direction.PAYABLE,Debt.Frequency.OPEN,0,"",false));ok(debt(l,free).schedule(l.snapshot()).isEmpty());l.deleteAccount(free);ok(l.snapshot().debts.size()==2);
  no(()->l.addDebt("صفر",0,"2026-01-01","",spec(Debt.Direction.PAYABLE,Debt.Frequency.OPEN,0,"",false)));
  no(()->l.addDebt("مبلغ",1000,"2026-01-01","",spec(Debt.Direction.PAYABLE,Debt.Frequency.MONTHLY,0,"2026-02-01",false)));
  no(()->l.addDebt("كثير",10000,"2026-01-01","",spec(Debt.Direction.PAYABLE,Debt.Frequency.WEEKLY,1,"2026-02-01",false)));
  no(()->l.addDebt("بدون موعد",100,"2026-01-01","",spec(Debt.Direction.PAYABLE,Debt.Frequency.OPEN,0,"",true)));
  Debt custom=new Debt("",Debt.Direction.PAYABLE,Debt.Frequency.CUSTOM,300,"2026-02-01",Arrays.asList("2026-02-01","2026-02-15","2026-05-03"),false,1,"09:00",Collections.emptyList());String c=l.addDebt("مخصص",800,"2026-01-01","",custom);ok(debt(l,c).schedule(l.snapshot()).get(2).amountMinor==200);
  l.configureDebt(rec,8000,"2026-01-01",spec(Debt.Direction.RECEIVABLE,Debt.Frequency.WEEKLY,2000,"2026-02-01",true));ok(debt(l,rec).remaining(l.snapshot())==6000);ok(debt(l,rec).schedule(l.snapshot()).get(1).date.equals("2026-02-08"));
  Ledger reload=new Ledger(store);reload.load();ok(reload.snapshot().debts.size()==3);ok(debt(reload,id).remaining(reload.snapshot())==0);ok(debt(reload,id).changes.size()==1);
  for(String q:Arrays.asList("سيارة","سياره","سَيَّارَة","عربية","كار","CAR","auto"))ok(AccountIconCatalog.search(q).stream().anyMatch(i->i.name.equals("car")));
  for(String q:Arrays.asList("موبايل","laptop","تلفزيون","صيانه","موتوسيكل","دفتر","فيزا","سماعة","طبيب","قلم","كتابة"))ok(!AccountIconCatalog.search(q).isEmpty());
  ok(AccountIconCatalog.ALL.size()==187);ok(AccountIconCatalog.ALL.stream().map(i->i.id).distinct().count()==187);ok(AccountIconCatalog.ALL.stream().allMatch(i->i.id.length()<=16));
  if(args.length>0)java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),v5());
  System.out.println("PASS: "+checks+" debt/settlement/schedule/reminders/icons/v5 migration assertions");
 }
}
