package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.InvalidScheduleOrderException;
import com.tournado.core.tournaments.exception.InvalidSlotLimitException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService implements TournamentQueryService {

    public static final UUID SYSTEM_ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Set<Integer> ALLOWED_SLOT_LIMITS = Set.of(8, 16, 32);

    private final TournamentRepository tournamentRepository;
    private final TournamentMapPoolProvider mapPoolProvider;

    public TournamentService(TournamentRepository tournamentRepository, TournamentMapPoolProvider mapPoolProvider) {
        this.tournamentRepository = tournamentRepository;
        this.mapPoolProvider = mapPoolProvider;
    }

    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        validateSlotLimit(request.slotLimit());
        validateScheduleOrder(request.registrationDeadlineAt(), request.checkInOpensAt(), request.startTimeAt());

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
        entity.setCreatedByUserId(SYSTEM_ORGANIZER_ID);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        List<String> mapNames = mapPoolProvider.getMapPoolForDiscipline(request.discipline());
        for (int i = 0; i < mapNames.size(); i++) {
            entity.addMap(mapNames.get(i), i);
        }

        TournamentEntity saved = tournamentRepository.save(entity);
        return TournamentResponse.fromEntity(saved, 0);
    }

    @Transactional(readOnly = true)
    public List<TournamentSummaryResponse> getTournaments() {
        return tournamentRepository.findAll().stream()
                .map(entity -> TournamentSummaryResponse.fromEntity(entity, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(UUID id) {
        TournamentEntity entity = tournamentRepository.findById(id)
                .orElseThrow(() -> new TournamentNotFoundException(id));
        return TournamentResponse.fromEntity(entity, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TournamentResponse> findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .map(entity -> TournamentResponse.fromEntity(entity, 0));
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
