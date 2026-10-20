package dev.haseeb.handi.data.pdf

import dev.haseeb.handi.data.model.Category
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
    val servingsLabel: String,
    /** Empty when the recipe has no cook time set — the renderer then shows one stat, not two. */
    val timeLabel: String,
    val sourceUrl: String?,
    val imagePath: String?,
    val ingredientGroups: List<IngredientGroup>,
    val steps: List<String>,
) {
    data class IngredientGroup(val categoryName: String, val categoryTint: Long, val lines: List<IngredientLine>)

    /** [label] is the ingredient name (+ note); [amount] is formatted separately so the
     * renderer can right-align it in its own column, matching the detail screen's layout. */
    data class IngredientLine(val label: String, val amount: String)
}

/**
 * [servings] is the (possibly scaled) serving count to format ingredient amounts for — same
 * scaling [dev.haseeb.handi.ui.detail.DetailScreen] does live on screen, so the PDF matches
 * whatever the user was looking at when they chose to share it.
 */
internal fun Recipe.toPdfContent(servings: Int, categories: Map<String, Category>): RecipePdfContent {
    val scale = this.servings.takeIf { it > 0 && servings > 0 }?.let { servings.toDouble() / it } ?: 1.0

    val groups = ingredients
        .groupBy { it.categoryId }
        .map { (categoryId, items) ->
            val category = categories[categoryId]
            RecipePdfContent.IngredientGroup(
                categoryName = category?.name ?: "Other",
                categoryTint = category?.tint ?: 0xFF8C8074,
                lines = items.map { item ->
                    RecipePdfContent.IngredientLine(
                        label = if (item.note.isNotBlank()) "${item.name} (${item.note})" else item.name,
                        amount = QuantityFormatter.format(item.amount, item.measure, scale),
                    )
                },
            )
        }

    return RecipePdfContent(
        title = title.ifBlank { "Untitled recipe" },
        description = description.trim(),
        servingsLabel = if (servings == 1) "1" else "$servings",
        timeLabel = if (cookMinutes > 0) formatMinutes(cookMinutes) else "",
        sourceUrl = sourceUrl,
        imagePath = imagePath,
        ingredientGroups = groups,
        steps = steps.map { it.trim() }.filter { it.isNotEmpty() },
    )
}

/** "35 min" under an hour, "1h 5m" at or above — matches `DetailScreen`'s `StatsStrip`. */
internal fun formatMinutes(minutes: Int): String =
    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "$minutes min"
