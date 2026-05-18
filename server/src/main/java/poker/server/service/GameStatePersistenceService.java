package poker.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import poker.common.Card;
import poker.server.model.GamePhase;
import poker.server.model.GameStateEntity;
import poker.server.model.PlayerHandEntity;
import poker.server.repository.GameStateRepository;
import poker.server.repository.PlayerHandRepository;
import poker.server.service.GameService.GamePlayer;
import poker.server.service.GameService.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for persisting and loading game state to/from database.
 * Bridges the gap between in-memory GameState and database entities.
 */
@Service
public class GameStatePersistenceService {
    
    @Autowired
    private GameStateRepository gameStateRepository;
    
    @Autowired
    private PlayerHandRepository playerHandRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Save the current game state to database.
     */
    @Transactional
    public void saveGameState(GameState game) {
        if (game == null) return;
        
        try {
            // Find or create GameStateEntity
            GameStateEntity entity = gameStateRepository.findByTournamentId(game.getTournamentId())
                .orElse(new GameStateEntity());
            
            entity.setTournamentId(game.getTournamentId());
            entity.setPhase(GamePhase.valueOf(game.getPhase()));
            entity.setCurrentPlayerId(getCurrentPlayerId(game));
            entity.setDealerPosition(game.getDealerButtonIndex());
            entity.setPot(game.getPot());
            entity.setCurrentBet(game.getCurrentBet());
            
            // Serialize community cards to JSON
            List<String> communityCardStrings = game.getCommunityCards().stream()
                .map(this::cardToString)
                .collect(Collectors.toList());
            entity.setCommunityCards(objectMapper.writeValueAsString(communityCardStrings));
            
            // Serialize deck state to JSON (card list)
            List<String> deckCards = game.getDeck().getCards().stream()
                .map(this::cardToString)
                .collect(Collectors.toList());
            entity.setDeckState(objectMapper.writeValueAsString(deckCards));
            
            gameStateRepository.save(entity);
            
            // Save all player hands
            savePlayerHands(game);
            
        } catch (Exception e) {
            System.err.println("Error saving game state: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save all player hands for a game.
     */
    @Transactional
    public void savePlayerHands(GameState game) throws JsonProcessingException {
        for (GamePlayer player : game.getPlayers()) {
            PlayerHandEntity handEntity = playerHandRepository
                .findByTournamentIdAndPlayerId(game.getTournamentId(), player.getPlayerId())
                .orElse(new PlayerHandEntity());
            
            handEntity.setTournamentId(game.getTournamentId());
            handEntity.setPlayerId(player.getPlayerId());
            handEntity.setCurrentChips(player.getChips());
            handEntity.setCurrentBet(player.getCurrentBet());
            handEntity.setFolded(player.isFolded());
            handEntity.setAllIn(player.getChips() <= 0 && player.getCurrentBet() > 0);
            
            // Serialize player's cards
            List<String> cardStrings = player.getHand().getCards().stream()
                .map(this::cardToString)
                .collect(Collectors.toList());
            handEntity.setCards(objectMapper.writeValueAsString(cardStrings));
            
            playerHandRepository.save(handEntity);
        }
    }
    
    /**
     * Load game state from database.
     * Returns null if no saved state exists.
     */
    @Transactional(readOnly = true)
    public GameState loadGameState(Long tournamentId) {
        try {
            GameStateEntity entity = gameStateRepository.findByTournamentId(tournamentId)
                .orElse(null);
            
            if (entity == null) {
                return null;
            }
            
            // Reconstruct GameState from entity
            GameState game = new GameState(tournamentId);
            game.setPhase(entity.getPhase().name());
            game.setPot(entity.getPot());
            game.setCurrentBet(entity.getCurrentBet());
            game.setDealerButtonIndex(entity.getDealerPosition());
            
            // Deserialize community cards
            List<String> communityCardStrings = objectMapper.readValue(
                entity.getCommunityCards(), new TypeReference<List<String>>() {});
            for (String cardStr : communityCardStrings) {
                Card card = stringToCard(cardStr);
                if (card != null) {
                    game.getCommunityCards().add(card);
                }
            }
            
            // Deserialize deck
            game.getDeck().getCards().clear();
            List<String> deckCardStrings = objectMapper.readValue(
                entity.getDeckState(), new TypeReference<List<String>>() {});
            for (String cardStr : deckCardStrings) {
                Card card = stringToCard(cardStr);
                if (card != null) {
                    game.getDeck().getCards().add(card);
                }
            }
            
            // Load player hands
            List<PlayerHandEntity> hands = playerHandRepository.findByTournamentId(tournamentId);
            for (PlayerHandEntity handEntity : hands) {
                GamePlayer player = new GamePlayer(
                    handEntity.getPlayerId(),
                    "", // Username will need to be fetched from PlayerEntity if needed
                    handEntity.getCurrentChips()
                );
                player.setCurrentBet(handEntity.getCurrentBet());
                player.setFolded(handEntity.getFolded());
                
                // Deserialize cards
                List<String> cardStrings = objectMapper.readValue(
                    handEntity.getCards(), new TypeReference<List<String>>() {});
                for (String cardStr : cardStrings) {
                    Card card = stringToCard(cardStr);
                    if (card != null) {
                        player.getHand().addCard(card);
                    }
                }
                
                game.getPlayers().add(player);
            }
            
            // Set current player index based on current player ID
            if (entity.getCurrentPlayerId() != null) {
                for (int i = 0; i < game.getPlayers().size(); i++) {
                    if (game.getPlayers().get(i).getPlayerId().equals(entity.getCurrentPlayerId())) {
                        game.setCurrentPlayerIndex(i);
                        break;
                    }
                }
            }
            
            return game;
            
        } catch (Exception e) {
            System.err.println("Error loading game state: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Delete game state from database.
     */
    @Transactional
    public void deleteGameState(Long tournamentId) {
        gameStateRepository.deleteByTournamentId(tournamentId);
        playerHandRepository.deleteByTournamentId(tournamentId);
    }
    
    /**
     * Check if game state exists in database.
     */
    public boolean gameStateExists(Long tournamentId) {
        return gameStateRepository.existsByTournamentId(tournamentId);
    }
    
    /**
     * Convert Card to simple string format: "AS" (Ace of Spades), "KH" (King of Hearts).
     */
    private String cardToString(Card card) {
        return card.getRank().getSymbol() + card.getSuit().getSymbol();
    }
    
    /**
     * Convert string back to Card object.
     */
    private Card stringToCard(String str) {
        if (str == null || str.length() != 2) return null;
        
        String rankStr = str.substring(0, 1);
        String suitStr = str.substring(1, 2);
        
        Card.Rank rank = null;
        for (Card.Rank r : Card.Rank.values()) {
            if (r.getSymbol().equals(rankStr)) {
                rank = r;
                break;
            }
        }
        
        Card.Suit suit = null;
        for (Card.Suit s : Card.Suit.values()) {
            if (s.getSymbol().equals(suitStr)) {
                suit = s;
                break;
            }
        }
        
        if (rank != null && suit != null) {
            return new Card(rank, suit);
        }
        
        return null;
    }
    
    /**
     * Get current player ID from game state.
     */
    private Long getCurrentPlayerId(GameState game) {
        int index = game.getCurrentPlayerIndex();
        if (index >= 0 && index < game.getPlayers().size()) {
            return game.getPlayers().get(index).getPlayerId();
        }
        return null;
    }
}
