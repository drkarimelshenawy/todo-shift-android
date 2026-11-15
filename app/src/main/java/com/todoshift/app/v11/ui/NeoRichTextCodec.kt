package com.todoshift.app.v11.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.*
import android.text.style.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Native spans are the editing model; portable markup remains in the existing details field. */
internal class NoteHeading(val level:Int)
internal class NoteHeadingBold:StyleSpan(Typeface.BOLD)
internal class NoteChecklistDone:StrikethroughSpan()
internal class NoteTaskToken(val id:String)
internal class NoteList(val kind:String,val number:Int=1,var checked:Boolean=false):ReplacementSpan() {
    override fun getSize(p:Paint,text:CharSequence,start:Int,end:Int,fm:Paint.FontMetricsInt?):Int=(p.textSize*1.5f).toInt()
    override fun draw(c:Canvas,text:CharSequence,start:Int,end:Int,x:Float,top:Int,y:Int,bottom:Int,p:Paint) {
        val old=p.style;val stroke=p.strokeWidth
        if(kind=="check") {
            val size=p.textSize*.85f;val t=y+p.fontMetrics.ascent*.8f
            p.style=Paint.Style.STROKE;p.strokeWidth=2f;c.drawRoundRect(x,t,x+size,t+size,3f,3f,p)
            if(checked) {c.drawLine(x+size*.2f,t+size*.5f,x+size*.45f,t+size*.75f,p);c.drawLine(x+size*.45f,t+size*.75f,x+size*.85f,t+size*.2f,p)}
        } else c.drawText(if(kind=="number") "$number." else "•",x,y.toFloat(),p)
        p.style=old;p.strokeWidth=stroke
    }
}
internal data class ParsedRichNote(val text:SpannableStringBuilder,val rawToVisible:IntArray) {
    fun visible(offset:Int)=rawToVisible[offset.coerceIn(rawToVisible.indices)]
    fun source(offset:Int):Int {
        val position=offset.coerceIn(0,text.length)
        // Prefer the first character after opening syntax, before closing syntax.
        return rawToVisible.indices.firstOrNull {it<rawToVisible.lastIndex&&rawToVisible[it]==position&&rawToVisible[it+1]>position}
            ?: rawToVisible.indexOfLast {it==position}.coerceAtLeast(0)
    }
}

