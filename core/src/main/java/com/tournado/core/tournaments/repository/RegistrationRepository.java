package com.tournado.core.tournaments.repository;

import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistrationRepository extends JpaRepository<RegistrationEntity, UUID> {

    interface TournamentRegistrationCount {
        UUID getTournamentId();

        long getRegisteredCount();
    }

    @Query("""
            SELECT r.tournamentId AS tournamentId, COUNT(r) AS registeredCount
            FROM RegistrationEntity r
            WHERE r.tournamentId IN :tournamentIds
              AND r.status IN :statuses
            GROUP BY r.tournamentId
            """)
    List<TournamentRegistrationCount> countByTournamentIdsAndStatusIn(
            @Param("tournamentIds") Collection<UUID> tournamentIds,
            @Param("statuses") Collection<RegistrationStatus> statuses
    );

    long countByTournamentIdAndStatusIn(UUID tournamentId, Collection<RegistrationStatus> statuses);

    List<RegistrationEntity> findByTournamentIdAndStatusInOrderByRegisteredAtAsc(
            UUID tournamentId,
            Collection<RegistrationStatus> statuses
    );

    Optional<RegistrationEntity> findByTournamentIdAndTeamId(UUID tournamentId, UUID teamId);

    Optional<RegistrationEntity> findByTournamentIdAndTeamIdAndStatusIn(
            UUID tournamentId,
            UUID teamId,
            Collection<RegistrationStatus> statuses
    );

    boolean existsByTournamentIdAndTeamIdAndStatusIn(
            UUID tournamentId,
            UUID teamId,
            Collection<RegistrationStatus> statuses
    );

    List<RegistrationEntity> findByTeamIdAndStatusIn(UUID teamId, Collection<RegistrationStatus> statuses);
}
