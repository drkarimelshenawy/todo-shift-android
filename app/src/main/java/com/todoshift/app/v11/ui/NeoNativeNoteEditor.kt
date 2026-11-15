package com.todoshift.app.v11.ui

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** A native editable buffer: selection and deletion never operate on hidden HTML tokens. */
internal class NativeNoteEditText(context:Context):EditText(context) {
    var updating=false
    var published=TextFieldValue()
    var change:(TextFieldValue)->Unit={}
    private var newLine:Int?=null
    init {
        setTextIsSelectable(true);setSingleLine(false);minLines=7;gravity=android.view.Gravity.TOP or android.view.Gravity.START
        setBackgroundColor(Color.TRANSPARENT);setPadding(12,12,12,12)
        textDirection=View.TEXT_DIRECTION_FIRST_STRONG
        isSaveEnabled=false
        imeOptions=imeOptions or android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int)=Unit
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) {newLine=if(!updating&&before==0&&count==1&&s?.get(start)=='\n')start else null}
            override fun afterTextChanged(s:Editable?) {
                if(!updating&&s!=null) {
                    val at=newLine;newLine=null
                    if(at!=null) {
                        val from=s.toString().lastIndexOf('\n',at-1)+1
                        val list=s.getSpans(from,(from+1).coerceAtMost(s.length),NoteList::class.java).firstOrNull {s.getSpanStart(it)==from}
                        if(list!=null&&s.toString().substring(from+1,at).isNotBlank()) {
                            updating=true;s.insert(at+1,"\uFFFC ");s.setSpan(NoteList(list.kind,list.number+1,false),at+1,at+2,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);setSelection(at+3);updating=false
                        }
                    }
                    s.getSpans(0,s.length,NoteChecklistDone::class.java).forEach {s.removeSpan(it)}
                    s.getSpans(0,s.length,NoteList::class.java).filter {it.kind=="check"&&it.checked}.forEach {list->
                        val from=s.getSpanStart(list)+2;val end=s.toString().indexOf('\n',from).takeIf {it>=0} ?: s.length
                        if(from<end)s.setSpan(NoteChecklistDone(),from,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    publish()
                }
            }
        })
    }
    fun publish(textChanged:Boolean=true) {
        if(updating)return
        val current=editableText ?: return
        val value=if(textChanged) NeoRichTextCodec.value(current,selectionStart.coerceAtLeast(0),selectionEnd.coerceAtLeast(0)) else {
            val parsed=NeoRichTextCodec.parse(published.text)
            published.copy(selection=androidx.compose.ui.text.TextRange(parsed.source(selectionStart.coerceAtLeast(0)),parsed.source(selectionEnd.coerceAtLeast(0))))
        }
        published=value;change(value)
    }
    override fun onSelectionChanged(start:Int,end:Int) {super.onSelectionChanged(start,end);if(!updating&&hasFocus()) publish(false)}
    override fun onTouchEvent(event:MotionEvent):Boolean {
        if(event.action==MotionEvent.ACTION_UP && isEnabled) {
            val offset=getOffsetForPosition(event.x,event.y)
            val marker=editableText?.getSpans((offset-1).coerceAtLeast(0),(offset+1).coerceAtMost(length()),NoteList::class.java)?.firstOrNull {
                val at=editableText.getSpanStart(it);val a=layout?.getPrimaryHorizontal(at) ?: 0f;val z=layout?.getPrimaryHorizontal(at+1) ?: 0f
                val x=event.x-totalPaddingLeft+scrollX
                it.kind=="check"&&x>=minOf(a,z)&&x<=maxOf(a,z)
            }
            if(marker!=null) {
                marker.checked=!marker.checked
                val at=editableText.getSpanStart(marker)
                val end=editableText.toString().indexOf('\n',at).takeIf {it>=0} ?: length()
                editableText.getSpans(at,end,NoteChecklistDone::class.java).forEach {editableText.removeSpan(it)}
                if(marker.checked&&at+2<end)editableText.setSpan(NoteChecklistDone(),at+2,end,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                invalidate();publish();return true
            }
        }
        return super.onTouchEvent(event)
    }
    fun apply(value:TextFieldValue) {
        if(value.text==published.text) {
            if(value.selection!=published.selection) {
                val parsed=NeoRichTextCodec.parse(value.text)
                updating=true;setSelection(parsed.visible(value.selection.start).coerceIn(0,length()),parsed.visible(value.selection.end).coerceIn(0,length()));updating=false
                published=value
            }
            return
        }
        val parsed=NeoRichTextCodec.parse(value.text)
        updating=true
        setText(parsed.text,BufferType.EDITABLE)
        setSelection(parsed.visible(value.selection.start).coerceIn(0,length()),parsed.visible(value.selection.end).coerceIn(0,length()))
        published=value;updating=false
    }
}

@Composable
internal fun NeoNativeNoteEditor(value:TextFieldValue,change:(TextFieldValue)->Unit,enabled:Boolean,modifier:Modifier=Modifier) {
    val ink=MaterialTheme.colorScheme.onSurface.toArgb()
    val latest by rememberUpdatedState(change)
    OutlinedCard(modifier.fillMaxWidth()) {
        AndroidView(factory={NativeNoteEditText(it)},modifier=Modifier.fillMaxWidth().heightIn(min=200.dp,max=420.dp),update={view->
            view.change={latest(it)};view.isEnabled=enabled;view.setTextColor(ink);view.setHintTextColor(ink);view.textSize=16f
            view.apply(value)
        })
    }
}
