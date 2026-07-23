package com.khiemnph.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThumbnailFileStoreTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ThumbnailFileStore(context)

    @Test
    fun givenBitmap_whenSave_thenWritesPngUnderFilesDirThumbnailsAndReturnsAbsolutePath() = runTest {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        val path = store.save("session-1", bitmap)

        val file = File(path)
        assertTrue("Expected saved file to exist at $path", file.exists())
        assertTrue(file.length() > 0)
        assertEquals(File(context.filesDir, "thumbnails/session-1.png").absolutePath, path)
    }

    @Test
    fun givenTwoSessions_whenSave_thenEachGetsItsOwnFile() = runTest {
        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)

        val firstPath = store.save("session-a", bitmap)
        val secondPath = store.save("session-b", bitmap)

        assertTrue(firstPath != secondPath)
        assertTrue(File(firstPath).exists())
        assertTrue(File(secondPath).exists())
    }
}
