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
}
