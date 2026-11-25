package dev.haseeb.handi.data.remote

import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteRecipeMapperTest {

    private val recipe = Recipe(
        id = 42,
        title = "Chana Masala",
        description = "Chickpea curry.",
        imagePath = "/data/user/0/dev.haseeb.handi/files/recipe_images/x.jpg",
        servings = 3,
        cookMinutes = 50,
        ingredients = listOf(
            RecipeIngredient("Chickpeas", "pulses", 1.5, Measure.CUP, "soaked overnight"),
            RecipeIngredient("Salt", "masala", null, Measure.TO_TASTE, ""),
        ),
        steps = listOf("Soak chickpeas.", "Cook with spices."),
        updatedAt = 1_234_567L,
        sourceUrl = "https://example.com/chana-masala",
    )

    @Test fun `toRemote never carries the local id or the local image path`() {
        val remote = recipe.toRemote()
        assertEquals(recipe.title, remote.title)
        assertEquals(recipe.description, remote.description)
        assertEquals(recipe.servings, remote.servings)
        assertEquals(recipe.cookMinutes, remote.cookMinutes)
        assertEquals(recipe.steps, remote.steps)
        assertEquals(recipe.updatedAt, remote.updatedAt)
        assertEquals(recipe.sourceUrl, remote.sourceUrl)
        assertEquals(2, remote.ingredients.size)
        assertEquals("CUP", remote.ingredients[0].measure)
        assertEquals(1.5, remote.ingredients[0].amount)
        assertEquals("TO_TASTE", remote.ingredients[1].measure)
        assertNull(remote.ingredients[1].amount)
    }

    @Test fun `toRecipe restores the given local id and remoteId, and leaves imagePath null`() {
        val remote = recipe.toRemote()
        val restored = remote.toRecipe(localId = 99, remoteId = "doc-abc")

        assertEquals(99L, restored.id)
        assertEquals("doc-abc", restored.remoteId)
        assertNull(restored.imagePath)
        assertEquals(recipe.title, restored.title)
        assertEquals(recipe.ingredients, restored.ingredients)
        assertEquals(recipe.steps, restored.steps)
        assertEquals(recipe.sourceUrl, restored.sourceUrl)
    }

    @Test fun `a recipe with no reference link round-trips sourceUrl as null`() {
        val noLink = recipe.copy(sourceUrl = null)
        assertNull(noLink.toRemote().sourceUrl)
        assertNull(noLink.toRemote().toRecipe(localId = 1, remoteId = "doc-abc").sourceUrl)
    }
}
