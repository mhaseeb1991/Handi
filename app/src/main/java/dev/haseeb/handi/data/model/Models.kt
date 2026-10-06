package dev.haseeb.handi.data.model

data class Category(
    val id: String,
    val name: String,
    /** ARGB colour used for the category swatch. */
    val tint: Long,
)

data class Ingredient(
    val id: Long,
    val categoryId: String,
    val name: String,
    val defaultMeasure: Measure,
    val isCustom: Boolean,
)

data class RecipeIngredient(
    val name: String,
    val categoryId: String,
    val amount: Double?,
    val measure: Measure,
    val note: String = "",
)

data class Recipe(
    val id: Long = 0,
    val title: String,
    val description: String,
    val imagePath: String?,
    val servings: Int,
    val cookMinutes: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val updatedAt: Long = 0,
)
