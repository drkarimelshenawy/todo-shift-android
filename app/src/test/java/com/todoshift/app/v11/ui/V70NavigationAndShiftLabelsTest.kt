package com.todoshift.app.v11.ui

import android.content.Intent
import com.todoshift.app.v11.ShiftKind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class V70NavigationAndShiftLabelsTest {
    @Test fun legacyShiftNamesMatchApprovedArabicAndEnglishSpellings() {
        val kinds = listOf(ShiftKind.FULL_DAY, ShiftKind.LONG, ShiftKind.NIGHT,
            ShiftKind.MORNING, ShiftKind.EVENING)
        try {
            NeoLegacyLocale.english = false
            assertEquals(listOf("24h", "لونج", "نايت", "مورننج", "إيفننج"),
                kinds.map { neoShiftText(it) })
            NeoLegacyLocale.english = true
            assertEquals(listOf("24h", "Long", "Night", "Morning", "Evening"),
                kinds.map { neoShiftText(it) })
        } finally {
            NeoLegacyLocale.english = false
        }
    }

    @Test fun preservedSalaryWidgetOpensNestedFinanceInsteadOfNotes() {
        val routed = NeoEntry.parse(
            Intent().putExtra("neo4_open_tab", 1)
                .putExtra("todo_shift_finance_section", "salary"),
            10L
        )
        assertEquals(1, routed.tab)
        assertEquals("salary", routed.financeSection)
    }

    @Test fun taskAndNoteShortcutActionsStayDistinct() {
        val note = NeoEntry.parse(Intent().putExtra("todo_shift_task_action", "notes"), 11L)
        val task = NeoEntry.parse(Intent().putExtra("todo_shift_task_action", "tasks"), 12L)
        assertEquals(2, note.tab)
        assertEquals(4, task.tab)
    }
}
