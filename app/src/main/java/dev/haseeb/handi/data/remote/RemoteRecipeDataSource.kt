package dev.haseeb.handi.data.remote

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import dev.haseeb.handi.data.common.await
import dev.haseeb.handi.data.model.Recipe
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore-backed storage for one signed-in user's recipes, under `users/{uid}/recipes/{id}`.
 *
 * This is the "remote data source that sits **alongside** `RecipeRepository`'s Room-backed one"
 * issue #6 asks for — **deliberately not called from anywhere yet**. Room stays the single
 * source of truth for the whole app today; nothing here runs unless/until Phase 3 (issue #7)
 * wires it into an actual two-way sync flow. It exists now so that wiring is additive later,
 * not a redesign of this class.
 *
 * Unlike [dev.haseeb.handi.data.auth.AuthRepository] / [dev.haseeb.handi.data.remoteconfig.RemoteConfigRepository],
 * this doesn't defensively check whether Firebase is configured — because nothing constructs it
 * yet (Hilt only builds what's actually injected somewhere), there's no path that could crash a
 * real user's session today. **Phase 3 should add that same defensive check** when it actually
 * starts calling this.
 */
@Singleton
class RemoteRecipeDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun push(uid: String, recipe: Recipe) {
        recipeCollection(uid).document(recipe.id.toString()).set(recipe.toRemote()).await()
    }

    suspend fun fetchAll(uid: String): List<Recipe> =
        recipeCollection(uid).get().await().documents.mapNotNull { document ->
            val localId = document.id.toLongOrNull() ?: return@mapNotNull null
            document.toObject(RemoteRecipe::class.java)?.toRecipe(localId)
        }

    suspend fun delete(uid: String, recipeId: Long) {
        recipeCollection(uid).document(recipeId.toString()).delete().await()
    }

    private fun recipeCollection(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("recipes")
}
