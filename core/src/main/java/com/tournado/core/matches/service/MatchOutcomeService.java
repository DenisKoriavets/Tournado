package com.tournado.core.matches.service;

import java.util.UUID;

/**
 * Public write API used by the disputes module to freeze and finally close a match (US-30, US-34).
 */
public interface MatchOutcomeService {

    /**
     * Moves an in-progress match to {@code DISPUTED}, halting bracket progression (BR-46).
     */
    void markDisputed(UUID matchId);

    /**
     * Closes the match with the given winner and advances the winner to the next bracket round (BR-42, BR-50).
     */
    MatchView completeMatch(UUID matchId, UUID winnerTeamId);
}
