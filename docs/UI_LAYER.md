# UI layer

Everything under `app/src/main/java/dev/haseeb/handi/ui/`, plus the app's
two entry points.

## Entry points

- **`HandiApp.kt`** — `@HiltAndroidApp class HandiApp : Application()`.
  Empty body; its only job is to trigger Hilt's codegen for the whole app.
- **`MainActivity.kt`** — `@AndroidEntryPoint`, single-activity app. Calls
  `enableEdgeToEdge()`, then `setContent { HandiTheme { HandiNavHost() } }`.
  There is exactly one `Activity` and one Compose tree; every "screen" is a
  composable reached through navigation, not a separate `Activity`.

## Navigation — `ui/navigation/HandiNavHost.kt`

Three routes, declared as `@Serializable` data classes (Navigation Compose's
type-safe routing, no string paths):

```kotlin
@Serializable data object HomeRoute
@Serializable data class DetailRoute(val id: Long)
@Serializable data class EditorRoute(val id: Long = 0L)   // id == 0 -> new recipe
```

`HandiNavHost()` builds the `NavHost` with custom slide/fade transitions and
wires the three screens together:
- `HomeRoute` → tapping a recipe navigates to `DetailRoute(id)`; tapping
  "New recipe" navigates to `EditorRoute()` (no id).
- `DetailRoute` → back pops the stack; "Edit" navigates to `EditorRoute(id)`.
- `EditorRoute` → on save, if it was a *new* recipe, navigates to
  `DetailRoute(id)` and pops the editor off the stack
  (`popUpTo<EditorRoute> { inclusive = true }`) so back-from-detail goes to
  Home, not back into the editor; if it was an *edit*, it just pops back to
  Detail.

To add a fourth screen: add a `@Serializable` route, add a `composable<Route> { }`
block here, and have the originating screen call `nav.navigate(YourRoute(...))`.

## The unidirectional-state pattern

All three screens follow the same shape — learn it once:

```
@HiltViewModel class XyzViewModel : ViewModel() {
    val state: StateFlow<XyzUiState> = ...   // single source of truth
    fun someAction(...) { /* mutate state */ }
}

@Composable fun XyzScreen(viewModel: XyzViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // render `state`; every tap calls a viewModel function, never
    // mutates a local `var` that represents app data
}
```

Reasons this matters when you're adding a feature: the screen is a pure
function of `state`; if something is wrong on screen, the bug is almost
always in the ViewModel (or the repository/Flow it reads), not in the
Compose code. Three concrete flavours of this pattern exist:

### Home screen — `ui/home/HomeScreen.kt` + `HomeViewModel.kt`

Simplest case — state is **derived, no local mutable fields**:

```kotlin
data class HomeUiState(loading, query, total, recipes)

val state: StateFlow<HomeUiState> =
    combine(repository.recipes, query) { recipes, q -> /* filter */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
```

`query` is a local `MutableStateFlow<String>` the search box writes to via
`onQueryChange`; it's combined with the repository's recipe list so typing
re-filters live. `WhileSubscribed(5_000)` means the underlying Flow keeps
running for 5s after the last collector disappears (so rotating the screen,
which briefly drops and re-adds the collector, doesn't restart the DB
query).

UI: `Scaffold` with an `ExtendedFloatingActionButton` ("New recipe") and a
`LazyVerticalStaggeredGrid` of two columns. Private composables: `Header`
(title + search field), `RecipeCard` (alternates aspect ratio by
`recipe.id % 3` / `% 2` for a magazine-style uneven grid), `EmptyKitchen`
(shown when `total == 0`).

### Detail screen — `ui/detail/DetailScreen.kt` + `DetailViewModel.kt`

Introduces a **local override combined with upstream data**, and a
one-shot event channel:

```kotlin
data class DetailUiState(loading, recipe, categories, servings) {
    val scale: Double get() = /* servings / recipe.servings, else 1.0 */
}

val state: StateFlow<DetailUiState> =
    combine(recipes.observe(recipeId), catalog.categories, servingsOverride) { ... }
```

