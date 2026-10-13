package com.deathchat.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathchat.app.DeathChatApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val displayName: String = "",
    val selectedAvatar: String = "avatar_reaper",
    val serverUrl: String = "",
    val isLoading: Boolean = false,
    val isFetchingRandom: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel : ViewModel() {

    private val sessionRepo = DeathChatApp.instance.sessionRepository
    private val sessionPrefs = DeathChatApp.instance.sessionPreferences

    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            displayName = sessionPrefs.getDisplayName() ?: "",
            selectedAvatar = sessionPrefs.getAvatar(),
            serverUrl = sessionPrefs.getServerUrl()
        )
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.displayName.isBlank()) {
            rerollIdentity()
        }
    }

    fun onDisplayNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, error = null)
    }

    fun onAvatarSelected(avatar: String) {
        _uiState.value = _uiState.value.copy(selectedAvatar = avatar)
    }

    fun onServerUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
        sessionPrefs.saveServerUrl(url)
    }

    fun rerollIdentity() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingRandom = true)
            val result = sessionRepo.getRandomIdentity()
            result.fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        displayName = data.displayName,
                        selectedAvatar = data.avatar,
                        isFetchingRandom = false
                    )
                },
                onFailure = {
                    // Fallback local random generation if backend not yet contacted
                    val num = (100..999).random()
                    _uiState.value = _uiState.value.copy(
                        displayName = "ShadowSpectre_$num",
                        isFetchingRandom = false
                    )
                }
            )
        }
    }

    fun enterApp(onSuccess: () -> Unit) {
        val name = _uiState.value.displayName.trim()
        if (name.length < 2) {
            _uiState.value = _uiState.value.copy(error = "Codename must be at least 2 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = sessionRepo.createAnonymousSession(
                displayName = name,
                avatar = _uiState.value.selectedAvatar
            )

            res.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to connect to backend."
                    )
                }
            )
        }
    }
}
