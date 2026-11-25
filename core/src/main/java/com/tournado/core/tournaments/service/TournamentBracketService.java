package com.tournado.core.tournaments.service;

import java.util.List;
import java.util.UUID;

public interface TournamentBracketService {

    /**
     * Validates organizer access, locks rosters if not yet snapshotted,
     * purges no-show (REGISTERED) teams to EXCLUDED, ensures >= 4 CHECKED_IN teams,
     * transitions tournament to IN_PROGRESS, and returns checked-in team IDs.
     */
    List<UUID> prepareAndStartTournament(UUID tournamentId, UUID actorUserId);

    /**
     * Transitions tournament status to COMPLETED once the final match concludes.
     */
    void completeTournament(UUID tournamentId);
}