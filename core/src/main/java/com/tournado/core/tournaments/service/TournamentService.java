package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.InvalidScheduleOrderException;
import com.tournado.core.tournaments.exception.InvalidSlotLimitException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService implements TournamentQueryService {

    private static final Set<Integer> ALLOWED_SLOT_LIMITS = Set.of(8, 16, 32);
    private static final List<RegistrationStatus> ACTIVE_REGISTRATION_STATUSES = List.of(
            RegistrationStatus.REGISTERED,
            RegistrationStatus.CHECKED_IN
    );

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final TournamentMapPoolProvider mapPoolProvider;
    private final Clock clock;

    public TournamentService(
            TournamentRepository tournamentRepository,
            RegistrationRepository registrationRepository,
            TournamentMapPoolProvider mapPoolProvider,
            Clock clock) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.mapPoolProvider = mapPoolProvider;
        this.clock = clock;
    }

    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request, UUID creatorUserId) {
        Objects.requireNonNull(creatorUserId, "creatorUserId must not be null");
        validateSlotLimit(request.slotLimit());
        validateScheduleOrder(request.registrationDeadlineAt(), request.checkInOpensAt(), request.startTimeAt());

        Instant now = Instant.now(clock);
        TournamentEntity entity = new TournamentEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(request.name().trim());
        entity.setDescription(request.description());
        entity.setDiscipline(request.discipline());
        entity.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        entity.setSlotLimit(request.slotLimit());
        entity.setStatus(TournamentStatus.REGISTRATION_OPEN);
        entity.setRegistrationDeadlineAt(request.registrationDeadlineAt());
        entity.setCheckInOpensAt(request.checkInOpensAt());
        entity.setStartTimeAt(request.startTimeAt());
        entity.setCs2ServerConnect(request.cs2ServerConnect());
        entity.setCreatedByUserId(creatorUserId);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        List<String> mapNames = mapPoolProvider.getMapPoolForDiscipline(request.discipline());
        for (int i = 0; i < mapNames.size(); i++) {
            entity.addMap(mapNames.get(i), i);
        }

        TournamentEntity saved = tournamentRepository.save(entity);
        return TournamentResponse.fromEntity(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<TournamentSummaryResponse> getTournaments() {
        List<TournamentEntity> tournaments = tournamentRepository.findAll();
        if (tournaments.isEmpty()) {
            return List.of();
        }
        List<UUID> tournamentIds = tournaments.stream()
                .map(TournamentEntity::getId)
                .toList();
        Map<UUID, Integer> countsByTournamentId = registrationRepository
                .countByTournamentIdsAndStatusIn(tournamentIds, ACTIVE_REGISTRATION_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        RegistrationRepository.TournamentRegistrationCount::getTournamentId,
                        count -> Math.toIntExact(count.getRegisteredCount())
                ));
        return tournaments.stream()
                .map(entity -> TournamentSummaryResponse.fromEntity(
                        entity,
                        countsByTournamentId.getOrDefault(entity.getId(), 0)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(UUID id) {
        TournamentEntity entity = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
        int registeredCount = Math.toIntExact(
                registrationRepository.countByTournamentIdAndStatusIn(entity.getId(), ACTIVE_REGISTRATION_STATUSES)
        );
        return TournamentResponse.fromEntity(entity, registeredCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TournamentResponse> findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .map(entity -> TournamentResponse.fromEntity(
                        entity,
                        Math.toIntExact(registrationRepository.countByTournamentIdAndStatusIn(
                                entity.getId(),
                                ACTIVE_REGISTRATION_STATUSES
                        ))
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TournamentSummaryResponse> findAllTournaments() {
        return getTournaments();
    }

    private void validateSlotLimit(Integer slotLimit) {
        if (slotLimit == null || !ALLOWED_SLOT_LIMITS.contains(slotLimit)) {
            throw new InvalidSlotLimitException("Slot limit must be one of {8, 16, 32}.");
        }
    }

    private void validateScheduleOrder(Instant registrationDeadlineAt, Instant checkInOpensAt, Instant startTimeAt) {
        if (registrationDeadlineAt == null || checkInOpensAt == null || startTimeAt == null) {
            throw new InvalidScheduleOrderException("Schedule timestamps must not be null.");
        }
        if (!registrationDeadlineAt.isBefore(checkInOpensAt) || !checkInOpensAt.isBefore(startTimeAt)) {
            throw new InvalidScheduleOrderException("Schedule order violated: must be registrationDeadline < checkInOpen < startTime.");
        }
    }
}
