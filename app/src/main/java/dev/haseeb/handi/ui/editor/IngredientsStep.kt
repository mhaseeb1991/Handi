package dev.haseeb.handi.ui.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.haseeb.handi.core.designsystem.components.Hairline
import dev.haseeb.handi.core.designsystem.components.Kicker
import dev.haseeb.handi.core.designsystem.components.SectionTitle
import dev.haseeb.handi.core.designsystem.components.TintDot
import dev.haseeb.handi.data.model.Category
import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientsStep(
    state: EditorUiState,
    onCategory: (String) -> Unit,
    onQuery: (String) -> Unit,
    onIngredient: (Ingredient) -> Unit,
    onPicked: (PickedIngredient) -> Unit,
    onRemovePicked: (String) -> Unit,
    onAddCustom: (String, Measure) -> Unit,
) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val tints = state.categories.associate { it.id to it.tint }
    val pickedKeys = state.pickedKeys

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item("pick-title") {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)) {
                Kicker("1 · Choose a category", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item("categories") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryChip(
                        category = category,
                        selected = category.id == state.selectedCategoryId,
                        count = state.picked.count { it.categoryId == category.id },
                        onClick = { onCategory(category.id) },
                    )
                }
            }
        }

        val selected = state.selectedCategory
        if (selected == null) {
            item("hint") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                ) {
                    Text(
                        "Tap a category above — vegetables, masala, meat… — to see its ingredients.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            item("search") {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Kicker("2 · Tap ingredients to add", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.ingredientQuery,
                        onValueChange = onQuery,
                        singleLine = true,
                        placeholder = { Text("Search ${selected.name.lowercase()}") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
                    )
                }
            }
            item("ingredients-${selected.id}") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    state.visibleIngredients.forEach { ingredient ->
                        IngredientChip(
                            name = ingredient.name,
                            selected = PickedIngredient.keyOf(ingredient.categoryId, ingredient.name) in pickedKeys,
                            custom = ingredient.isCustom,
                            onClick = { onIngredient(ingredient) },
                        )
                    }
                    AddCustomChip(onClick = { showCustom = true })
                }
            }
        }

        if (state.picked.isNotEmpty()) {
            item("picked-title") {
                SectionTitle(
                    "In this recipe",
                    trailing = "${state.picked.size} added",
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 4.dp),
                )
            }
            items(state.picked, key = { "picked-${it.key}" }) { item ->
                PickedRow(
                    item = item,
                    tint = tints[item.categoryId] ?: 0xFF8C8074,
                    onClick = { onPicked(item) },
                    onRemove = { onRemovePicked(item.key) },
                )
            }
        }
    }

    if (showCustom && state.selectedCategory != null) {
        CustomIngredientDialog(
            categoryName = state.selectedCategory!!.name,
            onDismiss = { showCustom = false },
            onAdd = { name, measure ->
                showCustom = false
                onAddCustom(name, measure)
            },
        )
    }
}

@Composable
private fun CategoryChip(category: Category, selected: Boolean, count: Int, onClick: () -> Unit) {
    val tint = Color(category.tint)
    val container by animateColorAsState(
        if (selected) tint else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "category-bg",
    )
    val content = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
    ) {
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        } else {
            TintDot(category.tint)
        }
        Text(
            category.name,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (count > 0) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) tint else Color.White,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else tint)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun IngredientChip(name: String, selected: Boolean, custom: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) scheme.primaryContainer else scheme.surface,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, scheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp))
            }
            Text(name, style = MaterialTheme.typography.bodyMedium)
            if (custom) {
                Text(
                    " · yours",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun AddCustomChip(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, scheme.primary, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
        Text("Custom", style = MaterialTheme.typography.labelLarge, color = scheme.primary, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun PickedRow(item: PickedIngredient, tint: Long, onClick: () -> Unit, onRemove: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(start = 24.dp, end = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            TintDot(tint, size = 8.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                if (item.note.isNotBlank()) {
                    Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                item.quantityLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove ${item.name}", modifier = Modifier.size(18.dp))
            }
        }
        Hairline(Modifier.padding(end = 16.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomIngredientDialog(
    categoryName: String,
    onDismiss: () -> Unit,
    onAdd: (String, Measure) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var measure by rememberSaveable { mutableStateOf(Measure.GRAM) }
    val quickUnits = listOf(Measure.GRAM, Measure.PIECE, Measure.CUP, Measure.TABLESPOON, Measure.TEASPOON, Measure.MILLILITRE)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to $categoryName") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Ingredient name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Kicker("Usually measured in", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickUnits.forEach { unit -> UnitChip(unit, selected = unit == measure, onClick = { measure = unit }) }
                }
                Text(
                    "It’ll be saved in this category for your next recipes too.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, measure) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun UnitChip(measure: Measure, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (selected) scheme.onSurface else scheme.surfaceContainerLow,
        contentColor = if (selected) scheme.surface else scheme.onSurface,
    ) {
        Text(
            measure.short,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}
