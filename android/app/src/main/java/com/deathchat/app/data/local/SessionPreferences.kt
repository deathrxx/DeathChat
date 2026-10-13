package com.deathchat.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.deathchat.app.data.api.models.SessionDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("deathchat_prefs", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<SessionDto?>(loadSession())
    val sessionState: StateFlow<SessionDto?> = _sessionState.asStateFlow()

    private val _serverUrlState = MutableStateFlow(getServerUrl())
    val serverUrlState: StateFlow<String> = _serverUrlState.asStateFlow()

    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun getAvatar(): String = prefs.getString(KEY_AVATAR, "avatar_reaper") ?: "avatar_reaper"

    fun getServerUrl(): String {
        val saved = prefs.getString(KEY_SERVER_URL, null)
        // Auto-migrate from old 10.0.2.2 emulator default if previously stored in SharedPreferences
        if (saved.isNullOrBlank() || saved == "http://10.0.2.2:3000") {
            return DEFAULT_URL
        }
        return saved
    }

    fun saveSession(session: SessionDto) {
        prefs.edit()
            .putString(KEY_SESSION_ID, session.sessionId)
            .putString(KEY_DISPLAY_NAME, session.displayName)
            .putString(KEY_AVATAR, session.avatar)
            .apply()
        _sessionState.value = session
    }

    fun updateIdentity(displayName: String, avatar: String) {
        val current = _sessionState.value
        val updated = current?.copy(displayName = displayName, avatar = avatar)
            ?: SessionDto(
                sessionId = getSessionId() ?: "",
                displayName = displayName,
                avatar = avatar
            )
        saveSession(updated)
    }

    fun saveServerUrl(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        prefs.edit().putString(KEY_SERVER_URL, cleanUrl).apply()
        _serverUrlState.value = cleanUrl
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_AVATAR)
            .apply()
        _sessionState.value = null
    }

    private fun loadSession(): SessionDto? {
        val id = prefs.getString(KEY_SESSION_ID, null) ?: return null
        val name = prefs.getString(KEY_DISPLAY_NAME, "Anonymous") ?: "Anonymous"
        val avatar = prefs.getString(KEY_AVATAR, "avatar_reaper") ?: "avatar_reaper"
        return SessionDto(sessionId = id, displayName = name, avatar = avatar)
    }

    companion object {
        private const val KEY_SESSION_ID = "key_session_id"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_AVATAR = "key_avatar"
        private const val KEY_SERVER_URL = "key_server_url"

        // Default to Windows host LAN IP (192.168.31.251:3000) for MSI App Player / BlueStacks
        const val DEFAULT_URL = "http://192.168.31.251:3000"
    }
}
