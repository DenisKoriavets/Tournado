package com.tournado.core.tournaments.api.dto;

import java.util.List;
import java.util.UUID;

public record TournamentRegistrationsResponse(
        UUID tournamentId,
        UUID organizerId,
        int slotLimit,
        int registeredCount,
        boolean rostersLocked,
        List<RegisteredTeamItem> teams
) {
}
