package com.todoshift.cashbook.core;
import java.io.*;import java.util.*;import java.security.MessageDigest;
public class Release12Test{
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("check "+n);}interface Action{void run()throws Exception;}static void no(Action a)throws Exception{try{a.run();throw new AssertionError("accepted "+n);}catch(IllegalArgumentException|ArithmeticException|IOException e){n++;}}
 static class Store implements Ledger.Store{byte[] b;boolean fail;public byte[] read(){return b;}public void write(byte[] x)throws IOException{if(fail)throw new IOException("disk");b=x;}}
 static Debt d(Ledger l,String id){return l.snapshot().debts.stream().filter(x->x.accountId.equals(id)).findFirst().get();}
 static void account(DataOutputStream out,String id,String name,long amount,String icon)throws Exception{out.writeUTF(id);out.writeUTF(name);out.writeUTF("EGP");out.writeLong(amount);out.writeUTF("2026-01-01");out.writeUTF("إيراد");out.writeUTF("مصروف");out.writeUTF(icon);out.writeUTF("2026-01-01");out.writeBoolean(false);}
 public static byte[] legacy()throws Exception{
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeInt(0x43424b31);out.writeInt(6);out.writeInt(2);account(out,"old","محفظة قديمة",12345,"💳");account(out,"legacy-debt","مديونية قديمة",-30050,"i:006");
  out.writeInt(1);out.writeUTF("old-payment");out.writeUTF("TRANSFER");out.writeUTF("old");out.writeUTF("legacy-debt");out.writeLong(5050);out.writeUTF("ملاحظة قديمة");out.writeUTF("2026-11-12");out.writeUTF("09:00");out.writeBoolean(false);out.writeInt(0);out.writeInt(0);
  out.writeUTF("إيراد");out.writeUTF("مصروف");out.writeInt(1);out.writeUTF("راتب");out.writeUTF("فواتير");out.writeInt(1);out.writeUTF("💳");out.writeInt(1);out.writeUTF("💳");
  out.writeInt(1);out.writeUTF("legacy-debt");out.writeUTF("PAYABLE");out.writeUTF("MONTHLY");out.writeLong(10000);out.writeUTF("2026-12-01");out.writeBoolean(false);out.writeInt(1);out.writeUTF("09:00");out.writeInt(0);out.writeInt(0);out.flush();byte[] payload=bytes.toByteArray();bytes.write(MessageDigest.getInstance("SHA-256").digest(payload));return bytes.toByteArray();
 }
 public static void main(String[] args)throws Exception{
  for(String input:Arrays.asList("123","١٢٣","۱۲۳"))ok(WholeMoney.parse(input,false,null)==12300);
  ok(WholeMoney.parse("-٥٠",true,null)==-5000);ok(WholeMoney.format(170000).equals("1,700"));ok(WholeMoney.format(12345).equals("123.45"));ok(WholeMoney.edit(30000).equals("300"));ok(WholeMoney.edit(30050).equals("300.5"));
  for(String input:Arrays.asList("1.5","1.00","1,000","word","-1","","1e2"))no(()->WholeMoney.parse(input,false,null));
  ok(WholeMoney.parse("50.5",false,5050L)==5050);no(()->WholeMoney.parse("50.6",false,5050L));no(()->WholeMoney.parse("9223372036854775807",false,null));ok(!WholeMoney.input("123a",false));ok(!WholeMoney.input("12.3",false));ok(WholeMoney.input("-",true));
  Store s=new Store();s.b=legacy();byte[] old=s.b.clone();Ledger l=new Ledger(s);l.load();ok(Arrays.equals(old,s.b));ok(l.snapshot().entries.get(0).minor==5050);ok(l.snapshot().balance("old")==7295);ok(d(l,"legacy-debt").remaining(l.snapshot())==25000);ok(d(l,"legacy-debt").principal(l.snapshot())==30050);
  l.settle("legacy-debt","",6000,"2026-11-12","09:00","تعديل","old-payment",-1,false);ok(l.snapshot().balance("old")==12345);ok(d(l,"legacy-debt").remaining(l.snapshot())==24050);ok(l.snapshot().entries.get(0).kind==Ledger.Kind.SETTLEMENT);
  String p=l.settle("legacy-debt","",4000,"2026-11-12","10:00","",null,0,false);ok(d(l,"legacy-debt").schedule(l.snapshot()).get(0).remaining()==0);ok(l.snapshot().balance("old")==12345);
  l.setDeleted(p,true);ok(d(l,"legacy-debt").remaining(l.snapshot())==24050);l.setDeleted(p,false);ok(d(l,"legacy-debt").remaining(l.snapshot())==20050);
  l.settle("legacy-debt","old",4000,"2026-11-12","10:00","",p,0,false);ok(l.snapshot().balance("old")==8345);l.settle("legacy-debt","",4000,"2026-11-12","10:00","",p,0,false);ok(l.snapshot().balance("old")==12345);
  ok(l.duplicateSettlement("legacy-debt","",4000,"2026-11-12",null));ok(!l.duplicateSettlement("legacy-debt","old",4000,"2026-11-12",null));ok(!l.duplicateSettlement("legacy-debt","",4000,"2026-11-12",p));
  String full=l.settle("legacy-debt","",20050,"2026-11-12","11:00","",null,-1,false);ok(d(l,"legacy-debt").remaining(l.snapshot())==0);ok(l.snapshot().balance("old")==12345);no(()->l.settle("legacy-debt","",1,"2026-11-12","","",null));
  Debt spec=new Debt("",Debt.Direction.PAYABLE,Debt.Frequency.MONTHLY,10000,"2026-12-01",Collections.emptyList(),true,1,"09:00",Collections.emptyList());String id=l.addDebt("جديد",50000,"2026-11-12","",spec);
  String early=l.settle(id,"",15000,"2026-11-12","09:00","",null,1,true);Debt debt=d(l,id);ok(debt.remaining(l.snapshot())==35000);ok(debt.advanceCredit(l.snapshot())==5000);ok(debt.schedule(l.snapshot()).stream().filter(x->x.index==1).findFirst().get().paidMinor==10000);ok(debt.schedule(l.snapshot()).get(0).creditMinor==5000);ok(l.snapshot().balance("old")==12345);
  l.distributeCredit(id);ok(d(l,id).advanceCredit(l.snapshot())==0);ok(d(l,id).remaining(l.snapshot())==35000);ok(l.snapshot().entries.stream().filter(e->e.id.equals(early)).findFirst().get().installmentIndex==1);
  l.setDeleted(early,true);ok(debt.remaining(l.snapshot())==50000);l.setDeleted(early,false);ok(debt.remaining(l.snapshot())==35000);
  String rec=l.addDebt("لي",9000,"2026-11-12","",new Debt("",Debt.Direction.RECEIVABLE,Debt.Frequency.OPEN,0,"",Collections.emptyList(),false,1,"09:00",Collections.emptyList()));l.settle(rec,"",3000,"2026-11-12","09:00","",null);ok(d(l,rec).remaining(l.snapshot())==6000);ok(d(l,rec).advanceCredit(l.snapshot())==0);ok(l.snapshot().balance("old")==12345);
  ok(l.snapshot().total(Ledger.Kind.INCOME)==0);ok(l.snapshot().total(Ledger.Kind.EXPENSE)==0);
  byte[] before=s.b.clone();s.fail=true;no(()->l.settle(id,"old",100,"2026-11-12","","",null));ok(Arrays.equals(before,s.b));ok(debt.remaining(l.snapshot())==35000);s.fail=false;
  byte[] backup=l.backup();Ledger copy=new Ledger(new Store());copy.load();copy.restore(backup);ok(copy.snapshot().entries.size()==l.snapshot().entries.size());ok(copy.snapshot().balance("old")==12345);ok(d(copy,id).remaining(copy.snapshot())==35000);ok(copy.snapshot().preferences.favorites.contains("💳"));
  copy.restore(backup);ok(copy.snapshot().entries.size()==l.snapshot().entries.size());ok(copy.snapshot().accounts.size()==l.snapshot().accounts.size());
  Ledger reload=new Ledger(s);reload.load();ok(d(reload,id).remaining(reload.snapshot())==35000);ok(reload.snapshot().entries.stream().anyMatch(e->e.kind==Ledger.Kind.SETTLEMENT&&e.installmentIndex==0));
  if(args.length>0)java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),legacy());
  System.out.println("PASS: "+n+" v0.12 unlinked/targeted/early-credit/whole-money/legacy/backup assertions");
 }
}
