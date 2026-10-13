# 💀 DEATHCHAT

> *"Enter anonymously. Talk freely. Leave no unnecessary traces."*

**DeathChat** is a complete, production-grade, anonymous ephemeral chat application designed with a futuristic dark aesthetic. It empowers users to spawn temporary encrypted-in-transit chat rooms, invite friends via codes or links, communicate in real time, and let conversations automatically expire and vanish from persistence.

---

## 🌟 Key Features

1. **True Ephemeral Identity**:
   - Zero registration required (no email, phone, or passwords for accounts).
   - Generates random, futuristic cyber codenames (e.g. `ShadowSpectre_404`, `NeonWraith_101`) and stealth avatars.
   - Server-issued anonymous session tokens persisted securely without credentials.

2. **Public & Private Ephemeral Rooms**:
   - **Public Rooms**: Searchable and browsable directory of active conversations.
   - **Private Rooms**: Server-enforced password authorization (hashed with `bcrypt`). Never exposes passwords in listings or API responses.
   - Instant invite links and 6-character room codes (e.g. `DC7X9K`).

3. **Server-Enforced Message Retention & Room Expiration**:
   - Configurable message retention per room: **30 minutes**, **1 hour**, or **2 hours**.
   - Server-enforced room expiration (**24 hours** default).
   - MongoDB TTL indexes combined with explicit query exclusion (`expiresAt > now`), guaranteeing expired messages and rooms are never served even before asynchronous physical cleanup runs.
   - Periodic automated background cleanup worker evicting stale presence and pruning records.

4. **Real-Time WebSocket Protocol (Socket.IO)**:
   - Instant message transmission and reception with authoritative server timestamps.
   - Real-time typing indicators with auto-timeout.
   - Server-maintained presence tracking (preventing duplicate counts across reconnects or tabs).
   - Connection state banners (Connected, Reconnecting, Disconnected).

5. **Moderation Controls**:
   - **Room Owner Privileges**: Kick participants (blocks re-entry), mute/unmute participants, close room to new joins, or permanently destroy room.
   - **Participant Safeguards**: Leave room, report abusive users with reason logging.
   - Strict server-side permission validation: non-owners cannot perform moderation, and room owners cannot be kicked or muted.

6. **Cross-Platform Ecosystem**:
   - Native modern **Android Application** (Jetpack Compose, Material 3, Navigation Compose, Coroutines & Flow, Retrofit, Socket.IO Java Client).
   - Responsive **Companion Web Client** (HTML5, CSS3 cyberpunk glassmorphism, Socket.IO Web Client).

---

## 🏗️ Project Architecture

```
DeathChat/
├── android/                   # Native Android application
│   ├── app/
│   │   ├── src/main/java/com/deathchat/app/
│   │   │   ├── data/          # API, DTOs, SocketManager, Repositories, Preferences
│   │   │   ├── ui/            # Compose Theme, Components, Screens, ViewModels, Navigation
│   │   │   ├── DeathChatApp.kt
│   │   │   └── MainActivity.kt
│   │   ├── build.gradle.kts
│   │   └── proguard-rules.pro
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradlew & gradlew.bat
│   └── gradle/wrapper/
├── server/                    # Node.js Express & Socket.IO backend
│   ├── src/
│   │   ├── config/            # Database connection & memory fallback
│   │   ├── models/            # Mongoose schemas (Room, Message, Session, Report)
│   │   ├── routes/            # REST endpoints
│   │   ├── controllers/       # Controller logic
│   │   ├── services/          # Presence, Message, Room, and Cleanup services
│   │   ├── socket/            # Socket.IO handlers and auth
│   │   ├── middleware/        # Rate limiters, session auth, error handling
│   │   ├── app.js
│   │   └── server.js
│   ├── tests/                 # Jest & Supertest automated test suites
│   ├── .env.example
│   └── package.json
├── web/                       # Companion responsive web interface
│   ├── index.html
│   ├── style.css
│   └── app.js
├── .gitignore
└── README.md
```

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Android Client** | Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coroutines & Flow, ViewModel, Retrofit 2, OkHttp 3, Gson, `io.socket:socket.io-client:2.1.1` |
| **Backend** | Node.js (v20+), Express.js, Socket.IO (v4), Mongoose (v8), MongoDB, `bcryptjs`, `express-rate-limit`, `helmet`, `uuid` |
| **Companion Web** | HTML5, CSS3 (Cyberpunk Glassmorphism), Modern Vanilla JavaScript, Socket.IO Client |
| **Testing** | Jest, Supertest, `mongodb-memory-server` (Backend); JUnit 4, Kotlinx Coroutines Test (Android) |

