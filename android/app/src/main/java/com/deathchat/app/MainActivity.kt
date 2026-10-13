package com.deathchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.deathchat.app.ui.navigation.Screen
import com.deathchat.app.ui.screens.chat.ChatScreen
import com.deathchat.app.ui.screens.create.CreateRoomScreen
import com.deathchat.app.ui.screens.home.HomeScreen
import com.deathchat.app.ui.screens.join.JoinRoomScreen
import com.deathchat.app.ui.screens.onboarding.OnboardingScreen
import com.deathchat.app.ui.screens.publicrooms.PublicRoomsScreen
import com.deathchat.app.ui.screens.splash.SplashScreen
import com.deathchat.app.ui.theme.BgPrimary
import com.deathchat.app.ui.theme.DeathChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeathChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgPrimary
                ) {
                    DeathChatNavigation()
                }
            }
        }
    }
}

@Composable
fun DeathChatNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onCreateRoomClick = {
                    navController.navigate(Screen.CreateRoom.route)
                },
                onJoinRoomClick = {
                    navController.navigate(Screen.JoinRoom.createRoute())
                },
                onPublicRoomsClick = {
                    navController.navigate(Screen.PublicRooms.route)
                },
                onRoomItemClick = { roomId ->
                    navController.navigate(Screen.Chat.createRoute(roomId))
                },
                onEditIdentityClick = {
                    navController.navigate(Screen.Onboarding.route)
                }
            )
        }

        composable(Screen.CreateRoom.route) {
            CreateRoomScreen(
                onNavigateBack = { navController.popBackStack() },
                onRoomCreated = { roomId ->
                    navController.navigate(Screen.Chat.createRoute(roomId)) {
                        popUpTo(Screen.CreateRoom.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.JoinRoom.route,
            arguments = listOf(navArgument("code") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code") ?: ""
            JoinRoomScreen(
                initialCode = code,
                onNavigateBack = { navController.popBackStack() },
                onRoomJoined = { roomId ->
                    navController.navigate(Screen.Chat.createRoute(roomId)) {
                        popUpTo(Screen.JoinRoom.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PublicRooms.route) {
            PublicRoomsScreen(
                onNavigateBack = { navController.popBackStack() },
                onRoomSelected = { roomId ->
                    navController.navigate(Screen.Chat.createRoute(roomId))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("roomId") {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            ChatScreen(
                roomId = roomId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
