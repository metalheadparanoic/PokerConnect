package poker.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional; // IMPORT IMPORTANT
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poker.server.model.PlayerEntity;
import poker.server.model.TournamentEntity;
import poker.server.model.TournamentParticipant;
import poker.server.repository.PlayerRepository;
import poker.server.repository.TournamentParticipantRepository;
import poker.server.repository.TournamentRepository;
import poker.server.service.GameService;
import poker.server.service.GameService.GamePlayer;
import poker.server.websocket.GameWebSocketHandler;

/**
 * REST controller for tournament management.
 */
@RestController
@RequestMapping("/api/tournaments")
@CrossOrigin(origins = "*")
public class TournamentController {

    private static final Logger log = LoggerFactory.getLogger(TournamentController.class);

    @Autowired
    private GameWebSocketHandler gameWebSocketHandler;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameService gameService;

    @Autowired
    private TournamentParticipantRepository participantRepository;

    @GetMapping
    public ResponseEntity<List<TournamentEntity>> getAllTournaments() {
        List<TournamentEntity> tournaments = tournamentRepository.findAll().stream()
                .filter(t -> t.getStatus() != TournamentEntity.Status.FINISHED)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(tournaments);
    }

    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getTournamentHistory() {
        List<TournamentEntity> tournaments = tournamentRepository.findAll().stream()
                .filter(t -> t.getStatus() == TournamentEntity.Status.FINISHED)
                .collect(java.util.stream.Collectors.toList());
        
        // Enrich with winner information
        List<Map<String, Object>> enrichedTournaments = new ArrayList<>();
        for (TournamentEntity tournament : tournaments) {
            Map<String, Object> tournamentData = new HashMap<>();
            tournamentData.put("id", tournament.getId());
            tournamentData.put("name", tournament.getName());
            tournamentData.put("buyIn", tournament.getBuyIn());
            tournamentData.put("maxPlayers", tournament.getMaxPlayers());
            tournamentData.put("status", tournament.getStatus());
            
            // Get winner information
            if (tournament.getWinnerId() != null) {
                PlayerEntity winner = playerRepository.findById(tournament.getWinnerId()).orElse(null);
                if (winner != null) {
                    tournamentData.put("winnerName", winner.getUsername());
                    // Calculate prize (all buy-ins)
                    int totalParticipants = participantRepository.countByTournamentId(tournament.getId());
                    int prizeAmount = tournament.getBuyIn() * totalParticipants;
                    tournamentData.put("prizeAmount", prizeAmount);
                }
            }
            enrichedTournaments.add(tournamentData);
        }
        return ResponseEntity.ok(enrichedTournaments);
    }
    
