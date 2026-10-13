/**
 * DeathChat Companion Web Application
 * Ephemeral, Anonymous Real-Time Messaging Client
 */

// AVATAR EMOJI MAPPING
const AVATAR_MAP = {
  avatar_reaper: '💀',
  avatar_phantom: '👻',
  avatar_spectre: '👤',
  avatar_demon: '😈',
  avatar_stalker: '🕶️',
  avatar_wraith: '🥷',
  avatar_skull: '☠️',
  avatar_glitch: '🤖'
};

const STATE = {
  session: null,
  socket: null,
  currentRoom: null,
  isOwner: false,
  isTyping: false,
  typingTimeout: null,
  selectedAvatar: 'avatar_reaper'
};

// DOM ELEMENTS
const screens = {
  onboarding: document.getElementById('screenOnboarding'),
  home: document.getElementById('screenHome'),
  chat: document.getElementById('screenChat')
};

function switchScreen(screenName) {
  Object.values(screens).forEach(s => s.classList.remove('active'));
  if (screens[screenName]) {
    screens[screenName].classList.add('active');
  }
}

function showToast(message) {
  const toast = document.getElementById('toast');
  toast.textContent = message;
  toast.style.display = 'block';
  setTimeout(() => {
    toast.style.display = 'none';
  }, 3500);
}

// 1. INITIALIZATION & SESSION RESTORATION
async function init() {
  renderAvatarGrid();
  setupEventHandlers();

  // Check URL params for invite code e.g. ?room=DC7X9K
  const urlParams = new URLSearchParams(window.location.search);
  const inviteCode = urlParams.get('room');

  const savedSession = localStorage.getItem('deathchat_session');
  if (savedSession) {
    try {
      const parsed = JSON.parse(savedSession);
      // Validate session with backend
      const res = await fetch('/api/sessions/me', {
        headers: { 'x-session-id': parsed.sessionId }
      });
      if (res.ok) {
        const json = await res.json();
        STATE.session = json.data;
        updateHeaderIdentity();

        if (inviteCode) {
          switchScreen('home');
          openJoinModal(inviteCode);
        } else {
          switchScreen('home');
          loadPublicRooms();
        }
        return;
      }
    } catch (e) {
      console.warn('Could not restore session:', e);
    }
  }

  // If no valid session, generate random identity
  await fetchRandomIdentity();
  switchScreen('onboarding');
}

function renderAvatarGrid() {
  const grid = document.getElementById('avatarGrid');
  grid.innerHTML = '';
  Object.entries(AVATAR_MAP).forEach(([key, emoji]) => {
    const div = document.createElement('div');
    div.className = `avatar-option ${key === STATE.selectedAvatar ? 'selected' : ''}`;
    div.textContent = emoji;
    div.dataset.key = key;
    div.addEventListener('click', () => {
      document.querySelectorAll('.avatar-option').forEach(el => el.classList.remove('selected'));
      div.classList.add('selected');
      STATE.selectedAvatar = key;
    });
    grid.appendChild(div);
  });
}

async function fetchRandomIdentity() {
  try {
    const res = await fetch('/api/sessions/random-identity');
    const json = await res.json();
    if (json.success) {
      document.getElementById('inputDisplayName').value = json.data.displayName;
      STATE.selectedAvatar = json.data.avatar;
      renderAvatarGrid();
    }
  } catch (err) {
    console.error('Failed to get random identity:', err);
  }
}

function updateHeaderIdentity() {
  if (!STATE.session) return;
  document.getElementById('headerName').textContent = STATE.session.displayName;
  document.getElementById('headerAvatar').textContent = AVATAR_MAP[STATE.session.avatar] || '💀';
}

