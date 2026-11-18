package dev.haseeb.handi.ui.shared

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.share.SharedRecipeDataSource
import dev.haseeb.handi.ui.navigation.SharedRecipeRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedRecipeUiState(
    val loading: Boolean = true,
    val recipe: Recipe? = null,
    /** True once loading finished and [recipe] is still null -- a bad link, or the share was
     * removed. Distinct from [loading] so the UI can tell "still fetching" from "not found." */
    val notFound: Boolean = false,
)

/** Read-only viewer for a link opened via [dev.haseeb.handi.ui.navigation.SharedRecipeRoute]'s
 * deep link -- issue #8. Doesn't need [dev.haseeb.handi.data.auth.AuthRepository] or any local
 * Room data: the whole point is that whoever opens the link doesn't need an account. */
@HiltViewModel
class SharedRecipeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sharedRecipes: SharedRecipeDataSource,
) : ViewModel() {

    private val shareId = savedStateHandle.toRoute<SharedRecipeRoute>().shareId
    private val _state = MutableStateFlow(SharedRecipeUiState())
    val state: StateFlow<SharedRecipeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val recipe = runCatching { sharedRecipes.fetch(shareId) }.getOrNull()
            _state.value = SharedRecipeUiState(loading = false, recipe = recipe, notFound = recipe == null)
        }
    }
}
