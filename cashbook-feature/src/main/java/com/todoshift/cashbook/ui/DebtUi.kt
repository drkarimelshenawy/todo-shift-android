package com.todoshift.cashbook.ui
import com.todoshift.cashbook.R

import android.app.TimePickerDialog
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todoshift.cashbook.core.*
import java.time.*

@androidx.compose.runtime.NonSkippableComposable @Composable internal fun DebtSettingsDialog(existing:Debt?,dismiss:()->Unit){
 val vm=LocalCashBookVM.current;val state by vm.state.collectAsState();val snapshot=state.snapshot?:return;val a=existing?.account(snapshot)
 var name by rememberSaveable{mutableStateOf(a?.name.orEmpty())};var icon by rememberSaveable{mutableStateOf(a?.icon?:"i:006")}
 var total by rememberSaveable{mutableStateOf(existing?.let{com.todoshift.cashbook.core.WholeMoney.edit(it.principal(snapshot))}.orEmpty())}
 var start by rememberSaveable{mutableStateOf(a?.openingDate?.ifEmpty{LocalDate.now().toString()}?:LocalDate.now().toString())}
 var direction by rememberSaveable{mutableStateOf(existing?.direction?.name?:"PAYABLE")};var frequency by rememberSaveable{mutableStateOf(existing?.frequency?.name?:"OPEN")}
 var amount by rememberSaveable{mutableStateOf(existing?.installmentMinor?.takeIf{it>0}?.let(com.todoshift.cashbook.core.WholeMoney::edit).orEmpty())}
 var first by rememberSaveable{mutableStateOf(existing?.firstDue?.ifEmpty{LocalDate.now().plusMonths(1).toString()}?:LocalDate.now().plusMonths(1).toString())}
 var target by rememberSaveable{mutableStateOf(existing?.firstDue?.isNotBlank()?:false)}
 var remind by rememberSaveable{mutableStateOf(existing?.reminder?:false)};var before by rememberSaveable{mutableStateOf((existing?.beforeDays?:1).toString())};var clock by rememberSaveable{mutableStateOf(existing?.reminderTime?:"09:00")}
 var datesText by rememberSaveable{mutableStateOf(existing?.customDates?.joinToString("\n").orEmpty())}
 var validation by remember{mutableStateOf<String?>(null)}
 var menu by remember{mutableStateOf(false)};var dates by remember{mutableStateOf(false)};var discard by remember{mutableStateOf(false)}
 val context=LocalContext.current
 val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){remind=it}
 val schedule=frequency!="OPEN";val locked=existing?.changes?.isNotEmpty()==true
 fun spec()=Debt(existing?.accountId.orEmpty(),Debt.Direction.valueOf(direction),Debt.Frequency.valueOf(frequency),if(schedule)com.todoshift.cashbook.core.WholeMoney.parse(amount,false,existing?.installmentMinor)else 0,if(schedule||target)first else "",if(frequency=="CUSTOM")datesText.lines().filter{it.isNotBlank()}else emptyList(),remind,before.toIntOrNull()?:1,clock,existing?.changes.orEmpty())
 val count=runCatching{val t=Ledger.parseMoney(total,false);val unit=Ledger.parseMoney(amount,false);if(t>0&&unit>0)1+(t-1)/unit else 0}.getOrDefault(0)
 val close:()->Unit={if(!state.busy)discard=true}
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true)){
  ResizeCashBookDialog()
  Surface(Modifier.fillMaxWidth(0.94f).widthIn(max=460.dp).imePadding().heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*0.90f).dp),shape=MaterialTheme.shapes.large){Column(Modifier.padding(14.dp)){
   Text(if(existing==null)tr(R.string.cb_172) else tr(R.string.cb_173),style=MaterialTheme.typography.titleLarge)
   Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){
    if(existing==null){OutlinedTextField(name,{name=it.take(100)},singleLine=true,label={Text(tr(R.string.cb_174))},trailingIcon={AccountIconEditor(icon,name,!state.busy){icon=it}},modifier=Modifier.fillMaxWidth().testTag("debt_name"))}
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(direction=="PAYABLE",{direction="PAYABLE"},enabled=existing==null,label={Text(tr(R.string.cb_175))},modifier=Modifier.testTag("debt_payable"));FilterChip(direction=="RECEIVABLE",{direction="RECEIVABLE"},enabled=existing==null,label={Text(tr(R.string.cb_176))},modifier=Modifier.testTag("debt_receivable"))}
    MoneyField(total,{total=it},singleLine=true,label={Text(tr(R.string.cb_177))},modifier=Modifier.fillMaxWidth().testTag("debt_total"))
    DayPicker(tr(R.string.cb_178),start,!state.busy){start=it}
    Box{OutlinedButton({menu=true},enabled=!locked,modifier=Modifier.fillMaxWidth().testTag("debt_frequency")){Text(when(frequency){"OPEN"->tr(R.string.cb_179);"MONTHLY"->tr(R.string.cb_180);"WEEKLY"->tr(R.string.cb_181);else->tr(R.string.cb_182)})};DropdownMenu(menu,{menu=false}){listOf("OPEN" to tr(R.string.cb_179),"MONTHLY" to tr(R.string.cb_019),"WEEKLY" to tr(R.string.cb_018),"CUSTOM" to tr(R.string.cb_183)).forEach{(key,label)->DropdownMenuItem(text={Text(label)},onClick={frequency=key;menu=false},modifier=Modifier.testTag("frequency_$key"))}}}
    if(schedule){MoneyField(amount,{amount=it},singleLine=true,enabled=!locked,label={Text(tr(R.string.cb_184))},modifier=Modifier.fillMaxWidth().testTag("debt_installment"))}
    else Row(verticalAlignment=Alignment.CenterVertically){Checkbox(target,{target=it;if(!it)remind=false});Text(tr(R.string.cb_185))}
    if(schedule||target)DayPicker(if(schedule)tr(R.string.cb_186) else tr(R.string.cb_187),first,!state.busy&&!locked){first=it}
    if(frequency=="CUSTOM"){
     OutlinedButton({dates=true},enabled=!locked,modifier=Modifier.fillMaxWidth()){Text(tr(R.string.cb_188 ,datesText.lines().count{it.isNotBlank()},count))}
     if(dates)AlertDialog(onDismissRequest={dates=false},title={Text(tr(R.string.cb_189))},text={Column{Text(tr(R.string.cb_190 ,count));LazyColumn(Modifier.heightIn(max=280.dp)){val values=datesText.lines().filter{it.isNotBlank()};items(values.indices.toList()){index->Row{DayPicker("${index+1}",values[index]){day->datesText=values.toMutableList().apply{set(index,day)}.joinToString("\n")};TextButton({datesText=values.filterIndexed{i,_->i!=index}.joinToString("\n")}){Text(tr(R.string.cb_140))}}}};TextButton({val values=datesText.lines().filter{it.isNotBlank()};if(values.size<600)datesText=(values+(values.lastOrNull()?.let(LocalDate::parse)?.plusMonths(1)?.toString()?:first)).joinToString("\n")}){Text(tr(R.string.cb_191))}}},confirmButton={TextButton({dates=false}){Text(tr(R.string.cb_067))}})
    }
    if(schedule&&count in 1..600){val end=runCatching{when(frequency){"MONTHLY"->LocalDate.parse(first).plusMonths(count-1).toString();"WEEKLY"->LocalDate.parse(first).plusWeeks(count-1).toString();else->datesText.lines().lastOrNull().orEmpty()}}.getOrDefault("");Text(tr(R.string.cb_192 ,count,end),style=MaterialTheme.typography.bodySmall)}
    Row(verticalAlignment=Alignment.CenterVertically){Switch(remind,{enabled->if(enabled&&Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)request.launch(Manifest.permission.POST_NOTIFICATIONS)else remind=enabled},enabled=schedule||target,modifier=Modifier.testTag("debt_reminder"));Text(tr(R.string.cb_193))}
    if(remind){OutlinedTextField(before,{before=it.filter(Char::isDigit).take(2)},singleLine=true,label={Text(tr(R.string.cb_194))},modifier=Modifier.fillMaxWidth());TextButton({val t=LocalTime.parse(clock);TimePickerDialog(CashBookText.dialogContext(context),{_,h,m->clock=String.format(java.util.Locale.ROOT,"%02d:%02d",h,m)},t.hour,t.minute,true).apply{setOnShowListener{window?.decorView?.layoutDirection=android.view.View.LAYOUT_DIRECTION_LTR};show()}}){Text(tr(R.string.cb_195 ,clock))};Text(tr(R.string.cb_196),style=MaterialTheme.typography.bodySmall)}
    if(locked)Text(tr(R.string.cb_197),style=MaterialTheme.typography.bodySmall)
    validation?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(close,enabled=!state.busy){Text(tr(R.string.cb_034))};TextButton({runCatching{spec()}.onSuccess{if(existing==null)vm.debt(name,total,start,icon,it,dismiss)else vm.configureDebt(existing.accountId,total,start,it,dismiss)}.onFailure{validation=tr(R.string.cb_198)}},enabled=!state.busy&&total.isNotBlank()&&(existing!=null||name.isNotBlank())&&(!schedule||amount.isNotBlank())&&(!remind||before.toIntOrNull() in 0..30),modifier=Modifier.testTag("debt_save")){Text(tr(R.string.cb_145))}}
  }}
 }
 if(discard)DiscardDialog({discard=false},dismiss)
}

