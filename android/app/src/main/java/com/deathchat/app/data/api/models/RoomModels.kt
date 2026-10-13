package com.deathchat.app.data.api.models

import com.google.gson.annotations.SerializedName

data class CreateRoomRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("isPrivate") val isPrivate: Boolean = false,
    @SerializedName("password") val password: String? = null,
    @SerializedName("retentionSeconds") val retentionSeconds: Int = 3600
)

data class JoinRoomRequest(
    @SerializedName("password") val password: String? = null
)

data class RoomDto(
    @SerializedName("roomId") val roomId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("isPrivate") val isPrivate: Boolean = false,
    @SerializedName("creatorSessionId") val creatorSessionId: String? = null,
    @SerializedName("messageRetentionSeconds") val messageRetentionSeconds: Int = 3600,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("isClosed") val isClosed: Boolean = false,
    @SerializedName("isOwner") val isOwner: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("participantCount") val participantCount: Int = 0,
    @SerializedName("participants") val participants: List<ParticipantDto> = emptyList()
)

data class ParticipantDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("avatar") val avatar: String,
    @SerializedName("isOwner") val isOwner: Boolean = false,
    @SerializedName("joinedAt") val joinedAt: String? = null
)

data class PaginationDto(
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20,
    @SerializedName("total") val total: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 1
)

data class PublicRoomsListResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<RoomDto> = emptyList(),
    @SerializedName("pagination") val pagination: PaginationDto? = null
)
