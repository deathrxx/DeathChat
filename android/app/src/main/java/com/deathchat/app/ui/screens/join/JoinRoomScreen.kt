package com.deathchat.app.ui.screens.join

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deathchat.app.ui.components.CyberTextField
import com.deathchat.app.ui.components.GlowingCard
import com.deathchat.app.ui.components.NeonButton
import com.deathchat.app.ui.components.NeonButtonVariant
import com.deathchat.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    initialCode: String = "",
    onNavigateBack: () -> Unit,
    onRoomJoined: (String) -> Unit,
    viewModel: JoinRoomViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(initialCode) {
        if (initialCode.isNotBlank()) {
            viewModel.setInitialCode(initialCode)
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "JOIN ROOM",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSecondary)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlowingCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "🔑", fontSize = 42.sp)

                    Text(
                        text = "ENTER INVITATION",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = TextPrimary
                    )

                    CyberTextField(
                        value = state.roomCode,
                        onValueChange = viewModel::onCodeChanged,
                        label = "Room Code or Link",
                        placeholder = "e.g. DC7X9K"
                    )

                    if (state.requiresPassword) {
                        CyberTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Room Password",
                            placeholder = "Enter private room password",
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    if (state.error != null) {
                        Text(
                            text = state.error ?: "",
                            fontSize = 12.sp,
                            color = ErrorRed
                        )
                    }

                    NeonButton(
                        text = "Connect To Room",
                        onClick = { viewModel.joinRoom(onSuccess = onRoomJoined) },
                        variant = NeonButtonVariant.SECONDARY,
                        isLoading = state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