internal object NeoRichTextCodec {
    private const val FLAGS=Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    private data class Open(val key:String,val span:Any,val start:Int)
    fun parse(raw:String):ParsedRichNote {
        val out=SpannableStringBuilder();val map=IntArray(raw.length+1);val open=mutableListOf<Open>();val emptySpans=mutableListOf<Pair<Any,Int>>()
        var i=0;var lineStart=true;var paragraphStart=0;var heading=0;var alignment:String?=null
        fun mark(end:Int) {while(i<end) {map[i]=out.length;i++};map[i]=out.length}
        fun toggle(key:String,span:()->Any,closing:Boolean=false) {
            val at=open.indexOfLast {it.key==key}
            if(!closing&&key!in listOf("**","*")) open+=Open(key,span(),out.length)
            else if(at>=0) {val item=open.removeAt(at);out.setSpan(item.span,item.start,out.length,FLAGS)}
            else if(!closing) open+=Open(key,span(),out.length)
        }
        fun paragraphEnd() {
            if(heading>0) {
                val spans=listOf(NoteHeading(heading),RelativeSizeSpan(when(heading){1->1.6f;2->1.3f;else->1.1f}),NoteHeadingBold())
                spans.forEach {if(paragraphStart==out.length)emptySpans+=it to paragraphStart else out.setSpan(it,paragraphStart,out.length,FLAGS)}
            }
            alignment?.let {out.setSpan(AlignmentSpan.Standard(when(it){"center"->Layout.Alignment.ALIGN_CENTER;"end"->Layout.Alignment.ALIGN_OPPOSITE;else->Layout.Alignment.ALIGN_NORMAL}),paragraphStart,out.length,FLAGS)}
            out.getSpans(paragraphStart,(paragraphStart+1).coerceAtMost(out.length),NoteList::class.java).firstOrNull {it.kind=="check"&&it.checked}?.let {if(paragraphStart+2<out.length)out.setSpan(NoteChecklistDone(),paragraphStart+2,out.length,FLAGS)}
            heading=0;alignment=null
        }
        while(i<raw.length) {
            map[i]=out.length
            if(lineStart) {
                paragraphStart=out.length;lineStart=false
                val align=Regex("<div align=\"(start|center|end)\">").find(raw,i)?.takeIf {it.range.first==i}
                if(align!=null) {alignment=align.groupValues[1];mark(align.range.last+1)}
                val block=Regex("(#{1,3} |(?:- \\[[ xX]\\] |- |[0-9]+\\. ))").find(raw,i)?.takeIf {it.range.first==i}
                if(block!=null) {
                    val value=block.value
                    if(value.startsWith("#")) heading=value.count {it=='#'}
                    else {
                        val list=when {value.contains("[")->NoteList("check",checked=!value.contains("[ ]"));value.first().isDigit()->NoteList("number",value.substringBefore('.').toIntOrNull() ?: 1);else->NoteList("bullet")}
                        val start=out.length;out.append('\uFFFC');out.setSpan(list,start,start+1,FLAGS);out.append(' ')
                    }
                    mark(block.range.last+1)
                }
            }
            if(i>=raw.length) break
            val token=Regex(" ?<!--task:([A-Za-z0-9_-]+)-->").find(raw,i)?.takeIf {it.range.first==i}
            if(token!=null) {out.setSpan(NoteTaskToken(token.groupValues[1]),paragraphStart,out.length,FLAGS);mark(token.range.last+1);continue}
            if(raw[i]=='\n') {paragraphEnd();out.append('\n');i++;map[i]=out.length;lineStart=true;continue}
            if(raw.startsWith("</div>",i)) {mark(i+6);continue}
            val tag=Regex("<(b|i|u|s)>|</(b|i|u|s|font|mark|a)>|<font color=\"(#[0-9a-fA-F]{6})\">|<mark color=\"(#[0-9a-fA-F]{6})\">|<a href=\"([^\"]*)\">").find(raw,i)?.takeIf {it.range.first==i}
            if(tag!=null) {
                val closing=tag.groupValues[2].isNotEmpty()
                val key=tag.groupValues[1].ifEmpty {tag.groupValues[2]}.ifEmpty {when {tag.groupValues[3].isNotEmpty()->"font";tag.groupValues[4].isNotEmpty()->"mark";else->"a"}}
                toggle(key, {when(key){"b"->StyleSpan(Typeface.BOLD);"i"->StyleSpan(Typeface.ITALIC);"u"->UnderlineSpan();"s"->StrikethroughSpan();"font"->ForegroundColorSpan(color(tag.groupValues[3]));"mark"->BackgroundColorSpan(color(tag.groupValues[4]));else->URLSpan(unescape(tag.groupValues[5]))}},closing)
                mark(tag.range.last+1);continue
            }
            val markdown=when {raw.startsWith("**",i)->"**";raw[i]=='*'->"*";else->null}
            if(markdown!=null && (open.any {it.key==markdown}||raw.indexOf(markdown,i+markdown.length)>=0)) {
                toggle(markdown,{StyleSpan(if(markdown=="**")Typeface.BOLD else Typeface.ITALIC)});mark(i+markdown.length);continue
            }
            val link=if(raw[i]=='[') Regex("\\[([^\\n]*?)\\]\\((https?://[^\\s)]*|mailto:[^\\s)]*)\\)").find(raw,i)?.takeIf {it.range.first==i} else null
            if(link!=null) {
                val start=out.length;val label=link.groupValues[1];mark(i+1)
                label.forEach {map[i]=out.length;out.append(it);i++}
                out.setSpan(URLSpan(link.groupValues[2]),start,out.length,FLAGS);mark(link.range.last+1);continue
            }
            if(raw[i]=='&') {
                val entity=Regex("&(?:amp|lt|gt|quot|#[0-9]+);").find(raw,i)?.takeIf {it.range.first==i}
                if(entity!=null) {val decoded=unescape(entity.value);mark(entity.range.last+1);out.append(decoded);map[i]=out.length;continue}
            }
            out.append(raw[i]);i++;map[i]=out.length
        }
        paragraphEnd();open.forEach {out.setSpan(it.span,it.start,out.length,FLAGS)};normalize(out,ForegroundColorSpan::class.java){ForegroundColorSpan(it.foregroundColor)};normalize(out,BackgroundColorSpan::class.java){BackgroundColorSpan(it.backgroundColor)};emptySpans.forEach {(span,at)->out.setSpan(span,at,at,Spanned.SPAN_INCLUSIVE_INCLUSIVE)};map[raw.length]=out.length
        return ParsedRichNote(out,map)
    }
    private fun <T:CharacterStyle> normalize(text:SpannableStringBuilder,type:Class<T>,copy:(T)->T) {
        val spans=text.getSpans(0,text.length,type).map {Triple(text.getSpanStart(it),text.getSpanEnd(it),it)}
        val cuts=spans.flatMap {listOf(it.first,it.second)}.distinct().sorted()
        spans.forEach {text.removeSpan(it.third)}
        cuts.zipWithNext().forEach {(start,end)->spans.filter {it.first<=start&&it.second>=end}.minByOrNull {it.second-it.first}?.let {if(end>start)text.setSpan(copy(it.third),start,end,FLAGS)}}
    }
    private fun color(hex:String):Int=(0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()
    private fun hex(color:Int)="#%06X".format(java.util.Locale.ROOT,color and 0xFFFFFF)
    private fun unescape(s:String)=s.replace("&quot;","\"").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&").replace(Regex("&#([0-9]+);")){it.groupValues[1].toIntOrNull()?.takeIf {n->n in 0..65535}?.toChar()?.toString() ?: it.value}
    private fun escape(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;")
    private data class Tag(val key:String,val start:String,val end:String)
    private fun tags(text:Spanned,index:Int):List<Tag> {
        val spans=text.getSpans(index,index+1,Any::class.java).filter {text.getSpanStart(it)<=index&&text.getSpanEnd(it)>index}.filterNot {it is NoteHeadingBold||it is NoteChecklistDone}.map {if(it is CharacterStyle)it.underlying else it}
        val result=mutableListOf<Tag>()
        if(spans.filterIsInstance<StyleSpan>().any {it.style and Typeface.BOLD!=0})result+=Tag("b","<b>","</b>")
        if(spans.filterIsInstance<StyleSpan>().any {it.style and Typeface.ITALIC!=0})result+=Tag("i","<i>","</i>")
        if(spans.any {it is UnderlineSpan})result+=Tag("u","<u>","</u>")
        if(spans.any {it is StrikethroughSpan})result+=Tag("s","<s>","</s>")
        spans.filterIsInstance<ForegroundColorSpan>().lastOrNull()?.let {result+=Tag("font:${it.foregroundColor}","<font color=\"${hex(it.foregroundColor)}\">","</font>")}
        spans.filterIsInstance<BackgroundColorSpan>().lastOrNull()?.let {result+=Tag("mark:${it.backgroundColor}","<mark color=\"${hex(it.backgroundColor)}\">","</mark>")}
        spans.filterIsInstance<URLSpan>().lastOrNull()?.takeIf {safeLink(it.url)}?.let {result+=Tag("a:${it.url}","<a href=\"${escape(it.url)}\">","</a>")}
        return result
    }
    private fun copyStyle(s:CharacterStyle):CharacterStyle=when(val span=s.underlying) {
        is StyleSpan->StyleSpan(span.style);is UnderlineSpan->UnderlineSpan();is StrikethroughSpan->StrikethroughSpan()
        is ForegroundColorSpan->ForegroundColorSpan(span.foregroundColor);is BackgroundColorSpan->BackgroundColorSpan(span.backgroundColor);is URLSpan->URLSpan(span.url)
        else->CharacterStyle.wrap(s)
    }
    fun safeLink(url:String)=url.startsWith("https://",true)||url.startsWith("http://",true)||url.startsWith("mailto:",true)
    fun encode(text:Spanned):String=buildString {
        var start=0
        while(start<=text.length) {
            val end=text.toString().indexOf('\n',start).takeIf {it>=0} ?: text.length
            val block=text.getSpans(start,end,NoteHeading::class.java).firstOrNull {text.getSpanStart(it)<=start&&text.getSpanEnd(it)>=end}
            val align=text.getSpans(start,end,AlignmentSpan::class.java).lastOrNull()?.alignment
            if(align!=null)append("<div align=\"${when(align){Layout.Alignment.ALIGN_CENTER->"center";Layout.Alignment.ALIGN_OPPOSITE->"end";else->"start"}}\">")
            if(block!=null)append("#".repeat(block.level)+" ")
            val list=text.getSpans(start,(start+1).coerceAtMost(end),NoteList::class.java).firstOrNull {text.getSpanStart(it)==start}
            var content=start
            if(list!=null && start<end && text[start]=='\uFFFC') {
                append(when(list.kind){"check"->if(list.checked)"- [x] " else "- [ ] ";"number"->"${list.number}. ";else->"- "})
                content++;if(content<end&&text[content]==' ')content++
            }
            var active=emptyList<Tag>()
            for(index in content until end) {
                val next=tags(text,index);var common=0
                while(common<active.size&&common<next.size&&active[common].key==next[common].key)common++
                active.drop(common).asReversed().forEach {append(it.end)};next.drop(common).forEach {append(it.start)};active=next
                append(when(val c=text[index]){'*'->"&#42;";'['->"&#91;";'#'->"&#35;";else->escape(c.toString())})
            }
            active.asReversed().forEach {append(it.end)}
            text.getSpans(start,end,NoteTaskToken::class.java).distinctBy {it.id}.forEach {if(text.getSpanStart(it)>=start&&text.getSpanStart(it)<=end)append(" <!--task:${it.id}-->")}
            if(align!=null)append("</div>")
            if(end==text.length)break
            append('\n');start=end+1
        }
    }
    fun value(text:Spanned,start:Int,end:Int=start):TextFieldValue {
        val raw=encode(text);val parsed=parse(raw)
        return TextFieldValue(raw,TextRange(parsed.source(start),parsed.source(end)))
    }
    fun format(value:TextFieldValue,prefix:String,suffix:String):TextFieldValue {
        val parsed=parse(value.text);val text=parsed.text
        val start=parsed.visible(value.selection.min);val end=parsed.visible(value.selection.max)
        if(prefix=="\n---\n") {text.insert(start,"\n—\n");return NeoRichTextCodec.value(text,start+3,end+3)}
        if(prefix in listOf("# ","## ","### ","\n- ","\n1. ","\n- [ ] ") || prefix.startsWith("<align:")) {
            val first=text.toString().lastIndexOf('\n',start-1)+1
            val last=text.toString().indexOf('\n',end).takeIf {it>=0} ?: text.length
            val count=text.toString().substring(first,last).count {it=='\n'}+1
            val originalLength=text.length
            var line=first;var ordinal=1
            repeat(count) {
                var stop=text.toString().indexOf('\n',line).takeIf {it>=0} ?: text.length
                if(prefix.startsWith("<align:")) {
                    text.getSpans(line,stop,AlignmentSpan::class.java).forEach {text.removeSpan(it)}
                    text.setSpan(AlignmentSpan.Standard(when(prefix){"<align:center>"->Layout.Alignment.ALIGN_CENTER;"<align:end>"->Layout.Alignment.ALIGN_OPPOSITE;else->Layout.Alignment.ALIGN_NORMAL}),line,stop,FLAGS)
                } else {
                    val previousHeading=text.getSpans(line,stop,NoteHeading::class.java).firstOrNull()?.level
                    text.getSpans(line,stop,NoteHeading::class.java).forEach {text.removeSpan(it)}
                    text.getSpans(line,stop,NoteHeadingBold::class.java).forEach {text.removeSpan(it)}
                    text.getSpans(line,stop,RelativeSizeSpan::class.java).forEach {text.removeSpan(it)}
                    val old=text.getSpans(line,(line+1).coerceAtMost(stop),NoteList::class.java).firstOrNull {text.getSpanStart(it)==line}
                    if(old!=null && line<text.length&&text[line]=='\uFFFC') {text.removeSpan(old);val remove=if(line+1<text.length&&text[line+1]==' ')2 else 1;text.delete(line,line+remove);stop-=remove}
                    if(prefix.startsWith("#")) {
                        val level=prefix.count {it=='#'};if(previousHeading!=level)text.setSpan(NoteHeading(level),line,stop,if(line==stop)Spanned.SPAN_INCLUSIVE_INCLUSIVE else FLAGS)
                    } else {
                        val list=when(prefix){"\n1. "->NoteList("number",ordinal++);"\n- [ ] "->NoteList("check");else->NoteList("bullet")}
                        text.insert(line,"\uFFFC ");text.setSpan(list,line,line+1,FLAGS);stop+=2
                    }
                }
                line=stop+1
            }
            val raw=encode(text);return TextFieldValue(raw,TextRange(parse(raw).source(first),parse(raw).source((end+text.length-originalLength).coerceIn(0,text.length))))
        }
        if(start==end) return value
        val span:Any=when {
            prefix=="**"->StyleSpan(Typeface.BOLD);prefix=="*"->StyleSpan(Typeface.ITALIC)
            prefix=="<u>"->UnderlineSpan();prefix=="<s>"->StrikethroughSpan()
            prefix.startsWith("<font color=")->ForegroundColorSpan(color(prefix.substringAfter('"').substringBefore('"')))
            prefix.startsWith("<mark color=")->BackgroundColorSpan(color(prefix.substringAfter('"').substringBefore('"')))
            prefix=="["->URLSpan(suffix.substringAfter("](").removeSuffix(")"))
            else->return value
        }
        // Split same-property spans at the selection boundaries instead of nesting conflicting tags.
        val matching=text.getSpans(start,end,CharacterStyle::class.java).filter {old->when(span){is StyleSpan->old is StyleSpan&&old.style==span.style;else->old.javaClass==span.javaClass}}
        val toggleOff=(span is StyleSpan||span is UnderlineSpan||span is StrikethroughSpan) && (start until end).all {index->matching.any {text.getSpanStart(it)<=index&&text.getSpanEnd(it)>index}}
        matching.forEach {old->val a=text.getSpanStart(old);val z=text.getSpanEnd(old);text.removeSpan(old);if(a<start)text.setSpan(copyStyle(old),a,start,FLAGS);if(z>end)text.setSpan(copyStyle(old),end,z,FLAGS)}
        if(!toggleOff)text.setSpan(span,start,end,FLAGS)
        return NeoRichTextCodec.value(text,start,end)
    }
}
