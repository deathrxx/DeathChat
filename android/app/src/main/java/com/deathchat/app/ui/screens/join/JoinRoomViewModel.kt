package com.deathchat.app.ui.screens.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathchat.app.DeathChatApp
import com.deathchat.app.data.api.models.RoomDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JoinRoomUiState(
    val roomCode: String = "",
    val password: String = "",
    val requiresPassword: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class JoinRoomViewModel : ViewModel() {

    private val roomRepo = DeathChatApp.instance.roomRepository

    private val _uiState = MutableStateFlow(JoinRoomUiState())
    val uiState: StateFlow<JoinRoomUiState> = _uiState.asStateFlow()

    fun setInitialCode(code: String) {
        if (code.isNotBlank()) {
            _uiState.value = _uiState.value.copy(roomCode = code.uppercase())
        }
    }

    fun onCodeChanged(code: String) {
        var clean = code.trim()
        if (clean.contains("?room=")) {
            clean = clean.substringAfter("?room=").substringBefore("&")
        }
        _uiState.value = _uiState.value.copy(roomCode = clean.uppercase(), error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun joinRoom(onSuccess: (String) -> Unit) {
        val code = _uiState.value.roomCode.trim().uppercase()
        if (code.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid room code.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            roomRepo.joinRoomRealtime(
                roomId = code,
                password = if (_uiState.value.requiresPassword || _uiState.value.password.isNotEmpty()) _uiState.value.password else null
            ) { success, room, error, requiresPassword ->
                if (success && room != null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess(room.roomId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        requiresPassword = requiresPassword,
                        error = error ?: "Failed to connect to room."
                    )
                }
            }
        }
    }
}
