package com.todoshift.cashbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import com.todoshift.cashbook.R
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.todoshift.cashbook.core.Ledger

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun AccountsDialog(accounts: List<Ledger.Account>, active: String?, startArchived: Boolean, defaults: Ledger.ButtonLabels, busy: Boolean, error: String?, dismiss: () -> Unit, select: (String) -> Unit, add: () -> Unit, rename: (String, String, String, String, String, () -> Unit) -> Unit, reorder: (List<String>) -> Unit) {
    val vm=LocalCashBookVM.current
    val state by vm.state.collectAsState()
    var archived by rememberSaveable { mutableStateOf(startArchived) }
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var edit by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
    Dialog(dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val focus = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            Surface(Modifier.fillMaxWidth(0.86f).widthIn(max = 420.dp).imePadding().heightIn(max = maxHeight).testTag("accounts_dialog"), shape = ReferenceShape) {
                Column(Modifier.padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr(R.string.cb_006), fontSize = 17.sp, style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { focus.clearFocus(); keyboard?.hide(); edit = !edit }, shape = ReferenceShape, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp), enabled = !busy, modifier = Modifier.testTag("accounts_edit")) { Text(if (edit) tr(R.string.cb_067) else tr(R.string.cb_230)) }
                    }
                    Row(Modifier.fillMaxWidth().height(36.dp).border(1.dp, ReferenceBorder, ReferenceShape).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RefIcon(R.drawable.cb_search, tr(R.string.cb_231))
                        BasicTextField(query, { query = it }, singleLine = true, decorationBox = { inner -> Box { if (query.isEmpty()) Text(tr(R.string.cb_231), color = Color.Gray, fontSize = 13.sp); inner() } }, modifier = Modifier.weight(1f).padding(horizontal = 6.dp).testTag("accounts_search"), textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
                    }
                    Row {
                        FilterChip(!archived,{archived=false},label={Text(tr(R.string.cb_006))})
                        Spacer(Modifier.width(8.dp))
                        FilterChip(archived,{archived=true},label={Text(tr(R.string.cb_232))},modifier=Modifier.testTag("archived_accounts"))
                    }
                    Spacer(Modifier.height(6.dp))
                    if (edit) Text(if (query.isBlank()) tr(R.string.cb_233) else tr(R.string.cb_234), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 5.dp))
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("account_order_saving"))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 4.dp)) }
                    AccountOrderList(accounts.filter { it.archived==archived }, active, busy, error, edit && !archived, query, Modifier.weight(1f).fillMaxWidth(), {id->if(archived)action=id else select(id)},
                        { account -> action = account.id }, {ids->
                            val next=ids.iterator();reorder(accounts.map{if(!it.archived)next.next() else it.id})
                        })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = add, shape = ReferenceShape, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("accounts_add"), contentPadding = PaddingValues(6.dp)) { Text(tr(R.string.cb_155)) }
                }
            }
        }
    }
    action?.let { id -> accounts.find{it.id==id}?.let {account->
        val related=state.snapshot?.entries?.count{it.accountId==id||it.targetId==id}?:0
        AlertDialog(onDismissRequest={action=null},title={AccountName(account)},text={Column {
            Text(tr(R.string.cb_235 ,related))
            TextButton(onClick={action=null;renaming=id},enabled=!busy){Text(tr(R.string.cb_156))}
            TextButton(onClick={vm.archiveAccount(id,!account.archived){action=null}},enabled=!busy,modifier=Modifier.testTag("account_archive")){Text(if(account.archived) tr(R.string.cb_236) else tr(R.string.cb_237))}
            TextButton(onClick={deleting=id;action=null},enabled=!busy,modifier=Modifier.testTag("account_delete")){Text(tr(R.string.cb_238))}
        }},confirmButton={TextButton(onClick={action=null}){Text(tr(R.string.cb_029))}})
    } }
    deleting?.let{id->accounts.find{it.id==id}?.let{account->
        val related=state.snapshot?.entries?.count{it.accountId==id||it.targetId==id}?:0
        AlertDialog(onDismissRequest={deleting=null},title={Text(if(related>0) tr(R.string.cb_239) else tr(R.string.cb_240))},text={Text(if(related>0) tr(R.string.cb_241 ,related) else tr(R.string.cb_242 ,account.name))},confirmButton={if(related>0)TextButton(onClick={vm.archiveAccount(id,true){deleting=null}},enabled=!busy){Text(tr(R.string.cb_243))}else TextButton(onClick={vm.deleteAccount(id){deleting=null}},enabled=!busy,modifier=Modifier.testTag("account_delete_confirm")){Text(tr(R.string.cb_244))}},dismissButton={TextButton(onClick={deleting=null}){Text(tr(R.string.cb_034))}})
    }}
    renaming?.let { id -> accounts.find { it.id == id }?.let { account ->
        AccountDetailsDialog(account, defaults, busy, error, { renaming = null }, { name, plus, minus, icon -> rename(id, name, plus, minus, icon) { renaming = null } })
    } }
}
