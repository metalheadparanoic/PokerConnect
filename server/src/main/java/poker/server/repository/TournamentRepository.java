package poker.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poker.server.model.TournamentEntity;

import java.util.List;

/**
 * JPA Repository for TournamentEntity.
 */
@Repository
public interface TournamentRepository extends JpaRepository<TournamentEntity, Long> {
    
    List<TournamentEntity> findByStatus(TournamentEntity.Status status);
}
