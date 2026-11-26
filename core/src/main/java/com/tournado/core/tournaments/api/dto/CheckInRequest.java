package com.tournado.core.tournaments.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckInRequest(
        @NotNull(message = "teamId must not be null")
        UUID teamId
) {
}