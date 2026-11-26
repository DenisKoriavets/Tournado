package com.tournado.core.bracket.repository;

import com.tournado.core.bracket.domain.BracketEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BracketRepository extends JpaRepository<BracketEntity, UUID> {

    Optional<BracketEntity> findByTournamentId(UUID tournamentId);

    boolean existsByTournamentId(UUID tournamentId);
}