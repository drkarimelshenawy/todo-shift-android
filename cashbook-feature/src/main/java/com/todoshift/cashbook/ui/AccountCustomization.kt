package com.todoshift.cashbook.ui
import com.todoshift.cashbook.R

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoshift.cashbook.core.Ledger

internal val ButtonPresets get() = listOf(
    tr(R.string.cb_001) to tr(R.string.cb_002), tr(R.string.cb_115) to tr(R.string.cb_116), tr(R.string.cb_117) to tr(R.string.cb_118), tr(R.string.cb_119) to tr(R.string.cb_120),
    tr(R.string.cb_121) to tr(R.string.cb_122), tr(R.string.cb_123) to tr(R.string.cb_124), tr(R.string.cb_125) to tr(R.string.cb_126), tr(R.string.cb_127) to tr(R.string.cb_128),
    tr(R.string.cb_066) to tr(R.string.cb_129), tr(R.string.cb_130) to tr(R.string.cb_131), tr(R.string.cb_132) to tr(R.string.cb_133), tr(R.string.cb_134) to tr(R.string.cb_135)
)
internal fun Ledger.Account.displayName(): String = if(icon.isBlank()||icon.startsWith("i:")) name else "$icon $name"
internal fun validLabels(incoming: String, outgoing: String) = incoming.isNotBlank() && outgoing.isNotBlank() && incoming.length <= 30 && outgoing.length <= 30

