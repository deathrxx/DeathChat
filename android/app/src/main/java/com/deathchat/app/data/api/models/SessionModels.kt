package com.deathchat.app.data.api.models

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("requiresPassword") val requiresPassword: Boolean? = false
)

data class CreateSessionRequest(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatar") val avatar: String? = null
)

data class UpdateSessionRequest(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatar") val avatar: String? = null
)

data class SessionDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatar") val avatar: String,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null
)

data class RandomIdentityDto(
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatar") val avatar: String,
    @SerializedName("availableAvatars") val availableAvatars: List<String> = emptyList()
)
