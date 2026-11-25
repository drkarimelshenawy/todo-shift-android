package com.exmore.app.v11.ui

/** Source-aware editor tools. No document transformation is persisted by searching. */
internal object NeoNoteDocumentTools {
    data class Heading(val level:Int,val title:String,val start:Int)
    data class Stats(val words:Int,val characters:Int,val lines:Int,val paragraphs:Int)
    data class SearchMatch(val start:Int,val end:Int)

    fun visible(source:String):String=NeoRichEditorState.open(source).value.text

    fun stats(visible:String):Stats {
        val trimmed=visible.trim()
        val words=Regex("""[\p{L}\p{N}]+(?:['’_-][\p{L}\p{N}]+)*""")
            .findAll(visible).count()
        return Stats(words,visible.length,visible.count{it=='\n'}+1,
            if(trimmed.isEmpty()) 0 else visible.split(Regex("""\n\s*\n"""))
                .count{it.isNotBlank()})
    }

    fun headings(source:String):List<Heading> {
        val result=mutableListOf<Heading>()
        // H1/H2/H3 prefixes are hidden during WYSIWYG editing. Discover them
        // from the saved source, then map locations to visible caret offsets.
        val mapped=NeoNoteVisualTransformation().filter(
            androidx.compose.ui.text.AnnotatedString(source))
        var offset=0
        source.split('\n').forEach {line ->
            val match=Regex("""^\s{0,3}(#{1,3})\s+(.+)$""").find(line)
            if(match!=null) {
                result+=Heading(match.groupValues[1].length,
                    NeoRichEditorState.open(line).value.text.trim(),
                    mapped.offsetMapping.originalToTransformed(offset))
            }
            offset+=line.length+1
        }
        return result
    }

    fun find(visible:String,query:String):List<SearchMatch> {
        val term=query.trim()
        if(term.isEmpty() || term.length>128) return emptyList()
        val matches=mutableListOf<SearchMatch>()
        var start=0
        while(start<visible.length && matches.size<500) {
            val hit=visible.indexOf(term,start,ignoreCase=true)
            if(hit<0) break
            matches+=SearchMatch(hit,hit+term.length)
            start=hit+term.length
        }
        return matches
    }
}
