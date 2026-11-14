package com.todoshift.app.v11

/** A copied checklist label inherits its source note's protection across every representation. */
fun TaskV11.linkedNotePrivateV11(database: V11Database): Boolean =
    linkedNoteId?.let { source -> database.dayItems.firstOrNull {it.id==source&&it.type==DayItemTypeV11.NOTE}?.notePrivate != false } ?: false
