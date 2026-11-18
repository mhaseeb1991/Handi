package dev.haseeb.handi.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dev.haseeb.handi.data.common.await
import dev.haseeb.handi.data.model.Recipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed storage for one signed-in user's recipes, under
 * `users/{uid}/recipes/{remoteId}` — `remoteId` is a UUID (see
 * [dev.haseeb.handi.data.model.Recipe.remoteId]), not a local Room id, since two devices
 * independently hand out the same Room ids and would otherwise overwrite each other's recipes.
 *
 * Used by [dev.haseeb.handi.data.sync.SyncRepository], which is the only thing that calls this —
 * Room stays the single source of truth for the whole app; this is purely the sync transport.
 */
@Singleton
class RemoteRecipeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun push(uid: String, remoteId: String, recipe: Recipe) {
        recipeCollection(uid).document(remoteId).set(recipe.toRemote()).await()
    }

    /** Every remote recipe for [uid], paired with its document id (the recipe's `remoteId`). */
    suspend fun fetchAll(uid: String): List<Pair<String, Recipe>> =
        recipeCollection(uid).get().await().documents.mapNotNull { document ->
            document.toObject(RemoteRecipe::class.java)
                ?.toRecipe(localId = 0, remoteId = document.id)
                ?.let { document.id to it }
        }

    suspend fun delete(uid: String, remoteId: String) {
        recipeCollection(uid).document(remoteId).delete().await()
    }

    private fun recipeCollection(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("recipes")
}
