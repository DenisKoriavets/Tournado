package com.tournado.core.tournaments.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterTeamRequest(
        @NotNull(message = "Team ID is required")
        UUID teamId
) {
}
