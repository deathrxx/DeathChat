package com.deathchat.app.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deathchat.app.data.api.models.MessageDeliveryStatus
import com.deathchat.app.data.api.models.MessageDto
import com.deathchat.app.data.api.models.ParticipantDto
import com.deathchat.app.ui.components.AvatarHelper
import com.deathchat.app.ui.components.GlowingCard
import com.deathchat.app.ui.components.StatusBanner
import com.deathchat.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showParticipantsSheet by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf<ParticipantDto?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var showOwnerMenu by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) {
        viewModel.initRoom(roomId)
    }

    LaunchedEffect(state.shouldExit) {
        if (state.shouldExit) {
            onNavigateBack()
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfoMessage()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.room?.name ?: "Chat Room",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (state.room?.isPrivate == true) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Private",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.roomId,
                                fontSize = 11.sp,
                                color = PurpleLight,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "⏳ 24h",
                                fontSize = 10.sp,
                                color = WarningAmber
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::leaveRoom) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Leave Room",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Participant Counter Button
                    TextButton(onClick = { showParticipantsSheet = true }) {
                        Text(
                            text = "👥 ${state.participants.size}",
                            color = CyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (state.isOwner) {
                        IconButton(onClick = { showOwnerMenu = !showOwnerMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Owner Menu", tint = TextPrimary)
                        }

                        DropdownMenu(
                            expanded = showOwnerMenu,
                            onDismissRequest = { showOwnerMenu = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Close to New Users", color = WarningAmber) },
                                onClick = {
                                    showOwnerMenu = false
                                    viewModel.closeRoom()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Destroy Room", color = ErrorRed) },
                                onClick = {
                                    showOwnerMenu = false
                                    viewModel.deleteRoom()
                                }
                            )
                        }
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
        ) {
            // Live Status Banner
            StatusBanner(connectionState = state.connectionState)

            // Message Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.messages.isEmpty() && !state.isLoadingHistory) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet.\nBreak the silence anonymously.",
                            color = TextMuted,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.messages, key = { it.messageId }) { msg ->
                            val isSelf = msg.senderSessionId == state.currentSessionId
                            MessageBubble(message = msg, isSelf = isSelf)
                        }
                    }
                }
            }

            // Typing Indicator
            AnimatedVisibility(visible = state.typingUsers.isNotEmpty()) {
                val typerText = state.typingUsers.joinToString(", ")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$typerText is typing...",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Composition Input Bar
            Surface(
                color = BgSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.inputMessage,
                        onValueChange = viewModel::onInputMessageChanged,
                        placeholder = { Text("Send an ephemeral message...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = PurpleGlow,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = viewModel::sendMessage,
                        enabled = state.inputMessage.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (state.inputMessage.isNotBlank()) ElectricPurple else SurfaceCard,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (state.inputMessage.isNotBlank()) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // Participants Modal Bottom Sheet
    if (showParticipantsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showParticipantsSheet = false },
            containerColor = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "ROOM PARTICIPANTS (${state.participants.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.participants) { participant ->
                        val isSelf = participant.sessionId == state.currentSessionId
                        ParticipantItem(
                            participant = participant,
                            isSelf = isSelf,
                            isRoomOwner = state.isOwner,
                            onKick = { viewModel.kickParticipant(participant.sessionId) },
                            onMute = { viewModel.muteParticipant(participant.sessionId) },
                            onReport = { showReportDialog = participant }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Abuse Report Dialog
    if (showReportDialog != null) {
        val target = showReportDialog!!
        AlertDialog(
            onDismissRequest = { showReportDialog = null },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Report Participant",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reporting: ${target.displayName}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        placeholder = { Text("Reason for reporting abusive behavior...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportReason.isNotBlank()) {
                            viewModel.reportUser(target.sessionId, reportReason)
                            reportReason = ""
                            showReportDialog = null
                        }
                    }
                ) {
                    Text("Submit Report", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: MessageDto,
    isSelf: Boolean
) {
    val bubbleShape = if (isSelf) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isSelf) {
            Surface(
                shape = CircleShape,
                color = SurfaceCard,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = AvatarHelper.getEmoji(message.senderAvatar), fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isSelf) {
                Text(
                    text = message.senderDisplayName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Surface(
                shape = bubbleShape,
                color = if (isSelf) ElectricPurple else SurfaceCard,
                modifier = Modifier.clip(bubbleShape)
            ) {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                val timeStr = formatIsoTime(message.createdAt)
                Text(
                    text = timeStr,
                    fontSize = 10.sp,
                    color = TextMuted
                )

                if (isSelf) {
                    when (message.deliveryStatus) {
                        MessageDeliveryStatus.SENDING -> Text("○", fontSize = 10.sp, color = WarningAmber)
                        MessageDeliveryStatus.SENT -> Text("✓", fontSize = 10.sp, color = SuccessGreen)
                        MessageDeliveryStatus.FAILED -> Text("✕", fontSize = 10.sp, color = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantItem(
    participant: ParticipantDto,
    isSelf: Boolean,
    isRoomOwner: Boolean,
    onKick: () -> Unit,
    onMute: () -> Unit,
    onReport: () -> Unit
) {
    GlowingCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = AvatarHelper.getEmoji(participant.avatar), fontSize = 20.sp)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = participant.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        if (participant.isOwner) {
                            Text(text = "★", color = WarningAmber, fontSize = 12.sp)
                        }
                        if (isSelf) {
                            Text(text = "(You)", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isRoomOwner && !isSelf && !participant.isOwner) {
                    IconButton(onClick = onKick, modifier = Modifier.size(28.dp)) {
                        Text(text = "🥾", fontSize = 14.sp)
                    }
                    IconButton(onClick = onMute, modifier = Modifier.size(28.dp)) {
                        Text(text = "🔇", fontSize = 14.sp)
                    }
                } else if (!isSelf) {
                    IconButton(onClick = onReport, modifier = Modifier.size(28.dp)) {
                        Text(text = "🚩", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun formatIsoTime(iso: String): String {
    return try {
        if (iso.contains("T")) {
            val timePart = iso.substringAfter("T").substring(0, 5)
            timePart
        } else iso
    } catch (_: Exception) {
        iso
    }
}
