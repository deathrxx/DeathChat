package com.deathchat.app.data.repository

import com.deathchat.app.data.api.ApiClient
import com.deathchat.app.data.api.models.*
import com.deathchat.app.data.local.SessionPreferences
import com.deathchat.app.data.socket.RoomEvent
import com.deathchat.app.data.socket.SocketConnectionState
import com.deathchat.app.data.socket.SocketManager
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class RoomRepository(
    private val apiClient: ApiClient,
    private val socketManager: SocketManager,
    private val sessionPreferences: SessionPreferences
) {
    val connectionState: StateFlow<SocketConnectionState> = socketManager.connectionState
    val incomingMessages: SharedFlow<MessageDto> = socketManager.incomingMessages
    val typingUpdates: SharedFlow<TypingUpdateDto> = socketManager.typingUpdates
    val participantUpdates: SharedFlow<List<ParticipantDto>> = socketManager.participantUpdates
    val roomEvents: SharedFlow<RoomEvent> = socketManager.roomEvents
    val roomErrors: SharedFlow<String> = socketManager.roomErrors

    suspend fun getPublicRooms(search: String? = null, page: Int = 1): Result<List<RoomDto>> {
        return try {
            val res = apiClient.getApi().getPublicRooms(search = search, page = page)
            if (res.isSuccessful && res.body()?.success == true) {
                Result.success(res.body()?.data ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch public rooms."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRoom(
        name: String,
        description: String? = null,
        isPrivate: Boolean = false,
        password: String? = null,
        retentionSeconds: Int = 3600
    ): Result<RoomDto> {
        return try {
            val req = CreateRoomRequest(
                name = name,
                description = description,
                isPrivate = isPrivate,
                password = password,
                retentionSeconds = retentionSeconds
            )
            val res = apiClient.getApi().createRoom(req)
            val body = res.body()
            if (res.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error ?: "Failed to create room."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRoomDetails(roomId: String): Result<RoomDto> {
        return try {
            val res = apiClient.getApi().getRoom(roomId)
            val body = res.body()
            if (res.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error ?: "Room not found or expired."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMessageHistory(roomId: String, limit: Int = 50): Result<List<MessageDto>> {
        return try {
            val res = apiClient.getApi().getMessages(roomId, limit = limit)
            val body = res.body()
            if (res.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error ?: "Failed to load messages."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun ensureSocketConnected() {
        val serverUrl = sessionPreferences.getServerUrl()
        val sessionId = sessionPreferences.getSessionId() ?: return
        socketManager.connect(serverUrl, sessionId)
    }

    fun joinRoomRealtime(
        roomId: String,
        password: String? = null,
        onResult: (Boolean, RoomDto?, String?, Boolean) -> Unit
    ) {
        ensureSocketConnected()
        socketManager.joinRoom(roomId, password, onResult)
    }

    fun sendMessageRealtime(
        roomId: String,
        content: String,
        onAck: (Boolean, MessageDto?, String?) -> Unit
    ) {
        socketManager.sendMessage(roomId, content, onAck)
    }

    fun startTyping(roomId: String) = socketManager.startTyping(roomId)

    fun stopTyping(roomId: String) = socketManager.stopTyping(roomId)

    fun kickParticipant(roomId: String, targetSessionId: String, onResult: (Boolean, String?) -> Unit) {
        socketManager.kickParticipant(roomId, targetSessionId, onResult)
    }

    fun muteParticipant(
        roomId: String,
        targetSessionId: String,
        onResult: (Boolean, Boolean, String?) -> Unit
    ) {
        socketManager.muteParticipant(roomId, targetSessionId, onResult)
    }

    fun closeRoom(roomId: String, onResult: (Boolean, String?) -> Unit) {
        socketManager.closeRoom(roomId, onResult)
    }

    fun deleteRoom(roomId: String, onResult: (Boolean, String?) -> Unit) {
        socketManager.deleteRoom(roomId, onResult)
    }

    fun leaveRoom(roomId: String) {
        socketManager.leaveRoom(roomId)
    }

    suspend fun reportUser(reportedSessionId: String, roomId: String?, reason: String): Result<String> {
        return try {
            val req = CreateReportRequest(reportedSessionId, roomId, reason)
            val res = apiClient.getApi().createReport(req)
            val body = res.body()
            if (res.isSuccessful && body?.success == true) {
                Result.success(body.message ?: "Report submitted.")
            } else {
                Result.failure(Exception(body?.error ?: "Failed to submit report."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
