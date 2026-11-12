package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@LooperMode(LooperMode.Mode.PAUSED)
class PainterQueueUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun tapsDuringSlowCommitStayVisibleAndPersistInOrderWithoutModalSaving() {
        val date=LocalDate.parse("2026-11-12")
        var db by mutableStateOf(V11Database(calendars=listOf(AppCalendarV11("a","Work","#00A4B8"))))
        var request by mutableIntStateOf(0)
        var clicked by mutableStateOf(date)
        var preview: V11Database?=null
        val calls=mutableListOf<Pair<V11Database,(Boolean)->Unit>>()
        compose.setContent {
            NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH),true,{}) {
                CompositionLocalProvider(LocalNeoCommitRequest provides { next,done -> calls.add(next to done) }) {
                    NeoPainterBar(db,listOf(Workplace("w","Clinic","#00A4B8")),"a",emptySet(),clicked,request,false,{}, {}, {}, {}, { preview=it })
                }
            }
        }
        compose.onNodeWithContentDescription("Eraser").assertExists()
        compose.runOnIdle { request++ }
        compose.waitForIdle()
        assertEquals(compose.onRoot(useUnmergedTree=true).printToString(),1,calls.size)
        compose.runOnIdle { clicked=date.plusDays(1);request++ }
        compose.waitForIdle()
        assertEquals(compose.onRoot(useUnmergedTree=true).printToString(),2,preview?.days?.size)
        compose.onNodeWithText("جارٍ الحفظ… / Saving…").assertDoesNotExist()
        compose.runOnIdle { db=calls[0].first;calls[0].second(true) }
        compose.waitForIdle()
        assertEquals(compose.onRoot(useUnmergedTree=true).printToString(),2,calls.size)
        compose.runOnIdle { db=calls[1].first;calls[1].second(true) }
        compose.waitForIdle()
        assertNull(preview)
        assertEquals(2,db.days.size)
        assertEquals(2,db.days.flatMap { it.segments }.size)
    }
    @Test fun failedPaintingKeepsDraftAndRetryUsesSameSegmentIdentity() {
        val date=LocalDate.parse("2026-11-12")
        var db by mutableStateOf(V11Database(calendars=listOf(AppCalendarV11("a","Work","#00A4B8"))))
        var request by mutableIntStateOf(0)
        val calls=mutableListOf<Pair<V11Database,(Boolean)->Unit>>()
        compose.setContent {
            NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH),true,{}) {
                CompositionLocalProvider(LocalNeoCommitRequest provides { next,done -> calls.add(next to done) }) {
                    NeoPainterBar(db,listOf(Workplace("w","Clinic","#00A4B8")),"a",emptySet(),date,request,false,{}, {}, {}, {})
                }
            }
        }
        compose.runOnIdle { request++ };compose.waitForIdle()
        assertEquals(compose.onRoot(useUnmergedTree=true).printToString(),1,calls.size)
        compose.runOnIdle { calls[0].second(false) }
        compose.onNodeWithText("Retry").performClick()
        compose.waitForIdle()
        assertEquals(compose.onRoot(useUnmergedTree=true).printToString(),2,calls.size)
        assertEquals(calls[0].first,calls[1].first)
        assertTrue(db.days.isEmpty())
        compose.runOnIdle { db=calls[1].first;calls[1].second(true) }
    }
}
