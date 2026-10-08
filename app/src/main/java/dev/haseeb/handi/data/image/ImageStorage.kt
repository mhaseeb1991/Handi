package dev.haseeb.handi.data.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recipe photos are copied into app-private storage so they survive the user
 * deleting them from the gallery, and so we never hold on to transient content:// grants.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val imagesDir: File get() = File(context.filesDir, "recipe_images").apply { mkdirs() }
    private val cameraDir: File get() = File(context.cacheDir, "camera").apply { mkdirs() }

    /** A fresh content Uri the camera app can write a full-size photo into. */
    fun newCameraUri(): Uri {
        val file = File(cameraDir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Copies [source] into private storage and returns the absolute path of the copy. */
    suspend fun import(source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(imagesDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            target.absolutePath
        }.getOrNull().also { clearCameraCache() }
    }

    /**
     * Writes raw [bytes] (e.g. read from a backup archive entry) into private storage and
     * returns the absolute path of the copy. Used by backup restore, which has already
     * extracted the photo from the archive rather than holding a content [Uri] for it.
     */
    suspend fun importBytes(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val target = File(imagesDir, "${UUID.randomUUID()}.jpg")
        target.writeBytes(bytes)
        target.absolutePath
    }

    suspend fun delete(path: String?) {
        if (path == null) return
        withContext(Dispatchers.IO) {
            runCatching { File(path).takeIf { it.parentFile == imagesDir }?.delete() }
        }
    }

    private fun clearCameraCache() {
        cameraDir.listFiles()?.forEach { it.delete() }
    }
}
