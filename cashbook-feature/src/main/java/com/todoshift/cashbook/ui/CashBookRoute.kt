package com.todoshift.cashbook.ui

import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoshift.cashbook.core.Ledger
import com.todoshift.cashbook.core.LedgerQuery
import com.todoshift.cashbook.core.MoneyCalculator
import java.time.*
import com.todoshift.cashbook.R
import androidx.compose.ui.unit.sp

private val Incoming = Color(0xFF4CAF50)
private val Outgoing = Color(0xFFF45151)

/** A module route, with the screenshot's ledger layout; no separate Activity or launcher. */
@androidx.compose.runtime.NonSkippableComposable
@Composable
fun CashBookRoute(vm: CashBookViewModel, modifier: Modifier = Modifier, weekStart: DayOfWeek = DayOfWeek.SATURDAY, hostLocale: java.util.Locale? = null) {
    val language by CashBookText.language.collectAsStateWithLifecycle()
    CashBookText.hostLocale(hostLocale ?: LocalContext.current.resources.configuration.locales[0])
    CashBookCompactTheme { CompositionLocalProvider(LocalCashBookVM provides vm, LocalLayoutDirection provides LayoutDirection.Ltr) { Box(modifier.fillMaxSize()){
        CashBookContent(vm,Modifier.fillMaxSize(),weekStart,language)
        val undo by vm.undo.collectAsStateWithLifecycle()
        val operation by vm.state.collectAsStateWithLifecycle()
        LaunchedEffect(undo){if(undo!=null){kotlinx.coroutines.delay(8000);vm.clearUndo()}}
        if(undo!=null)Snackbar(Modifier.align(Alignment.BottomCenter).padding(8.dp),action={TextButton({vm.undoDelete()},enabled=!operation.busy,modifier=Modifier.testTag("undo_delete")){Text(tr(R.string.cb_245))}}){Text(tr(R.string.cb_246))}
    } } }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun CashBookContent(vm: CashBookViewModel, modifier: Modifier, weekStart: DayOfWeek,language:String) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.message) { if (state.message != null) { kotlinx.coroutines.delay(2800); vm.clearMessage() } }
    val snapshot = state.snapshot
    val allAccounts = snapshot?.accounts.orEmpty()
    val accounts = allAccounts.filter { !it.archived }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val active = selected?.takeIf { id -> accounts.any { it.id == id } } ?: accounts.firstOrNull()?.id
    val activeAccount = accounts.find { it.id == active }
    val activeDebt=snapshot?.debts?.find{it.accountId==active}
    val navigation by vm.navigation.collectAsStateWithLifecycle()
    val inLabel = activeAccount?.let{displayLabels(it.incomingLabel,it.outgoingLabel).first} ?: tr(R.string.cb_001)
    val outLabel = activeAccount?.let{displayLabels(it.incomingLabel,it.outgoingLabel).second} ?: tr(R.string.cb_002)
    val defaults = snapshot?.defaults ?: Ledger.ButtonLabels(tr(R.string.cb_001), tr(R.string.cb_002))
    var page by rememberSaveable { mutableStateOf("movement") }
    var mainSection by rememberSaveable { mutableStateOf("accounts") }
    var debtDetail by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(navigation){navigation?.let{selected=it;page="movement";mainSection="debts";debtDetail=true;vm.navigationHandled()}}
    var dataMenu by remember { mutableStateOf(false) }
    var archivedMenu by remember { mutableStateOf(false) }
    var accountMenu by remember { mutableStateOf(false) }
    var overflow by remember { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf("ALL") }
    var anchor by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var from by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var to by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var custom by remember { mutableStateOf(false) }
    val type = ""
    val descending = true
    var debtCreate by rememberSaveable{mutableStateOf(false)}
    var paymentEdit by rememberSaveable{mutableStateOf<String?>(null)}
    var addAccount by rememberSaveable { mutableStateOf(false) }
    var editor by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteEntry by remember{mutableStateOf<String?>(null)}
    var actionId by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var sectionSettings by rememberSaveable{mutableStateOf(false)}
    var defaultSettings by rememberSaveable { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { it?.let(vm::backup) }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingRestore = it }
    val filter = if (period == "CUSTOM") LedgerQuery.Filter(LocalDate.parse(from), LocalDate.parse(to), search, type.takeIf { it.isNotEmpty() }?.let(Ledger.Kind::valueOf), page == "trash", descending)
        else LedgerQuery.Filter.period(LedgerQuery.Period.valueOf(period), LocalDate.parse(anchor), weekStart, search, type.takeIf { it.isNotEmpty() }?.let(Ledger.Kind::valueOf), page == "trash", descending)
    val view = if (snapshot != null && active != null) LedgerQuery.account(snapshot, active, filter) else null
    BackHandler(editor == null && (page != "movement" || showSearch)) { if (showSearch) { showSearch = false; search = "" } else page = "movement" }
    fun open(kind: String) { editId = null; editor = kind; vm.clearMessage() }
    if (editor != null && accounts.isNotEmpty()) {
        EntryScreen(accounts.filter{a->snapshot?.debts?.none{it.accountId==a.id}!=false}, active, editor!!, snapshot?.entries?.find { it.id == editId }, state.busy, state.error,
            { editor = null; editId = null; vm.clearMessage() }) { kind, source, target, amount, note, day, time, items, bills, done ->
            vm.post(kind, source, target, amount, note, day, time, items, bills, editId, done)
        }
        return
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
            // Cash Book partitions. Work entitlements deliberately have no local salary data.
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 6.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(
                    Triple("accounts", R.string.cb_section_accounts, "section_accounts"),
                    Triple("debts", R.string.cb_section_debts, "section_debts"),
                    Triple("salary", R.string.cb_section_salary, "section_salary")
                ).forEach { (key, label, tag) ->
                    val chosen = mainSection == key
                    Button(
                        onClick = { mainSection = key; debtDetail = false; page = "movement" },
                        modifier = Modifier.weight(1f).height(42.dp).testTag(tag),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (chosen) Color.White else Color.White.copy(alpha = 0.13f),
                            contentColor = if (chosen) MaterialTheme.colorScheme.primary else Color.White
                        )
                    ) { Text(tr(label), fontSize = 11.sp, maxLines = 1) }
                }
            }
            if (mainSection == "salary") {
                WorkEntitlementsEmpty(Modifier.weight(1f))
            } else if (mainSection == "debts" && !debtDetail) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(12.dp)) {
                    Text(tr(R.string.cb_section_debts), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    val debtAccounts = allAccounts.filter { account -> snapshot?.debts?.any { it.accountId == account.id } == true }
                    if (debtAccounts.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(tr(R.string.cb_debts_empty), textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(debtAccounts, key = { it.id }) { account ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable { selected = account.id; debtDetail = true; page = "movement" },
                                    shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp
                                ) {
                                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AccountName(account, Modifier.weight(1f))
                                        Text("›", style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
            // Section navigation: account selector, summary, search and management options.
            Row(Modifier.fillMaxWidth().heightIn(min = CashBookCompact.HeaderHeight).background(MaterialTheme.colorScheme.primary), verticalAlignment = Alignment.CenterVertically) {
                if(page != "movement" || (mainSection == "debts" && debtDetail)) IconButton(onClick={if (mainSection == "debts") debtDetail = false else page="movement"}){RefIcon(R.drawable.cb_back,tr(R.string.cb_003),tint=Color.White)}
                Box(Modifier.weight(1f)) {
                    TextButton(onClick = { archivedMenu=false;accountMenu = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.testTag("account_picker_open")) {
                        if(page=="movement"&&activeAccount!=null){AccountName(activeAccount,color=Color.White);Text(" ▾",color=Color.White)}else Text(if(page=="summary") tr(R.string.cb_004) else if(page=="trash") tr(R.string.cb_005) else tr(R.string.cb_006),color=Color.White)
                    }
                }
                IconButton(onClick = { page = if(page=="summary") "movement" else "summary" }, modifier = Modifier.testTag("summary_open").semantics { contentDescription = tr(R.string.cb_004) }) { RefIcon(R.drawable.cb_report, tr(R.string.cb_007), tint = Color.White) }
                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) search = "" }, modifier = Modifier.semantics { contentDescription = tr(R.string.cb_008) }) { RefIcon(R.drawable.cb_search, tr(R.string.cb_008), tint = Color.White) }
                Box {
                    IconButton(onClick = { overflow = true }, modifier = Modifier.testTag("section_options").semantics { contentDescription = tr(R.string.cb_009) }) { RefIcon(R.drawable.cb_more, tr(R.string.cb_010), tint = Color.White) }
                    DropdownMenu(overflow, { overflow = false }) {
                        DropdownMenuItem(text={Text(tr(R.string.cb_011))},enabled=!state.busy&&accounts.count{a->snapshot?.debts?.none{it.accountId==a.id}!=false}>1,onClick={overflow=false;open("TRANSFER")})
                        DropdownMenuItem(text={Text(tr(R.string.cb_012))},enabled=!state.busy&&snapshot!=null,onClick={overflow=false;sectionSettings=true;vm.clearMessage()},modifier=Modifier.testTag("default_settings_open"))
                        DropdownMenuItem(text={Text(tr(R.string.cb_013))},enabled=snapshot!=null,onClick={overflow=false;dataMenu=true},modifier=Modifier.testTag("data_management_open"))
                    }
                }
            }
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, Modifier.padding(8.dp), color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodySmall) }
            if (snapshot == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { if (!state.busy) Button(onClick = vm::reload) { Text(tr(R.string.cb_014)) } }
            } else if (accounts.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Button(onClick = { addAccount = true }) { Text(tr(R.string.cb_015)) } }
            } else if(activeDebt!=null&&page=="movement"){
                DebtPanel(snapshot,activeDebt,Modifier.weight(1f))
            } else {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL" to tr(R.string.cb_016), "DAILY" to tr(R.string.cb_017), "WEEKLY" to tr(R.string.cb_018), "MONTHLY" to tr(R.string.cb_019), "YEARLY" to tr(R.string.cb_020)).forEach { (key, label) ->
                        ReferencePill(label, period == key, Modifier.weight(1f)) { period = key }
                    }
                }
                Box(Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.primary).border(1.dp, Color.White.copy(alpha = 0.7f)).clickable { custom = true }.testTag("period_caption"), contentAlignment = Alignment.Center) {
                    Text(if (period == "ALL") tr(R.string.cb_016) else "${filter.from} — ${filter.to}", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
                if (showSearch) OutlinedTextField(search, { search = it }, label = { Text(tr(R.string.cb_008)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(8.dp))
                if (page != "summary") Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(tr(R.string.cb_021), Modifier.weight(1f)); Text(inLabel, Modifier.weight(1f), color = Incoming, fontSize = 14.sp, textAlign = TextAlign.Center); Text(outLabel, Modifier.weight(1f), color = Outgoing, fontSize = 14.sp, textAlign = TextAlign.End)
                }
                if (page == "summary") {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(accounts, key = { it.id }) { a ->
                            val v = LedgerQuery.account(snapshot, a.id, filter)
                            Surface(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = CashBookCompact.SummaryPadding)) {
                                AccountName(a, Modifier.fillMaxWidth().padding(bottom = CashBookCompact.SummaryTitleGap))
                                Totals(referenceMoney(v.incomeMinor), referenceMoney(v.expenseMinor), referenceMoney(v.currentBalanceMinor), displayLabels(a.incomingLabel,a.outgoingLabel).first, displayLabels(a.incomingLabel,a.outgoingLabel).second)
                            } }
                        }
                    }
                } else {
                    val rows = view?.rows.orEmpty()
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        if (rows.isEmpty()) item { Text(tr(R.string.cb_022), Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center) }
                        items(rows.indices.toList(), key = { rows[it].entry.id }) { index ->
                            val row = rows[index]; val e = row.entry
                            if (index == 0 || rows[index-1].entry.date != e.date) Text(referenceDay(e.date), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 6.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
                            Surface(Modifier.fillMaxWidth().padding(bottom = 3.dp).clickable { actionId = e.id }) {
                                Column(Modifier.padding(horizontal = 8.dp, vertical = CashBookCompact.RowVerticalPadding)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(e.note.ifBlank { if (e.kind == Ledger.Kind.TRANSFER) tr(R.string.cb_011) else tr(R.string.cb_023) }, Modifier.weight(if (e.kind == Ledger.Kind.EXPENSE) 2f else 1f), fontSize = 15.sp, textAlign = TextAlign.Center)
                                        if (e.kind != Ledger.Kind.EXPENSE) Text(if (e.kind == Ledger.Kind.INCOME || e.kind == Ledger.Kind.SETTLEMENT) referenceMoney(e.minor) else "⇄", Modifier.weight(1f), color = Incoming, fontSize = 14.sp, textAlign = TextAlign.Center)
                                        Text(if (e.kind == Ledger.Kind.EXPENSE || e.kind == Ledger.Kind.TRANSFER) referenceMoney(e.minor) else "", Modifier.weight(1f), color = Outgoing, fontSize = 14.sp, textAlign = TextAlign.End)
                                    }
                                    Spacer(Modifier.height(CashBookCompact.RowMetaGap))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(referenceTime(e.time), color = Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
                                        Text(row.runningMinor?.let { tr(R.string.cb_024 ,referenceMoney(it)) } ?: tr(R.string.cb_025), color = Color(0xFF666666), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    if (page == "movement") Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(shape = ReferenceShape, contentPadding = PaddingValues(6.dp), onClick = { open("INCOME") }, enabled = !state.busy, colors = ButtonDefaults.buttonColors(containerColor = Incoming), modifier = Modifier.weight(1f).testTag("quick_income")) { Text(inLabel) }
                        Button(shape = ReferenceShape, contentPadding = PaddingValues(6.dp), onClick = { open("EXPENSE") }, enabled = !state.busy, colors = ButtonDefaults.buttonColors(containerColor = Outgoing), modifier = Modifier.weight(1f).testTag("quick_expense")) { Text(outLabel) }
                    }
                }
                val views = if (page == "summary") accounts.map { LedgerQuery.account(snapshot, it.id, filter) } else listOfNotNull(view)
                fun sum(selector: (LedgerQuery.View) -> Long): String = java.math.BigDecimal(views.fold(java.math.BigInteger.ZERO) { n, v -> n.add(java.math.BigInteger.valueOf(selector(v))) }, 2).toPlainString().let(::referenceDecimal)
                Totals(sum { it.incomeMinor }, sum { it.expenseMinor }, sum { it.currentBalanceMinor }, if (page == "summary") tr(R.string.cb_001) else inLabel, if (page == "summary") tr(R.string.cb_002) else outLabel)
            }
            } // Existing accounts/debt-detail screen
        }
    }
    if (accountMenu) AccountsDialog(allAccounts, active, archivedMenu, defaults, state.busy, state.error, { accountMenu = false },
        { selected = it; page = "movement"; accountMenu = false },
        { accountMenu = false; addAccount = true },
        { id, name, plus, minus, icon, done -> vm.customizeAccount(id, name, plus, minus, icon, done) }, { ids -> selected = active; vm.reorderAccounts(ids) })
    if(dataMenu) AlertDialog(onDismissRequest={dataMenu=false},title={Text(tr(R.string.cb_013))},text={Column {
        TextButton(onClick={dataMenu=false;page="trash"}){Text(tr(R.string.cb_005))}
        TextButton(onClick={dataMenu=false;archivedMenu=true;accountMenu=true}){Text(tr(R.string.cb_026))}
        TextButton(onClick={export.launch("cashbook-${LocalDate.now()}.cbk");dataMenu=false},enabled=!state.busy){Text(tr(R.string.cb_027))}
        TextButton(onClick={restore.launch(arrayOf("*/*"));dataMenu=false},enabled=!state.busy){Text(tr(R.string.cb_028))}
    }},confirmButton={TextButton(onClick={dataMenu=false}){Text(tr(R.string.cb_029))}})
    if (custom) AlertDialog(onDismissRequest = { custom = false }, title = { Text(tr(R.string.cb_030)) }, text = {
        Column { DayPicker(tr(R.string.cb_031), from, !state.busy) { from = it; if (to < it) to = it }; DayPicker(tr(R.string.cb_032), to, !state.busy) { to = it; if (from > it) from = it } }
    }, confirmButton = { TextButton(onClick = { period = "CUSTOM"; custom = false }) { Text(tr(R.string.cb_033)) } }, dismissButton = { TextButton(onClick = { custom = false }) { Text(tr(R.string.cb_034)) } })
    if(sectionSettings)SectionSettings({sectionSettings=false}){sectionSettings=false;defaultSettings=true}
    if (defaultSettings) DefaultButtonsDialog(defaults, state.busy, state.error, { defaultSettings = false; vm.clearMessage() }) { plus, minus -> vm.defaultLabels(plus, minus) { defaultSettings = false } }
    if (addAccount && !debtCreate) CompactAccountDialog(null,defaults,state.busy,state.error,{addAccount=false;vm.clearMessage()},{name,opening,day,created,plus,minus,icon->vm.newAccount(name,opening,day,created,plus,minus,icon){addAccount=false}},createDebt={debtCreate=true})
    if(addAccount&&debtCreate)DebtSettingsDialog(null){addAccount=false;debtCreate=false;vm.clearMessage()}
    paymentEdit?.let{id->snapshot?.entries?.find{it.id==id}?.let{entry->snapshot.debts.find{it.accountId==entry.accountId||it.accountId==entry.targetId}?.let{debt->SettlementDialog(snapshot,debt,entry,{paymentEdit=null})}}}
    actionId?.let { id -> snapshot?.entries?.find { it.id == id }?.let { e -> AlertDialog(onDismissRequest = { actionId = null }, title = { Text(e.note.ifBlank { tr(R.string.cb_035) }) }, text = {
        Column {
            if (!e.deleted) TextButton(onClick = { if(snapshot.debts.any{it.accountId==e.accountId||it.accountId==e.targetId})paymentEdit=e.id else{editId = e.id; editor = e.kind.name}; actionId = null; vm.clearMessage() }) { Text(tr(R.string.cb_036)) }
            TextButton(enabled = !state.busy, onClick = { if(e.deleted)vm.deleted(e.id,false)else deleteEntry=e.id; actionId = null }) { Text(if (e.deleted) tr(R.string.cb_037) else tr(R.string.cb_038)) }
        }
    }, confirmButton = { TextButton(onClick = { actionId = null }) { Text(tr(R.string.cb_029)) } }) } }
    deleteEntry?.let{id->AlertDialog(onDismissRequest={deleteEntry=null},title={Text(tr(R.string.cb_247))},confirmButton={TextButton({vm.deleted(id,true);deleteEntry=null}){Text(tr(R.string.cb_140))}},dismissButton={TextButton({deleteEntry=null}){Text(tr(R.string.cb_034))}})}
    pendingRestore?.let { uri -> AlertDialog(onDismissRequest = { pendingRestore = null }, title = { Text(tr(R.string.cb_039)) }, text = { Text(tr(R.string.cb_040)) },
        confirmButton = { TextButton(onClick = { vm.restore(uri) { pendingRestore = null } }, enabled = !state.busy) { Text(tr(R.string.cb_041)) } }, dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text(tr(R.string.cb_034)) } }) }
}

