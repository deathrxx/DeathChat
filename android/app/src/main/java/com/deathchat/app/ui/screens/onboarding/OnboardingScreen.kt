package com.deathchat.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deathchat.app.ui.components.*
import com.deathchat.app.ui.theme.*

@Composable
fun OnboardingScreen(
    onNavigateHome: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showServerSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GlowingCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "💀", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ANONYMOUS IDENTITY",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = TextPrimary
                    )

                    Text(
                        text = "No accounts. No phone numbers. Ephemeral sessions.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Avatar Picker
                    Text(
                        text = "SELECT AVATAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AvatarPicker(
                        selectedAvatar = state.selectedAvatar,
                        onAvatarSelected = viewModel::onAvatarSelected
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Codename / Display Name
                    Text(
                        text = "CODENAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            CyberTextField(
                                value = state.displayName,
                                onValueChange = viewModel::onDisplayNameChanged,
                                label = "Display Name",
                                placeholder = "e.g. PhantomReaper_99",
                                isError = state.error != null,
                                errorMessage = state.error
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = viewModel::rerollIdentity,
                            enabled = !state.isFetchingRandom,
                            modifier = Modifier
                                .size(50.dp)
                                .background(SurfaceCard, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = "Reroll Name",
                                tint = PurpleLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Server Settings Toggle
                    TextButton(
                        onClick = { showServerSettings = !showServerSettings }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showServerSettings) "Hide Server Config" else "Server Config (${state.serverUrl})",
                            fontSize = 12.sp,
                            color = CyanAccent
                        )
                    }

                    if (showServerSettings) {
                        Spacer(modifier = Modifier.height(6.dp))
                        CyberTextField(
                            value = state.serverUrl,
                            onValueChange = viewModel::onServerUrlChanged,
                            label = "Backend Base URL",
                            placeholder = "http://192.168.31.251:3000"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = state.serverUrl == "http://192.168.31.251:3000",
                                onClick = { viewModel.onServerUrlChanged("http://192.168.31.251:3000") },
                                label = { Text("MSI Player (192.168.31.251)", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricPurple,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                            FilterChip(
                                selected = state.serverUrl == "http://10.0.2.2:3000",
                                onClick = { viewModel.onServerUrlChanged("http://10.0.2.2:3000") },
                                label = { Text("AVD (10.0.2.2)", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricPurple,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                        Text(
                            text = "💡 MSI App Player uses host IP: http://192.168.31.251:3000",
                            fontSize = 11.sp,
                            color = CyanAccent,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    NeonButton(
                        text = "Enter The Void",
                        onClick = { viewModel.enterApp(onSuccess = onNavigateHome) },
                        isLoading = state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
