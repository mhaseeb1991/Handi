package dev.haseeb.handi.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.haseeb.handi.data.auth.AuthRepository
import dev.haseeb.handi.data.auth.User
import dev.haseeb.handi.data.remoteconfig.FeatureFlag
import dev.haseeb.handi.data.remoteconfig.RemoteConfigRepository
import dev.haseeb.handi.data.sync.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val backupRestoreEnabled: Boolean = FeatureFlag.BACKUP_RESTORE_ENABLED.defaultValue,
    val isGoogleSignInAvailable: Boolean = false,
    val user: User? = null,
    val signingIn: Boolean = false,
    val signInError: String? = null,
    val syncing: Boolean = false,
    val syncMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val remoteConfig: RemoteConfigRepository,
    private val auth: AuthRepository,
    private val sync: SyncRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(
            backupRestoreEnabled = remoteConfig.isEnabled(FeatureFlag.BACKUP_RESTORE_ENABLED),
            isGoogleSignInAvailable = auth.isGoogleSignInAvailable,
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        // Best-effort: refresh from Remote Config, then re-read in case anything changed.
        // The initial state above already has a correct value either way.
        viewModelScope.launch {
            remoteConfig.refresh()
            _state.update { it.copy(backupRestoreEnabled = remoteConfig.isEnabled(FeatureFlag.BACKUP_RESTORE_ENABLED)) }
        }
        viewModelScope.launch {
            auth.currentUser.collect { user -> _state.update { it.copy(user = user) } }
        }
        // Kick off a sync as soon as someone signs in (including a session restored on launch) --
        // distinctUntilChangedBy(uid) so this fires once per sign-in, not on every currentUser
        // emission (e.g. Firestore's own listener churn).
        viewModelScope.launch {
            auth.currentUser.map { it?.uid }.distinctUntilChangedBy { it }.collect { uid ->
                if (uid != null) syncNow()
            }
        }
    }

    /** [activityContext] must come from the UI (`LocalContext.current`) — see
     * [AuthRepository.signInWithGoogle] for why a plain application context can't launch it. */
    fun signIn(activityContext: Context) {
        if (_state.value.signingIn) return
        _state.update { it.copy(signingIn = true, signInError = null) }
        viewModelScope.launch {
            val result = auth.signInWithGoogle(activityContext)
            _state.update {
                it.copy(
                    signingIn = false,
                    signInError = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun signOut() = auth.signOut()

    fun consumeSignInError() = _state.update { it.copy(signInError = null) }

    fun syncNow() {
        if (_state.value.syncing) return
        _state.update { it.copy(syncing = true, syncMessage = null) }
        viewModelScope.launch {
            val result = sync.syncNow()
            _state.update {
                it.copy(
                    syncing = false,
                    syncMessage = result.fold(
                        onSuccess = { "Synced: ${it.pulled} pulled, ${it.pushed} pushed" },
                        onFailure = { e -> "Couldn't sync: ${e.message}" },
                    ),
                )
            }
        }
    }

    fun consumeSyncMessage() = _state.update { it.copy(syncMessage = null) }
}
