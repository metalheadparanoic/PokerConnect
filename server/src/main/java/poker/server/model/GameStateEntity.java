package poker.server.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing the persistent game state for a tournament.
 * All game state is stored in the database to survive server restarts.
 */
@Entity
@Table(name = "game_states")
public class GameStateEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tournament_id", nullable = false, unique = true)
    private Long tournamentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase = GamePhase.WAITING;
    
    @Column(name = "current_player_id")
    private Long currentPlayerId;
    
    @Column(name = "dealer_position")
    private Integer dealerPosition = 0;
    
    @Column(name = "small_blind")
    private Integer smallBlind = 50;
    
    @Column(name = "big_blind")
    private Integer bigBlind = 100;
    
    @Column(nullable = false)
    private Integer pot = 0;
    
    @Column(name = "current_bet")
    private Integer currentBet = 0;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "community_cards", columnDefinition = "jsonb")
    private String communityCards = "[]";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deck_state", columnDefinition = "jsonb")
    private String deckState = "[]";
    
    @Column(name = "round_number")
    private Integer roundNumber = 0;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public GameStateEntity() {
    }
    
    public GameStateEntity(Long tournamentId) {
        this.tournamentId = tournamentId;
        this.phase = GamePhase.WAITING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle hook to update timestamp
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTournamentId() {
        return tournamentId;
    }
    
    public void setTournamentId(Long tournamentId) {
        this.tournamentId = tournamentId;
    }
    
    public GamePhase getPhase() {
        return phase;
    }
    
    public void setPhase(GamePhase phase) {
        this.phase = phase;
    }
    
    public Long getCurrentPlayerId() {
        return currentPlayerId;
    }
    
    public void setCurrentPlayerId(Long currentPlayerId) {
        this.currentPlayerId = currentPlayerId;
    }
    
    public Integer getDealerPosition() {
        return dealerPosition;
    }
    
    public void setDealerPosition(Integer dealerPosition) {
        this.dealerPosition = dealerPosition;
    }
    
    public Integer getSmallBlind() {
        return smallBlind;
    }
    
    public void setSmallBlind(Integer smallBlind) {
        this.smallBlind = smallBlind;
    }
    
    public Integer getBigBlind() {
        return bigBlind;
    }
    
    public void setBigBlind(Integer bigBlind) {
        this.bigBlind = bigBlind;
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
    
    public String getCommunityCards() {
        return communityCards;
    }
    
    public void setCommunityCards(String communityCards) {
        this.communityCards = communityCards;
    }
    
    public String getDeckState() {
        return deckState;
    }
    
    public void setDeckState(String deckState) {
        this.deckState = deckState;
    }
    
    public Integer getRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
