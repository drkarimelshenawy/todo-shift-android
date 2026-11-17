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
import com.todoshift.app.v11.V11Database
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import org.junit.Assert.assertNotNull
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
        val bmp=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        assertNotNull("Screen capture unavailable",bmp)
        FileOutputStream(File(dir,name)).use {
            bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
        android.util.Log.i("NotesToolbarDeviceTest","screenshot="+File(dir,name).absolutePath)
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
        rule.onNodeWithTag("notes_quick_bar").assertIsDisplayed()
        rule.onNodeWithTag("notes_body_input").performClick()
        rule.onNodeWithTag("notes_body_input").performTextInput("اختبار")
        rule.waitForIdle()
        screenshot("02-open.png")
        rule.onNodeWithTag("notes_quick_bar").assertIsDisplayed()
    }
}
