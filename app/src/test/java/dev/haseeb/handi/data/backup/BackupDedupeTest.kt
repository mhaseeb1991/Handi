package dev.haseeb.handi.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDedupeTest {

    private fun recipe(title: String) = BackupRecipe(title = title, description = "", servings = 1, cookMinutes = 0)

    @Test fun `keeps every recipe when nothing on the device matches`() {
        val backup = listOf(recipe("Aloo Gobi"), recipe("Chana Masala"))
        val result = backup.excludingTitlesAlreadyIn(existingTitles = listOf("Biryani"))
        assertEquals(backup, result)
    }

    @Test fun `drops a recipe whose title already exists on the device`() {
        val backup = listOf(recipe("Aloo Gobi"), recipe("Chana Masala"))
        val result = backup.excludingTitlesAlreadyIn(existingTitles = listOf("Aloo Gobi"))
        assertEquals(listOf(recipe("Chana Masala")), result)
    }

    @Test fun `matching is case-insensitive and ignores surrounding whitespace`() {
        val backup = listOf(recipe("  ALOO gobi  "))
        val result = backup.excludingTitlesAlreadyIn(existingTitles = listOf("aloo Gobi"))
        assertTrue(result.isEmpty())
    }

    @Test fun `drops repeats of the same title within the backup itself, keeping the first`() {
        val backup = listOf(recipe("Aloo Gobi"), recipe("Aloo Gobi"), recipe("Biryani"))
        val result = backup.excludingTitlesAlreadyIn(existingTitles = emptyList())
        assertEquals(listOf(recipe("Aloo Gobi"), recipe("Biryani")), result)
    }

    @Test fun `empty backup and empty device both produce an empty result`() {
        assertTrue(emptyList<BackupRecipe>().excludingTitlesAlreadyIn(emptyList()).isEmpty())
    }

    @Test fun `every device recipe already covered means nothing is imported`() {
        val backup = listOf(recipe("Aloo Gobi"), recipe("Chana Masala"))
        val result = backup.excludingTitlesAlreadyIn(existingTitles = listOf("aloo gobi", "CHANA MASALA", "Biryani"))
        assertTrue(result.isEmpty())
    }
}
