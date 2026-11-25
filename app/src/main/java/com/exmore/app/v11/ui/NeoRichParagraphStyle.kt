package com.exmore.app.v11.ui

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp

/**
 * Line-level formatting is independent of character spans and editor direction.
 * A small, strictly validated trailing HTML comment keeps older notes, backups
 * and heading/list prefixes readable without any schema migration.
 */
internal data class NeoRichParagraphStyle(
    val align:String="auto",
    val indent:Int=0,
) {
    init {
        require(align in ALIGNMENTS)
        require(indent in 0..4)
    }
    fun portableTag():String? =
        if(align=="auto" && indent==0) null else "<!--neo-p:$align:$indent-->"

    fun asCompose():ParagraphStyle = ParagraphStyle(
        textAlign=when(align) {
            "left" -> TextAlign.Left
            "center" -> TextAlign.Center
            "right" -> TextAlign.Right
            "justify" -> TextAlign.Justify
            else -> TextAlign.Unspecified
        },
        textIndent=TextIndent(firstLine=(24*indent).sp,restLine=(24*indent).sp),
    )

    companion object {
        val ALIGNMENTS=setOf("auto","left","center","right","justify")
        val TOKEN=Regex("""<!--neo-p:(auto|left|center|right|justify):([0-4])-->""")
        fun fromSavedLine(source:String):NeoRichParagraphStyle =
            TOKEN.findAll(source).lastOrNull()?.let {
                NeoRichParagraphStyle(it.groupValues[1],it.groupValues[2].toInt())
            } ?: NeoRichParagraphStyle()
    }
}
