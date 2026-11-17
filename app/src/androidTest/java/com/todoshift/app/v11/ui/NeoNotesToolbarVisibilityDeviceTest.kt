package com.todoshift.app.v11.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import android.view.KeyEvent
import kotlin.math.roundToInt
import com.todoshift.app.v11.V11Database
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regression against the actual full-screen Note Dialog (not a mock toolbar). */
@RunWith(AndroidJUnit4::class)
class NeoNotesToolbarVisibilityDeviceTest {
    @get:Rule val rule=createAndroidComposeRule<ComponentActivity>()

    private fun screenshot(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val dir=File(context.getExternalFilesDir(null),"notes-toolbar").apply {mkdirs()}
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        val bmp=automation.takeScreenshot()
        assertNotNull("Screen capture unavailable",bmp)
        FileOutputStream(File(dir,name)).use {
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        // Shell-owned screenshot survives uninstall after a failed test.
        val destination="/sdcard/Download/notes-toolbar-"+name
        automation.executeShellCommand("screencap -p "+destination).close()
        android.util.Log.i("NotesToolbarDeviceTest","screenshot="+destination)
    }

    @Test fun toolbarVisibleWhenKeyboardClosedAndOpen() {
        rule.setContent {
            MaterialTheme {
                NeoDayNoteDialog(
                    date=LocalDate.of(2026,11,17),
                    existing=null,
                    expectedDatabase=V11Database(),
                    calendarId=null,
                    onDismiss={},
                    onSave={}
                )
            }
        }
        rule.waitForIdle()
        screenshot("01-closed.png")
        run {
        val toolbar=rule.onNodeWithTag("notes_quick_bar")
        val semantics=toolbar.fetchSemanticsNode()
        val metrics=rule.activity.resources.displayMetrics
        val diagnostic="NOTE_TOOLBAR_BOUNDS root="+semantics.boundsInRoot+
            ", window="+semantics.boundsInWindow+
            ", display="+metrics.widthPixels+"x"+metrics.heightPixels
        android.util.Log.e("NotesToolbarDeviceTest",diagnostic)
        try {
            toolbar.assertIsDisplayed()
        } catch(failure:AssertionError) {
            throw AssertionError(diagnostic+" -- "+failure.message,failure)
        }
        }
        val closedBottom=rule.onNodeWithTag("notes_quick_bar")
            .fetchSemanticsNode().boundsInWindow.bottom
        // Compose performTextInput injects through test APIs and may leave the
        // actual Android IME hidden. Use genuine device taps instead.
        val field=rule.onNodeWithTag("notes_body_input")
            .fetchSemanticsNode().boundsInWindow
        val x=(field.right-65f).roundToInt()
        val y=(field.top+45f).roundToInt()
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue("Real input tap failed",device.click(x,y))
        rule.waitForIdle()
        device.pressKeyCode(KeyEvent.KEYCODE_A)
        // Text entry alone does not prove that the software keyboard opened.
        // Require the real toolbar to rise above the newly shown IME.
        val moved=runCatching {
            rule.waitUntil(12_000) {
                rule.onNodeWithTag("notes_quick_bar")
                    .fetchSemanticsNode().boundsInWindow.bottom < closedBottom-100f
            }
        }.isSuccess
        rule.waitForIdle()
        screenshot("02-open.png")
        val openBottom=rule.onNodeWithTag("notes_quick_bar")
            .fetchSemanticsNode().boundsInWindow.bottom
        assertTrue("Software IME never moved the toolbar: closedBottom="+closedBottom+
            ", openBottom="+openBottom, moved)
        rule.onNodeWithTag("notes_quick_bar").assertIsDisplayed()
    }
}
