# Architecture

Handi is a small single-module Android app: one `app` Gradle module, no
feature modules. It follows a conventional three-layer split —
**data → domain models → UI** — wired together with Hilt.

```
┌─────────────────────────────────────────────────────────────────┐
│ ui/                                                             │
│   Screens (@Composable) ── collect StateFlow ──> @HiltViewModel │
│   Navigation Compose routes glue the three screens together     │
└────────────────────────────────┬────────────────────────────────┘
                                 │ calls repository functions
┌────────────────────────────────▼─────────────────────────────────┐
│ data/repository/                                                 │
│   RecipeRepository, CatalogRepository                            │
│   — expose domain models (data/model/) as Flow / suspend fun     │
│   — convert to/from Room entities via Mappers.kt                 │
└───────────────┬──────────────────────────────────┬───────────────┘
                │                                  │
┌───────────────▼───────────────┐   ┌──────────────▼─────────────────┐
│ data/local/ (Room )           │   │ data/image/ (ImageStorage)     │
│ HandiDatabase, DAOs, entities │   │ copies photos into private     │
│ seeded on first create        │   │ app storage, cleans up on      │
│                               │   │ replace/delete                 │
└───────────────────────────────┘   └────────────────────────────────┘
```

## Request / response flow, end to end

Take "user opens the Home screen and sees their recipes" as an example:

1. `HandiNavHost` (`ui/navigation/HandiNavHost.kt`) starts at `HomeRoute` and
   renders `HomeScreen`.
2. `HomeScreen` asks Hilt for a `HomeViewModel` via `hiltViewModel()`.
3. `HomeViewModel.state` is a `StateFlow<HomeUiState>` built by combining
   `RecipeRepository.recipes` (a `Flow<List<Recipe>>`) with the current
   search query, recomputing on every change
   (`ui/home/HomeViewModel.kt:29`).
4. `RecipeRepository.recipes` is `dao.observeAll().map { it.map { it.toModel() } }`
   — `RecipeDao.observeAll()` is a Room `@Query` returning
   `Flow<List<RecipeWithDetails>>`; every insert/update/delete to the
   `recipes`, `recipe_ingredients` or `recipe_steps` tables automatically
   re-emits (`data/local/Daos.kt:28`).
5. `RecipeWithDetails.toModel()` (`data/repository/Mappers.kt:25`) turns the
   Room relation (entity + child entities) into the plain `Recipe` domain
   model the UI understands.
6. `HomeScreen` collects `state` with `collectAsStateWithLifecycle()` and
   renders a staggered grid of `RecipeCard`s.

Saving a recipe from the editor works the same way in reverse: `EditorViewModel.save()`
builds a `Recipe` domain object from its `EditorUiState`, calls
`RecipeRepository.save(recipe)`, which maps it back to `RecipeEntity` +
children and writes them in one Room `@Transaction`
(`data/local/Daos.kt:61`, `RecipeDao.save`). The Flow from step 4 then
re-emits automatically — nobody has to manually refresh the Home screen.

## Why it's shaped this way

- **ViewModels never see Room types.** Repositories are the seam: they're
  the only code that imports both `data/local/*Entity` and `data/model/*`.
  This means the UI and ViewModel layers could be pointed at a different
  storage backend without changing a single screen.
- **Everything reactive is a `Flow`/`StateFlow`, nothing is fetched once and
  held.** Room DAOs return `Flow`, repositories map that `Flow`, ViewModels
  combine/`stateIn` it into UI state. A change anywhere (insert a recipe,
  add a custom ingredient) propagates to every open screen automatically.
- **One `@HiltViewModel` per screen, one `UiState` data class per ViewModel.**
  See [UI layer](UI_LAYER.md#the-unidirectional-state-pattern) for the shape
  all three follow.
- **Dependency injection is Hilt, entry points are minimal.** `HandiApp`
  (`@HiltAndroidApp`) and `MainActivity` (`@AndroidEntryPoint`) are the only
  two Hilt-aware Android framework classes; everything else is plain
  constructor injection (`@Inject constructor`). The only `@Module` today is
  `di/DatabaseModule.kt`, which provides the singleton `HandiDatabase` and
  its two DAOs. Repositories and `ImageStorage` don't need an explicit module
  — Hilt derives their bindings from their own `@Inject constructor` plus
  `@Singleton`.
- **Navigation is type-safe.** Routes are `@Serializable` data classes
  (`HomeRoute`, `DetailRoute(val id: Long)`, `EditorRoute(val id: Long = 0)`)
  instead of hand-built string paths; `SavedStateHandle.toRoute<...>()` reads
  the argument back out in `DetailViewModel` / `EditorViewModel`. See
  `ui/navigation/HandiNavHost.kt`.

## Module/package map

| Package | Responsibility | Depends on |
|---|---|---|
| `data.local` | Room entities, DAOs, `HandiDatabase`, first-run seed data | — |
| `data.model` | Plain domain models + `Measure` + `QuantityFormatter` | — |
| `data.repository` | `RecipeRepository`, `CatalogRepository`, entity↔model `Mappers.kt` | `data.local`, `data.model`, `data.image` |
| `data.image` | `ImageStorage` (private-storage photo copy/cleanup) | Android `Context` only |
| `di` | Hilt `@Module`s | `data.local` |
| `ui.home` / `ui.detail` / `ui.editor` | Screens + ViewModels | `data.repository`, `data.model`, `ui.navigation`, `core.designsystem` |
| `ui.navigation` | Route definitions + `NavHost` | `ui.home`, `ui.detail`, `ui.editor` |
| `core.designsystem` | Theme + shared Compose components | nothing app-specific |

Further reading: [Data layer](DATA_LAYER.md) · [UI layer](UI_LAYER.md) ·
[Design system](DESIGN_SYSTEM.md).
