# Handi — a recipe notebook for Android

Native Android app (Kotlin, Jetpack Compose, Material 3) for writing down, browsing and editing recipes.

## Features
- **Recipe grid** — staggered, magazine-style grid of saved recipes with photo + name, search by dish or ingredient.
- **Recipe detail** — hero photo, ingredients grouped by category, numbered method, servings scaler (quantities rescale live), edit & delete.
- **4-step editor** (used for both *new* and *edit*):
  1. **Dish** — name, short description, serves, cook time
  2. **Ingredients** — 15 prefilled categories (Vegetables, Fruits, Pulses & Lentils, Meat & Poultry, Seafood, Masala & Spices, Fresh Herbs, Dairy & Eggs, Grains & Flour, Oils & Fats, Sauces & Condiments, Nuts & Dry Fruits, Baking, Sweeteners, Stocks & Liquids) with ~250 ingredients. Pick a category → tap ingredients → set quantity in a bottom sheet (amount with −/+ and quick fractions, unit chips incl. *to taste*, optional note). Add **custom ingredients** to any category; they’re saved for reuse.
  3. **Method** — numbered steps, reorder up/down, delete
  4. **Photo** — gallery (Android Photo Picker) or camera, then save
- **Custom light & dark theme** — paper/ink + paprika/herb/saffron palette, serif headlines. Dynamic colour is deliberately off.

## Architecture
```
data/
  local/        Room entities, DAOs, DB + first-run seed (IngredientCatalog)
  model/        Domain models, Measure units, QuantityFormatter (fractions, scaling, parsing)
  repository/   RecipeRepository, CatalogRepository
  image/        ImageStorage — copies photos into app-private storage, camera FileProvider
di/             Hilt modules
ui/
  home/ detail/ editor/   Screen + @HiltViewModel (unidirectional StateFlow)
  navigation/   Type-safe Navigation Compose routes
core/designsystem/        Theme (colour, type, shape) + shared components
```
Stack: Kotlin 2.1, Compose BOM 2024.12, Material 3, Hilt 2.54 (KSP), Room 2.6, Navigation 2.8 (type-safe), Coil 2.7. minSdk 26, targetSdk 35.

## Run
Open the folder in Android Studio (Ladybug or newer), let Gradle sync, run the `app` configuration.
`./gradlew testDebugUnitTest` runs the formatter unit tests.
