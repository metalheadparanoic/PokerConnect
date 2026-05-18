# Poker Tournament - Java Application

A complete poker tournament application with client-server architecture built with Java, Spring Boot, JavaFX, and PostgreSQL.

## Project Structure

```
poker/
├── common/          # Shared code (Card, Deck, Hand, HandEvaluator)
├── server/          # Spring Boot backend server
├── client/          # JavaFX desktop client
├── docker-compose.yml
└── README.md
```

### Common Module

Contains shared poker logic used by both client and server:

- **Card.java** - Represents a playing card with Suit and Rank enums
- **Deck.java** - Standard 52-card deck with shuffle and deal methods
- **Hand.java** - Player's hand of cards
- **HandRank.java** - Enum for poker hand rankings (High Card to Royal Flush)
- **HandEvaluator.java** - Evaluates poker hands and compares them

### Server Module

Spring Boot application with REST API and WebSocket support:

- **ServerMain.java** - Application entry point
- **Model**
  - PlayerEntity.java - JPA entity for players
  - TournamentEntity.java - JPA entity for tournaments
- **Repository**
  - PlayerRepository.java - Spring Data JPA repository
  - TournamentRepository.java - Tournament database access
- **Controller**
  - AuthController.java - Registration and login endpoints
  - TournamentController.java - Tournament CRUD operations
- **Service**
  - GameService.java - Game logic and state management
- **WebSocket**
  - GameWebSocketHandler.java - Real-time game communication
  - WebSocketConfig.java - WebSocket configuration

### Client Module

JavaFX desktop application:

- **Main.java** - Application entry point and scene manager
- **LoginScreen.java** - User login and registration UI
- **LobbyScreen.java** - Tournament list and creation UI
- **GameScreen.java** - Main poker game interface
- **ServerConnection.java** - HTTP client for REST API calls
- **GameState.java** - Client-side game state model

## Prerequisites

- **Java 21 LTS** (Required - Gradle 8.14 not compatible with Java 25+)
  - Download from: https://adoptium.net/temurin/releases/?version=21
  - Set `JAVA_HOME` environment variable to JDK 21 installation path
- **Docker and Docker Compose** (for PostgreSQL database)
- **Gradle** (wrapper included - no separate installation needed)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd PokerConnect
```

### 2. Configure Java 21

**Windows:**
1. Install JDK 21 from Adoptium/Temurin
2. Set system environment variable:
   - `JAVA_HOME = C:\Program Files\Java\jdk-21` (adjust path as needed)
3. Restart your IDE/terminal

**Verify installation:**
```powershell
java -version
# Should show: java version "21.x.x"
```

### 3. Start the Database

Start PostgreSQL using Docker Compose:

```bash
docker-compose up -d
```

This will:
- Start PostgreSQL 15 on port 5432
- Create database `pokerdb`
- Set up user `poker` with password `pokerpass`
- Persist data in a Docker volume

### 4. Run the Server

Start the Spring Boot server:

```bash
./gradlew :server:bootRun
```

The server will start on `http://localhost:8081` (port 8081)

Database tables will be created automatically via Flyway migrations.

**Expected output:**
```
Started ServerMain in X.XXX seconds
```

### 5. Run the Client (Local)

In a new terminal, start the JavaFX client:

```bash
./gradlew :client:run
```

The client GUI will open showing the login screen.

## 🌐 Playing Online with Friends

The server supports multiplayer connections through WebSocket. Players can connect from different computers.

### Option 1: Local Network (Same WiFi/LAN)

**On the server machine:**

1. Start the server:
   ```powershell
   docker-compose up -d
   ./gradlew :server:bootRun
   ```

2. Find your local IP address:
   ```powershell
   ipconfig
   ```
   Look for "IPv4 Address" (e.g., `192.168.1.100`)

3. **(Optional)** Open firewall port 8081:
   ```powershell
   New-NetFirewallRule -DisplayName "Poker Server" -Direction Inbound -LocalPort 8081 -Protocol TCP -Action Allow
   ```

**On client machines (including server machine):**

Set the server URL before running the client:

