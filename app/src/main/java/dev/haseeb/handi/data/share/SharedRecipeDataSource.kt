package dev.haseeb.handi.data.share

import com.google.firebase.firestore.FirebaseFirestore
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.remote.RemoteRecipe
import dev.haseeb.handi.data.remote.toRecipe
import dev.haseeb.handi.data.remote.toRemote
import dev.haseeb.handi.data.common.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4 (issue #8): a one-time, read-only snapshot of a recipe published for anyone with the
 * link, under the top-level `shared_recipes/{shareId}` Firestore collection -- not
 * `users/{uid}/...` like [dev.haseeb.handi.data.remote.RemoteRecipeDataSource], since a viewer
 * doesn't sign in to open a shared link. Reuses `RemoteRecipe`/its mapper for the document shape
 * (same fields, no photo) rather than inventing a parallel one.
 *
 * **Needs console setup this repo can't do for you**: a Firestore security rule allowing public
 * read (and, practically, authenticated create -- see `docs/DATA_LAYER.md`) on documents under
 * `shared_recipes`. Without it, [publish] and [fetch] fail with a permission-denied error
 * rather than crashing.
 *
 * Sharing again creates a brand new document rather than reusing a previous one -- there's no
 * persisted mapping from a recipe to its last share id, so an old link keeps working
 * independently rather than being revoked. A deliberately simple MVP; see `docs/DATA_LAYER.md`.
 */
@Singleton
class SharedRecipeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    /** Publishes a snapshot of [recipe] and returns its new share id. */
    suspend fun publish(recipe: Recipe): String {
        val shareId = UUID.randomUUID().toString()
        collection().document(shareId).set(recipe.toRemote()).await()
        return shareId
    }

    /** `null` if [shareId] doesn't exist (bad link, or the document was removed). */
    suspend fun fetch(shareId: String): Recipe? =
        collection().document(shareId).get().await()
            .toObject(RemoteRecipe::class.java)
            ?.toRecipe(localId = 0, remoteId = shareId)

    private fun collection() = firestore.collection("shared_recipes")
}
