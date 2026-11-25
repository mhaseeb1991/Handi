package dev.haseeb.handi.data.repository

import dev.haseeb.handi.data.image.ImageStorage
import dev.haseeb.handi.data.local.CatalogDao
import dev.haseeb.handi.data.local.IngredientEntity
import dev.haseeb.handi.data.local.RecipeDao
import dev.haseeb.handi.data.model.Category
import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogRepository @Inject constructor(
    private val dao: CatalogDao,
) {
    val categories: Flow<List<Category>> = dao.observeCategories().map { list -> list.map { it.toModel() } }

    /** All ingredients grouped by category id. */
    val ingredientsByCategory: Flow<Map<String, List<Ingredient>>> =
        dao.observeIngredients().map { list -> list.map { it.toModel() }.groupBy { it.categoryId } }

    suspend fun addCustomIngredient(categoryId: String, name: String, unit: Measure) {
        dao.insertIngredient(
            IngredientEntity(
                categoryId = categoryId,
                name = name.trim().replaceFirstChar { it.uppercase() },
                defaultUnit = unit.name,
                isCustom = true,
            ),
        )
    }
}

@Singleton
class RecipeRepository @Inject constructor(
    private val dao: RecipeDao,
    private val images: ImageStorage,
) {
    val recipes: Flow<List<Recipe>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observe(id: Long): Flow<Recipe?> = dao.observe(id).map { it?.toModel() }

    suspend fun get(id: Long): Recipe? = dao.get(id)?.toModel()

    suspend fun save(recipe: Recipe): Long {
        val now = System.currentTimeMillis()
        val existing = if (recipe.id != 0L) dao.get(recipe.id) else null
        val id = dao.save(
            recipe = recipe.toEntity(createdAt = existing?.recipe?.createdAt ?: now, now = now),
            ingredients = recipe.ingredientEntities(),
            steps = recipe.stepEntities(),
        )
        // Photo replaced or removed -> clean up the old file.
        val oldImage = existing?.recipe?.imagePath
        if (oldImage != null && oldImage != recipe.imagePath) images.delete(oldImage)
        return id
    }

    suspend fun delete(id: Long) {
        val image = dao.get(id)?.recipe?.imagePath
        dao.delete(id)
        images.delete(image)
    }

    /**
     * Writes a recipe pulled from sync, preserving its own [Recipe.updatedAt]/[Recipe.remoteId]
     * exactly as given (unlike [save], which always stamps the current time) — otherwise every
     * pull would look like a fresh local edit and immediately re-trigger a push. [recipe.id] is
     * `0` for a recipe this device hasn't seen before (Room assigns a fresh local id) or an
     * existing local id when updating something already synced.
     */
    suspend fun upsertFromRemote(recipe: Recipe): Long {
        val existing = if (recipe.id != 0L) dao.get(recipe.id) else null
        return dao.save(
            recipe = recipe.toEntity(createdAt = existing?.recipe?.createdAt ?: recipe.updatedAt, now = recipe.updatedAt),
            ingredients = recipe.ingredientEntities(),
            steps = recipe.stepEntities(),
        )
    }

    /** Persists the UUID [SyncRepository][dev.haseeb.handi.data.sync.SyncRepository] generates
     * the first time a recipe is pushed, without touching [Recipe.updatedAt]. */
    suspend fun assignRemoteId(id: Long, remoteId: String) = dao.setRemoteId(id, remoteId)
}
