package dev.haseeb.handi.data.sync

import dev.haseeb.handi.data.model.Recipe

/**
 * The pure last-write-wins decision at the heart of [SyncRepository] -- separated out so it's
 * unit-testable without touching Room or Firestore, same split as `RemoteRecipeMapper.kt` next
 * to `RemoteRecipeDataSource.kt`.
 */

/** Remote recipes that are new to this device, or newer than this device's copy -- ready to
 * hand to [dev.haseeb.handi.data.repository.RecipeRepository.upsertFromRemote] as-is. */
internal fun recipesToPull(local: List<Recipe>, remoteByRemoteId: Map<String, Recipe>): List<Recipe> {
    val localByRemoteId = local.filter { it.remoteId != null }.associateBy { it.remoteId }
    return remoteByRemoteId.mapNotNull { (remoteId, remoteRecipe) ->
        val existing = localByRemoteId[remoteId]
        if (existing == null || remoteRecipe.updatedAt > existing.updatedAt) {
            remoteRecipe.copy(id = existing?.id ?: 0)
        } else {
            null
        }
    }
}

/** Local recipes that have never been pushed, or are newer than their remote copy. */
internal fun recipesToPush(local: List<Recipe>, remoteByRemoteId: Map<String, Recipe>): List<Recipe> =
    local.filter { recipe ->
        val remoteRecipe = recipe.remoteId?.let { remoteByRemoteId[it] }
        remoteRecipe == null || recipe.updatedAt > remoteRecipe.updatedAt
    }
