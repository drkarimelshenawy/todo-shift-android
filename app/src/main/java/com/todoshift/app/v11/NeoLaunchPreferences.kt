package com.todoshift.app.v11

import android.content.Context

/** Destination preferences never change record IDs or shortcut requests. */
class NeoLaunchPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("todo_shift_launch_v44", Context.MODE_PRIVATE)
    var landingTab: Int
        get() = prefs.getInt("landing", 0).takeIf { it in 0..4 } ?: 0
        set(value) { require(value in 0..4); check(prefs.edit().putInt("landing", value).commit()) }
    var defaultNoteBackground:String
        get()=prefs.getString("note_background","").orEmpty()
        set(value) {
            require(value.isBlank() || value.matches(Regex("#[0-9a-fA-F]{6}")))
            // Freeze the old shared background as the card default on first edit.
            val edit=prefs.edit()
            if(!prefs.contains("note_card_background"))
                edit.putString("note_card_background",prefs.getString("note_background",""))
            check(edit.putString("note_background",value).commit())
        }
    var defaultNoteCardBackground:String
        get()=prefs.getString("note_card_background",
            prefs.getString("note_background","")).orEmpty()
        set(value) {require(value.isBlank() || value.matches(Regex("#[0-9a-fA-F]{6}")))
            check(prefs.edit().putString("note_card_background",value).commit())}
    var defaultNoteRuled:Boolean
        get()=prefs.getBoolean("note_ruled",true)
        set(value) {check(prefs.edit().putBoolean("note_ruled",value).commit())}

    var defaultArabicNoteFont:String
        get()=prefs.getString("note_font_ar","system").orEmpty()
            .takeIf {it in NeoNotePresentation.FONT_KEYS} ?: "system"
        set(value) {
            require(value in NeoNotePresentation.FONT_KEYS)
            check(prefs.edit().putString("note_font_ar",value).commit())
        }

    var defaultEnglishNoteFont:String
        get()=prefs.getString("note_font_en","system").orEmpty()
            .takeIf {it in NeoNotePresentation.FONT_KEYS} ?: "system"
        set(value) {
            require(value in NeoNotePresentation.FONT_KEYS)
            check(prefs.edit().putString("note_font_en",value).commit())
        }

    /** 0 means keep notes in Trash until manually deleted. */
    var noteTrashRetentionDays: Int
        get() = prefs.getInt("note_trash_days", 30).takeIf { it in setOf(0,7,15,30,60,90) } ?: 30
        set(value) { require(value in setOf(0,7,15,30,60,90)); check(prefs.edit().putInt("note_trash_days",value).commit()) }

    var openNotes: Boolean
        get() = prefs.getBoolean("notes", false)
        set(value) { check(prefs.edit().putBoolean("notes", value).commit()) }
}
