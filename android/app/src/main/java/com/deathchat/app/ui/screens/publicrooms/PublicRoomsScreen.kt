package com.deathchat.app.ui.screens.publicrooms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deathchat.app.ui.components.CyberTextField
import com.deathchat.app.ui.components.GlowingCard
import com.deathchat.app.ui.screens.home.PublicRoomCard
import com.deathchat.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicRoomsScreen(
    onNavigateBack: () -> Unit,
    onRoomSelected: (String) -> Unit,
    viewModel: PublicRoomsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PUBLIC DIRECTORY",
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
                actions = {
                    IconButton(onClick = { viewModel.loadRooms() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            CyberTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                label = "Search Public Rooms",
                placeholder = "Filter by room name...",
                trailingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading && state.rooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PurpleGlow)
                }
            } else if (state.rooms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No public rooms match your search query.",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.rooms) { room ->
                        PublicRoomCard(
                            room = room,
                            onClick = { onRoomSelected(room.roomId) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
