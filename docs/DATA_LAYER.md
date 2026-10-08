# Data layer

Everything under `app/src/main/java/dev/haseeb/handi/data/`.

## 1. Domain models — `data/model/`

Plain Kotlin data classes with no Room/Android annotations. This is the
vocabulary the UI layer speaks.

**`Models.kt`**
- `Category(id, name, tint)` — one of the 15 prefilled ingredient
  categories (Vegetables, Spices, …). `tint` is a packed ARGB `Long` used to
  colour-code chips and dots.
- `Ingredient(id, categoryId, name, defaultMeasure, isCustom)` — a catalogue
  entry a user can pick from. `isCustom = true` for ingredients the user
  typed in themselves (see [UI layer](UI_LAYER.md#ingredients-step)).
- `RecipeIngredient(name, categoryId, amount, measure, note)` — an ingredient
  *as used inside one recipe* (a snapshot — not a foreign key to
  `Ingredient`, so renaming a catalogue ingredient later never changes past
  recipes).
- `Recipe(id, title, description, imagePath, servings, cookMinutes, ingredients, steps, updatedAt)`
  — the aggregate root. `id = 0` means "not saved yet".

**`Measure.kt`** — the enum of units: `GRAM`, `KILOGRAM`, `MILLILITRE`,
`LITRE`, `CUP`, `TABLESPOON`, `TEASPOON`, `PIECE`, `CLOVE`, `BUNCH`,
`HANDFUL`, `PINCH`, `TO_TASTE`. Each carries a display `label`/`short` and a
`needsAmount` flag — `TO_TASTE` is the only one that's `false`, meaning the
UI skips asking for a numeric amount for it. `Measure.from(raw)` parses a
Room-stored string back into the enum, defaulting to `PIECE` for anything
unrecognised (keeps old data readable even if a unit name ever changes).

**`QuantityFormatter.kt`** — all the fraction/parsing logic in one place, so
it's unit-testable in isolation (see
`app/src/test/java/dev/haseeb/handi/data/model/QuantityFormatterTest.kt`):
- `amount(value: Double): String` — renders `1.5` as `"1½"`, `0.25` as
  `"¼"`, `2.0` as `"2"`, anything else as a trimmed 2-decimal string.
- `format(amount, measure, scale): String` — the full label shown in the UI,
  e.g. `"2 pcs"` or `"to taste"`. Handles pluralising (`pc`→`pcs`, `bunch`→
  `bunches`) and applying the servings `scale` (see
  [Detail screen](UI_LAYER.md#detail-screen--detailviewmodel)).
- `parse(input: String): Double?` — accepts free-form kitchen input:
  `"1.5"`, `"1,5"`, `"1/2"`, `"1 1/2"`, or a typed fraction glyph like `"½"`.
  Returns `null` for anything invalid or `≤ 0`.

## 2. Room persistence — `data/local/`

**`Entities.kt`** defines five `@Entity` tables plus one POJO:

| Entity | Table | Notes |
|---|---|---|
| `CategoryEntity` | `categories` | seeded once, `position` controls display order |
| `IngredientEntity` | `ingredients` | FK → `categories.id`, `CASCADE` delete; unique `(categoryId, name)` index |
| `RecipeEntity` | `recipes` | the recipe itself |
| `RecipeIngredientEntity` | `recipe_ingredients` | FK → `recipes.id`, `CASCADE` delete; `position` preserves order |
| `RecipeStepEntity` | `recipe_steps` | FK → `recipes.id`, `CASCADE` delete; `position` preserves order |
| `RecipeWithDetails` | — | `@Embedded recipe` + `@Relation` lists of ingredients/steps — Room's way of loading a recipe and its children in one query |

**`Daos.kt`**
- `CatalogDao` — `observeCategories()` / `observeIngredients()` (both
  `Flow`), plus `insertIngredient` (`OnConflictStrategy.IGNORE`, used when a
  user adds a custom ingredient that might already exist).
- `RecipeDao` — an `abstract class` (not `interface`) because it needs a
  **non-generated `@Transaction` function**: `save(recipe, ingredients, steps)`.
  This is the one piece of logic worth understanding closely:

  ```kotlin
  @Transaction
  open suspend fun save(recipe, ingredients, steps): Long {
      val id = if (recipe.id == 0L) {
          insertRecipe(recipe)                 // new recipe
      } else {
          updateRecipe(recipe)                 // existing recipe:
          clearIngredients(recipe.id)          //  replace children wholesale —
          clearSteps(recipe.id)                //  simpler than diffing, and
          recipe.id                             //  cheap at this data size
      }
      insertIngredients(ingredients.map { it.copy(id = 0, recipeId = id) })
      insertSteps(steps.map { it.copy(id = 0, recipeId = id) })
      return id
  }
  ```

  Every save is "replace the recipe row, then delete-and-reinsert all of its
  ingredients and steps" — inside one `@Transaction` so observers never see
  a half-written recipe.

**`HandiDatabase.kt`** — the `@Database` (`version = 1`, `exportSchema =
true`; the exported schema JSON lives in
`app/schemas/dev.haseeb.handi.data.local.HandiDatabase/1.json` and is
checked in). `SeedCallback` is a `RoomDatabase.Callback` that runs **only
when the database file is first created** (`onCreate`, not every launch) —
it bulk-inserts every category and ingredient from `IngredientCatalog` using
raw `ContentValues` inserts inside one transaction, wired up in
`di/DatabaseModule.kt`.

> **No migrations exist yet.** If you change any `@Entity`, you must bump
> `version` and add a `Migration`, or add
> `fallbackToDestructiveMigration()` — otherwise existing installs crash on
> the schema mismatch.

**`IngredientCatalog.kt`** — the static seed data: 15 `SeedCategory` entries
(Vegetables, Fruits, Pulses & Lentils, Meat & Poultry, Seafood, Masala &
Spices, Fresh Herbs, Dairy & Eggs, Grains & Flour, Oils & Fats, Sauces &
Condiments, Nuts & Dry Fruits, Baking, Sweeteners, Stocks & Liquids), each
with ~15-50 ingredients paired to a sensible default `Measure` (e.g. onions
default to `PIECE`, rice to `CUP`) so the quantity sheet opens on a plausible
unit. To add an ingredient to the built-in catalogue, add it here, inside
the matching `SeedCategory`'s `items(...)` call — note this only affects
**new installs**; existing users would need a migration that inserts the
row (seeding only runs on database creation).

## 3. Repositories — `data/repository/`

The only layer that imports *both* `data.local` and `data.model`.

**`CatalogRepository`** (`@Singleton`)
- `categories: Flow<List<Category>>`
- `ingredientsByCategory: Flow<Map<String, List<Ingredient>>>` — grouped by
  category id, which is exactly the shape the ingredients-step UI needs
- `addCustomIngredient(categoryId, name, unit)` — trims and title-cases the
  name, inserts with `isCustom = true`

**`RecipeRepository`** (`@Singleton`), depends on `RecipeDao` + `ImageStorage`
- `recipes: Flow<List<Recipe>>`, `observe(id): Flow<Recipe?>`, `get(id): Recipe?`
- `save(recipe): Long` — maps the domain `Recipe` to entities via
  `Mappers.kt`, calls `RecipeDao.save(...)`, and **also** handles photo
  lifecycle: if the recipe already existed and its `imagePath` changed, the
  old file is deleted (`images.delete(oldImage)`). This is the only place
  that cleans up a *replaced* photo; a *removed-during-editing-but-never-saved*
  photo is cleaned up by `EditorViewModel` itself (see
  [UI layer](UI_LAYER.md#editor-screen--editorviewmodel)).
- `delete(id)` — deletes the DB row (cascades to ingredients/steps via FK)
  then deletes the photo file.

**`Mappers.kt`** — pure extension functions, all `internal` (repository-only):
`CategoryEntity.toModel()`, `IngredientEntity.toModel()`,
`RecipeWithDetails.toModel()`, and the reverse direction `Recipe.toEntity()`
/ `Recipe.ingredientEntities()` / `Recipe.stepEntities()`. If you add a field
to a domain model or entity, this is the file you also need to touch.

## 4. Photos — `data/image/ImageStorage.kt`

Recipe photos are **copied into app-private storage**
(`context.filesDir/recipe_images/`) rather than referenced by their original
`content://` URI, because:
- a `content://` grant from the photo picker / camera can be transient, and
- the user might delete the original from their gallery later.

- `newCameraUri(): Uri` — a `FileProvider` URI in `cacheDir/camera/` for the
  camera app to write a full-size photo into.
- `import(source: Uri): String?` — copies `source` into
  `filesDir/recipe_images/<uuid>.jpg` on `Dispatchers.IO`, returns the
  absolute path (or `null` on failure), and clears the camera cache
  afterwards.
- `delete(path: String?)` — deletes a file **only if its parent is the
  managed `recipe_images` directory** (a defensive check against ever
  deleting something outside app storage).

See `app/src/main/AndroidManifest.xml` / `app/src/main/res/xml/file_paths.xml`
for the matching `FileProvider` declaration.

## 5. Dependency injection — `di/DatabaseModule.kt`

The only Hilt module in the app today. `@InstallIn(SingletonComponent::class)`,
provides:
- `HandiDatabase` (singleton, built with `SeedCallback` attached)
- `CatalogDao`, `RecipeDao` (derived from the database instance)

`CatalogRepository`, `RecipeRepository` and `ImageStorage` don't need their
own `@Module` entries — Hilt can satisfy them directly from their
`@Inject constructor` + `@Singleton` annotations once their own dependencies
(the DAOs, `ImageStorage`, `@ApplicationContext Context`) are available.