// 2. SOCKET.IO CONNECTION
function initSocket() {
  if (STATE.socket) {
    STATE.socket.disconnect();
  }

  STATE.socket = io({
    auth: { token: STATE.session.sessionId },
    transports: ['websocket', 'polling']
  });

  const banner = document.getElementById('connectionBanner');

  STATE.socket.on('connect', () => {
    banner.className = 'connection-banner connected';
    banner.textContent = '● Connected to Real-time Ephemeral Network';
  });

  STATE.socket.on('disconnect', () => {
    banner.className = 'connection-banner disconnected';
    banner.textContent = '○ Reconnecting to network...';
  });

  STATE.socket.on('room_joined', payload => {
    if (!payload.success) return;
    const room = payload.data;
    STATE.currentRoom = room;
    STATE.isOwner = room.isOwner;
    setupChatView(room);
  });

  STATE.socket.on('new_message', message => {
    appendMessage(message);
  });

  STATE.socket.on('typing_update', data => {
    const indicator = document.getElementById('typingIndicator');
    const typingText = document.getElementById('typingText');
    if (data.isTyping) {
      typingText.textContent = `${data.displayName} is typing...`;
      indicator.style.display = 'flex';
    } else {
      indicator.style.display = 'none';
    }
  });

  STATE.socket.on('participants_updated', data => {
    updateParticipantsList(data.participants, data.count);
  });

  STATE.socket.on('participant_kicked', data => {
    showToast(data.message || 'You were kicked from this room.');
    leaveCurrentRoom();
  });

  STATE.socket.on('participant_muted', data => {
    if (data.sessionId === STATE.session.sessionId) {
      showToast(data.isMuted ? 'You have been muted in this room.' : 'You have been unmuted.');
    }
  });

  STATE.socket.on('room_closed', data => {
    showToast('This room is now closed to new participants.');
  });

  STATE.socket.on('room_deleted', data => {
    showToast('The room was deleted by the owner.');
    leaveCurrentRoom();
  });

  STATE.socket.on('room_expired', data => {
    showToast('The room has reached its 24-hour expiration limit and expired.');
    leaveCurrentRoom();
  });

  STATE.socket.on('room_error', err => {
    showToast(`Error: ${err.error}`);
  });
}

// 3. CHAT VIEW & MESSAGES
function setupChatView(room) {
  switchScreen('chat');
  document.getElementById('chatRoomName').textContent = room.name;
  document.getElementById('chatRoomCodeBadge').textContent = room.roomId;
  document.getElementById('chatRetentionBadge').textContent = `⏱️ ${room.messageRetentionSeconds / 60}m retention`;
  document.getElementById('chatRoomPrivacyBadge').textContent = room.isPrivate ? 'PRIVATE' : 'PUBLIC';
  document.getElementById('chatRoomPrivacyBadge').className = `badge ${room.isPrivate ? 'private' : ''}`;

  document.getElementById('ownerControlsSection').style.display = STATE.isOwner ? 'block' : 'none';

  // Clear messages list and load recent history
  document.getElementById('messagesList').innerHTML = '';
  loadRoomMessages(room.roomId);
  updateParticipantsList(room.participants || [], room.participantCount || 1);
}

async function loadRoomMessages(roomId) {
  try {
    const res = await fetch(`/api/rooms/${roomId}/messages?limit=50`);
    const json = await res.json();
    if (json.success && Array.isArray(json.data)) {
      json.data.forEach(m => appendMessage(m));
    }
  } catch (err) {
    console.error('Failed to load room messages:', err);
  }
}

function appendMessage(msg) {
  const list = document.getElementById('messagesList');
  const isSelf = msg.senderSessionId === STATE.session.sessionId;

  const item = document.createElement('div');
  item.className = `message-item ${isSelf ? 'self' : ''}`;

  const timeStr = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  const avatarEmoji = AVATAR_MAP[msg.senderAvatar] || '💀';

  item.innerHTML = `
    <div class="message-avatar">${avatarEmoji}</div>
    <div class="message-body">
      <div class="message-header">
        <span class="message-sender">${escapeHtml(msg.senderDisplayName)}</span>
        <span class="message-time">${timeStr}</span>
      </div>
      <div class="message-bubble">${escapeHtml(msg.content)}</div>
    </div>
  `;

  list.appendChild(item);
  list.scrollTop = list.scrollHeight;
}

