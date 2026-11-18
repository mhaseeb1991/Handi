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
    /** Optional link to where this recipe came from (a chef's blog, a video, a cookbook page). */
    val sourceUrl: String? = null,
    /**
     * Stable cross-device identity for sync (issue #7) — a UUID assigned the first time this
     * recipe is pushed to Firestore, used as its document id there instead of [id]. [id] is a
     * local, per-device Room autoincrement value, so two devices can (and will) hand out the
     * same [id] to two different recipes; only [remoteId] is safe to use as a shared key.
     */
    val remoteId: String? = null,
)