    @PostMapping("/deleteAll")
    public ResponseEntity<?> deleteAllTournaments() {
        try {
            List<TournamentEntity> allTournaments = tournamentRepository.findAll();
            for (TournamentEntity tournament : allTournaments) {
                // Remove game from memory
                gameService.removeGame(tournament.getId());
            }
            // Delete all participants first (foreign key constraint)
            participantRepository.deleteAll();
            // Delete all tournaments
            tournamentRepository.deleteAll();
            return ResponseEntity.ok(Map.of("message", "All tournaments deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete tournaments: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTournament(@PathVariable("id") @NonNull Long id) {
        return tournamentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTournament(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            Integer maxPlayers = request.get("maxPlayers") instanceof Number ? ((Number) request.get("maxPlayers")).intValue() : null;
            Integer buyIn = request.get("buyIn") instanceof Number ? ((Number) request.get("buyIn")).intValue() : null;
            Integer startingChips = request.get("startingChips") instanceof Number ? ((Number) request.get("startingChips")).intValue() : null;

            if (name == null || maxPlayers == null || buyIn == null || startingChips == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            if (maxPlayers < 2 || maxPlayers > 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Max players must be between 2 and 6"));
            }

            TournamentEntity tournament = new TournamentEntity(name, maxPlayers, buyIn, startingChips);
            tournament = tournamentRepository.save(tournament);
            return ResponseEntity.status(HttpStatus.CREATED).body(tournament);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Tournament creation failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinTournament(@PathVariable("id") @NonNull Long id, @RequestBody Map<String, Long> request) {
        try {
            TournamentEntity tournament = tournamentRepository.findById(id).orElse(null);
            if (tournament == null) {
                return ResponseEntity.notFound().build();
            }

            Long playerId = ((Number) request.get("playerId")).longValue();
            PlayerEntity player = playerRepository.findById(playerId).orElse(null);
            if (player == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Player not found"));
            }

            if (player.getChips() < tournament.getBuyIn()) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of("error", "Insufficient chips"));
            }

            if (participantRepository.existsByTournamentIdAndPlayerId(id, playerId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Already joined this tournament"));
            }

            if (tournament.getCurrentPlayers() >= tournament.getMaxPlayers()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tournament is full"));
            }

            player.setChips(player.getChips() - tournament.getBuyIn());
            playerRepository.save(player);

            TournamentParticipant participant = new TournamentParticipant(tournament, player, tournament.getStartingChips());
            participantRepository.save(participant);

            tournament.setCurrentPlayers(tournament.getCurrentPlayers() + 1);
            tournament = tournamentRepository.save(tournament);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully joined tournament");
            response.put("tournament", tournament);
            response.put("newBalance", player.getChips());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to join tournament: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveTournament(@PathVariable("id") @NonNull Long id, @RequestBody Map<String, Long> request) {
        try {
            Long playerId = ((Number) request.get("playerId")).longValue();
            TournamentParticipant participant = participantRepository.findByTournamentIdAndPlayerId(id, playerId).orElse(null);

            if (participant == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Not in this tournament"));
            }

            GameService.GameState game = gameService.getGame(id);
            boolean inProgress = game != null && !"WAITING".equals(game.getPhase()) && !"FINISHED".equals(game.getPhase());

            participant.setStatus(inProgress ? TournamentParticipant.Status.ELIMINATED : TournamentParticipant.Status.LEFT);
            participantRepository.save(participant);

            // If a game exists and isn't finished, eliminate the player immediately
            if (game != null && !"FINISHED".equals(game.getPhase())) {
                gameWebSocketHandler.handlePlayerLeft(id, playerId);
            }

            TournamentEntity tournament = participant.getTournament();
            if (tournament.getCurrentPlayers() > 0) {
                tournament.setCurrentPlayers(tournament.getCurrentPlayers() - 1);
                if (tournament.getCurrentPlayers() == 0) {
                    tournament.setStatus(TournamentEntity.Status.WAITING);
                    gameService.removeGame(id);
                }
                tournamentRepository.save(tournament);
            }
            return ResponseEntity.ok(Map.of("message", "Left tournament successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to leave: " + e.getMessage()));
        }
    }

    // --- METODELE CRITICE CU @TRANSACTIONAL ---
    @PostMapping("/{id}/ready")
    @Transactional
    public ResponseEntity<?> toggleReady(@PathVariable("id") @NonNull Long id, @RequestBody Map<String, Object> request) {
        try {
            Long playerId = request.get("playerId") instanceof Number ? ((Number) request.get("playerId")).longValue() : null;
            Boolean ready = (Boolean) request.get("ready");
            
            if (playerId == null || ready == null) return ResponseEntity.badRequest().body(Map.of("error", "Missing data"));
            
            TournamentParticipant participant = participantRepository.findByTournamentIdAndPlayerId(id, playerId).orElse(null);
            if (participant == null) return ResponseEntity.notFound().build();
            
            participant.setReady(ready);
            participantRepository.save(participant);
            
            // Verificăm dacă toți sunt gata
            List<TournamentParticipant> allParticipants = participantRepository.findByTournamentIdAndStatus(id, TournamentParticipant.Status.ACTIVE);
            boolean allReady = allParticipants.stream().allMatch(p -> p.getReady() != null && p.getReady());
            
            // Dacă suntem destui jucători și toți sunt gata -> START JOC
            if (allReady && allParticipants.size() >= 2) {
                TournamentEntity tournament = tournamentRepository.findById(id).orElse(null);
                if (tournament != null && tournament.getStatus() == TournamentEntity.Status.WAITING) {
                    // Keep a deterministic seating order (join order) so dealer/blinds rotation
                    // starts from the player who opened the table.
                    List<TournamentParticipant> ordered = allParticipants.stream()
                        .sorted(java.util.Comparator.comparing(TournamentParticipant::getJoinedAt))
                        .toList();

                    List<GamePlayer> gamePlayers = new ArrayList<>();
                    for (TournamentParticipant tp : ordered) {
                        PlayerEntity p = tp.getPlayer();
                        gamePlayers.add(new GamePlayer(p.getId(), p.getUsername(), tournament.getStartingChips()));
                    }
                    if (!gamePlayers.isEmpty()) {
                        System.out.println("Starting game for tournament " + id);
                        gameService.createGame(id, gamePlayers);
                        
                        // <--- ADAUGĂ ACEASTĂ LINIE NOUĂ AICI:
                        gameService.startHand(id); 
                        // Acum jocul trece din WAITING în PRE_FLOP și se împart cărțile
                        
                        tournament.setStatus(TournamentEntity.Status.IN_PROGRESS);
                        tournamentRepository.save(tournament);
                        
                        gameWebSocketHandler.broadcastGameState(id); 
                    }
                }
            } else {
                // Dacă nu a început jocul, trimitem totuși un update ca să vadă ceilalți că un jucător e Ready
                gameWebSocketHandler.broadcastGameState(id);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("ready", ready);
            response.put("allReady", allReady);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update ready status for tournament {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to ready: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/ready")
    @Transactional // Fix pentru 500 Error (Lazy Loading)
    public ResponseEntity<?> getReadyStatus(@PathVariable("id") @NonNull Long id) {
        try {
            List<TournamentParticipant> participants = participantRepository.findByTournamentIdAndStatus(id, TournamentParticipant.Status.ACTIVE);

            List<Map<String, Object>> readyList = new ArrayList<>();
            for (TournamentParticipant p : participants) {
                Map<String, Object> playerReady = new HashMap<>();
                playerReady.put("playerId", p.getPlayer().getId());
                // Aici aparea eroarea 500, acum @Transactional o repara:
                playerReady.put("username", p.getPlayer().getUsername());
                playerReady.put("ready", p.getReady() != null && p.getReady());
                readyList.add(playerReady);
            }
            return ResponseEntity.ok(readyList);
        } catch (Exception e) {
            log.error("Failed to get ready status for tournament {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed get status: " + e.getMessage()));
        }
    }
}
