package com.todoshift.app.v11.ui

import android.graphics.Typeface
import android.text.style.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal fun noteFormat(value:TextFieldValue,prefix:String,suffix:String=""):TextFieldValue=NeoRichTextCodec.format(value,prefix,suffix)

@Composable
internal fun NeoNoteFormatting(value:TextFieldValue,change:(TextFieldValue)->Unit,undo:()->Unit,redo:()->Unit,canUndo:Boolean,canRedo:Boolean) {
    var link by remember {mutableStateOf<TextFieldValue?>(null)}
    var url by remember {mutableStateOf("https://")}
    var colors by remember {mutableStateOf(false)}
    var highlight by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)}
    val selectMessage=neoLabel("حدد النص أولًا","Select text first")
    fun apply(prefix:String,suffix:String="") {
        if(value.selection.collapsed&&!prefix.startsWith("#")&&!prefix.startsWith("\n")&&!prefix.startsWith("<align:")) {message=selectMessage;return}
        change(noteFormat(value,prefix,suffix));message=null
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        TextButton(enabled=canUndo,onClick=undo,contentPadding=PaddingValues(4.dp)){Text("↶",fontSize=22.sp)}
        TextButton(enabled=canRedo,onClick=redo,contentPadding=PaddingValues(4.dp)){Text("↷",fontSize=22.sp)}
        listOf("B" to ("**" to "**"),"I" to ("*" to "*"),"U" to ("<u>" to "</u>"),"S̶" to ("<s>" to "</s>"),"H1" to ("# " to ""),"H2" to ("## " to ""),"H3" to ("### " to ""),"•" to ("\n- " to ""),"1." to ("\n1. " to ""),"☐" to ("\n- [ ] " to ""),"—" to ("\n---\n" to "")).forEach {(label,marks)->
            TextButton(onClick={apply(marks.first,marks.second)},contentPadding=PaddingValues(4.dp),modifier=Modifier.widthIn(min=44.dp)){Text(label,fontWeight=if(label=="B")FontWeight.Bold else null)}
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(2.dp)) {
        TextButton(onClick={if(value.selection.collapsed)message=selectMessage else {link=value;url="https://"}}){Text(neoLabel("رابط","Link"))}
        TextButton(onClick={colors=!colors;highlight=false}){Text(neoLabel("لون الخط","Text color"))}
        TextButton(onClick={colors=!colors;highlight=true}){Text(neoLabel("تمييز","Highlight"))}
        listOf("start" to neoLabel("بداية","Start"),"center" to neoLabel("وسط","Center"),"end" to neoLabel("نهاية","End")).forEach {(alignment,label)->TextButton(onClick={apply("<align:$alignment>")}){Text(label)}}
    }
    if(colors) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        listOf("#202124","#C62828","#1565C0","#2E7D32","#7B1FA2","#FBC02D","#FFFFFF").forEach {hex->
            FilterChip(selected=false,onClick={apply(if(highlight)"<mark color=\"$hex\">" else "<font color=\"$hex\">",if(highlight)"</mark>" else "</font>")},label={Text("●",color=Color(hex.removePrefix("#").toLong(16) or 0xFF000000L),fontSize=22.sp)})
        }
    }
    message?.let {Text(it,style=MaterialTheme.typography.labelSmall)}
    link?.let {selected->AlertDialog(onDismissRequest={link=null},title={Text(neoLabel("رابط للنص المحدد","Link selected text"))},text={OutlinedTextField(url,{url=it},label={Text("https:// / mailto:")},singleLine=true)},confirmButton={TextButton(enabled=NeoRichTextCodec.safeLink(url)&&url.substringAfter(":").trim('/').isNotBlank(),onClick={change(noteFormat(selected,"[","]($url)"));link=null}){Text(neoLabel("تطبيق","Apply"))}},dismissButton={TextButton(onClick={link=null}){Text(neoLabel("إلغاء","Cancel"))}})}
}

