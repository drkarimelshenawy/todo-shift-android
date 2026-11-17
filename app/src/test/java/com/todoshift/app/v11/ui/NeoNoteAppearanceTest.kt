package com.todoshift.app.v11.ui

import android.content.Context
import androidx.compose.ui.text.AnnotatedString
import com.todoshift.app.v11.NeoLaunchPreferences
import com.todoshift.app.v11.NeoNotePresentation
import com.todoshift.app.v11.NeoSmartStore
import com.todoshift.app.v11.V11Runtime
import com.todoshift.app.v11.DayItemV11
import com.todoshift.app.v11.DayItemTypeV11
import com.todoshift.app.backup.Neo4LocalBackup
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class NeoNoteAppearanceTest {
    private val context:Context get()=RuntimeEnvironment.getApplication()
    @Before fun reset() {
        context.getSharedPreferences(NeoSmartStore.NAME,Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("todo_shift_launch_v44",Context.MODE_PRIVATE)
            .edit().clear().commit()
    }
    @Test fun appDefaultsAndPerNoteOverridesDoNotMutateNoteText() {
        val prefs=NeoLaunchPreferences(context)
        assertTrue(prefs.defaultNoteRuled)
        prefs.defaultNoteRuled=false
        prefs.defaultArabicNoteFont="serif"
        prefs.defaultEnglishNoteFont="mono"
        assertFalse(NeoLaunchPreferences(context).defaultNoteRuled)
        assertEquals("serif",prefs.defaultArabicNoteFont)
        assertEquals("mono",prefs.defaultEnglishNoteFont)
        val store=NeoSmartStore(context)
        assertEquals(NeoNotePresentation(),store.notePresentation("note-1"))
        val original=V11Runtime(context).start()
        val note=DayItemV11("appearance-note","2026-11-17",
            type=DayItemTypeV11.NOTE,title="عنوان",details="عربي English")
        val saved=Neo4LocalBackup(context).saveNote(
            original,store.snapshot().toString(),note,"#112233",false,
            presentation=NeoNotePresentation(true,"serif","mono"))
        assertEquals(note.details,saved.dayItems.first{it.id==note.id}.details)
        val presentation=NeoSmartStore(context).notePresentation(note.id)
        assertEquals(true,presentation.ruled)
        assertEquals("serif",presentation.arabicFont)
        assertEquals("mono",presentation.englishFont)
    }

    @Test fun invalidFontIsRejectedByMetadataValidation() {
        val invalid=JSONObject().put("version",1).put(
            "notePresentations",JSONObject().put("id",JSONObject().put("ar","invalid"))
        )
        try {
            NeoSmartStore.validate(invalid)
            fail("Unrecognized font must be rejected")
        } catch(_:IllegalArgumentException) {}
        assertTrue(NeoNotePresentation.FONT_KEYS.containsAll(
            setOf("system","sans","serif","mono","amiri","tajawal",
                "reem","montserrat","lora","oswald")))
    }

    @Test fun bilingualFontsPreserveIdentityOffsetsAndExistingMarkupSpans() {
        val source=AnnotatedString("عنوان Arabic text")
        val result=NeoNoteTypefaces.visual("serif","mono").filter(source)
        assertEquals(source.text,result.text.text)
        for(index in 0..source.length) {
            assertEquals(index,result.offsetMapping.originalToTransformed(index))
            assertEquals(index,result.offsetMapping.transformedToOriginal(index))
        }
        assertTrue(result.text.spanStyles.size>=2)
        assertEquals(source,NeoNoteTypefaces.styled(source,"system","system"))
    }
}
