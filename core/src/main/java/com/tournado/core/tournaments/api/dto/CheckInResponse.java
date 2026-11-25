package com.tournado.core.tournaments.api.dto;

import com.tournado.core.tournaments.domain.RegistrationStatus;
import java.time.Instant;

public record CheckInResponse(
        RegistrationStatus status,
        Instant checkedInAt
) {
}