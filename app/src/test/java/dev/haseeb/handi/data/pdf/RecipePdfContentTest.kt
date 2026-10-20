package dev.haseeb.handi.data.pdf

import dev.haseeb.handi.data.model.Category
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
    private val categories = mapOf(
        "pulses" to Category("pulses", "Pulses & Lentils", 0xFFC08A2E),
        "vegetables" to Category("vegetables", "Vegetables", 0xFF5B8A3C),
        "masala" to Category("masala", "Masala & Spices", 0xFFC4661F),
    )

    @Test fun `carries title, description, source link and photo through untouched`() {
        val withPhoto = recipe.copy(imagePath = "/data/.../recipe_images/x.jpg")
        val content = withPhoto.toPdfContent(servings = 4, categories = categories)
        assertEquals("Chana Masala", content.title)
        assertEquals("A chickpea curry.", content.description)
        assertEquals("https://example.com/chana-masala", content.sourceUrl)
        assertEquals("/data/.../recipe_images/x.jpg", content.imagePath)
    }

    @Test fun `builds separate servings and time labels, matching the detail screen's format`() {
        val content = recipe.toPdfContent(4, categories)
        assertEquals("4", content.servingsLabel)
        assertEquals("50 min", content.timeLabel)
        assertEquals("1", recipe.toPdfContent(1, categories).servingsLabel)
    }

    @Test fun `an hour or more formats as h and m, and zero cook time is an empty label`() {
        assertEquals("1h 5m", formatMinutes(65))
        assertEquals("", recipe.copy(cookMinutes = 0).toPdfContent(4, categories).timeLabel)
    }

    @Test fun `scales ingredient amounts to the requested servings, matching the detail screen`() {
        val content = recipe.toPdfContent(servings = 8, categories = categories) // double the recipe's own 4
        val pulses = content.ingredientGroups.first { it.categoryName == "Pulses & Lentils" }
        assertEquals(listOf(RecipePdfContent.IngredientLine("Chickpeas (soaked overnight)", "2 cups")), pulses.lines)
    }

    @Test fun `appends the note in parentheses only when present`() {
        val content = recipe.toPdfContent(4, categories)
        val vegetables = content.ingredientGroups.first { it.categoryName == "Vegetables" }
        assertEquals(listOf(RecipePdfContent.IngredientLine("Onion", "2 pcs")), vegetables.lines)
    }

    @Test fun `carries the category's tint through for the renderer's colour dot`() {
        val content = recipe.toPdfContent(4, categories)
        val vegetables = content.ingredientGroups.first { it.categoryName == "Vegetables" }
        assertEquals(0xFF5B8A3C, vegetables.categoryTint)
    }

    @Test fun `falls back to Other (and a neutral tint) for an unknown category id`() {
        val content = recipe.toPdfContent(4, emptyMap())
        assertTrue(content.ingredientGroups.all { it.categoryName == "Other" })
        assertTrue(content.ingredientGroups.all { it.categoryTint == 0xFF8C8074L })
    }

    @Test fun `drops blank method steps but keeps real ones in order`() {
        val content = recipe.toPdfContent(4, categories)
        assertEquals(listOf("Soak chickpeas.", "Cook with spices."), content.steps)
    }

    @Test fun `a blank title falls back to a placeholder rather than an empty PDF heading`() {
        val content = recipe.copy(title = "   ").toPdfContent(4, categories)
        assertEquals("Untitled recipe", content.title)
    }

    @Test fun `zero servings on the recipe itself doesn't divide by zero`() {
        val broken = recipe.copy(servings = 0)
        val content = broken.toPdfContent(servings = 4, categories = categories)
        // scale falls back to 1.0 when either side is non-positive; amounts stay as authored
        val pulses = content.ingredientGroups.first { it.categoryName == "Pulses & Lentils" }
        assertEquals(listOf(RecipePdfContent.IngredientLine("Chickpeas (soaked overnight)", "1 cup")), pulses.lines)
    }
}
