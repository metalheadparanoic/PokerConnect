package poker.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import poker.server.model.PlayerHandEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing and managing player hand persistence.
 */
@Repository
public interface PlayerHandRepository extends JpaRepository<PlayerHandEntity, Long> {
    
    /**
     * Find all player hands for a tournament.
     */
    List<PlayerHandEntity> findByTournamentId(Long tournamentId);
    
    /**
     * Find a specific player's hand in a tournament.
     */
    Optional<PlayerHandEntity> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);
    
    /**
     * Find all active (not folded) players in a tournament.
     */
    @Query("SELECT ph FROM PlayerHandEntity ph WHERE ph.tournamentId = ?1 AND ph.folded = false")
    List<PlayerHandEntity> findActivePlayers(Long tournamentId);
    
    /**
     * Count active players in a tournament.
     */
    @Query("SELECT COUNT(ph) FROM PlayerHandEntity ph WHERE ph.tournamentId = ?1 AND ph.folded = false")
    long countActivePlayers(Long tournamentId);
    
    /**
     * Find players who can still act (not folded, not all-in).
     */
    @Query("SELECT ph FROM PlayerHandEntity ph WHERE ph.tournamentId = ?1 AND ph.folded = false AND ph.allIn = false")
    List<PlayerHandEntity> findPlayersWhoCanAct(Long tournamentId);
    
    /**
     * Delete all hands for a tournament.
     */
    void deleteByTournamentId(Long tournamentId);
}
