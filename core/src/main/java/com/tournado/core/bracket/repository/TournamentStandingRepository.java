package com.tournado.core.bracket.repository;

import com.tournado.core.bracket.domain.TournamentStandingEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentStandingRepository extends JpaRepository<TournamentStandingEntity, UUID> {

    List<TournamentStandingEntity> findByTournamentIdOrderByRankPlaceAsc(UUID tournamentId);

    boolean existsByTournamentId(UUID tournamentId);
}