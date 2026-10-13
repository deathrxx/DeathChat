package com.deathchat.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathchat.app.DeathChatApp
import com.deathchat.app.data.api.models.RoomDto
import com.deathchat.app.data.api.models.SessionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val session: SessionDto? = null,
    val publicRooms: List<RoomDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    private val sessionRepo = DeathChatApp.instance.sessionRepository
    private val roomRepo = DeathChatApp.instance.roomRepository

    private val _uiState = MutableStateFlow(
        HomeUiState(session = sessionRepo.getCurrentSession())
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val session = sessionRepo.getCurrentSession()
            _uiState.value = _uiState.value.copy(session = session)

            val roomsResult = roomRepo.getPublicRooms(page = 1)
            roomsResult.fold(
                onSuccess = { rooms ->
                    _uiState.value = _uiState.value.copy(
                        publicRooms = rooms,
                        isLoading = false
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message
                    )
                }
            )
        }
    }
}
