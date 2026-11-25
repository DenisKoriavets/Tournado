package com.tournado.core.matches.service;

import java.util.UUID;

public record MatchCompletedEvent(
        UUID tournamentId,
        UUID matchId,
        int roundNumber,
        int matchOrder,
        UUID winnerTeamId
) {
}