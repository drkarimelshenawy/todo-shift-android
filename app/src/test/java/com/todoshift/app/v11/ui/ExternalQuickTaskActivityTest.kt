package com.todoshift.app.v11.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.todoshift.app.QuickTaskActivity
import com.todoshift.app.v11.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="en-rUS-w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class ExternalQuickTaskActivityTest {
    @get:Rule(order=0) val fixture=object: ExternalResource() {
        override fun before() {
            val c=RuntimeEnvironment.getApplication();V11Runtime(c).start()
            NeoAppearanceStore(c).save(NeoAppearance(language=NeoLanguage.ENGLISH,reduceMotion=true))
        }
    }
    @get:Rule(order=1) val compose=createAndroidComposeRule<QuickTaskActivity>()
    private val ui by lazy { ActivitySemantics { compose.mainClock.advanceTimeBy(32) } }
    @Test fun widgetEditorSavesAndFinishesWithoutOpeningMainActivity() {
        ui.await("Title")
        assertFalse(ui.has("More: notes, time, reminder and links"))
        ui.input("Title","Quick home screen task")
        ui.click("Save")
        val runtime=V11Runtime(RuntimeEnvironment.getApplication())
        ui.until { runtime.start().tasks.any { it.title=="Quick home screen task" } }
        ui.until { compose.activity.isFinishing }
        assertEquals(1,runtime.start().tasks.count { it.title=="Quick home screen task" })
    }
}
