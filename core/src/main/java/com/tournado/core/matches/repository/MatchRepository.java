package com.tournado.core.matches.repository;

import com.tournado.core.matches.domain.MatchEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {

    /**
     * Row-level lock for result-changing operations (score submission, dispute, completion) so two captains
     * reporting at the same moment are serialized instead of both seeing "first submission".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MatchEntity m WHERE m.id = :id")
    Optional<MatchEntity> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m FROM MatchEntity m
            WHERE m.tournamentId = :tournamentId AND m.roundNumber = :roundNumber AND m.matchOrder = :matchOrder
            """)
    Optional<MatchEntity> findBracketSlotForUpdate(
            @Param("tournamentId") UUID tournamentId,
            @Param("roundNumber") int roundNumber,
            @Param("matchOrder") int matchOrder);

    List<MatchEntity> findByTournamentIdOrderByRoundNumberAscMatchOrderAsc(UUID tournamentId);

    Optional<MatchEntity> findByTournamentIdAndRoundNumberAndMatchOrder(
            UUID tournamentId,
            int roundNumber,
            int matchOrder
    );
}