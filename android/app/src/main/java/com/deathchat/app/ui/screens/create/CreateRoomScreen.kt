package com.deathchat.app.ui.screens.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deathchat.app.ui.components.CyberTextField
import com.deathchat.app.ui.components.GlowingCard
import com.deathchat.app.ui.components.NeonButton
import com.deathchat.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    onNavigateBack: () -> Unit,
    onRoomCreated: (String) -> Unit,
    viewModel: CreateRoomViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SPAWN ROOM",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlowingCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CyberTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChanged,
                        label = "Room Name *",
                        placeholder = "e.g. Underground Syndicate",
                        isError = state.error != null
                    )

                    CyberTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChanged,
                        label = "Description (Optional)",
                        placeholder = "Brief ephemeral context"
                    )

                    // Visibility Selector
                    Text(
                        text = "ROOM VISIBILITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VisibilityCard(
                            title = "Public",
                            subtitle = "Visible on dashboard",
                            icon = Icons.Default.Public,
                            isSelected = !state.isPrivate,
                            onClick = { viewModel.onVisibilityChanged(false) },
                            modifier = Modifier.weight(1f)
                        )

                        VisibilityCard(
                            title = "Private",
                            subtitle = "Password protected",
                            icon = Icons.Default.Lock,
                            isSelected = state.isPrivate,
                            onClick = { viewModel.onVisibilityChanged(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.isPrivate) {
                        CyberTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChanged,
                            label = "Room Password *",
                            placeholder = "Min 4 characters",
                            visualTransformation = PasswordVisualTransformation()
                        )
                    }

                    // Retention Policy Selector
                    Text(
                        text = "MESSAGE RETENTION POLICY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RetentionChip(
                            label = "30 Mins",
                            seconds = 1800,
                            selectedSeconds = state.retentionSeconds,
                            onClick = { viewModel.onRetentionChanged(1800) },
                            modifier = Modifier.weight(1f)
                        )
                        RetentionChip(
                            label = "1 Hour",
                            seconds = 3600,
                            selectedSeconds = state.retentionSeconds,
                            onClick = { viewModel.onRetentionChanged(3600) },
                            modifier = Modifier.weight(1f)
                        )
                        RetentionChip(
                            label = "2 Hours",
                            seconds = 7200,
                            selectedSeconds = state.retentionSeconds,
                            onClick = { viewModel.onRetentionChanged(7200) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "⏳ The room automatically self-destructs after 24 hours.",
                        fontSize = 11.sp,
                        color = WarningAmber
                    )

                    if (state.error != null) {
                        Text(
                            text = state.error ?: "",
                            fontSize = 12.sp,
                            color = ErrorRed
                        )
                    }

                    NeonButton(
                        text = "Spawn Room",
                        onClick = {
                            viewModel.createRoom { created ->
                                onRoomCreated(created.roomId)
                            }
                        },
                        isLoading = state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun VisibilityCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (isSelected) Color(0x33A855F7) else SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) PurpleGlow else BorderSubtle
        ),
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PurpleLight else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) TextPrimary else TextSecondary
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun RetentionChip(
    label: String,
    seconds: Int,
    selectedSeconds: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = seconds == selectedSeconds
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ElectricPurple else SurfaceCard,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
}
