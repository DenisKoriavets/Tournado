package com.tournado.core.tournaments.api.dto;

import com.tournado.core.tournaments.domain.Discipline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTournamentRequest(
        @NotBlank(message = "Tournament name must not be blank")
        @Size(max = 150, message = "Tournament name must not exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "Discipline must not be null")
        Discipline discipline,

        @NotNull(message = "Slot limit must not be null")
        Integer slotLimit,

        @NotNull(message = "Registration deadline must not be null")
        Instant registrationDeadlineAt,

        @NotNull(message = "Check-in opening time must not be null")
        Instant checkInOpensAt,

        @NotNull(message = "Start time must not be null")
        Instant startTimeAt,

        String cs2ServerConnect
) {
}
