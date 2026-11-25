package com.tournado.core.tournaments.api.dto;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisteredTeamItem(
        UUID registrationId,
        UUID teamId,
        String teamName,
        Discipline discipline,
        RegistrationStatus status,
        int memberCount,
        Instant registeredAt,
        boolean rosterLocked
) {
}
