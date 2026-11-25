package com.tournado.core.matches.service;

import com.tournado.core.matches.domain.MatchStatus;
import java.time.Instant;
import java.util.UUID;

public record BracketMatchSeed(
        UUID matchId,
        UUID tournamentId,
        UUID bracketId,
        int roundNumber,
        int matchOrder,
        UUID team1Id,
        UUID team2Id,
        UUID winnerTeamId,
        MatchStatus status,
        boolean isBye,
        Instant readyDeadlineAt
) {
}