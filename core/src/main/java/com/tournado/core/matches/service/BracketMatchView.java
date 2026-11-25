package com.tournado.core.matches.service;

import com.tournado.core.matches.domain.MatchStatus;
import java.util.UUID;

public record BracketMatchView(
        UUID matchId,
        int roundNumber,
        int matchOrder,
        UUID team1Id,
        UUID team2Id,
        UUID winnerTeamId,
        MatchStatus status,
        boolean isBye
) {
}