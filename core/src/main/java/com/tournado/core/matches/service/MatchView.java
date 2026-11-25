package com.tournado.core.matches.service;

import com.tournado.core.matches.domain.MatchStatus;
import java.util.List;
import java.util.UUID;

/**
 * Read-only projection of a match exposed to other modules (disputes, bracket).
 */
public record MatchView(
        UUID id,
        UUID tournamentId,
        int roundNumber,
        int matchOrder,
        UUID team1Id,
        UUID team2Id,
        UUID winnerTeamId,
        MatchStatus status,
        List<ScoreSubmissionView> scoreSubmissions
) {
}
