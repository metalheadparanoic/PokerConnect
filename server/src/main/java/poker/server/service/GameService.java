package poker.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import poker.common.Card;
import poker.common.Deck;
import poker.common.Hand;
import poker.common.HandEvaluator;
import poker.common.HandEvaluator.HandResult;
import poker.server.model.PlayerEntity;
import poker.server.model.TournamentEntity;
import poker.server.model.TournamentParticipant;
import poker.server.repository.PlayerRepository;
import poker.server.repository.TournamentParticipantRepository;
import poker.server.repository.TournamentRepository;

/**
 * Service for managing game logic and state.
 */
@Service
public class GameService {

    @Autowired
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameStatePersistenceService persistenceService;

    /**
     * Represents a player in a game.
     */
    public static class GamePlayer {

        private Long playerId;
        private String username;
        private int chips;
        private Hand hand;
        private boolean folded;
        private boolean eliminated;
        private int currentBet;

        public GamePlayer(Long playerId, String username, int startingChips) {
            this.playerId = playerId;
            this.username = username;
            this.chips = startingChips;
            this.hand = new Hand();
            this.folded = false;
            this.eliminated = false;
            this.currentBet = 0;
        }

        // Getters and setters
        public Long getPlayerId() {
            return playerId;
        }

        public String getUsername() {
            return username;
        }

        public int getChips() {
            return chips;
        }

        public void setChips(int chips) {
            this.chips = chips;
        }

        public Hand getHand() {
            return hand;
        }

        public boolean isFolded() {
            return folded;
        }

        public void setFolded(boolean folded) {
            this.folded = folded;
        }

        public boolean isEliminated() {
            return eliminated;
        }

        public void setEliminated(boolean eliminated) {
            this.eliminated = eliminated;
        }

        public int getCurrentBet() {
            return currentBet;
        }

        public void setCurrentBet(int currentBet) {
            this.currentBet = currentBet;
        }
    }

    /**
     * Represents a game state.
     */
    public static class GameState {

        private Long tournamentId;
        private List<GamePlayer> players;
        private Deck deck;
        private List<Card> communityCards;
        private int pot;
        private int currentBet;
        private int currentPlayerIndex;
        private int dealerButtonIndex;
        private int smallBlind;
        private int bigBlind;
        private String phase; // WAITING, PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN
        private int handNumber;
        private String lastWinner;

        public GameState(Long tournamentId) {
            this.tournamentId = tournamentId;
            this.players = new ArrayList<>();
            this.deck = new Deck();
            this.communityCards = new ArrayList<>();
            this.pot = 0;
            this.currentBet = 0;
            this.currentPlayerIndex = 0;
            this.dealerButtonIndex = 0;
            this.smallBlind = 10;  // Starting blinds
            this.bigBlind = 20;
            this.phase = "WAITING";
            this.handNumber = 0;
        }

        // Getters and setters
        public Long getTournamentId() {
            return tournamentId;
        }

        public List<GamePlayer> getPlayers() {
            return players;
        }

        public Deck getDeck() {
            return deck;
        }

        public List<Card> getCommunityCards() {
            return communityCards;
        }

        public int getPot() {
            return pot;
        }

        public void setPot(int pot) {
            this.pot = pot;
        }

        public int getCurrentBet() {
            return currentBet;
        }

        public void setCurrentBet(int currentBet) {
            this.currentBet = currentBet;
        }

        public int getCurrentPlayerIndex() {
            return currentPlayerIndex;
        }

        public void setCurrentPlayerIndex(int index) {
            this.currentPlayerIndex = index;
        }

        public int getDealerButtonIndex() {
            return dealerButtonIndex;
        }

        public void setDealerButtonIndex(int index) {
            this.dealerButtonIndex = index;
        }

        public int getSmallBlind() {
            return smallBlind;
        }

        public void setSmallBlind(int amount) {
            this.smallBlind = amount;
        }

        public int getBigBlind() {
            return bigBlind;
        }

        public void setBigBlind(int amount) {
            this.bigBlind = amount;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }

        public int getHandNumber() {
            return handNumber;
        }

        public void setHandNumber(int handNumber) {
            this.handNumber = handNumber;
        }

        public String getLastWinner() {
            return lastWinner;
        }

        public void setLastWinner(String lastWinner) {
            this.lastWinner = lastWinner;
        }
    }

    // Store active games by tournament ID
    private Map<Long, GameState> activeGames = new ConcurrentHashMap<>();

