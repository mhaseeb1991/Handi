# Design system

`app/src/main/java/dev/haseeb/handi/core/designsystem/` — the theme and the
small set of shared Compose primitives every screen builds on. Nothing here
imports from `data.*` or `ui.*`; it's pure presentation.

## Theme — `theme/`

- **`Color.kt`** — the named colour tokens (`Paprika`, `Herb`, `Saffron`,
  `Paper`, `Ink`, `Char`, `Cream`, and their `*Container`/`*Dark`/`*Soft`
  variants). This is the file to edit to retint the app.
- **`Theme.kt`** — builds a Material 3 `lightColorScheme`/`darkColorScheme`
  from those tokens and exposes `HandiTheme { content }`. **Dynamic colour
  (Material You) is deliberately not used** — the app keeps its own paper/
  ink + paprika/herb/saffron identity regardless of the user's wallpaper, on
  every device, in both light and dark. `HandiTheme` defaults to
  `isSystemInDarkTheme()` but accepts an explicit `darkTheme: Boolean` for
  previews/tests.
- **`Type.kt`** — `HandiTypography`, a serif-headline / sans-body type scale.
- **`Shape.kt`** — `HandiShapes`, the corner-radius scale (`MaterialTheme.shapes.small/medium/large/extraLarge`
  used throughout the UI layer for cards, chips, buttons, sheets).

Every screen is wrapped once, at the root, in `MainActivity.kt`:
```kotlin
HandiTheme { Surface(...) { HandiNavHost() } }
```
Individual screens just read `MaterialTheme.colorScheme` / `.typography` /
`.shapes` — they never construct colours or type styles themselves.

## Shared components — `components/Components.kt`

| Component | What it is | Used by |
|---|---|---|
| `Kicker(text, color, modifier)` | small uppercase label above a headline, cookbook-style | Home header, Detail sections, every editor step, the quantity sheet |
| `SectionTitle(title, trailing, modifier)` | a headline with an optional right-aligned trailing label (e.g. "scaled for 6") | Detail (Ingredients/Method headers), Ingredients step ("In this recipe") |
| `Hairline(modifier)` | a 1dp `HorizontalDivider` in the outline-variant colour | Detail ingredient rows, Basics step, Ingredients step picked rows |
| `TintDot(tint, modifier, size)` | a small filled circle in a category's colour | Detail ingredient groups, category/ingredient chips, picked rows, quantity sheet |
| `RecipeImage(imagePath, title, modifier, initialSize)` | shows the recipe photo via Coil's `AsyncImage` if `imagePath != null`, otherwise a deterministic colour + first-letter placeholder (picked from `title.hashCode()`, so the same title always gets the same placeholder) | Home `RecipeCard`, Detail hero, Photo step preview |
| `HandiIllustration(modifier)` | a small hand-drawn-style `Canvas` line illustration of a *handi* (clay pot) with steam | Home empty state |
| `VSpace(height)` / `HSpace(width)` | tiny spacer helpers | available, lightly used |
| `FillBox(modifier, content)` | `Box` that fills its parent | available, lightly used |

If you need a new small reusable piece (a chip style, a label pattern) that
more than one screen will use, add it here rather than duplicating it —
`UnitChip` in `ui/editor/IngredientsStep.kt` is a borderline example that's
currently editor-only (shared between `IngredientsStep`'s custom-ingredient
dialog and `QuantityBottomSheet`) but could move here if a third screen
needs it.

## Where screen-specific UI stays local

Composables that only make sense for one screen (e.g. `RecipeCard`,
`EmptyKitchen` in `ui/home/HomeScreen.kt`; `StatsStrip`, `RoundAction` in
`ui/detail/DetailScreen.kt`; `CategoryChip`, `IngredientChip`,
`CustomIngredientDialog` in `ui/editor/IngredientsStep.kt`) are kept
`private` in their screen's file rather than promoted to the design system.
Promote something only once a second, unrelated screen genuinely needs the
same piece.
