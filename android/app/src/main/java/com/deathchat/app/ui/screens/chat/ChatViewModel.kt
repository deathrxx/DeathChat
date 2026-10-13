package com.deathchat.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deathchat.app.DeathChatApp
import com.deathchat.app.data.api.models.MessageDeliveryStatus
import com.deathchat.app.data.api.models.MessageDto
import com.deathchat.app.data.api.models.ParticipantDto
import com.deathchat.app.data.api.models.RoomDto
import com.deathchat.app.data.socket.RoomEvent
import com.deathchat.app.data.socket.SocketConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val roomId: String = "",
    val room: RoomDto? = null,
    val currentSessionId: String = "",
    val messages: List<MessageDto> = emptyList(),
    val participants: List<ParticipantDto> = emptyList(),
    val typingUsers: List<String> = emptyList(),
    val isOwner: Boolean = false,
    val isMuted: Boolean = false,
    val inputMessage: String = "",
    val isLoadingHistory: Boolean = false,
    val connectionState: SocketConnectionState = SocketConnectionState.DISCONNECTED,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val shouldExit: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val roomRepo = DeathChatApp.instance.roomRepository
    private val sessionRepo = DeathChatApp.instance.sessionRepository

    private val _uiState = MutableStateFlow(
        ChatUiState(currentSessionId = sessionRepo.getCurrentSession()?.sessionId ?: "")
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingJob: Job? = null
    private var isCurrentlyTyping = false

    fun initRoom(roomId: String) {
        _uiState.value = _uiState.value.copy(
            roomId = roomId,
            currentSessionId = sessionRepo.getCurrentSession()?.sessionId ?: ""
        )

        observeSocketStreams()
        loadRoomData(roomId)
    }

    private fun loadRoomData(roomId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)

            // 1. Fetch room details
            val roomRes = roomRepo.getRoomDetails(roomId)
            roomRes.onSuccess { room ->
                val mySessionId = sessionRepo.getCurrentSession()?.sessionId
                val isOwner = room.creatorSessionId == mySessionId
                _uiState.value = _uiState.value.copy(room = room, isOwner = isOwner)
            }

            // 2. Fetch recent message history
            val msgRes = roomRepo.loadMessageHistory(roomId)
            msgRes.onSuccess { list ->
                _uiState.value = _uiState.value.copy(
                    messages = list,
                    isLoadingHistory = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingHistory = false)
            }

            // 3. Connect socket & join room real-time
            roomRepo.joinRoomRealtime(roomId) { success, roomDto, error, _ ->
                if (success && roomDto != null) {
                    val isOwner = roomDto.isOwner || (roomDto.creatorSessionId == sessionRepo.getCurrentSession()?.sessionId)
                    _uiState.value = _uiState.value.copy(
                        room = roomDto,
                        isOwner = isOwner,
                        participants = roomDto.participants
                    )
                } else if (error != null) {
                    _uiState.value = _uiState.value.copy(errorMessage = error)
                }
            }
        }
    }

    private fun observeSocketStreams() {
        // Connection state
        viewModelScope.launch {
            roomRepo.connectionState.collect { conn ->
                _uiState.value = _uiState.value.copy(connectionState = conn)
            }
        }

        // Real-time messages
        viewModelScope.launch {
            roomRepo.incomingMessages.collect { msg ->
                if (msg.roomId.equals(_uiState.value.roomId, ignoreCase = true)) {
                    val currentList = _uiState.value.messages.toMutableList()
                    // Avoid duplicates
                    val existingIndex = currentList.indexOfFirst { it.messageId == msg.messageId }
                    if (existingIndex >= 0) {
                        currentList[existingIndex] = msg
                    } else {
                        currentList.add(msg)
                    }
                    _uiState.value = _uiState.value.copy(messages = currentList)
                }
            }
        }

        // Typing updates
        viewModelScope.launch {
            roomRepo.typingUpdates.collect { update ->
                val myId = _uiState.value.currentSessionId
                if (update.sessionId != myId) {
                    val typers = _uiState.value.typingUsers.toMutableList()
                    if (update.isTyping) {
                        if (!typers.contains(update.displayName)) typers.add(update.displayName)
                    } else {
                        typers.remove(update.displayName)
                    }
                    _uiState.value = _uiState.value.copy(typingUsers = typers)
                }
            }
        }

        // Participant updates
        viewModelScope.launch {
            roomRepo.participantUpdates.collect { list ->
                _uiState.value = _uiState.value.copy(participants = list)
            }
        }

        // Room events
        viewModelScope.launch {
            roomRepo.roomEvents.collect { event ->
                when (event) {
                    is RoomEvent.Kicked -> {
                        _uiState.value = _uiState.value.copy(
                            infoMessage = event.reason,
                            shouldExit = true
                        )
                    }
                    is RoomEvent.Muted -> {
                        _uiState.value = _uiState.value.copy(
                            isMuted = event.isMuted,
                            infoMessage = if (event.isMuted) "You have been muted." else "You have been unmuted."
                        )
                    }
                    is RoomEvent.Closed -> {
                        _uiState.value = _uiState.value.copy(
                            infoMessage = "Room closed to new participants."
                        )
                    }
                    is RoomEvent.Deleted -> {
                        _uiState.value = _uiState.value.copy(
                            infoMessage = "Room deleted by owner.",
                            shouldExit = true
                        )
                    }
                    is RoomEvent.Expired -> {
                        _uiState.value = _uiState.value.copy(
                            infoMessage = "Room has expired (24-hour limit reached).",
                            shouldExit = true
                        )
                    }
                }
            }
        }

        // Room errors
        viewModelScope.launch {
            roomRepo.roomErrors.collect { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err)
            }
        }
    }

    fun onInputMessageChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputMessage = text)

        if (text.isNotBlank() && !isCurrentlyTyping) {
            isCurrentlyTyping = true
            roomRepo.startTyping(_uiState.value.roomId)
        }

        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            delay(2000)
            if (isCurrentlyTyping) {
                isCurrentlyTyping = false
                roomRepo.stopTyping(_uiState.value.roomId)
            }
        }
    }

    fun sendMessage() {
        val content = _uiState.value.inputMessage.trim()
        if (content.isBlank()) return

        val roomId = _uiState.value.roomId
        val session = sessionRepo.getCurrentSession() ?: return

        // Temporary optimistic message
        val tempId = UUID.randomUUID().toString()
        val tempMessage = MessageDto(
            messageId = tempId,
            roomId = roomId,
            senderSessionId = session.sessionId,
            senderDisplayName = session.displayName,
            senderAvatar = session.avatar,
            content = content,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
            deliveryStatus = MessageDeliveryStatus.SENDING
        )

        val updatedList = _uiState.value.messages + tempMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedList,
            inputMessage = ""
        )

        // Stop typing indicator
        isCurrentlyTyping = false
        roomRepo.stopTyping(roomId)

        // Emit through real-time socket with server ack
        roomRepo.sendMessageRealtime(roomId, content) { success, confirmedMsg, error ->
            val list = _uiState.value.messages.toMutableList()
            val index = list.indexOfFirst { it.messageId == tempId }

            if (success && confirmedMsg != null) {
                if (index >= 0) {
                    list[index] = confirmedMsg.copy(deliveryStatus = MessageDeliveryStatus.SENT)
                } else {
                    list.add(confirmedMsg)
                }
                _uiState.value = _uiState.value.copy(messages = list)
            } else {
                if (index >= 0) {
                    list[index] = list[index].copy(deliveryStatus = MessageDeliveryStatus.FAILED)
                }
                _uiState.value = _uiState.value.copy(
                    messages = list,
                    errorMessage = error ?: "Failed to deliver message."
                )
            }
        }
    }

    fun kickParticipant(targetSessionId: String) {
        roomRepo.kickParticipant(_uiState.value.roomId, targetSessionId) { success, err ->
            if (!success && err != null) {
                _uiState.value = _uiState.value.copy(errorMessage = err)
            }
        }
    }

    fun muteParticipant(targetSessionId: String) {
        roomRepo.muteParticipant(_uiState.value.roomId, targetSessionId) { success, _, err ->
            if (!success && err != null) {
                _uiState.value = _uiState.value.copy(errorMessage = err)
            }
        }
    }

    fun closeRoom() {
        roomRepo.closeRoom(_uiState.value.roomId) { success, err ->
            if (success) {
                _uiState.value = _uiState.value.copy(infoMessage = "Room closed to new participants.")
            } else if (err != null) {
                _uiState.value = _uiState.value.copy(errorMessage = err)
            }
        }
    }

    fun deleteRoom() {
        roomRepo.deleteRoom(_uiState.value.roomId) { success, err ->
            if (success) {
                _uiState.value = _uiState.value.copy(shouldExit = true)
            } else if (err != null) {
                _uiState.value = _uiState.value.copy(errorMessage = err)
            }
        }
    }

    fun reportUser(targetSessionId: String, reason: String) {
        viewModelScope.launch {
            val res = roomRepo.reportUser(targetSessionId, _uiState.value.roomId, reason)
            res.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(infoMessage = msg)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(errorMessage = err.message)
                }
            )
        }
    }

    fun leaveRoom() {
        roomRepo.leaveRoom(_uiState.value.roomId)
        _uiState.value = _uiState.value.copy(shouldExit = true)
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