    /**
     * Create a new game for a tournament.
     */
    public GameState createGame(Long tournamentId, List<GamePlayer> players) {
        GameState game = new GameState(tournamentId);
        game.getPlayers().addAll(players);
        activeGames.put(tournamentId, game);

        // Persist to database
        persistenceService.saveGameState(game);

        return game;
    }

    /**
     * Start a new hand in the game.
     */
    public void startHand(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return;
        }

        // Increment hand number
        game.setHandNumber(game.getHandNumber() + 1);
        game.setLastWinner(null);

        // Mark players with no chips as eliminated
        for (GamePlayer player : game.getPlayers()) {
            if (player.getChips() <= 0 && !player.isEliminated()) {
                player.setEliminated(true);
            }
        }

        // Check and update eliminations in database
        checkEliminations(tournamentId);

        // Count players who still have chips
        long playersWithChips = game.getPlayers().stream()
                .filter(p -> !p.isEliminated())
                .count();

        // Check if tournament is over (only 1 player with chips)
        if (playersWithChips <= 1) {
            game.setPhase("FINISHED");
            // Award winnings to the winner
            awardTournamentWin(tournamentId);
            return;
        }

        // Move dealer button
        game.setDealerButtonIndex((game.getDealerButtonIndex() + 1) % game.getPlayers().size());

        // Reset deck and shuffle
        game.getDeck().reset();
        game.getDeck().shuffle();

        // Clear community cards
        game.getCommunityCards().clear();

        // Reset player states
        for (GamePlayer player : game.getPlayers()) {
            player.getHand().clear();
            player.setFolded(false);
            player.setCurrentBet(0);
        }

        // Post blinds
        postBlinds(game);

        // Deal cards to each player
        for (int i = 0; i < 2; i++) {
            for (GamePlayer player : game.getPlayers()) {
                Card card = game.getDeck().deal();
                if (card != null) {
                    player.getHand().addCard(card);
                }
            }
        }

        game.setPhase("PRE_FLOP");

        // Set first player to act (after big blind)
        int firstPlayer = (game.getDealerButtonIndex() + 3) % game.getPlayers().size();
        game.setCurrentPlayerIndex(firstPlayer);

