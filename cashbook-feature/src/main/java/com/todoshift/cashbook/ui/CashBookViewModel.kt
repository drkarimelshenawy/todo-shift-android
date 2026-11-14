package com.todoshift.cashbook.ui
import com.todoshift.cashbook.R

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.todoshift.cashbook.core.Ledger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Scope one instance to the host Activity/root graph, never one per nested destination.
class CashBookViewModel @JvmOverloads constructor(app: Application, private val reminderHost: com.todoshift.cashbook.core.DebtReminders.Host = AndroidDebtReminderHost(app)) : AndroidViewModel(app) {
    private val ledger = Ledger(AndroidLedgerStore(app))
    data class State(val snapshot: Ledger.Snapshot? = null, val busy: Boolean = false,
                     val error: String? = null, val message: String? = null)
    private val mutable = MutableStateFlow(State())
    val state = mutable.asStateFlow()
    data class Undo(val id:String,val deleted:Boolean)
    val undo=kotlinx.coroutines.flow.MutableStateFlow<Undo?>(null)
    fun clearUndo(){undo.value=null}
    fun undoDelete(){if(mutable.value.busy)return;val action=undo.value?:return;undo.value=null;deleted(action.id,action.deleted)}
    private val requested=MutableStateFlow<String?>(null)
    val navigation=requested.asStateFlow()
    fun openDebt(id:String?){if(!id.isNullOrBlank())requested.value=id}
    fun navigationHandled(){requested.value=null}
    init { CashBookText.initialize(app);reload() }
    fun clearMessage() { mutable.value = mutable.value.copy(error = null, message = null) }
    private fun run(message: String? = null, done: () -> Unit = {}, failureMessage: String? = null, action: () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = null, message = null)
        viewModelScope.launch {
            try {
                var reminderFailed=false
                val snapshot = withContext(Dispatchers.IO) {
                    synchronized(Ledger::class.java) {
                    ledger.load()
                    action()
                    ledger.snapshot().also { snapshot ->
                        try { reminderHost.sync(snapshot) } catch(e:Exception) {
                            if(e is kotlinx.coroutines.CancellationException)throw e
                            reminderFailed=true
                        }
                    }
                    }
                }
                mutable.value = State(snapshot, message = if(reminderFailed) tr(R.string.cb_079) else message)
                done()
            } catch (e: Exception) {
                // Cancellation must remain cancellation.
                if (e is kotlinx.coroutines.CancellationException) throw e
                mutable.value = mutable.value.copy(busy = false,
                    error = failureMessage ?: if (e is IllegalArgumentException || e is ArithmeticException)
                        tr(R.string.cb_080)
                    else tr(R.string.cb_081))
            }
        }
    }
    fun reload() = run(action = { ledger.load() })
    fun account(name: String, opening: String, openingDate: String, done: () -> Unit) = run(tr(R.string.cb_082), done) {
        ledger.addAccount(name, "EGP", com.todoshift.cashbook.core.WholeMoney.parse(opening.ifBlank{"0"},true,null), openingDate)
    }
    fun renameAccount(id: String, name: String, done: () -> Unit) = run(tr(R.string.cb_083), done) { ledger.renameAccount(id, name) }
    fun reorderAccounts(ids: List<String>) = run(failureMessage = tr(R.string.cb_084)) { ledger.reorderAccounts(ids.toList()) }
    fun customizedAccount(name: String, opening: String, day: String, incoming: String, outgoing: String, icon: String, done: () -> Unit) = run(tr(R.string.cb_082), done, tr(R.string.cb_085)) {
        ledger.addAccount(name,"EGP",com.todoshift.cashbook.core.WholeMoney.parse(opening.ifBlank{"0"},true,null),day,incoming,outgoing,icon)
    }
    fun customizeAccount(id: String, name: String, incoming: String, outgoing: String, icon: String, done: () -> Unit) = run(tr(R.string.cb_086), done, tr(R.string.cb_087)) {
        ledger.customizeAccount(id,name,incoming,outgoing,icon)
    }
    fun defaultLabels(incoming: String, outgoing: String, done: () -> Unit) = run(tr(R.string.cb_088), done, tr(R.string.cb_089)) {
        ledger.setDefaultLabels(incoming,outgoing)
    }
    fun newAccount(name:String,opening:String,day:String,created:String,incoming:String,outgoing:String,icon:String,done:()->Unit)=run(tr(R.string.cb_082),done) {
        ledger.addAccount(name,"EGP",com.todoshift.cashbook.core.WholeMoney.parse(opening.ifBlank{"0"},true,null),day,incoming,outgoing,icon,created)
    }
    fun editAccount(id:String,name:String,opening:String,day:String,created:String,incoming:String,outgoing:String,icon:String,done:()->Unit)=run(tr(R.string.cb_090),done) {
        ledger.editAccount(id,name,com.todoshift.cashbook.core.WholeMoney.parse(opening.ifBlank{"0"},true,null),day,created,incoming,outgoing,icon)
    }
    fun savePreset(incoming:String,outgoing:String)=run(tr(R.string.cb_091)) {ledger.savePreset(incoming,outgoing)}
    fun removePreset(index:Int)=run(tr(R.string.cb_092)) {ledger.removePreset(index)}
    fun useIcon(icon:String)=run {ledger.useIcon(icon)}
    fun favoriteIcon(icon:String)=run {ledger.favoriteIcon(icon)}
    fun archiveAccount(id:String,archived:Boolean,done:()->Unit)=run(if(archived) tr(R.string.cb_093) else tr(R.string.cb_094),done) {ledger.archiveAccount(id,archived)}
    fun deleteAccount(id:String,done:()->Unit)=run(tr(R.string.cb_095),done,failureMessage=tr(R.string.cb_096)) {ledger.deleteAccount(id)}
    fun debt(name:String,principal:String,start:String,icon:String,spec:com.todoshift.cashbook.core.Debt,done:()->Unit)=run(tr(R.string.cb_097),done,tr(R.string.cb_098)) {ledger.addDebt(name,com.todoshift.cashbook.core.WholeMoney.parse(principal,false,null),start,icon,spec)}
    fun configureDebt(id:String,principal:String,start:String,spec:com.todoshift.cashbook.core.Debt,done:()->Unit)=run(tr(R.string.cb_099),done,tr(R.string.cb_100)) {ledger.configureDebt(id,com.todoshift.cashbook.core.WholeMoney.parse(principal,false,ledger.snapshot().debts.first{it.accountId==id}.principal(ledger.snapshot())),start,spec)}
    fun settle(id:String,money:String,amount:String,day:String,time:String,note:String,edit:String?,index:Int=-1,early:Boolean=false,legacyFull:Boolean=false,done:()->Unit)=run(tr(R.string.cb_248),done,tr(R.string.cb_102)) {
        val snapshot=ledger.snapshot();val original=edit?.let{key->snapshot.entries.first{it.id==key}.minor}?:if(legacyFull)snapshot.debts.first{it.accountId==id}.let{debt->debt.schedule(snapshot).find{it.index==index}?.remaining()?:debt.remaining(snapshot)}else null
        ledger.settle(id,money,com.todoshift.cashbook.core.WholeMoney.parse(amount,false,original),day,time,note,edit,index,early)
    }
    fun distribute(id:String)=run(tr(R.string.cb_249)) {ledger.distributeCredit(id)}
    fun postpone(id:String,index:Int,day:String,done:()->Unit)=run(tr(R.string.cb_103),done,tr(R.string.cb_104)) {ledger.postpone(id,index,day)}
    fun post(kind: Ledger.Kind, source: String, target: String?, amount: String,
             note: String, date: String, time: String, items: List<Ledger.Item>, bills: List<Ledger.Bill>, editId: String? = null, done: () -> Unit) = run(tr(R.string.cb_105), done) {
        val minor = com.todoshift.cashbook.core.WholeMoney.parse(amount,false,editId?.let{key->ledger.snapshot().entries.first{it.id==key}.minor})
        if (editId == null) ledger.post(kind, source, target, minor, note, date, time, items, bills)
        else ledger.edit(editId, kind, source, target, minor, note, date, time, items, bills)
    }
    fun deleted(id: String, value: Boolean) = run(if (value) tr(R.string.cb_106) else tr(R.string.cb_107)) {
        ledger.setDeleted(id, value);if(value)undo.value=Undo(id,false)
    }
    fun backup(uri: Uri) = run(tr(R.string.cb_108)) {
        val bytes = ledger.backup()
        val resolver = getApplication<Application>().contentResolver
        requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes); it.flush() }
    }
    fun restore(uri: Uri, done: () -> Unit) = run(tr(R.string.cb_109), done) {
        val resolver = getApplication<Application>().contentResolver
        val bytes = requireNotNull(resolver.openInputStream(uri)).use { it.readBytesLimited(16 * 1024 * 1024 + 32) }
        ledger.restore(bytes)
    }
}
