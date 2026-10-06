package dev.haseeb.handi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val query: String = "",
    val total: Int = 0,
    val recipes: List<Recipe> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: RecipeRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val state: StateFlow<HomeUiState> = combine(repository.recipes, query) { recipes, q ->
        val needle = q.trim()
        HomeUiState(
            loading = false,
            query = q,
            total = recipes.size,
            recipes = if (needle.isEmpty()) {
                recipes
            } else {
                recipes.filter { r ->
                    r.title.contains(needle, ignoreCase = true) ||
                        r.ingredients.any { it.name.contains(needle, ignoreCase = true) }
                }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }
}
