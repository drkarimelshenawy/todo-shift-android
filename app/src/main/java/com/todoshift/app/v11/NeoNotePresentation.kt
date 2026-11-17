package com.todoshift.app.v11

/**
 * Per-note visual preferences. Null means follow the current app default.
 * Layout settings are separate from note content and never rewrite text.
 */
data class NeoNotePresentation(
    val ruled: Boolean? = null,
    val arabicFont: String? = null,
    val englishFont: String? = null
) {
    companion object {
        val FONT_KEYS = setOf("system", "sans", "serif", "mono")
        fun validFont(value: String?):Boolean = value==null || value in FONT_KEYS
    }
}
