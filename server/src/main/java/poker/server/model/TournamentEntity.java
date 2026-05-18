package poker.server.model;

import jakarta.persistence.*;

/**
 * JPA Entity representing a poker tournament.
 */
@Entity
@Table(name = "tournaments")
public class TournamentEntity {
    
    public enum Status {
        WAITING, IN_PROGRESS, FINISHED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.WAITING;
    
    @Column(nullable = false)
    private Integer maxPlayers;
    
    @Column(nullable = false)
    private Integer currentPlayers = 0;
    
    @Column(nullable = false)
    private Integer buyIn;
    
    @Column(nullable = false)
    private Integer startingChips;
    
    private Long hostId; // Player who created the tournament
    
    private Long winnerId;
    
    // Constructors
    public TournamentEntity() {
    }
    
    public TournamentEntity(String name, Integer maxPlayers, Integer buyIn, Integer startingChips) {
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.buyIn = buyIn;
        this.startingChips = startingChips;
        this.status = Status.WAITING;
        this.currentPlayers = 0;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public Integer getMaxPlayers() {
        return maxPlayers;
    }
    
    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
    
    public Integer getCurrentPlayers() {
        return currentPlayers;
    }
    
    public void setCurrentPlayers(Integer currentPlayers) {
        this.currentPlayers = currentPlayers;
    }
    
    public Integer getBuyIn() {
        return buyIn;
    }
    
    public void setBuyIn(Integer buyIn) {
        this.buyIn = buyIn;
    }
    
    public Integer getStartingChips() {
        return startingChips;
    }
    
    public void setStartingChips(Integer startingChips) {
        this.startingChips = startingChips;
    }
    
    public Long getWinnerId() {
        return winnerId;
    }
    
    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }
}
