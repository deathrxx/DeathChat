package com.deathchat.app.data.api.models

import com.google.gson.annotations.SerializedName

enum class MessageDeliveryStatus {
    SENDING,
    SENT,
    FAILED
}

data class MessageDto(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("roomId") val roomId: String,
    @SerializedName("senderSessionId") val senderSessionId: String,
    @SerializedName("senderDisplayName") val senderDisplayName: String,
    @SerializedName("senderAvatar") val senderAvatar: String,
    @SerializedName("content") val content: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.SENT
)

data class SendMessageRequest(
    @SerializedName("content") val content: String
)

data class TypingUpdateDto(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("isTyping") val isTyping: Boolean
)
