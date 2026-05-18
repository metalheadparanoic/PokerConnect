package poker.common.messages;

import java.util.List;

/**
 * Player state information sent in game state updates.
 */
public class PlayerStateDTO {
    private Long playerId;
    private String username;
    private Integer chips;
    private Integer currentBet;
    private Boolean folded;
    private Boolean eliminated;
    private Boolean allIn;
    private List<String> cards; // Only visible to the player themselves
    private Integer position;
    private Boolean isReady;
    
    public PlayerStateDTO() {
    }
    
    public PlayerStateDTO(Long playerId, String username, Integer chips, Integer currentBet,
                         Boolean folded, Boolean eliminated, Boolean allIn, List<String> cards, Integer position, Boolean isReady) {
        this.playerId = playerId;
        this.username = username;
        this.chips = chips;
        this.currentBet = currentBet;
        this.folded = folded;
        this.eliminated = eliminated;
        this.allIn = allIn;
        this.cards = cards;
        this.position = position;
        this.isReady = isReady;
    }
    
    public Long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Integer getChips() {
        return chips;
    }
    
    public void setChips(Integer chips) {
        this.chips = chips;
    }
    
    public Integer getCurrentBet() {
        return currentBet;
    }
    
    public void setCurrentBet(Integer currentBet) {
        this.currentBet = currentBet;
    }
    
    public Boolean getFolded() {
        return folded;
    }
    
    public void setFolded(Boolean folded) {
        this.folded = folded;
    }
    
    public Boolean getEliminated() {
        return eliminated;
    }
    
    public void setEliminated(Boolean eliminated) {
        this.eliminated = eliminated;
    }
    
    public Boolean getAllIn() {
        return allIn;
    }
    
    public void setAllIn(Boolean allIn) {
        this.allIn = allIn;
    }
    
    public List<String> getCards() {
        return cards;
    }
    
    public void setCards(List<String> cards) {
        this.cards = cards;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
    }
    
    public Boolean getIsReady() {
        return isReady;
    }
    
    public void setIsReady(Boolean isReady) {
        this.isReady = isReady;
    }
}
