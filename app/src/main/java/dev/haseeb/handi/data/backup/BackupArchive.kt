package dev.haseeb.handi.data.backup

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Packs/unpacks a backup `.zip`: one `backup.json` manifest entry plus one entry per recipe
 * photo under `images/`. Uses only `java.io`/`java.util.zip`, so it's unit-testable without
 * an Android device or emulator (see `BackupArchiveTest`).
 */
internal object BackupArchive {

    private const val MANIFEST_ENTRY = "backup.json"
    private const val IMAGES_DIR = "images/"

    /** The decoded contents of a backup archive: the raw manifest JSON, and photo bytes by filename. */
    data class Contents(val manifestJson: String, val images: Map<String, ByteArray>)

    fun write(output: OutputStream, manifestJson: String, images: Map<String, ByteArray>) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            images.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(IMAGES_DIR + name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    /** @throws BackupFormatException if [input] isn't a readable zip, or has no `backup.json` entry. */
    fun read(input: InputStream): Contents {
        var manifestJson: String? = null
        val images = mutableMapOf<String, ByteArray>()

        try {
            ZipInputStream(input).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == MANIFEST_ENTRY -> manifestJson = zip.readBytes().toString(Charsets.UTF_8)
                        name.startsWith(IMAGES_DIR) && !entry.isDirectory -> {
                            images[name.removePrefix(IMAGES_DIR)] = zip.readBytes()
                        }
                        // anything else is ignored, so future schema additions don't break old readers
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: IOException) {
            throw BackupFormatException("That file isn't a readable Handi backup", e)
        }

        return Contents(
            manifestJson = manifestJson ?: throw BackupFormatException("Archive has no backup.json entry"),
            images = images,
        )
    }
}
