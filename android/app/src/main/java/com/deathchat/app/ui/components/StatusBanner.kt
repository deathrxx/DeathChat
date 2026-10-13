package com.deathchat.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deathchat.app.data.socket.SocketConnectionState
import com.deathchat.app.ui.theme.ErrorRed
import com.deathchat.app.ui.theme.SuccessGreen
import com.deathchat.app.ui.theme.WarningAmber

@Composable
fun StatusBanner(
    connectionState: SocketConnectionState,
    modifier: Modifier = Modifier
) {
    val isVisible = connectionState != SocketConnectionState.CONNECTED

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val (bgColor, textColor, text) = when (connectionState) {
            SocketConnectionState.CONNECTED -> Triple(
                Color(0x2210B981),
                SuccessGreen,
                "● Connected to Ephemeral Network"
            )
            SocketConnectionState.CONNECTING -> Triple(
                Color(0x33F59E0B),
                WarningAmber,
                "○ Connecting to Ephemeral Network..."
            )
            SocketConnectionState.DISCONNECTED -> Triple(
                Color(0x33EF4444),
                ErrorRed,
                "✕ Disconnected. Reconnecting..."
            )
            SocketConnectionState.ERROR -> Triple(
                Color(0x44EF4444),
                ErrorRed,
                "✕ Network Error. Check connection."
            )
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(vertical = 6.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp
            )
        }
    }
}
