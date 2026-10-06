package dev.haseeb.handi.ui.editor

import dev.haseeb.handi.data.model.Category
import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.QuantityFormatter

enum class EditorStep(val label: String, val headline: String) {
    BASICS("Dish", "Name your dish"),
    INGREDIENTS("Ingredients", "What goes in?"),
    METHOD("Method", "How is it made?"),
    PHOTO("Photo", "Add a photo"),
}

data class PickedIngredient(
    val name: String,
    val categoryId: String,
    val amount: Double?,
    val measure: Measure,
    val note: String,
) {
    val key: String get() = keyOf(categoryId, name)
    val quantityLabel: String get() = QuantityFormatter.format(amount, measure)

    companion object {
        fun keyOf(categoryId: String, name: String) = "$categoryId:${name.lowercase()}"
    }
}

data class StepDraft(val id: Long, val text: String)

/** State of the quantity bottom sheet for one ingredient. */
data class QuantitySheet(
    val name: String,
    val categoryId: String,
    val amountText: String,
    val measure: Measure,
    val note: String,
    val isExisting: Boolean,
) {
    val parsedAmount: Double? get() = QuantityFormatter.parse(amountText)
    val isValid: Boolean get() = !measure.needsAmount || parsedAmount != null
}

data class EditorUiState(
    val recipeId: Long = 0,
    val loading: Boolean = true,
    val step: EditorStep = EditorStep.BASICS,
    val title: String = "",
    val description: String = "",
    val servings: Int = 2,
    val cookMinutes: Int = 30,
    val categories: List<Category> = emptyList(),
    val catalog: Map<String, List<Ingredient>> = emptyMap(),
    val selectedCategoryId: String? = null,
    val ingredientQuery: String = "",
    val picked: List<PickedIngredient> = emptyList(),
    val steps: List<StepDraft> = listOf(StepDraft(id = 1, text = "")),
    val imagePath: String? = null,
    val importingImage: Boolean = false,
    val sheet: QuantitySheet? = null,
    val saving: Boolean = false,
) {
    val isEditing: Boolean get() = recipeId != 0L
    val pickedKeys: Set<String> get() = picked.mapTo(HashSet()) { it.key }
    val selectedCategory: Category? get() = categories.firstOrNull { it.id == selectedCategoryId }

    val visibleIngredients: List<Ingredient>
        get() {
            val all = catalog[selectedCategoryId].orEmpty()
            val q = ingredientQuery.trim()
            return if (q.isEmpty()) all else all.filter { it.name.contains(q, ignoreCase = true) }
        }

    val isDirty: Boolean
        get() = title.isNotBlank() || picked.isNotEmpty() || steps.any { it.text.isNotBlank() } || imagePath != null

    fun isComplete(step: EditorStep): Boolean = when (step) {
        EditorStep.BASICS -> title.isNotBlank()
        EditorStep.INGREDIENTS -> picked.isNotEmpty()
        EditorStep.METHOD, EditorStep.PHOTO -> true
    }

    /** A step can be opened when every step before it is complete. */
    fun canOpen(step: EditorStep): Boolean = EditorStep.entries.take(step.ordinal).all { isComplete(it) }
}
