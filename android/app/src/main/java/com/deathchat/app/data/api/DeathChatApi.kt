package com.deathchat.app.data.api

import com.deathchat.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface DeathChatApi {

    @POST("api/sessions")
    suspend fun createSession(
        @Body req: CreateSessionRequest
    ): Response<ApiResponse<SessionDto>>

    @GET("api/sessions/random-identity")
    suspend fun getRandomIdentity(): Response<ApiResponse<RandomIdentityDto>>

    @GET("api/sessions/me")
    suspend fun getSession(): Response<ApiResponse<SessionDto>>

    @PUT("api/sessions/me")
    suspend fun updateSession(
        @Body req: UpdateSessionRequest
    ): Response<ApiResponse<SessionDto>>

    @GET("api/rooms")
    suspend fun getPublicRooms(
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<PublicRoomsListResponse>

    @POST("api/rooms")
    suspend fun createRoom(
        @Body req: CreateRoomRequest
    ): Response<ApiResponse<RoomDto>>

    @GET("api/rooms/{roomId}")
    suspend fun getRoom(
        @Path("roomId") roomId: String
    ): Response<ApiResponse<RoomDto>>

    @POST("api/rooms/{roomId}/join")
    suspend fun joinRoom(
        @Path("roomId") roomId: String,
        @Body req: JoinRoomRequest
    ): Response<ApiResponse<RoomDto>>

    @GET("api/rooms/{roomId}/participants")
    suspend fun getRoomParticipants(
        @Path("roomId") roomId: String
    ): Response<ApiResponse<List<ParticipantDto>>>

    @GET("api/rooms/{roomId}/messages")
    suspend fun getMessages(
        @Path("roomId") roomId: String,
        @Query("limit") limit: Int = 50,
        @Query("before") before: String? = null
    ): Response<ApiResponse<List<MessageDto>>>

    @POST("api/rooms/{roomId}/messages")
    suspend fun sendMessage(
        @Path("roomId") roomId: String,
        @Body req: SendMessageRequest
    ): Response<ApiResponse<MessageDto>>

    @POST("api/rooms/{roomId}/close")
    suspend fun closeRoom(
        @Path("roomId") roomId: String
    ): Response<ApiResponse<Any>>

    @DELETE("api/rooms/{roomId}")
    suspend fun deleteRoom(
        @Path("roomId") roomId: String
    ): Response<ApiResponse<Any>>

    @POST("api/reports")
    suspend fun createReport(
        @Body req: CreateReportRequest
    ): Response<ApiResponse<ReportResponseData>>
}
