package poker.server.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA Entity representing a player's participation in a tournament.
 * This is the join table between tournaments and players.
 */
@Entity
@Table(name = "tournament_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tournament_id", "player_id"})
})
public class TournamentParticipant {
    
    public enum Status {
        ACTIVE,      // Currently playing
        ELIMINATED,  // Lost all chips
        LEFT         // Voluntarily left before elimination
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
    
    @Column(nullable = false)
    private Integer currentChips;
    
    @Column(nullable = false)
    private LocalDateTime joinedAt;
    
    private LocalDateTime eliminatedAt;
    
    private Integer finalPosition;
    
    @Column(nullable = true, columnDefinition = "boolean default false")
    private Boolean ready = false;
    
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
    
    // Constructors
    public TournamentParticipant() {
    }
    
    public TournamentParticipant(TournamentEntity tournament, PlayerEntity player, Integer startingChips) {
        this.tournament = tournament;
        this.player = player;
        this.currentChips = startingChips;
        this.status = Status.ACTIVE;
        this.joinedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public TournamentEntity getTournament() {
        return tournament;
    }
    
    public void setTournament(TournamentEntity tournament) {
        this.tournament = tournament;
    }
    
    public PlayerEntity getPlayer() {
        return player;
    }
    
    public void setPlayer(PlayerEntity player) {
        this.player = player;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
        if (status == Status.ELIMINATED || status == Status.LEFT) {
            this.eliminatedAt = LocalDateTime.now();
        }
    }
    
    public Integer getCurrentChips() {
        return currentChips;
    }
    
    public void setCurrentChips(Integer currentChips) {
        this.currentChips = currentChips;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public LocalDateTime getEliminatedAt() {
        return eliminatedAt;
    }
    
    public void setEliminatedAt(LocalDateTime eliminatedAt) {
        this.eliminatedAt = eliminatedAt;
    }
    
    public Integer getFinalPosition() {
        return finalPosition;
    }
    
    public void setFinalPosition(Integer finalPosition) {
        this.finalPosition = finalPosition;
    }
    
    public Boolean getReady() {
        return ready;
    }
    
    public void setReady(Boolean ready) {
        this.ready = ready;
    }
}
