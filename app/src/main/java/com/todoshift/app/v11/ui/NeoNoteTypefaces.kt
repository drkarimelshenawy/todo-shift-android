package com.todoshift.app.v11.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Font-only visual transformation; original text, spans, direction, IME
 * composition, selection and source offsets remain unchanged.
 *
 * System font availability varies by Android device. Unknown glyphs naturally
 * fall back to the device's default Arabic/Latin font.
 */
internal object NeoNoteTypefaces {
    val choices=listOf("system","sans","serif","mono")

    fun family(key:String):FontFamily=when(key) {
        "sans" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    private fun script(c:Char):Int = when(Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.ARABIC -> 1
        Character.UnicodeScript.LATIN -> 2
        else -> 0
    }

    fun styled(source:AnnotatedString,arabic:String,english:String):AnnotatedString {
        if(arabic=="system" && english=="system") return source
        return buildAnnotatedString {
            append(source)
            var active=0
            var start=0
            fun finish(end:Int) {
                if(active==1 && arabic!="system")
                    addStyle(SpanStyle(fontFamily=family(arabic)),start,end)
                if(active==2 && english!="system")
                    addStyle(SpanStyle(fontFamily=family(english)),start,end)
            }
            source.text.forEachIndexed {index,char ->
                val current=script(char)
                // Leave numbers, whitespace and combining marks with their
                // surrounding script for natural mixed Arabic/English layout.
                if(current!=0 && current!=active) {
                    finish(index)
                    active=current
                    start=index
                }
            }
            finish(source.length)
        }
    }

    fun visual(arabic:String,english:String)=VisualTransformation {source ->
        TransformedText(styled(source,arabic,english),OffsetMapping.Identity)
    }
}
