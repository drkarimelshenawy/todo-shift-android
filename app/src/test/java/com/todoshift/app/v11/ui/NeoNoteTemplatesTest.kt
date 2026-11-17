package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

class NeoNoteTemplatesTest {
    @Test fun templatesAreOneRegularRichNoteFormatWithUniqueIdentity() {
        val templates=NeoNoteTemplates.builtIn
        assertEquals(6,templates.size)
        assertEquals(templates.size,templates.map {it.id}.toSet().size)
        for(template in templates) {
            assertTrue(template.titleAr.isNotBlank())
            assertTrue(template.titleEn.isNotBlank())
            assertTrue(template.bodyAr.startsWith("# "))
            assertTrue(template.bodyEn.startsWith("# "))
            assertFalse(template.bodyAr.contains("[ ]"))
            assertFalse(template.bodyEn.contains("[ ]"))
            assertEquals(template,NeoNoteTemplates.byId(template.id))
            assertEquals(template.bodyAr,template.body(true))
            assertEquals(template.bodyEn,template.body(false))
            assertEquals(template.titleAr,template.title(true))
        }
        assertNull(NeoNoteTemplates.byId("unknown"))
    }
}
