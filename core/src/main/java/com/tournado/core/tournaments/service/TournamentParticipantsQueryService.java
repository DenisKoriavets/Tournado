package com.tournado.core.tournaments.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read API for organizer ownership and locked rosters, used by the disputes module (US-31..US-34).
 */
public interface TournamentParticipantsQueryService {

    Optional<UUID> findOrganizerId(UUID tournamentId);

    /**
     * Roster snapshot taken at Roster Lock; empty if the team's roster has not been locked for this tournament.
     */
    List<RosterEntryView> findLockedRoster(UUID tournamentId, UUID teamId);
}
