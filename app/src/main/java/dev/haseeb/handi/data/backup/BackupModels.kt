package dev.haseeb.handi.data.backup

import kotlinx.serialization.Serializable

/**
 * The JSON payload stored as `backup.json` inside a backup archive (see [BackupArchive]).
 *
 * Recipe photos are NOT embedded here — [BackupRecipe.imageFile] names an entry under
 * `images/` in the same archive instead, so the JSON stays small and photos can be
 * streamed in/out without loading the whole backup into memory at once.
 */
@Serializable
data class BackupFile(
    /** Bumped whenever a field is added/removed in a way old readers can't ignore. */
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val recipes: List<BackupRecipe> = emptyList(),
    /** Custom ingredients the user added to the catalogue (not the ~250 built-in ones). */
    val customIngredients: List<BackupIngredient> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

/**
 * A recipe as stored in a backup. Deliberately has no `id` — on import every recipe is
 * inserted as a brand-new row (see [dev.haseeb.handi.data.backup.BackupRepository]), since a
 * Room-assigned id from one device/install has no meaning on another.
 */
@Serializable
data class BackupRecipe(
    val title: String,
    val description: String,
    /** Filename of this recipe's photo under the archive's `images/` entry, or null. */
    val imageFile: String? = null,
    val servings: Int,
    val cookMinutes: Int,
    val ingredients: List<BackupIngredientLine> = emptyList(),
    val steps: List<String> = emptyList(),
    val updatedAt: Long = 0,
)

@Serializable
data class BackupIngredientLine(
    val name: String,
    val categoryId: String,
    val amount: Double?,
    /** [dev.haseeb.handi.data.model.Measure.name], e.g. "GRAM". */
    val measure: String,
    val note: String = "",
)

@Serializable
data class BackupIngredient(
    val categoryId: String,
    val name: String,
    /** [dev.haseeb.handi.data.model.Measure.name]. */
    val measure: String,
)
