package com.tournado.core.tournaments.api.dto;

import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.time.Instant;
import java.util.UUID;

public record TournamentSummaryResponse(
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
        int registeredTeamsCount,
        Instant createdAt
) {
    public static TournamentSummaryResponse fromEntity(TournamentEntity entity, int registeredTeamsCount) {
        return new TournamentSummaryResponse(
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
                registeredTeamsCount,
                entity.getCreatedAt()
        );
    }
}
