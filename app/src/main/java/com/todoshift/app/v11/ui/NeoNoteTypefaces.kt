package com.todoshift.app.v11.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import com.todoshift.app.R
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
    val arabicChoices=listOf("system","amiri","tajawal","reem")
    val englishChoices=listOf("system","montserrat","lora","oswald")
    // Legacy keys remain valid and readable for existing notes.
    val choices=listOf("system","sans","serif","mono")+
        arabicChoices.drop(1)+englishChoices.drop(1)

    private val amiri=FontFamily(
        Font(R.font.note_ar_amiri,FontWeight.Normal),
        Font(R.font.note_ar_amiri_bold,FontWeight.Bold))
    private val tajawal=FontFamily(
        Font(R.font.note_ar_tajawal,FontWeight.Normal),
        Font(R.font.note_ar_tajawal_bold,FontWeight.Bold))
    private val reem=FontFamily(Font(R.font.note_ar_reem_kufi))
    private val montserrat=FontFamily(Font(R.font.note_en_montserrat))
    private val lora=FontFamily(Font(R.font.note_en_lora))
    private val oswald=FontFamily(Font(R.font.note_en_oswald))

    fun family(key:String):FontFamily=when(key) {
        "amiri" -> amiri
        "tajawal" -> tajawal
        "reem" -> reem
        "montserrat" -> montserrat
        "lora" -> lora
        "oswald" -> oswald
        "sans" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    fun keyOf(font:FontFamily?):String?=when(font) {
        amiri -> "amiri"
        tajawal -> "tajawal"
        reem -> "reem"
        montserrat -> "montserrat"
        lora -> "lora"
        oswald -> "oswald"
        FontFamily.SansSerif -> "sans"
        FontFamily.Serif -> "serif"
        FontFamily.Monospace -> "mono"
        else -> null
    }

    private fun script(c:Char):Int = when(Character.UnicodeScript.of(c.code)) {
        Character.UnicodeScript.ARABIC -> 1
        Character.UnicodeScript.LATIN -> 2
        else -> 0
    }

    fun styled(source:AnnotatedString,arabic:String,english:String):AnnotatedString {
        if(arabic=="system" && english=="system") return source
        val explicit=BooleanArray(source.length)
        source.spanStyles.filter{it.item.fontFamily!=null}.forEach {range ->
            for(index in range.start.coerceAtLeast(0) until
                range.end.coerceAtMost(explicit.size)) explicit[index]=true
        }
        return buildAnnotatedString {
            append(source)
            var active=0
            var start=0
            fun finish(end:Int) {
                if(start>=end)return
                if(active==1 && arabic!="system")
                    addStyle(SpanStyle(fontFamily=family(arabic)),start,end)
                if(active==2 && english!="system")
                    addStyle(SpanStyle(fontFamily=family(english)),start,end)
            }
            source.text.forEachIndexed {index,char ->
                val current=if(explicit[index]) 0 else script(char)
                // Explicit per-range font always wins over note-level defaults.
                // Preserve punctuation and combining marks with their runs.
                val next=if(explicit[index])0 else if(current!=0)current else active
                if(next!=active) {
                    finish(index)
                    active=next
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
