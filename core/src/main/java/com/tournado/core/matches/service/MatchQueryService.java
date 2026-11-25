package com.tournado.core.matches.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public read API of the matches module (ADR-0003: modules interact through service interfaces only).
 */
public interface MatchQueryService {

    Optional<MatchView> findMatchById(UUID matchId);

    List<MatchView> findMatchesByTournamentId(UUID tournamentId);
}