private val PaidColor=androidx.compose.ui.graphics.Color(0xFF27865B)
private val Ink=androidx.compose.ui.graphics.Color(0xFF172A36)
private val Muted=androidx.compose.ui.graphics.Color(0xFF82929E)
@androidx.compose.runtime.NonSkippableComposable @Composable private fun StatusMark(status:String){
 val color=when(status){"PAID","CREDIT"->PaidColor;"OVERDUE"->androidx.compose.ui.graphics.Color(0xFFCA5549);"PARTIAL"->androidx.compose.ui.graphics.Color(0xFFB47D23);else->Muted}
 androidx.compose.foundation.Canvas(Modifier.size(28.dp)){
  val center=androidx.compose.ui.geometry.Offset(size.width/2,size.height/2);val radius=size.width*0.38f
  drawCircle(color,radius,center,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
  if(status=="PAID"||status=="CREDIT"){drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.27f,size.height*.5f),androidx.compose.ui.geometry.Offset(size.width*.44f,size.height*.66f),2.dp.toPx());drawLine(color,androidx.compose.ui.geometry.Offset(size.width*.44f,size.height*.66f),androidx.compose.ui.geometry.Offset(size.width*.73f,size.height*.34f),2.dp.toPx())}
  else{drawLine(color,center,androidx.compose.ui.geometry.Offset(center.x,center.y-radius*.58f),2.dp.toPx());drawLine(color,center,androidx.compose.ui.geometry.Offset(center.x+radius*.5f,center.y),2.dp.toPx())}
 }
}
@androidx.compose.runtime.NonSkippableComposable @Composable private fun MoneyLabel(value:Long,large:Boolean=false,color:androidx.compose.ui.graphics.Color=Ink,modifier:Modifier=Modifier){
 Row(modifier.widthIn(max=180.dp).semantics(mergeDescendants=true){},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
  Text(referenceMoney(value),color=color,fontSize=if(large)30.sp else 20.sp,style=MaterialTheme.typography.titleMedium.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr),maxLines=1)
  Text(tr(R.string.cb_254),color=color,fontSize=11.sp)
 }
}
private fun dueStatus(status:String)=when(status){"PAID"->tr(R.string.cb_206);"CREDIT"->tr(R.string.cb_255);"PARTIAL"->tr(R.string.cb_208);"OVERDUE"->tr(R.string.cb_207);else->tr(R.string.cb_209)}
@androidx.compose.runtime.NonSkippableComposable @Composable internal fun DebtPanel(snapshot:Ledger.Snapshot,debt:Debt,modifier:Modifier=Modifier){
 val vm=LocalCashBookVM.current;val state by vm.state.collectAsState()
 var tab by rememberSaveable(debt.accountId){mutableIntStateOf(0)}
 var settings by remember{mutableStateOf(false)};var paying by remember{mutableStateOf(false)};var edit by remember{mutableStateOf<Ledger.Entry?>(null)}
 var target by remember{mutableIntStateOf(-1)};var selectedDue by remember{mutableStateOf<Debt.Due?>(null)};var selectedPayment by remember{mutableStateOf<Ledger.Entry?>(null)}
 var confirmDelete by remember{mutableStateOf<Ledger.Entry?>(null)};var postponing by remember{mutableStateOf<Debt.Due?>(null)};var postponedDate by remember{mutableStateOf("")}
 val remaining=debt.remaining(snapshot);val paid=debt.paid(snapshot);val rows=debt.schedule(snapshot);val today=LocalDate.now()
 val payments=snapshot.entries.filter{it.accountId==debt.accountId||it.targetId==debt.accountId}.sortedByDescending{it.date+it.time}
 Column(modifier.fillMaxWidth().padding(horizontal=12.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(if(debt.direction==Debt.Direction.PAYABLE)tr(R.string.cb_175)else tr(R.string.cb_176),style=MaterialTheme.typography.labelLarge,color=Muted);TextButton({settings=true},modifier=Modifier.testTag("debt_settings")){Text(tr(R.string.cb_199))}}
  Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp),color=androidx.compose.ui.graphics.Color.White){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text(tr(R.string.cb_200),color=Muted);Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){MoneyLabel(remaining,true,modifier=Modifier.testTag("debt_remaining"));Text("${(paid.toDouble()/debt.principal(snapshot)*100).toInt()}%",color=PaidColor,modifier=Modifier.testTag("debt_percent"))}
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(tr(R.string.cb_256),color=Muted,style=MaterialTheme.typography.labelSmall);MoneyLabel(paid,color=PaidColor)};Column{Text(tr(R.string.cb_257),color=Muted,style=MaterialTheme.typography.labelSmall);MoneyLabel(debt.principal(snapshot))}}
   LinearProgressIndicator(progress={paid.toFloat()/debt.principal(snapshot).toFloat()},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp).testTag("debt_progress"))
   Button({edit=null;target=-1;paying=true},enabled=!state.busy&&remaining>0,shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().testTag("debt_pay")){Text(if(debt.direction==Debt.Direction.PAYABLE)tr(R.string.cb_203)else tr(R.string.cb_204))}
  }}
  val credit=debt.advanceCredit(snapshot)
  if(credit>0)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(tr(R.string.cb_258 ,referenceMoney(credit)),Modifier.weight(1f),style=MaterialTheme.typography.labelSmall);TextButton({vm.distribute(debt.accountId)}){Text(tr(R.string.cb_259))}}
  TabRow(tab,containerColor=androidx.compose.ui.graphics.Color.Transparent){Tab(tab==0,{tab=0},text={Text(tr(R.string.cb_260))},modifier=Modifier.testTag("debt_tab_schedule"));Tab(tab==1,{tab=1},text={Text(tr(R.string.cb_212))},modifier=Modifier.testTag("debt_tab_payments"))}
  if(tab==0)LazyColumn(Modifier.weight(1f).testTag("debt_schedule"),contentPadding=PaddingValues(vertical=8.dp)){
   if(rows.isEmpty())item{Text(tr(R.string.cb_261),Modifier.padding(18.dp),color=Muted)}
   items(rows,key={it.index}){due->val status=due.status(today);Surface(onClick={selectedDue=due},color=androidx.compose.ui.graphics.Color.White,shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(vertical=3.dp).testTag("due_${due.index}")){
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=17.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Column(Modifier.widthIn(min=86.dp)){MoneyLabel(due.amountMinor,color=if(status=="PAID"||status=="CREDIT")PaidColor else Ink);if(due.remaining()>0&&due.remaining()!=due.amountMinor)Text(tr(R.string.cb_262 ,referenceMoney(due.remaining())),style=MaterialTheme.typography.labelSmall,color=Muted)}
     Column(Modifier.weight(1f),horizontalAlignment=if(CashBookText.arabic())Alignment.End else Alignment.Start){Text(referenceDayShort(due.date),fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,color=Ink,maxLines=2);Text(tr(R.string.cb_263 ,due.index+1,dueStatus(status)),style=MaterialTheme.typography.bodySmall,color=if(status=="PAID")PaidColor else Muted)}
     StatusMark(status)
    }
   }}
   if(debt.changes.isNotEmpty())item{Text(tr(R.string.cb_216),Modifier.padding(vertical=10.dp),style=MaterialTheme.typography.titleSmall);debt.changes.takeLast(10).forEach{c->Text(tr(R.string.cb_264 ,c.index+1,referenceDayShort(c.oldDate),referenceDayShort(c.newDate)),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(bottom=6.dp))}}
  }else LazyColumn(Modifier.weight(1f).testTag("debt_payments"),contentPadding=PaddingValues(vertical=8.dp)){
   if(payments.isEmpty())item{Text(tr(R.string.cb_265),Modifier.padding(18.dp),color=Muted)}
   items(payments,key={it.id}){entry->Surface(onClick={selectedPayment=entry},color=androidx.compose.ui.graphics.Color.White,shape=androidx.compose.foundation.shape.RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(vertical=3.dp).testTag("payment_${entry.id}")){
    Row(Modifier.fillMaxWidth().padding(13.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     MoneyLabel(entry.minor,color=if(entry.deleted)Muted else PaidColor)
     Column(Modifier.weight(1f),horizontalAlignment=if(CashBookText.arabic())Alignment.End else Alignment.Start){Text(referenceDayShort(entry.date),color=Ink);Text(if(entry.deleted)tr(R.string.cb_266) else if(entry.kind==Ledger.Kind.SETTLEMENT)tr(R.string.cb_267) else tr(R.string.cb_268),style=MaterialTheme.typography.bodySmall,color=Muted)}
     StatusMark(if(entry.deleted)"UPCOMING"else"PAID")
    }
   }}
  }
 }
 if(settings)DebtSettingsDialog(debt){settings=false}
 if(paying)SettlementDialog(snapshot,debt,edit,{paying=false;edit=null},target)
 selectedDue?.let{due->AlertDialog(onDismissRequest={selectedDue=null},title={Text(referenceDayShort(due.date))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){MoneyLabel(due.amountMinor);Text(dueStatus(due.status(today)));Text(tr(R.string.cb_269 ,referenceMoney(due.remaining())));if(due.remaining()>0)Button({target=due.index;edit=null;selectedDue=null;paying=true},modifier=Modifier.fillMaxWidth().testTag("due_pay_now")){Text(tr(R.string.cb_270))};if(due.paidMinor==0L&&due.creditMinor==0L)TextButton({postponedDate=LocalDate.parse(due.date).plusDays(1).toString();postponing=due;selectedDue=null},modifier=Modifier.testTag("due_postpone")){Text(tr(R.string.cb_271))}}},confirmButton={TextButton({selectedDue=null}){Text(tr(R.string.cb_029))}})}
 selectedPayment?.let{entry->AlertDialog(onDismissRequest={selectedPayment=null},title={Text(tr(R.string.cb_272))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){MoneyLabel(entry.minor);Text(referenceDayShort(entry.date));Text(entry.note);if(entry.kind!=Ledger.Kind.SETTLEMENT){val other=snapshot.accounts.find{it.id==if(entry.accountId==debt.accountId)entry.targetId else entry.accountId};Text(other?.name.orEmpty())}else Text(tr(R.string.cb_267));if(!entry.deleted)TextButton({edit=entry;target=entry.installmentIndex;paying=true;selectedPayment=null},modifier=Modifier.testTag("payment_edit")){Text(tr(R.string.cb_222))};TextButton({if(entry.deleted)vm.deleted(entry.id,false)else confirmDelete=entry;selectedPayment=null},modifier=Modifier.testTag("payment_delete")){Text(if(entry.deleted)tr(R.string.cb_037)else tr(R.string.cb_215))}}},confirmButton={TextButton({selectedPayment=null}){Text(tr(R.string.cb_029))}})}
 confirmDelete?.let{entry->AlertDialog(onDismissRequest={confirmDelete=null},title={Text(tr(R.string.cb_273))},text={Text(tr(R.string.cb_274))},confirmButton={TextButton({vm.deleted(entry.id,true);confirmDelete=null},modifier=Modifier.testTag("payment_delete_confirm")){Text(tr(R.string.cb_140))}},dismissButton={TextButton({confirmDelete=null}){Text(tr(R.string.cb_034))}})}
 postponing?.let{due->AlertDialog(onDismissRequest={postponing=null},title={Text(tr(R.string.cb_218 ,due.index+1))},text={Column{DayPicker(tr(R.string.cb_219),postponedDate,!state.busy){postponedDate=it};state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton({vm.postpone(debt.accountId,due.index,postponedDate){postponing=null}},enabled=!state.busy&&postponedDate>due.date,modifier=Modifier.testTag("postpone_save")){Text(tr(R.string.cb_220))}},dismissButton={TextButton({postponing=null}){Text(tr(R.string.cb_034))}})}
}

