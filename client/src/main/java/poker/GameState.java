package poker;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the current state of the game on the client side.
 */
public class GameState {
    
    private String phase;
    private int pot;
    private int currentBet;
    private int currentPlayerIndex;
    private List<String> communityCards;
    private List<PlayerState> players;
    
    public GameState() {
        this.phase = "WAITING";
        this.pot = 0;
        this.currentBet = 0;
        this.currentPlayerIndex = 0;
        this.communityCards = new ArrayList<>();
        this.players = new ArrayList<>();
    }
    
    public static class PlayerState {
        private Long playerId;
        private String username;
        private int chips;
        private boolean folded;
        private boolean eliminated;
        private int currentBet;
        
        public PlayerState(Long playerId, String username, int chips) {
            this.playerId = playerId;
            this.username = username;
            this.chips = chips;
            this.folded = false;
            this.eliminated = false;
            this.currentBet = 0;
        }
        
        // Getters and setters
        public Long getPlayerId() { return playerId; }
        public void setPlayerId(Long playerId) { this.playerId = playerId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public int getChips() { return chips; }
        public void setChips(int chips) { this.chips = chips; }
        
        public boolean isFolded() { return folded; }
        public void setFolded(boolean folded) { this.folded = folded; }
        
        public boolean isEliminated() { return eliminated; }
        public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }
        
        public int getCurrentBet() { return currentBet; }
        public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }
    }
    
    // Getters and setters
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    
    public int getPot() { return pot; }
    public void setPot(int pot) { this.pot = pot; }
    
    public int getCurrentBet() { return currentBet; }
    public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }
    
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
    public void setCurrentPlayerIndex(int index) { this.currentPlayerIndex = index; }
    
    public List<String> getCommunityCards() { return communityCards; }
    public void setCommunityCards(List<String> cards) { this.communityCards = cards; }
    
    public List<PlayerState> getPlayers() { return players; }
    public void setPlayers(List<PlayerState> players) { this.players = players; }
}