/** Shared native-span projection: editor, preview and cards interpret the same formatting. */
internal fun noteInline(raw:String):AnnotatedString {
    val parsed=NeoRichTextCodec.parse(raw);val text=parsed.text
    val visible=StringBuilder(text.toString())
    text.getSpans(0,text.length,NoteList::class.java).forEach {span->val at=text.getSpanStart(span);if(at in visible.indices)visible.setCharAt(at,when(span.kind){"check"->if(span.checked)'☑' else '☐';"number"->('0'.code+(span.number%10)).toChar();else->'•'})}
    return buildAnnotatedString {
        append(visible.toString())
        text.getSpans(0,text.length,Any::class.java).forEach {span->
            val start=text.getSpanStart(span).coerceIn(0,length);val end=text.getSpanEnd(span).coerceIn(start,length)
            if(end>start) when(span) {
                is StyleSpan->addStyle(SpanStyle(fontWeight=if(span.style and Typeface.BOLD!=0)FontWeight.Bold else null,fontStyle=if(span.style and Typeface.ITALIC!=0)FontStyle.Italic else null),start,end)
                is UnderlineSpan->addStyle(SpanStyle(textDecoration=TextDecoration.Underline),start,end)
                is StrikethroughSpan->addStyle(SpanStyle(textDecoration=TextDecoration.LineThrough),start,end)
                is ForegroundColorSpan->addStyle(SpanStyle(color=Color(span.foregroundColor)),start,end)
                is BackgroundColorSpan->addStyle(SpanStyle(background=Color(span.backgroundColor)),start,end)
                is RelativeSizeSpan->addStyle(SpanStyle(fontSize=(16*span.sizeChange).sp),start,end)
                is AlignmentSpan->addStyle(ParagraphStyle(textAlign=when(span.alignment){android.text.Layout.Alignment.ALIGN_CENTER->TextAlign.Center;android.text.Layout.Alignment.ALIGN_OPPOSITE->TextAlign.End;else->TextAlign.Start}),start,end)
                is URLSpan->if(NeoRichTextCodec.safeLink(span.url)){addStringAnnotation("URL",span.url,start,end);addStyle(SpanStyle(color=Color(0xFF1565C0),textDecoration=TextDecoration.Underline),start,end)}
            }
        }
    }
}
internal fun notePlainText(raw:String):String=noteInline(raw).text
/** Strip only private linkage tokens. Markup is interpreted by the shared renderer. */
internal fun noteDisplayText(text:String):String=text.replace(Regex(" ?<!--task:[A-Za-z0-9_-]+-->"),"")

@Composable
internal fun NeoNotePreview(text:String,checklistsOnly:Boolean=false,onChecklist:((Int)->Unit)?=null,onCreateTask:((Int,String)->Unit)?=null) {
    val uri=LocalUriHandler.current
    Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        text.lines().forEachIndexed {index,line->
            val checklistLine=line.replaceFirst(Regex("^<div align=\"(?:start|center|end)\">"),"").removeSuffix("</div>")
            val checked=Regex("^\\s*- \\[([ xX])\\] (.*)$").matchEntire(checklistLine)
            if(checked!=null) Row {
                Checkbox(checked.groupValues[1]!=" ",onCheckedChange=onChecklist?.let {callback->{_:Boolean->callback(index)}})
                Text(noteInline(checked.groupValues[2]),Modifier.weight(1f).padding(top=12.dp),textDecoration=if(checked.groupValues[1]!=" ")TextDecoration.LineThrough else null)
                if(onCreateTask!=null) TextButton(onClick={onCreateTask(index,notePlainText(checked.groupValues[2]))}){Text(if(line.contains("<!--task:"))neoLabel("فتح المهمة","Open task") else neoLabel("إلى مهمة","To task"))}
            } else if(!checklistsOnly) {
                val rendered=noteInline(line)
                ClickableText(rendered,style=MaterialTheme.typography.bodyMedium.copy(color=MaterialTheme.colorScheme.onSurface),onClick={offset->rendered.getStringAnnotations("URL",offset,offset).firstOrNull()?.let {runCatching {uri.openUri(it.item)}}})
            }
        }
    }
}

/** Kept for inherited read-only projections; editing uses the native buffer above. */
internal class NeoNoteVisualTransformation:androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text:AnnotatedString):androidx.compose.ui.text.input.TransformedText {
        val parsed=NeoRichTextCodec.parse(text.text)
        return androidx.compose.ui.text.input.TransformedText(noteInline(text.text),object:androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset:Int)=parsed.visible(offset)
            override fun transformedToOriginal(offset:Int)=parsed.source(offset)
        })
    }
}
