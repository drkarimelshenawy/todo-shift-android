package com.todoshift.app.v11

import android.graphics.Bitmap
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PhotoImportTest {
    @Test fun validPhotoBoundsDoNotTreatTheExpectedNullBitmapAsFailure() {
        val context = RuntimeEnvironment.getApplication()
        val source = File(context.cacheDir, "synthetic-photo.png")
        Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888).also { bitmap ->
            source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
        val name = Neo4ImageStore.importPhoto(context, Uri.fromFile(source))
        assertTrue(name.matches(Regex("neo4-theme-[a-zA-Z0-9-]+[.]jpg")))
        val loaded = Neo4ImageStore.load(context, name)
        assertNotNull(loaded); assertEquals(100, loaded!!.width); assertEquals(50, loaded.height)
    }
}
