package poker.common.messages;

/**
 * Message sent from client to server when player takes an action.
 */
public class PlayerActionMessage {
    private Long playerId;
    private Long tournamentId;
    private String actionType; // FOLD, CHECK, CALL, BET, RAISE, ALL_IN, READY
    private Integer amount; // Optional: for BET, RAISE
    
    public PlayerActionMessage() {
    }
    
    public PlayerActionMessage(Long playerId, Long tournamentId, String actionType, Integer amount) {
        this.playerId = playerId;
        this.tournamentId = tournamentId;
        this.actionType = actionType;
        this.amount = amount;
    }
    
    public Long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
    
    public Long getTournamentId() {
        return tournamentId;
    }
    
    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public Integer getAmount() {
        return amount;
    }
    
    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
