package com.todoshift.cashbook.ui
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.todoshift.cashbook.R
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
/** Module language override; AUTO uses the host context locale. Geometry stays LTR. */
object CashBookText{
 private lateinit var app:Context
 val language=MutableStateFlow("AUTO")
 private var host:Locale?=null
 private val revision=mutableIntStateOf(0)
 private val contexts=mutableMapOf<String,Context>()
 @Synchronized private fun context(locale:Locale):Context=contexts.getOrPut(locale.toLanguageTag()){val cfg=Configuration(app.resources.configuration);cfg.setLocale(locale);app.createConfigurationContext(cfg)}
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
 AlertDialog(onDismissRequest=dismiss,title={Text(tr(R.string.cb_250))},text={Column{
  listOf("AUTO" to tr(R.string.cb_251),"ar" to tr(R.string.cb_252),"en" to tr(R.string.cb_lang_english)).forEach{(id,label)->TextButton({CashBookText.select(id);vm.reload()},modifier=Modifier.fillMaxWidth().then(Modifier.testTag("language_$id"))){Text((if(CashBookText.language.value==id)"✓ " else "")+label)}}
 }},confirmButton={TextButton(dismiss){Text(tr(R.string.cb_067))}})
}
@androidx.compose.runtime.NonSkippableComposable @Composable internal fun SectionSettings(dismiss:()->Unit,buttons:()->Unit){
 var language by remember{mutableStateOf(false)}
 AlertDialog(onDismissRequest=dismiss,title={Text(tr(R.string.cb_012))},text={Column{
  TextButton({language=true},modifier=Modifier.fillMaxWidth()){Text(tr(R.string.cb_250))}
  TextButton(buttons,modifier=Modifier.fillMaxWidth()){Text(tr(R.string.cb_165))}
 }},confirmButton={TextButton(dismiss){Text(tr(R.string.cb_029))}})
 if(language)LanguageDialog{language=false}
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
