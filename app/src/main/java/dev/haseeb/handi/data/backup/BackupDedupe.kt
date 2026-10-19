package dev.haseeb.handi.data.backup

/**
 * Decides which recipes from a backup are genuinely new, so [BackupRepository.import] doesn't
 * insert a duplicate for a recipe that's already on this device.
 *
 * There's no id shared between devices/installs (see `docs/DATA_LAYER.md`), so recipes are
 * matched by title — trimmed and case-insensitive, matching how a person would recognise "the
 * same recipe". This is intentionally simple rather than a full merge: a recipe whose title
 * matches an existing one is skipped entirely (the existing recipe's ingredients/steps/photo are
 * left untouched), not updated/overwritten.
 *
 * A plain, Android-free function so it's unit-testable like the rest of `data/backup/`.
 */
internal fun List<BackupRecipe>.excludingTitlesAlreadyIn(existingTitles: Collection<String>): List<BackupRecipe> {
    val seen = existingTitles.mapTo(mutableSetOf()) { it.normalizedForTitleMatch() }
    val result = mutableListOf<BackupRecipe>()
    for (recipe in this) {
        val key = recipe.title.normalizedForTitleMatch()
        if (seen.add(key)) {
            // first time this title is seen (not already on the device, and not a repeat
            // within this same backup) -> keep it
            result += recipe
        }
    }
    return result
}

private fun String.normalizedForTitleMatch(): String = trim().lowercase()
