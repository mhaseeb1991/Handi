package dev.haseeb.handi.data.remote

import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient

/** Pure conversions between the domain model and the Firestore shape — see `RemoteRecipe.kt`. */

internal fun Recipe.toRemote(): RemoteRecipe = RemoteRecipe(
    id = id.toString(),
    title = title,
    description = description,
    servings = servings,
    cookMinutes = cookMinutes,
    ingredients = ingredients.map { it.toRemote() },
    steps = steps,
    updatedAt = updatedAt,
    sourceUrl = sourceUrl,
)

private fun RecipeIngredient.toRemote() = RemoteIngredient(
    name = name,
    categoryId = categoryId,
    amount = amount,
    measure = measure.name,
    note = note,
)

/** [localId] is the Room-assigned id to restore this recipe under locally — see
 * [RemoteRecipeDataSource] for why this isn't carried on [RemoteRecipe] itself. */
internal fun RemoteRecipe.toRecipe(localId: Long): Recipe = Recipe(
    id = localId,
    title = title,
    description = description,
    imagePath = null,
    servings = servings,
    cookMinutes = cookMinutes,
    ingredients = ingredients.map { it.toDomain() },
    steps = steps,
    updatedAt = updatedAt,
    sourceUrl = sourceUrl,
)

private fun RemoteIngredient.toDomain() = RecipeIngredient(
    name = name,
    categoryId = categoryId,
    amount = amount,
    measure = Measure.from(measure),
    note = note,
)
