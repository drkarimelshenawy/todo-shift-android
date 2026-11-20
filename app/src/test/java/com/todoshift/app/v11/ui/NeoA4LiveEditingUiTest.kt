package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS-w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class NeoA4LiveEditingUiTest {
    @get:Rule val compose=createComposeRule()

    @Test fun secondPageEditsSameOriginalParagraphWithoutSyntheticPageBreaks() {
        val initial=("Arabic كلمة repeated paragraph. ").repeat(150)
        var source by mutableStateOf(initial)
        var failure:String?=null
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp).height(600.dp)
                    .verticalScroll(rememberScrollState())) {
                    NeoDocumentCanvas(
                        noteId="a4-multipage-edit-regression",
                        source=source,editable=true,
                        onSourceChange={source=it},onError={failure=it},
                        a4=true,inlineEditing=true,
                        onParagraphEdited={startLine,updated ->
                            source=NeoDocumentBlocks.replaceParagraphAt(
                                source,startLine,updated)
                        }
                    )
                }
            }
        }
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("a4_note_slice_0_1",useUnmergedTree=true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("a4_note_slice_0_1",useUnmergedTree=true)
            .performScrollTo()
            .performTextInput(" اختبار")
        compose.waitForIdle()
        assertNull(failure)
        assertTrue(source.contains("اختبار"))
        // Recomposition, focus handoff and autosave must not duplicate, drop
        // or reorder a single character of the original long paragraph.
        assertEquals(initial.length+" اختبار".length,source.length)
        assertEquals(initial,source.replaceFirst(" اختبار",""))
        assertTrue("Pagination must not insert serialized page breaks",
            !source.contains(NeoDocumentBlocks.PAGE_BREAK))
    }
}