- The recipe id comes from the nav arg via
  `savedStateHandle.toRoute<DetailRoute>().id`.
- `servingsOverride: MutableStateFlow<Int?>` holds the user's live "scale to
  N servings" choice; `null` means "use the recipe's own serving count".
  `changeServings(delta)` nudges it, clamped to `1..50`.
- `scale` is then used by `QuantityFormatter.format(amount, measure, scale)`
  to rescale every ingredient amount shown on screen — the source recipe
  data on disk is never touched by this.
- `delete()` deletes via `RecipeRepository.delete(id)` then sends on a
  `Channel<Unit>` (`deleted`); the screen collects it with a
  `LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }` to
  navigate back exactly once. (A `Channel` rather than another `StateFlow`
  field, because "navigate back" is a one-time event, not a piece of state
  that should survive recomposition/replay.)

UI: full-bleed hero photo with a gradient fade into the background, then
title/description, a `StatsStrip` (servings stepper + cook time), ingredients
grouped by category with a coloured `TintDot`, numbered method steps. Two
floating round buttons (back, delete) over the hero, an `AlertDialog` to
confirm deletion, an `ExtendedFloatingActionButton` ("Edit recipe").

### Editor screen — `ui/editor/EditorScreen.kt` + `EditorViewModel.kt`

The complex one: a 4-step wizard that creates **or** edits a recipe, backed
by one mutable `EditorUiState` (`ui/editor/EditorState.kt`) that the
ViewModel updates imperatively with `_state.update { ... }` (unlike Home/
Detail, this state isn't purely derived from a Flow — it's a form).

