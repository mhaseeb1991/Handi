package dev.haseeb.handi.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.haseeb.handi.core.designsystem.components.Hairline
import dev.haseeb.handi.core.designsystem.components.Kicker
import dev.haseeb.handi.core.designsystem.components.SectionTitle
import dev.haseeb.handi.data.model.QuantityFormatter
import dev.haseeb.handi.data.model.Recipe

/**
 * Read-only view of a recipe opened via a shared `handi://recipe/{shareId}` link (issue #8) --
 * no sign-in, no local Room data, no edit/delete/scale controls. Deliberately a plain
 * rendering of [Recipe], not a reuse of `DetailScreen`'s `RecipeContent` -- that one depends on
 * `DetailUiState` (servings override, category tinting from the local catalogue) that doesn't
 * apply to a recipe someone else shared.
 */
@Composable
fun SharedRecipeScreen(onBack: () -> Unit, viewModel: SharedRecipeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.notFound -> Text(
                    "This recipe link isn't available anymore.",
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                else -> state.recipe?.let { SharedRecipeContent(it) }
            }

            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        }
    }
}

@Composable
private fun SharedRecipeContent(recipe: Recipe) {
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item("title") {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 48.dp)) {
                Kicker("Shared recipe", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (recipe.description.isNotBlank()) {
                    Text(
                        recipe.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                Text(
                    "Serves ${recipe.servings} · ${recipe.cookMinutes} min",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
        item("ingredients-title") {
            SectionTitle("Ingredients", modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp))
        }
        itemsIndexed(recipe.ingredients, key = { i, _ -> "ing-$i" }) { _, item ->
            Column(Modifier.padding(horizontal = 24.dp)) {
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        if (item.note.isNotBlank()) {
                            Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        QuantityFormatter.format(item.amount, item.measure, scale = 1.0),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                    )
                }
                Hairline()
            }
        }
        if (recipe.steps.isNotEmpty()) {
            item("method-title") {
                SectionTitle("Method", modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 8.dp))
            }
            itemsIndexed(recipe.steps, key = { i, _ -> "step-$i" }) { index, step ->
                Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Text(
                        (index + 1).toString().padStart(2, '0'),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Text(step, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
