package com.exmore.app

import android.content.Context
import com.exmore.app.appcore.AppCoreReleaseClient
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OfficialIdentityMigrationTest {
    @Test
    fun runtimeIdentityAndReleaseSequenceAreOfficial() {
        val context: Context = RuntimeEnvironment.getApplication()
        assertEquals("com.exmore.app", context.packageName)
        assertEquals("com.exmore.app", BuildConfig.APPLICATION_ID)
        assertEquals(81, BuildConfig.VERSION_CODE)
        assertEquals("4.4.2", BuildConfig.VERSION_NAME)
    }

    @Test
    fun appCoreUsesStableExMoreProjectIdentity() {
        assertEquals("exmore", AppCoreReleaseClient.PROJECT_ID)
    }
}
