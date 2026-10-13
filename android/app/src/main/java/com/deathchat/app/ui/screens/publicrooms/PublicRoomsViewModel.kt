package com.deathchat.app.ui.screens.publicrooms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathchat.app.DeathChatApp
import com.deathchat.app.data.api.models.RoomDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PublicRoomsUiState(
    val rooms: List<RoomDto> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class PublicRoomsViewModel : ViewModel() {

    private val roomRepo = DeathChatApp.instance.roomRepository

    private val _uiState = MutableStateFlow(PublicRoomsUiState())
    val uiState: StateFlow<PublicRoomsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadRooms()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadRooms(query.trim().ifEmpty { null })
        }
    }

    fun loadRooms(query: String? = _uiState.value.searchQuery.ifEmpty { null }) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val res = roomRepo.getPublicRooms(search = query, page = 1)
            res.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        rooms = list,
                        isLoading = false
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to load public rooms."
                    )
                }
            )
        }
    }
}
