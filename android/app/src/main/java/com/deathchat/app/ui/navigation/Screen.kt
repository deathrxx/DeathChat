package com.deathchat.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object CreateRoom : Screen("create_room")
    object JoinRoom : Screen("join_room?code={code}") {
        fun createRoute(code: String = ""): String = "join_room?code=$code"
    }
    object PublicRooms : Screen("public_rooms")
    object Chat : Screen("chat/{roomId}") {
        fun createRoute(roomId: String): String = "chat/$roomId"
    }
}
