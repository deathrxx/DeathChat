package com.deathchat.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deathchat.app.data.api.models.RoomDto
import com.deathchat.app.ui.components.AvatarHelper
import com.deathchat.app.ui.components.GlowingCard
import com.deathchat.app.ui.components.NeonButton
import com.deathchat.app.ui.components.NeonButtonVariant
import com.deathchat.app.ui.theme.*

@Composable
fun HomeScreen(
    onCreateRoomClick: () -> Unit,
    onJoinRoomClick: () -> Unit,
    onPublicRoomsClick: () -> Unit,
    onRoomItemClick: (String) -> Unit,
    onEditIdentityClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            HomeTopBar(
                displayName = state.session?.displayName ?: "Anonymous",
                avatar = state.session?.avatar,
                onEditIdentityClick = onEditIdentityClick,
                onRefreshClick = viewModel::loadData
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Quick Action Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ActionCard(
                        title = "CREATE ROOM",
                        description = "Spawn a temporary room with custom retention.",
                        icon = "⚡",
                        buttonText = "Spawn",
                        onClick = onCreateRoomClick,
                        modifier = Modifier.weight(1f)
                    )

                    ActionCard(
                        title = "JOIN ROOM",
                        description = "Drop into a conversation with a 6-digit code.",
                        icon = "🔑",
                        buttonText = "Join",
                        onClick = onJoinRoomClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTIVE PUBLIC ROOMS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Join unexpired anonymous public discussions",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    TextButton(onClick = onPublicRoomsClick) {
                        Text(
                            text = "View All →",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (state.isLoading && state.publicRooms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurpleGlow)
                    }
                }
            } else if (state.publicRooms.isEmpty()) {
                item {
                    GlowingCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "📡", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No public rooms active right now.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            NeonButton(
                                text = "Spawn First Room",
                                onClick = onCreateRoomClick,
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                    }
                }
            } else {
                items(state.publicRooms) { room ->
                    PublicRoomCard(
                        room = room,
                        onClick = { onRoomItemClick(room.roomId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    displayName: String,
    avatar: String?,
    onEditIdentityClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgSecondary)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = "💀", fontSize = 26.sp)
            Column {
                Text(
                    text = "DEATHCHAT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = TextPrimary
                )
                Text(
                    text = "EPHEMERAL PROTOCOL",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = CyanAccent
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
            }

            // Identity Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceCard,
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = AvatarHelper.getEmoji(avatar), fontSize = 16.sp)
                    Text(
                        text = displayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onEditIdentityClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Codename",
                            tint = PurpleLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlowingCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp,
                minLines = 2
            )
            Spacer(modifier = Modifier.height(14.dp))
            NeonButton(
                text = buttonText,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PublicRoomCard(
    room: RoomDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlowingCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x33A855F7)
                ) {
                    Text(
                        text = room.roomId,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!room.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = room.description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x2210B981)
                ) {
                    Text(
                        text = "● ${room.participantCount} online",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x2206B6D4)
                ) {
                    Text(
                        text = "⏱️ ${room.messageRetentionSeconds / 60}m retention",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x2294A3B8)
                ) {
                    Text(
                        text = "⏳ 24h",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
