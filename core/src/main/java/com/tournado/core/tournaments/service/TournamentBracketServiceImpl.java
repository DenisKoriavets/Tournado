package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
import com.tournado.core.tournaments.exception.RegistrationClosedException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentBracketServiceImpl implements TournamentBracketService {

    private static final String NO_SHOW_REASON = "Excluded automatically: check-in was not confirmed in time.";

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationService registrationService;
    private final Clock clock;

    public TournamentBracketServiceImpl(
            TournamentRepository tournamentRepository,
            RegistrationRepository registrationRepository,
            RegistrationService registrationService,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.registrationService = registrationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<UUID> prepareAndStartTournament(UUID tournamentId, UUID actorUserId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        TournamentEntity tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        if (!tournament.getCreatedByUserId().equals(actorUserId)) {
            throw new OnlyOrganizerAllowedException();
        }
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new RegistrationClosedException("Tournament has already started or completed.");
        }

        // Ensure immutable roster snapshots exist before freezing bracket participants (US-14)
        registrationService.lockRosters(tournamentId, actorUserId);

        Instant now = Instant.now(clock);
        List<RegistrationEntity> noShows = registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(
                tournamentId,
                List.of(RegistrationStatus.REGISTERED)
        );
        if (!noShows.isEmpty()) {
            for (RegistrationEntity noShow : noShows) {
                noShow.setStatus(RegistrationStatus.EXCLUDED);
                noShow.setExcludedAt(now);
                noShow.setExclusionReason(NO_SHOW_REASON);
            }
            registrationRepository.saveAll(noShows);
        }

        List<RegistrationEntity> checkedInRegistrations =
                registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(
                        tournamentId,
                        List.of(RegistrationStatus.CHECKED_IN)
                );

        List<UUID> checkedInTeamIds = checkedInRegistrations.stream()
                .map(RegistrationEntity::getTeamId)
                .toList();

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);

        return checkedInTeamIds;
    }

    @Override
    @Transactional
    public void completeTournament(UUID tournamentId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        TournamentEntity tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        if (tournament.getStatus() != TournamentStatus.COMPLETED) {
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournamentRepository.save(tournament);
        }
    }
}