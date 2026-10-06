package dev.haseeb.handi.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.haseeb.handi.core.designsystem.components.Hairline
import dev.haseeb.handi.core.designsystem.components.Kicker
import dev.haseeb.handi.core.designsystem.components.RecipeImage
import dev.haseeb.handi.core.designsystem.components.SectionTitle
import dev.haseeb.handi.core.designsystem.components.TintDot
import dev.haseeb.handi.data.model.QuantityFormatter
import dev.haseeb.handi.data.model.Recipe

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }

    val recipe = state.recipe
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (recipe != null) {
            RecipeContent(
                state = state,
                recipe = recipe,
                onServingsChange = viewModel::changeServings,
            )
            ExtendedFloatingActionButton(
                onClick = { onEdit(recipe.id) },
                icon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                text = { Text("Edit recipe") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp),
            )
        } else if (!state.loading) {
            Text(
                "This recipe no longer exists.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            RoundAction(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            if (recipe != null) {
                RoundAction(Icons.Rounded.DeleteOutline, "Delete recipe") { confirmDelete = true }
            }
        }
    }

    if (confirmDelete && recipe != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete “${recipe.title}”?") },
            text = { Text("The recipe and its photo will be removed from this device. This can’t be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
    }
}

@Composable
private fun RecipeContent(
    state: DetailUiState,
    recipe: Recipe,
    onServingsChange: (Int) -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val grouped = recipe.ingredients.groupBy { it.categoryId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        item("hero") {
            Box(Modifier.fillMaxWidth().height(380.dp)) {
                RecipeImage(
                    imagePath = recipe.imagePath,
                    title = recipe.title,
                    initialSize = 140,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to background,
                            ),
                        ),
                )
            }
        }
        item("title") {
            Column(Modifier.padding(horizontal = 24.dp)) {
                Kicker("${recipe.ingredients.size} ingredients · ${recipe.steps.size} steps")
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
                StatsStrip(
                    servings = state.servings,
                    minutes = recipe.cookMinutes,
                    onServingsChange = onServingsChange,
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
                )
            }
        }

        item("ingredients-title") {
            SectionTitle(
                "Ingredients",
                trailing = if (state.scale != 1.0) "scaled for ${state.servings}" else null,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
            )
        }
        grouped.forEach { (categoryId, items) ->
            val category = state.categories[categoryId]
            item("cat-$categoryId") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 4.dp),
                ) {
                    TintDot(category?.tint ?: 0xFF8C8074, size = 8.dp)
                    Kicker(
                        category?.name ?: "Other",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            itemsIndexed(items, key = { i, ing -> "ing-$categoryId-$i-${ing.name}" }) { _, item ->
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            if (item.note.isNotBlank()) {
                                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            QuantityFormatter.format(item.amount, item.measure, state.scale),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 64.dp),
                        )
                    }
                    Hairline()
                }
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
                        modifier = Modifier.width(52.dp),
                    )
                    Text(
                        step,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsStrip(
    servings: Int,
    minutes: Int,
    onServingsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
                Kicker("Serves", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onServingsChange(-1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Fewer servings")
                    }
                    Text("$servings", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { onServingsChange(1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = "More servings")
                    }
                }
            }
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Kicker("Time", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "$minutes min",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}