internal val LocalCashBookVM = staticCompositionLocalOf<CashBookViewModel> { error("Cash Book scope missing") }

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun ButtonLabelsEditor(incoming: String, outgoing: String, enabled: Boolean, change: (String, String) -> Unit) {
    val vm=LocalCashBookVM.current
    val state by vm.state.collectAsState()
    val prefs=state.snapshot?.preferences
    val defaults=state.snapshot?.defaults
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var label by rememberSaveable { mutableStateOf("") }
    val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
    Box {
        OutlinedButton(onClick={focus.clearFocus();keyboard?.hide();menu=true}, enabled=enabled, shape=ReferenceShape, modifier=Modifier.fillMaxWidth().testTag("button_presets")) { Text(tr(R.string.cb_136)) }
        DropdownMenu(menu,{menu=false},modifier=Modifier.heightIn(max=310.dp)) {
            DropdownMenuItem(text={Text(tr(R.string.cb_137))},onClick={defaults?.let {change(it.incoming,it.outgoing)};menu=false})
            DropdownMenuItem(text={Text(tr(R.string.cb_138))},enabled=validLabels(incoming,outgoing)&&!state.busy,onClick={vm.savePreset(incoming,outgoing);menu=false},modifier=Modifier.testTag("save_personal_preset"))
            if(!prefs?.presets.isNullOrEmpty()) {
                HorizontalDivider();Text(tr(R.string.cb_139),Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.labelMedium)
                prefs?.presets?.forEachIndexed { index,pair ->
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        TextButton(onClick={change(pair.incoming,pair.outgoing);menu=false},modifier=Modifier.weight(1f).testTag("personal_preset_$index")){Text("${pair.incoming} / ${pair.outgoing}")}
                        TextButton(onClick={vm.removePreset(index)},enabled=!state.busy){Text(tr(R.string.cb_140))}
                    }
                }
            }
            HorizontalDivider()
            ButtonPresets.forEachIndexed { index,(plus,minus) -> DropdownMenuItem(text={Text("$plus / $minus")},modifier=Modifier.testTag("button_preset_$index"),onClick={change(plus,minus);menu=false}) }
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        listOf(displayLabels(incoming,outgoing).first to Color(0xFF4CAF50),displayLabels(incoming,outgoing).second to Color(0xFFF45151)).forEachIndexed { index,(text,color) ->
            Button(onClick={focus.clearFocus();keyboard?.hide();label=text;editing=index},enabled=enabled,colors=ButtonDefaults.buttonColors(containerColor=color),shape=ReferenceShape,modifier=Modifier.weight(1f).testTag(if(index==0) "edit_incoming" else "edit_outgoing")) {Text(text.ifBlank{"…"},maxLines=2)}
        }
    }
    editing?.let { index ->
        AlertDialog(onDismissRequest={editing=null},title={Text(if(index==0) tr(R.string.cb_141) else tr(R.string.cb_142))},text={
            OutlinedTextField(label,{if(it.length<=30)label=it},singleLine=true,label={Text(tr(R.string.cb_143))},supportingText={Text(tr(R.string.cb_144))},modifier=Modifier.testTag(if(index==0) "custom_incoming" else "custom_outgoing"))
        },confirmButton={TextButton(onClick={if(index==0)change(label.trim(),outgoing)else change(incoming,label.trim());editing=null},enabled=label.isNotBlank(),modifier=Modifier.testTag("label_save")){Text(tr(R.string.cb_145))}},dismissButton={TextButton(onClick={editing=null}){Text(tr(R.string.cb_034))}})
    }
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun AccountIconEditor(icon: String, name: String, enabled: Boolean, change: (String) -> Unit) {
    var picker by rememberSaveable { mutableStateOf(false) }
    val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
    IconButton(onClick={focus.clearFocus();keyboard?.hide();picker=true},enabled=enabled,modifier=Modifier.testTag("account_icon_picker")) {AccountSymbol(icon)}
    if(picker) IconPicker(icon,{picker=false}){change(it);picker=false}
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
private fun IconPicker(icon:String,dismiss:()->Unit,choose:(String)->Unit) {
    val vm=LocalCashBookVM.current;val state by vm.state.collectAsState();val prefs=state.snapshot?.preferences
    var query by rememberSaveable { mutableStateOf("") };var group by rememberSaveable { mutableIntStateOf(-1) }
    var custom by rememberSaveable { mutableStateOf(if(icon.startsWith("i:")) "" else icon) };var customEditing by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    fun select(value:String){vm.useIcon(value);choose(value)}
    AlertDialog(onDismissRequest=dismiss,title={Text(tr(R.string.cb_146))},text={
        Column(Modifier.heightIn(max=430.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(query,{query=it},singleLine=true,placeholder={Text(tr(R.string.cb_147))},modifier=Modifier.fillMaxWidth().testTag("icon_search"))
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(onClick={group=-1;favoritesOnly=false}){Text(tr(R.string.cb_148))}
                com.todoshift.cashbook.core.AccountIconCatalog.ALL.map{it.group}.distinct().forEachIndexed { index,title -> TextButton(onClick={group=index;favoritesOnly=false},modifier=Modifier.testTag("icon_group_$index")){Text(if(group==index) "• ${iconGroupName(index)}" else iconGroupName(index))} }
                TextButton(onClick={favoritesOnly=true;group=-1}){Text(tr(R.string.cb_149))}
            }
            val catalog=com.todoshift.cashbook.core.AccountIconCatalog.ALL
            val groups=catalog.map{it.group}.distinct()
            val prioritized=(prefs?.favorites.orEmpty()+prefs?.recent.orEmpty()).distinct()
            val symbols:List<String> = when {query.isNotBlank()->com.todoshift.cashbook.core.AccountIconCatalog.search(query).map{it.id};favoritesOnly->prefs?.favorites.orEmpty();group>=0->catalog.filter{it.group==groups[group]}.map{it.id};else->(prioritized+catalog.take(20).map{it.id}).distinct()}
            if(group<0&&query.isBlank())Text(tr(R.string.cb_150),style=MaterialTheme.typography.bodySmall)
            LazyVerticalGrid(GridCells.Fixed(4),Modifier.weight(1f,fill=false).heightIn(max=230.dp)) {
                items(symbols,key={it}){symbol->Column(horizontalAlignment=Alignment.CenterHorizontally){
                    TextButton(onClick={select(symbol)},enabled=!state.busy,contentPadding=PaddingValues(4.dp),modifier=Modifier.sizeIn(minHeight=44.dp).testTag("icon_$symbol")){AccountSymbol(symbol)}
                    TextButton(onClick={vm.favoriteIcon(symbol)},enabled=!state.busy,contentPadding=PaddingValues(0.dp),modifier=Modifier.height(28.dp).testTag("favorite_$symbol")){Text(if(prefs?.favorites?.contains(symbol)==true) "★" else "☆")}
                }}
            }
            if(symbols.isEmpty())Text(tr(R.string.cb_151))
            if(customEditing)OutlinedTextField(custom,{if(it.length<=16)custom=it},singleLine=true,label={Text(tr(R.string.cb_152))},modifier=Modifier.fillMaxWidth().testTag("custom_icon"))
            else Row {TextButton(onClick={customEditing=true},modifier=Modifier.testTag("custom_icon_open")){Text(tr(R.string.cb_152))};TextButton(onClick={choose("")},modifier=Modifier.testTag("account_icon_clear")){Text(tr(R.string.cb_153))}}
            state.error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        }
    },confirmButton={if(customEditing)TextButton(onClick={select(custom.trim())},enabled=!state.busy,modifier=Modifier.testTag("custom_icon_apply")){Text(tr(R.string.cb_154))}},dismissButton={TextButton(onClick=dismiss){Text(tr(R.string.cb_034))}})
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun CompactAccountDialog(account:Ledger.Account?,defaults:Ledger.ButtonLabels,busy:Boolean,error:String?,dismiss:()->Unit,save:(String,String,String,String,String,String,String)->Unit, createDebt:(()->Unit)?=null) {
    var name by rememberSaveable(account?.id){mutableStateOf(account?.name.orEmpty())}
    var opening by rememberSaveable(account?.id){mutableStateOf(account?.let{com.todoshift.cashbook.core.WholeMoney.edit(it.openingMinor)}.orEmpty())}
    var day by rememberSaveable(account?.id){mutableStateOf(account?.openingDate ?: java.time.LocalDate.now().toString())}
    var created by rememberSaveable(account?.id){mutableStateOf(account?.createdDate ?: java.time.LocalDate.now().toString())}
    var incoming by rememberSaveable(account?.id){mutableStateOf(account?.incomingLabel ?: defaults.incoming)}
    var outgoing by rememberSaveable(account?.id){mutableStateOf(account?.outgoingLabel ?: defaults.outgoing)}
    var icon by rememberSaveable(account?.id){mutableStateOf(account?.icon.orEmpty())}
    var dates by rememberSaveable{mutableStateOf(false)};var discard by remember{mutableStateOf(false)}
    val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
    val initialOpening=account?.let{com.todoshift.cashbook.core.WholeMoney.edit(it.openingMinor)}.orEmpty()
    val dirty=name!=account?.name.orEmpty()||opening!=initialOpening||incoming!=(account?.incomingLabel?:defaults.incoming)||outgoing!=(account?.outgoingLabel?:defaults.outgoing)||icon!=account?.icon.orEmpty()||day!=(account?.openingDate?:java.time.LocalDate.now().toString())||created!=(account?.createdDate?:java.time.LocalDate.now().toString())
    val close:()->Unit={if(!busy){if(dirty)discard=true else dismiss()}}
    androidx.compose.ui.window.Dialog(close,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(0.94f).widthIn(max=430.dp).imePadding().heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp*0.90f).dp),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(if(account==null) tr(R.string.cb_155) else tr(R.string.cb_156),style=MaterialTheme.typography.titleLarge);TextButton(onClick={dates=true},enabled=!busy,modifier=Modifier.testTag("account_dates")){Text(tr(R.string.cb_021))}}
                Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(createDebt!=null)TextButton(createDebt,enabled=!busy,modifier=Modifier.testTag("create_debt_open")){Text(tr(R.string.cb_157))}
                    OutlinedTextField(name,{if(it.length<=100)name=it},placeholder={Text(tr(R.string.cb_158))},singleLine=true,enabled=!busy,trailingIcon={AccountIconEditor(icon,name,!busy){icon=it}},shape=ReferenceShape,modifier=Modifier.fillMaxWidth().testTag(if(account==null) "account_name" else "account_edit_name"))
                    MoneyField(opening,{opening=it},placeholder={Text(tr(R.string.cb_159))},singleLine=true,enabled=!busy,keyboardOptions=KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number,imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={focus.clearFocus();keyboard?.hide()}),shape=ReferenceShape,modifier=Modifier.fillMaxWidth().testTag("account_opening"),signed=true)
                    ButtonLabelsEditor(incoming,outgoing,!busy){plus,minus->incoming=plus;outgoing=minus}
                    error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton(onClick=close,enabled=!busy){Text(tr(R.string.cb_034))}
                    TextButton(onClick={save(name,opening,day,created,incoming,outgoing,icon)},enabled=!busy&&name.isNotBlank()&&validLabels(incoming,outgoing),modifier=Modifier.testTag(if(account==null) "account_create_save" else "account_details_save")){Text(tr(R.string.cb_145))}
                }
            }
        }
    }
    if(dates)AlertDialog(onDismissRequest={dates=false},title={Text(tr(R.string.cb_160))},text={Column{DayPicker(tr(R.string.cb_161),created,!busy){created=it};DayPicker(tr(R.string.cb_162),day,!busy){day=it}}},confirmButton={TextButton(onClick={dates=false}){Text(tr(R.string.cb_067))}})
    if(discard)DiscardDialog({discard=false},dismiss)
}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun AccountDetailsDialog(account:Ledger.Account,defaults:Ledger.ButtonLabels,busy:Boolean,error:String?,dismiss:()->Unit,save:(String,String,String,String)->Unit) {
    val vm=LocalCashBookVM.current
    val state by vm.state.collectAsState()
    if(state.snapshot?.debts?.any{it.accountId==account.id}==true){
        var name by rememberSaveable{mutableStateOf(account.name)};var icon by rememberSaveable{mutableStateOf(account.icon)}
        AlertDialog(onDismissRequest=dismiss,title={Text(tr(R.string.cb_163))},text={Column{OutlinedTextField(name,{name=it.take(100)},label={Text(tr(R.string.cb_164))});AccountIconEditor(icon,name,!busy){icon=it};error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton({vm.customizeAccount(account.id,name,account.incomingLabel,account.outgoingLabel,icon,dismiss)},enabled=!busy&&name.isNotBlank()){Text(tr(R.string.cb_145))}},dismissButton={TextButton(dismiss){Text(tr(R.string.cb_034))}})
    }else CompactAccountDialog(account,defaults,busy,error,dismiss,{name,opening,day,created,plus,minus,icon->vm.editAccount(account.id,name,opening,day,created,plus,minus,icon,dismiss)})

}

@androidx.compose.runtime.NonSkippableComposable
@Composable
internal fun DefaultButtonsDialog(defaults:Ledger.ButtonLabels,busy:Boolean,error:String?,dismiss:()->Unit,save:(String,String)->Unit) {
    var incoming by rememberSaveable{mutableStateOf(defaults.incoming)};var outgoing by rememberSaveable{mutableStateOf(defaults.outgoing)};var discard by remember{mutableStateOf(false)}
    val close:()->Unit={if(!busy){if(incoming!=defaults.incoming||outgoing!=defaults.outgoing)discard=true else dismiss()}}
    AlertDialog(onDismissRequest=close,title={Text(tr(R.string.cb_165))},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(tr(R.string.cb_166));ButtonLabelsEditor(incoming,outgoing,!busy){a,b->incoming=a;outgoing=b};error?.let{Text(it,color=MaterialTheme.colorScheme.error)}}},confirmButton={TextButton(onClick={save(incoming,outgoing)},enabled=!busy&&validLabels(incoming,outgoing),modifier=Modifier.testTag("defaults_save")){Text(tr(R.string.cb_145))}},dismissButton={TextButton(onClick=close){Text(tr(R.string.cb_034))}})
    if(discard)DiscardDialog({discard=false},dismiss)
}
