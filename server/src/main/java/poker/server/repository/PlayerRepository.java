package poker.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import poker.server.model.PlayerEntity;

import java.util.Optional;

/**
 * JPA Repository for PlayerEntity.
 */
@Repository
public interface PlayerRepository extends JpaRepository<PlayerEntity, Long> {
    
    Optional<PlayerEntity> findByUsername(String username);
    
    Optional<PlayerEntity> findByEmail(String email);
    
    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
