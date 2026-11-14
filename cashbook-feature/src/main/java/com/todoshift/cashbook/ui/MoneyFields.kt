package com.todoshift.cashbook.ui
import android.text.*
import android.text.method.DigitsKeyListener
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.foundation.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.todoshift.cashbook.core.WholeMoney

@androidx.compose.runtime.NonSkippableComposable @Composable internal fun IntegerEditor(value:String,change:(String)->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,readOnly:Boolean=false,signed:Boolean=false,placeholder:String=""){
 val current by rememberUpdatedState(change)
 val good:(String)->Boolean={WholeMoney.input(it,signed)}
 AndroidView(factory={context->EditText(context).apply{
  setSingleLine(true);setTextSize(16f);setTextColor(android.graphics.Color.rgb(30,40,50));setPadding(12,12,12,12)
  background=null;layoutDirection=android.view.View.LAYOUT_DIRECTION_LTR;textDirection=android.view.View.TEXT_DIRECTION_LTR
  keyListener=DigitsKeyListener.getInstance("0123456789٠١٢٣٤٥٦٧٨٩۰۱۲۳۴۵۶۷۸۹"+if(signed)"-" else "")
  setRawInputType(android.text.InputType.TYPE_CLASS_NUMBER or if(signed)android.text.InputType.TYPE_NUMBER_FLAG_SIGNED else 0)
  imeOptions=EditorInfo.IME_ACTION_DONE;setSelectAllOnFocus(true)
  addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){if(tag!="updating"&&good(s.toString()))current(s.toString())};override fun afterTextChanged(s:Editable?){} })
  filters=arrayOf(InputFilter{source,start,end,dest,dstart,dend->val candidate=dest.toString().substring(0,dstart)+source.subSequence(start,end)+dest.toString().substring(dend);if(tag=="updating"||good(candidate))null else dest.subSequence(dstart,dend)})
 }},update={edit->edit.isEnabled=enabled&&!readOnly;edit.hint=placeholder;if(edit.text.toString()!=value){edit.tag="updating";val listener=edit.keyListener;val rules=edit.filters;val type=edit.inputType;edit.keyListener=null;edit.filters=emptyArray();edit.setText(value);edit.setSelection(value.length);edit.keyListener=listener;edit.filters=rules;edit.setRawInputType(type);edit.tag=null}},modifier=modifier.heightIn(min=48.dp).semantics{editableText=AnnotatedString(value);setText{if(good(it.text)){current(it.text);true}else false}})
}
@androidx.compose.runtime.NonSkippableComposable @Composable internal fun MoneyField(value:String,onValueChange:(String)->Unit,modifier:Modifier=Modifier,label:(@Composable ()->Unit)?=null,placeholder:(@Composable ()->Unit)?=null,enabled:Boolean=true,readOnly:Boolean=false,singleLine:Boolean=true,shape:Shape=MaterialTheme.shapes.small,keyboardOptions:KeyboardOptions=KeyboardOptions.Default,keyboardActions:KeyboardActions=KeyboardActions.Default,trailingIcon:(@Composable ()->Unit)?=null,signed:Boolean=false){
 Column(modifier.semantics{editableText=AnnotatedString(value);setText{if(com.todoshift.cashbook.core.WholeMoney.input(it.text,signed)){onValueChange(it.text);true}else false}}){label?.invoke();Surface(shape=shape,border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline),color=MaterialTheme.colorScheme.surface){Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically){IntegerEditor(value,onValueChange,Modifier.weight(1f),enabled,readOnly,signed);trailingIcon?.invoke();if(value.isNotEmpty())TextButton({onValueChange("")},enabled=enabled&&!readOnly,contentPadding=PaddingValues(2.dp)){Text("×")}}}}
}

/** Keep dialog footers inside the window resized by the IME, including API 26. */
@Composable internal fun ResizeCashBookDialog(){
 val view=androidx.compose.ui.platform.LocalView.current
 val window=(view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
 SideEffect{window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)}
}