```powershell
$env:POKER_SERVER_URL="http://192.168.1.100:8081"
./gradlew :client:run
```

Replace `192.168.1.100` with the actual IP address of the server machine.

### Option 2: Internet (ngrok)

For playing over the internet without port forwarding:

1. Download ngrok: https://ngrok.com/download
2. Start your server locally
3. Expose the server:
   ```powershell
   ngrok http 8081
   ```
4. Copy the public URL (e.g., `https://abc123.ngrok.io`)
5. Players connect with:
   ```powershell
   $env:POKER_SERVER_URL="https://abc123.ngrok.io"
   ./gradlew :client:run
   ```

### Option 3: Cloud Hosting

For persistent hosting, deploy to cloud platforms:

**Railway.app (Recommended):**
- Free tier with $5/month credit
- Automatic PostgreSQL setup
- Visit: https://railway.app

**Render.com:**
- Free tier available
- Manual PostgreSQL configuration
- Visit: https://render.com

**Fly.io:**
- Free tier with limitations
- Docker support
- Visit: https://fly.io

### Environment Variables

The client supports flexible server configuration:

```powershell
# Set server URL
$env:POKER_SERVER_URL="http://localhost:8081"

# Or use Java system property
./gradlew :client:run -Dpoker.serverUrl=http://localhost:8081
```

**Priority order:**
1. `-Dpoker.serverUrl` system property
2. `POKER_SERVER_URL` environment variable
3. `POKER_SERVER_BASE_URL` environment variable
4. Default: `http://localhost:8081`

## API Endpoints

### Authentication

- **POST** `/api/register` - Register a new player
  ```json
  {
    "username": "player1",
    "email": "player1@example.com",
    "password": "password123"
  }
  ```
  **Response:**
  ```json
  {
    "message": "Player registered successfully",
    "playerId": 1,
    "token": "eyJhbGc..."
  }
  ```

- **POST** `/api/login` - Login a player
  ```json
  {
    "username": "player1",
    "password": "password123"
  }
  ```
  **Response:**
  ```json
  {
    "message": "Login successful",
    "playerId": 1,
    "token": "eyJhbGc...",
    "username": "player1"
  }
  ```

### Tournaments

- **GET** `/api/tournaments` - List all tournaments
- **POST** `/api/tournaments` - Create a tournament
  ```json
  {
    "name": "Friday Night Poker",
    "maxPlayers": 8,
    "buyIn": 1000,
    "startingChips": 10000
  }
  ```
- **POST** `/api/tournaments/{id}/join` - Join a tournament
  - Requires `Authorization: Bearer {token}` header
- **POST** `/api/tournaments/{id}/start` - Start a tournament

### WebSocket

- **WS** `/game` - Real-time game communication
  - Connect with `Authorization: Bearer {token}` header
  - Send actions: FOLD, CALL, RAISE, ALL_IN
  - Receive game state updates in real-time
  - Automatic broadcasting to all players in tournament

## Database Configuration

The default database configuration is in `server/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pokerdb
spring.datasource.username=poker
spring.datasource.password=pokerpass
```

To use a different database, update these properties or set environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Card Images

The client is designed to display card images, but they are not included in this repository.

To add card images:

