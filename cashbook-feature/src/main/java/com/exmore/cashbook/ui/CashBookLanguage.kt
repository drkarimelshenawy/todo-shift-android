package com.exmore.cashbook.ui
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.exmore.cashbook.R
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
/** Persisted landing section. Switching tabs doesn't overwrite the user's preference. */
object CashBookSectionPreference {
    const val ACCOUNTS = "accounts"
    const val DEBTS = "debts"
    const val SALARY = "salary"
    val validSections = listOf(ACCOUNTS, DEBTS, SALARY)
    private const val FILE = "cashbook_section_preferences"
    private const val KEY = "default_section"
    private val revision = mutableIntStateOf(0)

    fun normalize(section: String?): String = section?.takeIf { it in validSections } ?: ACCOUNTS
    fun load(context: Context): String {
        revision.intValue // Make settings and the active Cash Book react to changed defaults.
        return normalize(context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, ACCOUNTS))
    }
    fun save(context: Context, section: String): Boolean {
        require(section in validSections)
        val ok = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY, section).commit()
        if (ok) revision.intValue++
        return ok
    }
}

/** Module language override; AUTO uses the host context locale. Layout follows the selected language. */
object CashBookText{
 private lateinit var app:Context
 val language=MutableStateFlow("AUTO")
 private var host:Locale?=null
 private val revision=mutableIntStateOf(0)
 // Configuration contexts must reflect current density, locale and configuration.
 private fun context(locale:Locale):Context { val cfg=Configuration(app.resources.configuration);cfg.setLocale(locale);return app.createConfigurationContext(cfg) }
 fun initialize(context:Context){app=context.applicationContext;val prefs=app.getSharedPreferences("cashbook-language",0);if(!prefs.contains("language"))prefs.edit().putString("language",if(java.io.File(app.noBackupFilesDir,"cashbook-v1.bin").exists())"ar"else"AUTO").apply();language.value=prefs.getString("language","AUTO")?:"AUTO"}
 fun hostLocale(locale:Locale?){if(host!=locale){host=locale;revision.intValue++}}
 fun select(value:String){require(value in listOf("AUTO","ar","en"));app.getSharedPreferences("cashbook-language",0).edit().putString("language",value).apply();language.value=value;revision.intValue++}
 fun locale():Locale{revision.intValue;return when(language.value){"ar"->Locale("ar");"en"->Locale.ENGLISH;else->(host?:app.resources.configuration.locales[0]).let{if(it.language=="ar")Locale("ar")else Locale.ENGLISH}}}
 fun get(id:Int,vararg args:Any):String{val context=context(locale());return if(args.isEmpty())context.getString(id) else context.getString(id,*args.map{"\u2068$it\u2069"}.toTypedArray())}
 fun inLocale(language:String,id:Int):String{return context(Locale(language)).getString(id)}
 fun dialogContext(base:Context):Context=android.view.ContextThemeWrapper(base,android.R.style.Theme_Material_Light_Dialog_Alert).apply{applyOverrideConfiguration(Configuration(base.resources.configuration).apply{setLocale(locale())})}
 fun arabic()=locale().language=="ar"
}
internal fun tr(id:Int,vararg args:Any)=CashBookText.get(id,*args)
@androidx.compose.runtime.NonSkippableComposable @Composable internal fun LanguageDialog(dismiss:()->Unit){
 val vm=LocalCashBookVM.current
 val selectedLanguage by CashBookText.language.collectAsState()
 AlertDialog(onDismissRequest=dismiss,title={Text(tr(R.string.cb_250))},text={Column{
  listOf("AUTO" to tr(R.string.cb_251),"ar" to tr(R.string.cb_252),"en" to tr(R.string.cb_lang_english)).forEach{(id,label)->
   TextButton({CashBookText.select(id);vm.reload()},modifier=Modifier.fillMaxWidth().then(Modifier.testTag("language_$id"))){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Icon(Icons.Rounded.Language,null)
     Text(label,Modifier.weight(1f))
     if(selectedLanguage==id)Icon(Icons.Rounded.Check,null)
    }
   }
  }
 }},confirmButton={TextButton(dismiss){Text(tr(R.string.cb_067))}})
}
@androidx.compose.runtime.NonSkippableComposable @Composable internal fun SectionSettings(dismiss:()->Unit,buttons:()->Unit){
 var language by remember{mutableStateOf(false)}
 AlertDialog(onDismissRequest=dismiss,title={Text(tr(R.string.cb_012))},text={Column{
  TextButton({language=true},modifier=Modifier.fillMaxWidth()){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Rounded.Language,null); Spacer(Modifier.width(8.dp)); Text(tr(R.string.cb_250)) }
  }
  TextButton(buttons,modifier=Modifier.fillMaxWidth()){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Rounded.Tune,null); Spacer(Modifier.width(8.dp)); Text(tr(R.string.cb_165)) }
  }
 }},confirmButton={TextButton(dismiss){Text(tr(R.string.cb_029))}})
 if(language)LanguageDialog{language=false}
}
@Composable
internal fun CashBookDefaultSectionDialog(dismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selected by remember { mutableStateOf(CashBookSectionPreference.load(context)) }
    var failure by remember { mutableStateOf(false) }
    val options = listOf(
        Triple(CashBookSectionPreference.ACCOUNTS, tr(R.string.cb_section_accounts), Icons.Rounded.AccountBalanceWallet),
        Triple(CashBookSectionPreference.DEBTS, tr(R.string.cb_section_debts), Icons.Rounded.ReceiptLong),
        Triple(CashBookSectionPreference.SALARY, tr(R.string.cb_section_salary), Icons.Rounded.Payments),
    )
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (CashBookText.arabic()) "القسم الافتراضي" else "Default section") },
        text = {
            Column {
                options.forEach { (key, label, icon) ->
                    TextButton(onClick = {
                        if (CashBookSectionPreference.save(context, key)) { selected = key; dismiss() }
                        else failure = true
                    }, modifier = Modifier.fillMaxWidth().testTag("cashbook_default_$key")) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(icon, null)
                            Text(label, Modifier.weight(1f))
                            if (selected == key) Icon(Icons.Rounded.Check, null)
                        }
                    }
                }
                if (failure) Text(if (CashBookText.arabic()) "تعذر حفظ الاختيار" else "Unable to save selection",
                    color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(tr(R.string.cb_029)) } },
    )
}

