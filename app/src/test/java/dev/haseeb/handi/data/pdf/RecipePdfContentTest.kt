package dev.haseeb.handi.data.pdf

import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipePdfContentTest {

    private val recipe = Recipe(
        title = "Chana Masala",
        description = "A chickpea curry.",
        imagePath = null,
        servings = 4,
        cookMinutes = 50,
        ingredients = listOf(
            RecipeIngredient("Chickpeas", "pulses", 1.0, Measure.CUP, "soaked overnight"),
            RecipeIngredient("Onion", "vegetables", 2.0, Measure.PIECE, ""),
            RecipeIngredient("Salt", "masala", null, Measure.TO_TASTE, ""),
        ),
        steps = listOf("Soak chickpeas.", "  ", "Cook with spices.", ""),
        sourceUrl = "https://example.com/chana-masala",
    )
    private val categoryNames = mapOf("pulses" to "Pulses & Lentils", "vegetables" to "Vegetables", "masala" to "Masala & Spices")

    @Test fun `carries title, description and source link through untouched`() {
        val content = recipe.toPdfContent(servings = 4, categoryNames = categoryNames)
        assertEquals("Chana Masala", content.title)
        assertEquals("A chickpea curry.", content.description)
        assertEquals("https://example.com/chana-masala", content.sourceUrl)
    }

    @Test fun `builds the meta line from servings and cook time`() {
        assertEquals("Serves 4 · 50 min", recipe.toPdfContent(4, categoryNames).metaLine)
        assertEquals("Serves 1", recipe.copy(cookMinutes = 0).toPdfContent(1, categoryNames).metaLine)
    }

    @Test fun `scales ingredient amounts to the requested servings, matching the detail screen`() {
        val content = recipe.toPdfContent(servings = 8, categoryNames = categoryNames) // double the recipe's own 4
        val pulses = content.ingredientGroups.first { it.categoryName == "Pulses & Lentils" }
        assertEquals(listOf("Chickpeas — 2 cups (soaked overnight)"), pulses.lines)
    }

    @Test fun `appends the note in parentheses only when present`() {
        val content = recipe.toPdfContent(4, categoryNames)
        val vegetables = content.ingredientGroups.first { it.categoryName == "Vegetables" }
        assertEquals(listOf("Onion — 2 pcs"), vegetables.lines)
    }

    @Test fun `falls back to Other for an unknown category id`() {
        val content = recipe.toPdfContent(4, emptyMap())
        assertTrue(content.ingredientGroups.all { it.categoryName == "Other" })
    }

    @Test fun `drops blank method steps but keeps real ones in order`() {
        val content = recipe.toPdfContent(4, categoryNames)
        assertEquals(listOf("Soak chickpeas.", "Cook with spices."), content.steps)
    }

    @Test fun `a blank title falls back to a placeholder rather than an empty PDF heading`() {
        val content = recipe.copy(title = "   ").toPdfContent(4, categoryNames)
        assertEquals("Untitled recipe", content.title)
    }

    @Test fun `zero servings on the recipe itself doesn't divide by zero`() {
        val broken = recipe.copy(servings = 0)
        val content = broken.toPdfContent(servings = 4, categoryNames = categoryNames)
        // scale falls back to 1.0 when either side is non-positive; amounts stay as authored
        val pulses = content.ingredientGroups.first { it.categoryName == "Pulses & Lentils" }
        assertEquals(listOf("Chickpeas — 1 cup (soaked overnight)"), pulses.lines)
    }
}