1. Download free poker card images from:
   - [OpenGameArt.org](https://opengameart.org/content/playing-cards-vector-png)
   - [Wikimedia Commons](https://commons.wikimedia.org/wiki/Category:SVG_playing_cards)
   - [Kenney.nl](https://kenney.nl/assets/boardgame-pack)

2. Place PNG images in `client/src/main/resources/cards/`

3. Use naming format: `{rank}{suit}.png` (e.g., `AH.png`, `2D.png`, `KS.png`)

See `client/src/main/resources/cards/README.md` for more details.

## Development

### Project Structure

The project uses a multi-module Gradle build:

```gradle
settings.gradle.kts    # Defines modules
build.gradle.kts       # Root configuration
common/build.gradle.kts
server/build.gradle.kts
client/build.gradle.kts
```

### Technologies Used

**Backend:**
- Spring Boot 3.2.0
- Spring Data JPA
- Spring WebSocket
- Spring Security (JWT authentication)
- PostgreSQL 15
- Flyway (database migrations)
- Jackson (JSON)
- jjwt 0.12.3 (JWT tokens)

**Frontend:**
- JavaFX 21
- Gson (JSON parsing)
- Java HTTP Client (REST calls)
- Custom WebSocket client

**Common:**
- Java 21 LTS
- Gradle 8.14

### Building Distributions

**Create client executable:**

```bash
./gradlew :client:jpackage
```

This will create a native executable in `client/build/jpackage/`.

Note: This requires jpackage tools to be installed (included in JDK 14+).

## Testing

Run all tests:

```bash
./gradlew test
```

Run tests for specific module:

```bash
./gradlew :common:test
./gradlew :server:test
./gradlew :client:test
```

## Troubleshooting

### Database Connection Issues

If the server can't connect to PostgreSQL:

1. Check if Docker container is running:
   ```bash
   docker ps
   ```

2. Check PostgreSQL logs:
   ```bash
   docker-compose logs db
   ```

3. Verify connection settings in `application.properties`

### Port Already in Use

If port 8081 is already in use:

1. Find and stop the process using the port
2. Or change the server port in `application.properties`:
   ```properties
   server.port=8082
   ```
3. Update client connection accordingly:
   ```powershell
   $env:POKER_SERVER_URL="http://localhost:8082"
   ```

### JavaFX Not Working

If JavaFX doesn't start:

1. Ensure you have JDK 21 with JavaFX support
2. Check that the JavaFX modules are correctly configured in `client/build.gradle.kts`
3. Try clearing Gradle cache:
   ```bash
   ./gradlew clean
   ./gradlew :client:run
   ```

### Java Version Issues

If you see errors about Java version compatibility:

1. Verify Java version: `java -version` (must be 21.x.x)
2. Check JAVA_HOME: `echo $env:JAVA_HOME` (Windows PowerShell)
3. Stop Gradle daemon: `./gradlew --stop`
4. Retry the command

### Client Can't Connect to Server

1. Verify server is running and accessible:
   ```powershell
   curl http://localhost:8081/api/tournaments
   ```
2. Check firewall settings (especially for remote connections)
3. Verify the correct IP address and port
4. Ensure `POKER_SERVER_URL` is set correctly

## Next Steps for Development

This is a fully functional multiplayer poker application with online play capabilities. Here are features to enhance further:

### High Priority
- [x] JWT authentication tokens
- [x] WebSocket real-time communication
- [x] Player registration and login
- [x] Tournament creation and management
- [x] Game state persistence
- [x] Multiplayer online support
- [ ] Implement stronger authentication (currently allows unauthenticated WebSocket for testing)
- [ ] Use BCrypt for password hashing (currently using SHA-256)
- [ ] Complete betting UI interactions in GameScreen
- [ ] Add tournament elimination logic
- [ ] Implement blind increases over time

### Medium Priority
- [ ] Implement spectator mode
- [ ] Add chat functionality
- [ ] Create leaderboard system
- [ ] Add tournament scheduling
- [ ] Implement different tournament types (Sit & Go, Multi-table)
- [ ] Add sound effects
- [ ] Player statistics and history

### Low Priority
- [ ] Add player avatars
- [ ] Implement themes/skins
- [ ] Add replay functionality
- [ ] Create admin panel
- [ ] Mobile client support
- [ ] AI opponents for practice

## Security Notes

⚠️ **Important for Production:**

1. **WebSocket Authentication:** Currently allows unauthenticated connections for testing. Change line 77 in `WebSocketConfig.java` to `return false;` for production.

2. **Secret Key:** Update JWT secret in `JwtService.java` with a strong, environment-specific key.

3. **Database Credentials:** Use environment variables for production database credentials instead of hardcoded values.

4. **Password Hashing:** Consider migrating from SHA-256 to BCrypt for stronger password security.

5. **HTTPS:** Use HTTPS/WSS in production, not HTTP/WS.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is open source and available under the MIT License.

## Contact

For questions or issues, please open an issue on GitHub.
