package poker.server.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity representing a player's hand and chip state in a tournament.
 * Each player in a tournament has one PlayerHandEntity that persists throughout the game.
 */
@Entity
@Table(name = "player_hands", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tournament_id", "player_id"})
})
public class PlayerHandEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;
    
    @Column(name = "player_id", nullable = false)
    private Long playerId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cards", columnDefinition = "jsonb")
    private String cards = "[]";
    
    @Column(name = "current_chips", nullable = false)
    private Integer currentChips = 0;
    
    @Column(name = "current_bet")
    private Integer currentBet = 0;
    
    @Column(name = "total_bet_this_round")
    private Integer totalBetThisRound = 0;
    
    @Column(nullable = false)
    private Boolean folded = false;
    
    @Column(name = "all_in")
    private Boolean allIn = false;
    
    @Column
    private Integer position;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public PlayerHandEntity() {
    }
    
    public PlayerHandEntity(Long tournamentId, Long playerId, Integer startingChips) {
        this.tournamentId = tournamentId;
        this.playerId = playerId;
        this.currentChips = startingChips;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle hook
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business logic methods
    
    /**
     * Check if player can act (not folded, not all-in).
     */
    public boolean canAct() {
        return !folded && !allIn;
    }
    
    /**
     * Check if player has enough chips to make a bet.
     */
    public boolean hasChips(int amount) {
        return currentChips >= amount;
    }
    
    /**
     * Deduct chips for a bet and update current bet.
     */
    public void makeBet(int amount) {
        if (amount > currentChips) {
            // All-in
            currentBet += currentChips;
            totalBetThisRound += currentChips;
            currentChips = 0;
            allIn = true;
        } else {
            currentChips -= amount;
            currentBet += amount;
            totalBetThisRound += amount;
        }
    }
    
    /**
     * Reset bet counters for new betting round.
     */
    public void resetForNewRound() {
        currentBet = 0;
        totalBetThisRound = 0;
    }
    
    /**
     * Add chips to player (from pot win).
     */
    public void addChips(int amount) {
        currentChips += amount;
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
    
    public Long getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }
    
    public String getCards() {
        return cards;
    }
    
    public void setCards(String cards) {
        this.cards = cards;
    }
    
    public Integer getCurrentChips() {
        return currentChips;
    }
    
    public void setCurrentChips(Integer currentChips) {
        this.currentChips = currentChips;
    }
    
    public Integer getCurrentBet() {
        return currentBet;
    }
    
    public void setCurrentBet(Integer currentBet) {
        this.currentBet = currentBet;
    }
    
    public Integer getTotalBetThisRound() {
        return totalBetThisRound;
    }
    
    public void setTotalBetThisRound(Integer totalBetThisRound) {
        this.totalBetThisRound = totalBetThisRound;
    }
    
    public Boolean getFolded() {
        return folded;
    }
    
    public void setFolded(Boolean folded) {
        this.folded = folded;
    }
    
    public Boolean getAllIn() {
        return allIn;
    }
    
    public void setAllIn(Boolean allIn) {
        this.allIn = allIn;
    }
    
    public Integer getPosition() {
        return position;
    }
    
    public void setPosition(Integer position) {
        this.position = position;
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
