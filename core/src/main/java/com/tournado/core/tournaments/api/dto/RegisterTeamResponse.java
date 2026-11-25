package com.tournado.core.tournaments.api.dto;

import com.tournado.core.tournaments.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisterTeamResponse(
        UUID registrationId,
        UUID tournamentId,
        UUID teamId,
        RegistrationStatus status,
        Instant registeredAt
) {
}
