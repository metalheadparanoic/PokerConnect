package poker.common.messages;

/**
 * Message sent when tournament status changes (player joins/leaves, ready state changes).
 */
public class TournamentUpdateMessage {
    private Long tournamentId;
    private String updateType; // PLAYER_JOINED, PLAYER_LEFT, PLAYER_READY, GAME_STARTING, GAME_ENDED
    private Long playerId;
    private String playerUsername;
    private Integer currentPlayerCount;
    private Integer maxPlayers;
    private String message;
    
    public TournamentUpdateMessage() {
    }
    
    public TournamentUpdateMessage(Long tournamentId, String updateType, Long playerId, String playerUsername,
                                  Integer currentPlayerCount, Integer maxPlayers, String message) {
        this.tournamentId = tournamentId;
        this.updateType = updateType;
        this.playerId = playerId;
        this.playerUsername = playerUsername;
        this.currentPlayerCount = currentPlayerCount;
        this.maxPlayers = maxPlayers;
        this.message = message;
    }
    
    public Long getTournamentId() {
        return tournamentId;
    }
    
    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }
    
    public String getUpdateType() {
        return updateType;
    }
    
    public void setUpdateType(String updateType) {
        this.updateType = updateType;
    }
    
    public Long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
    
    public String getPlayerUsername() {
        return playerUsername;
    }
    
    public void setPlayerUsername(String playerUsername) {
        this.playerUsername = playerUsername;
    }
    
    public Integer getCurrentPlayerCount() {
        return currentPlayerCount;
    }
    
    public void setCurrentPlayerCount(Integer currentPlayerCount) {
        this.currentPlayerCount = currentPlayerCount;
    }
    
    public Integer getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
