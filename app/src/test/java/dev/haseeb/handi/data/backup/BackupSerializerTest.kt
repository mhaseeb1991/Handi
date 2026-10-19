package dev.haseeb.handi.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupSerializerTest {

    private val sample = BackupFile(
        exportedAt = 1_700_000_000_000L,
        recipes = listOf(
            BackupRecipe(
                title = "Aloo Gobi",
                description = "A dry potato & cauliflower curry.",
                imageFile = "img_0.jpg",
                servings = 4,
                cookMinutes = 35,
                ingredients = listOf(
                    BackupIngredientLine("Potato", "vegetables", 2.0, "PIECE", ""),
                    BackupIngredientLine("Salt", "masala", null, "TO_TASTE", "to taste"),
                ),
                steps = listOf("Chop everything.", "Cook until tender."),
                updatedAt = 1_700_000_001_000L,
            ),
            BackupRecipe(
                title = "No-photo recipe",
                description = "",
                imageFile = null,
                servings = 1,
                cookMinutes = 0,
            ),
        ),
        customIngredients = listOf(BackupIngredient("vegetables", "Dragon Fruit", "PIECE")),
    )

    @Test fun `round-trips a full backup through JSON`() {
        val json = BackupSerializer.encode(sample)
        val decoded = BackupSerializer.decode(json)
        assertEquals(sample, decoded)
    }

    @Test fun `round-trips a recipe with no photo and no ingredients or steps`() {
        val lonely = BackupFile(exportedAt = 1L, recipes = listOf(sample.recipes[1]))
        assertEquals(lonely, BackupSerializer.decode(BackupSerializer.encode(lonely)))
    }

    @Test fun `ignores unknown fields so a newer export stays readable`() {
        val json = """
            {"schemaVersion":1,"exportedAt":1,"recipes":[],"customIngredients":[],"somethingFromTheFuture":42}
        """.trimIndent()
        val decoded = BackupSerializer.decode(json)
        assertEquals(1L, decoded.exportedAt)
        assertFalse(decoded.recipes.isNotEmpty())
    }

    @Test fun `rejects garbage input as a BackupFormatException`() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.decode("this is not json")
        }
    }

    @Test fun `rejects valid json missing required fields`() {
        assertThrows(BackupFormatException::class.java) {
            BackupSerializer.decode("""{"foo":"bar"}""")
        }
    }
}