/** Keep all financial dropdowns aligned to the Cash Book language, even inside dialogs. */
@Composable
internal fun CashBookMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides
        if (CashBookText.arabic()) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest,
            modifier = modifier, content = content)
    }
}

internal fun iconGroupName(index:Int)=tr(intArrayOf(R.string.cb_group_0,R.string.cb_group_1,R.string.cb_group_2,R.string.cb_group_3,R.string.cb_group_4,R.string.cb_group_5,R.string.cb_group_6,R.string.cb_group_7)[index])
private val presetIds=listOf(R.string.cb_001 to R.string.cb_002,R.string.cb_115 to R.string.cb_116,R.string.cb_117 to R.string.cb_118,R.string.cb_119 to R.string.cb_120,R.string.cb_121 to R.string.cb_122,R.string.cb_123 to R.string.cb_124,R.string.cb_125 to R.string.cb_126,R.string.cb_127 to R.string.cb_128,R.string.cb_066 to R.string.cb_129,R.string.cb_130 to R.string.cb_131,R.string.cb_132 to R.string.cb_133,R.string.cb_134 to R.string.cb_135)
internal fun displayLabels(incoming:String,outgoing:String):Pair<String,String>{
 // Stored labels are never rewritten. Built-in pairs have language-aware presentation.
 for(pair in presetIds){
  val ar=CashBookText.inLocale("ar",pair.first) to CashBookText.inLocale("ar",pair.second)
  val en=CashBookText.inLocale("en",pair.first) to CashBookText.inLocale("en",pair.second)
  if(incoming==ar.first&&outgoing==ar.second||incoming==en.first&&outgoing==en.second)return tr(pair.first) to tr(pair.second)
 }
 return incoming to outgoing
}