**`EditorStep` enum**: `BASICS → INGREDIENTS → METHOD → PHOTO`, each with a
`label` (tab text) and `headline` (big title). `EditorUiState.isComplete(step)`
defines what "done" means per step (`BASICS` needs a non-blank title,
`INGREDIENTS` needs ≥1 picked ingredient, `METHOD`/`PHOTO` are always
"complete" since they're optional) and `canOpen(step)` says a step is only
reachable once every step before it is complete — this is what disables
jumping ahead in the top progress bar.

**Supporting state types** (all in `EditorState.kt`):
- `PickedIngredient` — an ingredient added to this recipe-in-progress; `key`
  is `"<categoryId>:<name.lowercase>"`, used to dedupe/find/replace a pick.
- `StepDraft(id, text)` — a method step; `id` is a locally-generated
  monotonic long (`EditorViewModel.stepIds`), not a DB id, used purely as a
  stable Compose list key while reordering.
- `QuantitySheet` — the bottom sheet's own working state (amount text,
  measure, note, whether it's editing an existing pick or adding new).

**Loading**: on init, the ViewModel subscribes to
`catalog.categories`/`catalog.ingredientsByCategory` (so newly added custom
ingredients show up immediately), and — if `editingId != 0L` — loads the
existing `Recipe` once and seeds the form fields from it.

**Photo handling is the subtle part** — because the editor can be abandoned
without saving, and photos are real files on disk:
- `sessionImages: MutableSet<String>` tracks every photo path imported
  *during this editing session* (not ones the recipe already had on disk
  before editing started).
- Picking a new photo while one is already set discards the previous one
  immediately if it was imported this session (`discardIfUnsaved`) — but
  leaves alone a photo that came from the original saved recipe (that one
  only gets cleaned up by `RecipeRepository.save()` if the save actually
  goes through and replaces it, see [Data layer](DATA_LAYER.md#3-repositories--datarepository)).
  removing the photo in-editor behaves the same way.
- `save()` clears `sessionImages` on success (the photo is now "owned" by
  the saved recipe, not a draft).
- `onCleared()` (called when the ViewModel is destroyed — e.g. user backs
  out without saving) deletes every file still in `sessionImages`, so
  cancelled edits never leak files into `filesDir/recipe_images/`.

**Quantity sheet step math** lives in `EditorViewModel`'s companion object
and is itself a small piece of UX logic worth knowing about if you touch
quantities:
- `defaultAmountFor(measure)` — `"100"` for gram/millilitre, `""` for
  to-taste, `"1"` otherwise — the amount shown when a sheet first opens.
- `stepFor(measure)` — how much `±` nudges by (50 for gram/ml, 0.5 for
  kg/L/cup/tbsp/tsp, 1 otherwise).
- `quickAmounts(measure)` — the tappable quick-pick chips (`"50","100","250","500"`
  for gram/ml; fractions for everything else; none for to-taste).

**Screen composition** (`EditorScreen.kt`): `Scaffold` with a custom
`EditorTopBar` (close button + step progress bar, tapping a reached step
jumps to it) and `EditorBottomBar` (Back/Continue/Save, shows a hint like
"Pick at least one ingredient" when the current step isn't complete yet).
The body is an `AnimatedContent` keyed on `state.step` that slides
horizontally between the four step composables. A `BackHandler` intercepts
the system back gesture: if a quantity sheet is open it's ignored (the sheet
has its own dismiss), otherwise it goes to the previous step, or — on the
first step — prompts to discard if the form `isDirty`.

#### The four step composables

- **`BasicsStep.kt`** — title (`BasicTextField`, auto-focused on first open
  if empty), optional description, and two `StepperCard`s (servings, cook
  time) with `±` buttons.
- **`IngredientsStep.kt`** — a horizontal row of category chips (`CategoryChip`,
  tinted, with a picked-count badge), then a search field + wrapping
  `FlowRow` of ingredient chips for the selected category
  (`state.visibleIngredients`, filtered by `state.ingredientQuery`), a
  dialog to add a custom ingredient (`CustomIngredientDialog` →
  `EditorViewModel.addCustomIngredient`), and a "In this recipe" list of
  already-picked ingredients (`PickedRow`) that can be tapped to re-open
  their quantity sheet or removed with an X.
- **`QuantityBottomSheet.kt`** — a `ModalBottomSheet` for setting one
  ingredient's amount/unit/note: a `±`-nudgeable `BasicTextField` amount,
  quick-amount chips, a `FlowRow` of every `Measure` as a `UnitChip`, a note
  field, and Add/Update (+ Remove, if editing an existing pick). The column
  has `verticalScroll(...)` + `imePadding()` so it stays usable with the
  keyboard open (see the fix for GitHub issue #1, committed in `320a225`).
- **`MethodStep.kt`** — a numbered `LazyColumn` of step text fields with
  up/down/delete `IconButton`s per row, and an "Add step" button at the
  bottom.
- **`PhotoStep.kt`** — a tappable preview box (gallery picker or, once a
  photo is set, the photo itself via `RecipeImage`), Gallery/Camera buttons
  using `ActivityResultContracts.PickVisualMedia` /
  `ActivityResultContracts.TakePicture()` (camera URI comes from
  `EditorViewModel.newCameraUri()` → `ImageStorage.newCameraUri()`), a
  "Remove photo" button, and a read-only "Ready to save" summary card.

## Adding a new screen — checklist

1. Add a `@Serializable` route to `HandiNavHost.kt`.
2. Create `ui/<feature>/<Feature>Screen.kt` and `<Feature>ViewModel.kt`
   following the pattern above: one `UiState` data class, one
   `StateFlow<UiState>` in the ViewModel, mutations only through ViewModel
   functions.
3. If the screen needs recipe/catalog data, inject `RecipeRepository` /
   `CatalogRepository` — don't reach into `data.local` directly.
4. Wire the route into `HandiNavHost.kt`'s `NavHost { }` block.
5. Reuse `core.designsystem.components` (see
   [Design system](DESIGN_SYSTEM.md)) instead of duplicating `Kicker`/
   `Hairline`/etc.
