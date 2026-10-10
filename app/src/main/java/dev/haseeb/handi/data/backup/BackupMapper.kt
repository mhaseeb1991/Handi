package dev.haseeb.handi.data.backup

import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient

/** Pure conversions between domain models ([dev.haseeb.handi.data.model]) and backup DTOs. */

internal fun Recipe.toBackup(imageFile: String?): BackupRecipe = BackupRecipe(
    title = title,
    description = description,
    imageFile = imageFile,
    servings = servings,
    cookMinutes = cookMinutes,
    ingredients = ingredients.map { it.toBackup() },
    steps = steps,
    updatedAt = updatedAt,
)

private fun RecipeIngredient.toBackup() = BackupIngredientLine(
    name = name,
    categoryId = categoryId,
    amount = amount,
    measure = measure.name,
    note = note,
)

/** [imagePath] is the already-imported, device-local path (or null) for this recipe's photo. */
internal fun BackupRecipe.toRecipe(imagePath: String?): Recipe = Recipe(
    title = title,
    description = description,
    imagePath = imagePath,
    servings = servings,
    cookMinutes = cookMinutes,
    ingredients = ingredients.map { it.toDomain() },
    steps = steps,
    updatedAt = updatedAt,
)

private fun BackupIngredientLine.toDomain() = RecipeIngredient(
    name = name,
    categoryId = categoryId,
    amount = amount,
    measure = Measure.from(measure),
    note = note,
)

/** Only meaningful for catalogue [Ingredient]s where [Ingredient.isCustom] is true. */
internal fun Ingredient.toBackupCustom(): BackupIngredient = BackupIngredient(
    categoryId = categoryId,
    name = name,
    measure = defaultMeasure.name,
)
