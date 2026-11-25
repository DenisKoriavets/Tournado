package com.tournado.core.matches.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MatchBracketService {

    void createBracketMatches(List<BracketMatchSeed> seeds);

    List<BracketMatchView> findMatchesByTournamentId(UUID tournamentId);

    void advanceTeamToSlot(
            UUID tournamentId,
            int targetRoundNumber,
            int targetMatchOrder,
            boolean isTeam1Slot,
            UUID advancingTeamId,
            Instant readyDeadlineAt
    );
}