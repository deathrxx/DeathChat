package com.deathchat.app.data.repository

import com.deathchat.app.data.api.ApiClient
import com.deathchat.app.data.api.models.CreateSessionRequest
import com.deathchat.app.data.api.models.RandomIdentityDto
import com.deathchat.app.data.api.models.SessionDto
import com.deathchat.app.data.api.models.UpdateSessionRequest
import com.deathchat.app.data.local.SessionPreferences
import kotlinx.coroutines.flow.StateFlow

class SessionRepository(
    private val apiClient: ApiClient,
    private val sessionPreferences: SessionPreferences
) {
    val sessionState: StateFlow<SessionDto?> = sessionPreferences.sessionState

    fun getCurrentSession(): SessionDto? = sessionPreferences.sessionState.value

    fun hasSession(): Boolean = !sessionPreferences.getSessionId().isNullOrBlank()

    suspend fun createAnonymousSession(
        displayName: String? = null,
        avatar: String? = null
    ): Result<SessionDto> {
        return try {
            val response = apiClient.getApi().createSession(
                CreateSessionRequest(displayName = displayName, avatar = avatar)
            )
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                sessionPreferences.saveSession(body.data)
                Result.success(body.data)
            } else {
                val errorMsg = body?.error ?: "Failed to create anonymous session (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreSession(): Result<SessionDto> {
        val sessionId = sessionPreferences.getSessionId()
        if (sessionId.isNullOrBlank()) {
            return Result.failure(Exception("No active session found."))
        }

        return try {
            val response = apiClient.getApi().getSession()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                sessionPreferences.saveSession(body.data)
                Result.success(body.data)
            } else {
                sessionPreferences.clearSession()
                Result.failure(Exception(body?.error ?: "Session expired or invalid."))
            }
        } catch (e: Exception) {
            // If offline, still return local cached session if available
            val local = sessionPreferences.sessionState.value
            if (local != null) {
                Result.success(local)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun updateIdentity(displayName: String, avatar: String): Result<SessionDto> {
        return try {
            val response = apiClient.getApi().updateSession(
                UpdateSessionRequest(displayName = displayName, avatar = avatar)
            )
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                sessionPreferences.saveSession(body.data)
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.error ?: "Failed to update identity."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomIdentity(): Result<RandomIdentityDto> {
        return try {
            val response = apiClient.getApi().getRandomIdentity()
            val body = response.body()
            if (response.isSuccessful && body?.success == true && body.data != null) {
                Result.success(body.data)
            } else {
                Result.failure(Exception("Failed to generate random identity."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearSession() {
        sessionPreferences.clearSession()
    }
}
