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

- **Java 17 or higher** (Java 21 recommended)
- **Docker and Docker Compose** (for PostgreSQL database)
- **Gradle** (wrapper included)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Poker_Java
```

### 2. Start the Database

Start PostgreSQL using Docker Compose:

```bash
docker-compose up -d db
```

This will:
- Start PostgreSQL 15 on port 5432
- Create database `pokerdb`
- Set up user `poker` with password `pokerpass`
- Persist data in a Docker volume

### 3. Build the Project

```bash
./gradlew build
```

This will compile all modules and run tests.

### 4. Run the Server

Start the Spring Boot server:

```bash
./gradlew :server:bootRun
```

The server will start on `http://localhost:8080`

Database tables will be created automatically via JPA/Hibernate.

### 5. Run the Client

In a new terminal, start the JavaFX client:

```bash
./gradlew :client:run
```

The client GUI will open showing the login screen.

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

- **POST** `/api/login` - Login a player
  ```json
  {
    "username": "player1",
    "password": "password123"
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
- **POST** `/api/tournaments/{id}/start` - Start a tournament

### WebSocket

- **WS** `/game` - Real-time game communication
  - Connect with player and tournament IDs
  - Send actions: FOLD, CALL, RAISE, ALL_IN
  - Receive game state updates

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
- Spring Boot 3.2
- Spring Data JPA
- Spring WebSocket
- PostgreSQL
- Jackson (JSON)

**Frontend:**
- JavaFX 21
- Gson (JSON parsing)
- Java-WebSocket (WebSocket client)

**Common:**
- Java 21 (can run on Java 17+)

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

If port 8080 is already in use:

1. Find and stop the process using port 8080
2. Or change the server port in `application.properties`:
   ```properties
   server.port=8081
   ```
3. Update the client's `ServerConnection.java` BASE_URL accordingly

### JavaFX Not Working

If JavaFX doesn't start:

1. Ensure you have Java 17 or higher with JavaFX support
2. Check that the JavaFX modules are correctly configured in `client/build.gradle.kts`

## Next Steps for Development

This is a working foundation. Here are features to implement next:

### High Priority
- [ ] Implement JWT authentication tokens (currently using simple tokens)
- [ ] Use BCrypt for password hashing (currently using SHA-256)
- [ ] Complete WebSocket integration in GameScreen
- [ ] Add player-to-tournament relationship table
- [ ] Implement full game flow (blinds, betting rounds, showdown)
- [ ] Add hand history and winner determination

### Medium Priority
- [ ] Implement spectator mode
- [ ] Add chat functionality
- [ ] Create leaderboard system
- [ ] Add tournament scheduling
- [ ] Implement different tournament types (Sit & Go, Multi-table)
- [ ] Add sound effects

### Low Priority
- [ ] Add player avatars
- [ ] Implement themes/skins
- [ ] Add statistics and analytics
- [ ] Create admin panel
- [ ] Add replay functionality

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is open source and available under the MIT License.

## Contact

For questions or issues, please open an issue on GitHub.
