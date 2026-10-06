package dev.haseeb.handi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM categories ORDER BY position")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM ingredients ORDER BY isCustom DESC, name COLLATE NOCASE")
    fun observeIngredients(): Flow<List<IngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIngredient(ingredient: IngredientEntity): Long
}

@Dao
abstract class RecipeDao {

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY updatedAt DESC")
    abstract fun observeAll(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract fun observe(id: Long): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    abstract suspend fun get(id: Long): RecipeWithDetails?

    @Insert
    abstract suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    abstract suspend fun updateRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertIngredients(items: List<RecipeIngredientEntity>)

    @Insert
    abstract suspend fun insertSteps(items: List<RecipeStepEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    abstract suspend fun clearIngredients(recipeId: Long)

    @Query("DELETE FROM recipe_steps WHERE recipeId = :recipeId")
    abstract suspend fun clearSteps(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /** Inserts or fully replaces a recipe and its children atomically. Returns the recipe id. */
    @Transaction
    open suspend fun save(
        recipe: RecipeEntity,
        ingredients: List<RecipeIngredientEntity>,
        steps: List<RecipeStepEntity>,
    ): Long {
        val id = if (recipe.id == 0L) {
            insertRecipe(recipe)
        } else {
            updateRecipe(recipe)
            clearIngredients(recipe.id)
            clearSteps(recipe.id)
            recipe.id
        }
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = id) })
        insertSteps(steps.map { it.copy(id = 0, recipeId = id) })
        return id
    }
}
