package dev.haseeb.handi.data.sync

import dev.haseeb.handi.data.auth.AuthRepository
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.remote.RemoteRecipeDataSource
import dev.haseeb.handi.data.repository.RecipeRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(val pulled: Int, val pushed: Int)

/**
 * Phase 3 (issue #7): one pass of last-write-wins sync between Room and Firestore, keyed by each
 * recipe's stable [Recipe.remoteId] (never its local, per-device Room id -- see
 * [RemoteRecipeDataSource]'s doc comment for why). The actual merge decision is pure --
 * see [recipesToPull]/[recipesToPush] in `SyncPlan.kt` -- this class is just the Room/Firestore
 * plumbing around it.
 *
 * **Known limitation, by design:** deleting a recipe is local-only. It is not yet removed from
 * Firestore or from other devices' copies -- that needs delete tombstones, which this phase
 * doesn't add. See `docs/DATA_LAYER.md`.
 */
@Singleton
class SyncRepository @Inject constructor(
    private val recipes: RecipeRepository,
    private val remote: RemoteRecipeDataSource,
    private val auth: AuthRepository,
) {
    suspend fun syncNow(): Result<SyncResult> = runCatching {
        val uid = auth.currentUser.first()?.uid ?: error("Sign in to sync your recipes.")

        val localRecipes = recipes.recipes.first()
        val remoteByRemoteId = remote.fetchAll(uid).toMap()

        val toPull = recipesToPull(localRecipes, remoteByRemoteId)
        toPull.forEach { recipes.upsertFromRemote(it) }

        val toPush = recipesToPush(localRecipes, remoteByRemoteId)
        toPush.forEach { local ->
            val remoteId = local.remoteId ?: UUID.randomUUID().toString().also {
                recipes.assignRemoteId(local.id, it)
            }
            remote.push(uid, remoteId, local)
        }

        SyncResult(pulled = toPull.size, pushed = toPush.size)
    }
}