@androidx.compose.runtime.NonSkippableComposable @Composable internal fun SettlementDialog(snapshot:Ledger.Snapshot,debt:Debt,entry:Ledger.Entry?,dismiss:()->Unit,installment:Int=-1){
 val vm=LocalCashBookVM.current;val state by vm.state.collectAsState()
 val wallets=snapshot.accounts.filter{!it.archived&&snapshot.debts.none{d->d.accountId==it.id}}
 val index=entry?.installmentIndex?:installment
 val due=debt.schedule(snapshot).find{it.index==index}
 var linked by rememberSaveable{mutableStateOf(entry?.kind==Ledger.Kind.TRANSFER)}
 var source by rememberSaveable{mutableStateOf(entry?.takeIf{it.kind==Ledger.Kind.TRANSFER}?.let{if(it.accountId==debt.accountId)it.targetId else it.accountId}.orEmpty())}
 var amount by rememberSaveable{mutableStateOf(entry?.let{com.todoshift.cashbook.core.WholeMoney.edit(it.minor)}?:due?.let{com.todoshift.cashbook.core.WholeMoney.edit(it.remaining())}.orEmpty())}
 var legacyFull by rememberSaveable{mutableStateOf(entry==null&&due!=null&&due.remaining()%100L!=0L)}
 var day by rememberSaveable{mutableStateOf(entry?.date?:LocalDate.now().toString())};var note by rememberSaveable{mutableStateOf(entry?.note.orEmpty())};var menu by remember{mutableStateOf(false)};var discard by remember{mutableStateOf(false)};var duplicate by remember{mutableStateOf(false)};var validation by remember{mutableStateOf<String?>(null)}
 var early by rememberSaveable{mutableStateOf(entry?.earlyCredit?:false)}
 val value=runCatching{Ledger.parseMoney(amount,false)}.getOrDefault(0)
 val currentDue=due?:debt.schedule(snapshot).firstOrNull{it.remaining()>0}
 val surplus=currentDue!=null&&value>currentDue.remaining()
 fun save(){vm.settle(debt.accountId,if(linked)source else "",amount,day,entry?.time?:LocalTime.now().toString().take(5),note,entry?.id,if(index<0&&early&&surplus)currentDue?.index?:-1 else index,early&&surplus,legacyFull,dismiss)}
 fun submit(){validation=null;try{val original=entry?.minor?:if(legacyFull)due?.remaining()?:debt.remaining(snapshot)else null;val parsed=com.todoshift.cashbook.core.WholeMoney.parse(amount,false,original);require(parsed>0);if(snapshot.entries.any{!it.deleted&&it.id!=entry?.id&&(it.accountId==debt.accountId||it.targetId==debt.accountId)&&it.minor==parsed&&it.date==day&&(if(it.kind==Ledger.Kind.SETTLEMENT)"" else if(it.accountId==debt.accountId)it.targetId else it.accountId)==if(linked)source else ""})duplicate=true else save()}catch(_:Exception){validation=tr(R.string.cb_275)}}
 Dialog({if(!state.busy)discard=true},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=true)){
  ResizeCashBookDialog()
  Surface(Modifier.fillMaxWidth(.94f).widthIn(max=430.dp).imePadding().heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*.88f).dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(18.dp)){Column(Modifier.padding(16.dp)){
   Text(if(entry==null)tr(R.string.cb_203)else tr(R.string.cb_222),style=MaterialTheme.typography.titleLarge)
   Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
    MoneyField(amount,{amount=it;legacyFull=false},label={Text(tr(R.string.cb_225))},enabled=!state.busy,readOnly=legacyFull,modifier=Modifier.fillMaxWidth().testTag("payment_amount"))
    if(legacyFull)Text(tr(R.string.cb_276),style=MaterialTheme.typography.bodySmall)
    if(entry==null&&debt.remaining(snapshot)%100L!=0L)TextButton({amount=com.todoshift.cashbook.core.WholeMoney.edit(due?.remaining()?:debt.remaining(snapshot));legacyFull=true},modifier=Modifier.testTag("legacy_full")){Text(tr(R.string.cb_277))}
    DayPicker(tr(R.string.cb_226),day,!state.busy){day=it}
    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(linked,{linked=it},modifier=Modifier.testTag("payment_link"));Text(tr(R.string.cb_278),style=MaterialTheme.typography.bodySmall)}
    if(linked)Box{OutlinedButton({menu=true},modifier=Modifier.fillMaxWidth().testTag("payment_account")){Text(wallets.find{it.id==source}?.name?:tr(R.string.cb_044))};DropdownMenu(menu,{menu=false}){wallets.forEach{a->DropdownMenuItem(text={AccountName(a)},onClick={source=a.id;menu=false})}}}
    if(surplus){Text(tr(R.string.cb_279),style=MaterialTheme.typography.labelMedium);Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!early,{early=false});Text(tr(R.string.cb_280),style=MaterialTheme.typography.bodySmall)};Row(verticalAlignment=Alignment.CenterVertically){RadioButton(early,{early=true},modifier=Modifier.testTag("early_credit"));Text(tr(R.string.cb_281),style=MaterialTheme.typography.bodySmall)}}
    OutlinedTextField(note,{note=it.take(2000)},label={Text(tr(R.string.cb_227))},modifier=Modifier.fillMaxWidth())
    validation?.let{Text(it,color=MaterialTheme.colorScheme.error)};state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton({if(!state.busy)discard=true},modifier=Modifier.weight(1f)){Text(tr(R.string.cb_034))};TextButton({submit()},enabled=!state.busy&&amount.isNotBlank()&&(!linked||wallets.any{it.id==source}),modifier=Modifier.weight(1f).testTag("payment_save")){Text(tr(R.string.cb_228))}}
  }}
 }
 if(discard)DiscardDialog({discard=false},dismiss)
 if(duplicate)AlertDialog(onDismissRequest={duplicate=false},title={Text(tr(R.string.cb_282))},text={Text(tr(R.string.cb_283))},confirmButton={TextButton({duplicate=false;save()},modifier=Modifier.testTag("duplicate_continue")){Text(tr(R.string.cb_284))}},dismissButton={TextButton({duplicate=false}){Text(tr(R.string.cb_003))}})
}
