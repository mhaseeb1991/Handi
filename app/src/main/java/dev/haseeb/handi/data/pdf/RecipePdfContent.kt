package dev.haseeb.handi.data.pdf

import dev.haseeb.handi.data.model.QuantityFormatter
import dev.haseeb.handi.data.model.Recipe

/**
 * A flattened, print-ready view of a [Recipe] — pure data, no Android/PDF dependency, so it's
 * unit-testable the same way `data/backup/`'s DTOs are. [RecipePdfRenderer] (Android-coupled:
 * `android.graphics`) is the only thing that turns this into an actual PDF page.
 */
data class RecipePdfContent(
    val title: String,
    val description: String,
    /** e.g. "Serves 4 · 35 min". */
    val metaLine: String,
    val sourceUrl: String?,
    val ingredientGroups: List<IngredientGroup>,
    val steps: List<String>,
) {
    data class IngredientGroup(val categoryName: String, val lines: List<String>)
}

/**
 * [servings] is the (possibly scaled) serving count to format ingredient amounts for — same
 * scaling [dev.haseeb.handi.ui.detail.DetailScreen] does live on screen, so the PDF matches
 * whatever the user was looking at when they chose to share it.
 */
internal fun Recipe.toPdfContent(servings: Int, categoryNames: Map<String, String>): RecipePdfContent {
    val scale = this.servings.takeIf { it > 0 && servings > 0 }?.let { servings.toDouble() / it } ?: 1.0

    val groups = ingredients
        .groupBy { it.categoryId }
        .map { (categoryId, items) ->
            RecipePdfContent.IngredientGroup(
                categoryName = categoryNames[categoryId] ?: "Other",
                lines = items.map { item ->
                    val amount = QuantityFormatter.format(item.amount, item.measure, scale)
                    if (item.note.isNotBlank()) "${item.name} — $amount (${item.note})" else "${item.name} — $amount"
                },
            )
        }

    return RecipePdfContent(
        title = title.ifBlank { "Untitled recipe" },
        description = description.trim(),
        metaLine = buildString {
            append(if (servings == 1) "Serves 1" else "Serves $servings")
            if (cookMinutes > 0) append(" · $cookMinutes min")
        },
        sourceUrl = sourceUrl,
        ingredientGroups = groups,
        steps = steps.map { it.trim() }.filter { it.isNotEmpty() },
    )
}