function updateParticipantsList(participants, count) {
  document.getElementById('participantCountBadge').textContent = count;
  document.getElementById('sideParticipantCount').textContent = count;

  const list = document.getElementById('participantsList');
  list.innerHTML = '';

  participants.forEach(p => {
    const li = document.createElement('li');
    li.className = 'participant-item';

    const isSelf = p.sessionId === STATE.session.sessionId;
    const isTargetOwner = p.isOwner;

    let actionsHtml = '';
    if (STATE.isOwner && !isSelf && !isTargetOwner) {
      actionsHtml = `
        <div class="participant-actions">
          <button class="btn-icon btn-kick" data-id="${p.sessionId}" title="Kick">🥾</button>
          <button class="btn-icon btn-mute" data-id="${p.sessionId}" title="Mute">🔇</button>
        </div>
      `;
    } else if (!isSelf) {
      actionsHtml = `
        <button class="btn-icon btn-report" data-id="${p.sessionId}" data-name="${escapeHtml(p.displayName)}" title="Report User">🚩</button>
      `;
    }

    li.innerHTML = `
      <div class="participant-info">
        <span>${AVATAR_MAP[p.avatar] || '👤'}</span>
        <span class="participant-name">${escapeHtml(p.displayName)} ${isTargetOwner ? '<span class="owner-star" title="Room Owner">★</span>' : ''} ${isSelf ? '(You)' : ''}</span>
      </div>
      ${actionsHtml}
    `;

    list.appendChild(li);
  });

  // Attach kick/mute/report handlers
  list.querySelectorAll('.btn-kick').forEach(b => {
    b.addEventListener('click', () => {
      const targetId = b.dataset.id;
      if (confirm('Are you sure you want to kick this participant?')) {
        STATE.socket.emit('kick_participant', {
          roomId: STATE.currentRoom.roomId,
          targetSessionId: targetId
        }, res => {
          if (!res.success) showToast(res.error);
        });
      }
    });
  });

  list.querySelectorAll('.btn-mute').forEach(b => {
    b.addEventListener('click', () => {
      const targetId = b.dataset.id;
      STATE.socket.emit('mute_participant', {
        roomId: STATE.currentRoom.roomId,
        targetSessionId: targetId
      }, res => {
        if (!res.success) showToast(res.error);
      });
    });
  });

  list.querySelectorAll('.btn-report').forEach(b => {
    b.addEventListener('click', () => {
      openReportModal(b.dataset.id, b.dataset.name);
    });
  });
}

function leaveCurrentRoom() {
  if (STATE.socket && STATE.currentRoom) {
    STATE.socket.emit('leave_room', { roomId: STATE.currentRoom.roomId });
  }
  STATE.currentRoom = null;
  STATE.isOwner = false;
  switchScreen('home');
  loadPublicRooms();
}

// 4. PUBLIC ROOMS DISCOVERY
async function loadPublicRooms() {
  const list = document.getElementById('roomsList');
  const emptyState = document.getElementById('roomsEmptyState');
  const search = document.getElementById('searchPublicRooms').value.trim();

  try {
    const res = await fetch(`/api/rooms?search=${encodeURIComponent(search)}`);
    const json = await res.json();

    if (!json.success || !json.data || json.data.length === 0) {
      list.innerHTML = '';
      emptyState.style.display = 'block';
      return;
    }

    emptyState.style.display = 'none';
    list.innerHTML = '';

    json.data.forEach(room => {
      const card = document.createElement('div');
      card.className = 'room-item-card';

      const retentionMin = room.messageRetentionSeconds / 60;

      card.innerHTML = `
        <div>
          <div class="room-item-top">
            <span class="room-item-title">${escapeHtml(room.name)}</span>
            <span class="code-badge">${room.roomId}</span>
          </div>
          <p class="room-item-desc">${escapeHtml(room.description || 'Ephemeral conversation.')}</p>
          <div class="room-item-meta">
            <span class="chip presence">● ${room.participantCount} online</span>
            <span class="chip retention">⏱️ ${retentionMin}m retention</span>
            <span class="chip">⏳ 24h</span>
          </div>
        </div>
        <button class="btn-neon-primary btn-sm full-width btn-join-card" data-id="${room.roomId}">JOIN ROOM</button>
      `;

      list.appendChild(card);
    });

    list.querySelectorAll('.btn-join-card').forEach(b => {
      b.addEventListener('click', () => {
        joinRoomDirect(b.dataset.id);
      });
    });
  } catch (err) {
    console.error('Failed to load public rooms:', err);
  }
}

