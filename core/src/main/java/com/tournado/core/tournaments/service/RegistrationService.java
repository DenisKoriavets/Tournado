package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.CheckInResponse;
import com.tournado.core.tournaments.api.dto.LockRostersResponse;
import com.tournado.core.tournaments.api.dto.RegisterTeamResponse;
import com.tournado.core.tournaments.api.dto.RegisteredTeamItem;
import com.tournado.core.tournaments.api.dto.TournamentRegistrationsResponse;
import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.RosterSnapshotEntity;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.AlreadyRegisteredException;
import com.tournado.core.tournaments.exception.CheckInAlreadyConfirmedException;
import com.tournado.core.tournaments.exception.CheckInExpiredException;
import com.tournado.core.tournaments.exception.CheckInNotOpenException;
import com.tournado.core.tournaments.exception.MissingGameIdentifierException;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
import com.tournado.core.tournaments.exception.RegisteredTeamNotFoundException;
import com.tournado.core.tournaments.exception.RegistrationCaptainForbiddenException;
import com.tournado.core.tournaments.exception.RegistrationClosedException;
import com.tournado.core.tournaments.exception.RegistrationDisciplineMismatchException;
import com.tournado.core.tournaments.exception.RegistrationNotFoundException;
import com.tournado.core.tournaments.exception.RosterNotFullException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private static final List<RegistrationStatus> ACTIVE_STATUSES = List.of(
            RegistrationStatus.REGISTERED,
            RegistrationStatus.CHECKED_IN
    );

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final RosterSnapshotRepository rosterSnapshotRepository;
    private final TeamQueryService teamQueryService;
    private final GameIdentifierExtractor gameIdentifierExtractor;
    private final Clock clock;

    public RegistrationService(
            TournamentRepository tournamentRepository,
            RegistrationRepository registrationRepository,
            RosterSnapshotRepository rosterSnapshotRepository,
            TeamQueryService teamQueryService,
            GameIdentifierExtractor gameIdentifierExtractor,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.rosterSnapshotRepository = rosterSnapshotRepository;
        this.teamQueryService = teamQueryService;
        this.gameIdentifierExtractor = gameIdentifierExtractor;
        this.clock = clock;
    }

    @Transactional
    public RegisterTeamResponse registerTeam(UUID tournamentId, UUID teamId, UUID actorUserId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(teamId, "teamId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        TournamentEntity tournament = findTournamentOrThrow(tournamentId);
        Instant now = Instant.now(clock);
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN
                || now.isAfter(tournament.getRegistrationDeadlineAt())) {
            throw new RegistrationClosedException("Tournament registration is closed.");
        }

        TeamView team = teamQueryService.findTeamById(teamId)
                .orElseThrow(() -> new RegisteredTeamNotFoundException(teamId));
        if (!team.captainId().equals(actorUserId)) {
            throw new RegistrationCaptainForbiddenException();
        }
        if (team.discipline() != tournament.getDiscipline()) {
            throw new RegistrationDisciplineMismatchException(team.discipline(), tournament.getDiscipline());
        }

        registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .ifPresent(existing -> {
                    if (existing.getStatus() == RegistrationStatus.EXCLUDED) {
                        throw new RegistrationClosedException("Team was removed from this tournament by the organizer.");
                    }
                    throw new AlreadyRegisteredException(teamId, tournamentId);
                });

        long activeCount = registrationRepository.countByTournamentIdAndStatusIn(tournamentId, ACTIVE_STATUSES);
        if (activeCount >= tournament.getSlotLimit()) {
            throw new RegistrationClosedException("Tournament slot limit has been reached.");
        }

        if (!team.isFull()) {
            throw new RosterNotFullException(team.memberCount(), team.maxMembers(), team.discipline());
        }

        validateRosterGameIdentifiers(tournament.getDiscipline(), team);

        RegistrationEntity registration = new RegistrationEntity();
        registration.setId(UUID.randomUUID());
        registration.setTournamentId(tournamentId);
        registration.setTeamId(teamId);
        registration.setStatus(RegistrationStatus.REGISTERED);
        registration.setRegisteredAt(now);

        RegistrationEntity saved = registrationRepository.save(registration);
        return new RegisterTeamResponse(
                saved.getId(),
                saved.getTournamentId(),
                saved.getTeamId(),
                saved.getStatus(),
                saved.getRegisteredAt()
        );
    }

    @Transactional(readOnly = true)
    public TournamentRegistrationsResponse getRegistrations(UUID tournamentId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        TournamentEntity tournament = findTournamentOrThrow(tournamentId);
        List<RegistrationEntity> registrations = registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(
                tournamentId,
                ACTIVE_STATUSES
        );
        if (registrations.isEmpty()) {
            return new TournamentRegistrationsResponse(
                    tournament.getId(),
                    tournament.getCreatedByUserId(),
                    tournament.getSlotLimit(),
                    0,
                    false,
                    List.of()
            );
        }

        List<UUID> teamIds = registrations.stream()
                .map(RegistrationEntity::getTeamId)
                .toList();
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(teamIds);
        List<UUID> registrationIds = registrations.stream()
                .map(RegistrationEntity::getId)
                .toList();
        Set<UUID> snapshottedRegistrationIds = rosterSnapshotRepository.findSnapshottedRegistrationIds(registrationIds);
        boolean deadlinePassed = Instant.now(clock).isAfter(tournament.getRegistrationDeadlineAt());

        List<RegisteredTeamItem> items = registrations.stream()
                .map(registration -> toRegisteredTeamItem(
                        registration,
                        teamsById.get(registration.getTeamId()),
                        tournament.getDiscipline(),
                        snapshottedRegistrationIds.contains(registration.getId()) || deadlinePassed
                ))
                .toList();

        boolean rostersLocked = registrations.stream()
                .allMatch(reg -> snapshottedRegistrationIds.contains(reg.getId()));

        return new TournamentRegistrationsResponse(
                tournament.getId(),
                tournament.getCreatedByUserId(),
                tournament.getSlotLimit(),
                items.size(),
                rostersLocked,
                items
        );
    }

    @Transactional
    public void removeTeamRegistration(UUID tournamentId, UUID teamId, UUID actorUserId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(teamId, "teamId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        TournamentEntity tournament = findTournamentOrThrow(tournamentId);
        verifyOrganizer(tournament, actorUserId);

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new RegistrationClosedException("Cannot remove team registration after tournament has started.");
        }

        RegistrationEntity registration = registrationRepository
                .findByTournamentIdAndTeamIdAndStatusIn(tournamentId, teamId, ACTIVE_STATUSES)
                .orElseThrow(() -> new RegistrationNotFoundException(tournamentId, teamId));

        registration.setStatus(RegistrationStatus.EXCLUDED);
        registration.setExcludedAt(Instant.now(clock));
        registration.setExclusionReason("Removed by tournament organizer");
        registrationRepository.save(registration);
    }

    @Transactional
    public LockRostersResponse lockRosters(UUID tournamentId, UUID actorUserId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        TournamentEntity tournament = findTournamentOrThrow(tournamentId);
        verifyOrganizer(tournament, actorUserId);

        List<RegistrationEntity> registrations = registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(
                tournamentId,
                ACTIVE_STATUSES
        );
        if (registrations.isEmpty()) {
            return new LockRostersResponse("LOCKED", 0);
        }

        List<UUID> registrationIds = registrations.stream()
                .map(RegistrationEntity::getId)
                .toList();
        List<RosterSnapshotEntity> existingSnapshots = rosterSnapshotRepository.findByRegistrationIdIn(registrationIds);
        Set<UUID> snapshottedRegIds = existingSnapshots.stream()
                .map(RosterSnapshotEntity::getRegistrationId)
                .collect(Collectors.toSet());

        List<RegistrationEntity> pendingRegistrations = registrations.stream()
                .filter(reg -> !snapshottedRegIds.contains(reg.getId()))
                .toList();
        if (pendingRegistrations.isEmpty()) {
            return new LockRostersResponse("LOCKED", existingSnapshots.size());
        }

        List<UUID> pendingTeamIds = pendingRegistrations.stream()
                .map(RegistrationEntity::getTeamId)
                .toList();
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(pendingTeamIds);
        Instant now = Instant.now(clock);
        List<RosterSnapshotEntity> newSnapshots = new ArrayList<>();

        for (RegistrationEntity registration : pendingRegistrations) {
            TeamView team = teamsById.get(registration.getTeamId());
            if (team == null) {
                throw new RegisteredTeamNotFoundException(registration.getTeamId());
            }
            validateRosterGameIdentifiers(tournament.getDiscipline(), team);
            for (TeamMemberView member : team.members()) {
                RosterSnapshotEntity snapshot = new RosterSnapshotEntity();
                snapshot.setId(UUID.randomUUID());
                snapshot.setRegistrationId(registration.getId());
                snapshot.setUserId(member.userId());
                snapshot.setDisplayName(member.displayName());
                snapshot.setGameIdentifier(
                        gameIdentifierExtractor.extract(tournament.getDiscipline(), member).orElse("")
                );
                snapshot.setCaptain(member.isCaptain());
                snapshot.setSnapshottedAt(now);
                newSnapshots.add(snapshot);
            }
        }

        rosterSnapshotRepository.saveAll(newSnapshots);
        return new LockRostersResponse("LOCKED", existingSnapshots.size() + newSnapshots.size());
    }

    @Transactional
    public CheckInResponse checkInTeam(UUID tournamentId, UUID teamId, UUID actorUserId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(teamId, "teamId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        TournamentEntity tournament = findTournamentOrThrow(tournamentId);
        Instant now = Instant.now(clock);

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS
                || tournament.getStatus() == TournamentStatus.COMPLETED
                || !now.isBefore(tournament.getStartTimeAt())) {
            throw new CheckInExpiredException(tournamentId);
        }
        if (tournament.getStatus() == TournamentStatus.DRAFT
                || (tournament.getStatus() != TournamentStatus.CHECK_IN_ACTIVE
                && now.isBefore(tournament.getCheckInOpensAt()))) {
            throw new CheckInNotOpenException(tournamentId);
        }

        TeamView team = teamQueryService.findTeamById(teamId)
                .orElseThrow(() -> new RegisteredTeamNotFoundException(teamId));
        if (!team.captainId().equals(actorUserId)) {
            throw new RegistrationCaptainForbiddenException();
        }

        RegistrationEntity registration = registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .orElseThrow(() -> new RegistrationNotFoundException(tournamentId, teamId));

        if (registration.getStatus() == RegistrationStatus.EXCLUDED) {
            throw new RegistrationNotFoundException(tournamentId, teamId);
        }
        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw new CheckInAlreadyConfirmedException(teamId, tournamentId);
        }

        registration.setStatus(RegistrationStatus.CHECKED_IN);
        registration.setCheckedInAt(now);
        registrationRepository.save(registration);

        if (tournament.getStatus() == TournamentStatus.REGISTRATION_OPEN) {
            tournament.setStatus(TournamentStatus.CHECK_IN_ACTIVE);
            tournamentRepository.save(tournament);
        }

        return new CheckInResponse(RegistrationStatus.CHECKED_IN, now);
    }

    private TournamentEntity findTournamentOrThrow(UUID tournamentId) {
        return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
    }

    private void verifyOrganizer(TournamentEntity tournament, UUID actorUserId) {
        if (!tournament.getCreatedByUserId().equals(actorUserId)) {
            throw new OnlyOrganizerAllowedException();
        }
    }

    private void validateRosterGameIdentifiers(Discipline discipline, TeamView team) {
        String fieldName = gameIdentifierExtractor.getFieldName(discipline);
        String requirementLabel = gameIdentifierExtractor.getRequirementLabel(discipline);
        List<InvalidParam> missingParams = team.members().stream()
                .filter(member -> gameIdentifierExtractor.extract(discipline, member).isEmpty())
                .map(member -> new InvalidParam(
                        fieldName,
                        "Player '" + member.displayName() + "' is missing required " + requirementLabel + "."
                ))
                .toList();
        if (!missingParams.isEmpty()) {
            throw new MissingGameIdentifierException(
                    "All roster members must have a valid " + requirementLabel + " to register.",
                    missingParams
            );
        }
    }

    private RegisteredTeamItem toRegisteredTeamItem(
            RegistrationEntity registration,
            TeamView team,
            Discipline fallbackDiscipline,
            boolean rosterLocked) {
        String teamName = team != null ? team.name() : "Unknown Team";
        Discipline discipline = team != null ? team.discipline() : fallbackDiscipline;
        int memberCount = team != null ? team.memberCount() : 0;
        return new RegisteredTeamItem(
                registration.getId(),
                registration.getTeamId(),
                teamName,
                discipline,
                registration.getStatus(),
                memberCount,
                registration.getRegisteredAt(),
                rosterLocked
        );
    }
}