package poker.server.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import poker.server.model.PlayerEntity;
import poker.server.model.TournamentEntity;
import poker.server.model.TournamentParticipant;
import poker.server.repository.PlayerRepository;
import poker.server.repository.TournamentParticipantRepository;
import poker.server.repository.TournamentRepository;
import poker.server.service.GameService;
import poker.server.service.GameService.GamePlayer;
import poker.server.service.GameService.GameState;

/**
 * WebSocket handler for real-time game communication.
 * Handles player actions, game initialization, and broadcasts game state updates.
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private GameService gameService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;
    
    @Autowired
    private TournamentParticipantRepository participantRepository;
    
    // We use Jackson ObjectMapper for JSON processing (Standard in Spring Boot)
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Tracks active WebSocket sessions: Map<SessionID, WebSocketSession>
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Maps SessionID to Player Information for quick lookup
    private final Map<String, PlayerInfo> sessionPlayers = new ConcurrentHashMap<>();
    
    // Timers for auto-advancing turns
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> turnTimers = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> handStartTimers = new ConcurrentHashMap<>();
    
    private static final int TURN_TIMEOUT_SECONDS = 30;
    private static final int HAND_START_DELAY_SECONDS = 3;
    
    /**
     * Internal helper class to link a WebSocket session to a specific player and tournament.
     */
    private static class PlayerInfo {
        Long playerId;
        Long tournamentId;
        
        PlayerInfo(Long playerId, Long tournamentId) {
            this.playerId = playerId;
            this.tournamentId = tournamentId;
        }
    }
    
    /**
     * Invoked after WebSocket negotiation has succeeded and the connection is opened.
     */
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket connection established: " + session.getId());
    }
    
    /**
     * Handle incoming text messages from clients.
     * Parses the JSON payload and delegates to specific handlers based on message type.
     */
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            JsonNode json = objectMapper.readTree(payload);
            String type = json.has("type") ? json.get("type").asText() : "";
            
            switch (type.toUpperCase()) {
                case "CONNECT": 
                case "JOIN":
                    handleJoin(session, json);
                    break;
                    
                case "ACTION":
                    handleAction(session, json);
                    break;
                    
                case "START_HAND":
                    handleStartHand(session, json);
                    break;
                    
                case "ADVANCE_PHASE":
                    handleAdvancePhase(session, json);
                    break;
                    
                default:
                    // Fallback: check if it's an action without a specific type
                    if (json.has("action")) {
                        handleAction(session, json);
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }
    
    /**
     * Invoked after the WebSocket connection has been closed.
     * Cleans up session tracking data.
     */
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        sessionPlayers.remove(session.getId());
        System.out.println("WebSocket connection closed: " + session.getId());
    }
    
    /**
     * Handle player joining a game/tournament channel.
     * Links the session to the player ID and sends the current game state.
     */
    private void handleJoin(WebSocketSession session, JsonNode payload) throws IOException {
        Long playerId = payload.get("playerId").asLong();
        Long tournamentId = payload.get("tournamentId").asLong();
        
        sessionPlayers.put(session.getId(), new PlayerInfo(playerId, tournamentId));
        System.out.println("Player " + playerId + " joined tournament " + tournamentId);
        
        // Send current game state immediately upon join
        // If no game exists yet (WAITING phase), broadcast waiting state to all players in tournament
        GameState game = gameService.getGame(tournamentId);
        if (game == null) {
            broadcastWaitingState(tournamentId);
        } else {
            broadcastGameState(tournamentId);
        }
    }
    
    /**
     * Handle player in-game actions (FOLD, CALL, RAISE, ALL_IN).
     * Validates the player and delegates logic to GameService.
     */
    private void handleAction(WebSocketSession session, JsonNode payload) throws IOException {
        PlayerInfo playerInfo = sessionPlayers.get(session.getId());
        if (playerInfo == null) {
            sendError(session, "Not joined to any game");
            return;
        }
        
        String action = payload.get("action").asText();
        int amount = payload.has("amount") ? payload.get("amount").asInt() : 0;
        
        boolean success = gameService.processAction(
            playerInfo.tournamentId,
            playerInfo.playerId,
            action,
            amount
        );
        
        if (success) {
            // Cancel current turn timer since player acted
            cancelTurnTimer(playerInfo.tournamentId);
            
            // Check if only one player remains (early winner)
            checkEarlyWinner(playerInfo.tournamentId);
            
            GameState game = gameService.getGame(playerInfo.tournamentId);
            
            // Check if betting round is complete
            if (gameService.isBettingRoundComplete(playerInfo.tournamentId)) {
                if (game != null && !game.getPhase().equals("SHOWDOWN") && !game.getPhase().equals("FINISHED")) {
                    gameService.advancePhase(playerInfo.tournamentId);
                    game = gameService.getGame(playerInfo.tournamentId); // Refresh game state
                }
            }
            
            broadcastGameState(playerInfo.tournamentId);
            
            // Start appropriate timer
            if (game != null) {
                if ("SHOWDOWN".equals(game.getPhase()) || "HAND_COMPLETE".equals(game.getPhase())) {
                    scheduleNextHand(playerInfo.tournamentId);
                } else if (!"FINISHED".equals(game.getPhase())) {
                    startTurnTimer(playerInfo.tournamentId);
                }
            }
        } else {
            sendError(session, "Invalid action or not your turn");
        }
    }
    
    /**
     * Handle the request to start a new hand.
     * Includes logic to auto-create the game in memory if it doesn't exist (Self-Healing).
     */
    private void handleStartHand(WebSocketSession session, JsonNode payload) throws IOException {
        PlayerInfo playerInfo = sessionPlayers.get(session.getId());
        if (playerInfo == null) return;
        
        Long tournamentId = playerInfo.tournamentId;
        System.out.println("Request START_HAND for T: " + tournamentId);

        // --- SELF-HEALING LOGIC ---
        // If the server restarted, the game might be missing from memory.
        // We recreate it using the data from the database and connected sockets.
        if (gameService.getGame(tournamentId) == null) {
            System.out.println("Game not found in memory. Auto-creating for T: " + tournamentId);
            
            TournamentEntity tournament = tournamentRepository.findById(tournamentId).orElse(null);
            if (tournament != null) {
                // Find all players currently connected to this tournament via WebSocket
                Set<Long> connectedPlayerIds = sessionPlayers.values().stream()
                        .filter(info -> info.tournamentId.equals(tournamentId))
                        .map(info -> info.playerId)
                        .collect(Collectors.toSet());
                
                if (!connectedPlayerIds.isEmpty()) {
                    List<GamePlayer> gamePlayers = new ArrayList<>();
                    for (Long pid : connectedPlayerIds) {
                        if (pid != null) {
                            PlayerEntity p = playerRepository.findById(pid).orElse(null);
                            if (p != null) {
                                gamePlayers.add(new GamePlayer(p.getId(), p.getUsername(), tournament.getStartingChips()));
                            }
                        }
                    }
                    // Initialize game engine
                    gameService.createGame(tournamentId, gamePlayers);
                    System.out.println("Auto-created game with " + gamePlayers.size() + " players.");
                }
            }
        }
        // ---------------------------
        
        gameService.startHand(tournamentId);
        broadcastGameState(tournamentId);
        
        // Start turn timer
        startTurnTimer(tournamentId);
    }
    
    /**
     * Handle the request to advance to the next phase.
     */
    private void handleAdvancePhase(WebSocketSession session, JsonNode payload) throws IOException {
        PlayerInfo playerInfo = sessionPlayers.get(session.getId());
        if (playerInfo == null) return;
        
        gameService.advancePhase(playerInfo.tournamentId);
        broadcastGameState(playerInfo.tournamentId);
        
        GameState game = gameService.getGame(playerInfo.tournamentId);
        if (game != null) {
            if ("SHOWDOWN".equals(game.getPhase()) || "HAND_COMPLETE".equals(game.getPhase())) {
                // Schedule next hand after showdown
                scheduleNextHand(playerInfo.tournamentId);
            } else {
                // Continue turn timer for next phase
                startTurnTimer(playerInfo.tournamentId);
            }
        }
    }
    
    /**
     * Check if only one player remains and award them the pot.
     * Also checks if only one player has chips left (tournament winner).
     */
    private void checkEarlyWinner(Long tournamentId) {
        GameState game = gameService.getGame(tournamentId);
        if (game == null) return;
        
        // Check if only one non-folded, non-eliminated player in current hand
        long activePlayers = game.getPlayers().stream()
            .filter(p -> !p.isFolded() && !p.isEliminated())
            .count();
        
        if (activePlayers == 1) {
            // Award pot to last remaining player in hand
            GamePlayer winner = game.getPlayers().stream()
                .filter(p -> !p.isFolded() && !p.isEliminated())
                .findFirst()
                .orElse(null);
            
            if (winner != null) {
                winner.setChips(winner.getChips() + game.getPot());
                game.setPot(0);
                game.setPhase("HAND_COMPLETE");
                
                // Check if this was the tournament-winning hand
                long playersWithChips = game.getPlayers().stream()
                    .filter(p -> !p.isEliminated())
                    .count();
                
                if (playersWithChips <= 1) {
                    // Tournament over! Mark as finished and award winnings
                    game.setPhase("FINISHED");
                    gameService.awardTournamentWin(tournamentId);
                } else {
                    // Auto-start next hand after delay
                    scheduleNextHand(tournamentId);
                }
            }
        }
    }
    
    /**
     * Schedule the next hand to start automatically.
     */
    private void scheduleNextHand(Long tournamentId) {
        cancelHandStartTimer(tournamentId);
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                GameState game = gameService.getGame(tournamentId);
                if (game != null && !"FINISHED".equals(game.getPhase())) {
                    gameService.startHand(tournamentId);
                    broadcastGameState(tournamentId);
                    startTurnTimer(tournamentId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, HAND_START_DELAY_SECONDS, TimeUnit.SECONDS);
        
        handStartTimers.put(tournamentId, future);
    }
    
    /**
     * Start turn timer for current player.
     */
    private void startTurnTimer(Long tournamentId) {
        cancelTurnTimer(tournamentId);
        
        GameState game = gameService.getGame(tournamentId);
        if (game == null || "FINISHED".equals(game.getPhase()) || "HAND_COMPLETE".equals(game.getPhase())) {
            return;
        }
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                // Auto-fold if player hasn't acted
                GameState g = gameService.getGame(tournamentId);
                if (g != null && !g.getPhase().equals("FINISHED")) {
                    int currentIndex = g.getCurrentPlayerIndex();
                    if (currentIndex >= 0 && currentIndex < g.getPlayers().size()) {
                        GamePlayer currentPlayer = g.getPlayers().get(currentIndex);
                        System.out.println("Turn timeout for player: " + currentPlayer.getUsername());
                        
                        // Auto-fold
                        gameService.processAction(tournamentId, currentPlayer.getPlayerId(), "FOLD", 0);
                        
                        // Check for early winner
                        checkEarlyWinner(tournamentId);
                        
                        // Check if betting round complete
                        if (gameService.isBettingRoundComplete(tournamentId)) {
                            if (!g.getPhase().equals("SHOWDOWN") && !g.getPhase().equals("FINISHED")) {
                                gameService.advancePhase(tournamentId);
                            }
                        }
                        
                        broadcastGameState(tournamentId);
                        
                        // Start timer for next player
                        startTurnTimer(tournamentId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        
        turnTimers.put(tournamentId, future);
    }
    
    /**
     * Cancel turn timer.
     */
    private void cancelTurnTimer(Long tournamentId) {
        ScheduledFuture<?> future = turnTimers.remove(tournamentId);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    /**
     * Cancel hand start timer.
     */
    private void cancelHandStartTimer(Long tournamentId) {
        ScheduledFuture<?> future = handStartTimers.remove(tournamentId);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    /**
     * Broadcasts the current game state to all players in a specific tournament.
     */
    public void broadcastGameState(Long tournamentId) {
        try {
            // 1. Încercăm să luăm jocul activ
            GameState game = gameService.getGame(tournamentId);
            
            // 2. LOGICA NOUĂ: Dacă nu există joc activ sau e în faza WAITING
            // Trimitem starea de "Lobby" (lista participanților din baza de date)
            if (game == null || "WAITING".equals(game.getPhase())) {
                broadcastWaitingState(tournamentId);
                return;
            }
            
            // 3. Altfel, trimitem starea normală a jocului
            JsonNode gameStateNode = objectMapper.valueToTree(game);
            
            // --- OPTIMIZARE CRITICĂ: Curățăm JSON-ul de date inutile ---
            if (gameStateNode instanceof ObjectNode) {
                ObjectNode stateObj = (ObjectNode) gameStateNode;
                
                // 1. Scoatem pachetul (deja făcut)
                stateObj.remove("deck");
                
                // 2. Scoatem "sortedCards" de la fiecare jucător pentru a reduce mărimea
                if (stateObj.has("players")) {
                    JsonNode playersNode = stateObj.get("players");
                    if (playersNode.isArray()) {
                        for (JsonNode p : playersNode) { // Iterăm prin JsonNode (Jackson)
                            if (p.isObject() && p.has("hand")) {
                                JsonNode hand = p.get("hand");
                                if (hand instanceof ObjectNode) {
                                    ((ObjectNode) hand).remove("sortedCards");
                                }
                            }
                        }
                    }
                }
            }
            // -------------------------------------------------------------
            
            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "GAME_STATE");
            response.set("state", gameStateNode);
            
            String message = response.toString();
            
            // Broadcast to all sessions in this tournament
            for (Map.Entry<String, PlayerInfo> entry : sessionPlayers.entrySet()) {
                if (entry.getValue().tournamentId.equals(tournamentId)) {
                    WebSocketSession session = sessions.get(entry.getKey());
                    if (session != null && session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting game state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sends an error message to a specific session.
     */
    private void sendError(WebSocketSession session, String error) throws IOException {
        ObjectNode err = objectMapper.createObjectNode();
        err.put("type", "ERROR");
        err.put("message", error);
        session.sendMessage(new TextMessage(err.toString()));
    }
    
    /**
     * Sends a WAITING phase state to client when no game is active yet.
     * Creates a minimal game state object with WAITING phase and includes tournament participants.
     */
    private void sendWaitingState(WebSocketSession session, Long tournamentId, Long playerId) throws IOException {
        // Get tournament participants to show who's in the waiting room
        ArrayNode playersArray = objectMapper.createArrayNode();
        
        try {
            // Fetch participants for this tournament with player data eagerly loaded
            List<TournamentParticipant> participants = participantRepository.findByTournamentIdWithPlayer(tournamentId);
            
            for (TournamentParticipant participant : participants) {
                ObjectNode playerNode = objectMapper.createObjectNode();
                playerNode.put("id", participant.getPlayer().getId());
                playerNode.put("username", participant.getPlayer().getUsername());
                playerNode.put("chips", participant.getCurrentChips());
                playerNode.put("currentBet", 0);
                playerNode.put("folded", false);
                playerNode.put("allIn", false);
                playerNode.put("ready", participant.getReady() != null ? participant.getReady() : false);
                playerNode.set("cards", objectMapper.createArrayNode());
                playersArray.add(playerNode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching participants for waiting state: " + e.getMessage());
        }
        
        // Create a minimal waiting state
        ObjectNode waitingState = objectMapper.createObjectNode();
        waitingState.put("phase", "WAITING");
        waitingState.put("pot", 0);
        waitingState.put("currentBet", 0);
        waitingState.put("currentPlayerIndex", -1);
        waitingState.set("communityCards", objectMapper.createArrayNode());
        waitingState.set("players", playersArray);
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "GAME_STATE");
        response.set("state", waitingState);
        
        session.sendMessage(new TextMessage(response.toString()));
        System.out.println("Sent WAITING state to player " + playerId + " with " + playersArray.size() + " players");
    }
    
    /**
     * Broadcasts WAITING state to all players in a tournament.
     * Used when players join/leave during waiting phase.
     */
    private void broadcastWaitingState(Long tournamentId) throws IOException {
        // Build the waiting state once
        ArrayNode playersArray = objectMapper.createArrayNode();
        
        try {
            // Fetch participants for this tournament with player data eagerly loaded
            List<TournamentParticipant> participants = participantRepository.findByTournamentIdWithPlayer(tournamentId);
            
            for (TournamentParticipant participant : participants) {
                ObjectNode playerNode = objectMapper.createObjectNode();
                playerNode.put("id", participant.getPlayer().getId());
                playerNode.put("username", participant.getPlayer().getUsername());
                playerNode.put("chips", participant.getCurrentChips());
                playerNode.put("currentBet", 0);
                playerNode.put("folded", false);
                playerNode.put("allIn", false);
                playerNode.put("ready", participant.getReady() != null ? participant.getReady() : false);
                playerNode.set("cards", objectMapper.createArrayNode());
                playersArray.add(playerNode);
            }
        } catch (Exception e) {
            System.err.println("Error fetching participants for waiting state broadcast: " + e.getMessage());
        }
        
        ObjectNode waitingState = objectMapper.createObjectNode();
        waitingState.put("phase", "WAITING");
        waitingState.put("pot", 0);
        waitingState.put("currentBet", 0);
        waitingState.put("currentPlayerIndex", -1);
        waitingState.set("communityCards", objectMapper.createArrayNode());
        waitingState.set("players", playersArray);
        
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "GAME_STATE");
        response.set("state", waitingState);
        
        String message = response.toString();
        
        // Broadcast to all sessions in this tournament
        for (Map.Entry<String, PlayerInfo> entry : sessionPlayers.entrySet()) {
            if (entry.getValue().tournamentId.equals(tournamentId)) {
                WebSocketSession session = sessions.get(entry.getKey());
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            }
        }
        
        System.out.println("Broadcasted WAITING state to tournament " + tournamentId + " with " + playersArray.size() + " players");
    }
}