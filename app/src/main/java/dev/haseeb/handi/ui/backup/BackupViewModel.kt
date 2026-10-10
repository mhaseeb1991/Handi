package dev.haseeb.handi.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.backup.BackupRepository
import dev.haseeb.handi.data.backup.BackupSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val busy: Boolean = false,
    /** One-shot message for a snackbar; cleared by [BackupViewModel.consumeMessage]. */
    val message: String? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    fun export(destination: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = runCatching { repository.export(destination) }
                .fold(
                    onSuccess = { it.describeExport() },
                    onFailure = { it.describeFailure("export") },
                )
            _state.update { it.copy(busy = false, message = message) }
        }
    }

    fun import(source: Uri) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val message = runCatching { repository.import(source) }
                .fold(
                    onSuccess = { it.describeImport() },
                    onFailure = { it.describeFailure("restore") },
                )
            _state.update { it.copy(busy = false, message = message) }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    private fun BackupSummary.describeExport(): String {
        val recipePart = if (recipes == 1) "1 recipe" else "$recipes recipes"
        return "Exported $recipePart" + customIngredientsSuffix()
    }

    private fun BackupSummary.describeImport(): String {
        val recipePart = if (recipes == 1) "1 recipe" else "$recipes recipes"
        return "Restored $recipePart" + customIngredientsSuffix()
    }

    private fun BackupSummary.customIngredientsSuffix(): String = when (customIngredients) {
        0 -> ""
        1 -> " and 1 custom ingredient"
        else -> " and $customIngredients custom ingredients"
    }

    private fun Throwable.describeFailure(action: String): String =
        "Couldn't $action: ${message ?: "something went wrong"}"
}
