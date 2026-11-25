package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.domain.RosterSnapshotEntity;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentParticipantsQueryServiceImpl implements TournamentParticipantsQueryService {

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final RosterSnapshotRepository rosterSnapshotRepository;

    public TournamentParticipantsQueryServiceImpl(
            TournamentRepository tournamentRepository,
            RegistrationRepository registrationRepository,
            RosterSnapshotRepository rosterSnapshotRepository) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.rosterSnapshotRepository = rosterSnapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findOrganizerId(UUID tournamentId) {
        return tournamentRepository.findById(tournamentId).map(TournamentEntity::getCreatedByUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RosterEntryView> findLockedRoster(UUID tournamentId, UUID teamId) {
        return registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .map(registration -> rosterSnapshotRepository.findByRegistrationIdIn(List.of(registration.getId())))
                .orElse(List.of())
                .stream()
                .sorted(Comparator.comparing(RosterSnapshotEntity::isCaptain).reversed()
                        .thenComparing(RosterSnapshotEntity::getDisplayName))
                .map(snapshot -> new RosterEntryView(
                        snapshot.getUserId(),
                        snapshot.getDisplayName(),
                        snapshot.getGameIdentifier(),
                        snapshot.isCaptain()))
                .toList();
    }
}