---

## 🚀 Getting Started

### Prerequisites

- **Node.js**: v20 or newer (`node -v`)
- **npm**: v10 or newer (`npm -v`)
- **Android Studio**: Ladybug / Meerkat or newer with Android SDK 35
- **Java Development Kit (JDK)**: Java 21 (included inside Android Studio `jbr/`)
- **MongoDB** *(Optional)*: If no MongoDB instance is provided in `.env`, the backend **automatically spins up an embedded in-memory MongoDB** for zero-friction local development!

---

### 1. Setting Up the Backend

1. Navigate to the `server/` directory:
   ```bash
   cd server
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Configure environment variables:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   *(If `MONGODB_URI` is left blank, the server automatically starts an embedded in-memory MongoDB instance).*

4. Run the server:
   ```bash
   npm start
   ```
   Or run in development mode with auto-reload:
   ```bash
   npm run dev
   ```

   The server will output:
   ```
   ====================================================
   💀 DEATHCHAT BACKEND RUNNING ON PORT: 3000
   🔗 REST API:   http://localhost:3000/api
   🏥 HEALTH:     http://localhost:3000/health
   🌐 COMPANION:  http://localhost:3000/
   ====================================================
   ```

---

### 2. Setting Up and Running the Android App

1. Open **Android Studio**.
2. Select **Open** and choose the `DeathChat/android` folder.
3. Allow Gradle to sync. (The project uses Gradle 8.11 with Java 21).

#### Emulator Networking vs. Physical Device
- **Android Emulator**:
  Android Studio emulators use the special virtual loopback IP `10.0.2.2` to access the development host. The app is pre-configured with `http://10.0.2.2:3000` as the default base URL.
- **Physical Device**:
  Ensure your phone and computer are on the same Wi-Fi network. On the Android onboarding screen, tap **Server Config** and enter your computer's local IP address (e.g. `http://192.168.1.150:3000`).

To run the Android app via command line or compile the debug APK:
```bash
cd android
.\gradlew.bat assembleDebug
```
The resulting APK is generated at:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

### 3. How to Test Two Clients in the Same Room

Testing real-time conversation between two participants is seamless:

1. **Client 1 (Android App or Browser 1)**:
   - Launch the Android App in an emulator or open `http://localhost:3000` in Google Chrome.
   - Choose a codename (e.g., `NeonReaper_101`) and enter.
   - Click **Create Room** -> Spawn a room named "Cyber Crypt".
   - Note the room code (e.g. `DC7X9K`).

2. **Client 2 (Browser 2 / Incognito)**:
   - Open `http://localhost:3000` in an Incognito window or second browser.
   - Select another identity (e.g. `ShadowSpectre_202`).
   - Click **Join Room** and enter `DC7X9K`.

3. **Interact in Real Time**:
   - Send messages: observe immediate appearance on both screens with distinct sender styling and authoritative timestamps.
   - Type in the input field: observe the real-time typing indicator (`"ShadowSpectre_202 is typing..."`).
   - Check the online counter: observe presence incrementing and updating.
   - Owner actions: test kicking or muting participant from the owner menu.

---

## 🧪 Automated Testing

### Running Backend Tests
The backend test suite covers sessions, public and private room lifecycles, unauthorized access rejection, message retention expiration, owner moderation, and Socket.IO real-time events.

```bash
cd server
npm test
```

### Running E2E User Flow Verification
Executes the full 10-step multi-client integration flow:
```bash
cd server
node tests/e2e-verification.js
```

### Running Android Unit Tests
```bash
cd android
.\gradlew.bat testDebugUnitTest
```

---

## 🔒 Security & Privacy Notice

- **No Permanent Accounts**: Identity is tied strictly to ephemeral session tokens that expire after inactivity.
- **Password Protection**: Private room passwords are salted and hashed using `bcrypt`. Password hashes are marked `{ select: false }` in Mongoose and never returned in API payloads.
- **Server-Enforced Expiration**: MongoDB TTL indexes physically prune records, while database queries explicitly filter `{ expiresAt: { $gt: new Date() } }` to eliminate stale access before physical deletion occurs.
- **Transport Security**: In production environments, DeathChat must be hosted behind TLS (`https://` and `wss://`).
- **Anonymity Limitations**: Like all web and mobile platforms, transport-level network information (such as IP addresses) is visible to intermediate network infrastructure and hosting providers. Users are advised never to share sensitive personally identifiable information.

---

## 📄 License
MIT License. Created for the DeathChat project.
