package com.tournado.core.tournaments.api.dto;

import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TournamentResponse(
        UUID id,
        String publicSlug,
        String name,
        String description,
        Discipline discipline,
        int teamSize,
        TournamentFormat format,
        int slotLimit,
        TournamentStatus status,
        Instant registrationDeadlineAt,
        Instant checkInOpensAt,
        Instant startTimeAt,
        String cs2ServerConnect,
        List<String> mapPool,
        int registeredTeamsCount,
        Instant createdAt
) {
    public static TournamentResponse fromEntity(TournamentEntity entity, int registeredTeamsCount) {
        return new TournamentResponse(
                entity.getId(),
                entity.getId().toString(),
                entity.getName(),
                entity.getDescription(),
                entity.getDiscipline(),
                entity.getDiscipline().getTeamSize(),
                entity.getFormat(),
                entity.getSlotLimit(),
                entity.getStatus(),
                entity.getRegistrationDeadlineAt(),
                entity.getCheckInOpensAt(),
                entity.getStartTimeAt(),
                entity.getCs2ServerConnect(),
                entity.getMapNames(),
                registeredTeamsCount,
                entity.getCreatedAt()
        );
    }
}
