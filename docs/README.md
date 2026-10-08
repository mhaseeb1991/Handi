# Handi documentation

Start here if you're new to the codebase. Read in this order:

1. **[Architecture](ARCHITECTURE.md)** — the big picture: layers, data flow, how a
   screen gets from a tap to a database row and back.
2. **[Data layer](DATA_LAYER.md)** — Room entities, DAOs, repositories, domain
   models, the `QuantityFormatter`, and `ImageStorage`.
3. **[UI layer](UI_LAYER.md)** — navigation, the three screens (Home, Detail,
   Editor), their ViewModels, and the editor's four steps.
4. **[Design system](DESIGN_SYSTEM.md)** — theme (colour/type/shape) and the
   shared Compose components every screen is built from.

If you only have five minutes, read Architecture, then jump straight to the
file you need to touch — each doc below links back to exact file paths.

## Where things live

```
app/src/main/java/dev/haseeb/handi/
  HandiApp.kt, MainActivity.kt   App entry points (Hilt + Compose root)
  data/
    local/        Room: entities, DAOs, database, first-run seed catalogue
    model/        Domain models (Recipe, Category, Ingredient, Measure…)
    repository/   RecipeRepository, CatalogRepository + entity<->model mappers
    image/        ImageStorage — private-storage photo handling
  di/             Hilt modules (currently just the database)
  ui/
    home/         Recipe grid screen + HomeViewModel
    detail/       Recipe detail screen + DetailViewModel
    editor/       4-step create/edit flow, its ViewModel and shared state
    navigation/   Type-safe Navigation Compose graph
  core/designsystem/
    theme/        Colour, typography, shape — HandiTheme
    components/   Kicker, SectionTitle, RecipeImage, TintDot, etc.
app/src/test/java/…   Unit tests (currently: QuantityFormatterTest)
```

## Conventions worth knowing before you make changes

- **Unidirectional state.** Every screen follows the same shape: a
  `@HiltViewModel` exposes a single `StateFlow<XyzUiState>`; the `@Composable`
  screen collects it with `collectAsStateWithLifecycle()` and renders it. All
  mutations go through ViewModel functions — Compose code never writes state
  directly. See [UI layer](UI_LAYER.md) for the three concrete examples.
- **Domain models vs. Room entities are different types.** `data/model/` has
  no Room annotations and nothing in `ui/` imports from `data/local/` directly.
  Conversion happens only in `data/repository/Mappers.kt`
  (`app/src/main/java/dev/haseeb/handi/data/repository/Mappers.kt`). Keep it
  that way — it's what lets the UI layer stay ignorant of SQL.
- **Repositories are the only thing a ViewModel talks to.** DAOs are injected
  into repositories, never into ViewModels.
- **This project uses [graphify](../CLAUDE.md)** — a generated knowledge
  graph in `graphify-out/`. For "where is X" / "what calls Y" questions,
  `graphify query "<question>"` is usually faster than grepping, and the repo
  rule is to try it first when `graphify-out/graph.json` exists. Run
  `graphify update .` after changing code so the graph doesn't go stale.
- **minSdk 26 / targetSdk 35**, Kotlin 2.1, Compose BOM 2024.12, Hilt 2.54
  (KSP), Room 2.6, Navigation 2.8 (type-safe routes via `@Serializable` data
  classes), Coil 2.7. See the root `README.md` for how to build and run.

## Known gaps (as of this writing)

- No `ui-tooling-preview` `@Preview` functions exist on the screens yet
  (the dependency is present in `app/build.gradle.kts` but unused).
- Only `QuantityFormatter` has unit tests; the repositories, mappers and
  ViewModels have none.
- Room has no migrations yet (`version = 1`) — any schema change needs a
  `Migration` before bumping the version, or existing installs will crash on
  upgrade (Room is not configured with `fallbackToDestructiveMigration`).