// 5. ROOM ACTIONS
async function joinRoomDirect(roomId, password = null) {
  if (!STATE.socket) initSocket();

  STATE.socket.emit('join_room', { roomId, password }, res => {
    if (res.success) {
      document.querySelectorAll('.modal-backdrop').forEach(m => m.classList.remove('active'));
    } else {
      if (res.requiresPassword) {
        openJoinModal(roomId, true);
      } else {
        showToast(`Join Failed: ${res.error}`);
      }
    }
  });
}

function openJoinModal(code = '', showPassword = false) {
  const modal = document.getElementById('modalJoinRoom');
  document.getElementById('inputJoinCode').value = code;
  document.getElementById('joinPasswordGroup').style.display = showPassword ? 'block' : 'none';
  document.getElementById('joinErrorMsg').style.display = 'none';
  modal.classList.add('active');
}

function openReportModal(targetSessionId, targetName) {
  const modal = document.getElementById('modalReportUser');
  document.getElementById('reportTargetSessionId').value = targetSessionId;
  document.getElementById('reportTargetName').textContent = targetName;
  document.getElementById('reportReason').value = '';
  modal.classList.add('active');
}

// 6. EVENT HANDLERS
function setupEventHandlers() {
  // Enter App from Onboarding
  document.getElementById('btnEnterApp').addEventListener('click', async () => {
    const name = document.getElementById('inputDisplayName').value.trim();
    try {
      const res = await fetch('/api/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          displayName: name,
          avatar: STATE.selectedAvatar
        })
      });

      const json = await res.json();
      if (json.success) {
        STATE.session = json.data;
        localStorage.setItem('deathchat_session', JSON.stringify(STATE.session));
        updateHeaderIdentity();
        initSocket();
        switchScreen('home');
        loadPublicRooms();
      } else {
        showToast(json.error || 'Failed to initialize session.');
      }
    } catch (err) {
      showToast('Server unavailable.');
    }
  });

  // Reroll Name
  document.getElementById('btnRerollName').addEventListener('click', fetchRandomIdentity);

  // Edit Identity
  document.getElementById('btnEditIdentity').addEventListener('click', () => {
    switchScreen('onboarding');
  });

  // Open Modals
  document.getElementById('btnOpenCreateModal').addEventListener('click', () => {
    document.getElementById('modalCreateRoom').classList.add('active');
  });

  document.getElementById('btnOpenJoinModal').addEventListener('click', () => {
    openJoinModal();
  });

  // Modal Closers
  document.querySelectorAll('.btn-close-modal').forEach(b => {
    b.addEventListener('click', () => {
      const modalId = b.dataset.modal;
      document.getElementById(modalId).classList.remove('active');
    });
  });

  // Create Room Form Visibility Toggle
  document.querySelectorAll('input[name="roomVisibility"]').forEach(r => {
    r.addEventListener('change', e => {
      document.getElementById('privatePasswordGroup').style.display =
        e.target.value === 'private' ? 'block' : 'none';
    });
  });

  // Submit Create Room
  document.getElementById('formCreateRoom').addEventListener('submit', async e => {
    e.preventDefault();
    const name = document.getElementById('inputRoomName').value.trim();
    const description = document.getElementById('inputRoomDesc').value.trim();
    const isPrivate = document.querySelector('input[name="roomVisibility"]:checked').value === 'private';
    const password = document.getElementById('inputRoomPassword').value;
    const retentionSeconds = parseInt(document.querySelector('input[name="retentionPolicy"]:checked').value, 10);

    try {
      const res = await fetch('/api/rooms', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-id': STATE.session.sessionId
        },
        body: JSON.stringify({
          name,
          description,
          isPrivate,
          password: isPrivate ? password : null,
          retentionSeconds
        })
      });

      const json = await res.json();
      if (json.success) {
        document.getElementById('modalCreateRoom').classList.remove('active');
        joinRoomDirect(json.data.roomId, isPrivate ? password : null);
      } else {
        showToast(json.error || 'Room creation failed.');
      }
    } catch (err) {
      showToast('Network error while creating room.');
    }
  });

  // Submit Join Room
  document.getElementById('formJoinRoom').addEventListener('submit', e => {
    e.preventDefault();
    let code = document.getElementById('inputJoinCode').value.trim();
    // Support URL parsing if user pasted full invite URL e.g. http://localhost:3000/?room=DC7X9K
    if (code.includes('?room=')) {
      code = code.split('?room=')[1].split('&')[0];
    }
    const password = document.getElementById('inputJoinPassword').value;
    joinRoomDirect(code, password);
  });

  // Search and Refresh Public Rooms
  document.getElementById('btnRefreshRooms').addEventListener('click', loadPublicRooms);
  let searchDebounce = null;
  document.getElementById('searchPublicRooms').addEventListener('input', () => {
    clearTimeout(searchDebounce);
    searchDebounce = setTimeout(loadPublicRooms, 300);
  });

  // Chat Controls
  document.getElementById('btnLeaveRoom').addEventListener('click', leaveCurrentRoom);

  document.getElementById('btnToggleParticipants').addEventListener('click', () => {
    document.getElementById('participantsDrawer').classList.toggle('open');
  });

  document.getElementById('btnCloseSidebar').addEventListener('click', () => {
    document.getElementById('participantsDrawer').classList.remove('open');
  });

  // Copy Invite Link
  document.getElementById('btnShareInvite').addEventListener('click', () => {
    if (!STATE.currentRoom) return;
    const inviteUrl = `${window.location.origin}/?room=${STATE.currentRoom.roomId}`;
    navigator.clipboard.writeText(inviteUrl).then(() => {
      showToast(`Invite copied: ${STATE.currentRoom.roomId}`);
    }).catch(() => {
      prompt('Copy invite code:', STATE.currentRoom.roomId);
    });
  });

  // Send Message
  const inputMessage = document.getElementById('chatMessageInput');
  const sendMessageAction = () => {
    const text = inputMessage.value.trim();
    if (!text || !STATE.socket || !STATE.currentRoom) return;

    STATE.socket.emit('send_message', {
      roomId: STATE.currentRoom.roomId,
      content: text
    }, res => {
      if (!res.success) {
        showToast(`Send failed: ${res.error}`);
      }
    });

    inputMessage.value = '';
    // Stop typing
    STATE.socket.emit('typing_stop', { roomId: STATE.currentRoom.roomId });
    STATE.isTyping = false;
  };

  document.getElementById('btnSendMessage').addEventListener('click', sendMessageAction);
  inputMessage.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      sendMessageAction();
    }
  });

  // Typing emitter
  inputMessage.addEventListener('input', () => {
    if (!STATE.socket || !STATE.currentRoom) return;
    if (!STATE.isTyping) {
      STATE.isTyping = true;
      STATE.socket.emit('typing_start', { roomId: STATE.currentRoom.roomId });
    }

    clearTimeout(STATE.typingTimeout);
    STATE.typingTimeout = setTimeout(() => {
      STATE.isTyping = false;
      STATE.socket.emit('typing_stop', { roomId: STATE.currentRoom.roomId });
    }, 2000);
  });

  // Owner Actions: Close & Delete Room
  document.getElementById('btnCloseRoom').addEventListener('click', () => {
    if (confirm('Close this room to new participants?')) {
      STATE.socket.emit('close_room', { roomId: STATE.currentRoom.roomId }, res => {
        if (res.success) showToast('Room is now closed to new participants.');
        else showToast(res.error);
      });
    }
  });

  document.getElementById('btnDeleteRoom').addEventListener('click', () => {
    if (confirm('DANGER: Permanently destroy this room immediately?')) {
      STATE.socket.emit('delete_room', { roomId: STATE.currentRoom.roomId }, res => {
        if (!res.success) showToast(res.error);
      });
    }
  });

  // Submit Abuse Report
  document.getElementById('formReportUser').addEventListener('submit', async e => {
    e.preventDefault();
    const reportedSessionId = document.getElementById('reportTargetSessionId').value;
    const reason = document.getElementById('reportReason').value.trim();

    try {
      const res = await fetch('/api/reports', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'x-session-id': STATE.session.sessionId
        },
        body: JSON.stringify({
          reportedSessionId,
          roomId: STATE.currentRoom ? STATE.currentRoom.roomId : null,
          reason
        })
      });

      const json = await res.json();
      if (json.success) {
        showToast('Report submitted. Thank you for keeping DeathChat safe.');
        document.getElementById('modalReportUser').classList.remove('active');
      } else {
        showToast(json.error || 'Failed to submit report.');
      }
    } catch (err) {
      showToast('Network error while submitting report.');
    }
  });
}

function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// Start application
window.addEventListener('DOMContentLoaded', init);
