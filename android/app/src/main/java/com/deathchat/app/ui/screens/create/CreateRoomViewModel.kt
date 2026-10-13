package com.deathchat.app.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathchat.app.DeathChatApp
import com.deathchat.app.data.api.models.RoomDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateRoomUiState(
    val name: String = "",
    val description: String = "",
    val isPrivate: Boolean = false,
    val password: String = "",
    val retentionSeconds: Int = 3600, // Default 1 hour
    val isLoading: Boolean = false,
    val error: String? = null
)

class CreateRoomViewModel : ViewModel() {

    private val roomRepo = DeathChatApp.instance.roomRepository

    private val _uiState = MutableStateFlow(CreateRoomUiState())
    val uiState: StateFlow<CreateRoomUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.value = _uiState.value.copy(description = desc)
    }

    fun onVisibilityChanged(isPrivate: Boolean) {
        _uiState.value = _uiState.value.copy(isPrivate = isPrivate, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun onRetentionChanged(seconds: Int) {
        _uiState.value = _uiState.value.copy(retentionSeconds = seconds)
    }

    fun createRoom(onSuccess: (RoomDto) -> Unit) {
        val state = _uiState.value
        val trimmedName = state.name.trim()

        if (trimmedName.length < 2) {
            _uiState.value = _uiState.value.copy(error = "Room name must be at least 2 characters.")
            return
        }

        if (state.isPrivate && state.password.length < 4) {
            _uiState.value = _uiState.value.copy(error = "Private room password must be at least 4 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val res = roomRepo.createRoom(
                name = trimmedName,
                description = state.description.trim().ifEmpty { null },
                isPrivate = state.isPrivate,
                password = if (state.isPrivate) state.password else null,
                retentionSeconds = state.retentionSeconds
            )

            res.fold(
                onSuccess = { room ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(room)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to create room."
                    )
                }
            )
        }
    }
}
