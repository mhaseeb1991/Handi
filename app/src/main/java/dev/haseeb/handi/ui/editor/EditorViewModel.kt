package dev.haseeb.handi.ui.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.image.ImageStorage
import dev.haseeb.handi.data.model.Ingredient
import dev.haseeb.handi.data.model.Measure
import dev.haseeb.handi.data.model.QuantityFormatter
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.model.RecipeIngredient
import dev.haseeb.handi.data.repository.CatalogRepository
import dev.haseeb.handi.data.repository.RecipeRepository
import dev.haseeb.handi.ui.navigation.EditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

sealed interface EditorEvent {
    data class Saved(val id: Long, val wasNew: Boolean) : EditorEvent
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipes: RecipeRepository,
    private val catalog: CatalogRepository,
    private val images: ImageStorage,
) : ViewModel() {

    private val editingId = savedStateHandle.toRoute<EditorRoute>().id

    private val _state = MutableStateFlow(EditorUiState(recipeId = editingId))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<EditorEvent?>(null)
    val event: StateFlow<EditorEvent?> = _event.asStateFlow()

    /** Photos imported during this session but not (yet) saved; cleaned up on replace / cancel. */
    private val sessionImages = mutableSetOf<String>()
    private var stepIds = 1L
    private var saved = false

    init {
        viewModelScope.launch {
            combine(catalog.categories, catalog.ingredientsByCategory) { c, i -> c to i }
                .collect { (categories, byCategory) ->
                    _state.update { it.copy(categories = categories, catalog = byCategory) }
                }
        }
        viewModelScope.launch {
            val existing = if (editingId != 0L) recipes.get(editingId) else null
            _state.update { s ->
                if (existing == null) {
                    s.copy(loading = false)
                } else {
                    s.copy(
                        loading = false,
                        title = existing.title,
                        description = existing.description,
                        servings = existing.servings,
                        cookMinutes = existing.cookMinutes,
                        imagePath = existing.imagePath,
                        picked = existing.ingredients.map {
                            PickedIngredient(it.name, it.categoryId, it.amount, it.measure, it.note)
                        },
                        steps = existing.steps.map { StepDraft(++stepIds, it) }.ifEmpty { s.steps },
                    )
                }
            }
        }
    }

    // ---- Basics -------------------------------------------------------------------------------

    fun setTitle(value: String) = _state.update { it.copy(title = value.take(80)) }
    fun setDescription(value: String) = _state.update { it.copy(description = value.take(240)) }
    fun changeServings(delta: Int) = _state.update { it.copy(servings = (it.servings + delta).coerceIn(1, 50)) }
    fun changeMinutes(delta: Int) = _state.update { it.copy(cookMinutes = (it.cookMinutes + delta).coerceIn(0, 24 * 60)) }

    // ---- Step navigation ----------------------------------------------------------------------

    fun goTo(step: EditorStep) = _state.update { if (it.canOpen(step)) it.copy(step = step) else it }

    fun next() = _state.update { s ->
        val next = EditorStep.entries.getOrNull(s.step.ordinal + 1)
        if (next != null && s.isComplete(s.step)) s.copy(step = next) else s
    }

    /** @return false when already on the first step. */
    fun previous(): Boolean {
        val prev = EditorStep.entries.getOrNull(_state.value.step.ordinal - 1) ?: return false
        _state.update { it.copy(step = prev) }
        return true
    }

    // ---- Ingredients --------------------------------------------------------------------------

    fun selectCategory(id: String) = _state.update {
        it.copy(selectedCategoryId = if (it.selectedCategoryId == id) null else id, ingredientQuery = "")
    }

    fun setIngredientQuery(value: String) = _state.update { it.copy(ingredientQuery = value) }

    fun openIngredient(ingredient: Ingredient) = _state.update { s ->
        val existing = s.picked.firstOrNull { it.key == PickedIngredient.keyOf(ingredient.categoryId, ingredient.name) }
        s.copy(sheet = existing?.toSheet() ?: newSheet(ingredient.name, ingredient.categoryId, ingredient.defaultMeasure))
    }

    fun openPicked(item: PickedIngredient) = _state.update { it.copy(sheet = item.toSheet()) }

    fun dismissSheet() = _state.update { it.copy(sheet = null) }

    fun setSheetAmount(text: String) = updateSheet { it.copy(amountText = text.take(8)) }

    fun nudgeSheetAmount(up: Boolean) = updateSheet { sheet ->
        val step = stepFor(sheet.measure)
        val current = sheet.parsedAmount ?: 0.0
        val next = if (up) current + step else (current - step).coerceAtLeast(step)
        sheet.copy(amountText = QuantityFormatter.amount(next))
    }

    fun setSheetMeasure(measure: Measure) = updateSheet { sheet ->
        val amount = when {
            !measure.needsAmount -> sheet.amountText
            sheet.parsedAmount == null -> defaultAmountFor(measure)
            else -> sheet.amountText
        }
        sheet.copy(measure = measure, amountText = amount)
    }

    fun setSheetNote(note: String) = updateSheet { it.copy(note = note.take(60)) }

    fun confirmSheet() = _state.update { s ->
        val sheet = s.sheet ?: return@update s
        if (!sheet.isValid) return@update s
        val item = PickedIngredient(
            name = sheet.name,
            categoryId = sheet.categoryId,
            amount = if (sheet.measure.needsAmount) sheet.parsedAmount else null,
            measure = sheet.measure,
            note = sheet.note.trim(),
        )
        val index = s.picked.indexOfFirst { it.key == item.key }
        val picked = if (index >= 0) s.picked.toMutableList().apply { set(index, item) } else s.picked + item
        s.copy(picked = picked, sheet = null)
    }

    fun removeFromSheet() = _state.update { s ->
        val sheet = s.sheet ?: return@update s
        val key = PickedIngredient.keyOf(sheet.categoryId, sheet.name)
        s.copy(picked = s.picked.filterNot { it.key == key }, sheet = null)
    }

    fun removePicked(key: String) = _state.update { s -> s.copy(picked = s.picked.filterNot { it.key == key }) }

    /** Saves a custom ingredient to the catalogue for reuse and opens its quantity sheet. */
    fun addCustomIngredient(name: String, measure: Measure) {
        val categoryId = _state.value.selectedCategoryId ?: return
        val clean = name.trim().replaceFirstChar { it.uppercase() }
        if (clean.isEmpty()) return
        viewModelScope.launch {
            catalog.addCustomIngredient(categoryId, clean, measure)
            _state.update { it.copy(ingredientQuery = "", sheet = newSheet(clean, categoryId, measure)) }
        }
    }

    // ---- Method -------------------------------------------------------------------------------

    fun addStep() = _state.update { it.copy(steps = it.steps + StepDraft(++stepIds, "")) }

    fun updateStep(id: Long, text: String) = _state.update { s ->
        s.copy(steps = s.steps.map { if (it.id == id) it.copy(text = text) else it })
    }

    fun removeStep(id: Long) = _state.update { s ->
        val remaining = s.steps.filterNot { it.id == id }
        s.copy(steps = remaining.ifEmpty { listOf(StepDraft(++stepIds, "")) })
    }

    fun moveStep(id: Long, up: Boolean) = _state.update { s ->
        val from = s.steps.indexOfFirst { it.id == id }
        val to = if (up) from - 1 else from + 1
        if (from < 0 || to !in s.steps.indices) return@update s
        s.copy(steps = s.steps.toMutableList().apply { add(to, removeAt(from)) })
    }

    // ---- Photo --------------------------------------------------------------------------------

    fun newCameraUri(): Uri = images.newCameraUri()

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(importingImage = true) }
            val path = images.import(uri)
            val previous = _state.value.imagePath
            if (path != null) {
                sessionImages += path
                discardIfUnsaved(previous)
            }
            _state.update { it.copy(importingImage = false, imagePath = path ?: it.imagePath) }
        }
    }

    fun removeImage() {
        val previous = _state.value.imagePath
        _state.update { it.copy(imagePath = null) }
        viewModelScope.launch { discardIfUnsaved(previous) }
    }

    private suspend fun discardIfUnsaved(path: String?) {
        if (path != null && sessionImages.remove(path)) images.delete(path)
    }

    // ---- Save ---------------------------------------------------------------------------------

    fun save() {
        val s = _state.value
        if (s.saving || !s.isComplete(EditorStep.BASICS) || !s.isComplete(EditorStep.INGREDIENTS)) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = recipes.save(
                Recipe(
                    id = s.recipeId,
                    title = s.title,
                    description = s.description,
                    imagePath = s.imagePath,
                    servings = s.servings,
                    cookMinutes = s.cookMinutes,
                    ingredients = s.picked.map {
                        RecipeIngredient(it.name, it.categoryId, it.amount, it.measure, it.note)
                    },
                    steps = s.steps.map { it.text },
                ),
            )
            saved = true
            sessionImages.clear()
            _event.value = EditorEvent.Saved(id, wasNew = !s.isEditing)
        }
    }

    fun consumeEvent() {
        _event.value = null
    }

    override fun onCleared() {
        // Leaving without saving: drop any photo imported during this session.
        if (!saved && sessionImages.isNotEmpty()) {
            val leftovers = sessionImages.toList()
            runBlocking { leftovers.forEach { images.delete(it) } }
        }
    }

    // ---- helpers ------------------------------------------------------------------------------

    private inline fun updateSheet(crossinline block: (QuantitySheet) -> QuantitySheet) =
        _state.update { s -> s.sheet?.let { s.copy(sheet = block(it)) } ?: s }

    private fun PickedIngredient.toSheet() = QuantitySheet(
        name = name,
        categoryId = categoryId,
        amountText = amount?.let(QuantityFormatter::amount) ?: "",
        measure = measure,
        note = note,
        isExisting = true,
    )

    private fun newSheet(name: String, categoryId: String, measure: Measure) = QuantitySheet(
        name = name,
        categoryId = categoryId,
        amountText = defaultAmountFor(measure),
        measure = measure,
        note = "",
        isExisting = false,
    )

    companion object {
        fun defaultAmountFor(measure: Measure): String = when (measure) {
            Measure.GRAM, Measure.MILLILITRE -> "100"
            Measure.TO_TASTE -> ""
            else -> "1"
        }

        fun stepFor(measure: Measure): Double = when (measure) {
            Measure.GRAM, Measure.MILLILITRE -> 50.0
            Measure.KILOGRAM, Measure.LITRE, Measure.CUP, Measure.TABLESPOON, Measure.TEASPOON -> 0.5
            else -> 1.0
        }

        fun quickAmounts(measure: Measure): List<String> = when (measure) {
            Measure.GRAM, Measure.MILLILITRE -> listOf("50", "100", "250", "500")
            Measure.TO_TASTE -> emptyList()
            else -> listOf("¼", "½", "1", "2", "3")
        }
    }
}
