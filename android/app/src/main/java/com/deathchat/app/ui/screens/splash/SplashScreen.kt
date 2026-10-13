package com.deathchat.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deathchat.app.DeathChatApp
import com.deathchat.app.ui.navigation.Screen
import com.deathchat.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit
) {
    val sessionRepo = DeathChatApp.instance.sessionRepository

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    LaunchedEffect(Unit) {
        delay(1400) // Brief cinematic splash delay
        val hasSession = sessionRepo.hasSession()
        if (hasSession) {
            // Validate or restore in background without blocking
            sessionRepo.restoreSession()
            onNavigate(Screen.Home.route)
        } else {
            onNavigate(Screen.Onboarding.route)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2E1065), BgPrimary),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "💀",
                fontSize = 72.sp,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DEATHCHAT",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                color = TextPrimary
            )

            Text(
                text = "EPHEMERAL PROTOCOL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = CyanAccent
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "\"Enter anonymously. Talk freely. Leave no unnecessary traces.\"",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
