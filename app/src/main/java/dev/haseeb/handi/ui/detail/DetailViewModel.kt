package dev.haseeb.handi.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.model.Category
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.repository.CatalogRepository
import dev.haseeb.handi.data.repository.RecipeRepository
import dev.haseeb.handi.ui.navigation.DetailRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    val categories: Map<String, Category> = emptyMap(),
    /** Servings the user is cooking for; quantities scale against recipe.servings. */
    val servings: Int = 0,
) {
    val scale: Double
        get() = recipe?.servings?.takeIf { it > 0 && servings > 0 }?.let { servings.toDouble() / it } ?: 1.0
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipes: RecipeRepository,
    catalog: CatalogRepository,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<DetailRoute>().id
    private val servingsOverride = MutableStateFlow<Int?>(null)
    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()

    val state: StateFlow<DetailUiState> = combine(
        recipes.observe(recipeId),
        catalog.categories,
        servingsOverride,
    ) { recipe, categories, override ->
        DetailUiState(
            loading = false,
            recipe = recipe,
            categories = categories.associateBy { it.id },
            servings = override ?: recipe?.servings ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun changeServings(delta: Int) {
        val current = state.value.servings
        servingsOverride.value = (current + delta).coerceIn(1, 50)
    }

    fun delete() {
        viewModelScope.launch {
            recipes.delete(recipeId)
            _deleted.send(Unit)
        }
    }
}
