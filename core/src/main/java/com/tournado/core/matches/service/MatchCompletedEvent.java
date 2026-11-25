package com.tournado.core.matches.service;

import java.util.UUID;

/**
 * Published inside the completing transaction whenever a match gets a winner (US-28, US-34).
 * {@code finalMatch} is true when there is no next bracket round, i.e. the tournament winner is known (US-35).
 */
public record MatchCompletedEvent(
        UUID matchId,
        UUID tournamentId,
        UUID winnerTeamId,
        boolean finalMatch
) {
}
