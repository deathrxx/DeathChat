package com.deathchat.app.data.socket

import android.util.Log
import com.deathchat.app.data.api.models.*
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URI

enum class SocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

sealed class RoomEvent {
    data class Kicked(val reason: String) : RoomEvent()
    data class Muted(val isMuted: Boolean) : RoomEvent()
    object Closed : RoomEvent()
    object Deleted : RoomEvent()
    object Expired : RoomEvent()
}

class SocketManager {

    private val tag = "DeathChatSocket"
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Default)

    private var socket: Socket? = null
    private var currentServerUrl: String? = null
    private var currentSessionId: String? = null

    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<MessageDto>(extraBufferCapacity = 50)
    val incomingMessages: SharedFlow<MessageDto> = _incomingMessages.asSharedFlow()

    private val _typingUpdates = MutableSharedFlow<TypingUpdateDto>(extraBufferCapacity = 20)
    val typingUpdates: SharedFlow<TypingUpdateDto> = _typingUpdates.asSharedFlow()

    private val _participantUpdates = MutableSharedFlow<List<ParticipantDto>>(extraBufferCapacity = 20)
    val participantUpdates: SharedFlow<List<ParticipantDto>> = _participantUpdates.asSharedFlow()

    private val _roomEvents = MutableSharedFlow<RoomEvent>(extraBufferCapacity = 10)
    val roomEvents: SharedFlow<RoomEvent> = _roomEvents.asSharedFlow()

    private val _roomErrors = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val roomErrors: SharedFlow<String> = _roomErrors.asSharedFlow()

    fun connect(serverUrl: String, sessionId: String) {
        val cleanUrl = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl

        if (socket != null && socket?.connected() == true && currentServerUrl == cleanUrl && currentSessionId == sessionId) {
            Log.d(tag, "Socket already connected to $cleanUrl.")
            return
        }

        try {
            // Clean up any stale or previous socket instance
            socket?.disconnect()
            socket?.off()
            socket = null

            currentServerUrl = cleanUrl
            currentSessionId = sessionId
            _connectionState.value = SocketConnectionState.CONNECTING

            Log.i(tag, "Initializing Socket.IO connection to: $cleanUrl (Session: $sessionId)")

            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionAttempts = 20
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000
                // Use polling first, then websocket (standard Socket.IO order ensuring compatibility with emulator NAT/proxies)
                transports = arrayOf("polling", "websocket")
                auth = mapOf("token" to sessionId, "sessionId" to sessionId)
                query = "sessionId=$sessionId&token=$sessionId"
            }

            socket = IO.socket(URI.create(cleanUrl), options)
            setupListeners()
            socket?.connect()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize socket: ${e.message}", e)
            _connectionState.value = SocketConnectionState.ERROR
        }
    }

    private fun setupListeners() {
        val s = socket ?: return

        s.on(Socket.EVENT_CONNECT) {
            Log.i(tag, "Socket connected successfully to: $currentServerUrl")
            _connectionState.value = SocketConnectionState.CONNECTED
        }

        s.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args?.getOrNull(0)?.toString() ?: "unknown"
            Log.w(tag, "Socket disconnected from $currentServerUrl: $reason")
            _connectionState.value = SocketConnectionState.DISCONNECTED
        }

        s.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val errorObj = args?.getOrNull(0)
            val errorMsg = errorObj?.toString() ?: "Connection error"
            Log.e(tag, "Socket connect error to $currentServerUrl: $errorMsg")
            _connectionState.value = SocketConnectionState.ERROR
        }

        s.on("connection_status") { args ->
            Log.i(tag, "Server confirmed connection status: ${args?.getOrNull(0)}")
        }

        s.on("new_message") { args ->
            val data = args?.getOrNull(0) ?: return@on
            try {
                val jsonStr = data.toString()
                val message = gson.fromJson(jsonStr, MessageDto::class.java)
                scope.launch { _incomingMessages.emit(message) }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse new_message: ${e.message}")
            }
        }

        s.on("typing_update") { args ->
            val data = args?.getOrNull(0) ?: return@on
            try {
                val jsonStr = data.toString()
                val typing = gson.fromJson(jsonStr, TypingUpdateDto::class.java)
                scope.launch { _typingUpdates.emit(typing) }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse typing_update: ${e.message}")
            }
        }

        s.on("participants_updated") { args ->
            val data = args?.getOrNull(0) ?: return@on
            try {
                val json = JSONObject(data.toString())
                val listArray = json.optJSONArray("participants") ?: return@on
                val list = mutableListOf<ParticipantDto>()
                for (i in 0 until listArray.length()) {
                    val pJson = listArray.getJSONObject(i).toString()
                    list.add(gson.fromJson(pJson, ParticipantDto::class.java))
                }
                scope.launch { _participantUpdates.emit(list) }
            } catch (e: Exception) {
                Log.e(tag, "Failed to parse participants_updated: ${e.message}")
            }
        }

        s.on("participant_kicked") { args ->
            val data = args?.getOrNull(0)
            val reason = if (data != null) {
                try { JSONObject(data.toString()).optString("message", "You were kicked.") }
                catch (_: Exception) { "You were kicked from this room." }
            } else "You were kicked from this room."
            scope.launch { _roomEvents.emit(RoomEvent.Kicked(reason)) }
        }

        s.on("participant_muted") { args ->
            val data = args?.getOrNull(0) ?: return@on
            try {
                val isMuted = JSONObject(data.toString()).optBoolean("isMuted", false)
                scope.launch { _roomEvents.emit(RoomEvent.Muted(isMuted)) }
            } catch (_: Exception) {}
        }

        s.on("room_closed") {
            scope.launch { _roomEvents.emit(RoomEvent.Closed) }
        }

        s.on("room_deleted") {
            scope.launch { _roomEvents.emit(RoomEvent.Deleted) }
        }

        s.on("room_expired") {
            scope.launch { _roomEvents.emit(RoomEvent.Expired) }
        }

        s.on("room_error") { args ->
            val data = args?.getOrNull(0) ?: return@on
            try {
                val err = JSONObject(data.toString()).optString("error", "Room error")
                scope.launch { _roomErrors.emit(err) }
            } catch (_: Exception) {}
        }
    }

    fun joinRoom(
        roomId: String,
        password: String? = null,
        onResult: (success: Boolean, room: RoomDto?, error: String?, requiresPassword: Boolean) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            if (!password.isNullOrBlank()) {
                put("password", password)
            }
        }

        val ack = io.socket.client.Ack { args ->
            val resObj = if (args != null && args.isNotEmpty()) args[0] else null
            if (resObj != null) {
                try {
                    val json = JSONObject(resObj.toString())
                    val success = json.optBoolean("success", false)
                    if (success) {
                        val roomData = json.optJSONObject("data")?.toString()
                        val room = if (roomData != null) gson.fromJson(roomData, RoomDto::class.java) else null
                        onResult(true, room, null, false)
                    } else {
                        val err = json.optString("error", "Failed to join")
                        val reqPass = json.optBoolean("requiresPassword", false)
                        onResult(false, null, err, reqPass)
                    }
                } catch (e: Exception) {
                    onResult(false, null, e.message, false)
                }
            } else {
                onResult(false, null, "No response from server", false)
            }
        }

        val s = socket
        if (s == null) {
            onResult(false, null, "Real-time socket not initialized", false)
            return
        }

        if (s.connected()) {
            s.emit("join_room", arrayOf<Any>(payload), ack)
        } else {
            Log.d(tag, "Socket not connected yet. Waiting for connect event before emitting join_room...")
            s.once(Socket.EVENT_CONNECT) {
                Log.d(tag, "Socket connected, emitting join_room...")
                s.emit("join_room", arrayOf<Any>(payload), ack)
            }
            s.once(Socket.EVENT_CONNECT_ERROR) { args ->
                val err = args?.getOrNull(0)?.toString() ?: "Connection error"
                onResult(false, null, "Failed to connect to real-time server: $err", false)
            }
            s.connect()
        }
    }

    fun sendMessage(
        roomId: String,
        content: String,
        onAck: (success: Boolean, message: MessageDto?, error: String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("content", content)
        }

        val ack = io.socket.client.Ack { args ->
            val resObj = if (args != null && args.isNotEmpty()) args[0] else null
            if (resObj != null) {
                try {
                    val json = JSONObject(resObj.toString())
                    val success = json.optBoolean("success", false)
                    if (success) {
                        val msgData = json.optJSONObject("data")?.toString()
                        val msg = if (msgData != null) gson.fromJson(msgData, MessageDto::class.java) else null
                        onAck(true, msg, null)
                    } else {
                        onAck(false, null, json.optString("error", "Send failed"))
                    }
                } catch (e: Exception) {
                    onAck(false, null, e.message)
                }
            } else {
                onAck(false, null, "No server acknowledgement")
            }
        }

        val s = socket
        if (s == null || !s.connected()) {
            onAck(false, null, "Not connected to server")
            return
        }

        s.emit("send_message", arrayOf<Any>(payload), ack)
    }

    fun startTyping(roomId: String) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        socket?.emit("typing_start", arrayOf<Any>(payload))
    }

    fun stopTyping(roomId: String) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        socket?.emit("typing_stop", arrayOf<Any>(payload))
    }

    fun kickParticipant(roomId: String, targetSessionId: String, onResult: (Boolean, String?) -> Unit) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("targetSessionId", targetSessionId)
        }
        val ack = io.socket.client.Ack { args ->
            handleSimpleAck(args, onResult)
        }
        socket?.emit("kick_participant", arrayOf<Any>(payload), ack)
    }

    fun muteParticipant(
        roomId: String,
        targetSessionId: String,
        onResult: (Boolean, Boolean, String?) -> Unit
    ) {
        val payload = JSONObject().apply {
            put("roomId", roomId)
            put("targetSessionId", targetSessionId)
        }
        val ack = io.socket.client.Ack { args ->
            val res = if (args != null && args.isNotEmpty()) args[0] else null
            if (res != null) {
                val json = JSONObject(res.toString())
                val success = json.optBoolean("success", false)
                val isMuted = json.optBoolean("isMuted", false)
                val err = json.optString("error", null)
                onResult(success, isMuted, err)
            } else {
                onResult(false, false, "No server response")
            }
        }
        socket?.emit("mute_participant", arrayOf<Any>(payload), ack)
    }

    fun closeRoom(roomId: String, onResult: (Boolean, String?) -> Unit) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        val ack = io.socket.client.Ack { args ->
            handleSimpleAck(args, onResult)
        }
        socket?.emit("close_room", arrayOf<Any>(payload), ack)
    }

    fun deleteRoom(roomId: String, onResult: (Boolean, String?) -> Unit) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        val ack = io.socket.client.Ack { args ->
            handleSimpleAck(args, onResult)
        }
        socket?.emit("delete_room", arrayOf<Any>(payload), ack)
    }

    fun leaveRoom(roomId: String) {
        val payload = JSONObject().apply { put("roomId", roomId) }
        socket?.emit("leave_room", arrayOf<Any>(payload))
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        currentServerUrl = null
        currentSessionId = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    private fun handleSimpleAck(args: Array<out Any?>?, onResult: (Boolean, String?) -> Unit) {
        val res = if (args != null && args.isNotEmpty()) args[0] else null
        if (res != null) {
            try {
                val json = JSONObject(res.toString())
                val success = json.optBoolean("success", false)
                val err = json.optString("error", null)
                onResult(success, err)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        } else {
            onResult(false, "No response from server")
        }
    }
}
