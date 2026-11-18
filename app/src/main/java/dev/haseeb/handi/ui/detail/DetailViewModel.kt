package dev.haseeb.handi.ui.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.model.Category
import dev.haseeb.handi.data.model.Recipe
import dev.haseeb.handi.data.pdf.RecipePdfExporter
import dev.haseeb.handi.data.remoteconfig.FeatureFlag
import dev.haseeb.handi.data.remoteconfig.RemoteConfigRepository
import dev.haseeb.handi.data.repository.CatalogRepository
import dev.haseeb.handi.data.repository.RecipeRepository
import dev.haseeb.handi.data.share.SharedRecipeDataSource
import dev.haseeb.handi.data.share.shareLinkUri
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
    val exportingPdf: Boolean = false,
    val pdfExportEnabled: Boolean = FeatureFlag.PDF_EXPORT_ENABLED.defaultValue,
    val sharingLink: Boolean = false,
) {
    val scale: Double
        get() = recipe?.servings?.takeIf { it > 0 && servings > 0 }?.let { servings.toDouble() / it } ?: 1.0
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recipes: RecipeRepository,
    catalog: CatalogRepository,
    private val pdfExporter: RecipePdfExporter,
    private val remoteConfig: RemoteConfigRepository,
    private val sharedRecipes: SharedRecipeDataSource,
) : ViewModel() {

    private val recipeId = savedStateHandle.toRoute<DetailRoute>().id
    private val servingsOverride = MutableStateFlow<Int?>(null)
    private val exportingPdf = MutableStateFlow(false)
    private val pdfExportEnabled = MutableStateFlow(remoteConfig.isEnabled(FeatureFlag.PDF_EXPORT_ENABLED))
    private val sharingLink = MutableStateFlow(false)
    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted: Flow<Unit> = _deleted.receiveAsFlow()
    private val _shareUri = Channel<Uri>(Channel.BUFFERED)
    val shareUri: Flow<Uri> = _shareUri.receiveAsFlow()
    private val _shareError = Channel<String>(Channel.BUFFERED)
    val shareError: Flow<String> = _shareError.receiveAsFlow()
    private val _shareLink = Channel<String>(Channel.BUFFERED)
    val shareLink: Flow<String> = _shareLink.receiveAsFlow()

    val state: StateFlow<DetailUiState> = combine(
        recipes.observe(recipeId),
        catalog.categories,
        servingsOverride,
        exportingPdf,
        pdfExportEnabled,
    ) { recipe, categories, override, exporting, pdfEnabled ->
        DetailUiState(
            loading = false,
            recipe = recipe,
            categories = categories.associateBy { it.id },
            servings = override ?: recipe?.servings ?: 0,
            exportingPdf = exporting,
            pdfExportEnabled = pdfEnabled,
        )
    }.combine(sharingLink) { current, sharing -> current.copy(sharingLink = sharing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        viewModelScope.launch {
            remoteConfig.refresh()
            pdfExportEnabled.value = remoteConfig.isEnabled(FeatureFlag.PDF_EXPORT_ENABLED)
        }
    }

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

    /** Renders the recipe (scaled to the servings currently shown on screen) to a PDF and
     * sends its shareable [Uri] on [shareUri], or a message on [shareError] if it fails. */
    fun sharePdf() {
        val current = state.value
        val recipe = current.recipe ?: return
        if (current.exportingPdf || !current.pdfExportEnabled) return
        exportingPdf.value = true
        viewModelScope.launch {
            runCatching { pdfExporter.export(recipe, current.servings) }
                .onSuccess { uri -> _shareUri.send(uri) }
                .onFailure { _shareError.send(it.message ?: "Couldn't create the PDF") }
            exportingPdf.value = false
        }
    }

    /** Publishes a read-only snapshot of this recipe (issue #8) and sends its deep link on
     * [shareLink], or a message on [shareError] if it fails. Each call creates a fresh link --
     * see [SharedRecipeDataSource]'s doc comment for why an old one isn't reused or revoked. */
    fun shareLink() {
        val recipe = state.value.recipe ?: return
        if (sharingLink.value) return
        sharingLink.value = true
        viewModelScope.launch {
            runCatching { sharedRecipes.publish(recipe) }
                .onSuccess { shareId -> _shareLink.send(shareLinkUri(shareId)) }
                .onFailure { _shareError.send(it.message ?: "Couldn't create a share link") }
            sharingLink.value = false
        }
    }
}
