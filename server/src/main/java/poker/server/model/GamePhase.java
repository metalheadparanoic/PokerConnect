package poker.server.model;

/**
 * Enum representing the different phases of a poker game.
 * Used to control game flow and UI state transitions.
 */
public enum GamePhase {
    /**
     * Waiting for players to join and mark ready.
     */
    WAITING,
    
    /**
     * Pre-flop betting round (after hole cards dealt).
     */
    PRE_FLOP,
    
    /**
     * Flop betting round (after 3 community cards).
     */
    FLOP,
    
    /**
     * Turn betting round (after 4th community card).
     */
    TURN,
    
    /**
     * River betting round (after 5th community card).
     */
    RIVER,
    
    /**
     * Showdown phase - revealing hands and determining winner.
     */
    SHOWDOWN,
    
    /**
     * Game ended, winner determined.
     */
    ENDED
}
