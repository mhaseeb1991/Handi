package dev.haseeb.handi.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupArchiveTest {

    @Test fun `round-trips a manifest with no images`() {
        val output = ByteArrayOutputStream()
        BackupArchive.write(output, manifestJson = """{"hello":"world"}""", images = emptyMap())

        val contents = BackupArchive.read(ByteArrayInputStream(output.toByteArray()))

        assertEquals("""{"hello":"world"}""", contents.manifestJson)
        assertTrue(contents.images.isEmpty())
    }

    @Test fun `round-trips a manifest with several images, bytes untouched`() {
        val images = mapOf(
            "img_0.jpg" to byteArrayOf(1, 2, 3, 4, 5),
            "img_1.jpg" to ByteArray(10_000) { (it % 256).toByte() }, // bigger than one zip buffer
        )
        val output = ByteArrayOutputStream()
        BackupArchive.write(output, manifestJson = """{"v":1}""", images = images)

        val contents = BackupArchive.read(ByteArrayInputStream(output.toByteArray()))

        assertEquals("""{"v":1}""", contents.manifestJson)
        assertEquals(images.keys, contents.images.keys)
        images.forEach { (name, bytes) -> assertArrayEquals(bytes, contents.images[name]) }
    }

    @Test fun `throws BackupFormatException when the archive has no backup json entry`() {
        val output = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("images/img_0.jpg"))
            zip.write(byteArrayOf(1, 2, 3))
            zip.closeEntry()
        }

        assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(ByteArrayInputStream(output.toByteArray()))
        }
    }

    @Test fun `throws BackupFormatException for a file that isn't a zip at all`() {
        assertThrows(BackupFormatException::class.java) {
            BackupArchive.read(ByteArrayInputStream("not a zip file".toByteArray()))
        }
    }

    @Test fun `ignores entries outside images and the manifest`() {
        val output = ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(output).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry(BACKUP_ENTRY))
            zip.write("""{"ok":true}""".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("README.txt"))
            zip.write("not part of the backup".toByteArray())
            zip.closeEntry()
        }

        val contents = BackupArchive.read(ByteArrayInputStream(output.toByteArray()))
        assertEquals("""{"ok":true}""", contents.manifestJson)
        assertTrue(contents.images.isEmpty())
    }

    private companion object {
        const val BACKUP_ENTRY = "backup.json"
    }
}
