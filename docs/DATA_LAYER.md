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
- `Recipe(id, title, description, imagePath, servings, cookMinutes, ingredients, steps, updatedAt, sourceUrl)`
  — the aggregate root. `id = 0` means "not saved yet". `sourceUrl` is an
  optional link to where the recipe came from (a chef's blog, a video, a
  cookbook page) — see [UI layer](UI_LAYER.md#the-four-step-composables)
  for how it's entered, and the detail screen section for how it's shown.

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

> **`HandiDatabase` is at `version = 2`.** If you change any `@Entity`, you
> must bump `version` and add a `Migration`, or add
> `fallbackToDestructiveMigration()` — otherwise existing installs crash on
> the schema mismatch. `MIGRATION_1_2` (adding `recipes.sourceUrl`, issue
> #10) is the one migration that exists so far — follow its shape for the
> next one.

**How to add a migration safely.** A regression test harness already exists
for this: `app/src/androidTest/java/.../HandiDatabaseMigrationTest.kt`'s
`migrate1To2_addsSourceUrlColumn_keepingExistingRows` is a live, passing
example — using Room's `MigrationTestHelper` against the exported schema
JSON in `app/schemas/` (wired into the test APK as assets via the
`androidTest` `sourceSets` block in `app/build.gradle.kts`). When you
change an `@Entity`:

1. Bump `HandiDatabase.version` and let `ksp` export the new schema JSON.
2. Write the `Migration(oldVersion, newVersion) { ... }` and add it to
   `Room.databaseBuilder(...).addMigrations(...)` in `di/DatabaseModule.kt`.
3. Add a test to `HandiDatabaseMigrationTest` that calls
   `helper.createDatabase(TEST_DB, oldVersion)` (inserting a row or two to
   carry across), then `helper.runMigrationsAndValidate(TEST_DB, newVersion, true, YourMigration)`,
   and asserts the data survived.
4. Run it with `./gradlew connectedAndroidTest` against a real device or
   emulator — this repo has no CI emulator wired up (see the "known gaps"
   below), so this currently has to be run locally, not just trusted from a
   green CI run.

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

## 5. Backup & restore — `data/backup/`

Lets a user export every recipe (and its photo) plus custom catalogue
ingredients to a single `.zip` they pick a location for (GitHub issue
[#2](https://github.com/mhaseeb1991/Handi/issues/2)), and restore from one
— fully offline, no account or server involved. Split into layers so the
portable pieces are plain-JVM and unit-testable (`app/src/test/.../data/backup/`),
the same way `QuantityFormatter` is:

| File | Role | Android-dependent? |
|---|---|---|
| `BackupModels.kt` | `@Serializable` DTOs for the JSON payload: `BackupFile`, `BackupRecipe`, `BackupIngredientLine`, `BackupIngredient` | No |
| `BackupMapper.kt` | pure `Recipe.toBackup()` / `BackupRecipe.toRecipe()` / `Ingredient.toBackupCustom()` conversions | No |
| `BackupSerializer.kt` | JSON encode/decode via `kotlinx.serialization`; wraps failures in `BackupFormatException` | No |
| `BackupArchive.kt` | packs/unpacks the `.zip` (`backup.json` + `images/*`) using `java.util.zip` only | No |
| `BackupRepository.kt` | the orchestrator: reads `RecipeRepository`/`CatalogRepository`, writes/reads via `ContentResolver` + the four pieces above | Yes (`@ApplicationContext Context`) |

**Archive shape**: a zip with one `backup.json` (a `BackupFile`) and zero or
more `images/<name>.jpg` entries, one per recipe photo. `BackupRecipe.imageFile`
holds the matching entry name instead of embedding image bytes in the JSON.

**Import strategy — read this before changing it.** There's no id shared
between devices/installs, so recipes can't be matched by id. Instead,
`BackupRepository.import()` matches by **title** — trimmed and
case-insensitive, via `List<BackupRecipe>.excludingTitlesAlreadyIn()` in
`BackupDedupe.kt` (a plain, unit-tested function, see
`BackupDedupeTest.kt`). A recipe whose title already exists on the device
is **skipped entirely** — not duplicated, and not used to update/overwrite
the existing one; only recipes with a genuinely new title are inserted
(`BackupRecipe.toRecipe()` always produces `id = 0`, i.e. a fresh row).
Two repeats of the same title *within one backup* are also collapsed to
the first. `BackupSummary.skippedRecipes` reports how many were skipped,
and `BackupViewModel` surfaces it in the restore snackbar. Custom
ingredients don't need this: they go through
`CatalogRepository.addCustomIngredient`, which is already safe to repeat
thanks to Room's unique `(categoryId, name)` index on `ingredients`
(`OnConflictStrategy.IGNORE`).

This is intentionally a simple identity rule, not a full merge — renaming
a recipe before re-exporting will import it as a new copy rather than
updating the original, and two unrelated recipes that happen to share a
title will be treated as "the same" and the second skipped. A sturdier
merge strategy (e.g. a stable export-time id) is future work.

**Entry point**: `ui/backup/BackupScreen.kt` + `BackupViewModel.kt` (see
[UI layer](UI_LAYER.md)), reached from a toolbar icon on the Home screen.

## 6. Sharing a recipe as PDF — `data/pdf/`

Renders one recipe to a PDF and hands it to the system share sheet (GitHub
issue [#5](https://github.com/mhaseeb1991/Handi/issues/5)). Split the same
way `data/backup/` is, so the part that decides *what* goes on the page is
plain Kotlin and unit-tested, and only the part that actually draws it
touches `android.graphics`:

| File | Role | Android-dependent? |
|---|---|---|
| `RecipePdfContent.kt` | `Recipe.toPdfContent(servings, categories)` — builds a flat, print-ready model: title, separate servings/time labels, ingredients grouped by category (name **and** tint, for the renderer's colour dot) and formatted via `QuantityFormatter` (scaled to whatever servings the user had the detail screen set to), method steps, photo path | No |
| `RecipePdfRenderer.kt` | paginated `Canvas`/`PdfDocument` drawing of a `RecipePdfContent`, styled with Handi's own paper/ink/paprika palette — word-wraps text and starts a new page when one fills up | Yes (`android.graphics`) |
| `RecipePdfExporter.kt` | the orchestrator: reads `CatalogRepository` for the category map (name + tint), calls the two pieces above, writes the PDF to `cacheDir/pdf_exports/` (previous exports are deleted first — these are scratch files, not something kept around), and returns a `FileProvider` `content://` `Uri` | Yes (`@ApplicationContext Context`) |

No new Gradle dependency — PDF generation uses the Android framework's own
`android.graphics.pdf.PdfDocument`, the same way `ImageStorage` uses
`FileProvider` for camera photos rather than a third-party library.

**Visual design, not just a text dump.** `RecipePdfRenderer` hand-copies
the light-scheme hex values from `core/designsystem/theme/Color.kt`
(Paper background, Ink/InkSoft text, Paprika accent) rather than drawing
default black-on-white — a thin paprika bar across the top of every page,
a serif title, a "SERVES / COOK TIME" stat card (mirrors `StatsStrip`), the
recipe's own photo (centre-cropped into a rounded rect, same crop math as
`ContentScale.Crop`), a coloured dot per ingredient category (the same
`tint` `TintDot` uses on screen), the amount right-aligned in paprika next
to each ingredient, and a numbered serif accent for each method step. A
hairline + page number footer repeats on every page.

**Entry point**: a "Share as PDF" button on `DetailScreen` (see
[UI layer](UI_LAYER.md)); `DetailViewModel.sharePdf()` calls the exporter
on a background dispatcher and sends the resulting `Uri` on a one-shot
channel for the screen to launch `Intent.ACTION_SEND` with, mirroring how
`deleted` already signals a one-time navigation event.

## 7. Feature flags — `data/remoteconfig/`

Lets a feature be turned off remotely (a kill switch, a staged rollout)
without shipping a new release, via Firebase Remote Config (GitHub issue
[#13](https://github.com/mhaseeb1991/Handi/issues/13)). Split the same way
as `data/backup/` and `data/pdf/`:

| File | Role | Android-dependent? |
|---|---|---|
| `FeatureFlag.kt` | the enum of flags — each with a Remote Config `key` and a compiled-in `defaultValue` | No |
| `RemoteConfigRepository.kt` | wraps `FirebaseRemoteConfig`: `refresh()` (best-effort fetch-and-activate) and `isEnabled(flag)` | Yes (`@ApplicationContext Context`, Firebase) |

**This repo ships with no real Firebase project.** `app/build.gradle.kts`
only applies the `google-services` Gradle plugin when
`app/google-services.json` exists (that file is gitignored — it's
per-developer/per-environment, not something to commit). Without it:
- `RemoteConfigRepository`'s internal `FirebaseRemoteConfig` instance is
  `null` (checked via `FirebaseApp.getApps(context).isEmpty()`, no
  exception thrown).
- `isEnabled(flag)` always returns `flag.defaultValue`.
- `refresh()` is a no-op.

In other words: **every flag always has a working, compiled-in value,
with or without Firebase** — the app never blocks on or breaks from a
missing/unreachable Remote Config. To actually make a flag remotely
controllable, create a Firebase project, download its
`google-services.json` into `app/`, and set the flag's key (e.g.
`pdf_export_enabled`) in the Firebase console's Remote Config page.

**Adding a new flag:** add an entry to `FeatureFlag`, then read it via
`remoteConfigRepository.isEnabled(FeatureFlag.YOUR_FLAG)` wherever it
should gate something — see
[UI layer](UI_LAYER.md#feature-flags-in-the-ui) for the two existing
examples (Backup & Restore, Share as PDF).

## 8. Dependency injection — `di/DatabaseModule.kt`

The only Hilt module in the app today. `@InstallIn(SingletonComponent::class)`,
provides:
- `HandiDatabase` (singleton, built with `SeedCallback` attached)
- `CatalogDao`, `RecipeDao` (derived from the database instance)

`CatalogRepository`, `RecipeRepository` and `ImageStorage` don't need their
own `@Module` entries — Hilt can satisfy them directly from their
`@Inject constructor` + `@Singleton` annotations once their own dependencies
(the DAOs, `ImageStorage`, `@ApplicationContext Context`) are available.
