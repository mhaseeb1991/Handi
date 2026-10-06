package dev.haseeb.handi.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Paprika,
    onPrimary = Color.White,
    primaryContainer = PaprikaContainer,
    onPrimaryContainer = OnPaprikaContainer,
    secondary = Herb,
    onSecondary = Color.White,
    secondaryContainer = HerbContainer,
    onSecondaryContainer = OnHerbContainer,
    tertiary = Saffron,
    onTertiary = Color.White,
    tertiaryContainer = SaffronContainer,
    onTertiaryContainer = OnSaffronContainer,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperHigh,
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = PaperLowest,
    surfaceContainerLow = PaperLow,
    surfaceContainer = PaperMid,
    surfaceContainerHigh = PaperHigh,
    surfaceContainerHighest = PaperHighest,
    surfaceBright = PaperLowest,
    surfaceDim = PaperHighest,
    inverseSurface = Char,
    inverseOnSurface = Cream,
    inversePrimary = PaprikaDark,
    outline = Rule,
    outlineVariant = RuleSoft,
    scrim = Color.Black,
)

private val DarkColors = darkColorScheme(
    primary = PaprikaDark,
    onPrimary = OnPaprikaDark,
    primaryContainer = PaprikaContainerDark,
    onPrimaryContainer = OnPaprikaContainerDark,
    secondary = HerbDark,
    onSecondary = OnHerbDark,
    secondaryContainer = HerbContainerDark,
    onSecondaryContainer = OnHerbContainerDark,
    tertiary = SaffronDark,
    onTertiary = OnSaffronDark,
    tertiaryContainer = SaffronContainerDark,
    onTertiaryContainer = OnSaffronContainerDark,
    background = Char,
    onBackground = Cream,
    surface = Char,
    onSurface = Cream,
    surfaceVariant = CharHigh,
    onSurfaceVariant = CreamSoft,
    surfaceContainerLowest = CharLowest,
    surfaceContainerLow = CharLow,
    surfaceContainer = CharMid,
    surfaceContainerHigh = CharHigh,
    surfaceContainerHighest = CharHighest,
    surfaceBright = CharHighest,
    surfaceDim = CharLowest,
    inverseSurface = Cream,
    inverseOnSurface = Char,
    inversePrimary = Paprika,
    outline = RuleDark,
    outlineVariant = RuleSoftDark,
    scrim = Color.Black,
)

/**
 * Dynamic colour is intentionally NOT used: the app has its own identity
 * and should look the same on every device, in light and dark.
 */
@Composable
fun HandiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HandiTypography,
        shapes = HandiShapes,
        content = content,
    )
}
