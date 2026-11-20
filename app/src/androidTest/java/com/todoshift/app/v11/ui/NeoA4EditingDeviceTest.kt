package com.todoshift.app.v11.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android A4 editing regression, including Compose focus and source saving. */
@RunWith(AndroidJUnit4::class)
class NeoA4EditingDeviceTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()

    @Test fun editingOnSecondSheetPreservesWholeOriginalParagraph() {
        val original=("Arabic كلمة repeated paragraph. ").repeat(150)
        var source by mutableStateOf(original)
        var error:String?=null
        rule.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp).height(620.dp)
                    .verticalScroll(rememberScrollState())) {
                    NeoDocumentCanvas(
                        noteId="native-a4-long-paragraph",
                        source=source,editable=true,a4=true,inlineEditing=true,
                        onSourceChange={source=it},onError={error=it},
                        onParagraphEdited={line,edited ->
                            source=NeoDocumentBlocks.replaceParagraphAt(source,line,edited)
                        }
                    )
                }
            }
        }
        rule.waitUntil(20_000) {
            rule.onAllNodesWithTag("a4_note_slice_0_1",useUnmergedTree=true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("a4_note_slice_0_1",useUnmergedTree=true)
            .performScrollTo().performTextInput(" اختبار")
        rule.waitForIdle()
        assertNull("Saving text across A4 sheets failed",error)
        assertTrue(source.contains(" اختبار"))
        assertEquals("A4 edits must not lose or duplicate any original characters",
            original,source.replaceFirst(" اختبار",""))
        assertEquals(original.length+" اختبار".length,source.length)
        assertTrue(!source.contains(NeoDocumentBlocks.PAGE_BREAK))
    }
}
