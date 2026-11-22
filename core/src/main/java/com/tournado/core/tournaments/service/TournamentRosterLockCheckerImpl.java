package com.tournado.core.tournaments.service;

import com.tournado.core.teams.service.TournamentRosterLockChecker;
import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TournamentRosterLockCheckerImpl implements TournamentRosterLockChecker {

    private static final List<RegistrationStatus> ACTIVE_STATUSES = List.of(
            RegistrationStatus.REGISTERED,
            RegistrationStatus.CHECKED_IN
    );

    private final RegistrationRepository registrationRepository;
    private final RosterSnapshotRepository rosterSnapshotRepository;
    private final TournamentRepository tournamentRepository;
    private final Clock clock;

    public TournamentRosterLockCheckerImpl(
            RegistrationRepository registrationRepository,
            RosterSnapshotRepository rosterSnapshotRepository,
            TournamentRepository tournamentRepository,
            Clock clock) {
        this.registrationRepository = registrationRepository;
        this.rosterSnapshotRepository = rosterSnapshotRepository;
        this.tournamentRepository = tournamentRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeamRosterLocked(UUID teamId) {
        Objects.requireNonNull(teamId, "teamId must not be null");
        List<RegistrationEntity> activeRegistrations = registrationRepository.findByTeamIdAndStatusIn(
                teamId,
                ACTIVE_STATUSES
        );
        if (activeRegistrations.isEmpty()) {
            return false;
        }

        List<UUID> registrationIds = activeRegistrations.stream()
                .map(RegistrationEntity::getId)
                .toList();
        if (rosterSnapshotRepository.existsByRegistrationIdIn(registrationIds)) {
            return true;
        }

        List<UUID> tournamentIds = activeRegistrations.stream()
                .map(RegistrationEntity::getTournamentId)
                .distinct()
                .toList();
        Instant now = Instant.now(clock);
        List<TournamentEntity> tournaments = tournamentRepository.findAllById(tournamentIds);
        return tournaments.stream()
                .anyMatch(tournament -> now.isAfter(tournament.getRegistrationDeadlineAt()));
    }
}
