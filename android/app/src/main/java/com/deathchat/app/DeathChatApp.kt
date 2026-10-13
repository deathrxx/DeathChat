package com.deathchat.app

import android.app.Application
import com.deathchat.app.data.api.ApiClient
import com.deathchat.app.data.local.SessionPreferences
import com.deathchat.app.data.repository.RoomRepository
import com.deathchat.app.data.repository.SessionRepository
import com.deathchat.app.data.socket.SocketManager

class DeathChatApp : Application() {

    lateinit var sessionPreferences: SessionPreferences
        private set

    lateinit var apiClient: ApiClient
        private set

    lateinit var socketManager: SocketManager
        private set

    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var roomRepository: RoomRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        sessionPreferences = SessionPreferences(this)
        apiClient = ApiClient(sessionPreferences)
        socketManager = SocketManager()
        sessionRepository = SessionRepository(apiClient, sessionPreferences)
        roomRepository = RoomRepository(apiClient, socketManager, sessionPreferences)
    }

    companion object {
        lateinit var instance: DeathChatApp
            private set
    }
}
