package dev.haseeb.handi.data.backup

import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupMapperTest {

    private val recipe = Recipe(
        id = 42, // deliberately non-zero: toBackup()/toRecipe() must not carry this through
        title = "Chana Masala",
        description = "Chickpea curry.",
        imagePath = "/data/user/0/dev.haseeb.handi/files/recipe_images/original.jpg",
        servings = 3,
        cookMinutes = 50,
        ingredients = listOf(
            RecipeIngredient("Chickpeas", "pulses", 1.5, Measure.CUP, "soaked overnight"),
            RecipeIngredient("Salt", "masala", null, Measure.TO_TASTE, ""),
        ),
        steps = listOf("Soak chickpeas.", "Cook with spices."),
        updatedAt = 1_234_567L,
    )

    @Test fun `toBackup carries every field except id and the raw image path`() {
        val backup = recipe.toBackup(imageFile = "img_7.jpg")

        assertEquals(recipe.title, backup.title)
        assertEquals(recipe.description, backup.description)
        assertEquals("img_7.jpg", backup.imageFile)
        assertEquals(recipe.servings, backup.servings)
        assertEquals(recipe.cookMinutes, backup.cookMinutes)
        assertEquals(recipe.steps, backup.steps)
        assertEquals(recipe.updatedAt, backup.updatedAt)
        assertEquals(2, backup.ingredients.size)
        assertEquals("Chickpeas", backup.ingredients[0].name)
        assertEquals("CUP", backup.ingredients[0].measure)
        assertEquals(1.5, backup.ingredients[0].amount)
        assertEquals("soaked overnight", backup.ingredients[0].note)
        assertEquals("TO_TASTE", backup.ingredients[1].measure)
        assertNull(backup.ingredients[1].amount)
    }

    @Test fun `toBackup with no image produces a null imageFile`() {
        assertNull(recipe.toBackup(imageFile = null).imageFile)
    }

    @Test fun `toRecipe assigns id 0 and the given (device-local) image path`() {
        val backup = recipe.toBackup(imageFile = "img_7.jpg")
        val restored = backup.toRecipe(imagePath = "/data/.../recipe_images/new-copy.jpg")

        assertEquals(0L, restored.id) // always inserted as a new recipe, see BackupRepository
        assertEquals(recipe.title, restored.title)
        assertEquals(recipe.description, restored.description)
        assertEquals("/data/.../recipe_images/new-copy.jpg", restored.imagePath)
        assertEquals(recipe.servings, restored.servings)
        assertEquals(recipe.cookMinutes, restored.cookMinutes)
        assertEquals(recipe.steps, restored.steps)
        assertEquals(recipe.updatedAt, restored.updatedAt)
        assertEquals(recipe.ingredients, restored.ingredients)
    }

    @Test fun `toRecipe with no backed-up photo leaves imagePath null`() {
        val backup = recipe.toBackup(imageFile = null)
        assertNull(backup.toRecipe(imagePath = null).imagePath)
    }

    @Test fun `toBackupCustom carries the ingredient's category, name and default measure`() {
        val ingredient = Ingredient(id = 9, categoryId = "fruits", name = "Dragon Fruit", defaultMeasure = Measure.PIECE, isCustom = true)
        val backup = ingredient.toBackupCustom()

        assertEquals("fruits", backup.categoryId)
        assertEquals("Dragon Fruit", backup.name)
        assertEquals("PIECE", backup.measure)
    }

    @Test fun `round-tripping a recipe through toBackup and toRecipe preserves ingredient and step content`() {
        val backup = recipe.toBackup(imageFile = null)
        val restored = backup.toRecipe(imagePath = null)
        assertEquals(recipe.ingredients, restored.ingredients)
        assertEquals(recipe.steps, restored.steps)
    }
}