        // Persist to database
        persistenceService.saveGameState(game);
    }

    /**
     * Post small blind and big blind.
     */
    private void postBlinds(GameState game) {
        int numPlayers = game.getPlayers().size();

        // Small blind position (left of dealer)
        int smallBlindIndex = (game.getDealerButtonIndex() + 1) % numPlayers;
        GamePlayer smallBlindPlayer = game.getPlayers().get(smallBlindIndex);
        int sbAmount = Math.min(game.getSmallBlind(), smallBlindPlayer.getChips());
        smallBlindPlayer.setChips(smallBlindPlayer.getChips() - sbAmount);
        smallBlindPlayer.setCurrentBet(sbAmount);
        game.setPot(sbAmount);

        // Big blind position (left of small blind)
        int bigBlindIndex = (game.getDealerButtonIndex() + 2) % numPlayers;
        GamePlayer bigBlindPlayer = game.getPlayers().get(bigBlindIndex);
        int bbAmount = Math.min(game.getBigBlind(), bigBlindPlayer.getChips());
        bigBlindPlayer.setChips(bigBlindPlayer.getChips() - bbAmount);
        bigBlindPlayer.setCurrentBet(bbAmount);
        game.setPot(game.getPot() + bbAmount);

        // Set current bet to big blind
        game.setCurrentBet(bbAmount);
    }

    /**
     * Process a player action (FOLD, CALL, RAISE, ALL_IN).
     */
    public boolean processAction(Long tournamentId, Long playerId, String action, int amount) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return false;
        }

        // Find player index
        int playerIndex = -1;
        for (int i = 0; i < game.getPlayers().size(); i++) {
            if (game.getPlayers().get(i).getPlayerId().equals(playerId)) {
                playerIndex = i;
                break;
            }
        }

        // Validate it's this player's turn
        if (playerIndex != game.getCurrentPlayerIndex()) {
            System.out.println("Not player's turn. Current: " + game.getCurrentPlayerIndex() + ", Player index: " + playerIndex);
            return false;
        }

        GamePlayer player = game.getPlayers().get(playerIndex);

        if (player == null || player.isFolded() || player.isEliminated()) {
            return false;
        }

        switch (action.toUpperCase()) {
            case "FOLD":
                player.setFolded(true);
                break;

            // --- BUG FIX: Adăugat cazul CHECK ---
            case "CHECK":
                // Poți da Check doar dacă nimeni nu a pariat peste tine
                if (game.getCurrentBet() > player.getCurrentBet()) {
                    return false; // Invalid, trebuie să dai Call
                }
                // Check reușit, nu se schimbă banii
                break;
            // ------------------------------------

            case "CALL":
                int callAmount = game.getCurrentBet() - player.getCurrentBet();
                if (callAmount < 0) callAmount = 0; // Safety check

                if (callAmount > player.getChips()) {
                    callAmount = player.getChips(); // All-in
                }
                player.setChips(player.getChips() - callAmount);
                player.setCurrentBet(player.getCurrentBet() + callAmount);
                game.setPot(game.getPot() + callAmount);
                break;

            case "RAISE":
                int raiseAmount = amount - player.getCurrentBet();
                if (raiseAmount > player.getChips()) {
                    raiseAmount = player.getChips(); // All-in
                }
                player.setChips(player.getChips() - raiseAmount);
                player.setCurrentBet(player.getCurrentBet() + raiseAmount);
                game.setPot(game.getPot() + raiseAmount);
                game.setCurrentBet(amount); // Setează noul "High Bet" al mesei
                break;

            case "ALL_IN":
                int allInAmount = player.getChips();
                player.setChips(0);
                player.setCurrentBet(player.getCurrentBet() + allInAmount);
                game.setPot(game.getPot() + allInAmount);
                
                // Dacă All-in-ul meu crește miza mesei, actualizăm
                if (player.getCurrentBet() > game.getCurrentBet()) {
                    game.setCurrentBet(player.getCurrentBet());
                }
                break;

            default:
                return false;
        }

        // Move to next player
        advanceToNextPlayer(game);
        // Persist to database
        persistenceService.saveGameState(game);
        return true;
    }

    /**
     * Advance to the next betting phase (flop, turn, river, showdown).
     */
    public void advancePhase(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return;
        }

        switch (game.getPhase()) {
            case "PRE_FLOP":
                // Deal flop (3 cards)
                for (int i = 0; i < 3; i++) {
                    Card card = game.getDeck().deal();
                    if (card != null) {
                        game.getCommunityCards().add(card);
                    }
                }
                game.setPhase("FLOP");
                break;

            case "FLOP":
                // Deal turn (1 card)
                Card turn = game.getDeck().deal();
                if (turn != null) {
                    game.getCommunityCards().add(turn);
                }
                game.setPhase("TURN");
                break;

            case "TURN":
                // Deal river (1 card)
                Card river = game.getDeck().deal();
                if (river != null) {
                    game.getCommunityCards().add(river);
                }
                game.setPhase("RIVER");
                break;

            case "RIVER":
                game.setPhase("SHOWDOWN");
                // Determine winners and distribute pot
                determineWinners(tournamentId);
                // Check for eliminated players
                checkEliminations(tournamentId);
                
                // Check if tournament is over after distributing pot
                long playersWithChips = game.getPlayers().stream()
                    .filter(p -> !p.isEliminated())
                    .count();
                
                if (playersWithChips <= 1) {
                    game.setPhase("FINISHED");
                    awardTournamentWin(tournamentId);
                }
                
                return; // Don't reset bets - hand is over
        }

        // Reset bets for new betting round
        game.setCurrentBet(0);
        for (GamePlayer player : game.getPlayers()) {
            player.setCurrentBet(0);
        }

        // Persist to database
        persistenceService.saveGameState(game);
    }

    /**
     * Check if betting round is complete and all active players have acted.
     */
    public boolean isBettingRoundComplete(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return false;
        }

        // Get all non-folded, non-eliminated players
        List<GamePlayer> activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isFolded() && !p.isEliminated())
                .collect(Collectors.toList());

        // If only one player left, betting is complete
        if (activePlayers.size() <= 1) {
            return true;
        }

        // Get players who can still act (not all-in)
        List<GamePlayer> playersWhoCanAct = activePlayers.stream()
                .filter(p -> p.getChips() > 0)
                .collect(Collectors.toList());

        // If no one can act (all all-in), betting is complete
        if (playersWhoCanAct.isEmpty()) {
            return true;
        }

        // Check if all players who can act have matched the current bet
        for (GamePlayer player : playersWhoCanAct) {
            if (player.getCurrentBet() < game.getCurrentBet()) {
                return false; // This player hasn't matched the bet
            }
        }

        return true;
    }

    /**
     * Get game state for a tournament. Loads from database if not in memory.
     */
    public GameState getGame(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);

        // If not in memory, try loading from database
        if (game == null) {
            game = persistenceService.loadGameState(tournamentId);
            if (game != null) {
                activeGames.put(tournamentId, game);
                System.out.println("Loaded game state from database for tournament: " + tournamentId);
            }
        }

        return game;
    }

    /**
     * Remove a game from active games.
     */
    public void removeGame(Long tournamentId) {
        activeGames.remove(tournamentId);
        persistenceService.deleteGameState(tournamentId);
    }

    private void advanceToNextPlayer(GameState game) {
        int attempts = 0;
        GamePlayer currentPlayer;
        do {
            game.setCurrentPlayerIndex((game.getCurrentPlayerIndex() + 1) % game.getPlayers().size());
            currentPlayer = game.getPlayers().get(game.getCurrentPlayerIndex());
            attempts++;
        } while ((currentPlayer.isFolded() || currentPlayer.isEliminated())
                && attempts <= game.getPlayers().size());
    }

    /**
     * Determine the winner(s) and distribute the pot.
     */
    public List<GamePlayer> determineWinners(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return new ArrayList<>();
        }

        // Get all non-folded, non-eliminated players
        List<GamePlayer> activePlayers = game.getPlayers().stream()
                .filter(p -> !p.isFolded() && !p.isEliminated())
                .collect(java.util.stream.Collectors.toList());

        if (activePlayers.isEmpty()) {
            return new ArrayList<>();
        }

        // If only one player left, they win
        if (activePlayers.size() == 1) {
            GamePlayer winner = activePlayers.get(0);
            winner.setChips(winner.getChips() + game.getPot());
            game.setPot(0);
            return List.of(winner);
        }

        // Evaluate all hands
        List<HandResult> results = new ArrayList<>();
        for (GamePlayer player : activePlayers) {
            Hand fullHand = new Hand();
            // Add player's hole cards
            fullHand.addCard(player.getHand().getCards().get(0));
            fullHand.addCard(player.getHand().getCards().get(1));
            // Add community cards
            for (Card card : game.getCommunityCards()) {
                fullHand.addCard(card);
            }

            HandResult result = HandEvaluator.evaluateHand(fullHand);
            result.setPlayerId(player.getPlayerId());
            results.add(result);
        }

        // Sort by hand strength (best first)
        results.sort((a, b) -> b.compareTo(a));

        // Find all winners (could be a tie)
        List<GamePlayer> winners = new ArrayList<>();
        HandResult bestResult = results.get(0);

        for (HandResult result : results) {
            if (result.compareTo(bestResult) == 0) {
                GamePlayer player = game.getPlayers().stream()
                        .filter(p -> p.getPlayerId().equals(result.getPlayerId()))
                        .findFirst()
                        .orElse(null);
                if (player != null) {
                    winners.add(player);
                }
            } else {
                break; // No more ties
            }
        }

        // Distribute pot among winners
        distributePot(game, winners);

        return winners;
    }

    /**
     * Distribute the pot among winners.
     */
    private void distributePot(GameState game, List<GamePlayer> winners) {
        if (winners.isEmpty()) return;

        // --- ADĂUGAT PENTRU AFIȘARE CÂȘTIGĂTOR ---
        StringBuilder winnerText = new StringBuilder();
        for (GamePlayer w : winners) {
            if (winnerText.length() > 0) {
                winnerText.append(", ");
            }
            winnerText.append(w.getUsername());
        }
        winnerText.append(" won $").append(game.getPot());
        game.setLastWinner(winnerText.toString());
        // ----------------------------------------

        int potShare = game.getPot() / winners.size();
        int remainder = game.getPot() % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            GamePlayer winner = winners.get(i);
            int winnings = potShare;
            // Give remainder to first winner
            if (i == 0) {
                winnings += remainder;
            }
            winner.setChips(winner.getChips() + winnings);
        }

        game.setPot(0);
    }

    /**
     * Check for eliminated players and update tournament participants.
     */
    public List<GamePlayer> checkEliminations(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return new ArrayList<>();
        }

        List<GamePlayer> eliminatedPlayers = game.getPlayers().stream()
                .filter(p -> p.getChips() <= 0 && !p.isEliminated())
                .collect(java.util.stream.Collectors.toList());

        // Mark players as eliminated in game state
        for (GamePlayer eliminated : eliminatedPlayers) {
            eliminated.setEliminated(true);
            System.out.println("Player " + eliminated.getUsername() + " has been eliminated!");
        }

        // Update tournament participants in database
        for (GamePlayer eliminated : eliminatedPlayers) {
            TournamentParticipant participant = tournamentParticipantRepository
                    .findByTournamentIdAndPlayerId(tournamentId, eliminated.getPlayerId())
                    .orElse(null);

            if (participant != null) {
                participant.setStatus(TournamentParticipant.Status.ELIMINATED);
                participant.setEliminatedAt(java.time.LocalDateTime.now());
                participant.setFinalPosition(game.getPlayers().size());
                tournamentParticipantRepository.save(participant);
            }
        }

        // Update tournament current players count
        TournamentEntity tournament = tournamentRepository.findById(tournamentId).orElse(null);
        if (tournament != null) {
            long activePlayers = tournamentParticipantRepository.countByTournamentIdAndStatus(
                    tournamentId, TournamentParticipant.Status.ACTIVE);
            tournament.setCurrentPlayers((int) activePlayers);

            // If only one player left, tournament is over
            if (activePlayers <= 1) {
                tournament.setStatus(TournamentEntity.Status.FINISHED);

                // Mark winner with position 1 and distribute prize
                if (game.getPlayers().size() == 1) {
                    GamePlayer winner = game.getPlayers().get(0);
                    TournamentParticipant winnerParticipant = tournamentParticipantRepository
                            .findByTournamentIdAndPlayerId(tournamentId, winner.getPlayerId())
                            .orElse(null);
                    if (winnerParticipant != null) {
                        winnerParticipant.setFinalPosition(1);
                        tournamentParticipantRepository.save(winnerParticipant);

                        // Calculate and award prize (total buy-ins)
                        int totalPlayers = tournament.getMaxPlayers(); // or count participants
                        int prizePool = tournament.getBuyIn() * totalPlayers;

                        PlayerEntity winnerPlayer = winnerParticipant.getPlayer();
                        winnerPlayer.setMoney(winnerPlayer.getMoney() + prizePool);
                        winnerPlayer.setTournamentsWon(winnerPlayer.getTournamentsWon() + 1);
                        playerRepository.save(winnerPlayer);

                        // Set tournament winner
                        tournament.setWinnerId(winner.getPlayerId());

                        System.out.println("Tournament " + tournamentId + " won by " + winner.getUsername()
                                + " - Prize: $" + prizePool);
                    }
                }
            }

            tournamentRepository.save(tournament);
        }

        return eliminatedPlayers;
    }

    /**
     * Award tournament win to the last remaining player.
     */
    public void awardTournamentWin(Long tournamentId) {
        GameState game = activeGames.get(tournamentId);
        if (game == null) {
            return;
        }

        // Find the winner (player with chips remaining)
        GamePlayer winner = game.getPlayers().stream()
                .filter(p -> !p.isEliminated())
                .findFirst()
                .orElse(null);

        if (winner == null) {
            return;
        }

        TournamentEntity tournament = tournamentRepository.findById(tournamentId).orElse(null);
        if (tournament == null) {
            return;
        }

        // Set tournament as finished
        tournament.setStatus(TournamentEntity.Status.FINISHED);
        tournament.setWinnerId(winner.getPlayerId());

        // Calculate prize pool (all buy-ins)
        List<TournamentParticipant> allParticipants = tournamentParticipantRepository
                .findByTournamentId(tournamentId);
        int prizePool = tournament.getBuyIn() * allParticipants.size();

        // Award prize to winner
        PlayerEntity winnerPlayer = playerRepository.findById(winner.getPlayerId()).orElse(null);
        if (winnerPlayer != null) {
            winnerPlayer.setMoney(winnerPlayer.getMoney() + prizePool);
            winnerPlayer.setTournamentsWon(winnerPlayer.getTournamentsWon() + 1);
            playerRepository.save(winnerPlayer);

            System.out.println("Tournament " + tournamentId + " won by " + winner.getUsername()
                    + " - Prize: $" + prizePool);
        }

        // Update winner's tournament participant record
        TournamentParticipant winnerParticipant = tournamentParticipantRepository
                .findByTournamentIdAndPlayerId(tournamentId, winner.getPlayerId())
                .orElse(null);
        if (winnerParticipant != null) {
            winnerParticipant.setFinalPosition(1);
            winnerParticipant.setStatus(TournamentParticipant.Status.ACTIVE); // Winner is still active
            tournamentParticipantRepository.save(winnerParticipant);
        }

        tournamentRepository.save(tournament);
        game.setLastWinner(winner.getUsername() + " won the tournament! Prize: $" + prizePool);
    }
}
