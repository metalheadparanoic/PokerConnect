package poker.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poker.server.model.GamePhase;
import poker.server.model.GameStateEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing and managing game state persistence.
 */
@Repository
public interface GameStateRepository extends JpaRepository<GameStateEntity, Long> {
    
    /**
     * Find game state by tournament ID.
     */
    Optional<GameStateEntity> findByTournamentId(Long tournamentId);
    
    /**
     * Find all games in a specific phase.
     */
    List<GameStateEntity> findByPhase(GamePhase phase);
    
    /**
     * Check if a game state exists for a tournament.
     */
    boolean existsByTournamentId(Long tournamentId);
    
    /**
     * Delete game state by tournament ID.
     */
    void deleteByTournamentId(Long tournamentId);
}
