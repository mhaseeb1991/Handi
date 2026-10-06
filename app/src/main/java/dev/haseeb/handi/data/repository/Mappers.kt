package dev.haseeb.handi.data.repository

import dev.haseeb.handi.data.local.CategoryEntity
import dev.haseeb.handi.data.local.IngredientEntity
import dev.haseeb.handi.data.local.RecipeEntity
import dev.haseeb.handi.data.local.RecipeIngredientEntity
import dev.haseeb.handi.data.local.RecipeStepEntity
import dev.haseeb.handi.data.local.RecipeWithDetails
import dev.haseeb.handi.data.model.Category
import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient

internal fun CategoryEntity.toModel() = Category(id = id, name = name, tint = tint)

internal fun IngredientEntity.toModel() = Ingredient(
    id = id,
    categoryId = categoryId,
    name = name,
    defaultMeasure = Measure.from(defaultUnit),
    isCustom = isCustom,
)

internal fun RecipeWithDetails.toModel() = Recipe(
    id = recipe.id,
    title = recipe.title,
    description = recipe.description,
    imagePath = recipe.imagePath,
    servings = recipe.servings,
    cookMinutes = recipe.cookMinutes,
    ingredients = ingredients.sortedBy { it.position }.map {
        RecipeIngredient(
            name = it.name,
            categoryId = it.categoryId,
            amount = it.amount,
            measure = Measure.from(it.unit),
            note = it.note,
        )
    },
    steps = steps.sortedBy { it.position }.map { it.text },
    updatedAt = recipe.updatedAt,
)

internal fun Recipe.toEntity(createdAt: Long, now: Long) = RecipeEntity(
    id = id,
    title = title.trim(),
    description = description.trim(),
    imagePath = imagePath,
    servings = servings,
    cookMinutes = cookMinutes,
    createdAt = createdAt,
    updatedAt = now,
)

internal fun Recipe.ingredientEntities() = ingredients.mapIndexed { index, item ->
    RecipeIngredientEntity(
        recipeId = id,
        name = item.name,
        categoryId = item.categoryId,
        amount = item.amount,
        unit = item.measure.name,
        note = item.note.trim(),
        position = index,
    )
}

internal fun Recipe.stepEntities() = steps.map { it.trim() }.filter { it.isNotEmpty() }
    .mapIndexed { index, text -> RecipeStepEntity(recipeId = id, position = index, text = text) }
