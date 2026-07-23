package com.khiemnph.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Persists route-map thumbnails to app-private storage. */
class ThumbnailFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun save(sessionId: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val thumbnailsDir = File(context.filesDir, THUMBNAILS_DIR_NAME).apply { mkdirs() }
        val file = File(thumbnailsDir, "$sessionId.png")
        file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output) }
        file.absolutePath
    }

    private companion object {
        const val THUMBNAILS_DIR_NAME = "thumbnails"
        const val PNG_QUALITY = 100
    }
}