private fun shiftPeriod(anchor: String, period: String, step: Long): String = LocalDate.parse(anchor).let {
    when(period) { "WEEKLY" -> it.plusWeeks(step); "MONTHLY" -> it.plusMonths(step); "YEARLY" -> it.plusYears(step); else -> it.plusDays(step) }
}.toString()

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun Totals(income: String, expense: String, balance: String, incomingLabel: String = tr(R.string.cb_001), outgoingLabel: String = tr(R.string.cb_002)) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp).border(1.dp, ReferenceBorder).background(MaterialTheme.colorScheme.surface).heightIn(min = 54.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(Triple(tr(R.string.cb_042 ,incomingLabel), income, Incoming), Triple(tr(R.string.cb_042 ,outgoingLabel), expense, Outgoing), Triple(tr(R.string.cb_043), balance, MaterialTheme.colorScheme.onSurface)).forEachIndexed { index, (label, value, color) ->
            if (index > 0) VerticalDivider(Modifier.height(40.dp))
            Column(Modifier.weight(1f).padding(horizontal = 3.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = if (index == 2) MaterialTheme.colorScheme.onSurface else color, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(value, modifier = Modifier.testTag(if (index == 2) "total_balance" else if (index == 0) "total_income" else "total_expense"), color = if (index == 2 && balance.startsWith("-")) Outgoing else color, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun DayPicker(label: String, value: String, enabled: Boolean = true, choose: (String) -> Unit) {
    val context = CashBookText.dialogContext(LocalContext.current)
    OutlinedButton(modifier=Modifier.testTag("day_picker"), enabled = enabled, onClick = { val d = value.takeIf { it.isNotEmpty() }?.let(LocalDate::parse) ?: LocalDate.now(); android.app.DatePickerDialog(context, { _, y, m, day -> choose(LocalDate.of(y, m + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth).apply{setOnShowListener{window?.decorView?.layoutDirection=android.view.View.LAYOUT_DIRECTION_LTR};show()} }) { Text(if(label.isEmpty())referenceDayShort(value)else "$label ${referenceDayShort(value)}") }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun AccountSelect(accounts: List<Ledger.Account>, selected: String, enabled: Boolean, choose: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { OutlinedButton(onClick = { expanded = true }, enabled = enabled) { accounts.find { it.id == selected }?.let{AccountName(it)} ?: Text(tr(R.string.cb_044)) }
        DropdownMenu(expanded, { expanded = false }) { accounts.forEach { a -> DropdownMenuItem(text = { AccountName(a) }, onClick = { choose(a.id); expanded = false }) } }
    }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun DiscardDialog(cancel: () -> Unit, discard: () -> Unit) {
    AlertDialog(onDismissRequest = cancel, title = { Text(tr(R.string.cb_045)) }, confirmButton = { TextButton(onClick = discard) { Text(tr(R.string.cb_046)) } }, dismissButton = { TextButton(onClick = cancel) { Text(tr(R.string.cb_047)) } })
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun EntryScreen(accounts: List<Ledger.Account>, selected: String?, initialKind: String, editing: Ledger.Entry?, busy: Boolean, error: String?, dismiss: () -> Unit,
    save: (Ledger.Kind, String, String?, String, String, String, String, List<Ledger.Item>, List<Ledger.Bill>, () -> Unit) -> Unit) {
    var kindName by rememberSaveable(editing?.id) { mutableStateOf(editing?.kind?.name ?: initialKind) }
    val kind = Ledger.Kind.valueOf(kindName)
    var source by rememberSaveable(editing?.id) { mutableStateOf(editing?.accountId ?: selected ?: accounts.first().id) }
    var target by rememberSaveable(editing?.id) { mutableStateOf(editing?.targetId ?: accounts.firstOrNull { it.id != source }?.id.orEmpty()) }
    var amount by rememberSaveable(editing?.id) { mutableStateOf(editing?.let { com.todoshift.cashbook.core.WholeMoney.edit(it.minor) }.orEmpty()) }
    var note by rememberSaveable(editing?.id) { mutableStateOf(editing?.note.orEmpty()) }
    var day by rememberSaveable(editing?.id) { mutableStateOf(editing?.date ?: LocalDate.now().toString()) }
    var time by rememberSaveable(editing?.id) { mutableStateOf(editing?.time ?: LocalTime.now().withSecond(0).withNano(0).toString()) }
    val originalExtras = remember(editing?.id) { encodeExtras(editing?.items.orEmpty(), editing?.bills.orEmpty()) }
    var extras by rememberSaveable(editing?.id) { mutableStateOf(originalExtras) }
    val lineItems = draftItems(extras); val bills = draftBills(extras)
    var showAccounts by remember { mutableStateOf(false) }; var showItems by remember { mutableStateOf(false) }; var showBills by remember { mutableStateOf(false) }
    var calculator by remember { mutableStateOf(false) }; var discard by remember { mutableStateOf(false) }; var localError by remember { mutableStateOf<String?>(null) }
    var focusEpoch by remember { mutableStateOf(0) }; val focus = remember { FocusRequester() }
    val context = LocalContext.current
    val dirty = if (editing == null) amount.isNotBlank() || note.isNotBlank() || extras != originalExtras else
        amount != com.todoshift.cashbook.core.WholeMoney.edit(editing.minor) || note != editing.note || day != editing.date || time != editing.time || kind != editing.kind || source != editing.accountId || target != editing.targetId || extras != originalExtras
    val close = { if (!busy) { if (dirty) discard = true else dismiss() } }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { note = (note + " " + it).trim() }
    }
    val billPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) try {
            require(bills.size < 10)
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: tr(R.string.cb_048)
            extras = encodeExtras(lineItems, bills + Ledger.Bill(name.take(200), uri.toString()))
        } catch (e: Exception) { localError = tr(R.string.cb_049) }
    }
    val submit: (Boolean) -> Unit = { keep ->
        save(kind, source, if (kind == Ledger.Kind.TRANSFER) target else null, amount, note, day, time, lineItems, bills) {
            if (keep) { amount = ""; note = ""; extras = encodeExtras(emptyList(), emptyList()); localError = null; focusEpoch++ } else dismiss()
        }
    }
    val sourceAccount = accounts.find { it.id == source }
    val incomingLabel = sourceAccount?.let{displayLabels(it.incomingLabel,it.outgoingLabel).first} ?: tr(R.string.cb_001)
    val outgoingLabel = sourceAccount?.let{displayLabels(it.incomingLabel,it.outgoingLabel).second} ?: tr(R.string.cb_002)
    val label = when(kind) { Ledger.Kind.INCOME -> incomingLabel; Ledger.Kind.EXPENSE -> outgoingLabel; else -> tr(R.string.cb_050) }
    BackHandler { close() }
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    run {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxSize().testTag("editor_viewport")) {
                    Row(Modifier.fillMaxWidth().heightIn(min = CashBookCompact.HeaderHeight).background(MaterialTheme.colorScheme.primary), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = close, enabled = !busy, modifier = Modifier.semantics { contentDescription = tr(R.string.cb_003) }) { RefIcon(R.drawable.cb_back, tr(R.string.cb_003), tint = Color.White) }
                        Text((if(editing != null) tr(R.string.cb_051) else "") + label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { showAccounts = !showAccounts }, enabled = !busy, modifier = Modifier.semantics { contentDescription = tr(R.string.cb_052) }) { RefIcon(R.drawable.cb_more, tr(R.string.cb_010), tint = Color.White) }
                    }
                    Column(Modifier.weight(1f, fill = imeVisible).verticalScroll(rememberScrollState()).padding(CashBookCompact.ContentPadding)) {
                        Surface(shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
                            Column(Modifier.fillMaxWidth().padding(CashBookCompact.ContentPadding), verticalArrangement = Arrangement.spacedBy(CashBookCompact.EditorGap)) {
                                if (kind != Ledger.Kind.TRANSFER) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ReferencePill(incomingLabel, kind == Ledger.Kind.INCOME, Modifier.widthIn(min = 72.dp, max = 135.dp), !busy, active = Incoming) { kindName = "INCOME" }
                                    ReferencePill(outgoingLabel, kind == Ledger.Kind.EXPENSE, Modifier.widthIn(min = 72.dp, max = 135.dp), !busy, active = Outgoing) { kindName = "EXPENSE" }
                                }
                                if (showAccounts || kind == Ledger.Kind.TRANSFER) {
                                    AccountSelect(accounts, source, !busy) { source = it; if (target == it) target = accounts.firstOrNull { a -> a.id != it }?.id.orEmpty() }
                                    if (kind == Ledger.Kind.TRANSFER) AccountSelect(accounts.filter { it.id != source }, target, !busy) { target = it }
                                }
                                ReferenceDateTime(day, time, !busy, { day = it }, { time = it })
                                LaunchedEffect(focusEpoch) { focus.requestFocus(); keyboard?.show() }
                                ReferenceField(amount, { amount = it }, modifier = Modifier.focusRequester(focus).testTag("entry_amount"), label = label, enabled = !busy,
                                    readOnly = lineItems.isNotEmpty(), numeric = true) {
                                    IconButton(onClick = { calculator = true }, enabled = !busy && lineItems.isEmpty()) { RefIcon(R.drawable.cb_calculator, tr(R.string.cb_053)) }
                                }
                                ReferenceField(note, { note = it }, modifier = Modifier.testTag("entry_note"), placeholder = tr(R.string.cb_054), enabled = !busy) {
                                    IconButton(enabled = !busy, onClick = {
                                        try { speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)) }
                                        catch (e: Exception) { localError = tr(R.string.cb_055) }
                                    }) { RefIcon(R.drawable.cb_mic, tr(R.string.cb_056)) }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(shape = ReferenceShape, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), onClick = { if (bills.isEmpty()) billPicker.launch(arrayOf("image/*", "application/pdf")) else showBills = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                        Text(tr(R.string.cb_057) + if (bills.isEmpty()) "" else " (${bills.size})", fontSize = 12.sp); Spacer(Modifier.width(6.dp)); RefIcon(R.drawable.cb_camera, null)
                                    }
                                    OutlinedButton(shape = ReferenceShape, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp), onClick = { showItems = true }, enabled = !busy, modifier = Modifier.weight(1f)) {
                                        Text(tr(R.string.cb_058) + if (lineItems.isEmpty()) "" else " (${lineItems.size})", fontSize = 12.sp); Spacer(Modifier.width(6.dp)); RefIcon(R.drawable.cb_list, null)
                                    }
                                }
                                (localError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(6.dp).testTag("editor_save_bar"), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val valid = !busy && amount.isNotBlank() && (kind != Ledger.Kind.TRANSFER || target.isNotEmpty())
                        OutlinedButton(shape = ReferenceShape, colors = ButtonDefaults.outlinedButtonColors(disabledContainerColor = Color(0xFFBBDEFB), disabledContentColor = ReferenceBlue), contentPadding = PaddingValues(6.dp), onClick = { submit(false) }, enabled = valid, modifier = Modifier.weight(1f).testTag("save_exit")) { Text(tr(R.string.cb_059)) }
                        if (editing == null) Button(shape = ReferenceShape, colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFBBDEFB), disabledContentColor = ReferenceBlue), contentPadding = PaddingValues(6.dp), onClick = { submit(true) }, enabled = valid, modifier = Modifier.weight(1f)) { Text(tr(R.string.cb_060)) }
                    }
                }
            }
        }
    }
    if (discard) DiscardDialog({ discard = false }, dismiss)
    if (calculator) CalculatorDialog({ calculator = false }) { amount = it; calculator = false; localError = null }
    if (showItems) ItemsDialog(lineItems, { showItems = false }) { list ->
        try { val total = itemTotal(list); if (list.isNotEmpty() && total <= 0) throw IllegalArgumentException(); extras = encodeExtras(list, bills); if (list.isNotEmpty()) amount = com.todoshift.cashbook.core.WholeMoney.edit(total); showItems = false }
        catch (e: Exception) { localError = tr(R.string.cb_061); showItems = false }
    }
    if (showBills) AlertDialog(onDismissRequest = { showBills = false }, title = { Text(tr(R.string.cb_062)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            bills.forEach { bill -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(bill.uri), context.contentResolver.getType(Uri.parse(bill.uri))).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } catch (e: Exception) { localError = tr(R.string.cb_063) } }, modifier = Modifier.weight(1f)) { Text(bill.name) }
                TextButton(onClick = { extras = encodeExtras(lineItems, bills.filterNot { it === bill }) }) { Text(tr(R.string.cb_064)) }
            } }
            Text(tr(R.string.cb_065), style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { billPicker.launch(arrayOf("image/*", "application/pdf")) }, enabled = bills.size < 10) { Text(tr(R.string.cb_066)) } }, dismissButton = { TextButton(onClick = { showBills = false }) { Text(tr(R.string.cb_067)) } })
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun CalculatorDialog(dismiss: () -> Unit, apply: (String) -> Unit) {
    var expression by rememberSaveable { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(tr(R.string.cb_053)) }, text = {
        Column { OutlinedTextField(expression, { expression = it }, label = { Text(tr(R.string.cb_068)) }); Text(tr(R.string.cb_069), style = MaterialTheme.typography.bodySmall); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
    }, confirmButton = { TextButton(onClick = { try { val n = MoneyCalculator.calculate(expression); require(n > 0 && n%100L==0L); apply(com.todoshift.cashbook.core.WholeMoney.edit(n)) } catch (e: Exception) { error = tr(R.string.cb_070) } }) { Text(tr(R.string.cb_071)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(tr(R.string.cb_034)) } })
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun ItemsDialog(initial: List<Ledger.Item>, dismiss: () -> Unit, apply: (List<Ledger.Item>) -> Unit) {
    var json by rememberSaveable { mutableStateOf(encodeExtras(initial, emptyList())) }; val list = draftItems(json)
    var name by rememberSaveable { mutableStateOf("") }; var quantity by rememberSaveable { mutableStateOf("1") }; var unit by rememberSaveable { mutableStateOf("") }; var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(tr(R.string.cb_072)) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            list.forEachIndexed { index, item -> Row(verticalAlignment = Alignment.CenterVertically) { Text("${item.name} × ${item.quantity} • ${referenceMoney(item.totalMinor())}", Modifier.weight(1f)); TextButton(onClick = { json = encodeExtras(list.filterIndexed { i, _ -> i != index }, emptyList()) }) { Text(tr(R.string.cb_064)) } } }
            OutlinedTextField(name, { name = it }, label = { Text(tr(R.string.cb_073)) }); OutlinedTextField(quantity, { quantity = it }, label = { Text(tr(R.string.cb_074)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)); MoneyField(unit, { unit = it }, label = { Text(tr(R.string.cb_075)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            TextButton(onClick = { try {
                val q = quantity.toInt(); val value = com.todoshift.cashbook.core.WholeMoney.parse(unit,false,null); require(name.isNotBlank() && name.length <= 100 && q in 1..1000000 && value >= 0 && list.size < 100)
                val next = list + Ledger.Item(name.trim(), q, value); itemTotal(next); json = encodeExtras(next, emptyList()); name = ""; quantity = "1"; unit = ""; error = null
            } catch (e: Exception) { error = tr(R.string.cb_076) } }) { Text(tr(R.string.cb_077)) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = { apply(list) }) { Text(tr(R.string.cb_078)) } }, dismissButton = { TextButton(onClick = dismiss) { Text(tr(R.string.cb_034)) } })
}

/** Empty until the unified ToDo Shift shift/payments provider is connected. */
@Composable
private fun WorkEntitlementsEmpty(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tr(R.string.cb_section_salary), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Text(tr(R.string.cb_salary_unlinked), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
