package poker.common.messages;

import java.util.List;

/**
 * Message sent to clients with current game state updates.
 */
public class GameStateMessage {
    private String phase;
    private Long currentPlayerId;
    private Integer pot;
    private Integer currentBet;
    private List<String> communityCards;
    private List<PlayerStateDTO> players;
    private String message;
    
    public GameStateMessage() {
    }
    
    public GameStateMessage(String phase, Long currentPlayerId, Integer pot, Integer currentBet,
                           List<String> communityCards, List<PlayerStateDTO> players, String message) {
        this.phase = phase;
        this.currentPlayerId = currentPlayerId;
        this.pot = pot;
        this.currentBet = currentBet;
        this.communityCards = communityCards;
        this.players = players;
        this.message = message;
    }
    
    public String getPhase() {
        return phase;
    }
    
    public void setPhase(String phase) {
        this.phase = phase;
    }
    
    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }
    
    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
    
    public Integer getPot() {
        return pot;
    }
    
    public void setPot(Integer pot) {
        this.pot = pot;
    }
    
    public Integer getCurrentBet() {
        return currentBet;
    }
    
    public void setCurrentBet(Integer currentBet) {
        this.currentBet = currentBet;
    }
    
    public List<String> getCommunityCards() {
        return communityCards;
    }
    
    public void setCommunityCards(List<String> communityCards) {
        this.communityCards = communityCards;
    }
    
    public List<PlayerStateDTO> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<PlayerStateDTO> players) {
        this.players = players;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
