package poker.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import poker.server.model.TournamentParticipant;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TournamentParticipant entity.
 */
@Repository
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    
    /**
     * Find all participants in a specific tournament.
     */
    List<TournamentParticipant> findByTournamentId(Long tournamentId);
    
    /**
     * Find all participants in a specific tournament with player eagerly loaded.
     */
    @Query("SELECT tp FROM TournamentParticipant tp JOIN FETCH tp.player WHERE tp.tournament.id = :tournamentId")
    List<TournamentParticipant> findByTournamentIdWithPlayer(@Param("tournamentId") Long tournamentId);
    
    /**
     * Find all active participants in a tournament.
     */
    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, TournamentParticipant.Status status);
    
    /**
     * Find a specific player's participation in a tournament.
     */
    Optional<TournamentParticipant> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);
    
    /**
     * Check if a player is already in a tournament.
     */
    boolean existsByTournamentIdAndPlayerId(Long tournamentId, Long playerId);
    
    /**
     * Count active participants in a tournament.
     */
    long countByTournamentIdAndStatus(Long tournamentId, TournamentParticipant.Status status);
    
    /**
     * Count all participants in a tournament (regardless of status).
     */
    int countByTournamentId(Long tournamentId);
    
    /**
     * Get all tournaments a player has participated in.
     */
    List<TournamentParticipant> findByPlayerId(Long playerId);
}
