package dev.haseeb.handi.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.remoteconfig.FeatureFlag
import dev.haseeb.handi.data.remoteconfig.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val backupRestoreEnabled: Boolean = FeatureFlag.BACKUP_RESTORE_ENABLED.defaultValue,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(backupRestoreEnabled = remoteConfig.isEnabled(FeatureFlag.BACKUP_RESTORE_ENABLED)))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        // Best-effort: refresh from Remote Config, then re-read in case anything changed.
        // The initial state above already has a correct value either way.
        viewModelScope.launch {
            remoteConfig.refresh()
            _state.update { it.copy(backupRestoreEnabled = remoteConfig.isEnabled(FeatureFlag.BACKUP_RESTORE_ENABLED)) }
        }
    }
}
