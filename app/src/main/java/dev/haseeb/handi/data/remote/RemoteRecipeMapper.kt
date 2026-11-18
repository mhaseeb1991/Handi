package dev.haseeb.handi.data.remote

import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient

/** Pure conversions between the domain model and the Firestore shape — see `RemoteRecipe.kt`. */

internal fun Recipe.toRemote(): RemoteRecipe = RemoteRecipe(
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

/**
 * [localId] is the Room id to restore this recipe under on *this* device: `0` for "not seen here
 * yet, let Room assign a fresh one," or an existing local recipe's id when this is an update to
 * something already synced. [remoteId] is the stable Firestore document id (see
 * [RemoteRecipeDataSource]), always carried through so the local copy can be matched again next
 * sync.
 */
internal fun RemoteRecipe.toRecipe(localId: Long, remoteId: String): Recipe = Recipe(
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
    remoteId = remoteId,
)

private fun RemoteIngredient.toDomain() = RecipeIngredient(
    name = name,
    categoryId = categoryId,
    amount = amount,
    measure = Measure.from(measure),
    note = note,
)
